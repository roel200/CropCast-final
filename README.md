# CropCast

CropCast is a Kotlin/Jetpack Compose Android dashboard for an ESP32 farm sensor node. It follows the supplied green mobile references and includes sensor monitoring, configurable soil-moisture alerts, alert history, monthly sensor summaries, observed monthly crop recommendations, and a next-month forecast based on past sensor history.

The Settings screen includes editable farm/device names, connection testing, ESP32 restart commands, notification thresholds and behavior, crop/planting information, language preference, offline-data status, farmer/administrator profile details, password/sign-out actions, and privacy controls.

## Included sensor fields

| Sensor | Firebase field | Unit |
|---|---|---|
| DHT11 | `temperature`, `humidity` | °C, % |
| Capacitive moisture (optional reference-screen feature) | `soilMoisture` | % |
| Soil pH probe | `soilPh` | pH |
| RS485 NPK sensor | `nitrogen`, `phosphorus`, `potassium` | mg/kg |
| BH1750 light sensor | `lightIntensity` | lux |

If Firebase has not been configured, the login form starts a local demo session so the UI can be previewed immediately. Credentials are validated only for basic form requirements in this mode; email verification, cloud authentication, and synchronization require `app/google-services.json`.

## Android setup

1. Open this folder in Android Studio.
2. Create a Firebase project and register Android package `com.cropcast.app`.
3. Download `google-services.json` into `app/google-services.json`.
4. Enable **Email/Password** and **Anonymous** sign-in under Firebase Authentication.
5. Create at least one email/password user for farm staff who should log in.
6. Create Realtime Database, then deploy `firebase/database.rules.json`.
7. Optionally import `firebase/sample-data.json`, or generate the Potato test
   fixture described below.
8. Build and run on an Android 8.0+ device or emulator.

The default device path is:

```text
/devices/esp32-field-01
  /readings/current
  /readings/monthly/{yyyy-MM}/{yyyy-MM-dd_HH-mm-ss}
  /status
  /settings
  /alerts
```

The included rules are appropriate for a prototype: any authenticated Firebase user can access device data. Before production, bind users/devices with custom claims or per-device ownership rules and disable client-side simulation.

## Google sign-in setup

The login screen supports Google through Android Credential Manager and Firebase
Authentication. Email/password and guest login remain available. Google sign-in
is disabled in local demo mode.

1. In Firebase Console, open **Authentication → Sign-in method**, enable
   **Google**, select a support email, and save.
2. Run `./gradlew :app:signingReport` (Windows: `gradlew.bat :app:signingReport`).
   In **Project settings → Your apps → com.cropcast.app**, add the debug
   certificate SHA-1 and SHA-256 fingerprints. Add release signing fingerprints
   as well when distributing a release build.
3. Download the updated `google-services.json` from that Android app's settings
   and replace `app/google-services.json`. It must contain a Web OAuth client
   (client type 3), used to generate `default_web_client_id`.
4. Sync Gradle and rebuild. Use a real device with Google Play services or an
   emulator with a **Google Play** system image, and add a Google account.
5. Tap **Sign in with Google**, choose an account, and verify that the dashboard
   opens. Sign out and try another account; canceling the chooser should leave
   the login screen usable. Also check the existing email and guest flows.

The initial checked-in Firebase configuration has no OAuth clients, so it must
be updated before Google sign-in can succeed. Never put a Google client secret
in the Android app.

## ESP32 setup

Install these Arduino libraries:

- Firebase Arduino Client Library for ESP8266 and ESP32 (`Firebase_ESP_Client`)
- DHT sensor library
- BH1750

Copy `firmware/CropCastESP32/secrets.example.h` to `secrets.h`, fill in Wi-Fi/Firebase credentials, and create that email/password account in Firebase Authentication. Select the **Huge APP (3MB No OTA)** partition scheme before uploading.

