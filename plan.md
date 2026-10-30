# CropCast Prediction tab: plan

Note 2026-10-30: the **My Crop** tab now exists as the third tab (Home, Crop Recommendation, My Crop, Sensor). Prediction becomes the fifth tab; update `onOpenSensors` indexes in `CropCastApp.kt` when adding it.

Status: **planned, not built.** Date of this plan: 2026-10-05. The earlier monthly-recommendation plan was delivered; it is kept in `docs/plan-monthly-recommendation.md`.

## 1. Goal

A fourth bottom tab, **Prediction**, that answers "what should I plant, and when" for the next 12 months.

1. **Best to plant now**: the winning crop, its score and FAO class, expected income, and the main risk.
2. **Crop x month grid**: the 8 best crops for this farm down the side, the next 12 months across, each cell coloured by suitability if planted in that month.
3. **Crop detail**: tap a crop for its best planting window, the factors behind the score, season risks, and income.
4. **Honest provenance**: a label that says what the numbers rest on (real probe, simulated seed, climate averages, seasonal forecast).

It is a suitability prediction, not a yield forecast. Yield needs real harvest outcomes (see section 9).

### Decided

| Question | Decision |
|---|---|
| Horizon | 12 months, starting at the current month |
| Crops | Top 8 for this farm, with "show all crops" |
| Method | The existing rule-based engine. No new library, no trained model |
| AI | Optional second step; text only; scores never come from the LLM |

### Non-goals

- No ML model or yield prediction until there are 1-2 years of real readings plus outcome records.
- No new data source, no new Firebase paths, no database rules change.
- No change to the existing Crop Recommendation tab.

## 2. What already exists (reuse, do not rebuild)

| Need | Already in code |
|---|---|
| Per-month scores | `CropSuitability.monthlyScores`: score if planted in each month, **January first**, from `FarmSuitabilityEngine.recommend()`. Empty without climate normals. |
| Best window | `CropSuitability.bestPlantingMonths` (months within 5 points of the best). The Why-card already shows "Best months to plant: Jun, Jul". |
| Class colours | `SuitabilityClass.of(score)`: S1 >= 80, S2 >= 60, S3 >= 40, N below. |
| Factors and risks | `CropSuitability.factors`, `.risks`, `.limitingFactor`. |
| Income | `CropSuitability.economics.grossPhpPerHa`; `FarmRecommendation.bestValue` (highest gross among S1/S2 crops; shown as "Best income among suitable crops"). |
| Inputs | `farmRecommendation` is already computed in `CropCastApp.kt` from probe soil, site data (NASA POWER normals, ECMWF seasonal, Open-Meteo, SoilGrids/BSWM), soil type setting, irrigation, current crop. |
| Forecast vs average | `ClimateNormals.forecastMonths`: months replaced by the ECMWF seasonal forecast. |
| Explanations | `FarmAdviceRepository.explain()` via the Cloudflare worker (live). |

Consequence: **the grid is mostly UI over data that is already calculated.** No new engine call per month is needed.

## 3. Data flow

```
CropCastApp.farmRecommendation  (already computed)
        |  ranked: List<CropSuitability>
        v
PredictionModel.build(ranked, currentMonth, forecastMonths, status)   <- new, pure Kotlin, unit-tested
        v
PredictionScreen (grid + top card + detail sheet)
```

`PredictionModel` is a pure function so it can be tested without Android.

## 4. Computation rules

1. **Crop selection.** `ranked.take(8)` by current score. If the user picks "show all", use all crops, still sorted by score.
2. **Month rotation.** `monthlyScores` is January-first. Row cells are rotated so the first column is the current month: `cell[i] = monthlyScores[(currentMonth - 1 + i) % 12]`. The month label also carries the year when the sequence crosses December.
3. **Empty scores.** If `monthlyScores` is empty (no climate normals yet), show one state: "Set the farm location and connect to the internet once to see the planting calendar." No grid.
4. **Cell colour.** From `SuitabilityClass`: S1 green, S2 light green, S3 amber, N red. Also print the number in the cell, so colour is never the only signal.
5. **Best window per crop.** From `bestPlantingMonths`, rotated to start at the current month, grouped into contiguous runs ("Jun-Jul"). If the crop is already in a good window now, say "Plant now".
6. **Best to plant now.** The crop with the highest score for the current month among those with class S1/S2; if none, show the top crop with a warning, not a recommendation. Show the next best month for the top crop when "now" is not its window.
7. **Season length.** Each crop carries `growDays`. Show "Harvest about <month>" = planting month + `growDays`, using `YearMonth` arithmetic, never string maths.
8. **Income.** `grossPhpPerHa` is PSA national yield x farmgate price, before costs. Always label it "before costs, national average". It is shown beside the score, never mixed into the score.
9. **Forecast vs average.** A cell whose month is in `forecastMonths` is marked "seasonal forecast"; the others "climate average". Months beyond the forecast say so in the legend. The forecast reaches about 7 months; later cells use averages.
10. **Soil is constant across months.** The probe soil, soil type and salinity apply to every cell. Only climate (temperature, rain, humidity, disease pressure) moves between months. State this in the screen footer so users do not read the grid as a soil forecast.

## 5. Confidence and provenance label

A single line under the top card, chosen in this order (first match wins):

| Condition | Label |
|---|---|
| Device status says `simulated: true`, or firmware starts with `demo-seed` | "Demo data: soil values are simulated. Do not plant from this." |
| No probe soil (`soilPh == 0`) | "No soil reading yet: crop ranges only." |
| No climate normals loaded | "Climate not loaded." (grid hidden) |
| Probe pH differs from the soil map by more than 1.0 | "Probe pH disagrees with the soil map: check the probe." |
| Otherwise | "Based on your probe, NASA climate averages and the ECMWF seasonal forecast." |

