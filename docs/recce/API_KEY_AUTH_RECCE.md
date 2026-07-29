# API-key admission + layered auth for `/rest/v2` — recce + plan

**Status:** design / plan (no code written). **Date:** 2026-07-29.
**Trigger:** the Gemma REST API is moving *out from behind the lab VPN*. The VPN was
doing implicit network-layer authentication for every caller; once it is gone, every
control it provided has to be re-established at the application layer.

Siblings: [`AUTH_FOR_SPA_RECCE.md`](AUTH_FOR_SPA_RECCE.md) (the Bearer-token/`TokenStore`
login flow this builds on), [`../design/SPRING_SECURITY_7_READINESS.md`](../design/SPRING_SECURITY_7_READINESS.md).

---

## 1. Goals

1. **Admission control + attribution** — require an API key on `/rest/v2` traffic so
   anonymous internet requests are gated and *attributable* (who is calling, how often).
2. **A little more security on top of passwords** — layered, not a replacement. Passwords
   stay; the key and (for humans) a second factor sit around them (defence in depth).
3. **Tier the keys** — an anonymous/public key can *read* public data only; write
   operations require a non-anonymous key or an authenticated user.
4. **Zero interactive burden for non-interactive clients** — the curation agent, CLIs, the
   R client, and the GemBrow backend must never do an interactive login. They carry a
   static key and nothing else.

## 2. The correct framing (why keys alone are not the answer)

An API key is **not inherently more secure than a password** — it is a bearer credential,
a long random string. Its real strengths are: high entropy, no reuse across sites,
per-consumer revocation, and attribution. Its weakness is that it is a long-lived static
secret with no second factor. So "use API keys for admins" does *not*, by itself, address
the worry that admin password auth is too weak for an internet-facing box.

The worry splits into two problems with two different answers:

- **Machine / programmatic access** → **API keys**. Right tool. Removes human passwords
  from scripts, gives revocation + attribution.
- **Interactive human admin access** → the real levers are **MFA (TOTP) + rate-limiting +
  TLS**, not keys.

Both live behind one hard prerequisite (§3).

## 3. Hard prerequisite — TLS

frink currently speaks **plain HTTP on `:8080`** (see user memory `reference_gemma_base_url_keychain`).
Passwords, Bearer session tokens, **and** API keys all cross the wire in cleartext. Exposing
that publicly makes every credential trivially sniffable. **TLS termination in front of
`/rest/v2` is a non-negotiable prerequisite** for anything in this document. Confirm the
reverse proxy terminates TLS before the VPN comes down.

## 4. The layered model, by client type

Three layers, each covering a different failure mode:

- **Layer 1 — API key** (admission): *are you an admitted client?* No valid key → the
  request never reaches the login logic. Static header, sent every request.
- **Layer 2 — password** (something you know): *who are you?*
- **Layer 3 — TOTP** (something you have): *prove it's really you.* **Interactive humans only.**

Applied per client type:

| Client | Credential | Password? | TOTP? | Interactive re-auth frequency |
|---|---|---|---|---|
| Curation agent / CLIs / R client / GemBrow backend | API key only (static header) | no | no | **never** — key rides every request |
| Admin browser login | password + TOTP → session token | yes | yes (once per device) | ~once/day, or ~once/month/device with remember-device |
| Anonymous public reads | anonymous API key | no | no | never |

TOTP lives *only* in the human `/login` exchange. Non-interactive clients do zero
interactive auth, forever — this is the whole point of the machine-key tier, and it is why
the curation agent example does not conflict with adding MFA.

### Avoiding re-auth fatigue for humans

TOTP is entered **once per session at `/login`**, not per request. The existing
`TokenStore` (see `AUTH_FOR_SPA_RECCE.md`) issues an opaque Bearer token with an **8h
sliding TTL**, so an active curator is never re-prompted mid-day. Two softeners:

1. **Longer / refresh-backed sessions** — tune the sliding TTL or add a refresh token so
   the session renews silently while the device stays active.
