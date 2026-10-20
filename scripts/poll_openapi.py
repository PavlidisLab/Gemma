#!/usr/bin/env python3
"""Poll /rest/v2/openapi.json from the instant a Gemma instance opens its port.

Reproduces (or refutes) the startup race that stripped the ``servers`` list from
the served spec: Swagger's OpenApiResource used to answer from its own re-read of
the spec, so a request landing while OpenApiFactory's async build was still
running could pin an undecorated copy in Swagger's cache for the life of the JVM.
The tell is a response whose ``servers`` is absent -- Swagger UI then falls back
to the page origin and "Try it out" loses the /rest/v2 base path.

Start this BEFORE the instance, so the first request lands as early as possible:

    scripts/poll_openapi.py                              # localhost:8383 (IntelliJ Gemma Rest)
    scripts/poll_openapi.py --base http://frink.msl.ubc.ca:8080
    scripts/poll_openapi.py --duration 300 --interval 0.5

Exit status is 1 if any response lacked ``servers``, so it can gate a check.
"""

import argparse
import gzip
import json
import signal
import sys
import time
import urllib.error
import urllib.request

DEFAULT_BASE = "http://localhost:8383"


def fetch(url, timeout):
    """GET url, returning (http_status, body_bytes_on_the_wire, decoded_text).

    Asks for gzip because the spec is ~600 kB uncompressed; the served response is
    normally compressed, and the wire size is itself a useful signal (it changing
    between responses means different spec objects are being served).
    """
    req = urllib.request.Request(url, headers={"Accept-Encoding": "gzip"})
    with urllib.request.urlopen(req, timeout=timeout) as r:
        raw = r.read()
        if r.headers.get("Content-Encoding") == "gzip":
            text = gzip.decompress(raw).decode("utf-8")
        else:
            text = raw.decode("utf-8")
        return r.status, len(raw), text


def summarize(text):
    """Pull the diagnostic fields out of a spec body."""
    spec = json.loads(text)
    servers = spec.get("servers") or []
    params = [
        p
        for path in spec.get("paths", {}).values()
        for op in path.values()
        if isinstance(op, dict)
        for p in (op.get("parameters") or [])
        if "FilterArg" in (p.get("schema") or {}).get("$ref", "")
        or "SortArg" in (p.get("schema") or {}).get("$ref", "")
    ]
    return {
        "servers": [s.get("url") for s in servers],
        "paths": len(spec.get("paths", {})),
        # the other half of OpenApiFactory's post-read work; absent for the same
        # reason `servers` would be, so it corroborates a bad response
        "examples": sum(1 for p in params if "example" in p),
        "params": len(params),
    }


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--base", default=DEFAULT_BASE, help=f"instance base URL (default {DEFAULT_BASE})")
    ap.add_argument("--interval", type=float, default=1.0, help="seconds between polls (default 1.0)")
    ap.add_argument("--duration", type=float, default=180.0, help="stop after N seconds (default 180; Ctrl-C also works)")
    ap.add_argument("--timeout", type=float, default=30.0, help="per-request timeout (default 30)")
    args = ap.parse_args()

    url = args.base.rstrip("/") + "/rest/v2/openapi.json"
    print(f"polling {url} every {args.interval}s for up to {args.duration:.0f}s -- Ctrl-C to stop")
    print("(start the instance now if it isn't up yet)\n")

    signal.signal(signal.SIGINT, lambda *_: sys.exit(render_verdict(state)))
    state = {"responses": 0, "bad": 0, "sizes": set(), "first": None, "waiting_logged": False}
    deadline = time.monotonic() + args.duration

    while time.monotonic() < deadline:
        started = time.monotonic()
        try:
            status, wire, text = fetch(url, args.timeout)
        except (urllib.error.URLError, ConnectionError, OSError) as e:
            # port not open yet, or the app is still deploying
            if not state["waiting_logged"]:
                print(f"  waiting for {args.base} ... ({e})")
                state["waiting_logged"] = True
            time.sleep(args.interval)
            continue

        if state["first"] is None:
            state["first"] = started
            print(f"  first response after {time.strftime('%H:%M:%S')} -- clock below is seconds since then\n")
        elapsed = started - state["first"]
        state["responses"] += 1
        state["sizes"].add(wire)

        try:
            info = summarize(text)
        except (json.JSONDecodeError, UnicodeDecodeError) as e:
            print(f"t+{elapsed:6.1f}s  HTTP {status}  {wire:>8,} B  NOT JSON: {e}")
            state["bad"] += 1
            time.sleep(args.interval)
            continue

        ok = bool(info["servers"])
        if not ok:
            state["bad"] += 1
        flag = "ok  " if ok else "BAD "
        servers = info["servers"][0] if info["servers"] else "<none>"
        extra = f" (+{len(info['servers']) - 1} more)" if len(info["servers"]) > 1 else ""
        print(
            f"t+{elapsed:6.1f}s  HTTP {status}  {wire:>8,} B  {flag}"
            f"servers={len(info['servers'])} {servers}{extra}  "
            f"paths={info['paths']}  examples={info['examples']}/{info['params']}"
        )

        # took longer than the interval (e.g. blocked on the spec build): poll again at once
        time.sleep(max(0.0, args.interval - (time.monotonic() - started)))

    sys.exit(render_verdict(state))


def render_verdict(state):
    print()
    if not state["responses"]:
        print("no responses -- instance never answered; nothing concluded")
        return 1
    print(f"{state['responses']} responses, {state['bad']} without a servers list")
    if len(state["sizes"]) > 1:
        # distinct bodies over one process lifetime = more than one spec object was built
        print(f"wire sizes seen: {sorted(state['sizes'])} -- more than one spec object was served")
    if state["bad"]:
        print("VERDICT: reproduced -- at least one response would break Try it out")
        return 1
    print("VERDICT: clean -- every response carried servers")
    return 0


if __name__ == "__main__":
    main()
