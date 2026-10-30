# Graph Report - CropCast-final  (2026-10-31)

## Corpus Check
- 115 files · ~247,291 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 1218 nodes · 2942 edges · 89 communities (63 shown, 20 thin omitted)
- Extraction: 96% EXTRACTED · 4% INFERRED · 0% AMBIGUOUS · INFERRED: 118 edges (avg confidence: 0.84)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `103bac62`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- FarmForecastTest
- RoundedCard
- ClimateNormals
- assertequals
- FirebaseSensorRepository
- Philippine Vegetable Industry Roadmap 2021-2025
- FarmSuitabilityEngine (25-crop ranking)
- CropCastViewModel
- SeedsScreen.kt
- MainActivity.kt
- FarmSiteDataRepository.kt
- simulate_firebase_sensor.py
- SensorReading
- SettingsScreen.kt
- PublicCropModelEngine
- SettingsScreen
- LoginScreen.kt
- SeedRecommendationEngine
- build_validation_data.py
- FarmLocationPicker.kt
- localize
- HomeScreen.kt
- CropCastApp.kt
- SplashOverlay.kt
- CropCastViewModel.kt
- train_local_crop_model.py
- train_android_public_crop_model.py
- seed_crop_test_data.py
- seed_demo_history.py
- functions/index.js
- Sensor tab screen (Current Conditions dashboard)
- Prediction tab (12-month planting outlook)
- AGENTS.md (contributor and agent guide)
- FirebaseSensorRepository.kt
- SensorInfo
- seed_potato_test_data.py
- SeedRecommendationEngine
- CropCast ESP32 firmware README
- json
- build_iloilo_soil.py
- Cloudflare Worker LLM explanation proxy
- Pairing flow (PIN becomes device id)
- PredictionScreen (top card, grid, detail sheet)
- .onCreate
- CropCastApplication.kt
- worker/index.js
- CropCast logo image
- crop_alugbati.png (Alugbati photo)
- portal_logo.h
- gradlew
- FirebaseSensorRepository
- MyCropScreen
- tr
- Farm outcome records (harvest, rating) and conservative adjustment
- FarmSuitabilityEngineTest
- WaterScreen.kt
- FarmSiteData
- FarmSuitabilityEngine
- translateFilipino
- FarmAdviceTest
- MyCropScreen.kt
- Notifier.kt
- .idealFarm
- FarmSuitabilityEngine.kt
- GoogleSignIn.kt
- CropCastApp
- WeatherOutlook
- CropCastMessagingService.kt
- PublicCropModelEngineTest
- SuitabilityClass
- MapEventsReceiver
- PairingCode.kt
- CropCastViewModelFactory
- MonthlyRecommendationRow
- SoilSurveyTest
- AlertSettings
- PairingCodeTest.kt
- RainfallEstimate
- cropcasttheme
- farmsitedata
- monthlysensoraggregator
- publiccropresultstatus
- rainfallstatus

## God Nodes (most connected - your core abstractions)
1. `SensorReading` - 75 edges
2. `tr()` - 70 edges
3. `FarmSuitabilityEngine` - 40 edges
4. `FirebaseSensorRepository` - 40 edges
5. `CropCastViewModel` - 38 edges
6. `RoundedCard()` - 36 edges
7. `FarmSiteData` - 27 edges
8. `SettingsScreen()` - 24 edges
9. `FarmSuitabilityEngineTest` - 24 edges
10. `SeedRecommendationEngine` - 23 edges

## Surprising Connections (you probably didn't know these)
- `Data contract (reading record fields)` --semantically_similar_to--> `Data contract and three gates (server, app, scoring)`  [INFERRED] [semantically similar]
  AGENTS.md → firmware/CropCastESP32/README.md
- `Connection status: Demo mode (no live ESP32)` --semantically_similar_to--> `Demo mode with simulated sensor values`  [INFERRED] [semantically similar]
  cropcast-settings-runtime.png → cropcast-runtime.png
- `Cabbage (20-30 in/yr, uniform throughout growth)` --semantically_similar_to--> `Cabbage (highland, Oct-Dec planting)`  [INFERRED] [semantically similar]
  texas a&m vegetable requirements.pdf → Philippine-Vegetable-Industry-Roadmap-2021-2025.pdf
