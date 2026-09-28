"""Build the real-world validation fixture for CropCast's farm crop ranking.

Ground truth comes from two independent sources:

1. PSA OpenSTAT: official 2021-2025 production of all 12 CropCast crops by
   province (tables 0082E4EVCP3 vegetables/root crops, 0012E4EVCP0 palay/corn).
   A province that produces a lot of a crop is evidence the crop grows there.
2. SoilsSync.xlsx: real Philippine farms with the crop grown and a lab pH.

For every province point and farm the script also stores what the app would
download for that spot: NASA POWER climate normals (with the grid-cell
elevation), the true point elevation (Open-Meteo), and the ISRIC SoilGrids
topsoil. Every HTTP response is cached under data/validation/.cache, so the
script can be stopped and resumed. The output is one JSON file the Kotlin test
FarmSuitabilityValidationTest reads offline.

Run:  .venv/Scripts/python scripts/build_validation_data.py
"""

from __future__ import annotations

import hashlib
import io
import json
import re
import time
from pathlib import Path

import pandas as pd
import requests

PROJECT_ROOT = Path(__file__).resolve().parents[1]
OUT_PATH = PROJECT_ROOT / "data/validation/sites.json"
CACHE_DIR = PROJECT_ROOT / "data/validation/.cache"
SOILSSYNC_PATH = PROJECT_ROOT / "SoilsSync.xlsx"
PSA = "https://openstat.psa.gov.ph/PXWeb/api/v1/en/DB/2E/CS/"
USER_AGENT = "CropCast-validation/1.0 (thesis research; contact via repository owner)"
YEARS = ["2021", "2022", "2023", "2024", "2025"]

# PSA commodity label -> CropCast crop name.
VEGETABLES = {
    "Cabbage": "Cabbage", "Camote [Sweet potato]": "Sweet Potato", "Eggplant": "Eggplant",
    "Tomato": "Tomato", "Potato": "Potato", "Lettuce": "Lettuce", "Okra [Lady's finger]": "Okra",
    "Cucumber": "Cucumber", "Alugbati": "Alugbati", "Spinach": "Spinach",
    "..Pechay, native": "Pechay", "Kangkong [Swamp cabbage]": "Kangkong",
    "Ampalaya fruit [Bitter gourd]": "Ampalaya", "Sitao [Stringbeans]": "Sitaw", "Squash fruit": "Kalabasa",
}
GRAINS = {"Palay": "Rice", "Corn": "Corn"}
SOILSSYNC_CROPS = {"Corn": "Corn", "Tomato": "Tomato", "Rice": "Rice"}

session = requests.Session()
session.headers["User-Agent"] = USER_AGENT


def cached(key: str, fetch, delay: float = 0.0):
    """Return a cached JSON value, calling fetch() (and then sleeping) on a miss."""
    CACHE_DIR.mkdir(parents=True, exist_ok=True)
    path = CACHE_DIR / (hashlib.sha1(key.encode()).hexdigest() + ".json")
    if path.exists():
        return json.loads(path.read_text(encoding="utf-8"))
    value = fetch()
    path.write_text(json.dumps(value), encoding="utf-8")
    time.sleep(delay)
    return value


