# Local 12-crop model

A Random Forest for CropCast's 12 crops (Tomato, Okra, Alugbati, Potato, Rice,
Corn, Eggplant, Cucumber, Cabbage, Sweet Potato, Lettuce, Spinach). It uses the
same six inputs and the same `.bin` format as the public model: N, P, K,
temperature, humidity and pH.

No labeled Philippine dataset exists for these crops, so the training rows are
synthesized per crop:

| Input | Source |
|---|---|
| Temperature, pH ranges | FAO ECOCROP optimal ranges (`data/raw/ecocrop/ecocrop.rds`, CC BY 4.0) |
| pH values | Real Philippine topsoil, 0–30 cm, from ISRIC WoSIS (`data/raw/wosis/`) |
| Humidity, N, P, K | `data/processed/crop_requirements_selected.json` |

`SoilsSync.xlsx` (real Philippine soil tests) is used only as a pH check.

Train (uses a local `.venv`):

```bash
uv venv .venv --python 3.12
uv pip install --python .venv -r requirements-ml.txt
.venv/Scripts/python scripts/train_local_crop_model.py
```

Results are in `metrics.json`. Cross-validation accuracy measures how well the
forest separates the published ranges, not accuracy in the field.

**Before this model goes into the app:** the requirement-table NPK ranges
(N 53–223) sit far above what the 7-in-1 probe reads in real soil (for example
N 16, P 24, K 55 mg/kg), so real readings fall outside the training range. After
a laboratory soil test from the same spot as a probe reading, retrain with
`--npk-scale <ratio>`.
