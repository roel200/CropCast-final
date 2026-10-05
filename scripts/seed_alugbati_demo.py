"""Seed an INVENTED Alugbati-friendly sensor history to a separate demo device.

Not your soil: every value is generated inside the Alugbati profile (see
data/processed/crop_requirements_selected.json) so the Seeds tab's monthly
sensor engine ranks Alugbati first. It shows the engine works, nothing more.
The Crop Recommendation tab ignores most of it (it uses climate and soil maps).

    python scripts/seed_alugbati_demo.py                      # dry run
    python scripts/seed_alugbati_demo.py --email you@x.com --write
View it:  .\\gradlew.bat installDebug "-Pcropcast.deviceId=esp32-demo-alugbati"
"""

from __future__ import annotations

import argparse
import getpass
import json
import sys
from datetime import datetime, timedelta, timezone
from urllib.error import HTTPError
from urllib.parse import quote

from seed_crop_test_data import distinctive_values, make_reading
from simulate_firebase_sensor import AUTH_URL, PROJECT_ROOT, http_json, load_firebase_config, put_json

SAMPLES_PER_MONTH = 8  # SeedRecommendationEngine.MIN_MONTHLY_SAMPLES


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--device-id", default="esp32-demo-alugbati")
    parser.add_argument("--start", default="2026-01", help="first month, YYYY-MM")
    parser.add_argument("--email", help="Firebase user; password is prompted")
    parser.add_argument("--write", action="store_true", help="write to Firebase (default is a dry run)")
    args = parser.parse_args()

    if args.device_id.startswith("esp32-field") or args.device_id == "esp32-demo-01":
        parser.error(f"refusing to overwrite {args.device_id}; use a separate demo device id")
    if args.write and not args.email:
        parser.error("--write needs --email (the Firebase rules require a signed-in user)")

    profiles = json.loads((PROJECT_ROOT / "data" / "processed" / "crop_requirements_selected.json")
                          .read_text(encoding="utf-8"))["crops"]
    target = next(p for p in profiles if p["name"] == "Alugbati")
    base_values = distinctive_values(target, profiles)

    now = datetime.now(timezone.utc)
    month = datetime.strptime(args.start, "%Y-%m").replace(day=1, hour=9, tzinfo=timezone.utc)
    months: dict[str, dict[str, dict]] = {}
    while month <= now:
        samples = {}
        for i in range(SAMPLES_PER_MONTH):
            at = month + timedelta(hours=i)
            if at <= now:
                samples[at.strftime("%Y-%m-%d_%H-%M-%S")] = make_reading(base_values, target, at, i)
        months[month.strftime("%Y-%m")] = samples
        month = (month.replace(day=28) + timedelta(days=4)).replace(day=1)  # next month

    newest = max((s for m in months.values() for s in m.values()), key=lambda r: r["timestamp"])
    print("Sensor values (inside the Alugbati profile):", {k: round(v, 1) for k, v in base_values.items()})
    print(f"{len(months)} months, {SAMPLES_PER_MONTH} samples each ({', '.join(months)})")
    print("Latest sample:", json.dumps(newest))
    if not args.write:
        print(f"\nDry run: nothing written. Add --email and --write to send this to /devices/{args.device_id}.")
        return 0

    base, api_key = load_firebase_config()
    password = getpass.getpass(f"Password for {args.email}: ")
    try:
        token = http_json(f"{AUTH_URL}:signInWithPassword?key={quote(api_key, safe='')}",
                          {"email": args.email, "password": password, "returnSecureToken": True})["idToken"]
    except HTTPError as error:
        raise SystemExit(f"Sign-in failed: {error.read().decode(errors='replace')}")

    root = ["devices", args.device_id]
    for key, samples in months.items():
        put_json(base, root + ["readings", "monthly", key], samples, token)
        print(f"wrote {key}: {len(samples)} samples")
    put_json(base, root + ["readings", "current"], newest, token)
    put_json(base, root + ["status"], {
        "online": False, "lastSeen": newest["timestamp"], "firmware": "alugbati-demo (simulated)",
        "sensors": [], "simulated": True}, token)
    print("Done.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