- `Eggplant (20-35 in/yr, bloom through fruit set)` --semantically_similar_to--> `Eggplant (highest-yielding lowland vegetable)`  [INFERRED] [semantically similar]
  texas a&m vegetable requirements.pdf → Philippine-Vegetable-Industry-Roadmap-2021-2025.pdf
- `Potato (20-40 in/yr, vining, bloom, tuber initiation)` --semantically_similar_to--> `Potato (Sep-Jan planting)`  [INFERRED] [semantically similar]
  texas a&m vegetable requirements.pdf → Philippine-Vegetable-Industry-Roadmap-2021-2025.pdf

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **Device pairing across app PIN, firmware portal, and account link** — agents_pairing_flow, agents_pairing_code, firmware_cropcastesp32_readme_pairing, agents_pairing_security_weakness [EXTRACTED 0.95]
- **Prediction tab data flow from engine to screen** — agents_farm_suitability_engine, plan_farm_recommendation, plan_prediction_model, plan_prediction_screen [EXTRACTED 0.95]
- **Three-way validation of the farm crop ranking** — data_validation_readme_known_answer_farms, data_validation_readme_psa_production_validation, data_validation_readme_real_farms_soilssync, readme_farmsuitabilityengine [EXTRACTED 1.00]
- **Crop requirement and suitability tables** — philippine_vegetable_industry_roadmap_2021_2025_planting_calendar_table_36, texas_a_m_vegetable_requirements_water_use_table_1, texas_a_m_vegetable_requirements_fertilizer_requirements_table_3, texas_a_m_vegetable_requirements_soil_ph_sensitivity_table_6 [INFERRED 0.75]
- **Demo mode shown across sensor and settings screens** — cropcast_runtime_demo_mode, cropcast_runtime_sensor_status_card, cropcast_settings_runtime_demo_connection [INFERRED 0.75]
- **Local 12-crop model training inputs** — models_local_crop_readme_synthesized_training_rows, data_raw_crop_requirements_crop_requirements, data_readme_data_sources_table, requirements_ml_python_dependencies [INFERRED 0.75]
- **Reading data contract enforced across firmware, rules, and app** — agents_data_contract, firmware_cropcastesp32_readme_data_contract, firmware_cropcastesp32_readme_history_gate, docs_plan_monthly_recommendation_missing_data_validation [INFERRED 0.85]
- **Sensor dashboard screen composition** — cropcast_runtime_header_branding, cropcast_runtime_sensor_status_card, cropcast_runtime_current_conditions_grid, cropcast_runtime_bottom_nav [INFERRED 0.85]

## Communities (89 total, 20 thin omitted)

### Community 1 - "RoundedCard"
Cohesion: 0.17
Nodes (21): FarmAdviceRepository, CropSuitability, FarmRecommendation, RoundedCard(), CropScoreRow(), FarmAiAdviceCard(), FarmFactorsCard(), FarmNoticesCard() (+13 more)

### Community 2 - "ClimateNormals"
Cohesion: 0.23
Nodes (5): ClimateNormals, SoilMap, EcocropRange, FactorScore, FarmCrop

### Community 3 - "assertequals"
Cohesion: 0.22
Nodes (10): assertequals, assertfalse, assertnotnull, assertnull, assertsame, asserttrue, file, files (+2 more)

### Community 4 - "FirebaseSensorRepository"
Cohesion: 0.08
Nodes (10): FirebaseSensorRepository, ValueEventListener, ValueEventListener, Flow, T, AccountInfo, DatabaseError, DataSnapshot (+2 more)

### Community 5 - "Philippine Vegetable Industry Roadmap 2021-2025"
Cohesion: 0.10
Nodes (36): Cabbage (highland, Oct-Dec planting), Philippine Vegetable Industry Roadmap 2021-2025, Eggplant (highest-yielding lowland vegetable), Highland chopsuey priority crops (cabbage, Chinese cabbage, carrot, snap beans, potato), DA High Value Crops Development Program, Industry cluster governance network, Lowland pinakbet priority crops (ampalaya, tomato, squash, pole sitao, eggplant, okra), National Food Security Summit May 2021 and DA 18 key strategies (+28 more)