def psa_table(table: str, crop_variable: str, crops: dict[str, str]) -> pd.DataFrame:
    meta = cached(f"psa-meta-{table}", lambda: session.get(PSA + table, timeout=90).json())
    variables = {v["code"]: v for v in meta["variables"]}
    crop_var = variables[crop_variable]
    geo_var = variables["Geolocation"]
    period_var = variables["Period"]
    year_var = variables["Year"]
    year_codes = [c for c, t in zip(year_var["values"], year_var["valueTexts"]) if t in YEARS]
    crop_codes = [c for c, t in zip(crop_var["values"], crop_var["valueTexts"]) if t in crops]
    # Provinces carry exactly four leading dots; regions two, cities six.
    province_codes = [c for c, t in zip(geo_var["values"], geo_var["valueTexts"])
                      if t.startswith("....") and not t.startswith("......")]
    period_codes = [c for c, t in zip(period_var["values"], period_var["valueTexts"])
                    if t.replace(" ", "") in {"Quarter1", "Quarter2", "Quarter3", "Quarter4", "Annual"}]
    query = {
        "query": [
            {"code": crop_variable, "selection": {"filter": "item", "values": crop_codes}},
            {"code": "Geolocation", "selection": {"filter": "item", "values": province_codes}},
            {"code": "Year", "selection": {"filter": "item", "values": year_codes}},
            {"code": "Period", "selection": {"filter": "item", "values": period_codes}},
        ],
        "response": {"format": "csv"},
    }
    text = cached(f"psa-csv-{table}-{YEARS}-{sorted(crops)}", lambda: session.post(PSA + table, json=query, timeout=180).text)
    wide = pd.read_csv(io.StringIO(text))
    long = wide.melt(id_vars=[crop_variable, "Geolocation"], var_name="yearPeriod", value_name="tonnes")
    rows = []
    for r in long.itertuples(index=False):
        year, period = r.yearPeriod.split(" ", 1)
        rows.append({
            "crop": crops[r[0]],
            # Drop the leading dots and PSA footnote markers such as " a/" or " 1/".
            "province": re.sub(r"\s+[a-z0-9]+/$", "", r[1].strip(". ")),
            "year": year,
            "period": period.replace(" ", ""),
            "tonnes": pd.to_numeric(r[3], errors="coerce"),
        })
    return pd.DataFrame(rows)


def production() -> pd.DataFrame:
    frames = [psa_table("0082E4EVCP3.px", "Crop", VEGETABLES),
              psa_table("0012E4EVCP0.px", "Ecosystem/Croptype", GRAINS)]
    df = pd.concat(frames)
    df["tonnes"] = df["tonnes"].fillna(0.0)
    # Negros Occidental/Oriental and Siquijor appear under two regions after the
    # 2024 Negros Island Region split; keep the larger series for each.
    df = df.groupby(["crop", "province", "year", "period"], as_index=False)["tonnes"].max()
    table = df.pivot_table(index=["province", "crop"], columns="period", values="tonnes", aggfunc="mean")
    return table.fillna(0.0).reset_index()


def geocode(province: str) -> tuple[float, float] | None:
    def fetch():
        response = session.get("https://nominatim.openstreetmap.org/search",
                               params={"state": province, "country": "Philippines", "format": "json", "limit": 1},
                               timeout=30)
        hits = response.json()
        return [float(hits[0]["lat"]), float(hits[0]["lon"])] if hits else None
    point = cached(f"geocode-{province}", fetch, delay=1.1)  # Nominatim policy: 1 request/s
    return tuple(point) if point else None


def nasa_power(lat: float, lon: float) -> dict | None:
    url = ("https://power.larc.nasa.gov/api/temporal/climatology/point?parameters=T2M,RH2M,PRECTOTCORR"
           f"&community=AG&format=JSON&latitude={lat:.3f}&longitude={lon:.3f}")
    body = cached(f"nasa-{lat:.3f},{lon:.3f}", lambda: session.get(url, timeout=90).json())
    try:
        parameter = body["properties"]["parameter"]
        months = ["JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC"]
        return {
            "temperatureC": [parameter["T2M"][m] for m in months],
            "humidityPct": [parameter["RH2M"][m] for m in months],
            "rainMmPerDay": [parameter["PRECTOTCORR"][m] for m in months],
            "cellElevationM": body["geometry"]["coordinates"][2],
        }
    except (KeyError, TypeError):
        return None


def elevations(points: list[tuple[float, float]]) -> list[float]:
    result = []
    for start in range(0, len(points), 100):  # the API takes up to 100 points per call
        chunk = points[start:start + 100]
        lats = ",".join(f"{p[0]:.3f}" for p in chunk)
        lons = ",".join(f"{p[1]:.3f}" for p in chunk)
        body = cached(f"elevation-{lats}-{lons}", lambda: session.get(
            "https://api.open-meteo.com/v1/elevation", params={"latitude": lats, "longitude": lons}, timeout=60).json())
        result.extend(body["elevation"])
    return result


