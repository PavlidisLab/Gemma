# Jakarta Bean Validation (JSR-380) — opt-in notes

**Branch:** `worktree-validation-optin`
**Baseline commit:** `08e760bdaf` (off `phase2-acl-migrate`)
**Date:** 2026-05-18
**Companion audit:** `VALIDATION_AUDIT.md` (unmerged `worktree-validation-audit`,
commit `6a04942973`)

---

## TL;DR

Bean Validation (JSR-380 / Jakarta Validation 3.0) is now wired into
`gemma-rest`. JAX-RS resource methods can now legitimately use
`@Valid` on payload params, and DTO fields can carry `@NotNull`,
`@NotEmpty`, `@NotBlank`, `@Size`, `@Min`, `@Max`, `@Pattern`,
`@Email`, etc. — and the framework will actually fire them.

This commit is **infrastructure only**. No existing endpoints were
touched. Adoption is per-endpoint, incremental, and requires DTO
annotation work + careful thinking about auth and error response
shape (see "Recommendations" at the bottom).

---

## What changed

### `gemma-rest/pom.xml`

**Exclusions removed (2):**

- `org.hibernate.validator:hibernate-validator` was excluded from
  `org.glassfish.jersey.ext:jersey-spring6`. Removed — Hibernate
  Validator is now declared explicitly.
- `jakarta.validation:jakarta.validation-api` was excluded from
  `io.swagger.core.v3:swagger-core-jakarta`. Removed —
  `jakarta.validation-api` is now declared explicitly.

**Dependencies added (3):**

```xml
<dependency>
    <groupId>jakarta.validation</groupId>
    <artifactId>jakarta.validation-api</artifactId>
    <version>3.0.2</version>
</dependency>
<dependency>
    <groupId>org.hibernate.validator</groupId>
    <artifactId>hibernate-validator</artifactId>
    <version>8.0.1.Final</version>
</dependency>
<dependency>
    <groupId>org.glassfish.jersey.ext</groupId>
    <artifactId>jersey-bean-validation</artifactId>
    <version>${jersey.version}</version>
</dependency>
```

Versions: pinned per the Validation audit's recommendation.
`jakarta.validation-api` 3.0.2 and `hibernate-validator` 8.0.1.Final
are the Jakarta EE 10 / Spring 6 era pair. `jersey-bean-validation`
tracks `${jersey.version}` (currently 3.1.10, declared in root
`pom.xml`). No parent-pom dependency management exists for these
artifacts.

### No source changes

No `@Valid` was added anywhere. No `ConstraintValidator` impls
written. No `LocalValidatorFactoryBean` or
`MethodValidationPostProcessor` configured. None of that is needed
for the JAX-RS path — Jersey wires itself.

---

## How Jersey picks it up (no config code)

Putting `jersey-bean-validation` on the classpath is enough.
Jersey's `META-INF/services/javax.ws.rs.ext.Providers` (well,
`jakarta.ws.rs.ext.Providers` in Jersey 3) auto-registers
`org.glassfish.jersey.server.validation.internal.ValidationFeature`
which in turn binds Hibernate Validator as the `ValidatorFactory`
provider via `jakarta.validation.ValidationProviderResolver`.

Once `ValidationFeature` is active, Jersey:

1. Honours `@Valid` on resource method parameters (body, query,
   path, header).
2. Honours JSR-380 constraint annotations on bean fields reached
   via `@Valid`.
3. Throws `jakarta.validation.ConstraintViolationException` on
   violations, which the default Jersey
   `ValidationExceptionMapper` translates into `400 Bad Request`
   with a JSON body describing each violation.

If the existing gemma-rest exception-mapper stack (search for
`ExceptionMapper` impls in `ubic.gemma.rest`) wants to take over
the response shape, it can register a custom
`ExceptionMapper<ConstraintViolationException>` — but until then the
default mapper is fine for an opt-in rollout.

---

## Example: 3-line `@Valid` adoption on a REST endpoint

```java
@POST
@Path("/datasets/by-ids")
@Produces(MediaType.APPLICATION_JSON)
public Response getDatasetsByIds( @Valid @NotEmpty List<@NotNull Long> ids ) {
    return Response.ok( datasetService.loadByIds( ids ) ).build();
}
```

Submitting `{}` or `[]` or `[null, 42]` now returns `400 Bad
Request` automatically instead of being silently accepted and
trickling down to a `NullPointerException` or empty-result-set.

For DTO params:

```java
public class ExperimentFilterDto {
    @NotBlank
    public String taxonName;

    @Min( 1 ) @Max( 10_000 )
    public Integer limit;
}
```

then `@Valid ExperimentFilterDto filter` on the resource method
fires both constraints before the method body runs.

---

## Compile verification

```
JAVA_HOME=$(/usr/libexec/java_home -v 17) mvn -pl gemma-rest compile test-compile -am -DskipTests
# BUILD SUCCESS
JAVA_HOME=$(/usr/libexec/java_home -v 17) mvn compile -DskipTests
# BUILD SUCCESS (all four modules: Core, CLI, REST, Web)
```

No runtime smoke test was added — keeping the patch surgical.
First real `@Valid` adoption (per-endpoint) will serve as the
runtime canary.

---

## Recommendations — do NOT apply `@Valid` to existing endpoints in this commit

Adoption is **per-endpoint** and should be paired with:

1. **DTO field annotation work**: each endpoint's params need
   `@NotNull` / `@NotBlank` / `@Size` / etc. annotated on the right
   fields. This is a content-aware decision, not a mechanical
   rewrite. Wrong annotations are worse than no annotations
   (false 400s on legit requests).
2. **Auth model thinking**: some endpoints accept "weak" payloads
   on purpose (anonymous probes, health checks, deprecated v1
   shapes kept for back-compat). Don't add constraints to these
   blindly.
3. **Error response contract**: the default Jersey mapper emits a
   reasonable but not gemma-shaped JSON body. If gemma-rest has a
   canonical error envelope, a custom
   `ExceptionMapper<ConstraintViolationException>` should land
   alongside the first `@Valid` adoption to keep the shape
   consistent.
4. **OpenAPI integration**: Swagger / OpenAPI can read JSR-380
   annotations and surface them in the generated spec
   (`required: true`, `minLength`, etc.). The first adoption is a
   good time to verify the spec regenerates cleanly.

Cross-reference: the `@Secured` → `@PreAuthorize` agent's
observation that gemma-rest controllers carry `@PreAuthorize` but
no `@Valid` was correct under the old (no Bean Validation wired)
regime. With this commit landed, `@Valid` is no longer a no-op and
that gap can be closed deliberately, endpoint by endpoint.

---

## Rollback

If something downstream breaks:

```
git revert <this-commit>
```

The change is purely additive (deps + un-excludes) so revert is
clean. Existing endpoints have no `@Valid` annotations, so behavior
is unchanged until someone starts adopting per-endpoint.
