"""Build app/src/main/assets/crop_economics.json from official PSA OpenSTAT data.

For each CropCast crop:
- yield (t/ha): national production / area harvested, mean of 2021-2025
- farmgate price (PHP/kg): national annual average, mean of 2024-2025
- gross income (PHP/ha): yield x price

These are national averages. They exclude production costs, which PSA does not
publish for most vegetables, so the app labels the figure "gross".

Run:  .venv/Scripts/python scripts/build_crop_economics.py
"""

from __future__ import annotations

import io
import json
from pathlib import Path

import pandas as pd

from build_validation_data import PSA, cached, session

PROJECT_ROOT = Path(__file__).resolve().parents[1]
OUT_PATH = PROJECT_ROOT / "app/src/main/assets/crop_economics.json"
YIELD_YEARS = ["2021", "2022", "2023", "2024", "2025"]
PRICE_YEARS = ["2024", "2025"]
NFG = "https://openstat.psa.gov.ph/PXWeb/api/v1/en/DB/2M/NFG/"

# CropCast crop -> (production/area commodity label, farmgate table, farmgate commodity label)
CROPS = {
    "Tomato": ("Tomato", "0032M4AFN05", "Tomato"),
    "Okra": ("Okra [Lady's finger]", "0032M4AFN05", "Okra [Lady's finger]"),
    "Alugbati": ("Alugbati", "0032M4AFN06", "Alugbati"),
    "Potato": ("Potato", "0032M4AFN02", "Potato"),
    "Rice": ("Palay", "0032M4AFN01", "Palay [Paddy] Other Variety, dry (conv. to 14% mc)"),
    "Corn": ("Corn", "0032M4AFN01", "Corngrain [Maize] Yellow, matured"),
    "Eggplant": ("Eggplant", "0032M4AFN05", "Eggplant, long, purple"),
    "Cucumber": ("Cucumber", "0032M4AFN05", "Cucumber"),
    "Cabbage": ("Cabbage", "0032M4AFN06", "Cabbage"),
    "Sweet Potato": ("Camote [Sweet potato]", "0032M4AFN02", "Camote [Sweet potato]"),
    "Lettuce": ("Lettuce", "0032M4AFN06", "Lettuce"),
    "Spinach": ("Spinach", None, None),  # PSA publishes no spinach farmgate price
    "Pechay": ("..Pechay, native", "0032M4AFN06", "Pechay, native"),
    "Kangkong": ("Kangkong [Swamp cabbage]", "0032M4AFN06", "Kangkong [Swamp cabbage]"),
    "Ampalaya": ("Ampalaya fruit [Bitter gourd]", "0032M4AFN05", "Ampalaya fruit [Bitter gourd]"),
    "Sitaw": ("Sitao [Stringbeans]", "0032M4AFN03", "Sitao [Stringbeans]"),
    "Kalabasa": ("Squash fruit", "0032M4AFN05", "Squash fruit"),
}


def query(url: str, table: str, selections: dict[str, list[str]]) -> pd.DataFrame:
    """PXWeb query selecting values by their display text; returns the CSV as a DataFrame."""
    meta = cached(f"meta-{url}{table}", lambda: session.get(url + table + ".px", timeout=90).json())
    body = {"query": [], "response": {"format": "csv"}}
    for variable in meta["variables"]:
        wanted = selections.get(variable["code"])
        if wanted is None:
            continue
        codes = [c for c, t in zip(variable["values"], variable["valueTexts"]) if t.strip() in wanted]
        if not codes:
            raise ValueError(f"{table}: none of {wanted} in {variable['code']}")
        body["query"].append({"code": variable["code"], "selection": {"filter": "item", "values": codes}})
    text = cached(f"csv-{url}{table}-{json.dumps(selections, sort_keys=True)}",
                  lambda: session.post(url + table + ".px", json=body, timeout=180).text)
    return pd.read_csv(io.StringIO(text))


def national_annual(table: str, crop_code: str, labels: list[str], years: list[str]) -> dict[str, float]:
    df = query(PSA, table, {crop_code: labels, "Geolocation": ["PHILIPPINES"], "Year": years,
                            "Period": ["Annual"]})
    values = df.drop(columns=[crop_code, "Geolocation"]).apply(pd.to_numeric, errors="coerce")
    return dict(zip(df[crop_code].str.strip(), values.mean(axis=1)))


def main() -> None:
    veg_labels = [v[0] for k, v in CROPS.items() if k not in ("Rice", "Corn")]
    veg_production = national_annual("0082E4EVCP3", "Crop", veg_labels, YIELD_YEARS)
    veg_area = query(PSA, "0112E4EAHM3", {"Crop": veg_labels, "Geolocation": ["PHILIPPINES"],
                                             "Year": YIELD_YEARS, "Period": ["Annual"]})
    grain_production = national_annual("0012E4EVCP0", "Ecosystem/Croptype", ["Palay", "Corn"], YIELD_YEARS)
    grain_area = national_annual("0022E4EAHC0", "Ecosystem/Croptype", ["Palay", "Corn"], YIELD_YEARS)

    # The vegetable area table mixes "area planted" and "area harvested" columns; keep harvested.
    area_values = veg_area.drop(columns=["Crop", "Geolocation"])
    harvested = [c for c in area_values.columns if "arvest" in c] or list(area_values.columns)
    veg_area_by_crop = dict(zip(veg_area["Crop"].str.strip(),
                                area_values[harvested].apply(pd.to_numeric, errors="coerce").mean(axis=1)))

    result = {}
    for crop, (label, price_table, price_label) in CROPS.items():
        key = label.strip(". ") if label.startswith("..") else label
        production = grain_production.get(label) if crop in ("Rice", "Corn") else veg_production.get(label.strip())
        area = grain_area.get(label) if crop in ("Rice", "Corn") else veg_area_by_crop.get(label.strip())
        entry = {}
        if production and area:
            entry["yieldTonnesPerHa"] = round(production / area, 2)
        if price_table:
            prices = query(NFG, price_table, {"Commodity": [price_label], "Geolocation": ["PHILIPPINES"],
                                              "Year": PRICE_YEARS, "Period": ["Annual"]})
            values = prices.drop(columns=["Commodity", "Geolocation"]).apply(pd.to_numeric, errors="coerce")
            price = float(values.mean(axis=1).iloc[0])
            if price == price:  # not NaN
                entry["farmgatePricePhpPerKg"] = round(price, 2)
        if "yieldTonnesPerHa" in entry and "farmgatePricePhpPerKg" in entry:
            entry["grossPhpPerHa"] = round(entry["yieldTonnesPerHa"] * 1000 * entry["farmgatePricePhpPerKg"], -2)
        result[crop] = entry
        print(f"{crop:12s} {entry}")

    OUT_PATH.write_text(json.dumps({
        "source": "PSA OpenSTAT: production and area harvested 2021-2025 (0082E4EVCP3, 0112E4EAHM3, "
                  "0012E4EVCP0, 0022E4EAHC0); national farmgate prices 2024-2025 (2M/NFG tables)",
        "note": "National averages; gross income excludes production costs.",
        "crops": result,
    }, indent=1), encoding="utf-8")
    print(f"Wrote {OUT_PATH.relative_to(PROJECT_ROOT)}")


if __name__ == "__main__":
    main()
