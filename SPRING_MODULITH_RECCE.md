# Spring Modulith — Phase 3 reconnaissance

**Filed:** 2026-05-19. Phase 3, "modernise the monolith without
splitting it" track. Companion to `AUDIT_SYSTEM_AUDIT.md` /
`AUDIT_AS_WORKFLOW_RECCE.md` (which hand off chain events to whatever
infra we pick here) and to `EE_SERVICE_DECOMPOSITION_ROADMAP.md`
(which carves the read/write boundaries Modulith would then enforce).

Scope: what Spring Modulith would buy us, what the Maven layout looks
like (spoiler: no repo split), a proposed module map, four-phase
adoption plan, risks, and the direct tie-in to the `@Audited`
migration. Recce only — no production code or pom touched.

---

## 1. What Spring Modulith buys us

Spring Modulith (latest GA 1.4.x, requires Spring Framework 6.1+ —
we ship 6.2.8, so it just drops in) is a thin layer on top of stock
Spring that adds three capabilities we currently re-invent or skip:

### 1.1 Persistent `EventPublication` + auto-retry

Today an `ApplicationEvent` published inside a `@Transactional`
method is held in memory and fired on commit (or `BEFORE_COMMIT`).
If the handler throws, the *handler's* transaction rolls back but
the publication is gone — there is no replay. If the JVM crashes
between commit and handler dispatch, the event is lost.

Modulith ships an `EventPublicationRegistry` backed by a JDBC table
(`event_publication`: id, event_type, listener_id, serialised_event,
publication_date, completion_date). Each `@ApplicationModuleListener`
invocation is wrapped: the publication row is written in the same
transaction as the producer, marked complete only when the listener
returns, and any row still incomplete at startup is replayed.

For the audit-chain case (`AuditedEvent` → cache eviction → search
re-index → notification) this turns "fire and pray" into "fire and
the system will retry until it sticks". The auto-retry behaviour is
configurable (`spring.modulith.republish-outstanding-events-on-restart=true`
plus `IncompleteEventPublications.resubmitIncompletePublications`).

### 1.2 `@ApplicationModuleListener` — one annotation, three concerns

```java
@TransactionalEventListener     // listen after commit
@Async                          // run on a different thread
@Transactional(REQUIRES_NEW)    // start a fresh transaction
```

is the canonical "react to a domain event without blocking the
producer or accidentally enrolling in its transaction" stack. Three
annotations, each easy to forget, easy to misorder. Modulith collapses
the lot into a single `@ApplicationModuleListener` that also plugs
into the publication registry above.

### 1.3 Module-boundary verification

`ApplicationModules.of(GemmaApplication.class).verify()` (in a JUnit
test) walks the bean graph and asserts no module's `internal` types
are referenced from outside that module. Modules are declared by
package via `@ApplicationModule` on a `package-info.java`. The default
"module" is the first sub-package of the application root; sub-modules
must be named explicitly.

This gives us the same thing the ArchUnit checklist
(`ASPECTJ_INVARIANT_CHECKLIST.md`) is trying to do for cache /
security cross-cuts, but as a structural rule about which *packages*
are allowed to know about which other packages.

---

## 2. Maven layout: no repo split needed

Modulith does **not** require us to split `gemma-core` into N
artifacts. The recommended model is:

- One Maven module (still `gemma-core`).
- Inside it, top-level packages become application modules.
- `package-info.java` carries `@ApplicationModule("acl")` etc.
- Anything in an `internal` sub-package is hidden from other modules.
- Public API of a module = anything NOT in `internal`.

The existing `gemma-core` `package-info.java` files
(`expression/arrayDesign`, `expression/experiment`, etc. — six of
them already exist for `@NonNullApi`) are the natural insertion
points. Adding `@ApplicationModule("expression-experiment")` to one
of them is a one-line change.

Optional later step: split into separate Maven artifacts (Modulith
ships `spring-modulith-bom` for the multi-artifact case). Not on
the roadmap; the in-jar model is the lower-risk on-ramp and gives us
~90% of the value.

---

## 3. Proposed module map

Reading the current `gemma-core` tree against the seams the Phase 3
decomp work has been opening up, the natural boundaries are
**~11 modules**:

| Module | Lead package(s) | Notes |
|---|---|---|
| `acl` | `core.security.acl`, `gsec.acl` | Phase 2 ACL extraction lives here |
| `audit` | `core.security.audit`, `model.common.auditAndSecurity` | `@Audited` aspect + `AuditedEvent` publisher |
| `auth` | `core.security.authentication`, `authorization` | Login + role-check |
| `expression-experiment` | `model.expression.experiment`, `persistence.service.expression.experiment` | The big one; read/write split underway |
| `array-design` | `model.expression.arrayDesign`, `persistence.service.expression.arrayDesign` | Already split out as a sub-package |
| `gene` | `model.genome`, `persistence.service.genome` | Self-contained, easy first cut |
| `analytics` | `persistence.service.analysis`, `core.analysis.expression.diff` | DEA / coexpression / PCA |
| `loader-geo` | `core.loader.expression.geo` | GEO ingest, depends on `expression-experiment` public API |
| `loader-arrayexpress` | `core.loader.expression.arrayExpress` | + cellxgene, sra, synapse, zenodo siblings |
| `search` | `core.search` | Lucene + ontology fan-out |
| `workflow` | `model.common.workflow` (Ticket), `persistence.service.common.workflow` | Per `AUDIT_AS_WORKFLOW_RECCE.md` Phase B-1 scaffold |