2. **Remember-this-device** — after the first TOTP on a device, drop a trusted-device
   marker so subsequent logins on *that* device are password-only for e.g. 30 days; TOTP
   re-triggers only on a new/unknown device.

Trade-off: longer sessions widen the window if a token leaks — acceptable because tokens
are server-side revocable and TLS keeps them off the wire.

## 5. Securing the write-capable machine key (the real risk)

The curation agent **writes** to Gemma, so it needs a write-scoped, non-expiring,
MFA-less key. That is the credential most worth hardening. Machine credentials that cannot
do MFA are secured differently:

- **Dedicated service account** — the key is bound to e.g. a `curation-agent` User, never a
  human admin account. Blast radius and audit trail stay clean.
- **Least privilege via `SCOPE`** — the key grants only the operations the client performs
  (curation writes), not full admin.
- **Resolved from a secret store, never hardcoded** — the agent pulls its key from the
  macOS Keychain (`security find-generic-password`), same as `GEMMA_PASSWORD` today.
  Rotating = swap the Keychain entry + revoke the old key, no code change.
- **Instantly revocable + rotatable**; optionally **IP-allowlisted** to known agent hosts.
- **Monitored** — `LAST_USED` + the analytics dimension make an anomalous key (new IP,
  volume spike) visible.

## 6. Key mechanics

- **Transport:** header `X-Gemma-API-Key: <opaque>` (kept off `Authorization` so it
  composes with Bearer/Basic login) + optional `?apiKey=` for browser GETs.
- **Format:** visible prefix, e.g. `gmma_sk_<random>`, so keys are greppable in logs and
  catchable by GitHub secret-scanning / leak scanners. **Log only the prefix**, never the
  full key.
- **At rest:** store the **SHA-256 hash** only; show plaintext once at issuance. (SHA-256
  is fine — the input is already high-entropy; no bcrypt needed.)
- **Tiering:** encoded by the `OWNER_CONTACT_FK`. Null → anonymous tier → anonymous
  authorities → writes 403. Non-null → resolves the owner's authorities
  (`GROUP_USER` / `GROUP_ADMIN`) via the existing `UserManager` + role hierarchy.
- **Issuance:** **on request** (admin-issued). Each key tied to a real contact + purpose
  label — gives the audit trail and clean per-key revocation.

## 7. What already exists (adopt, don't reinvent)

All in `gemma-rest/src/main/java/ubic/gemma/rest/security/`:

- `RestSecurityConfig` — the Spring Security 6 filter chain for `/rest/v2/**` (`@Order(1)`).
  Ends today with `.anyRequest().permitAll()` + anonymous authority `IS_AUTHENTICATED_ANONYMOUSLY`.
- `BearerTokenAuthenticationFilter` + `TokenStore` — opaque-token pattern (no-op if header
  absent). The shape to clone for the key filter.
- `AuthWebService` — `/login` mints a session token, `/logout`, `/me`.
- `AnalyticsProvider` + `AnalyticsApplicationEventListener` — already fires a
  `gemma_rest_api_access` event per request; the tracking hook to extend with an
  `apiKeyId` dimension.
- ACL already blocks anonymous writes at the service layer (~88 write endpoints across 13
  WebServices) — the method-level block in §8 C3 is a cheap, explicit reinforcement, not a
  new authorization system.

## 8. Table shape

Two independent dialect sequences (same logical change gets different `V` numbers):
`db/migration/mysql/V23__api_key.sql`, `db/migration/h2/V24__api_key.sql`.

