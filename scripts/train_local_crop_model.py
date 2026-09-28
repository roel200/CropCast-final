"""Train a Random Forest for CropCast's 12 local crops from FAO ECOCROP ranges.

There is no labeled Philippine dataset for these 12 crops, so training rows are
synthesized per crop, the same way the public Crop_recommendation.csv was built:

- temperature and pH: FAO ECOCROP optimal ranges (data/raw/ecocrop/ecocrop.rds)
- pH values: drawn from real Philippine topsoil (WoSIS, 0-30 cm) that fall inside
  each crop's ECOCROP optimal pH range
- humidity and N, P, K: data/processed/crop_requirements_selected.json

The model therefore learns published crop requirements, not observed yields.
SoilsSync.xlsx (real Philippine soil tests) is used only as a pH plausibility check.

Run:  .venv/Scripts/python scripts/train_local_crop_model.py [--npk-scale 1.0]
"""

from __future__ import annotations

import argparse
import json
from datetime import datetime, timezone
from pathlib import Path

import numpy as np
import pandas as pd
import pyreadr
from sklearn.model_selection import StratifiedKFold, cross_val_predict

from train_android_public_crop_model import BASE_FEATURES, export_forest, new_model

PROJECT_ROOT = Path(__file__).resolve().parents[1]
ECOCROP_PATH = PROJECT_ROOT / "data/raw/ecocrop/ecocrop.rds"
WOSIS_PH_PATH = PROJECT_ROOT / "data/raw/wosis/wosis_latest_phaq.csv"
REQUIREMENTS_PATH = PROJECT_ROOT / "data/processed/crop_requirements_selected.json"
SOILSSYNC_PATH = PROJECT_ROOT / "SoilsSync.xlsx"
OUT_DIR = PROJECT_ROOT / "models/local_crop"
ECOCROP_SELECTED_PATH = PROJECT_ROOT / "data/processed/ecocrop_selected.csv"
SAMPLES_PER_CROP = 300
RANDOM_STATE = 42

# CropCast crop name -> ECOCROP NAME. Rice uses paddy Indica, the common Philippine lowland type.
ECOCROP_NAMES = {
    "Tomato": "Tomato",
    "Okra": "Okra, lady fingers",
    "Alugbati": "Ceylon spinach",
    "Potato": "Potato",
    "Rice": "Rice, paddy (Indica)",
    # Flint maize: its 90-140 day cycle matches Philippine corn; generic Maize runs to 365 days.
    "Corn": "Flint maize",
    "Eggplant": "Eggplant",
    "Cucumber": "Cucumber",
    "Cabbage": "Cabbage",
    "Sweet Potato": "Sweet potato",
    "Lettuce": "Lettuce",
    "Spinach": "Spinach",
}
# Extra Philippine crops ranked by the app's FarmSuitabilityEngine. They are exported to
# ecocrop_selected.csv but not used to train this model, which has no NPK data for them.
ENGINE_ONLY_NAMES = {
    "Pechay": "Pak choi",            # Brassica rapa, Pak Choi group
    "Kangkong": "Kang kong",         # Ipomoea aquatica
    "Ampalaya": "Bitter gourd *",    # Momordica charantia
    "Sitaw": "Asparagus bean",       # Vigna unguiculata subsp. sesquipedalis
    "Kalabasa": "Pumpkin",           # Cucurbita moschata
}
SOILSSYNC_TO_CROPCAST = {"Corn": "Corn", "Tomato": "Tomato", "Rice": "Rice"}


def load_ecocrop(names: dict[str, str] = ECOCROP_NAMES) -> pd.DataFrame:
    table = pyreadr.read_r(ECOCROP_PATH)[None]
    # "Kang kong" has two identical rows (aquatic and upland forms); keep one.
    rows = table[table["NAME"].isin(names.values())].drop_duplicates("NAME").set_index("NAME")
    missing = set(names.values()) - set(rows.index)
    if missing:
        raise ValueError(f"ECOCROP rows not found: {sorted(missing)}")
    selected = rows.loc[list(names.values())].reset_index()
    selected.insert(0, "crop", list(names))
    columns = ["crop", "NAME", "SCIENTNAME", "TMIN", "TOPMN", "TOPMX", "TMAX",
               "PHMIN", "PHOPMN", "PHOPMX", "PHMAX", "RMIN", "ROPMN", "ROPMX", "RMAX", "TEXT", "GMIN", "GMAX"]
    selected = selected[columns]
    numeric = [c for c in columns[3:] if c != "TEXT"]
    selected[numeric] = selected[numeric].astype(float)
    return selected


def load_wosis_topsoil_ph() -> np.ndarray:
    wosis = pd.read_csv(WOSIS_PH_PATH)
    topsoil = wosis[(wosis["country_name"] == "Philippines") & (wosis["upper_depth"] < 30)]
    return topsoil["value_avg"].dropna().to_numpy()


def spread(rng: np.random.Generator, low: float, high: float, n: int) -> np.ndarray:
    # Uniform inside the range plus 5% multiplicative noise, so single-value
    # ranges such as Alugbati NPK (180-180) still produce a spread of samples.
    return rng.uniform(low, high, n) * rng.normal(1.0, 0.05, n)