Cross-cutting (not modules in the Modulith sense — better as shared
"open" modules or kept in the root):

- `util` — shared helpers, no business logic
- `model` infrastructure (`AbstractIdentifiable`, base interfaces)
- `config` — Spring `@Configuration` classes

The REST layer (`gemma-rest`) stays a separate Maven module and is
not part of the Modulith map; it consumes module public APIs.

---

## 4. Phased adoption plan

### Phase A — drop the dep, write a smoke test (~50 LoC)

Add to `gemma-core/pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.modulith</groupId>
    <artifactId>spring-modulith-starter-core</artifactId>
    <version>1.4.0</version>
    <scope>test</scope>      <!-- Phase A only -->
</dependency>
<dependency>
    <groupId>org.springframework.modulith</groupId>
    <artifactId>spring-modulith-test</artifactId>
    <version>1.4.0</version>
    <scope>test</scope>
</dependency>
```

Add one test class:

```java
class ModuleStructureTest {
    @Test
    void verify() {
        ApplicationModules modules =
            ApplicationModules.of("ubic.gemma");
        // Phase A: just document what we have today. NO .verify()
        // call yet — we know it would fail until @ApplicationModule
        // annotations are placed.
        modules.forEach(System.out::println);
        // Optional: write a textual representation to docs/.
        new Documenter(modules).writeDocumentation();
    }
}
```

Output (under `target/spring-modulith-docs/`):
`components.puml`, per-module `module-*.adoc`, dependency graphs.
**That alone is worth the dep**: it gives us a continuously-updated
picture of which packages talk to which.

LoC: ~30 lines of test + ~10 lines of pom. Zero production-code
change. No `verify()` call yet, so it cannot fail.

### Phase B — annotate one module, turn on `verify()`

Pick `audit` (smallest, freshest, well-bounded):

```java
// gemma-core/src/main/java/ubic/gemma/core/security/audit/package-info.java
@ApplicationModule(
    displayName = "Audit",
    allowedDependencies = { "model.common.auditAndSecurity" }
)
package ubic.gemma.core.security.audit;

import org.springframework.modulith.ApplicationModule;
```

Then in the test:

```java
ApplicationModules.of("ubic.gemma")
    .verify();   // fails if audit/ touches anything outside its allowed list
```

Expected outcome on first run: it will list a handful of leaks —
typically into `model.common.auditAndSecurity.eventType.*` (the
concrete event-type classes). Resolve by either expanding
`allowedDependencies` or moving the leaker to `audit.internal`. This
exercise is the value: each leak is a real coupling we either
formalise or fix.

### Phase C — switch chain handlers to `@ApplicationModuleListener`

This is where the audit migration and Modulith meet. Today's plan
(per `AUDIT_AS_WORKFLOW_RECCE.md` Phase C and the Phase C blocker
note from commit `527e1198c4`) has us writing `@TransactionalEventListener`
chain handlers like:

```java
@Component
class CacheEvictionOnAudit {
    @TransactionalEventListener
    @Async
    @Transactional(REQUIRES_NEW)
    void on(AuditedEvent e) { evictCachesFor(e.getTarget()); }
}
```

Modulith collapses this to:

```java
@Component
class CacheEvictionOnAudit {
    @ApplicationModuleListener
    void on(AuditedEvent e) { evictCachesFor(e.getTarget()); }
}
```

Same semantics, plus: the publication is persisted to
`event_publication` in the producer's tx, the listener runs async in
its own tx after commit, completion is recorded, and any incomplete
row at JVM startup is replayed.

Migration steps for the `event_publication` table:

```sql
-- Modulith ships its own Flyway migration under
-- classpath:/META-INF/spring-modulith-jpa-schema.sql (JPA variant) or
-- spring-modulith-jdbc-schema-mysql.sql (JDBC variant). Either copy
-- into our Flyway sequence or enable
-- spring.modulith.events.schema-initialization.enabled=true.
CREATE TABLE event_publication (
    id              BINARY(16)    NOT NULL PRIMARY KEY,
    listener_id     VARCHAR(512)  NOT NULL,
    event_type      VARCHAR(512)  NOT NULL,
    serialized_event TEXT         NOT NULL,
    publication_date TIMESTAMP(6) NOT NULL,
    completion_date  TIMESTAMP(6),
    INDEX idx_event_pub_completion_date (completion_date),
    INDEX idx_event_pub_listener (listener_id, serialized_event(255))
);
```

