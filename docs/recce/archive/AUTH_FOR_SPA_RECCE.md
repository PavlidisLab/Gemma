# Auth-for-SPA recce: gemma-rest auth flows for the curation-UI

**Scope.** What authentication flow should the curator React SPA at
`gemma-curation-ui/apps/curation/` use when it talks to the standalone
`gemma-rest` WAR? Recce only — no code changes.

**Bottom line.** The SPA's API client already assumes a bearer-token
contract (`POST /rest/v2/login` → `{token, user}`, then
`Authorization: Bearer <token>` on every call). gemma-rest does NOT
currently issue tokens; it only validates HTTP Basic. The MVP gap is
small: a `POST /rest/v2/login` endpoint that mints an opaque token
backed by an in-memory store + a `BearerTokenAuthenticationFilter` in
the security chain. Recommended below as Option C-lite.

---

## 1. Current state in gemma-rest

### Security chain — `RestSecurityConfig`
(`gemma-rest/src/main/java/ubic/gemma/rest/security/RestSecurityConfig.java`)

```
.securityMatcher("/rest/v2/**")
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/rest/v2/users/**").hasAuthority("GROUP_USER")
    .anyRequest().permitAll())
.httpBasic(basic -> basic
    .realmName("Gemma RESTful API")
    .authenticationEntryPoint(restAuthEntryPoint))
.exceptionHandling(eh -> eh.authenticationEntryPoint(restAuthEntryPoint))
.anonymous(anon -> anon.authorities("IS_AUTHENTICATED_ANONYMOUSLY"))
.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
.csrf(csrf -> csrf.disable())
```

- HTTP Basic only. No form-login, no remember-me, no token filter.
- Stateless — no `JSESSIONID` written for `/rest/v2/**`.
- CSRF off (correct for a stateless basic-auth API).
- The entry point (`RestAuthEntryPoint`, "xBasic" realm to suppress
  the browser popup) returns a JSON `ResponseErrorObject` on 401.

### Endpoints inventory
`gemma-rest/src/main/java/ubic/gemma/rest/RootWebService.java` declares
two Swagger security schemes:
- `basicAuth` (HTTP Basic) — the only one wired by the filter chain.
- `cookieAuth` (`JSESSIONID` apiKey) — annotation only; the standalone
  REST WAR's chain is `STATELESS`, so this scheme is documentation
  carry-over from the gemma-web era and isn't actually live.

User-facing endpoints today:
- `GET /rest/v2/users/me` — current user (line 105)
- `GET /rest/v2/users/{username}` — admin-or-self (line 123)
- **NO** `/rest/v2/login`, `/rest/v2/logout`, `/rest/v2/me`.

`grep -rln 'login|/auth|JSESSIONID|signin|signout|token|Bearer'` in
`gemma-rest/src/main/java/` returns only these three files
(`RootWebService.java`, `RestAuthEntryPoint.java`,
`RestSecurityConfig.java`). No JWT / OAuth / bearer-token machinery
anywhere in the repo (`grep -rln 'JWT|Bearer|OAuth|Shibb' gemma-core
gemma-web gemma-rest` → zero hits).

### Auth infrastructure already in `gemma-core`
(`gemma-core/src/main/java/ubic/gemma/core/security/SecurityConfig.java`)
- `AuthenticationManager` bean (`ProviderManager` over
  `LegacyAwareDaoAuthenticationProvider`, `RunAsImplAuthenticationProvider`,
  `AnonymousAuthenticationProvider`) — usable directly by a future
  login endpoint to validate username+password.
- `UserManagerImpl` (`UserDetailsService` + `UserDetailsPasswordService`)
  — provides user lookup and BCrypt upgrade-on-login.
- `GemmaLegacyAwarePasswordEncoder` — accepts both legacy SHA-1 and
  BCrypt hashes.
