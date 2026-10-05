# Graph Report - CropCast-final  (2026-10-05)

## Corpus Check
- 65 files · ~238,842 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 1197 nodes · 3053 edges · 61 communities (38 shown, 23 thin omitted)
- Extraction: 97% EXTRACTED · 3% INFERRED · 0% AMBIGUOUS · INFERRED: 93 edges (avg confidence: 0.82)
- Token cost: 0 input · 0 output

## Community Hubs (Navigation)
- Farm Engine Tests
- Recommendation Screen UI
- Farm Suitability Engine
- Soil Survey & Advice Tests
- Firebase Sensor Repository
- Reference Research Docs
- Engine Design & Data Contract
- ViewModel & UI State
- Dashboard Screens
- App Entry & Sign-in
- Weather & Validation Tests
- Simulation & AI Test Scripts
- Sensor Models & Seed Tests
- Settings & Water Screens
- Public Crop Model Engine
- Settings & Location Picker
- Login & Info Dialogs
- Seed Recommendation Engine
- Validation & Economics Scripts
- Filipino Localization
- ViewModel Demo Data
- Local Model Training
- Public Crop Model Training
- Crop Test Data Seeding
- Demo History Seeding
- Android Model Export
- Sensor Screen Plan
- Prediction Tab Plan
- Monthly Recommendation Plan
- Firebase Repo Internals
- Sensor Info Dialog
- Potato Test Seeding
- History Gate & Site Data
- ESP32 Firmware Docs
- Dataset Preparation
- Iloilo Soil Builder
- AI Worker & Agent Setup
- Pairing Flow
- Prediction UI & Provenance
- App Theme
- Application Class
- Cloudflare Worker Code
- Logo & Branding
- Alugbati Icon
- Sensor Repository Node
- Outcome Records

## God Nodes (most connected - your core abstractions)
1. `SensorReading` - 73 edges
2. `tr()` - 60 edges
3. `CropCastViewModel` - 43 edges
4. `FirebaseSensorRepository` - 42 edges
5. `FarmSuitabilityEngine` - 40 edges
6. `RoundedCard()` - 31 edges
7. `CropCastUiState` - 29 edges
8. `SettingsScreen()` - 26 edges
9. `MonthlySensorSummary` - 24 edges
10. `FarmSuitabilityEngineTest` - 24 edges

## Surprising Connections (you probably didn't know these)
- `Data contract (reading record fields)` --semantically_similar_to--> `Data contract and three gates (server, app, scoring)`  [INFERRED] [semantically similar]
  AGENTS.md → firmware/CropCastESP32/README.md
- `Chemical characteristics and fertility of Iloilo soils` --semantically_similar_to--> `Fertilizer requirements of selected vegetable crops (Table 3)`  [INFERRED] [semantically similar]
  data/raw/bswm/Iloilo_soil_survey_report.pdf → texas a&m vegetable requirements.pdf
- `Iloilo climate types and monthly rainfall (Table 2, 1903-1918)` --semantically_similar_to--> `Planting calendar and maturity of vegetable crops (Table 36)`  [INFERRED] [semantically similar]
  data/raw/bswm/Iloilo_soil_survey_report.pdf → Philippine-Vegetable-Industry-Roadmap-2021-2025.pdf
- `Productivity ratings of Iloilo soils (Table 11)` --semantically_similar_to--> `Crop sensitivity to soil pH (Table 6)`  [INFERRED] [semantically similar]
  data/raw/bswm/Iloilo_soil_survey_report.pdf → texas a&m vegetable requirements.pdf
