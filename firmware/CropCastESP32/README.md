# CropCast ESP32 firmware

This sketch reads the CropCast sensor set and writes data to the Firebase paths consumed by the Android dashboard.

It is built for staged bring-up: sensors can be wired one at a time, and the firmware refuses to feed values it does not trust into the crop-recommendation pipeline. A serial console (115200 baud) exposes live diagnostics, and every reading is also logged to a microSD card, including while Wi-Fi is down.

## Wiring

| Module | ESP32 connection | Notes |
|---|---|---|
| DHT11 | DATA → GPIO 4 | Air temperature and humidity. Add a 10 kΩ pull-up from DATA to 3.3 V if your module does not include one. |
| SN-3002 7-in-1 soil probe | brown → 12 V +, black → 12 V −, yellow → 485 A, blue → 485 B | Moisture, soil temperature, EC, pH, N, P, K, salinity, TDS. Do not power the probe from the ESP32. |
| Auto-direction TTL485 module | TXD → GPIO 16, RXD → GPIO 17, VCC → 3.3 V, GND → GND | No DE/RE pin. Tie the 12 V supply's − to ESP32 GND. |
| BH1750 | SDA → GPIO 21, SCL → GPIO 22, VCC → 3.3 V | Default ESP32 I²C pins. |
| microSD module | CS → GPIO 5, SCK → GPIO 18, MOSI → GPIO 23, MISO → GPIO 19, VCC → 5 V | Default VSPI pins. |

The probe answers as Modbus slave `1` at 4800 baud. One request reads holding registers `0x0000`–`0x0008`:

| Register | Value | Scale |
|---|---|---|
| `0x0000` | moisture | ÷10, % |
| `0x0001` | soil temperature | signed, ÷10, °C |
| `0x0002` | EC | µS/cm |
| `0x0003` | pH | ÷10 |
| `0x0004`–`0x0006` | N, P, K | mg/kg |
| `0x0007` | salinity | raw |
| `0x0008` | TDS | raw |

The app's `temperature` and `humidity` still come from the DHT11 (air). EC is also sent to Firebase for the app's salinity check. Soil temperature, salinity and TDS go to the SD card only (see the data contract below).

## Arduino libraries

- Firebase Arduino Client Library for ESP8266 and ESP32 (`Firebase_ESP_Client`)
- DHT sensor library by Adafruit
- Adafruit Unified Sensor
- BH1750 by Christopher Laws

SPI, SD, Wire and WiFi are part of the ESP32 core. The probe is read with a raw Modbus frame, so no Modbus library is needed.

The sketch uses about 99 % of the default 1.3 MB app partition. Select **Tools → Partition Scheme → Huge APP (3MB No OTA)** before uploading.

## Setup

1. Create `secrets.h` (gitignored) with `FIREBASE_API_KEY`, `FIREBASE_DATABASE_URL`, `FIREBASE_USER_EMAIL` and `FIREBASE_USER_PASSWORD`. Wi-Fi and device id are no longer compiled in; they are set by pairing.
2. Create the firmware email/password user in Firebase Authentication.
3. Deploy `../../firebase/database.rules.json`.
4. Select the correct ESP32 board and serial port, then upload `CropCastESP32.ino`.
5. Pair the device (below). Serial Monitor at 115200 baud, **line ending Newline**, `help` lists commands.

## Pairing

A device with no saved Wi-Fi opens its own access point instead of connecting anywhere.

1. In the app, sign in (email or Google), open Settings, tap **Pair a device**. The app shows a pairing code.
2. On the phone, join the Wi-Fi network `CropCast-XXXX` printed in the serial log. The setup page opens by itself (or browse to `http://192.168.4.1/`).
3. Pick your home Wi-Fi (2.4 GHz), enter its password and the pairing code, tap **Connect**.
4. The firmware tests the Wi-Fi first; a wrong password is reported on the page and nothing is saved. On success it saves, restarts, joins the Wi-Fi and publishes to `/devices/<pairing code>`.

**The pairing code is the device id.** The app (`PairingCode.kt`) generates a random 4-digit PIN such as `4821`, skipping any PIN that already has a device. The firmware accepts 4-40 letters, digits, `-` or `_`. When `/devices/<code>/status/online` turns true, the app saves the code at `users/<uid>/deviceId`, so every phone signed in to that account reads this device. Accounts with no saved code still read the build-time `cropcast.deviceId`.

To pair again: hold the **BOOT** button 5 seconds, or run `pair` in the console. Both erase the saved Wi-Fi and code, then reopen the portal.

