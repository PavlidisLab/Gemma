#!/usr/bin/env python3
"""Flush Gemma's server-side caches through the admin REST surface.

Why this exists: a CLI rebuild (updateEe2c, updateOntologyRelations,
updateExternalRelations) runs in its own JVM, so gemma-rest learns nothing from it
and keeps serving the rows it cached before. Twice now a rebuild has looked like it
did nothing for exactly that reason. The eviction is a two-call chore behind a Bearer
token, which is enough friction to get skipped -- hence a script.

Credentials come from the macOS Keychain (GEMMA_USERNAME / GEMMA_PASSWORD), never a
.env file; a pre-set environment variable wins if you have one exported.

    scripts/evict_caches.py                      # after a relation/EE2C rebuild (default)
    scripts/evict_caches.py --list               # show every cache + its stats, change nothing
    scripts/evict_caches.py --cache OntologyService.search --cache OntologyService.parents
    scripts/evict_caches.py --all                # flush everything
    scripts/evict_caches.py --base http://localhost:8080

Exit status is non-zero if any requested cache could not be flushed, so this is safe
to chain after a rebuild with &&.
"""
import argparse
import json
import os
import subprocess
import sys
import urllib.error
import urllib.request

DEFAULT_BASE = "https://gemma2.msl.ubc.ca"

# What a Hibernate-backed rebuild leaves stale. The query-results region holds the rows
# themselves; the update-timestamps region is what Hibernate consults to decide whether a
# cached query is still valid, so flushing the first without the second can repopulate
# from a timestamp that says the table has not moved.
AFTER_REBUILD = ["default-query-results-region", "default-update-timestamps-region"]


def keychain(*services):
    """First Keychain entry that resolves, mirroring the shell keychain_export helper."""
    for service in services:
        if not service:
            continue
        out = subprocess.run(["security", "find-generic-password", "-s", service, "-w"],
                             capture_output=True, text=True)
        if out.returncode == 0 and out.stdout.strip():
            return out.stdout.strip()
    return None


def credential(var, *services):
    if os.environ.get(var):
        return os.environ[var]
    val = keychain(os.environ.get(f"{var}_KEYCHAIN_ENTRY"), *services)
    if not val:
        sys.exit(f"ERROR: no {var} in the environment or the Keychain (tried: "
                 f"{', '.join(s for s in services if s)}). Override the entry name with "
                 f"{var}_KEYCHAIN_ENTRY=<name>.")
    return val


def request(method, url, token=None, timeout=120):
    req = urllib.request.Request(url, method=method)
    if token:
        req.add_header("Authorization", f"Bearer {token}")
    try:
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            body = resp.read()
            return resp.status, (json.loads(body) if body else None)
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode("utf-8", "replace")


def login(base, username, password):
    # JSON body, not form-encoded -- the form-encoded spelling fails with a bare 401.
    req = urllib.request.Request(
        f"{base}/rest/v2/login",
        data=json.dumps({"username": username, "password": password}).encode(),
        headers={"Content-Type": "application/json"}, method="POST")
    try:
        with urllib.request.urlopen(req, timeout=30) as resp:
            token = (json.loads(resp.read()).get("data") or {}).get("token")
    except urllib.error.HTTPError as e:
        sys.exit(f"ERROR: login to {base} failed ({e.code}): {e.read().decode('utf-8','replace')[:200]}")
    if not token:
        sys.exit(f"ERROR: login to {base} returned no token.")
    return token


def list_caches(base, token):
    status, body = request("GET", f"{base}/rest/v2/admin/caches", token)
    if status != 200:
        sys.exit(f"ERROR: GET /admin/caches returned {status}: {str(body)[:300]}")
    data = body.get("data") if isinstance(body, dict) else body
    if isinstance(data, dict):
        data = data.get("caches") or data.get("data") or []
    return data or []


def main():
    p = argparse.ArgumentParser(description=__doc__,
                                formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument("--base", default=os.environ.get("GEMMA_BASE_URL") or
                   keychain("GEMMA_BASE_URL") or DEFAULT_BASE,
                   help=f"Gemma REST base URL (default: keychain GEMMA_BASE_URL, else {DEFAULT_BASE})")
    p.add_argument("--cache", action="append", default=[], metavar="NAME",
                   help="flush this cache; repeatable. Overrides the after-rebuild default.")
    p.add_argument("--all", action="store_true", help="flush every cache (DELETE /admin/caches)")
    p.add_argument("--list", action="store_true", help="list caches and exit, changing nothing")
    args = p.parse_args()

    base = args.base.rstrip("/")
    token = login(base, credential("GEMMA_USERNAME", "GEMMA_USERNAME", "gemma-username"),
                  credential("GEMMA_PASSWORD", "GEMMA_PASSWORD", "gemma-password"))
    print(f"authenticated against {base}")

    caches = list_caches(base, token)
    known = {c.get("name") for c in caches if isinstance(c, dict)}

    if args.list:
        print(f"\n{len(caches)} caches:")
        for c in sorted(caches, key=lambda x: str(x.get("name"))):
            hits, misses = c.get("hits"), c.get("misses")
            stats = f"  hits={hits} misses={misses}" if hits is not None else ""
            print(f"  {c.get('name')}{stats}")
        return 0

    if args.all:
        status, body = request("DELETE", f"{base}/rest/v2/admin/caches", token)
        ok = status in (200, 204)
        print(f"{'flushed' if ok else 'FAILED'} all {len(caches)} caches ({status})")
        return 0 if ok else 1

    targets = args.cache or AFTER_REBUILD
    if not args.cache:
        print("flushing the after-rebuild set (pass --cache NAME to choose your own)")

    failed = 0
    for name in targets:
        if known and name not in known:
            # Not fatal: the region may simply not have been created yet on a cold server.
            print(f"  {name}: NOT REGISTERED on this server -- skipped")
            continue
        status, body = request("DELETE", f"{base}/rest/v2/admin/caches/{name}", token)
        if status in (200, 204):
            print(f"  {name}: flushed")
        else:
            print(f"  {name}: FAILED ({status}) {str(body)[:200]}")
            failed += 1
    if failed:
        print(f"\n{failed} cache(s) could not be flushed.", file=sys.stderr)
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
