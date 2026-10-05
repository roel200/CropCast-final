"""Build app/src/main/assets/iloilo_soil_series.json from the BSWM Philippine Soil Series map.

Keeps the soil polygons that fall in the Iloilo / Panay window, simplifies them, and
attaches:
- texture (clay %, sand %): read from the soil name ("Alimodian clay loam"), as the
  centre of that USDA texture class
- pH: Table 15 of the Iloilo soil survey report, transcribed in
  data/processed/bswm/iloilo_soil_survey.json

The app uses these only where ISRIC SoilGrids has no value for a field.

Run:  python scripts/build_iloilo_soil.py     (needs pyshp and shapely)
"""

from __future__ import annotations

import json
import re
from pathlib import Path

import shapefile
from shapely.geometry import box, shape

ROOT = Path(__file__).resolve().parents[1]
SHAPEFILE = ROOT / "data/raw/bswm/PhilSoilSeries"
SURVEY = ROOT / "data/processed/bswm/iloilo_soil_survey.json"
OUT = ROOT / "app/src/main/assets/iloilo_soil_series.json"

# lon_min, lat_min, lon_max, lat_max: Iloilo plus the rest of Panay and Guimaras.
# ponytail: a window, not the provincial border, so a soil series with a surveyed pH
# is also given that pH in neighbouring provinces. Clip to a real boundary if that matters.
WINDOW = box(121.85, 10.30, 123.40, 11.90)
SIMPLIFY_DEG = 0.002  # about 200 m, far finer than the survey itself

# ponytail: the BSWM map is on the Luzon 1911 datum; the ~150 m shift to WGS84 is ignored.

# USDA texture class -> (clay %, sand %) at the class centre.
TEXTURES = {
    "clay": (60, 20), "silty clay": (47, 6), "silty clay loam": (34, 10), "clay loam": (34, 32),
    "sandy clay": (42, 52), "sandy clay loam": (27, 58), "loam": (19, 41), "silt loam": (13, 22),
    "silt": (6, 7), "sandy loam": (10, 65), "fine sandy loam": (10, 62), "loamy sand": (6, 82),
    "sand": (4, 90),
}
PHASE_WORDS = re.compile(r"\b(stony|rocky|gravelly|steep|rolling|lowland|deep|shallow|eroded|phase)\b")

# Table 15 name -> map name, where they differ. The report's "rolling phase" is not the
# map's "lowland phase", so it only maps to the plain series.
ALIASES = {"Barotac loam rolling phase": "Barotac loam", "Coastal beach sand": "Beach sand"}


def texture_of(desc: str, series: str) -> str | None:
    if "complex" in desc or "undifferentiated" in desc or " - " in desc:
        return None
    rest = desc[len(series):] if series and desc.startswith(series) else desc
    rest = re.sub(r"\(.*?\)", " ", rest)
    rest = " ".join(PHASE_WORDS.sub(" ", rest.lower()).split())
    return rest if rest in TEXTURES else None


def main() -> None:
    survey = json.loads(SURVEY.read_text(encoding="utf-8"))["soils"]
    ph_by_map_name = {ALIASES.get(name, name): row["ph"] for name, row in survey.items()}

    reader = shapefile.Reader(str(SHAPEFILE), encoding="latin-1")
    soils, matched = [], set()
    for sr in reader.shapeRecords():
        desc = sr.record["SOILDESC"]
        geometry = shape(sr.shape.__geo_interface__).intersection(WINDOW)
        if geometry.is_empty:
            continue
        texture = texture_of(desc, sr.record["SoilSeries"])
        ph = ph_by_map_name.get(desc)
        if texture is None and ph is None:
            continue  # nothing to offer the app
        geometry = geometry.simplify(SIMPLIFY_DEG, preserve_topology=True)
        polygons = [geometry] if geometry.geom_type == "Polygon" else [g for g in geometry.geoms if g.geom_type == "Polygon"]
        rings = []
        for polygon in polygons:
            for ring in [polygon.exterior, *polygon.interiors]:
                rings.append([round(v, 4) for xy in ring.coords for v in xy])
        if not rings:
            continue
        entry = {"name": desc, "rings": rings}
        if texture:
            entry["clay"], entry["sand"] = TEXTURES[texture]
        if ph is not None:
            entry["ph"] = ph
            matched.add(desc)
        soils.append(entry)

    OUT.write_text(json.dumps({"source": "BSWM Philippine Soil Series; pH from Iloilo soil survey Table 15", "soils": soils},
                              separators=(",", ":")), encoding="utf-8")
    print(f"{len(soils)} soil areas, {OUT.stat().st_size / 1024:.0f} KB -> {OUT.relative_to(ROOT)}")
    for name in survey:
        if ALIASES.get(name, name) not in matched:
            print(f"  no map polygon in window for Table 15 soil: {name}")


if __name__ == "__main__":
    main()