### Community 6 - "FarmSuitabilityEngine (25-crop ranking)"
Cohesion: 0.05
Nodes (50): Data contract (reading record fields), Months are UTC convention, Crop_Requirements.txt (NPK, temperature, humidity, pH, rainfall per crop), BSWM Iloilo/Panay soil survey data, CropCast data layout, Every data source table (NASA POWER, Open-Meteo, SEAS5, SoilGrids, ECOCROP, PSA, WoSIS), Processed outputs (tomato CSV, crop requirements JSON), Scientific limitations (NPK unit, moisture calibration, W/m2 vs lux) (+42 more)

### Community 7 - "CropCastViewModel"
Cohesion: 0.12
Nodes (4): authMessage(), CropCastViewModel, RemoteState, StateFlow

### Community 8 - "SeedsScreen.kt"
Cohesion: 0.17
Nodes (20): SectionTitle(), background, buildconfig, circularprogressindicator, clip, contentscale, Dp, image (+12 more)

### Community 9 - "MainActivity.kt"
Cohesion: 0.15
Nodes (13): activityresultcontracts, animatedvisibility, MainActivity, applicationinfo, ComponentActivity, delay, enableedgetoedge, fadeout (+5 more)

### Community 10 - "FarmSiteDataRepository.kt"
Cohesion: 0.06
Nodes (30): FarmSiteDataRepository, Flow, T, SeasonalForecast, SourceStatus, CACHED, LIVE, UNAVAILABLE (+22 more)

### Community 11 - "simulate_firebase_sensor.py"
Cohesion: 0.21
Nodes (18): ArgumentParser, main(), Seed an INVENTED Alugbati-friendly sensor history to a separate demo device.…, make_reading(), authenticate(), build_parser(), format_reading(), http_json() (+10 more)

### Community 12 - "SensorReading"
Cohesion: 0.21
Nodes (3): MonthlySensorSummary, SensorReading, SeedRecommendationEngineTest

### Community 13 - "SettingsScreen.kt"
Cohesion: 0.10
Nodes (26): ActionValueRow(), EditableRow(), InfoRow(), Color, arrowback, backhandler, border, centeralignedtopappbar (+18 more)

### Community 14 - "PublicCropModelEngine"
Cohesion: 0.11
Nodes (19): DoubleArray, Node, PublicCropConfidence, HIGH, LOW, MODERATE, UNAVAILABLE, PublicCropModelEngine (+11 more)

### Community 15 - "SettingsScreen"
Cohesion: 0.11
Nodes (20): CropIcon(), Modifier, PredictionRow(), ActionRow(), ChoiceDialog(), CropChoiceDialog(), Divider(), EditorRequest (+12 more)

### Community 16 - "LoginScreen.kt"
Cohesion: 0.13
Nodes (14): email, filterchip, imeaction, keyboardactions, keyboardcapitalization, keyboardoptions, keyboardtype, localfocusmanager (+6 more)

### Community 17 - "SeedRecommendationEngine"
Cohesion: 0.20
Nodes (7): CropOutcomeFeedback, MonthlyCropRecommendation, SeedRecommendation, CropProfile, ClosedFloatingPointRange, SeedRecommendationEngine, CropThumbnail()

### Community 18 - "build_validation_data.py"
Cohesion: 0.16
Nodes (23): io, pandas, pathlib, requests, main(), national_annual(), DataFrame, query() (+15 more)

### Community 19 - "FarmLocationPicker.kt"
Cohesion: 0.10
Nodes (20): androidview, box, cancellationtokensource, copyrightoverlay, dialog, dialogproperties, disposableeffect, lifecycle (+12 more)

### Community 20 - "localize"
Cohesion: 0.29
Nodes (3): localize(), LocalizationTest, staticcompositionlocalof

### Community 21 - "HomeScreen.kt"
Cohesion: 0.14
Nodes (24): AlertEvent, AlertsDialog(), HomeMetricCard(), Color, Modifier, NoticeLine(), arrangement, button (+16 more)

### Community 22 - "CropCastApp.kt"
Cohesion: 0.11
Nodes (19): HeaderButton(), autoawesome, collectasstatewithlifecycle, compositionlocalprovider, darkmode, home, icon, ImageVector (+11 more)

### Community 23 - "SplashOverlay.kt"
Cohesion: 0.10
Nodes (20): alpha, animatable, animatefloat, brush, canvas, color, deepgreen, infiniterepeatable (+12 more)