Needs one small change: `DeviceStatus` must expose `simulated` and `firmware` (the repository reads `firmware` already; `simulated` is written by `scripts/seed_demo_history.py`).

## 6. UI spec (phone width 360-412 dp)

```
Prediction
+--------------------------------------------+
| Best to plant now                          |
| Rice (Lowland)   86%  S1 Highly suitable   |
| Plant now. Harvest about Feb.              |
| ~ P85k/ha gross before costs               |
| Main risk: rainfall below 290 mm/month     |
| [Demo data banner / confidence line]       |
+--------------------------------------------+
| Planting calendar  (swipe sideways)        |
|            Oct Nov Dec Jan Feb ... Sep     |
| Rice        86  70  55  48 ...             |
| Cucumber    80  ...                         |
| (8 rows)         [Show all crops]          |
| Legend: S1 S2 S3 N  | forecast | average    |
+--------------------------------------------+
| footer: soil is the same in every month    |
+--------------------------------------------+
```

- The grid is one horizontally scrollable `LazyRow`-style table: crop names pinned in a left column, 12 month columns of 44 dp.
- Tapping a crop row opens a bottom sheet: best window, harvest month, factor list (same `FactorScore` rows as the Why card), risks, income.
- Dark mode uses the app theme; cell colours are the same hues with dark-mode alpha.
- Strings go through `tr()`; add Hiligaynon (and Filipino) entries in `Localization.kt` and `FilipinoStrings.kt`. `FilipinoCoverageTest` will fail on missing keys; add them with the screen.

## 7. Files

New:
- `app/src/main/java/com/cropcast/app/data/PredictionModel.kt`: pure model, rotation, windows, labels.
- `app/src/main/java/com/cropcast/app/ui/screens/PredictionScreen.kt`: top card, grid, detail sheet.
- `app/src/test/java/com/cropcast/app/data/PredictionModelTest.kt`.

Changed:
- `ui/CropCastApp.kt`: fourth tab in the bottom bar, route to `PredictionScreen(farmRecommendation, state, ...)`.
- `data/model/SensorModels.kt` and `data/FirebaseSensorRepository.kt`: `DeviceStatus.simulated`.
- `ui/localization/*`: new strings.

No ViewModel change and no Firebase change: `farmRecommendation` is already in `CropCastApp`.

## 8. Phases

| Phase | Work | Verify |
|---|---|---|
| 1 | `PredictionModel` + tests | `./gradlew.bat --offline :app:testDebugUnitTest` |
| 2 | Tab, top card, grid, detail sheet, strings | Install on phone, check each state with adb screenshots |
| 3 | Provenance label, `DeviceStatus.simulated` | Seeded account shows the demo banner; a real device does not |
| 4 (optional) | "Explain the planting window" via the worker | Worker output checked against the facts sent |

Phase 1-3 are the first version.

## 9. Tests (PredictionModelTest)

- Rotation: January-first scores rotate correctly for every current month, including December and January.
- Colour class boundaries at 80, 60, 40.
- Window grouping: contiguous runs, a run that wraps across December, a single month, no window.
- Empty `monthlyScores` returns the "needs climate" state, not a grid of zeros.
- "Best to plant now" never returns a class N crop as a recommendation.
- Harvest month crosses a year boundary.
- Provenance label priority order.
- Crops with `needKgHa == null` ("pH advice only") still appear.

Acceptance: all tests pass; the tab renders on a real phone in light and dark mode with the seeded account; both Hiligaynon and English show no raw keys; with no location set the screen shows the "set location" state.

## 10. Optional AI step (phase 4)

The worker (`worker/index.js`, deployed at `cropcast-advice.cropcast.workers.dev`) writes plain-language text from facts the app sends. Before using it for Prediction:

1. **Fix the income claim.** A live test returned "ranks highest for suitability and income" for rice while the app's own best income was tomato. Send an explicit fact line, "Highest income among suitable crops: <crop>", and tell the model to quote it and never to rank income itself.
2. **Send only the facts shown on screen** for the selected crop and window.
3. **Never let it change a score.** Text only.
4. Keep the existing disclaimer ("can make mistakes").
5. Add a per-IP rate limit (`LIMITER`) before the app goes public; the URL is public and spends the owner's API credit.

## 11. Risks and limits

| Risk | Handling |
|---|---|
| Seeded data is partly simulated (moisture modelled from rain; soil flat) | Demo banner (section 5). Never present it as real advice. |
| 8 months is not a seasonal cycle | The grid uses climate normals + forecast, not the 8 months. The months only cross-check. |
| Probe pH 7.3 vs soil map 5.7 | Existing warning stays; confidence line mentions it. |
| Salinity (ECe about 3.2 dS/m) dominates scores | Show it as the main risk; it does not vary by month. |
| `monthlyScores` empty offline | Explicit "needs climate once" state. |
| Cucumber and garlic rank high on wet soil | Known engine behaviour; section 4 rule 10 and the soil type setting reduce it. Revisit waterlogging weights separately. |
| Time zone | Months are UTC everywhere in the app; the grid follows the device's current UTC month. |

## 12. Later (not part of version 1)

- Record real outcomes (the app already stores them) and compare predicted vs actual per crop.
- After 1-2 years of real readings, fit local corrections; only then consider a trained model.
- Per-owner Firebase rules (`devices/$id/owner`) and a short-lived PIN that claims a long random device id.
- Real soil photos in the soil type picker.