- `SessionRegistryImpl` bean (for gemma-web's concurrency-control;
  irrelevant to the stateless REST chain).
- `ManualAuthenticationServiceImpl.authenticate(username, password)` —
  already exposes a programmatic login that returns an
  `Authentication`. **This is the building block.**

### Form-login (gemma-web only, NOT in standalone gemma-rest WAR)
`gemma-web/applicationContext-security.xml` lines 73-76:
- `<s:form-login login-page="/login.jsp" .../>` — default endpoint
  `POST /login` (Spring Security default, since SS4+).
- `<s:remember-me key="gemma_rm" user-service-ref="userManager"/>`.
- `<s:session-management>` with concurrency-control.
- `AjaxAuthenticationSuccessHandler` already returns JSON
  `{success, user, isAdmin}` when the form POST includes
  `ajaxLoginTrue=true`. **Sets a `JSESSIONID` cookie.** Lives in
  `gemma-core/src/main/java/ubic/gemma/core/security/authentication/
   AjaxAuthenticationSuccessHandler.java`.

This XML is **not** loaded by the standalone gemma-rest WAR
(`gemma-rest/src/main/webapp/WEB-INF/web.xml` deliberately omits
`applicationContext-security.xml`), so form-login is currently
inactive in the standalone path.

### CORS — already production-ready for the SPA
`gemma-rest/src/main/webapp/WEB-INF/web.xml` registers
`ubic.gemma.rest.servlet.CorsFilter` with:
- `allowedOrigins=${cors.allowedOrigins}` (configurable)
- `allowedHeaders=Authorization,Content-Type,X-Gemma-Client-ID,X-Requested-With`
- `allowCredentials=true`
- `maxAge=1200`

So preflight handling, credentials, and the `Authorization` header are
all whitelisted. **Nothing to add for the SPA cross-origin story.**

---

## 2. What the curation-UI sends today

`gemma-curation-ui/apps/curation/src/api/client.ts` (`request()`):

```ts
headers: {
  "Content-Type": "application/json",
  ...(token ? { Authorization: `Bearer ${token}` } : {}),
}
```

`bearerToken()` reads `localStorage["gemma-curation-session"].token`,
falling back to build-time `VITE_GEMMA_CURATION_API_KEY`.

`gemma-curation-ui/apps/curation/src/api/session.ts`:
- `useLogin()` → `api.post<LoginResponse>("/rest/v2/login", {username, password})`
  → expects `{token, user}` back → stores in localStorage.
- `useLogout()` → `api.post<null>("/rest/v2/logout", {})`.
- `useMe()` → `api.get<User|null>("/rest/v2/me")`.

The SPA's `SECURITY-TODO` comment (session.ts:6) explicitly flags
localStorage XSS risk and notes "the right fix is HttpOnly cookies set
by the real Gemma backend." Today the local mock server happily honours
the Bearer scheme; production is unspecified.

**The SPA is therefore aligned with Option C (bearer token), NOT
Option A (Basic) or B (cookie).** Switching to any other option
requires changes on both sides.

---

## 3. Options

### Option A — Keep HTTP Basic (status quo on the server side)

**Server changes.** None. SPA mints
`Authorization: Basic ${btoa(user+':'+pass)}` itself.

**SPA changes (medium).** Rewrite `session.ts` to capture
username+password in a login form, encode and stash in
sessionStorage/localStorage, attach to every request. Drop
`/rest/v2/login` and `/me` calls; instead probe `/rest/v2/users/me`
with the Basic header to verify credentials. `useLogout()` becomes
"clear in-memory creds."

**Pros.** Zero server work. Already verified end-to-end.

**Cons.**
- Plaintext creds in browser memory + storage indefinitely.
- No password-change observability: SPA holds the cred until reload.
- No graceful 401 redirect to login (every API call would 401, but
  the SPA has to translate that itself).
- Native browser Basic popup is suppressed (`xBasic` realm in
  `RestAuthEntryPoint`) so the SPA can render its own form — that
  part already works.