- `Scores are not verified probabilities of crop success` --semantically_similar_to--> `Experimental on-device Random Forest (public 22-crop dataset)`  [INFERRED] [semantically similar]
  models/public_crop/README.md → README.md

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **Three-way validation of the farm crop ranking** — data_validation_readme_known_answer_farms, data_validation_readme_psa_production_validation, data_validation_readme_real_farms_soilssync, readme_farmsuitabilityengine [EXTRACTED 1.00]
- **Iloilo soil series feeding productivity ratings** — data_raw_bswm_iloilo_soil_survey_report_alimodian_series, data_raw_bswm_iloilo_soil_survey_report_barotac_series, data_raw_bswm_iloilo_soil_survey_report_bantog_series, data_raw_bswm_iloilo_soil_survey_report_sta_rita_clay, data_raw_bswm_iloilo_soil_survey_report_productivity_ratings_table_11 [EXTRACTED 1.00]
- **Crop requirement and suitability tables** — philippine_vegetable_industry_roadmap_2021_2025_planting_calendar_table_36, texas_a_m_vegetable_requirements_water_use_table_1, texas_a_m_vegetable_requirements_fertilizer_requirements_table_3, texas_a_m_vegetable_requirements_soil_ph_sensitivity_table_6 [INFERRED 0.75]
- **Demo mode shown across sensor and settings screens** — cropcast_runtime_demo_mode, cropcast_runtime_sensor_status_card, cropcast_settings_runtime_demo_connection [INFERRED 0.75]
- **Local 12-crop model training inputs** — models_local_crop_readme_synthesized_training_rows, data_raw_crop_requirements_crop_requirements, data_readme_data_sources_table, requirements_ml_python_dependencies [INFERRED 0.75]
- **Sensor dashboard screen composition** — cropcast_runtime_header_branding, cropcast_runtime_sensor_status_card, cropcast_runtime_current_conditions_grid, cropcast_runtime_bottom_nav [INFERRED 0.85]
- **Reading data contract enforced across firmware, rules, and app** — agents_data_contract, firmware_cropcastesp32_readme_data_contract, firmware_cropcastesp32_readme_history_gate, docs_plan_monthly_recommendation_missing_data_validation [INFERRED 0.85]
- **Device pairing across app PIN, firmware portal, and account link** — agents_pairing_flow, agents_pairing_code, firmware_cropcastesp32_readme_pairing, agents_pairing_security_weakness [EXTRACTED 0.95]
- **Prediction tab data flow from engine to screen** — agents_farm_suitability_engine, plan_farm_recommendation, plan_prediction_model, plan_prediction_screen [EXTRACTED 0.95]

## Communities (61 total, 23 thin omitted)

### Community 0 - "Farm Engine Tests"
Cohesion: 0.06
Nodes (15): ClimateNormals, FarmSiteData, FarmSiteDataRepository, SeasonalForecast, SeasonalMonth, SoilMap, SourceStatus, CACHED (+7 more)

### Community 1 - "Recommendation Screen UI"
Cohesion: 0.06
Nodes (53): FarmAdviceRepository, CropSuitability, FarmRecommendation, RoundedCard(), InfoSection(), SensorInfoDialog(), CropCastApp(), CropCastHeader() (+45 more)

### Community 2 - "Farm Suitability Engine"
Cohesion: 0.07
Nodes (17): EcocropRange, FactorScore, FarmCrop, FarmSuitabilityEngine, HeavyRain, NutrientAdvice, NutrientStatus, HIGH (+9 more)

### Community 3 - "Soil Survey & Advice Tests"
Cohesion: 0.06
Nodes (9): CropEconomics, Area, SoilSurvey, AdviceCasesTest, Case, FarmAdviceRepositoryTest, PublicCropModelEngineTest, SoilSurveyTest (+1 more)

### Community 4 - "Firebase Sensor Repository"
Cohesion: 0.06
Nodes (6): FirebaseSensorRepository, ValueEventListener, ValueEventListener, AccountInfo, PairingCode, PairingCodeTest

### Community 5 - "Reference Research Docs"
Cohesion: 0.06
Nodes (57): Agriculture, crops and fruit trees (rice, corn, coconut, abaca, banana, home-garden vegetables), Alimodian series (largest series in the province), Bantog series (clay loam, suited to lowland rice), Barotac series (loam, clay loam), Chemical characteristics and fertility of Iloilo soils, Iloilo climate types and monthly rainfall (Table 2, 1903-1918), Coastal beach sand, Iloilo Province Soil Survey Report (scanned typescript) (+49 more)

### Community 6 - "Engine Design & Data Contract"
Cohesion: 0.05
Nodes (43): Data contract (reading record fields), Crop_Requirements.txt (NPK, temperature, humidity, pH, rainfall per crop), BSWM Iloilo/Panay soil survey data, CropCast data layout, Every data source table (NASA POWER, Open-Meteo, SEAS5, SoilGrids, ECOCROP, PSA, WoSIS), Processed outputs (tomato CSV, crop requirements JSON), build_validation_data.py rebuild script, Harvest season correlation with PSA quarterly harvests (+35 more)

### Community 7 - "ViewModel & UI State"
Cohesion: 0.09
Nodes (7): AlertEvent, AlertSettings, DeviceStatus, authMessage(), CropCastUiState, CropCastViewModel, RemoteState

### Community 8 - "Dashboard Screens"
Cohesion: 0.14
Nodes (4): CropIcon(), MetricCard(), SectionTitle(), StatusPill()

### Community 9 - "App Entry & Sign-in"
Cohesion: 0.07
Nodes (6): GoogleSignIn, LocalPreferences, MainActivity, CropCastViewModelFactory, LoadingDots(), SplashOverlay()

### Community 10 - "Weather & Validation Tests"
Cohesion: 0.10
Nodes (9): RainfallEstimate, RainfallState, RainfallStatus, AVAILABLE, LOADING, NOT_CONFIGURED, UNAVAILABLE, WeatherRainfallRepository (+1 more)

