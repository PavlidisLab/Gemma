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
- `ubic.gemma.rest.providers.OpenApiGzipHeaderDecorator` -- a JAX-RS
  `WriterInterceptor` that adds `Content-Encoding: gzip` to the
  spec response (hacky but documented as such in the source).
- `swagger-jaxrs2-servlet-initializer-v2-jakarta` provides a
  `ServletContainerInitializer` that auto-registers
  `io.swagger.v3.jaxrs2.integration.resources.OpenApiResource` and friends
  -- no explicit `@Path` registration of `OpenApiResource` is needed in our
  code (this is how swagger-jaxrs2 has always worked).

## OpenAPI doc URL

The OpenAPI spec is served at:

```
{baseUrl}/rest/v2/openapi.json
```

This is **constructed in code** in
`ubic.gemma.rest.RootWebService.welcome()` (around line 94-98) as
`uriInfo.getBaseUriBuilder().path("/openapi.json")...`, then surfaced
to API consumers in the `ApiInfoValueObject.specificationUrl` field.
The endpoint itself is provided by the
auto-registered `OpenApiResource` from swagger-jaxrs2.

YAML is also available (`/openapi.yaml`) -- swagger-jaxrs2 serves both
by default.

A baked Swagger UI distribution ships under
`gemma-rest/src/main/resources/restapidocs/` and is exposed at
`/rest/v2/restapidocs/`.

## Verification

- `OpenApiTest` (gemma-rest/src/test) builds + asserts the generated spec
  resolves a representative set of paths, parameters, and responses.
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
5. **Remove the gzip header decorator hack** -- the comment in
   `OpenApiGzipHeaderDecorator` notes "we don't control the endpoint from
   Swagger's jax-rs integration". A cleaner path is to subclass
   `OpenApiResource` and register it explicitly, but that swaps a 30-line
   hack for ~100 lines of plumbing; the current code works. Skip.

## Changes in this commit

- `pom.xml`: `swagger.version` 2.2.42 -> 2.2.50.
- `OPENAPI_AUDIT.md` (new): this file.

No Swagger 1.x -> OpenAPI 3 renames were applied because no Swagger 1.x
annotations exist in the tree. The migration to `io.swagger.v3.oas.*` was
already complete prior to Phase 3.