def synthesize(ecocrop: pd.DataFrame, wosis_ph: np.ndarray, npk_scale: float) -> pd.DataFrame:
    rng = np.random.default_rng(RANDOM_STATE)
    requirements = {
        c["name"]: c["ranges"]
        for c in json.loads(REQUIREMENTS_PATH.read_text(encoding="utf-8"))["crops"]
    }
    frames = []
    for row in ecocrop.itertuples():
        ranges = requirements[row.crop]
        in_range = wosis_ph[(wosis_ph >= row.PHOPMN) & (wosis_ph <= row.PHOPMX)]
        if len(in_range) < 10:
            raise ValueError(f"Too few WoSIS pH values inside the {row.crop} range")
        n = SAMPLES_PER_CROP
        frames.append(pd.DataFrame({
            "N": spread(rng, *ranges["nitrogen"], n) * npk_scale,
            "P": spread(rng, *ranges["phosphorus"], n) * npk_scale,
            "K": spread(rng, *ranges["potassium"], n) * npk_scale,
            "temperature": rng.uniform(row.TOPMN, row.TOPMX, n),
            "humidity": np.clip(spread(rng, *ranges["humidityPct"], n), 0, 100),
            "ph": rng.choice(in_range, n) + rng.normal(0, 0.05, n),
            "label": row.crop,
        }))
    return pd.concat(frames, ignore_index=True)


def soilssync_ph_check(ecocrop: pd.DataFrame) -> dict:
    soils = pd.read_excel(SOILSSYNC_PATH).dropna(subset=["crop", "ph_value"])
    ranges = ecocrop.set_index("crop")
    result = {}
    for source_name, crop in SOILSSYNC_TO_CROPCAST.items():
        ph = soils.loc[soils["crop"] == source_name, "ph_value"].astype(float)
        row = ranges.loc[crop]
        result[crop] = {
            "rows": int(len(ph)),
            "inside_optimal_ph": int(ph.between(row.PHOPMN, row.PHOPMX).sum()),
            "inside_absolute_ph": int(ph.between(row.PHMIN, row.PHMAX).sum()),
        }
    return result


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument(
        "--npk-scale", type=float, default=1.0,
        help="Multiply requirement-table NPK by this to match the RS485 probe's mg/kg. "
             "Keep 1.0 until a laboratory soil test gives a ratio.",
    )
    args = parser.parse_args()

    ecocrop = load_ecocrop()
    ECOCROP_SELECTED_PATH.parent.mkdir(parents=True, exist_ok=True)
    load_ecocrop({**ECOCROP_NAMES, **ENGINE_ONLY_NAMES}).to_csv(ECOCROP_SELECTED_PATH, index=False)
    wosis_ph = load_wosis_topsoil_ph()
    data = synthesize(ecocrop, wosis_ph, args.npk_scale)
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    data.to_csv(OUT_DIR / "training_data.csv", index=False)

    x, y = data[BASE_FEATURES], data["label"]
    folds = StratifiedKFold(n_splits=5, shuffle=True, random_state=RANDOM_STATE)
    probabilities = cross_val_predict(new_model(), x, y, cv=folds, method="predict_proba")
    classes = np.array(sorted(y.unique()))
    ranked = classes[np.argsort(-probabilities, axis=1)]
    top1 = float((ranked[:, 0] == y).mean())
    top3 = float((ranked[:, :3] == y.to_numpy()[:, None]).any(axis=1).mean())

    model = new_model().fit(x, y)
    asset = OUT_DIR / "local_crop_forest.bin"
    export_forest(model, asset, BASE_FEATURES)

    metrics = {
        "model_version": "local-crop-random-forest-v1",
        "trained_at_utc": datetime.now(timezone.utc).isoformat(),
        "features": BASE_FEATURES,
        "classes": classes.tolist(),
        "rows": len(data),
        "npk_scale": args.npk_scale,
        "cv5_accuracy": top1,
        "cv5_top3_accuracy": top3,
        "training_ranges": {f: [float(x[f].min()), float(x[f].max())] for f in BASE_FEATURES},
        "wosis_topsoil_ph": {
            "layers": int(len(wosis_ph)),
            "min": float(wosis_ph.min()),
            "median": float(np.median(wosis_ph)),
            "max": float(wosis_ph.max()),
        },
        "soilssync_ph_check": soilssync_ph_check(ecocrop),
        "asset": str(asset.relative_to(PROJECT_ROOT)),
        "asset_size_bytes": asset.stat().st_size,
        "sources": {
            "ecocrop": "FAO ECOCROP via Recocrop (github.com/cropmodels/Recocrop), CC BY 4.0",
            "wosis": "ISRIC WoSIS latest, layer wosis_latest_phaq, country Philippines",
            "npk_humidity": str(REQUIREMENTS_PATH.relative_to(PROJECT_ROOT)),
        },
        "limitations": [
            "Training rows are synthesized from published ranges; accuracy measures how well "
            "the forest separates those ranges, not field accuracy.",
            "NPK ranges have no stated unit; set --npk-scale after a laboratory soil test.",
            "Temperature is ECOCROP growing-season air temperature, not soil temperature.",
        ],
    }
    (OUT_DIR / "metrics.json").write_text(json.dumps(metrics, indent=2) + "\n", encoding="utf-8")
    print(f"rows={len(data)} cv5_accuracy={top1:.3f} cv5_top3={top3:.3f}")
    print(f"Asset: {metrics['asset']} ({metrics['asset_size_bytes']:,} bytes)")
    print("SoilsSync pH check:", metrics["soilssync_ph_check"])


if __name__ == "__main__":
    main()