The setup access point is open by default, so the Wi-Fi password crosses the air unencrypted during setup. Define `CROPCAST_AP_PASSWORD "..."` (8+ characters) in `secrets.h` to make it WPA2.

Security limit: the code is a secret path, and the rules still let any signed-in user read any device. Per-owner rules (`devices/$id/owner`) are the next step.

Live readings are written every 15 seconds. One sample per hour is stored in `readings/monthly/yyyy-MM/yyyy-MM-dd_HH-00-00`, so the Android dashboard can display every available month and generate crop recommendations from monthly averages. The `cloudHistoryEnabled` setting controls whether those history samples are stored.

## Serial console

Set the Serial Monitor line ending to **Newline** or **Both NL & CR**. With "No line ending" the console never receives a complete command and appears dead.

The console is serviced before the network check, so it keeps working with Wi-Fi or Firebase down. It is briefly unresponsive during an upload, because the Firebase client is synchronous.

| Command | What it does |
|---|---|
| `help`, `?` | List commands |
| `status` | Uptime, heap, Wi-Fi, clock, Firebase, history state, timers, last published values, health table |
| `health` | Per-sensor health table |
| `read` | Read every sensor now, including the probe extras, and show which fields are trusted. Uploads nothing. |
| `publish [current\|history\|status\|all]` | Force a publish and print the gate decision, including why something was withheld |
| `scan` | I²C bus scan, annotating BH1750 at `0x23` / `0x5C` |
| `soil [reg] [count]` | One raw Modbus read of the 7-in-1 probe (default: all nine registers from `0x0000`). The fastest way to debug RS485. |
| `time [sync]` | Clock state and the current hour slot; `time sync` re-arms NTP |
| `wifi [reconnect\|scan]` | Network state; force a reconnect; list nearby 2.4 GHz SSIDs, channels, and signal strengths |
| `fb` | Firebase ready state, UID, base path, last error and HTTP code |
| `sensors` | List channels; `sensors soil off` marks one not wired for this session |
| `log [level]` | Show or set verbosity: `off error warn info debug trace` |
| `history [on\|off]` | Local override of `cloudHistoryEnabled`, so a bench test never lands in the month |
| `pair` | Forget Wi-Fi and pairing code, restart into the setup portal |
| `reboot` | Mark the device offline, then restart |

Log lines are fixed-width and greppable:

```
[    12.345] I/PUB   : current uploaded (7/7 scored fields trusted)
[    12.401] W/SOIL  : no response, 3 consecutive, using cached value
```

Passwords and auth tokens are never printed. The API key is shown masked.

## microSD log

Every 15-second sensor cycle appends one row to `/soil_data/YYYY-MM/YYYY-MM-DD.csv`. Folder and file names and the `date_time` column use Philippine time (UTC+8); Firebase keeps using UTC. A row is written only after NTP has synced, so a device that never reaches the internet logs nothing.

```
date_time,moisture_pct,temp_c,ec_uscm,ph,nitrogen_mgkg,phosphorus_mgkg,potassium_mgkg,salinity,tds,light_lux,air_temp_c,humidity_pct
```

The first eleven columns match the standalone probe sketch, so its files can be merged with these. `temp_c` is soil temperature. An untrusted field is an empty cell, never `0`. If the card is missing or removed, logging retries every cycle; set `CROPCAST_SD_ENABLED false` in `secrets.h` to turn it off.

**Offline backfill.** While Wi-Fi is down only the SD card records. After it reconnects, the firmware reads the SD rows it missed and uploads the first complete row of each hour to `readings/monthly`, using the same hour keys as the live path, so it never double-counts an hour. Up to 8 Firebase operations per cycle, newest-first limit of 7 days, current hour left to the live path. Progress is kept in flash (`bf`, UTC seconds), so it survives reboots and resumes where it stopped. `readings/current` is never backfilled, since it is only meaningful live. Rows with an empty scored field (moisture, pH, N, P, K, air temperature, humidity) are skipped, as in the live path. The clock must have synced at least once, so a boot with no internet at all still logs nothing.

The probe's factory calibration is used as-is. Before trusting N/P/K for crop scoring, compare one reading against a laboratory soil test from the same spot.

## Bringing sensors online one at a time

Mark a channel off in `secrets.h` (`CROPCAST_SENSOR_SOIL_ENABLED false`) or at runtime with `sensors soil off`. What gets published in each state:

| State | `readings/current` | `readings/monthly` | `status` |
|---|---|---|---|
| All scored sensors trusted | full reading | one sample per hour | online |
| Any scored sensor missing | published, `0` placeholder for it, logged at WARN | **nothing** | online, `degraded:true` |
| Clock not synced | **nothing** | **nothing** | online, `lastSeen:0` |

