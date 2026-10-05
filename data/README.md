# CropCast data layout

`raw/` contains byte-for-byte copies of the downloaded source files. Do not edit
these files. `processed/` contains reproducible outputs created by
`scripts/prepare_cropcast_data.py`.

## Processed outputs

- `mobile_tomato_complete.csv` keeps the 693 rows in the mobile/manual tomato
  source where all 7-in-1, radiation, air-temperature, and air-humidity sensor
  fields are present. Headers are normalized and a non-destructive
  `quality_flag` identifies zero sensor values and extremely low air humidity.
- `crop_requirements_selected.json` extracts the 12 crops supported by CropCast,
  including Basella alba (displayed as Alugbati), from the downloaded
  crop-requirements file.

## Other data

- `raw/ecocrop/ecocrop.rds`: FAO ECOCROP crop parameters, via the Recocrop R package
  (CC BY 4.0). The 17 crops the app ranks are exported to
  `processed/ecocrop_selected.csv` by `scripts/train_local_crop_model.py`.
- `raw/wosis/`: ISRIC WoSIS topsoil pH for the Philippines, used to train the local model.
- `validation/`: ground truth and site data for the engine's validation tests; see
  `validation/README.md`.
- `app/src/main/assets/crop_economics.json` (in the app): PSA national yields and
  farmgate prices, built by `scripts/build_crop_economics.py`.

## BSWM soil survey (Iloilo / Panay)

- `raw/bswm/PhilSoilSeries.*`: BSWM Philippine Soil Series shapefile (539 polygons; fields
  `SOILDESC`, `AEZ`, `SoilSeries`). Licence not stated on the BSWM download page.
- `raw/bswm/Iloilo_soil_survey_report.pdf`: scanned Iloilo soil survey report (91 pages, no text layer).
- `processed/bswm/iloilo_soil_survey.json`: Table 15 (p. 75) transcribed by hand: pH, N, P, K, Ca
  per soil type. Old rapid field tests, pH to the nearest 0.5. Tables 13 (area, land use) and
  14 (yield indices) are not transcribed.
- `app/src/main/assets/iloilo_soil_series.json`: built by `scripts/build_iloilo_soil.py` (needs
  `pyshp`, `shapely`). Panay-window polygons with texture class and pH. The app uses it only
  where SoilGrids has no value (`SoilSurvey.kt`).

## Every data source the app and tests use

| Source | Used for | Where |
|---|---|---|
| NASA POWER climatology | monthly temperature, humidity, rain | live API, `FarmSiteDataRepository` |
| Open-Meteo forecast | 16-day weather, elevation | live API |
| ECMWF SEAS5 via Open-Meteo | seasonal rain and temperature | live API |
| ISRIC SoilGrids | topsoil pH, clay, sand, nitrogen | live API |
| BSWM soil series + Iloilo survey | texture and pH fallback on Panay | `app/src/main/assets/iloilo_soil_series.json` |
| FAO ECOCROP | crop requirement ranges | `raw/ecocrop/`, `processed/ecocrop_selected.csv` |
| PSA OpenSTAT | yields, farmgate prices, validation | `app/src/main/assets/crop_economics.json`, `validation/sites.json` |
| ISRIC WoSIS | topsoil pH for the local model | `raw/wosis/` |

## Scientific limitations

- The crop-requirements source does not state the NPK unit. Its numeric ranges
  must not be claimed as calibrated `mg/kg` thresholds until they are checked
  against the RS485 probe and a laboratory soil test.
- Soil-moisture percentages depend on sensor and soil calibration. The ranges in
  the processed crop table are explicit prototype defaults and must be replaced
  after local calibration trials.
- Solar radiation (`W/m2`) and illuminance (`lux`) are retained as different
  variables and are never directly merged or converted by this pipeline.
- The source datasets do not provide labeled outcomes for all 12 supported crops
  under one common schema that includes soil moisture. They are supporting
  references, not a valid 12-class machine-learning training set.

## Rebuild

Run with the bundled or project Python environment containing pandas:

```powershell
python scripts/prepare_cropcast_data.py
```