This is also the natural moment to retire the parts of `AuditAdvice`
that today exist *only* to fan an audit write into N side effects
synchronously — the blocker that `527e1198c4` documented.

### Phase D — extend module annotations + harden

As the EE decomp roadmap ships more read/write splits, drop
`@ApplicationModule` onto each new boundary. Eventually `verify()`
covers all 11 modules. At that point we can also experiment with
Modulith's externalisation: routing a subset of `AuditedEvent`s to
Kafka or RabbitMQ for the curation UI to consume directly, removing
some `PUT /datasets/{id}/curationDetails` polling. That is a Phase 4
conversation, not now.

---

## 5. Risks and costs

| Risk | Mitigation |
|---|---|
| **Dep weight.** `spring-modulith-starter-core` pulls `spring-modulith-api`, `-core`, `-events-api`, `-events-core`, jMolecules-ddd. ~6 jars, all small. | Phase A starts test-scope-only. Promote to compile-scope only when Phase C lands. |
| **Auto-retry surprises non-idempotent handlers.** A handler that increments a counter twice on replay is now a bug instead of a one-off. | Document the contract in `audit` module README. Phase C audit chain handlers are all idempotent by construction (cache eviction, search re-index, notification dispatch). |
| **`verify()` breaks the build until Phase B annotations stabilise.** | Phase A test does NOT call `verify()`. Phase B turns it on only for the `audit` module. Other modules opt in one at a time. |
| **Team learning curve.** Modulith terminology (module, named interface, application module listener) is new. | Single-page cheatsheet in `audit` module's package-info Javadoc. Same idioms apply elsewhere once learnt. |
| **`event_publication` table grows unbounded.** | Modulith ships `CompletedEventPublications.deletePublicationsOlderThan(Duration)` — wire to the existing nightly maintenance job (`persistence.service.maintenance`). |
| **Spring Boot bias.** Modulith examples assume Boot; we are still Spring Framework + WebApplicationInitializer (per `SPRING_BOOT_3_FEASIBILITY.md`). | The core / events / test starters work fine without Boot. Auto-configuration is opt-in via `@EnableSpringDataWebSupport` / explicit `@Bean` declarations — slightly more verbose but no blocker. |

---

## 6. Connection to the `@Audited` migration

The point where these two tracks meet is Phase C of audit migration
(retiring `AuditAdvice`). `AuditAdvice` today does two things at once:

1. **Write the audit row** — moves cleanly to `@Audited` aspect
   (already shipped in Phase A, commit `6dfa20c1a4`).
2. **Fan out to N side effects** — cache eviction, search reindex,
   notification dispatch, downstream analysis invalidation.

The Phase C blocker note (commit `527e1198c4`) describes the second
half as the hard part: the fan-out has implicit ordering, implicit
transaction enrolment, and no retry semantics. Hand-rolling that on
top of stock `@TransactionalEventListener` is doable but every
handler needs the same `@Async + @Transactional(REQUIRES_NEW)`
boilerplate, and any handler that throws silently loses the event.

`@ApplicationModuleListener` + `EventPublicationRegistry` give us
exactly the missing pieces:

- Fan-out: already free (Spring event multicast → multiple
  `@ApplicationModuleListener` methods).
- Async + own tx: one annotation instead of three.
- Persistence: per-listener row in `event_publication`.
- Retry: on JVM restart, OR by calling `resubmitIncompletePublications`
  from a scheduled job.

**The infra cost is effectively zero** once we are already publishing
`AuditedEvent` (we are, as of Phase A). The migration is "delete
three annotations per handler, add one, run a Flyway migration".

This is the cleanest path to retiring `AuditAdvice` we have surfaced
across the three audit recces. It also positions us for the workflow /
Ticket model in `AUDIT_AS_WORKFLOW_RECCE.md` Phase C, where ticket
state transitions are precisely the kind of multi-step domain event
that benefits from persistent publication.

---

## 7. Recommendation

**Do Phase A now** (one commit: dep + 30-line test + docs target).
Cost: ~50 LoC, test-scope-only, cannot break the build. Outcome:
Modulith's generated `components.puml` + per-module asciidoc lands
in `target/spring-modulith-docs/` and gives us a continuously-updated
architecture diagram for the next architecture review.

**Queue Phase B** behind the audit-aspect work currently in flight.
Picking `audit` as the first annotated module dovetails with the
already-extracted `core.security.audit` package.

**Plan Phase C alongside the AuditAdvice retirement** — they share
the same `event_publication` migration and the same listener
refactor. Doing them together avoids a double-touch on every
chain handler.

Phase D is opportunistic; each EE-decomp PR can drop in an
`@ApplicationModule` annotation as the cleanest natural commit.
