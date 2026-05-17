# Renovations

Working notes for the top-to-bottom modernization of Gemma. Living document — edit as decisions are made.

Branch: `renovations` (based on `hotfix-1.32.7`).

## The four prongs

1. **Infrastructure** — Java / Spring / Hibernate / build chain modernization (sticking with Java + MySQL).
2. **UI** — replace `gemma-web` (Ext JS + DWR + JSP) and `GemBrow` (Vue 2) with a single React app extending the curation-ui patterns.
3. **Data model** — flexibility for multiomics, heterogeneous data types and species.
4. **Agentic integration** — full integration of agentic assists and workflow management.

Order: 1 + 3 (design) in parallel first, then 4 against a real write-API, then 2.

---

## Phase 0 (infra housekeeping) — in progress

### Status

| Item | State | Notes |
|---|---|---|
| Java 8 → 17 runtime, bytecode 11 | ✅ done | See below |
| Flyway adoption | ⏳ deferred | Existing scheme is "poor man's Flyway"; needs prod coordination |
| JUnit 4 → 5 | ⏳ todo | Vintage engine for coexistence |
| hbm.xml → JPA annotations | ⏳ todo | Per-entity, distributable |
| Drop dead deps | ⏳ todo | |

### Java version

- Runtime: Amazon Corretto 17 (`~/Library/Java/JavaVirtualMachines/amazon-corretto-17.jdk/`, no sudo install).
- Build: `<maven.compiler.release>11</maven.compiler.release>` in root `pom.xml`.
- Enforcer: `requireJavaVersion` bumped to `[17,)`.
- Full reactor compile passes under JDK 17 in ~36s.
- Unit tests: 1090 run, 1061 pass. Failures bucket:
  - 22 errors: HDF5 native lib not installed (env, pre-existing).
  - 1 error: cellranger binary not on box (env, pre-existing).
  - 1 error: scratch dir missing (env, pre-existing).
  - **5 errors: `SearchServiceTest` — Spring 3.2's bundled CGLIB/ASM cannot read Java 17 bytecode (class file version 61).** This is why bytecode is pinned to 11; will lift in Phase 1.

### Why bytecode 11, not 17

Spring 3.2 ships an ancient ASM that throws `IllegalArgumentException` from `ClassReader.<init>` when it tries to CGLIB-proxy a `@Configuration` class compiled to bytecode 61. Only one test currently hits this (most contexts use XML config), but adopting more `@Configuration` would explode the failure count. JDK 17 *runtime* is fine — only the bytecode target is constrained.

Lift `release=11` → `release=17` once Spring is on 5.x (or 6.x), as part of Phase 1.

---

## Infrastructure baseline (audited)

| Layer | Current | EOL | Phase to address |
|---|---|---|---|
| Spring Framework | 3.2.18 | Dec 2020 | Phase 1 (→5), Phase 2 (→6) |
| Spring Security | 3.2.10 | Dec 2020 | Phase 1 (→5) |
| Hibernate ORM | 4.2.21 + hbm.xml | 2013 | Phase 1 (→5), Phase 2 (→6) |
| Hibernate Search + Lucene | 4.4 / 3.6.2 | 2011 | Phase 3 (Search rewrite — possibly replace) |
| Web frontend | Ext JS + DWR 2.0 + JSP + SiteMesh | ~2010 | Phase 2 cutover; new UI is React |
| JAX-RS (Jersey) | 2.25.1 | stale | Phase 1 (→2.39), Phase 2 (→3) |
| Servlet namespace | `javax.*` (Tomcat 9) | — | Phase 2 jakarta flag day |
| Java source/target | inherited 8; runtime ≥11 | — | **Phase 0: runtime 17, bytecode 11** (now) |
| Schema migrations | none (hand-rolled SQL in `sql/migrations/`) | — | Phase 0 if Flyway adopted |
| Tests | JUnit 4 only, 446 unit + ~100 IT | — | Phase 0 JUnit 5 vintage engine |

**Modern bits**: HikariCP 5.1, Log4j2 2.25, Jackson 2.21, MySQL connector 8.4, Maven plugins 2024–2025.

---

## Phase 1 sequence (Spring 3→5, Hibernate 4→5, still javax)

1. Spring 3.2 → Spring 4 → Spring 5 (incremental). Spring Security ACL is the hardest API.
2. Hibernate 4.2 → 5.6. `createCriteria` → JPA Criteria. Type system rewrite.
3. Hibernate Search 4.4 → 5.11 (still Lucene; whether to keep is a Phase 3 decision).
4. Jersey 2.25 → 2.39 (mechanical).
5. DWR endpoint inventory and conversion to REST (see DWR section below). DWR doesn't survive Phase 2; this is the conversion sprint.

Once Spring 5 is live: bytecode target → 17.

---

## Phase 2 — the jakarta flag day

One coordinated cutover:
- Spring 5 → 6
- Hibernate 5 → 6
- Jersey 2 → 3
- Tomcat 9 → 10.1
- Servlet 4 → 5 (`javax.servlet` → `jakarta.servlet`)
- JAXB / JavaMail → jakarta equivalents
- **DWR dies** — no jakarta successor. All DWR endpoints must already be REST.

