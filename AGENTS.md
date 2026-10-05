# AGENTS.md

Guide for AI coding agents and new contributors working on CropCast. Read this before changing anything. Keep it current: update the **Status** section whenever something is built, verified, or found broken.

## What CropCast is

An Android app plus ESP32 sensor for Philippine smallholder farmers (field: Mapili, San Enrique, Iloilo). The ESP32 reads air temperature/humidity (DHT11), soil moisture/pH/N/P/K/EC (SN-3002 7-in-1 probe over RS485), and light (BH1750), logs to microSD, and writes to Firebase Realtime Database. The app ranks crops for the farm and explains the result.

## Repository map

| Path | What |
|---|---|
| `app/` | Android app (Kotlin, Jetpack Compose, Firebase Auth + Realtime Database). Package `com.cropcast.app`. |
| `app/src/main/java/com/cropcast/app/data/` | Engines and repositories. `FarmSuitabilityEngine` (FAO/ECOCROP rule scorer, per-month scores), `SeedRecommendationEngine` (older monthly sensor scorer), `FirebaseSensorRepository`, `FarmSiteDataRepository` (NASA POWER, ECMWF seasonal, Open-Meteo, SoilGrids), `SoilSurvey` (BSWM Iloilo soil series), `FarmAdviceRepository` (LLM explanation), `PairingCode`. |
| `app/src/main/java/com/cropcast/app/ui/` | `CropCastApp.kt` (root, tabs, `farmRecommendation`), `CropCastViewModel.kt`, `SplashOverlay.kt`, `screens/`, `localization/` (English, Hiligaynon, Filipino). |
| `app/src/test/` | JVM unit tests only. No instrumented tests. |
| `firmware/CropCastESP32/` | Arduino sketch `CropCastESP32.ino`, `portal_logo.h`, README. `secrets.h` is gitignored. |
| `firebase/database.rules.json` | Realtime Database rules. |
| `worker/` | Cloudflare Worker (`index.js`, `wrangler.toml`): LLM explanation proxy. Keys are Wrangler secrets. |
| `scripts/` | Python tools: `seed_demo_history.py`, `simulate_firebase_sensor.py`, `test_ai_providers.py`, data builders. |
| `data/`, `models/` | Reference datasets and built tables. Not loaded at runtime except the assets copied into `app/src/main/assets/`. |
| `plan.md` | Current plan (Prediction tab). `docs/plan-monthly-recommendation.md` is the delivered earlier plan. |

## Build, test, install

```powershell
.\gradlew.bat --offline :app:testDebugUnitTest          # all JVM tests
.\gradlew.bat --offline :app:installDebug               # install on the connected phone (adb)
.\gradlew.bat installDebug -Pcropcast.deviceId=<id>     # build-time default device for accounts with no paired device
adb exec-out screencap -p > shot.png                    # screenshot the phone
```

- Gradle properties in `gradle.properties`: `cropcast.deviceId` (default `esp32-field-01`), `cropcast.adviceUrl` (the worker URL; empty hides the "Explain with AI" button).
- `app/google-services.json` is required for Firebase; without it the app runs in demo mode.
- The firmware has **no local compiler** on the development PC. Compile it in Arduino IDE (board ESP32, **Partition Scheme: Huge APP (3MB No OTA)**, libraries listed in `firmware/CropCastESP32/README.md`).

## First run on a new machine (agents: do this first)

1. Run Gradle **without `--offline`** once so dependencies download: `.\gradlew.bat :app:testDebugUnitTest`.
2. **Print this machine's debug signing fingerprint and show it to the user**, then ask them to add the SHA-1 in Firebase console > Project settings > the Android app `com.cropcast.app` > Add fingerprint. Google sign-in fails with the app installed from this machine until that is done. If they change it, they should download a fresh `app/google-services.json`.

```powershell
keytool -list -v -keystore $env:USERPROFILE\.android\debug.keystore -storepass android -alias androiddebugkey
```

   If the keystore does not exist yet, build once (`:app:installDebug`) and run the command again. Report the `SHA1:` line (and `SHA256:` if asked); do not edit Firebase yourself.
3. Check `firmware/CropCastESP32/secrets.h` exists (copy `secrets.example.h` and ask the user for the Firebase values). It is gitignored and must never be committed or printed.
4. `adb devices` should list the phone before `:app:installDebug`.
5. `graphify-out/` (the code knowledge graph: `graph.json`, `GRAPH_REPORT.md`, `graph.html`, `manifest.json`) is committed. Its `cache/`, `.bak` files and machine-specific interpreter paths are ignored. After changing code, refresh it with `/graphify --update` and commit the result.

## Conventions that matter

