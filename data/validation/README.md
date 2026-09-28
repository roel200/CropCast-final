# Validating the farm crop ranking

CropCast's main recommendation (`FarmSuitabilityEngine`) is tested three ways.
All tests are ordinary JVM unit tests under `app/src/test/java/com/cropcast/app/data/`.

## 1. Known-answer farms (`FarmKnownAnswerTest`)

A seeded generator builds random farms whose correct answer is known before the
engine runs:

| Test | Farms | What must hold |
|---|---|---|
| Ideal farms | 3,400 (200 per crop) | Every condition sits in the crop's FAO optimum all year, outside the wet-season disease window → the crop scores 100 and is in the top tier |
| Hostile farms | 1,700 | Always hotter or colder than the crop's absolute limits → the crop scores 0 |
| Random farms vs an oracle | 3,000 | An independent month-by-month check of the raw FAO ranges agrees with the engine on every "ideal" and "impossible" crop; a wrong pH costs at most 20 points |
| Missing data | 32 combinations | Any mix of missing climate, forecast, soil map and probe still gives scores in 0-100 |
| Monotonic temperature | 17 crops × 41 steps | Moving temperature toward the optimum never lowers the score (up to 24 °C for disease-prone crops, where wet-season loss starts on purpose) |

## 2. Where the Philippines really grows each crop (`FarmRealWorldValidationTest`)

Ground truth is official **PSA OpenSTAT** production, 2021-2025 mean, for all 17
crops in every province (tables `0082E4EVCP3` and `0012E4EVCP0`). Each province
is placed at its OpenStreetMap label point and given what the app would download
there: NASA POWER normals, Open-Meteo elevation and ISRIC SoilGrids soil. A crop
is **suitable** when its best planting month scores 50 or more.

- **Production covered:** share of national production grown in provinces rated suitable.
- **Top-5 suitable:** how many of the five biggest producing provinces are rated suitable.
- **Harvest season:** PSA reports production by harvest quarter; the test correlates it
  with the engine's scores for plantings that would be harvested in each quarter.

## 3. Real farms (`FarmRealWorldValidationTest`)

`SoilsSync.xlsx` farms growing a CropCast crop (49 corn, 9 tomato, 7 rice), with
their laboratory pH used as the probe reading.

Feature tests for FAO classes, salinity, fertilizer rates, season risks, field
samples and PSA economics are in `FarmAdviceTest`.

## Rebuilding the data

```powershell
.venv\Scripts\python scripts\build_validation_data.py
```

The script caches every download under `data/validation/.cache` (gitignored) and
respects each service's limits (Nominatim 1 request/s, SoilGrids 5/min), so a full
run takes about 40 minutes and can be resumed. It writes `sites.json`, which the
tests read offline.

## Results

Run of 2026-09-28: 59 tests pass. Every province and farm has NASA POWER climate
data; 81 of 85 provinces and all 65 farms also have SoilGrids soil data.

### Known-answer farms

All 3,400 ideal farms and all 1,700 hostile farms are scored as expected. On 3,000
random farms the engine agrees with the independent oracle every time, and on the
farms with an ideal crop its top pick is always an ideal crop.

### Where the Philippines grows each crop