### Community 24 - "CropCastViewModel.kt"
Cohesion: 0.11
Nodes (19): currentMonthKey(), demoMonthlyReadings(), cancellationexception, collect, combine, distinctuntilchanged, dropwhile, first (+11 more)

### Community 25 - "train_local_crop_model.py"
Cohesion: 0.18
Nodes (19): BinaryIO, ndarray, numpy, pyreadr, export_forest(), main(), new_model(), Path (+11 more)

### Community 26 - "train_android_public_crop_model.py"
Cohesion: 0.11
Nodes (22): collections, datetime, hashlib, joblib, main(), new_model(), RandomForestClassifier, Run repeatable five-fold tests for the Android public crop model design. (+14 more)

### Community 27 - "seed_crop_test_data.py"
Cohesion: 0.26
Nodes (13): itertools, build_fixture(), distinctive_values(), fixture_averages(), main(), parse_utc(), profile_score(), datetime (+5 more)

### Community 28 - "seed_demo_history.py"
Cohesion: 0.16
Nodes (20): getpass, os, build(), firebase_call(), main(), parse_soil(), datetime, Seed demo monthly history for a SEPARATE demo device (never the real one). Soil… (+12 more)

### Community 29 - "functions/index.js"
Cohesion: 0.08
Nodes (32): DEFAULTS, evaluate(), isDue(), offlineAlert(), assert, { evaluate, isDue, offlineAlert }, ok, test (+24 more)

### Community 30 - "Sensor tab screen (Current Conditions dashboard)"
Cohesion: 0.21
Nodes (13): Bottom navigation: Home, Pest & Disease, Water, Sensor, Current Conditions 2-column metric card grid with progress bars, Demo mode with simulated sensor values, ESP32 sensor suite (DHT11, NPK, pH, LUX), Design language: green palette, rounded cards, emoji icons, color-coded progress bars, CropCast header: green logo, 'Smart Farming Assistant - Demo', bell and settings buttons, Sensor tab screen (Current Conditions dashboard), Sensor Status card (Demo, last sync, ESP32/DHT11/NPK/pH/LUX checks) (+5 more)

### Community 31 - "Prediction tab (12-month planting outlook)"
Cohesion: 0.20
Nodes (11): Best to plant now card, Crop x month suitability grid, CropSuitability (monthlyScores, bestPlantingMonths, factors, risks, economics), Gross income per ha (before costs, national average), monthlyScores January-first rotation, No ML or yield prediction yet, PredictionModel (pure Kotlin model), Prediction tab (12-month planting outlook) (+3 more)

### Community 32 - "AGENTS.md (contributor and agent guide)"
Cohesion: 0.24
Nodes (10): AGENTS.md (contributor and agent guide), CropCast (Android app + ESP32 sensor), Monthly Crop Recommendation Plan (delivered), Completed-month (observed) recommendation, Confidence labels (High, Moderate, Low, Limited history), Missing-field validation (no silent zeros), MonthlySensorAggregator, Next-month forecast (60/25/15 blend) (+2 more)

### Community 33 - "FirebaseSensorRepository.kt"
Cohesion: 0.12
Nodes (16): DeviceStatus, FieldSample, isValidForRecommendation(), CropScoreBreakdown, MonthlySensorAggregator, assumetrue, await, awaitclose (+8 more)

### Community 34 - "SensorInfo"
Cohesion: 0.20
Nodes (10): SensorInfo, HUMIDITY, LIGHT, NITROGEN, PHOSPHORUS, POTASSIUM, SOIL_MOISTURE, SOIL_PH (+2 more)

### Community 35 - "seed_potato_test_data.py"
Cohesion: 0.30
Nodes (11): math, build_fixture(), main(), parse_utc(), datetime, Path, range_score(), Generate deterministic-looking Firebase test data that should recommend Potato. (+3 more)

### Community 36 - "SeedRecommendationEngine"
Cohesion: 0.25
Nodes (9): FarmSiteDataRepository (NASA POWER, ECMWF, Open-Meteo, SoilGrids), FarmSuitabilityEngine (FAO/ECOCROP rule scorer), SeedRecommendationEngine (older monthly sensor scorer), Light kept display-only until lux calibrated, Range-based seven-field scoring, SeedRecommendationEngine, All-or-nothing history gate, Bringing sensors online one at a time (+1 more)