- **Line endings are CRLF** in most source files. Preserve them; do not rewrite whole files with LF. Edit with exact-match replacements, then check with `file <path>`.
- **Months are UTC.** `MonthlySensorAggregator`, Firebase month keys (`yyyy-MM`) and the daily drill-down all use UTC. Use `YearMonth`, never string maths. The Philippines is UTC+8.
- **Data contract** (firmware, rules, app must change together): every reading carries `temperature humidity soilMoisture soilPh nitrogen phosphorus potassium lightIntensity timestamp` (epoch ms UTC) plus optional `electricalConductivity` (uS/cm, 0 = not measured). `0` means "no data"; the firmware zeroes untrusted fields. Do not add other keys to a reading record.
- **Never write test data to a real device.** `seed_demo_history.py` refuses `esp32-field-01`. Seeded data is flagged `simulated: true` in `/status`.
- **Secrets** stay out of git: `firmware/CropCastESP32/secrets.h`, `app/google-services.json`, Wrangler secrets (`OPENROUTER_KEY`, `XKIRO_KEY`). Never print or paste keys or passwords. Agents must not type credentials; tell the user to run commands that need a password.
- **UI strings** go through `tr()`. Add Hiligaynon (`Localization.kt`) and Filipino (`FilipinoStrings.kt`) entries; `FilipinoCoverageTest` checks coverage.
- **Scores are rule-based and explainable.** The LLM only writes text from facts the app sends. It never changes a score.
- After a UI change, install on the phone and look at it (`adb`); a green build is not proof the screen is right.

## Pairing flow (how a device becomes a user's device)

1. Unpaired ESP32 (or `pair` in the serial console, or BOOT held 5 s) opens AP `CropCast-XXXX` with a captive setup page.
2. In the app: Settings > Farm & Device > **Pair a device** shows a 4-digit PIN (`PairingCode`, skips PINs that already have a device).
3. On the setup page the user picks Wi-Fi, enters its password and the PIN. The firmware tests the Wi-Fi, saves Wi-Fi + PIN to flash, restarts.
4. The device publishes to `/devices/<PIN>`. When `status/online` goes false -> true, the app saves `users/<uid>/deviceId = PIN`. Re-pairing keeps the account's PIN, so history survives.
5. Accounts with no saved device read the build default (`cropcast.deviceId`).

Known weakness: any signed-in user can read any device, and a 4-digit PIN is guessable. Fix needs per-owner rules and a long random device id.

## Seeding demo history

```powershell
python scripts/seed_demo_history.py --email <account email> --link-account --poll-minutes 20 --recent-days 400 --write
```

Needs an email/password login (a Google-only account must first set a password with Settings > Change password). Soil comes from sampling the real probe; weather is real Open-Meteo history; soil moisture is modelled. `--export file.json` saves the data; `--source-device <id>` picks the device to sample.

## Status (update this)

Last updated 2026-10-05. "Verified" means I saw it work on the real phone or in a passing test; "unverified" means written but not run.

### Working and verified

| Area | Evidence |
|---|---|
| App build, install, all JVM unit tests | Gradle run passed after the latest logic changes |
| Crop ranking with farm location (rain, climate, soil map) | Read on phone: Rice 86%, Cucumber 80%, Garlic 78% |
| Soil type setting + picker (drawn illustrations and hand tests) | Opened on phone; unit tests for the type -> texture mapping |
| Sensor screen: Current Conditions above Monthly; month -> days -> hourly drill-down (UTC) | Used on phone |
| Splash screen (system splash + animated overlay) | Frames captured on phone |
| Pair dialog + PIN generator | `PairingCodeTest` passes; account linked to PIN 1139 by the seed run |
| Seed script: 8 months hourly, account linked | User ran it; output showed all months written and the link |
| AI explanation through the worker | Direct call returned 200 in 4.2 s; "Explain with AI" worked in the app |
| Crop variety hidden until a crop is set | Built and installed; not looked at on the phone |

### Written but unverified

| Area | Why unverified |
|---|---|
| ESP32 firmware pairing (AP, setup page, flash storage, BOOT hold, `pair`) | Never compiled or flashed by me. The setup page HTML was checked only in a desktop browser. |
| `scripts/test_ai_providers.py` | Needs the user's API keys; not run |
| Both LLM providers individually | The worker fell back correctly, but which provider answered is unknown (`wrangler tail` shows it) |
| Google sign-in | Needs the debug SHA-1 added in Firebase (`33:5D:6B:FD:...:9D:EE`) and a new `google-services.json`; unconfirmed |

### Known problems

- **App shows Offline though the ESP32 sends data.** The app reads `/devices/1139` (seeded, `online:false`). The ESP32 is probably publishing elsewhere (likely `esp32-field-01` on old firmware). Fix: flash the new firmware and pair with PIN 1139.
- **AI explanation can misstate income.** It said rice "ranks highest for ... income"; the app's best income is tomato. Fix is in `plan.md` section 10.
- **Probe pH 7.3 vs soil map 5.7.** The app already warns; the probe may need a buffer check.
- **Cucumber and garlic rank high on soil that is wet most of the year.** Waterlogging is weighted lightly; revisit.
- **Cold start is slow** (about 1.7 s, many skipped frames; Choreographer logged "Skipped 64 frames"). Likely cause, not confirmed: the soil survey JSON is parsed during first composition in `CropCastApp.kt`.
- **Real soil photos** were not added: the Wikimedia sample photos were too small and mislabelled for a hand-test guide. Drawings are used.
- **Sensor Status card** prints fixed checkmarks (`ESP32 ✓ DHT11 ✓ ...`) regardless of real sensor health.

### Next

Build the Prediction tab per `plan.md`.
