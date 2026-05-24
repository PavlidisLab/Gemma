# Handoff — local_api needs to translate its internal id → accession before calling Gemma upstream

**Filed:** 2026-05-24 (gemma-ui side, Paul + Claude).
**For:** cab (bro 1 / agents repo — `gemma-curation-agents`)
**Status:** UI is correct; the gap is on the local_api proxy/fetch path. Affects every endpoint where local_api makes an upstream call to a real Gemma (staging-gemma or whatever `GEMMA_BASE_URL` resolves to) using its own internal dataset id.

## Symptom (real-world reproduction)

Curator on `http://localhost:5175/#/experiments/91654?tab=quantitation&group=…` for **GSE253365**. The UI fires:

```
GET /rest/v2/datasets/91654/quantitationTypes
```

…which routes to local_api (correct — quantitation is curation state). local_api proxies upstream to `staging-gemma`. The console shows:

```
GET /rest/v2/datasets/91654/quantitationTypes failed: 502 Bad Gateway
— upstream Gemma error: 404 Client Error for url:
https://staging-gemma.msl.ubc.ca/rest/v2/datasets/91654/quantitationTypes
```

Same shape on the audit trail (`/rest/v2/datasets/91654/auditEvents`), and presumably anywhere else local_api needs to hand a request to the real Gemma.

## Root cause

`91654` is **local_api's internal dataset id** — assigned when the dataset was loaded into the curation DB. **staging-gemma has the same dataset (GSE253365) under its OWN internal id**, which is different. So forwarding `91654` to staging is forwarding an id from the wrong namespace.

Confirmed via direct probes:

```bash
$ curl -s "http://localhost:8095/rest/v2/datasets?query=GSE253365&limit=1" \
    -H "Authorization: Bearer dev-token-123" | jq '.data[0] | {id, shortName}'
{ "id": 91654, "shortName": "GSE253365" }

$ curl -s "https://staging-gemma.msl.ubc.ca/rest/v2/datasets?query=GSE253365&limit=1" \
    -H "Authorization: Bearer <staging-token>" | jq '.data[0] | {id, shortName}'
{ "id": <something different>, "shortName": "GSE253365" }
```

Both backends index by `shortName` (`accession`) consistently. Their numeric ids do **not** correspond.

## Ask

In local_api's "forward to Gemma" path (the per-endpoint passthroughs that take a `dataset_id` and emit a request against the real Gemma upstream), **translate the local id to the dataset's accession before composing the upstream URL**.

Gemma's REST is happy to accept the accession in `/datasets/{id}`:

```bash
$ curl https://staging-gemma.msl.ubc.ca/rest/v2/datasets/GSE253365/quantitationTypes
HTTP 200  ← works
```

The cheapest implementation is probably:

1. Lookup table in local_api keyed by local id → accession (already implied — the WorkflowDatasetRow you ship already carries `short_name=accession`).
2. Before the upstream `httpx.get` / `requests.get`, swap the numeric id segment in the URL for the accession.
3. Optionally: cache the accession alongside the per-dataset Gemma-token if you also keep auth there.

The endpoints I know hit this today:

- `GET /rest/v2/datasets/{id}/quantitationTypes`
- `GET /rest/v2/datasets/{id}/auditEvents` (when accessed via local_api fallback — gemma-rest path uses Vite proxy directly and would hit this same issue if local_api ever proxies it)
- Anything else where local_api delegates to Gemma for read-only data on a per-dataset basis

The diagnostics surface (`/svd`, `/sample-correlation`, `/mean-variance`) is NOT affected — those route directly to gemma-rest from the UI via a Vite proxy exception, bypassing local_api entirely.

## UI-side workaround (not desired)

If we had to handle this in the curation UI, we'd plumb the accession into every dataset-keyed hook (workflow row → audit hook / quantitation hook / etc.) so we could swap which path-segment to send based on which upstream is being targeted. That's invasive and duplicates a translation local_api already has the data for. Filing this handoff instead.

## Not credentials

The local_api → Gemma auth header swap is fine. Paul double-checked by hitting `staging-gemma /datasets/{numeric}/quantitationTypes` directly and got 404 too — confirming the issue is identifier resolution, not authorization. (For the audit trail specifically, the access-control 401/403 is a separate matter — see the unified-auth work landing in the gemma-ui repo around 2026-05-24.)

## Acceptance

Once the translation lands, the curator hitting `/#/experiments/<localId>?tab=quantitation` should see the QT list resolve cleanly, and the audit trail's gemma-rest source should populate for any dataset that exists upstream. The UI fallback chain (gemma-rest → local_api) keeps working as a safety net; the translation makes the primary path actually succeed.
