"""Write reproducible random sensor scenarios (inputs only) for blind LLM-vs-app crop comparison."""
import argparse
import json
import random
from pathlib import Path

# Same units as firebase/sample-data.json; spans cover the public 22-crop dataset.
SPANS = {
    "nitrogen": (0, 140, 0),
    "phosphorus": (5, 145, 0),
    "potassium": (5, 205, 0),
    "soilPh": (4.5, 8.0, 1),
    "temperature": (18, 36, 1),
    "humidity": (40, 95, 0),
    "soilMoisture": (15, 90, 0),
    "lightIntensity": (200, 900, -1),
}

p = argparse.ArgumentParser()
p.add_argument("--seed", type=int, default=2026)
p.add_argument("--count", type=int, default=30)
p.add_argument("--output", type=Path, default=Path(__file__).resolve().parents[1] / "data/validation/llm_compare_cases.json")
a = p.parse_args()

rng = random.Random(a.seed)
cases = []
for i in range(1, a.count + 1):
    r = {}
    for k, (lo, hi, nd) in SPANS.items():
        v = rng.uniform(lo, hi)
        r[k] = round(v, -1) if nd < 0 else round(v, nd) if nd else int(round(v))
    cases.append({"id": f"case-{i:02d}", "reading": r})

out = {
    "seed": a.seed,
    "units": {"nitrogen/phosphorus/potassium": "mg/kg", "temperature": "C", "humidity": "%", "soilMoisture": "%", "lightIntensity": "lux", "soilPh": "pH"},
    "location": "Iloilo, Philippines (tropical lowland)",
    "cases": cases,
}
a.output.parent.mkdir(parents=True, exist_ok=True)
a.output.write_text(json.dumps(out, indent=2) + "\n", encoding="utf-8")
assert len(cases) == a.count
print(f"{a.count} cases -> {a.output}")
