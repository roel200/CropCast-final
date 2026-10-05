"""Check that the two LLM providers behind worker/index.js really answer.

Keys are read from the environment (or prompted, never printed), so they are not stored anywhere:
    $env:OPENROUTER_KEY = "sk-or-..."; $env:XKIRO_KEY = "..."
    python scripts/test_ai_providers.py
Test a deployed worker as well:
    python scripts/test_ai_providers.py --worker https://cropcast-advice.<you>.workers.dev
"""

from __future__ import annotations

import argparse
import getpass
import json
import os
import re
import time
import urllib.error
import urllib.request

FACTS = (
    "Farm: Mapili, San Enrique, Iloilo. Top crop: Rice (Lowland), score 86%, S1 highly suitable, about 85k PHP/ha gross. "
    "Main limit: season rainfall 72-274 mm/month expected (best 290-480). Soil salinity ECe 3.2 dS/m. Soil pH 7.3. "
    "Nitrogen 158 mg/kg OK, phosphorus 221 high, potassium 506 high. Soil texture heavy clay."
)
SYSTEM = ("You are an agronomy assistant for Philippine smallholder farmers. Explain the crop recommendation facts "
          "you are given in plain, short sentences, in English. Use only the numbers and crops in the facts. "
          "No markdown, max 150 words.")


def post(url: str, key: str, body: dict) -> tuple[int, dict | str, float]:
    request = urllib.request.Request(url, data=json.dumps(body).encode(), method="POST",
                                     headers={"Content-Type": "application/json", "Authorization": f"Bearer {key}"})
    started = time.time()
    try:
        with urllib.request.urlopen(request, timeout=40) as response:
            return response.status, json.load(response), time.time() - started
    except urllib.error.HTTPError as error:
        return error.code, error.read().decode(errors="replace")[:300], time.time() - started
    except Exception as error:  # network, timeout
        return 0, str(error)[:300], time.time() - started


def report(name: str, status: int, data, seconds: float) -> None:
    if status != 200:
        print(f"[FAIL] {name}: HTTP {status or 'no response'} after {seconds:.1f}s -> {data}")
        return
    text = re.sub(r"<think>[\s\S]*?</think>", "", (data.get("choices") or [{}])[0].get("message", {}).get("content") or "").strip()
    if not text:
        print(f"[FAIL] {name}: HTTP 200 but empty reply ({seconds:.1f}s) -> {str(data)[:200]}")
        return
    words = len(text.split())
    notes = []
    if words > 170:
        notes.append("longer than the 150-word limit")
    if re.search(r"[*#`]", text):
        notes.append("contains markdown")
    print(f"[ OK ] {name}: {seconds:.1f}s, {words} words, model {data.get('model', '?')}" + (f" ({'; '.join(notes)})" if notes else ""))
    print("       " + text.replace("\n", " ")[:600])


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--worker", help="also POST to this deployed worker URL")
    parser.add_argument("--openrouter-models", default="google/gemma-4-26b-a4b-it:free,qwen/qwen3.8-27b:free")
    parser.add_argument("--xkiro-url", default="https://api.xkiro.com/v1")
    parser.add_argument("--xkiro-model", default="mistralai/mistral-small-2603")
    args = parser.parse_args()
    messages = [{"role": "system", "content": SYSTEM}, {"role": "user", "content": FACTS}]
    common = {"messages": messages, "max_tokens": 500, "temperature": 0.3}

    key = os.environ.get("OPENROUTER_KEY") or getpass.getpass("OpenRouter key (empty to skip): ")
    if key:
        for model in args.openrouter_models.split(","):
            report(f"OpenRouter {model}", *post("https://openrouter.ai/api/v1/chat/completions", key, {**common, "model": model}))
    key = os.environ.get("XKIRO_KEY") or getpass.getpass("xKiro key (empty to skip): ")
    if key:
        report(f"xKiro {args.xkiro_model}", *post(args.xkiro_url.rstrip("/") + "/chat/completions", key, {**common, "model": args.xkiro_model}))

    if args.worker:
        started = time.time()
        request = urllib.request.Request(args.worker, data=json.dumps({"facts": FACTS, "language": "English"}).encode(),
                                         method="POST", headers={"Content-Type": "application/json"})
        try:
            with urllib.request.urlopen(request, timeout=60) as response:
                print(f"[ OK ] worker: {time.time() - started:.1f}s -> {json.load(response).get('text', '')[:400]}")
        except urllib.error.HTTPError as error:
            print(f"[FAIL] worker: HTTP {error.code} -> {error.read().decode(errors='replace')[:200]}")
        except Exception as error:
            print(f"[FAIL] worker: {error}")


if __name__ == "__main__":
    main()