### Community 11 - "Simulation & AI Test Scripts"
Cohesion: 0.13
Nodes (13): main(), authenticate(), build_parser(), firebase_url(), format_reading(), http_json(), load_firebase_config(), main() (+5 more)

### Community 12 - "Sensor Models & Seed Tests"
Cohesion: 0.22
Nodes (5): FieldSample, MonthlySensorSummary, SensorReading, MonthlySensorAggregator, SeedRecommendationEngineTest

### Community 13 - "Settings & Water Screens"
Cohesion: 0.10
Nodes (3): AlertHistory(), MoistureBadge(), WaterScreen()

### Community 14 - "Public Crop Model Engine"
Cohesion: 0.11
Nodes (16): Node, PublicCropConfidence, HIGH, LOW, MODERATE, UNAVAILABLE, PublicCropModelEngine, PublicCropPrediction (+8 more)

### Community 15 - "Settings & Location Picker"
Cohesion: 0.12
Nodes (21): FarmLocationPickerDialog(), MapEventsReceiver, ActionRow(), ActionValueRow(), ChoiceDialog(), CropChoiceDialog(), Divider(), EditableRow() (+13 more)

### Community 17 - "Seed Recommendation Engine"
Cohesion: 0.19
Nodes (6): CropOutcomeFeedback, MonthlyCropRecommendation, SeedRecommendation, CropProfile, CropScoreBreakdown, SeedRecommendationEngine

### Community 18 - "Validation & Economics Scripts"
Cohesion: 0.17
Nodes (13): main(), national_annual(), query(), cached(), elevations(), geocode(), fetch(), main() (+5 more)

### Community 20 - "Filipino Localization"
Cohesion: 0.14
Nodes (7): f(), Rule, sacks(), translateFilipino(), localize(), FilipinoCoverageTest, LocalizationTest

### Community 25 - "Local Model Training"
Cohesion: 0.21
Nodes (8): main(), new_model(), load_ecocrop(), load_wosis_topsoil_ph(), main(), soilssync_ph_check(), spread(), synthesize()

### Community 26 - "Public Crop Model Training"
Cohesion: 0.13
Nodes (5): main(), parse_args(), load_dataset(), main(), parse_args()

### Community 27 - "Crop Test Data Seeding"
Cohesion: 0.25
Nodes (10): build_fixture(), distinctive_values(), fixture_averages(), main(), make_reading(), parse_utc(), profile_score(), range_score() (+2 more)

### Community 28 - "Demo History Seeding"
Cohesion: 0.29
Nodes (8): build(), fetch(), firebase_call(), main(), parse_soil(), real_soil(), summary(), weather()

### Community 29 - "Android Model Export"
Cohesion: 0.28
Nodes (5): export_forest(), main(), new_model(), write_int(), write_string()

### Community 30 - "Sensor Screen Plan"
Cohesion: 0.21
Nodes (13): Bottom navigation: Home, Pest & Disease, Water, Sensor, Current Conditions 2-column metric card grid with progress bars, Demo mode with simulated sensor values, ESP32 sensor suite (DHT11, NPK, pH, LUX), Design language: green palette, rounded cards, emoji icons, color-coded progress bars, CropCast header: green logo, 'Smart Farming Assistant - Demo', bell and settings buttons, Sensor tab screen (Current Conditions dashboard), Sensor Status card (Demo, last sync, ESP32/DHT11/NPK/pH/LUX checks) (+5 more)

### Community 31 - "Prediction Tab Plan"
Cohesion: 0.20
Nodes (9): Best to plant now card, Crop x month suitability grid, CropSuitability (monthlyScores, bestPlantingMonths, factors, risks, economics), Gross income per ha (before costs, national average), monthlyScores January-first rotation, PredictionModel (pure Kotlin model), Prediction tab (12-month planting outlook), PredictionModelTest (+1 more)

### Community 32 - "Monthly Recommendation Plan"
Cohesion: 0.24
Nodes (9): AGENTS.md (contributor and agent guide), CropCast (Android app + ESP32 sensor), Monthly Crop Recommendation Plan (delivered), Completed-month (observed) recommendation, Confidence labels (High, Moderate, Low, Limited history), MonthlySensorAggregator, Next-month forecast (60/25/15 blend), SeedsScreen (crop recommendation UI) (+1 more)

### Community 34 - "Sensor Info Dialog"
Cohesion: 0.20
Nodes (10): SensorInfo, HUMIDITY, LIGHT, NITROGEN, PHOSPHORUS, POTASSIUM, SOIL_MOISTURE, SOIL_PH (+2 more)