def soilgrids(lat: float, lon: float) -> dict | None:
    url = (f"https://rest.isric.org/soilgrids/v2.0/properties/query?lat={lat:.3f}&lon={lon:.3f}"
           "&property=phh2o&property=clay&property=sand&depth=0-5cm&depth=5-15cm&depth=15-30cm&value=mean")

    def fetch():
        for _ in range(3):
            try:
                response = session.get(url, timeout=90)
                if response.status_code == 200:
                    return response.json()
            except requests.RequestException:
                pass
            time.sleep(15)
        return None
    body = cached(f"soilgrids-{lat:.3f},{lon:.3f}", fetch, delay=12.5)  # fair use: 5 requests/min
    if not body:
        return None
    out = {}
    for layer in body["properties"]["layers"]:
        factor = layer["unit_measure"]["d_factor"]
        weighted = thickness = 0.0
        for depth in layer["depths"]:
            mean = depth["values"]["mean"]
            if mean is None:
                continue
            cm = depth["range"]["bottom_depth"] - depth["range"]["top_depth"]
            weighted += mean / factor * cm
            thickness += cm
        out[layer["name"]] = round(weighted / thickness, 2) if thickness else None
    if out.get("phh2o") is None and out.get("clay") is None:
        return None
    return {"ph": out.get("phh2o"), "clayPct": out.get("clay"), "sandPct": out.get("sand")}


def site_record(lat: float, lon: float, elevation: float) -> dict:
    return {"lat": round(lat, 3), "lon": round(lon, 3), "elevationM": elevation,
            "climate": nasa_power(lat, lon), "soil": soilgrids(lat, lon)}


def main() -> None:
    prod = production()
    provinces = sorted(prod["province"].unique())
    print(f"PSA production: {len(prod)} province-crop rows, {len(provinces)} provinces")

    points = {p: geocode(p) for p in provinces}
    located = [p for p in provinces if points[p]]
    print(f"Geocoded {len(located)}/{len(provinces)} provinces")

    soils = pd.read_excel(SOILSSYNC_PATH).dropna(subset=["crop", "ph_value", "lat", "long"])
    soils = soils[soils["crop"].isin(SOILSSYNC_CROPS)]
    farm_points = [(float(r.lat), float(r.long)) for r in soils.itertuples()]

    all_points = [points[p] for p in located] + farm_points
    heights = elevations(all_points)

    records = {"provinces": [], "farms": []}
    for index, province in enumerate(located):
        lat, lon = points[province]
        site = site_record(lat, lon, heights[index])
        rows = prod[prod["province"] == province]
        site["name"] = province
        site["production"] = {
            r.crop: {"annualTonnes": round(r.Annual, 1),
                     "quarterTonnes": [round(getattr(r, f"Quarter{q}"), 1) for q in range(1, 5)]}
            for r in rows.itertuples()
        }
        records["provinces"].append(site)
        print(f"  {province}: climate={'ok' if site['climate'] else 'missing'} soil={'ok' if site['soil'] else 'missing'}")

    for offset, row in enumerate(soils.itertuples()):
        site = site_record(float(row.lat), float(row.long), heights[len(located) + offset])
        site.update({"id": int(row.item_id), "crop": SOILSSYNC_CROPS[row.crop], "labPh": float(row.ph_value),
                     "province": str(row.province)})
        records["farms"].append(site)

    records["sources"] = {
        "production": "PSA OpenSTAT tables 0082E4EVCP3 and 0012E4EVCP0, mean of 2021-2025",
        "points": "OpenStreetMap Nominatim province label points",
        "climate": "NASA POWER climatology (AG community), 2001-2020",
        "elevation": "Open-Meteo elevation API (Copernicus DEM 90 m)",
        "soil": "ISRIC SoilGrids 2.0, 0-30 cm thickness-weighted",
        "farms": "SoilsSync.xlsx",
    }
    OUT_PATH.parent.mkdir(parents=True, exist_ok=True)
    OUT_PATH.write_text(json.dumps(records, indent=1), encoding="utf-8")
    print(f"Wrote {OUT_PATH.relative_to(PROJECT_ROOT)}: {len(records['provinces'])} provinces, {len(records['farms'])} farms")


if __name__ == "__main__":
    main()