The firmware wiring defaults are DHT11 GPIO 4, SN-3002 7-in-1 soil probe through an auto-direction TTL485 module on GPIO 16/17 (4800 baud, slave `1`), BH1750 on I²C GPIO 21/22, and a microSD card on SPI with CS GPIO 5. See `firmware/CropCastESP32/README.md`.

## Crop recommendation engine

The main recommendation (`FarmSuitabilityEngine`) ranks 17 crops for the farm: the original 12 plus pechay, kangkong, ampalaya, sitaw and kalabasa. It ranks them for planting this month, gives each an FAO suitability class, and shows the best months to plant. Temperature and rain follow FAO's ECOCROP model exactly as its reference implementation (R package Recocrop) computes it: monthly climate is interpolated to half-months, each half-month is scored on the crop's trapezoid (0 outside the absolute range, 100 inside the optimal range), the worst half-month over the growing season counts, and ECOCROP's crop-cycle rain totals are converted to monthly limits.

| Factor | Role | Data source |
|---|---|---|
| Air temperature over the growing season | caps the score | NASA POWER 20-year normals, corrected for the farm's elevation; the Open-Meteo 16-day forecast replaces the first month; DHT11 when offline |
| Soil pH | scales the score (weight 0.5) | Field sample or 7-in-1 probe; ISRIC SoilGrids map when neither exists |
| Rain over the growing season | scales the score (weight 0.3) | NASA POWER normals |
| Soil texture (clay/sand) | scales the score (weight 0.2) | ISRIC SoilGrids |
| Soil salinity | multiplies by FAO relative yield | Probe EC, scored with FAO's crop salt tolerance (Maas–Hoffman threshold and slope) |
| Wet-season disease | multiplies by 0.85 in wet, warm seasons | Tomato, eggplant and potato family; cucumber, ampalaya and kalabasa family; cabbage and pechay family |
| Heavy rain at planting | multiplies by 0.85 (50–100 mm/day) or 0.70 (100 mm/day or more) | Open-Meteo 16-day forecast, PAGASA rainfall categories. Only crops that need well-drained soil in ECOCROP; rice, kangkong and sitaw tolerate waterlogging |
| Crop ranges and cycle lengths | — | FAO ECOCROP (`data/processed/ecocrop_selected.csv`, bundled in the app) |

- **Temperature caps the score** because a farmer cannot change it (0 means the crop cannot grow there). **pH, rain and texture scale it between 60 % and 100 %**, because lime or compost, irrigation and drainage can manage them. Unlike ECOCROP, pH is not absolute: validation showed tomato thriving on Ilocos soils above pH 7.5 and corn on Bukidnon soils below pH 4.5.
- **FAO classes:** S1 highly suitable (80+), S2 moderately (60–79), S3 marginally (40–59), N not suitable (below 40).
- **Prediction, not just today's weather:**
  - *Whole season:* the ECMWF SEAS5 seasonal forecast (via Open-Meteo, up to 6 months) replaces the normals for the months it covers. Rain is scaled by the forecast's ratio to ECMWF's own average, which cancels the model's local bias; temperature gets the forecast anomaly. A predicted wet season lowers crops that need little water; a predicted dry season lowers water-hungry crops unless irrigation is on.
  - *Planting window:* heavy rain forecast in the next 16 days lowers crops that need well-drained soil, and the app gives the first safe planting date after the last heavy-rain day.
  - *Wind:* gusts of 62 km/h or more (PAGASA Wind Signal No. 2) trigger a warning to stake crops and delay transplanting.
  - *Typhoons:* tall crops are flagged when their flowering and ripening months fall in July–November; PAGASA's wind signals describe crop damage as worst at those stages.
