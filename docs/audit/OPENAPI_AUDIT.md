# OpenAPI / Swagger Integration Audit (Phase 3)

Date: 2026-05-18
Branch: `worktree-openapi-audit` (off `phase2-acl-migrate` @ `08e760bdaf`)

## TL;DR

gemma-rest is already on the modern OpenAPI 3 runtime
(`io.swagger.core.v3` "Swagger Core 2.x", which is the OpenAPI 3.0/3.1 implementation).
All in-source annotations are the v3 family (`io.swagger.v3.oas.annotations.*`).
No legacy Swagger 1.x annotations (`io.swagger.annotations.*`) remain.
springdoc is not applicable (Jersey, not Spring MVC).

Bumped `swagger.version` 2.2.42 -> 2.2.50 (matches the maven-modernize
agent's target). Compiles clean.

## Swagger artifacts in use

All under `io.swagger.core.v3` at `${swagger.version}` (2.2.50 after this commit).

| artifact | scope | role |
|---|---|---|
| swagger-core-jakarta            | compile | core OpenAPI model + serialization |
| swagger-jaxrs2-jakarta          | compile | JAX-RS scanner + processor |
| swagger-jaxrs2-servlet-initializer-v2-jakarta | runtime | servlet `ServletContainerInitializer` that auto-registers `OpenApiResource` etc. |
| swagger-integration-jakarta     | compile | servlet-side glue (context init, config loading) |
| swagger-models-jakarta          | compile | OpenAPI POJOs (`OpenAPI`, `PathItem`, ...) |
| swagger-annotations-jakarta     | compile | annotation surface for gemma-rest |
| swagger-annotations (non-jakarta) | transitive via gemma-core | annotation surface for value objects |

These are the **canonical, current** artifacts for a Jersey 3 / Jakarta EE 9+
stack. There is no further "next-gen" runtime to migrate to in the OpenAPI 3
world for a non-Spring-MVC application.

### Note on the duplicate annotation jar

Both `swagger-annotations` (pulled transitively via gemma-core) and
`swagger-annotations-jakarta` (pulled directly by gemma-rest) are on the
gemma-rest classpath. They carry the same `io.swagger.v3.oas.annotations.*`
package and are functionally interchangeable (the `-jakarta` variant only
differs in that its transitives use `jakarta.*` instead of `javax.*`, but for
annotation-only consumers this is moot). They co-exist cleanly today
(verified via `mvn dependency:tree`), but a future tidy-up could pick one
variant. Leaving as-is for now (changing gemma-core to depend on
`-jakarta` would ripple beyond this audit's scope).

## Annotation family counts

| family                                            | sites |
|---|---|
| `io.swagger.v3.oas.annotations.*` (OpenAPI 3)     | 133 imports across the repo |
| `io.swagger.annotations.*` (Swagger 1.x DEPRECATED) | 0 |
| `@Operation / @ApiResponse / @Schema / @Tag / @Parameter` usage sites in gemma-rest/src/main | 387 |

No renames applied: there is nothing to rename.

## Config / integration glue

- `gemma-rest/src/main/resources/openapi-configuration.yaml` --
  `resourcePackages: ubic.gemma.rest`, plus `openAPI.info` (title, version, description, contact, terms of service) and global security schemes.
- `gemma-rest/src/main/resources/openapi-configuration.schema.json` --
  IDE-side validation schema, references OAS 3.1.
- `ubic.gemma.rest.util.OpenApiConfig` -- Spring `@Configuration` that builds
  the `OpenAPI` bean via an `OpenApiFactory`, injects per-environment server
  URLs (prod / staging / dev / localhost), and wires the
  `CustomModelResolver` (a `ModelConverter` that customises schema
  resolution).
- `ubic.gemma.rest.util.OpenApiFactory` -- an `AbstractAsyncFactoryBean<OpenAPI>`
  that calls `JaxrsOpenApiContextBuilder.buildContext(false)` from
  `swagger-jaxrs2`. Singleton scope.
- `ubic.gemma.rest.OpenApiWebService` -- our own `@Path("/openapi.{type:json|yaml}")`
  resource, which resolves the `openApi` bean's `Future<OpenAPI>` and serializes
  it. It sets `Content-Encoding: gzip` when the client advertises gzip, which is
  what makes Jersey's `GZipEncoder` (a `ContentEncoder`, keyed off that response
  header rather than off `Accept-Encoding`) compress the ~600 kB payload.
  Replaced `OpenApiGzipHeaderDecorator` (deleted) plus Swagger's `OpenApiResource`
  -- see "Why we serve the spec ourselves" below.
- `swagger-jaxrs2-servlet-initializer-v2-jakarta` provides a
  `ServletContainerInitializer` (`SwaggerServletInitializer`). NOTE: contrary to
  what this audit originally said, it does NOT register `OpenApiResource`; all it
  does is `buildContext(true)` under the *default* context id from the `@Path`
  classes it scanned. `OpenApiResource` used to be reachable only because
  `io.swagger.v3.jaxrs2.integration.resources` was in
  `jersey.config.server.provider.packages` in `web.xml` -- that entry is now gone.

## OpenAPI doc URL

The OpenAPI spec is served at:

```
{baseUrl}/rest/v2/openapi.json
```

This is **constructed in code** in
`ubic.gemma.rest.RootWebService.welcome()` (around line 94-98) as
`uriInfo.getBaseUriBuilder().path("/openapi.json")...`, then surfaced
to API consumers in the `ApiInfoValueObject.specificationUrl` field.
The endpoint itself is `ubic.gemma.rest.OpenApiWebService`.

YAML is also available (`/openapi.yaml`) -- the same resource serves both
extensions off the one path template.

## Why we serve the spec ourselves

`OpenApiFactory.createObject()` decorates the object `ctx.read()` hands back
(the `servers` list built from `gemma.hosturl`, the `FilterArg`/`SortArg`
examples for issue #786, `${...}` placeholder resolution) rather than the
context's cache slot. Swagger's `GenericOpenApiContext.read()` caches with
`cacheTTL = -1` -- forever -- but its check-then-act is unsynchronized, and the
factory runs on `AbstractAsyncFactoryBean`'s background thread while Tomcat is
already serving. A request for `/openapi.json` landing inside that window built
a *second* `OpenAPI` instance and pinned it in the cache for the life of the
JVM; the Spring bean kept the decorated copy, so error responses still carried
the right `apiVersion` while the served document had no `servers` at all. With
no `servers`, Swagger UI falls back to the page origin and "Try it out" drops
the `/rest/v2` base path.

Observed on frink 2026-07-30: `/rest/v2/openapi.json` answered 24 kB, then
57 kB, then 76 kB within five seconds of the connector opening (a curation-UI
health probe polls that URL every 15 s), and the 76 kB undecorated copy was
served for the next 17 hours. Serving the bean's `Future` leaves exactly one
instance and nothing to race over; `OpenApiWebServiceTest` pins it.

A baked Swagger UI distribution ships under
`gemma-rest/src/main/resources/restapidocs/` and is exposed at
`/rest/v2/restapidocs/`.

## Verification

- `OpenApiTest` (gemma-rest/src/test) builds + asserts the generated spec
  resolves a representative set of paths, parameters, and responses.
- `OpenApiWebServiceTest` (gemma-rest/src/test) asserts that
  `/openapi.{json,yaml}` serializes the `openApi` bean's own instance, using a
  server URL only that bean carries.
- `scripts/poll_openapi.py` polls a live instance from the moment its port
  opens, which is the only way to exercise the startup window the race lived in.
  It reports `servers` / examples / wire size per response and exits non-zero if
  any response lacked `servers`.
- Compile clean against bumped 2.2.50:
  `JAVA_HOME=$(/usr/libexec/java_home -v 17) mvn compile test-compile -DskipTests` -- BUILD SUCCESS.

## Modernization opportunities (defer / future)

1. **Swagger Core 2.3 line** -- not released yet at time of audit. Watch
   https://github.com/swagger-api/swagger-core for a 2.3.x release; the 2.2.x
   line is still actively maintained and OAS 3.1 compatible.
2. **Consolidate annotation jar** -- pick `-jakarta` or non-jakarta variant
   for `swagger-annotations`; today both are pulled. Low-risk, low-impact;
   skip unless a dependency-convergence enforcer flags it.
3. **springdoc-openapi** -- NOT applicable. springdoc targets Spring MVC /
   WebFlux annotated controllers. gemma-rest is a Jersey 3 / JAX-RS
   application; the correct integration is `swagger-jaxrs2`, which we use.
4. **OpenAPI 3.1 features** -- the spec we emit is currently 3.0. The
   `openapi-configuration.schema.json` references 3.1; switching the
   emitted spec to 3.1 is a Swagger Core config flag but would require
   downstream tooling (the baked Swagger UI bundle, any client codegen)
   to support 3.1. Not pursued here.
5. ~~**Remove the gzip header decorator hack**~~ -- DONE 2026-07-30. The
   assessment that "the current code works" turned out to be wrong: not owning
   the endpoint was also what let the served spec lose its `servers` list (see
   "Why we serve the spec ourselves"). `OpenApiWebService` replaced both the
   decorator and Swagger's `OpenApiResource` in ~70 lines, and the gzip header
   is now set where the entity is built -- no more matching the payload by its
   leading `{"openapi"` characters.

## Changes in this commit

- `pom.xml`: `swagger.version` 2.2.42 -> 2.2.50.
- `OPENAPI_AUDIT.md` (new): this file.

No Swagger 1.x -> OpenAPI 3 renames were applied because no Swagger 1.x
annotations exist in the tree. The migration to `io.swagger.v3.oas.*` was
already complete prior to Phase 3.
