"""Seed demo monthly history for a SEPARATE demo device (never the real one).

Soil values (pH, N, P, K, EC, moisture baseline) come from your real probe, read
from the real device in Firebase or given with --soil. Temperature, humidity,
rain and light come from Open-Meteo history for the farm coordinates (satellite
and model data, not PAGASA stations). Soil moisture is MODELLED from that rain,
so it is synthetic. Everything written is demo data, flagged in /status.

Dry run (default, no Firebase contact unless you also pass --email):
    python scripts/seed_demo_history.py --soil ph=6.4,n=30,p=12,k=180,ec=250,moisture=28
Real soil values from your sensor, then write:
    python scripts/seed_demo_history.py --email you@example.com --write
Seed the account you log in with (picks a free 4-digit PIN, links it to the account):
    python scripts/seed_demo_history.py --email you@example.com --link-account --write
then in the app: Settings > Pair a device. The account keeps that PIN, so type the same PIN
into the ESP32 setup page and its real readings continue the seeded history.
View it without pairing a device:
    .\\gradlew.bat installDebug -Pcropcast.deviceId=esp32-demo-01
"""

from __future__ import annotations

import argparse
import getpass
import json
import random
import statistics
import sys
import time
from collections import defaultdict
from datetime import datetime, timedelta, timezone
from urllib.error import HTTPError, URLError
from urllib.parse import quote, urlencode
from urllib.request import Request, urlopen

from simulate_firebase_sensor import AUTH_URL, firebase_url, http_json, load_firebase_config

REAL_DEVICE = "esp32-field-01"
HOURS_PER_DAY = (0, 6, 12, 18)  # UTC; four spread samples keep the monthly mean unbiased by time of day
HOURLY = "temperature_2m,relative_humidity_2m,precipitation,shortwave_radiation"
LUX_PER_WM2 = 120.0  # rough sunlight conversion
RAIN_DECAY_PER_HOUR = 0.85 ** (1 / 24)


def fetch(url: str, **query) -> dict:
    with urlopen(f"{url}?{urlencode(query)}", timeout=60) as response:
        return json.load(response)


def weather(lat: float, lon: float, start: datetime, now: datetime) -> dict[datetime, tuple]:
    """Hourly (temp, rh, rain_mm, radiation) by UTC hour; archive first, forecast API for the last days."""
    archive_end = (now - timedelta(days=7)).date()
    hours: dict[datetime, tuple] = {}
    for url, extra in (
        ("https://archive-api.open-meteo.com/v1/archive",
         {"start_date": start.date().isoformat(), "end_date": archive_end.isoformat()}),
        ("https://api.open-meteo.com/v1/forecast", {"past_days": 8, "forecast_days": 1}),
    ):
        data = fetch(url, latitude=lat, longitude=lon, hourly=HOURLY, timezone="UTC", **extra)["hourly"]
        for i, stamp in enumerate(data["time"]):
            t = datetime.fromisoformat(stamp).replace(tzinfo=timezone.utc)
            row = (data["temperature_2m"][i], data["relative_humidity_2m"][i],
                   data["precipitation"][i], data["shortwave_radiation"][i])
            if t >= start and t <= now and None not in row:
                hours[t] = row
    return hours


def parse_soil(text: str) -> dict:
    keys = {"ph": "soilPh", "n": "nitrogen", "p": "phosphorus", "k": "potassium",
            "ec": "electricalConductivity", "moisture": "soilMoisture"}
    soil = {}
    for part in text.split(","):
        name, value = part.split("=")
        soil[keys[name.strip().lower()]] = float(value)
    missing = set(keys.values()) - set(soil)
    if missing:
        raise SystemExit(f"--soil is missing: {sorted(missing)}")
    return soil


def firebase_call(method: str, base: str, path: list[str], token: str, payload=None):
    body = None if payload is None else json.dumps(payload).encode()
    request = Request(firebase_url(base, path, token), data=body, method=method,
                      headers={"Content-Type": "application/json"})
    try:
        with urlopen(request, timeout=30) as response:
            return json.load(response)
    except HTTPError as error:
        raise SystemExit(f"Firebase {method} /{'/'.join(path)} failed: {error.read().decode(errors='replace')}")
    except URLError as error:
        raise SystemExit(f"Could not reach Firebase: {error.reason}")