### Community 37 - "CropCast ESP32 firmware README"
Cohesion: 0.25
Nodes (8): CropCast ESP32 firmware README, Offline backfill from SD to Firebase, Huge APP partition scheme requirement, microSD CSV log, Serial console diagnostics, SN-3002 7-in-1 soil probe Modbus read, Firmware troubleshooting table, Sensor wiring (DHT11, SN-3002, TTL485, BH1750, microSD)

### Community 38 - "json"
Cohesion: 0.18
Nodes (14): argparse, json, random, main(), parse_args(), Namespace, Run a prediction with the trained public crop recommendation model., clean_mobile_tomato() (+6 more)

### Community 39 - "build_iloilo_soil.py"
Cohesion: 0.33
Nodes (6): re, main(), Build app/src/main/assets/iloilo_soil_series.json from the BSWM Philippine Soil…, texture_of(), shapefile, shapely_geometry

### Community 40 - "Cloudflare Worker LLM explanation proxy"
Cohesion: 0.40
Nodes (6): Cloudflare Worker LLM explanation proxy, FarmAdviceRepository (LLM explanation), First run on a new machine checklist, Scores are rule-based and explainable; LLM text only, Secrets stay out of git, Optional AI step (explain planting window)

### Community 41 - "Pairing flow (PIN becomes device id)"
Cohesion: 0.40
Nodes (6): Known problems (offline app, AI income misstatement, pH mismatch), PairingCode (4-digit PIN generator), Pairing flow (PIN becomes device id), Pairing weakness: guessable PIN, any user reads any device, Firmware pairing portal (AP CropCast-XXXX), Fix AI income claim with explicit fact line

### Community 42 - "PredictionScreen (top card, grid, detail sheet)"
Cohesion: 0.33
Nodes (6): seed_demo_history.py (demo history seeding), UI strings via tr() (English, Hiligaynon, Filipino), DeviceStatus.simulated flag, Prediction risks and limits (simulated data, salinity, pH mismatch), PredictionScreen (top card, grid, detail sheet), Confidence and provenance label

### Community 43 - ".onCreate"
Cohesion: 0.22
Nodes (7): LoadingDots(), SplashOverlay(), CropCastTheme(), Bundle, darkcolorscheme, issystemindarktheme, lightcolorscheme

### Community 45 - "worker/index.js"
Cohesion: 0.80
Nodes (4): ask(), fetch(), json(), SYSTEM()

### Community 46 - "CropCast logo image"
Cohesion: 0.67
Nodes (4): CropCast brand (agritech, green and teal palette), CropCast logo image, Line graph nodes and blue concentric arcs (data and forecast), Green seedling sprouting from soil

### Community 47 - "crop_alugbati.png (Alugbati photo)"
Cohesion: 1.00
Nodes (3): crop_alugbati.png (Alugbati photo), Alugbati (Malabar spinach), Climbing vine trained on a stake with glossy heart-shaped leaves

### Community 52 - "MyCropScreen"
Cohesion: 0.10
Nodes (29): CheckLevel, ACT, NONE, OK, TIP, WATCH, CropAction, CropCheck (+21 more)

### Community 53 - "tr"
Cohesion: 0.17
Nodes (22): androidx, InfoSection(), SensorInfoDialog(), CropCastUiState, tr(), DataReadinessCard(), FarmOverviewCard(), HomeScreen() (+14 more)

### Community 61 - "WaterScreen.kt"
Cohesion: 0.15
Nodes (15): alignment, Color, MetricCard(), StatusPill(), AlertHistory(), MoistureBadge(), WaterScreen(), buttondefaults (+7 more)

### Community 62 - "FarmSiteData"
Cohesion: 0.16
Nodes (4): FarmSiteData, FarmAdviceRepositoryTest, FilipinoCoverageTest, SoilTypeTest

### Community 63 - "FarmSuitabilityEngine"
Cohesion: 0.26
Nodes (3): CropEconomics, FarmSuitabilityEngine, SaltTolerance

### Community 64 - "translateFilipino"
Cohesion: 0.21
Nodes (5): f(), Rule, sacks(), translateFilipino(), MyCropAdvisorTest

### Community 66 - "MyCropScreen.kt"
Cohesion: 0.24
Nodes (11): alertdialog, column, cropgreen, datetimeformatter, fontweight, height, rememberscrollstate, sp (+3 more)

