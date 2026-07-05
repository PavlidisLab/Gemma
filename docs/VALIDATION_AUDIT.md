# Jakarta Validation Audit (Bean Validation 3.0 / JSR-380)

**Branch:** `worktree-validation-audit`
**Baseline commit:** `08e760bdaf` (off `phase2-acl-migrate`)
**Date:** 2026-05-18
**Scope:** Recce-only. No source modifications.

---

## TL;DR

**Gemma does not use Jakarta Bean Validation at all.** Not javax, not
jakarta, no hibernate-validator, no `@Valid`, no `@NotNull`, no
custom `ConstraintValidator`s, no `LocalValidatorFactoryBean`, no
Jersey `ValidationFeature`. The Bean Validation API and its reference
implementation are *actively excluded* from transitively-pulled
artifacts.

All "validation" in the codebase is hand-rolled implementations of
the older Spring `org.springframework.validation.Validator` interface
(`validate(Object target, Errors errors)`) — a different,
pre-JSR-303 mechanism that doesn't share annotations or runtime with
JSR-380.

There is no migration work to do for Phase 3 in this area. The
"upgrade `javax.validation` → `jakarta.validation`" task line item
is a no-op for Gemma — there's nothing to upgrade.

---

## Inventory

| Probe | Result |
|---|---:|
| `import jakarta.validation.*` in `*.java` | **0** |
| `import javax.validation.*` in `*.java` | **0** |
| `@Valid` / `@NotNull` / `@NotEmpty` / `@NotBlank` / `@Size` / `@Min` / `@Max` / `@Email` / `@Pattern` / `@Constraint` | **0** |
| `@Constraint(validatedBy=…)` + `ConstraintValidator<A,T>` impls | **0** |
| `LocalValidatorFactoryBean` references (Spring wiring) | **0** |
| `MethodValidationPostProcessor` references | **0** |
| `@EnableValidation` annotations | **0** |
| `jersey-bean-validation` coordinate anywhere | **0** |
| `ValidationFeature` references in `*.java` / `*.xml` | **0** |
| `jakarta.validation-api` declared dependency | **0** (excluded only) |
| `hibernate-validator` declared dependency | **0** (excluded only) |

### What the 23 `*validation*` imports actually are

All 23 hits resolve to `org.springframework.validation.*` — the
older Spring Validator/Errors/BindingResult API used by Spring MVC's
DataBinder. They are not Bean Validation. Notable users:

- `gemma-web/.../ExpressionExperimentEditController` —
  inner-class `ExpressionExperimentEditFormValidator implements
  org.springframework.validation.Validator` invoked via
  `ValidationUtils.invokeValidator(...)`.
- `gemma-rest/.../analytics/ga4/GoogleAnalytics4Provider` —
  inner-class `EventValidator implements
  org.springframework.validation.Validator` validating GA4 event
  payloads.
- `gemma-core/.../util/StrictBeanDefinitionValidator` —
  internal JavaBeans-contract checker. `implements
  org.springframework.validation.Validator`.
- `gemma-web/.../SimpleFormController`, `ArrayDesignFormController`,
  `PubMedQueryController` — Spring MVC `BindException` /
  `BindingResult` / `ObjectError` handling.
- One incidental import in `SlackAppender` is
  `org.apache.logging.log4j.core.config.plugins.validation.constraints.Required`
  — a log4j-internal annotation, unrelated to Bean Validation.

---

## Versions

Nothing declared, so nothing to report. For the record:

- `jakarta.validation:jakarta.validation-api` — not in any pom as
  a dependency. Appears once in `gemma-rest/pom.xml:187-188` as an
  **exclusion** from `swagger-core-jakarta`.
- `hibernate-validator` — not in any pom as a dependency. Appears
  once in `gemma-rest/pom.xml:90-92` as an **exclusion** from
  `jersey-spring6`.

The Maven modernization agent's commit `43a98535bb` on the unmerged
`worktree-maven-modernize` branch is not reachable from this
worktree's history, so any property bumps it introduced are not
visible here.

---

## Wiring (Spring side)

**None.** No `LocalValidatorFactoryBean` declared in any
`applicationContext-*.xml` or `@Configuration` class. No
`MethodValidationPostProcessor`. No `@EnableValidation`. Spring MVC
controllers would have a default `Validator` injected by
`mvc:annotation-driven` *if* a JSR-303 provider were on the
classpath, but it isn't.