| Crop | Production covered | Top-5 rated suitable | Top producers (best score) |
|---|---|---|---|
| Tomato | 100 % | 5/5 | Bukidnon 98, Ilocos Norte 86, Nueva Ecija 97, Ilocos Sur 85, Pangasinan 83 |
| Okra | 100 % | 5/5 | Nueva Ecija 100, Isabela 100, Bulacan 100, Cagayan 100, Tarlac 98 |
| Alugbati | 100 % | 5/5 | Iloilo 92, Cebu 84, Maguindanao 87, Misamis Oriental 88, Negros Oriental 86 |
| Potato | 89 % | 4/5 | Benguet 90, Davao del Sur 41, Mountain Province 95, Bukidnon 98, Nueva Vizcaya 97 |
| Rice | 99 % | 5/5 | Nueva Ecija 100, Isabela 97, Pangasinan 97, Cagayan 95, Iloilo 100 |
| Corn | 100 % | 5/5 | Isabela 98, Bukidnon 98, Cagayan 100, Maguindanao del Sur 98, Lanao del Sur 98 |
| Eggplant | 100 % | 5/5 | Pangasinan 86, Quezon 86, Iloilo 86, Tarlac 86, Nueva Ecija 86 |
| Cucumber | 100 % | 5/5 | Benguet 86, Nueva Vizcaya 95, Nueva Ecija 92, Agusan del Sur 82, Lanao del Norte 85 |
| Cabbage | 97 % | 4/5 | Benguet 79, Mountain Province 89, Cebu 88, Bukidnon 88, Davao del Sur 49 |
| Sweet Potato | 100 % | 5/5 | Leyte 98, Albay 98, Tarlac 95, Northern Samar 100, Quezon 97 |
| Lettuce | 89 % | 3/5 | Benguet 92, Bukidnon 75, Negros Oriental 76, Cavite 36, Laguna 47 |
| Spinach | 30 % | 1/5 | Benguet 77, Capiz 4, Nueva Vizcaya 44, Isabela 27, Antique 6 |
| Pechay | 99 % | 5/5 | Albay 80, Camarines Sur 92, Quezon 76, Cagayan 88, Isabela 88 |
| Kangkong | 100 % | 5/5 | Maguindanao 88, Leyte 90, Quezon 90, Cotabato 88, Ilocos Norte 90 |
| Ampalaya | 100 % | 5/5 | Pampanga 86, Quezon 79, Nueva Ecija 84, Bulacan 86, Batangas 85 |
| Sitaw | 100 % | 5/5 | Bulacan 96, Nueva Ecija 96, Pangasinan 100, Nueva Vizcaya 97, Isabela 95 |
| Kalabasa | 100 % | 5/5 | Quezon 98, Albay 98, Nueva Ecija 98, Nueva Vizcaya 100, Bukidnon 98 |

For the crops that only some provinces grow, producers outscore non-producers
(AUC): potato 0.69, cabbage 0.78, lettuce 0.57. The test requires at least 80 %
coverage for every crop except spinach.

Known limits:

- **Spinach:** CropCast models true spinach (*Spinacia oleracea*, which dies above
  27 °C). PSA's lowland "spinach" in Capiz or Isabela is a different, heat-tolerant
  leafy green. The engine scores Benguet, where true spinach grows, at 77.
- **One point per province:** each province is scored at its label point. Highland
  production inside mostly lowland provinces is missed, e.g. potato on Mt. Apo
  (Davao del Sur 41) and lettuce around Tagaytay (Cavite 36).

### Harvest season

Production-weighted correlation between the engine's scores and PSA's quarterly harvests.
Each crop is shown under the water regime Filipino farmers mostly use for it:

| Rain-fed | | Irrigated (dry-season vegetables) | |
|---|---|---|---|
| Potato | +0.62 | Tomato | +0.61 |
| Rice | +0.34 | Eggplant | +0.63 |
| Corn | +0.26 | Lettuce | +0.62 |
| | | Pechay | +0.54 |
| | | Okra | +0.50 |
| | | Ampalaya | +0.33 |

Still wrong: cucumber (−0.16 rain-fed, −0.27 irrigated). Kangkong (−0.08) and sitaw
(−0.15 rain-fed, +0.05 irrigated) are near zero. The wet-season disease factor
(15 % loss) is what moved eggplant from −0.54 to +0.53 and ampalaya from −0.55 to
+0.33 under rain-fed scoring.

### Real farms

| Crop | Rated suitable | Median rank of 17 |
|---|---|---|
| Corn | 49/49 | 4 |
| Rice | 7/7 | 2 |
| Tomato | 9/9 | 12 |

The Ilocos tomato farms have pH 7.4–8.0. Tomato counts as suitable there but ranks
low, because alkaline soil costs it up to 20 points.
