# CORS Recce — gemma-rest standalone WAR for curation-UI

Recce only. No code changes. Worktree:
`/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/standalone-batch75`
Baseline: `3c232b2777e9e14dcf864cd2fffb9126e260f1d8`.

## 1. Current state

CORS in this repo is **filter-based** (servlet filter wired via `web.xml`),
not Spring Security `.cors(...)` and not `@CrossOrigin` annotations. There
is **zero** `.cors(...)` invocation in `RestSecurityConfig.java`. Single
implementation; both WARs reference it.

### 1.1 Implementation

- `gemma-rest/src/main/java/ubic/gemma/rest/servlet/CorsFilter.java`
  — hand-rolled `OncePerRequestFilter`. Properties: `allowedOrigins`,
  `allowedMethods`, `allowedHeaders`, `allowCredentials`, `maxAge`.
  Comma-separated `allowedOrigins`, case-insensitive match,
  echoes the matched origin + emits `Vary: Origin`. Non-matching
  origin with a non-wildcard config → `403 "Invalid CORS request"`.
  Preflight detection: `Origin` header + `OPTIONS` method → returns
  `204` early without invoking chain. Asserts wildcards
  incompatible with `allowCredentials=true`. Placeholder substitution
  for `allowedOrigins` is resolved manually via
  `ConfigurableApplicationContext.resolveEmbeddedValue` because Spring
  doesn't substitute `${...}` in `<init-param>`s (see FIXME L119).

### 1.2 Configuration

- `gemma-core/src/main/resources/default.properties:48-49`
  ```
  cors.allowedOrigins=${gemma.hosturl}
  ```
  Default `gemma.hosturl=https://gemma.msl.ubc.ca` (L39). Production-only
  by default; **dev does not get a free localhost allow.**

### 1.3 gemma-rest standalone WAR wiring

- `gemma-rest/src/main/webapp/WEB-INF/web.xml:74-93` — filter declaration
  - `allowedOrigins=${cors.allowedOrigins}` (substituted at filter init)
  - `allowedHeaders=Authorization,Content-Type,X-Gemma-Client-ID,X-Requested-With`
  - `allowCredentials=true`
  - `maxAge=1200`
  - **No `allowedMethods` set** → preflight will not emit
    `Access-Control-Allow-Methods` header. Browsers usually only
    enforce-check when the requested method is non-simple
    (PATCH/PUT/DELETE).
- `gemma-rest/src/main/webapp/WEB-INF/web.xml:154-157` — mapped at
  `/rest/v2/*` only. Ordering documented L138-149: encoding → cors →
  springSecurityFilterChain → MDC. Cors before security so 401s still
  carry the `Access-Control-Allow-Origin` header.

### 1.4 gemma-web WAR wiring (for reference / parity)

- `gemma-web/src/main/webapp/WEB-INF/web.xml:84-103,127-139` — same
  filter class, same init-params, additional mappings for legacy GemBrow
  endpoints (`/j_spring_security_logout`, `/home.html`, `/login.jsp`,
  `/whatsnew/generateCache.html`). Standalone gemma-rest WAR mounts only
  `/rest/v2/*`, which is the curation-UI's only target. **Parity is fine
  for the API surface.**

### 1.5 Test coverage

`gemma-rest/src/test/java/ubic/gemma/rest/servlet/CorsFilterTest.java` —
covers wildcard, no-origin no-op, single-origin echo + credentials,
custom headers, mismatched origin rejection, and `maxAge`. Solid.

## 2. Gap vs curation-UI needs

Curation-UI inspected at `/Users/pzoot/Dev/gemma-curation-ui/apps/curation`.

### 2.1 Origins

- **Dev**: `http://localhost:5173` (Vite default; `vite.config.ts` L11
  hard-codes `port: 5173`).
  - *However:* dev currently goes through Vite's proxy
    (`vite.config.ts` L21-29 proxies `/rest/*` to `GEMMA_CURATION_URL`
    with `changeOrigin: true`), so browser requests are same-origin
    against localhost:5173. **The proxy path needs no CORS at all.**
    CORS only matters when a deploy bypasses the proxy.
- **Prod**: TBD. Plausible options: `https://curation.gemma.msl.ubc.ca`,
  or co-hosted behind the same nginx as gemma-rest (same-origin → no
  CORS). No prod hostname is declared in repo today.

### 2.2 HTTP methods

`src/api/client.ts` issues `GET`, `POST`, `PUT`, `PATCH`, `DELETE`.
`PATCH` / `PUT` / `DELETE` are **non-simple** → preflight required →
**`Access-Control-Allow-Methods` MUST be set**. Currently unset in
web.xml. **This is a real gap.**

### 2.3 Credentials / auth scheme

- `client.ts:128-136` — uses `Authorization: Bearer <token>` header,
  **no `credentials: 'include'`**, no cookies, no Basic.
- gemma-rest auth scheme: `RestSecurityConfig.java:161,168-170` —
  `httpBasic()` + `SessionCreationPolicy.STATELESS` + `csrf().disable()`.
  **Bearer tokens are not supported server-side** — there's no
  `BearerTokenAuthenticationFilter` or equivalent. Out of scope for
  this recce but worth flagging: even with CORS fixed, real auth from
  curation-UI → gemma-rest will fail until either (a) gemma-rest grows
  a Bearer scheme or (b) curation-UI swaps to HTTP Basic. See "Open
  questions" below.
- For CORS itself: curation-UI sets `Authorization: Bearer ...` →
  preflight request includes `Access-Control-Request-Headers:
  authorization,content-type` → server must echo `Authorization` in
  `Access-Control-Allow-Headers`. **Currently does** (init-param
  already includes `Authorization`). 