- **Irrigation available** (Settings → Crop Profile) stops dry months from lowering scores; too much rain still counts. Validation against PSA harvest quarters favours this for dry-season vegetables and the rain-fed default for rice, corn and highland potato.
- **Elevation correction:** NASA's ~50 km grid cell can sit far below or above the farm (Benguet's cell averages ~700 m), so normals are shifted by the standard lapse rate (6.5 °C per km) using the farm elevation from the forecast.
- **Soil and fertilizer plan** for the top crop:
  - The probe's N, P and K are classed Low, Medium or High against general soil-test levels (N 125/250, P 10/25, K 78/156 mg/kg).
  - Low soils get the top of the crop's per-hectare need, medium soils the middle, high soils a maintenance dose.
  - The result is kilograms of urea, solophos and muriate of potash per hectare, in 50 kg bags and per 1,000 m². pH gives lime or compost advice.
  - Kangkong, ampalaya and sitaw have no verified per-hectare rate, so the app points to the DA production guide and a BSWM soil test instead.
  - N, P and K never change the ranking, because fertilizer can correct them.
- **Income:** each crop shows gross income per hectare from official PSA data (2021–2025 national yield × 2024–2025 farmgate price, before costs). The card names the best-income crop among S1 and S2 crops when it differs from the most suitable one. Rebuild with `scripts/build_crop_economics.py`.
- **Season risks** (advice only):
  - typhoon season (July–November) for tall, staked or trellised crops north of 9.5° N;
  - wet-season disease by crop family;
  - planting the same family twice for the tomato, cucumber and cabbage families, whose soil-borne diseases build up. Continuous rice and rice–corn are normal practice, so they are not flagged.
- **Field soil sample** (Sensor screen): record the probe at 5–10 spots across the field, as in BSWM composite sampling. The app averages them, flags a pH spread over 1.0, saves the sample to `samples/latest`, and uses it for 180 days in place of the monthly average.
- **Corn uses ECOCROP's Flint maize**, whose 90–140 day cycle matches Philippine corn; the generic Maize entry runs to 365 days and distorts the rain limits.
- **Calibration knobs** in `FarmSuitabilityEngine`:
  - `SOIL_TEST_LEVELS` and `PROBE_EC_TO_ECE`: set both from one BSWM laboratory test taken at the same spot as a probe reading.
  - `WET_SEASON_LOSS_PCT`: the flat 15 % wet-season loss.
  - `HEAVY_RAIN_LOSS_PCT` and `INTENSE_RAIN_LOSS_PCT`: the 15 % and 30 % planting-window losses, which are estimates.
- **Warnings** compare the probe pH with the SoilGrids map, flag saline soil (ECe 4 dS/m or more), and report the elevation correction. They summarize the seasonal forecast (months 25 % or more drier or wetter than normal, and warming of 1 °C or more). They flag heavy rain with a safe planting date, gale-force gusts, little rain, high humidity and a dry recent month, and repeat the top crop's season risks.
- **Data sources need no API key.** Every online answer is cached on the phone, so the last good copy is used without internet. With no location or cache, the engine still ranks crops from the probe and the DHT11. Set the farm latitude and longitude in **Settings** to turn on climate and soil data.
- **Validation:** see `data/validation/README.md`.

## Notes