**Why history is all-or-nothing.** `readings/monthly` is the only path the app aggregates. `MonthlySensorAggregator` takes an unweighted mean over every valid sample in the month, and `SeedRecommendationEngine` scores seven fields. With N/P/K at zero, three of those seven collapse — against the Tomato profile (N 80–150, P 50–70, K 100–150) they score 20/0/0 instead of up to 100 — and the ranking between crops ends up decided by residual noise instead of by soil. Withholding costs nothing: the month simply stays below the 8-sample minimum and the app honestly reports that there is not enough data, which is far better than a confidently wrong crop.

Nothing is lost by waiting. The gate is re-checked every 15 s, so a probe that comes online at 14:37 still fills the 14:00 slot.

**During bring-up, placeholder zeros will trip the app's low-moisture and pH-out-of-range alerts**, repeating on the interval in Settings. This is pre-existing app behaviour for any missing reading, not something the firmware introduced. Turn alerts off in the app's Settings screen, or lower the thresholds, until the probes are wired.

## Data contract

Every reading record carries these nine required fields, and `timestamp` is Unix milliseconds UTC:

```
temperature humidity soilMoisture soilPh nitrogen phosphorus potassium lightIntensity timestamp
```

Three gates a reading must survive:

- **Server** — `firebase/database.rules.json` requires all nine children and validates temperature −20..80, humidity 0..100, soilMoisture 0..100, soilPh 0..14, and N/P/K/light ≥ 0. One out-of-range field rejects the **entire** write, which is why the firmware zeroes an implausible value rather than letting it take the other eight fields down.
- **App** — `isValidForRecommendation()` repeats those ranges and adds `timestamp > 0`. A reading that fails is dropped, and the dashboard then renders zeros.
- **Scoring** — seven fields are scored. `lightIntensity` is deliberately excluded until lux thresholds are validated, so it is the one field where a placeholder is harmless.

It also carries one optional field, `electricalConductivity`: the probe's soil EC in µS/cm, or `0` when the probe is untrusted. The app uses it for its salinity check and treats `0` as "not measured", so readings from older firmware still work. Do not add any other field to a reading record. The rules would accept it, but the Android app logs an unknown-property warning on every snapshot and ignores the value. Diagnostics belong under `/status`, which the app reads with explicit lookups and which is safe to extend. The firmware only ever **reads** `/settings`; it must never write there.

### Status payload

Alongside `online`, `lastSeen`, `firmware`, and `sensors[]`, the firmware publishes `wifiRssi`, `ipAddress`, `uptimeSeconds`, and three diagnostic keys the app ignores: `sensorHealth` (per-channel state), `degraded`, and `historyState` (`publishing` / `withheld` / `disabled`). `sensors[]` now lists only channels that are currently healthy, always including `ESP32`.

`status` is published on every cycle no matter what fails, so a single bad sensor can never make the device look offline.

## Troubleshooting

| Symptom | Check |
|---|---|
| Console appears dead | Serial Monitor line ending must be Newline, baud 115200 |
| `BH1750 not on the bus` | Run `scan`. Expect `0x23` (ADDR low) or `0x5C` (ADDR high). Check SDA 21 / SCL 22 and 3.3 V. |
| `SOIL: no response` | Run `soil`. Check TXD→16, RXD→17, yellow→A, blue→B, 12 V probe power, 12 V ground tied to ESP32 GND. |
| `no valid frame (bad CRC or wrong slave id)` | Bytes arrive but do not decode. Swap A and B, then check the slave ID and 4800 baud in the probe manual. |
| `SD: no card on CS GPIO 5` | Card missing or not FAT32. Check CS 5, SCK 18, MOSI 23, MISO 19 and 5 V. |
| No `CropCast-XXXX` network | Device already holds a Wi-Fi + code. Hold BOOT 5 s or run `pair`. |
| Portal says it could not join the Wi-Fi | Wrong password, or a 5 GHz-only network. The ESP32 needs 2.4 GHz. |
| `clock not synced` | NTP is unreachable. Readings are withheld on purpose until it succeeds; `time sync` retries. |
| Firebase error with `http 400` | The database rules rejected the write — usually a value outside the validated range. |
| Firebase error with `http 401` | Authentication failed. Check the API key and that the firmware user exists in Firebase Authentication. |
| `ESP_RST_BROWNOUT` in the boot banner | Power supply cannot hold up under load, commonly an under-powered RS485 probe. |
| History never publishes | `status` → `historyState`. `withheld` means a sensor is untrusted (`health` says which); `disabled` means `cloudHistoryEnabled` is off. |
