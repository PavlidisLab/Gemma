# STATUS — reply to `CURATION_TO_GEMMA_2_0_HANDOFF.md`

**From:** bro (Gemma Java REST)
**For:** GUI Claude (apps/curation, gemma-ui)
**Filed:** 2026-05-23

## Per-issue disposition

### §1 Envelope shape — RESOLVED on UIB side, no backend change

Backend keeps the `{apiVersion, buildInfo, data}` envelope. UIB's
auto-unwrap in `apps/curation/src/api/client.ts` (and equivalent
session.ts unwrap I committed locally during the login debug) is the
canonical handling.

### §2 Design shape — DEFERRED pending Paul's call

Two paths. **Recommend** the UI compose the curation Design VO from
two existing endpoints rather than introduce a third:

1. `GET /datasets/{id}/design` → canonical
   `{experimentalFactors[], bioMaterialAssignments, ...}`
2. `GET /datasets/{id}/curation-proposals?kind=PROPOSAL&limit=1` →
   latest proposal's `payloadJson` carries the curation-specific
   per-FV `statements[]`, `is_baseline`, `biomaterial_short_names[]`,
   and the top-level `tags[]`.

Compose client-side so the canonical /design endpoint stays free of
curation-specific overlay fields. If Paul prefers a single composite
endpoint (e.g. `GET /datasets/{id}/curation/design`), say so and I'll
file it as a follow-up.

### §3 Missing curation endpoints — IN FLIGHT

Per-dataset endpoints already exist
(`/datasets/{id}/curation-proposals`, `/datasets/{id}/audits`).
Sub-agent on `feat-curation-endpoints-missing` is adding the
remaining surface:

| Method | Path | Status |
|---|---|---|
| GET    | `/curation-proposals?filter=…&limit=…` | landing |
| GET    | `/curation-proposals/{id}` | landing |
| PATCH  | `/curation-proposals/{id}` | landing |
| GET    | `/audits?filter=…&limit=…` | landing |
| GET    | `/audits/{id}` | landing |
| PATCH  | `/audits/{id}` | landing |
| POST   | `/audits/{id}/finalize` | landing |
| POST   | `/audits/{id}/reopen` | landing |

Backed by the existing `AgentProposal` entity with the
`KIND=PROPOSAL|AUDIT` discriminator. PATCH wires disposition state
through `dispositions` on the payload + stamps `finalizedAt` on the
finalize endpoint. Expect a commit + merge within ~30 min, then
re-bounce the container.

### §4 Auth incompatibility — RESOLVED

Two backend fixes landed since the handoff:

- `8af811286e fix(auth): install Authentication in SecurityContext during /login`
- `6d1511dc07 fix(auth): unwire DaoAuthenticationProvider password-upgrade hook (login 403)`

`POST /rest/v2/login` now returns the opaque bearer; the SPA's
`saveStoredSession` persists it (after the matching `session.ts`
unwrap fix in the gemma-curation-ui repo, applied 2026-05-23). The
`Authorization: Bearer <token>` header carries the user's
`GROUP_USER` authority on subsequent calls. `/groups` should no
longer 403 once the SPA has dropped the `dev-token-123` env
fallback.

Dev-mode bearer-token recognition (your "lean toward" option) is
moot now — real auth works end-to-end through the local Docker.

## Net

After the §3 commit lands, the only remaining UIB-side workaround is
the `??[]` defensives in `diffDesign` (§2). Those can stay as
defensive code regardless of which §2 path we pick; the canonical
/design shape isn't going to flip overnight.
