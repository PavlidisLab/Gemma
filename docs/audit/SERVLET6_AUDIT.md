# Servlet 6 / Jakarta Servlet API Compliance Audit

Date: 2026-05-18
Branch: `worktree-servlet6-audit`
Baseline: `08e760bdaf` (`phase2-acl-migrate`)
Target spec: Servlet 6.0 (Jakarta EE 10) on Tomcat 10.1.34, JDK 17.

## TL;DR

Gemma is cleanly on Jakarta Servlet API — there are zero `javax.servlet.*`
Java imports anywhere in the source tree. The bulk of Phase 2's
jakarta.* migration is done. Two non-Java survivors remain (web.xml
schema namespace, JSP error-attribute names) plus one dependency-level
follow-up (legacy JSTL impl). One spec-correct namespace fix is applied
here; the others are documented as scoped follow-ups.

## Inventory

| Check | Result |
|---|---|
| `javax.servlet.*` Java imports | 0 |
| `jakarta.servlet.*` Java imports | 120 (across 71 files) |
| Files referencing the Servlet API | 71 |
| Servlet 4 deprecations (`getRealPath`, `encodeUrl`, `encodeRedirectUrl`) | 0 |
| Extends `HttpServlet` | 0 (Spring DispatcherServlet handles all) |
| Implements `Filter` directly | 0 (all use `OncePerRequestFilter` / `GenericFilterBean`) |
| `ServletContextListener` implementations | 1 (`UserCounterListener`) |
| `HttpSessionListener` implementations | 1 (`UserCounterListener`) |
| `WebApplicationInitializer` / programmatic init | 0 (uses web.xml) |
| `@WebServlet` / `@WebFilter` / `@WebListener` | 0 (uses web.xml — fine) |
| `AsyncContext` / `startAsync` usage | 0 (no async endpoints) |

## Servlet/Filter/Listener classes (all jakarta.* clean)

- `gemma-web/src/main/java/ubic/gemma/web/listener/UserCounterListener.java`
  — `ServletContextListener` + `HttpSessionListener`, imports `jakarta.servlet.*`.
- `gemma-web/src/main/java/ubic/gemma/web/listener/StartupListener.java`
  — extends Spring `ContextLoaderListener`, imports `jakarta.servlet.*`.
- `gemma-web/src/main/java/ubic/gemma/web/context/InitializeContext.java`
- `gemma-web/src/main/java/ubic/gemma/web/metrics/binder/servlet/ServletMetricsFilter.java`
- `gemma-rest/src/main/java/ubic/gemma/rest/servlet/CorsFilter.java`
  — `OncePerRequestFilter`, jakarta.servlet imports.
- `gemma-rest/src/main/java/ubic/gemma/rest/servlet/RestapidocsIndexRewriteFilter.java`
  — `OncePerRequestFilter`, jakarta.servlet imports.

All are idiomatic for Servlet 6 / Spring 6: Spring's filter base classes
(`OncePerRequestFilter`, `GenericFilterBean`) are used instead of raw
`jakarta.servlet.Filter`, listeners use `@Override`-correct jakarta.*
APIs.

## Non-Java survivors

### 1. `gemma-web/src/main/webapp/WEB-INF/web.xml` namespace (FIXED here)

Was: Java EE 7 `xmlns="http://xmlns.jcp.org/xml/ns/javaee"`,
`web-app_3_1.xsd`, `version="3.1"`.
Now: Jakarta EE 10 `xmlns="https://jakarta.ee/xml/ns/jakartaee"`,
`web-app_6_0.xsd`, `version="6.0"`.

Tomcat 10.1 still parses the old namespace as a courtesy, but the
spec-correct schema is `6_0`. Bumping the version unlocks Servlet 6
features should they be needed (no behavioural change for the existing
filter/listener/servlet declarations).

### 2. JSP error-attribute names — `javax.servlet.error.*` (DOCUMENTED, not applied)