- Doesn't match the SPA's existing contract; requires SPA refactor.

**Effort.** Server S (none). SPA M.

### Option B — Session cookie via form-login (`JSESSIONID`)

**Server changes (M).** Add a `formLogin(...)` config to the REST
chain (or a small Spring MVC controller doing the same), turn
`sessionCreationPolicy` from `STATELESS` to `IF_REQUIRED`, mount the
`AjaxAuthenticationSuccessHandler` for JSON response shape. Enable a
CSRF filter (CookieCsrfTokenRepository) for non-idempotent verbs OR
require a `SameSite=Lax` cookie + same-origin (which kills the SPA's
cross-origin dev setup).

**Cross-origin cookie pain.**
- Cookie must be `SameSite=None; Secure` for cross-origin → forces
  HTTPS-only in prod. (Dev uses Vite's `/rest` proxy → same-origin,
  so this is only a prod concern.)
- SPA must send `credentials: "include"` on every `fetch` —
  client.ts currently does not.
- CORS allowedOrigins must list the exact SPA origin (no wildcard)
  with `allowCredentials=true` — already true today.
- Browsers increasingly restrict third-party cookies; future-proofing
  is iffy if SPA and API end up on different eTLD+1.

**SPA changes (M).** Rewrite `client.ts` to use
`credentials: "include"` (drop bearer header), rewrite `session.ts`
to expect a 200 + Set-Cookie on `/rest/v2/login` (no token in body)
and to read identity from a follow-up `/rest/v2/me`.

**Pros.** Server holds session, can invalidate on logout, normal
Spring Security concurrency control still applies.

**Cons.**
- Cross-origin cookie semantics are fragile.
- Need CSRF tokens for cookie-auth (extra round trip + header
  management on the SPA).
- Stateful — defeats the point of `STATELESS` REST API.

**Effort.** Server M. SPA M.

### Option C — Bearer token (opaque or JWT)

**Server changes (M).**
1. Add `POST /rest/v2/login` (JAX-RS): take `{username, password}`,
   call `authenticationManager.authenticate(...)`, mint an opaque
   token (UUID), store it in a `Map<String, AuthenticationToken>`
   (or Caffeine cache with TTL — e.g. 8h sliding).
2. Add `POST /rest/v2/logout`: remove from store.
3. Add `GET /rest/v2/me` (or wire `/users/me` as the SPA target).
4. Add a `OncePerRequestFilter` BEFORE the basic-auth filter: if
   `Authorization: Bearer <opaque>` present, look up in store,
   populate `SecurityContextHolder` with the cached `Authentication`.
   Fall through to `HttpBasic` if no Bearer (keeps CLI clients alive).
5. Keep `SessionCreationPolicy.STATELESS`. CSRF stays disabled
   (no cookies → no CSRF surface).

**Pros.**
- Matches the SPA's existing contract; SPA needs zero code changes.
- Stateless from Spring Security's POV (token lookup is just an
  `Authentication` providing the principal).
- No cookie cross-origin headaches.
- Token rotation, revocation, TTL all straightforward.
- CLI / scripted clients can keep using Basic (or graduate to tokens
  later).

**Cons.**
- localStorage still XSS-readable. The SPA `SECURITY-TODO` is
  honest about this; mitigation is short-lived tokens + the standard
  XSS hygiene the SPA already has (React escapes, CSP planned).
- Opaque-token store is in-memory by default → loses tokens on WAR
  restart. Acceptable for MVP (curator gets bumped to re-login);
  graduation path is Redis or a `auth_tokens` DB table.
- Server now has a non-trivial new code path to audit-log.

**JWT vs opaque.** Opaque is simpler (no key management, no
verification cost, easy revocation by store delete). JWT only buys
horizontal scaling without a shared store — not a current need.
**Recommend opaque.**