- `allowCredentials=true` is currently set but is **unused** by the
  curation-UI fetch path (no cookies, no `withCredentials`). Harmless,
  but the constraint it enforces ("no wildcard origin") still bites.

### 2.4 Exposed response headers

curation-UI does not read any custom response headers
(`grep -rn "headers.get\|Response.headers" src/api/` returns nothing
user-driven). No `Access-Control-Expose-Headers` required today.
If gemma-rest later adds pagination headers (e.g. `X-Total-Count`)
they'd need exposing.

### 2.5 Summary — three most important gaps

1. **`allowedMethods` is not configured** → preflight for
   `PATCH`/`PUT`/`DELETE` returns no `Access-Control-Allow-Methods`
   header; browsers will block the actual request.
2. **No dev origin allowed** by default. `cors.allowedOrigins` defaults
   to `${gemma.hosturl}=https://gemma.msl.ubc.ca`. A developer running
   curation-UI against a localhost gemma-rest deploy bypasses Vite's
   proxy and gets 403 from `CorsFilter`. Need either a profile-driven
   override (`cors.allowedOrigins=http://localhost:5173` in `Gemma-dev.properties`)
   or a documented override per developer.
3. **No prod origin declared**. Once curation-UI lands a public hostname,
   it must be added to `cors.allowedOrigins` (comma-separated list).

## 3. Recommended config

This codebase uses a hand-rolled filter, not Spring's
`CorsConfigurationSource`. Two options:

### Option A (lighter — recommended): extend the existing filter wiring

In `gemma-rest/src/main/webapp/WEB-INF/web.xml` add an `allowedMethods`
init-param:

```xml
<init-param>
    <param-name>allowedMethods</param-name>
    <param-value>GET,POST,PUT,PATCH,DELETE,OPTIONS</param-value>
</init-param>
```

And in `gemma-core/src/main/resources/default.properties` (or
`Gemma-dev.properties` only):

```
cors.allowedOrigins=https://gemma.msl.ubc.ca,https://curation.gemma.msl.ubc.ca
# dev override (in Gemma-dev.properties):
# cors.allowedOrigins=http://localhost:5173,http://localhost:8080
```

### Option B (idiomatic): swap to Spring Security `.cors(...)` + `CorsConfigurationSource` @Bean

Drop `CorsFilter` from `web.xml`, add to `RestSecurityConfig`:

```java
@Bean
CorsConfigurationSource corsConfigurationSource(@Value("${cors.allowedOrigins}") String origins) {
    CorsConfiguration c = new CorsConfiguration();
    c.setAllowedOrigins(Arrays.asList(origins.split("\\s*,\\s*")));
    c.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
    c.setAllowedHeaders(List.of("Authorization","Content-Type","X-Gemma-Client-ID","X-Requested-With"));
    c.setAllowCredentials(true);  // drop if Bearer-only auth, no cookies
    c.setMaxAge(1200L);
    UrlBasedCorsConfigurationSource s = new UrlBasedCorsConfigurationSource();
    s.registerCorsConfiguration("/rest/v2/**", c);
    return s;
}
```
…and `http.cors(Customizer.withDefaults())` in the security chain.

Option A is **strictly less code change** (and the existing
`CorsFilterTest` keeps working). Recommend A unless there's an
appetite to delete the bespoke filter.

## 4. Open questions

1. **Prod hostname for curation-UI?** Same host as gemma-rest (nginx
   reverse-proxy, same-origin, no CORS) or a subdomain? If same-origin,
   the gap shrinks to "fix `allowedMethods`" only.
2. **Dev story for non-proxy dev?** Most devs hit Vite's proxy → no CORS
   needed. But agents running curation-UI prod-builds against a localhost
   gemma-rest will need a dev profile origin. Acceptable to add
   `http://localhost:5173` to a `Gemma-dev.properties` only?
3. **Bearer token support in gemma-rest** (out of scope for CORS, but
   blocks the integration). Curation-UI sends `Authorization: Bearer …`;
   gemma-rest only validates HTTP Basic. Decision needed: extend
   `RestSecurityConfig` with a bearer filter, or swap curation-UI to
   Basic auth.
4. **`allowCredentials=true`** — keep? Curation-UI doesn't use cookies.
   If kept, prod-origin must be exact (no wildcard). If dropped, dev
   could allow `*` origin. Recommend: drop unless gemma-web is still
   sharing the same CorsFilter and needs credentials.
5. **Wildcard subdomain support?** `CorsFilter` does exact (case-insensitive)
   string match — no pattern matching. If curation-UI ever uses
   per-PR preview deploys (`*.curation.gemma.msl.ubc.ca`), Option A
   filter needs upgrading or Option B becomes necessary
   (`setAllowedOriginPatterns`).

## 5. Effort estimate

**S (small).** For the Option-A path:

- web.xml: add one `<init-param>` block (~4 lines).
- default.properties: add prod curation-UI origin to the
  comma-separated list.
- Gemma-dev.properties (if exists): add localhost:5173.
- One new `CorsFilterTest` case asserting `allowedMethods` is echoed
  on preflight.

~30-60 min including running `CorsFilterTest` locally. Dependencies:
need the prod curation-UI hostname before committing to a value.

Option B is **M**: rip out filter wiring, add `@Bean` + `.cors()`
in security config, update the test (probably `MockMvc`-style now),
verify ordering in security chain matches the documented
"cors-before-security" intent. ~half day.