Spring's own `ValidationUtils.invokeValidator()` is used in
`ExpressionExperimentEditController`, but it accepts a hand-written
`org.springframework.validation.Validator` instance — it does
**not** route through Bean Validation.

## Wiring (Jersey side)

**None.** Jersey 3.1.10 (`jersey.version` in root pom line 1028)
auto-registers `org.glassfish.jersey.server.validation.internal.ValidationFeature`
when `jersey-bean-validation` is on the classpath. That artifact is
not declared anywhere. Furthermore, `jersey-spring6` explicitly
excludes `hibernate-validator`, which would have been the JSR-380
runtime impl. So even if a hidden transitive pulled
`jersey-bean-validation` in, there would be no validator
implementation to back it.

Conclusion: **no JAX-RS parameter / payload validation is happening
in gemma-rest.** Any `@NotNull` / `@Valid` annotations you might
later add would be silently ignored.

---

## Are constraints actively enforced?

There are no constraints, so the question is moot. The hand-written
`Validator` impls in `gemma-web` and `gemma-rest` are the only
enforcement and they are explicitly invoked
(`ValidationUtils.invokeValidator`, `EVENT_VALIDATOR.validate`),
not auto-discovered.

---

## Cross-references to other Phase 3 agents

- **`@Secured` → `@PreAuthorize` agent** (separate worktree): noted
  that `gemma-rest` controllers carry `@PreAuthorize` annotations
  but no `@Valid` on payload params. That observation is correct,
  and the reason is what this audit confirms — Bean Validation isn't
  wired in, so `@Valid` would be a no-op anyway. The two changes
  (adding `@Valid` and wiring the validator) must land together or
  not at all.
- **Maven modernization agent** (unmerged
  `worktree-maven-modernize`, commit `43a98535bb`): may have
  declared `jakarta.validation-api` 3.0+ and `hibernate-validator`
  8.0+ as managed dependencies in anticipation. Those declarations
  are harmless if no module depends on them, but the current
  `phase2-acl-migrate` HEAD does not contain them.

---

## Recommendations

### Option A — leave as-is (recommended for Phase 3 scope)

Hand-rolled `Validator` impls work, are clearly scoped, and have
zero Phase 3 migration cost. The Bean Validation no-op is invisible
because no one wrote `@NotNull`-style annotations expecting them to
fire. **Do nothing in this audit's pass.**

### Option B — adopt Jakarta Bean Validation in `gemma-rest` only

If REST payload validation is desired (e.g., to reject
nonsensical filter params in `ExpressionExperimentsWebService`
without writing imperative checks), the minimal wiring is:

1. Add to `gemma-rest/pom.xml`:
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
       <scope>runtime</scope>
   </dependency>
   <dependency>
       <groupId>org.glassfish.jersey.ext</groupId>
       <artifactId>jersey-bean-validation</artifactId>
       <version>${jersey.version}</version>
   </dependency>
   ```
2. Remove the two exclusion blocks (`gemma-rest/pom.xml:88-93`
   for `jersey-spring6`, and `gemma-rest/pom.xml:186-189` for
   `swagger-core-jakarta`).
3. Add `@Valid` to controller method params and constraint
   annotations on DTO fields where the value is load-bearing.
4. Wire a `JerseyViolationExceptionMapper` to translate
   `ConstraintViolationException` → `400 Bad Request` with a
   useful body. Jersey provides one by default but the gemma-rest
   exception-mapper stack may need to opt out for it to win.

This is a **medium-touch** change, scoped purely to `gemma-rest`.
Do NOT do this in this commit — propose, don't apply, per audit
charter.

### Option C — adopt Jakarta Bean Validation in `gemma-web` too

Not recommended. The Spring MVC frontend is being replaced by
`gemma-curation-ui` per the project memory note; investing in
validation wiring there is dead weight.

---

## Files inspected (key)

- `/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-validation-audit/pom.xml`
- `/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-validation-audit/gemma-rest/pom.xml`
- `/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-validation-audit/gemma-web/src/main/java/ubic/gemma/web/controller/expression/experiment/ExpressionExperimentEditController.java`
- `/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-validation-audit/gemma-rest/src/main/java/ubic/gemma/rest/analytics/ga4/GoogleAnalytics4Provider.java`
- `/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-validation-audit/gemma-core/src/main/java/ubic/gemma/core/util/StrictBeanDefinitionValidator.java`