**Effort.** Server M (1-2 days of dev + tests). SPA S (nothing if
the wire matches; localStorage→cookie graduation deferred).

### Option D — OAuth / SSO / Shibboleth

Heavy: needs an IdP, redirect dance, token exchange. UBC has Shibboleth
infra but Gemma users aren't all UBC. Overkill for the curator-tool
MVP. **Out of scope.**

---

## 4. Recommendation

**Option C with an opaque token.** Three reasons:

1. **The SPA contract already exists.** `session.ts` and `client.ts`
   were authored against bearer-token semantics. Choosing anything
   else means rewriting the SPA's API layer for no functional gain.
2. **Stateless wins for ops.** No cookie cross-origin / SameSite
   / CSRF token plumbing; one straight `Authorization: Bearer` header
   on every call.
3. **Reuses what's already there.** `AuthenticationManager`,
   `UserManagerImpl`, `ManualAuthenticationServiceImpl.authenticate(...)`
   all do the heavy lifting already. The new code is a tiny REST
   endpoint, a `Map` (or Caffeine), and one servlet filter.

The `localStorage`-XSS risk is real but not blocking for MVP: pair
short TTL (8 hours, sliding) with the standard `X-Frame-Options`,
CSP, and `Strict-Transport-Security` headers gemma-rest should be
setting anyway. Migration to HttpOnly cookies is a v2 concern that
the token-store abstraction does not preclude.

---

## 5. Open questions for Paul

1. **Token TTL?** 8h sliding feels right for a curator workday;
   confirm or override.
2. **Concurrent sessions?** Allow multiple devices per curator
   (multiple live tokens), or enforce single-session like the gemma-web
   form-login does (`<s:concurrency-control max-sessions="1"/>`)?
3. **CLI / script clients.** Keep `Authorization: Basic` working
   indefinitely (recommended — RClient / cron / ad-hoc curl), or
   require everyone onto tokens?
4. **Token store persistence.** OK with in-memory (curator re-logs
   in after a WAR restart), or do we want a `gemd.auth_tokens` table
   from day one?
5. **`/rest/v2/me` vs `/rest/v2/users/me`.** SPA expects `/me`;
   gemma-rest exposes `/users/me`. Add `/me` as a shortcut, or
   update the SPA mirror?
6. **Audit logging.** Login successes/failures into Gemma's normal
   audit trail, or a separate auth log?

---

## 6. Concrete next steps (if Option C is approved)

Sketch — not part of this recce, just to scope effort:

- New file `gemma-rest/src/main/java/ubic/gemma/rest/security/AuthWebService.java`:
  `@Path("/auth")` with `@POST /login`, `@POST /logout`, `@GET /me`
  (or wire under `/rest/v2/login`, `/rest/v2/logout`, `/rest/v2/me`
  to match the SPA literally).
- New `gemma-rest/src/main/java/ubic/gemma/rest/security/TokenStore.java`:
  Caffeine-backed `ConcurrentMap<String, Authentication>` with TTL.
- New `gemma-rest/src/main/java/ubic/gemma/rest/security/BearerTokenAuthenticationFilter.java`:
  `OncePerRequestFilter` that runs before `BasicAuthenticationFilter`
  in the SS6 chain. Registered via `http.addFilterBefore(...)` in
  `RestSecurityConfig`.
- Open `RestSecurityConfig.restSecurityFilterChain`:
  `.authorizeHttpRequests` carves an exception for
  `/rest/v2/login` (`permitAll()`).
- Tests in `gemma-rest/src/test/java/ubic/gemma/rest/security/`:
  Jersey integration test for the three endpoints + filter unit
  test for the Bearer→Authentication path.

Wire shape on `/login` matches SPA expectations exactly:
```json
POST /rest/v2/login
{"username": "...", "password": "..."}
→ 200 {"token": "uuid", "user": {"username", "fullName", "email"}}
```