- The 7-in-1 probe's factory calibration is used as-is. Compare its N/P/K against a laboratory soil test before relying on them for crop scoring.
- Firebase timestamps use NTP-derived Unix milliseconds, which the app uses to determine whether the ESP32 was seen within the last two minutes.
- The firmware stores one history sample per hour under a UTC `yyyy-MM` bucket. The dashboard validates readings, tracks monthly averages/ranges/variability, creates an observed recommendation for every eligible closed month, and forecasts the next month from the latest month, same-calendar-month history when available, and the three preceding complete months. The rule-based engine recommends the best-matching crop from Tomato, Okra, Alugbati, Potato, Rice, Corn, Eggplant, Cucumber, Cabbage, Sweet Potato, Lettuce, and Spinach using N, P, K, pH, soil moisture, temperature, and humidity.
- The Crop Recommendation screen also shows, for comparison, an experimental on-device Random Forest trained on the public 22-crop dataset (N, P, K, temperature, humidity, pH). Its saved metrics describe only a held-out portion of that dataset and are not evidence of Philippine field accuracy; see `models/public_crop/README.md`.
- Recommendations show the runner-up, confidence, unstable fields, month-to-month trends, and general Philippine wet/dry-season guidance. This seasonal note is advisory and explicitly asks the farmer to confirm a local weather forecast.
- Farmers can save planting outcomes under `devices/{deviceId}/recommendations/feedback`. A crop's score receives a conservative local adjustment only after at least three valid 1–5 outcome ratings; harvest weight and notes are stored for later evaluation but do not change the score because field area and yield units are not yet normalized.
- Light remains visible in the dashboard but is intentionally excluded from crop scoring until crop-specific lux thresholds are validated. The tomato reference dataset's solar radiation in `W/m2` is not converted to lux.
- Dataset preparation is reproducible with `scripts/prepare_cropcast_data.py`; raw downloads stay unchanged under `data/raw`, and cleaned/selected outputs are written under `data/processed`.
- Restart requests are written to `/devices/esp32-field-01/commands/restart`; the included firmware checks this command every five seconds and acknowledges it before restarting.

## Crop recommendation test data

Generate a Firebase-ready fixture containing eight simulated Potato-compatible
sensor readings:

```powershell
python scripts/seed_potato_test_data.py
```

The fixture defaults to the previous UTC month so it can be used as a completed
month for the next-month forecast. Use `--at` to choose a different UTC month.

Generate and verify a Rice fixture with:

```powershell
python scripts/seed_crop_test_data.py --crop Rice
```

Generate verified fixtures for every supported crop with:

```powershell
python scripts/seed_crop_test_data.py --all
```

The commands write `firebase/potato-test-data.json` or
`firebase/rice-test-data.json` and verify that their monthly
averages score the selected crop highest. Import it only into a test
Firebase Realtime Database because a root-level JSON import can replace existing
test data. After signing in and opening the Seeds screen, CropCast should show
the selected crop as the next-month forecast when the fixture is in a closed
month, with its observed recommendation available in the monthly history. The
`W/m2` versus lux limitation still applies: the generated lux values are
displayed but are not part of the recommendation score.

You can verify the recommendation engine without Firebase by running:

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.cropcast.app.data.SeedRecommendationEngineTest.recommendsPotatoForSeededPotatoReadings"
```

## Live Firebase sensor simulator

When physical sensors are not available, send changing readings to Firebase
with the included simulator. It uses anonymous Firebase authentication by
default, writes only to the selected device paths, and never performs a
root-level JSON import:

```powershell
python scripts/simulate_firebase_sensor.py --crop Alugbati --interval 5
```

Keep the app open on the Home or Sensor screen; its Firebase listeners should
update the current values and online status every five seconds. Stop the
simulator with `Ctrl+C`; it marks the simulated device offline.

To create eight valid readings in a completed month and test the next-month
recommendation, use a past UTC month:

```powershell
python scripts/simulate_firebase_sensor.py --crop Alugbati --backfill-month 2026-08 --count 8 --interval 1
```

Use `--dry-run` to check a reading without changing Firebase. Use
`--email EMAIL --password PASSWORD` when anonymous sign-in is disabled, or
pass an existing Firebase `--id-token`. This simulator is for app and Firebase
testing only; its readings are not real sensor evidence.

For a visual preview in a debug build, launch the app with its local demo data:

```powershell
adb shell am start -S -n com.cropcast.app/.MainActivity --ez forceDemo true
```

Continue as guest, then open **Crop Recommendation**. The `forceDemo` extra is
ignored by release builds.

To preview any supported crop instead of Potato, include the debug-only crop selector:

```powershell
adb shell am start -S -n com.cropcast.app/.MainActivity --ez forceDemo true --es demoCrop Rice
```