### Community 35 - "Potato Test Seeding"
Cohesion: 0.38
Nodes (6): build_fixture(), main(), parse_utc(), range_score(), reading(), verify_recommendation()

### Community 36 - "History Gate & Site Data"
Cohesion: 0.25
Nodes (7): FarmSiteDataRepository (NASA POWER, ECMWF, Open-Meteo, SoilGrids), FarmSuitabilityEngine (FAO/ECOCROP rule scorer), SeedRecommendationEngine (older monthly sensor scorer), Range-based seven-field scoring, SeedRecommendationEngine, Bringing sensors online one at a time, farmRecommendation (computed in CropCastApp.kt)

### Community 37 - "ESP32 Firmware Docs"
Cohesion: 0.25
Nodes (7): CropCast ESP32 firmware README, Offline backfill from SD to Firebase, microSD CSV log, Serial console diagnostics, SN-3002 7-in-1 soil probe Modbus read, Firmware troubleshooting table, Sensor wiring (DHT11, SN-3002, TTL485, BH1750, microSD)

### Community 38 - "Dataset Preparation"
Cohesion: 0.39
Nodes (4): clean_mobile_tomato(), main(), _parse_requirement(), select_crop_requirements()

### Community 40 - "AI Worker & Agent Setup"
Cohesion: 0.40
Nodes (4): Cloudflare Worker LLM explanation proxy, FarmAdviceRepository (LLM explanation), First run on a new machine checklist, Optional AI step (explain planting window)

### Community 41 - "Pairing Flow"
Cohesion: 0.40
Nodes (4): Known problems (offline app, AI income misstatement, pH mismatch), PairingCode (4-digit PIN generator), Pairing flow (PIN becomes device id), Firmware pairing portal (AP CropCast-XXXX)

### Community 42 - "Prediction UI & Provenance"
Cohesion: 0.33
Nodes (6): seed_demo_history.py (demo history seeding), UI strings via tr() (English, Hiligaynon, Filipino), DeviceStatus.simulated flag, Prediction risks and limits (simulated data, salinity, pH mismatch), PredictionScreen (top card, grid, detail sheet), Confidence and provenance label

### Community 45 - "Cloudflare Worker Code"
Cohesion: 0.80
Nodes (4): ask(), fetch(), json(), SYSTEM()

### Community 46 - "Logo & Branding"
Cohesion: 0.67
Nodes (4): CropCast brand (agritech, green and teal palette), CropCast logo image, Line graph nodes and blue concentric arcs (data and forecast), Green seedling sprouting from soil

### Community 47 - "Alugbati Icon"
Cohesion: 1.00
Nodes (3): crop_alugbati.png (Alugbati photo), Alugbati (Malabar spinach), Climbing vine trained on a stake with glossy heart-shaped leaves

## Knowledge Gaps
- **75 isolated node(s):** `HIGH`, `LOW`, `MODERATE`, `UNAVAILABLE`, `INVALID_INPUT` (+70 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 292 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **23 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `SensorReading` connect `Sensor Models & Seed Tests` to `Farm Engine Tests`, `Firebase Repo Internals`, `Farm Suitability Engine`, `Recommendation Screen UI`, `Firebase Sensor Repository`, `Sensor Info Dialog`, `Soil Survey & Advice Tests`, `ViewModel & UI State`, `Weather & Validation Tests`, `Public Crop Model Engine`, `Seed Recommendation Engine`, `Filipino Localization`, `Sensor Dashboard & Month Detail`, `ViewModel Demo Data`?**
  _High betweenness centrality (0.218) - this node is a cross-community bridge._
- **What connects `HIGH`, `LOW`, `MODERATE` to the rest of the system?**
  _75 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Farm Engine Tests` be split into smaller, more focused modules?**
  _Cohesion score 0.06229797237731413 - nodes in this community are weakly interconnected._
- **Why does `CropCastViewModel` connect `ViewModel & UI State` to `Recommendation Screen UI`, `Farm Suitability Engine`, `Firebase Sensor Repository`, `App Entry & Sign-in`, `ViewModel Demo Data`?**
  _High betweenness centrality (0.046) - this node is a cross-community bridge._
- **Should `Recommendation Screen UI` be split into smaller, more focused modules?**
  _Cohesion score 0.0645045045045045 - nodes in this community are weakly interconnected._
- **Why does `FarmSuitabilityEngine` connect `Farm Suitability Engine` to `Farm Engine Tests`, `Soil Survey & Advice Tests`, `Settings & Water Screens`, `Seed Recommendation Engine`, `App Shell & Navigation`?**
  _High betweenness centrality (0.045) - this node is a cross-community bridge._
- **Should `Farm Suitability Engine` be split into smaller, more focused modules?**
  _Cohesion score 0.06925418569254185 - nodes in this community are weakly interconnected._