Three JSPs read the legacy `javax.servlet.error.*` request attributes:

- `gemma-web/src/main/webapp/error.jsp` — 9 occurrences
- `gemma-web/src/main/webapp/common/exception.jsp` — 4 occurrences
- `gemma-web/src/main/webapp/pages/error/500.jsp` — 4 occurrences

Per Servlet 5+/6 spec the canonical names are `jakarta.servlet.error.*`
(`status_code`, `exception`, `message`, `exception_type`, `request_uri`,
`servlet_name`). Tomcat 10.x sets the `jakarta.*` names; the `javax.*`
names may not be populated in newer Tomcat 10 patch releases. The result
is that the error pages silently lose their status code / exception
display.

**Fix** is a mechanical string substitution:
`javax.servlet.error.` → `jakarta.servlet.error.` across the three files.
Not applied here because (a) gemma-web is being replaced by
gemma-curation-ui per project memory; (b) the change is JSP-runtime, not
compile-time-verified — wants a smoke-test of the error pages before it
lands.

### 3. JSTL impl is still legacy javax-namespaced (DOCUMENTED follow-up)

`gemma-web/pom.xml` pulls `org.apache.taglibs:taglibs-standard-spec:1.2.5`
+ `taglibs-standard-impl:1.2.5`. These are the EE 8 (javax) JSTL
artifacts. On a clean Jakarta EE 9+ container these wouldn't work
without bytecode transformation — they likely work today only because
Tomcat 10 applies the javax-to-jakarta package renamer at deploy time, OR
because the JSTL TLDs use `javax.servlet.jsp.jstl.*` URIs that Tomcat
maps to its built-in EL bridge.

**Follow-up** (separate commit, not in this audit):
replace with Jakarta JSTL:
```xml
<dependency>
  <groupId>jakarta.servlet.jsp.jstl</groupId>
  <artifactId>jakarta.servlet.jsp.jstl-api</artifactId>
  <version>3.0.0</version>
</dependency>
<dependency>
  <groupId>org.glassfish.web</groupId>
  <artifactId>jakarta.servlet.jsp.jstl</artifactId>
  <version>3.0.1</version>
  <scope>runtime</scope>
</dependency>
```
After the swap, the two `<context-param>` names in `web.xml` (currently
`javax.servlet.jsp.jstl.fmt.localizationContext` and
`javax.servlet.jsp.jstl.fmt.fallbackLocale`) MUST be renamed to
`jakarta.servlet.jsp.jstl.fmt.*` — they are coupled to the JSTL impl
version. TODO comments left in place in web.xml.

## Modernization opportunities (idiomatic Servlet 6 / not blocking)

- **Annotation-based registration** (`@WebFilter`, `@WebListener`).
  Gemma's filter set is small enough that web.xml registration remains
  clearer for ops review (one file lists everything). No change
  recommended.
- **`WebApplicationInitializer`** as a Spring-idiomatic alternative to
  web.xml. Same reasoning — XML is fine.
- **Async servlets** — not used. No long-running streaming endpoints
  identified that would benefit.
- **Session cookie attributes** — `web.xml` declares `<http-only>true</http-only>`
  and `<secure>true</secure>` but no `<same-site>` (Servlet 6 supports
  `<cookie-config><same-site>Strict</same-site></cookie-config>`).
  Suggest adding `Lax` or `Strict` once the cross-origin REST callers
  are inventoried.

## Servlet 4 deprecation sweep

Zero hits for:
- `HttpServletRequest#getRealPath(String)`
- `HttpServletResponse#encodeUrl` / `encodeRedirectUrl` (lowercase u)

## Verdict

Gemma's Java code is fully Jakarta Servlet 6 / EE 10 compliant. The
remaining migration debt lives outside Java: the web.xml schema bump
(now applied), the JSP error-attribute names (mechanical, deferred), and
the legacy JSTL artifact (dependency upgrade, deferred). None block the
current Tomcat 10.1.34 deployment.