def real_soil(base: str, token: str, now: datetime, poll_minutes: float = 0.0, source: str = REAL_DEVICE) -> dict:
    """Median of the real device's trusted soil samples.

    Default: this month's history plus the current reading. That mixes every soil the probe sat in
    this month, so when you moved the probe use poll_minutes: only readings taken from now on count.
    """
    readings = ["devices", source, "readings"]
    trusted = lambda r: r.get("soilPh", 0) > 0 and r.get("nitrogen", 0) > 0  # 0 = untrusted placeholder
    if poll_minutes > 0:
        rows, deadline = [], time.time() + poll_minutes * 60
        print(f"Sampling {source} for {poll_minutes:g} min. Keep the probe in the NEW soil.")
        while time.time() < deadline:
            row = firebase_call("GET", base, readings + ["current"], token) or {}
            if trusted(row) and (not rows or row.get("timestamp") != rows[-1].get("timestamp")):
                rows.append(row)
            next_poll = time.time() + 15  # the firmware publishes every 15 s
            while time.time() < min(next_poll, deadline):
                left = max(0, int(deadline - time.time()))
                last = rows[-1] if rows else {}
                print(f"\r  {left // 60:02d}:{left % 60:02d} left | {len(rows)} readings | "
                      f"pH {last.get('soilPh', '-')} N {last.get('nitrogen', '-')} P {last.get('phosphorus', '-')} "
                      f"K {last.get('potassium', '-')} moist {last.get('soilMoisture', '-')}%   ", end="", flush=True)
                time.sleep(1)
        print()
    else:
        rows = list((firebase_call("GET", base, readings + ["monthly", now.strftime("%Y-%m")], token) or {}).values())
        rows.append(firebase_call("GET", base, readings + ["current"], token) or {})
        rows = [r for r in rows if trusted(r)]
    if not rows:
        raise SystemExit(f"No trusted soil readings on {source}. Is the probe sending?")
    fields = ("soilPh", "nitrogen", "phosphorus", "potassium", "electricalConductivity", "soilMoisture")
    print(f"Real soil baseline from {len(rows)} reading(s) on {source}.")
    return {f: statistics.median(r.get(f, 0) for r in rows) for f in fields}


def build(hours: dict, soil: dict, seed: int, recent_start: datetime) -> dict[str, dict[str, dict]]:
    rng = random.Random(seed)
    order = sorted(hours)
    rain_index, index = {}, 0.0
    for t in order:
        index = index * RAIN_DECAY_PER_HOUR + hours[t][2]
        rain_index[t] = index
    mean, spread = statistics.mean(rain_index.values()), statistics.pstdev(rain_index.values()) or 1.0

    months: dict[str, dict[str, dict]] = defaultdict(dict)
    for t in order:
        # Older months: four spread samples a day. The last --recent-days: every hour, like the firmware.
        if t.hour not in HOURS_PER_DAY and t < recent_start:
            continue
        temp, rh, _, radiation = hours[t]
        z = max(-2.0, min(3.0, (rain_index[t] - mean) / spread))
        # ponytail: crude rain -> moisture link; swap for a real water balance if it must be defensible
        moisture = max(5.0, min(90.0, soil["soilMoisture"] * (1 + 0.25 * z)))
        months[t.strftime("%Y-%m")][t.strftime("%Y-%m-%d_%H-00-00")] = {
            "temperature": round(temp, 1),
            "humidity": round(max(0.0, min(100.0, rh)), 1),
            "soilMoisture": round(moisture, 1),
            "soilPh": round(max(0.0, min(14.0, soil["soilPh"] + rng.gauss(0, 0.05))), 1),
            "nitrogen": max(0, round(soil["nitrogen"] * (1 + rng.gauss(0, 0.02)))),
            "phosphorus": max(0, round(soil["phosphorus"] * (1 + rng.gauss(0, 0.02)))),
            "potassium": max(0, round(soil["potassium"] * (1 + rng.gauss(0, 0.02)))),
            "lightIntensity": round(radiation * LUX_PER_WM2),
            "timestamp": int(t.timestamp() * 1000),
            "electricalConductivity": max(0, round(soil["electricalConductivity"] * (1 + rng.gauss(0, 0.03)))),
        }
    return months