```sql
CREATE TABLE API_KEY (
    ID               BIGINT       NOT NULL AUTO_INCREMENT,
    KEY_HASH         VARCHAR(64)  NOT NULL,          -- SHA-256 hex; plaintext never stored
    KEY_PREFIX       VARCHAR(16)  NOT NULL,          -- visible prefix for logs / identification
    OWNER_CONTACT_FK BIGINT       NULL,              -- NULL => anonymous/public tier
    LABEL            VARCHAR(255) NULL,              -- purpose: "curation-agent", "GemBrow", "R-client-jsmith"
    SCOPE            VARCHAR(32)  NOT NULL,           -- e.g. READ | WRITE | ADMIN (least privilege)
    CREATED          DATETIME     NOT NULL,
    LAST_USED        DATETIME     NULL,              -- bumped by the auth filter (tracking)
    EXPIRES          DATETIME     NULL,              -- NULL = no expiry (anonymous/agent keys)
    REVOKED          DATETIME     NULL,              -- soft-revoke; NULL = active
    PRIMARY KEY (ID),
    UNIQUE KEY UQ_API_KEY_HASH (KEY_HASH),
    CONSTRAINT FK_API_KEY_OWNER FOREIGN KEY (OWNER_CONTACT_FK) REFERENCES CONTACT (ID)
);
CREATE INDEX IX_API_KEY_OWNER ON API_KEY (OWNER_CONTACT_FK);
```

## 9. Implementation plan (staged commits)

Each commit compiles and passes focused tests. No `Co-Authored-By` trailer (repo rule).
The migration lands and is applied to prod **before** the entity commit merges (standing
rule: don't bundle migration + dependent entity in one commit).

- **C1 — Flyway migration.** `mysql/V23` + `h2/V24` per §8. Applied by Paul against prod
  (sandbox can't run credentialed DDL). Validated by a clean gemdtest rebuild.
- **C2 — persistence (`gemma-core`).** `ApiKey` JPA entity + `ApiKeyDao`/`ApiKeyService`
  (read/write split idiom). `mint(owner, label, scope, expires)` → plaintext once, persists
  hash; `resolve(plaintext)` → active/non-expired/non-revoked, bumps `LAST_USED`;
  `revoke(id)`.
- **C3 — auth filter + chain (`gemma-rest`).** `ApiKeyAuthenticationFilter` (clone of the
  Bearer filter) registered before the Bearer filter. Reads the header/param, resolves via
  `ApiKeyService`, builds an `Authentication` with the tier's authorities. `RestSecurityConfig`:
  admission gate governed by `gemma.rest.apiKey.mode = off | warn | enforce` (respect the
  `SettingsConfig` `gemma.`-prefix gotcha); anonymous-write block on unsafe methods
  (POST/PUT/DELETE/PATCH) for principals lacking `GROUP_USER` → clean 403. Extend
  `RestSecurityConfigTest` with the tier × method × mode matrix.
- **C4 — key management endpoints.** `ApiKeysWebService` — admin `POST /rest/v2/apiKeys`
  (mint), `GET` (list, no plaintext), `DELETE /{id}` (revoke). `hasAuthority("GROUP_ADMIN")`.
- **C5 — `/login` rate-limit + lockout.** Cheap, high value once off the VPN — brute-force /
  credential-stuffing defence the VPN used to provide. (Caffeine-backed attempt counter per
  username/IP.)
- **C6 — TOTP on human login.** `TOTP_SECRET` column (own migration), setup/verify endpoint,
  a check in the `/login` flow, and remember-device (§4). **Interactive humans only** — does
  not touch the key path. Self-contained; can land independently of C1–C5.
- **C7 — tracking dimension.** Add `apiKeyId` (or `anonymous:<label>`) to the existing
  `gemma_rest_api_access` analytics event.
- **C8 — consumer updates + rollout.** Update our own callers to send a key
  (`scripts/perf_search.py`, GemBrow, curation-ui — UI change via a handoff note per the
  handoff convention). Flip frink `off → warn → enforce`.

## 10. Rollout

`off` (ship, no enforcement) → `warn` (log missing/invalid keys, still serve) → `enforce`
(401 without a valid key). Frink advances stages only once our own consumers carry keys.
Keys issued **on request**, admin-minted, one per named consumer.

## 11. Open decisions

1. **TOTP scope** — confirmed in this doc as human-login-only; the agent never touches it.
   Confirm C6 is in-scope for this effort vs. a follow-on (C1–C5/C7/C8 stand alone without it).
2. **Session softeners** — how far to push TTL / remember-device (§4). Default: keep the
   existing 8h sliding token, add remember-device.
3. **Anonymous-key issuance policy** — currently "on request, admin-issued." Revisit if a
   self-service public-key page is ever wanted.