### Community 67 - "Notifier.kt"
Cohesion: 0.25
Nodes (7): LocalPreferences, Context, Notifier, build, configuration, intent, manifest

### Community 69 - "FarmSuitabilityEngine.kt"
Cohesion: 0.20
Nodes (9): abs, NutrientAdvice, NutrientStatus, HIGH, LOW, OK, ceil, min (+1 more)

### Community 70 - "GoogleSignIn.kt"
Cohesion: 0.22
Nodes (7): GoogleSignIn, Context, credentialmanager, customcredential, getcredentialrequest, getsigninwithgoogleoption, googleidtokencredential

### Community 71 - "CropCastApp"
Cohesion: 0.24
Nodes (10): PublicCropRecommendationResult, RainfallState, CropCastApp(), CropCastHeader(), NavItem, GoogleBadge(), LoginScreen(), PublicCropModelCard() (+2 more)

### Community 72 - "WeatherOutlook"
Cohesion: 0.22
Nodes (4): WeatherOutlook, HeavyRain, AdviceCasesTest, Case

### Community 73 - "CropCastMessagingService.kt"
Cohesion: 0.32
Nodes (5): CropCastMessagingService, firebaseauth, firebasedatabase, FirebaseMessagingService, RemoteMessage

### Community 75 - "SuitabilityClass"
Cohesion: 0.33
Nodes (5): SuitabilityClass, N, S1, S2, S3

### Community 76 - "MapEventsReceiver"
Cohesion: 0.70
Nodes (3): FarmLocationPickerDialog(), MapEventsReceiver, GeoPoint

### Community 78 - "CropCastViewModelFactory"
Cohesion: 0.50
Nodes (3): CropCastViewModelFactory, T, Factory

### Community 79 - "MonthlyRecommendationRow"
Cohesion: 0.50
Nodes (4): Color, MonthlyRecommendationRow(), scoreColor(), SeedCard()

## Knowledge Gaps
- **100 isolated node(s):** `LIVE`, `CACHED`, `UNAVAILABLE`, `S1`, `S2` (+95 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 332 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **20 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `SensorReading` connect `SensorReading` to `FarmForecastTest`, `ClimateNormals`, `assertequals`, `FirebaseSensorRepository`, `FarmSiteDataRepository.kt`, `SettingsScreen.kt`, `PublicCropModelEngine`, `SeedRecommendationEngine`, `HomeScreen.kt`, `CropCastViewModel.kt`, `FirebaseSensorRepository.kt`, `SensorInfo`, `MyCropScreen`, `tr`, `FarmSuitabilityEngineTest`, `FarmSiteData`, `FarmSuitabilityEngine`, `translateFilipino`, `FarmAdviceTest`, `MyCropScreen.kt`, `.idealFarm`, `FarmSuitabilityEngine.kt`, `WeatherOutlook`, `PublicCropModelEngineTest`?**
  _High betweenness centrality (0.231) - this node is a cross-community bridge._
- **Why does `FarmSuitabilityEngineTest` connect `FarmSuitabilityEngineTest` to `FarmSiteDataRepository.kt`, `assertequals`, `SensorReading`, `FarmSiteData`?**
  _High betweenness centrality (0.039) - this node is a cross-community bridge._
- **Why does `FarmSuitabilityEngine` connect `FarmSuitabilityEngine` to `ClimateNormals`, `FarmSuitabilityEngine.kt`, `WeatherOutlook`, `SettingsScreen.kt`, `SeedRecommendationEngine`, `CropCastApp.kt`, `FarmSiteData`?**
  _High betweenness centrality (0.036) - this node is a cross-community bridge._
- **What connects `LIVE`, `CACHED`, `UNAVAILABLE` to the rest of the system?**
  _100 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `FirebaseSensorRepository` be split into smaller, more focused modules?**
  _Cohesion score 0.07755102040816327 - nodes in this community are weakly interconnected._
- **Should `Philippine Vegetable Industry Roadmap 2021-2025` be split into smaller, more focused modules?**
  _Cohesion score 0.09523809523809523 - nodes in this community are weakly interconnected._
- **Should `FarmSuitabilityEngine (25-crop ranking)` be split into smaller, more focused modules?**
  _Cohesion score 0.053061224489795916 - nodes in this community are weakly interconnected._