---

## Phase 3 — Search

Decision later. Options:
1. Hibernate Search 7 + Lucene 9 (continuity).
2. MySQL `FULLTEXT` (kills a dep; loses faceting).
3. External OpenSearch / Elasticsearch (ops burden; proper search).

---

## DWR inventory

Single file: `gemma-web/src/main/webapp/WEB-INF/gemma-servlet.xml`.

- **40 remoted Spring beans**
- **279 exposed methods** total
- **~114 unique JS call sites** across `scripts/api/` and `scripts/app/` (suggests many exposed methods are dead — pre-pass to remove)
- 45 generated DWR interface JS files

Top controllers by method count:

| Controller | Methods |
|---|---|
| `expressionExperimentController` | 37 |
| `geneSetController` | 23 |
| `securityController` | 20 |
| `expressionExperimentSetController` | 20 |
| `experimentalDesignController` | 18 |
| `genePickerController` | 15 |
| `arrayDesignController` | 11 |
| `dEDVController` | 9 |

Easy wins (1–2 method controllers, 14 of them): convert first as muscle-memory builder.

Hardest: `expressionExperimentController`, `geneSetController` — entangled with Ext JS grids.

Cleanup target: remove dead `<dwr:include>` declarations before migration. Inventory of "exposed but not called from JS" reduces the 279 number substantially.

---

## Schema migrations — already exist

`gemma-core/src/main/resources/sql/migrations/` has **50 versioned files** named `db.X.Y.Z.sql` matching Gemma releases. Some `_rollback` and `_postponed` variants. Latest: `db.1.32.5.sql`.

**No runner in code** — applied manually by DBA.

Fresh-install bootstrap: `sql/init-entities.sql`, `sql/h2/init-entities.sql`, `sql/mysql/init-entities.sql`, plus `init-acls.sql` + `init-data*.sql`.

### Flyway adoption — three options

| Option | Effort | Pros | Cons |
|---|---|---|---|
| A. Rename to `V<n>__desc.sql`, baseline = concatenated current state | Low | Clean | Loses release-anchored filenames |
| B. Use Flyway versioned naming `V1.32.5__name.sql` with `baseline-version=1.32.5` | Medium | Preserves naming, real Flyway | Need to rename existing files |
| C. Hand-rolled `MigrationRunner` Spring bean + `gemma_migration_history` table | Lowest | Zero new deps | Reinvents Flyway |

Decision pending. Needs prod DBA confirmation of current applied version.

Action item this phase: write `sql/migrations/README.md` documenting current convention + future plan.

---

## Frontend strategy

### Inventory

| App | Stack | Scope | Plan |
|---|---|---|---|
| `gemma-web` (this repo) | Ext JS + DWR + JSP | Everything (legacy) | Decommission incrementally |
| `GemBrow` (`~/Dev/GemBrow`) | Vue 2.7 + Vuetify 2.7 | Browser + search (`Dataset.vue` is a stub) | Port to React (one-time) |
| `gemma-curation-ui` | React + TS + Tailwind | Curation review | Becomes the foundation |

### Decisions made

- **One app** going forward, not multiple SPAs.
- Built on the curation-ui (React + TS) foundation.
- Will eventually move into this monorepo (not yet).
- **Pixel fidelity to GemBrow is not a goal** — free to redesign.
- Visual identity / look-and-feel: TBD. Default Tailwind-y look is *not* the target. Decide before Stage 1 ships.

### Staged rollout (deferred — not started)

1. **Home page** — landing, search entry, login. Owns the shared shell.
2. **Browser** — React port of `Browser.vue` + `AnnotationSelector.vue` (~3.2K Vue lines total).
3. **Experiment pages** — extend curation-ui with visualization (heatmap, PCA, sample correlation, design tile) and download (raw, processed, DE, design).

---

## Sister repos (for context)

| Path | Role |
|---|---|
| `~/Dev/gemma-curation-agents` | Python agentic helpers (scrape-screen, curation-proposer, audit). Posts to a **mock** write-API queue. |
| `~/Dev/gemma-curation-agents-eval` | Eval harness, calibration packages, decks, notable_cases. |
| `~/Dev/gemma-curation-ui` | React/TS curator review UI. The seed for the future single React frontend. |
| `~/Dev/gemma-mcp` | MCP server wrapping `gemmapy` (16 tools). |
| `~/Dev/GemBrow` | Vue 2 browse/search UI (v0.4.8). To be ported to React. |

Prong #4 (agentic) needs: replace the mock write-API with a real `gemma-rest` endpoint. Can be built on existing Jersey 2 (javax) now, ported in Phase 2.

---

## Environment

- JDK 17 at `~/Library/Java/JavaVirtualMachines/amazon-corretto-17.jdk/Contents/Home`.
- Python 3.10 venv at `.venv/` (gitignored) — for any glue scripts.
- Build: `mvn -P fast install` for a no-tests no-webpack no-delombok pass.
- Full unit tests run in ~34 min on Paul's laptop. Integration tests need MySQL (docker-compose.yml provides), skip on laptop.