def summary(months: dict, hours: dict) -> None:
    rain = defaultdict(float)
    for t, row in hours.items():
        rain[t.strftime("%Y-%m")] += row[2]
    print(f"{'month':8} {'samples':>7} {'temp C':>7} {'RH %':>6} {'moist %':>8} {'rain mm':>8}  season")
    for month in sorted(months):
        rows = list(months[month].values())
        avg = lambda key: statistics.mean(r[key] for r in rows)
        print(f"{month:8} {len(rows):7} {avg('temperature'):7.1f} {avg('humidity'):6.1f} "
              f"{avg('soilMoisture'):8.1f} {rain[month]:8.0f}  {'wet' if rain[month] >= 200 else 'dry' if rain[month] < 100 else 'transition'}")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--device-id", help="default esp32-demo-01, or a free 4-digit PIN with --link-account")
    parser.add_argument("--source-device", default=REAL_DEVICE,
                        help="device whose probe is sampled for the soil baseline (your paired PIN once paired)")
    parser.add_argument("--export", metavar="FILE",
                        help="also save the generated data as JSON in Firebase shape (works in a dry run)")
    parser.add_argument("--link-account", action="store_true",
                        help="also set users/<uid>/deviceId so the signed-in account shows this device")
    parser.add_argument("--recent-days", type=int, default=31, help="days before now stored hourly (default 31)")
    parser.add_argument("--lat", type=float, default=11.10, help="Mapili, San Enrique, Iloilo")
    parser.add_argument("--lon", type=float, default=122.74)
    parser.add_argument("--start", help="first month, YYYY-MM (default: 8 months back)")
    parser.add_argument("--poll-minutes", type=float, default=0.0,
                        help="sample the real probe for this long instead of using this month's history")
    parser.add_argument("--overwrite", action="store_true", help="allow writing to a device that already has data")
    parser.add_argument("--soil", help="ph=..,n=..,p=..,k=..,ec=..,moisture=.. (skips reading the real device)")
    parser.add_argument("--email", help="Firebase user; password is prompted")
    parser.add_argument("--write", action="store_true", help="write to Firebase (default is a dry run)")
    parser.add_argument("--seed", type=int, default=1)
    args = parser.parse_args()

    if args.link_account and not (args.email and args.write):
        parser.error("--link-account needs --email and --write")
    if args.device_id == REAL_DEVICE:
        parser.error(f"refusing to write to the real device {REAL_DEVICE}; pick another --device-id")
    if not args.soil and not args.email:
        parser.error("give --soil or --email so the soil values are known")
    if args.write and not args.email:
        parser.error("--write needs --email (the Firebase rules require a signed-in user)")

    now = datetime.now(timezone.utc).replace(minute=0, second=0, microsecond=0)
    if args.start:
        start = datetime.strptime(args.start, "%Y-%m").replace(tzinfo=timezone.utc)
    else:
        months_back = now.year * 12 + now.month - 1 - 8
        start = datetime(months_back // 12, months_back % 12 + 1, 1, tzinfo=timezone.utc)
    base, api_key = load_firebase_config()

    token, uid = "", ""
    if args.email:
        password = getpass.getpass(f"Password for {args.email}: ")
        try:
            signed_in = http_json(f"{AUTH_URL}:signInWithPassword?key={quote(api_key, safe='')}",
                                  {"email": args.email, "password": password, "returnSecureToken": True})
            token, uid = signed_in["idToken"], signed_in["localId"]
        except HTTPError as error:
            raise SystemExit(f"Sign-in failed: {error.read().decode(errors='replace')}")

    soil = parse_soil(args.soil) if args.soil else real_soil(base, token, now, args.poll_minutes, args.source_device)
    print("Soil baseline:", {k: round(v, 2) for k, v in soil.items()})

    hours = weather(args.lat, args.lon, start, now)
    months = build(hours, soil, args.seed, now - timedelta(days=args.recent_days))
    summary(months, hours)

    newest = max(months[max(months)].values(), key=lambda r: r["timestamp"])
    print("Latest sample:", json.dumps(newest))
    if args.export:
        shape = {"devices": {args.device_id or "esp32-demo-01": {
            "readings": {"monthly": months, "current": newest},
            "status": {"online": False, "lastSeen": newest["timestamp"],
                       "firmware": "demo-seed (not a real device)", "sensors": [], "simulated": True}}}}
        with open(args.export, "w", encoding="utf-8") as handle:
            json.dump(shape, handle, indent=1)
        print(f"Saved {sum(len(v) for v in months.values())} samples to {args.export}")
    if not args.write:
        print(f"\nDry run: nothing written. Add --write to send this to /devices/{args.device_id or 'esp32-demo-01 (or a free PIN with --link-account)'}.")
        return 0

    device_id = args.device_id
    if not device_id and args.link_account:
        # The app skips PINs that already have a device, so a free one is safe to claim.
        while True:
            device_id = f"{random.randrange(10_000):04d}"
            if not firebase_call("GET", base, ["devices", device_id, "status"], token):
                break
    device_id = device_id or "esp32-demo-01"
    root = ["devices", device_id]
    if not args.overwrite and firebase_call("GET", base, root + ["status"], token):
        raise SystemExit(f"{device_id} already has data. Pick a new --device-id or pass --overwrite.")
    for month, samples in months.items():
        firebase_call("PATCH", base, root + ["readings", "monthly", month], token, samples)
        print(f"wrote {month}: {len(samples)} samples")
    firebase_call("PUT", base, root + ["readings", "current"], token, newest)
    firebase_call("PUT", base, root + ["status"], token, {
        "online": False, "lastSeen": newest["timestamp"], "firmware": "demo-seed (not a real device)",
        "sensors": [], "simulated": True})
    if args.link_account:
        firebase_call("PUT", base, ["users", uid, "deviceId"], token, device_id)
        print(f"Linked {args.email} to device {device_id}. Open the app: the history is there.")
        print(f"To pair the ESP32 later: Settings > Pair a device (the PIN stays {device_id}).")
    print("Done.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
