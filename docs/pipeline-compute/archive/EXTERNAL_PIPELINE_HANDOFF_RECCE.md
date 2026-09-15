# External pipeline handoff via Tickets + Externalized events — Phase 3 recce

**Filed:** 2026-05-19. Phase 3 "modernise the monolith without splitting
it" track. Builds on three pieces already on disk:

- `@Audited` annotation + AOP aspect + Spring `AuditedEvent` publication
  (commit `6dfa20c1a4`).
- `Ticket` entity + state-machine service + 5 read-only REST endpoints
  (commits `3f50ccdf06`, `e9ea2cd3b3`).
- `SPRING_MODULITH_RECCE.md` — phased adoption plan for Spring Modulith;
  not yet adopted.
- `AUDIT_AS_WORKFLOW_RECCE.md` — decision record for the ticket data
  model.

Scope: a two-way event handoff between Gemma and external compute
pipelines (Nextflow / nf-core, ad-hoc Python workers, LLM agents),
using the `Ticket` as the durable correlation handle. RNA-seq
processing is the worked example; the doc is written so the recipe
generalises to variant calling, methylation, single-cell QC, and
LLM-based curation handoffs. Recce only — no production code, no
`pom.xml` change, no Flyway migration.

---

## 1. Purpose

Today Gemma sits on the same JVM as everything it does. Pipeline
runs (RNA-seq alignment, quantification, variant calling, …) are
launched out-of-band by a curator on a separate machine, results are
later loaded back via `loader-*` packages or hand-imported, and the
correlation between "we asked for X" and "X arrived" lives in
Slack / spreadsheets / human memory.

The pattern proposed here uses the Modulith event-publication
substrate (persistent `event_publication` table + `@Externalized`
annotation) to make the handoff first-class:

1. **Gemma emits a process-request event.** A curator (or auto-
   trigger) opens a `Ticket` of an appropriate `TicketType`; the
   ticket service publishes a request event; Modulith persists
   the publication and `@Externalized` routes it to a message
   broker (RabbitMQ).
2. **External pipeline consumes the request.** A Nextflow run, a
   Python worker, or an LLM agent subscribes to the queue, pulls
   raw data, does its work, and writes results to a known location
   (S3, NFS, on-disk scratch).
3. **Pipeline emits a process-completed event.** The worker
   publishes a terminal event (completed / failed) — and optionally
   intermediate progress events — back through the broker.
4. **Gemma ingests results.** An `@ApplicationModuleListener` on
   the Gemma side picks up the completion event, attaches results
   to the target entity (vectors to the EE, variants to the
   genome, …), and transitions the `Ticket` to `RESOLVED` (or back
   to `OPEN` on failure).

The `Ticket` is the durable correlation handle. Its `id` rides on
every event in both directions; ingestion is idempotent on
`(ticketId, terminal state)`. The event_publication table is the
source of truth on the Gemma side; broker durability is enough on
the worker side because the worker can always be re-driven by
re-publishing the request event from a `Ticket` still in `OPEN` or
`IN_PROGRESS`.

This is **explicitly NOT a workflow engine**. There is no DAG
across tickets, no compensating-transaction support, no built-in
retry policy beyond Modulith's "replay incomplete publications on
startup". One ticket = one external job. Cross-pipeline
orchestration is Section 9 (out of scope).

---

## 2. The RNA-seq concrete example

End-to-end walk through one ticket, from curator click to vectors on
disk.

### 2.1 Trigger

A curator opens a ticket via the new REST endpoint (or an
auto-trigger fires when a new EE arrives with RNA-seq raw data and
no quantification on disk):

```java
Ticket t = ticketService.openTicket(
    reporter,
    TicketType.RNASEQ_PROCESSING_REQUESTED,
    "Process GSE12345",
    List.of(new TicketTarget(EE_ID_123, TicketTargetType.EXPRESSION_EXPERIMENT)),
    new RnaSeqRequestPayload(
        "SRP000123",                         // sraAccession
        List.of("SRR1", "SRR2", "SRR3"),     // sampleAccessions
        "GRCh38.p14",                        // genomeBuild
        Quantifier.SALMON                    // quantifier
    )
);
```

`openTicket` is annotated `@Audited(TicketOpenedEvent.class)`. The
audit aspect writes the audit row, then the ticket service
publishes a Spring `ApplicationEvent` carrying the ticket id and
the typed request payload.

### 2.2 Event flow out

```
TicketService.openTicket(...)
    │
    ├── @Audited writes audit row
    ├── ticket row INSERTed (state=OPEN)
    └── publisher.publishEvent(new RnaSeqProcessRequestedEvent(ticketId, ee, payload))
            │
            ├── Modulith writes event_publication row
            │   (in same tx as ticket INSERT — both commit or neither)
            │
            └── after commit:
                ├── in-process @ApplicationModuleListener(s)  ← local subscribers
                └── @Externalized("rnaseq-process-requested::rnaseq")
                        │
                        └── RabbitMQ exchange "rnaseq", routing key
                            "rnaseq-process-requested" — JSON body
```

If the JVM crashes after the ticket commit but before RabbitMQ
acks the publish, the event_publication row is still incomplete on
restart and Modulith replays it. No lost requests.

### 2.3 Pipeline side

A Python worker (or nf-core/rnaseq with an event-driven entry point)
subscribes to the `rnaseq-process-requested` queue. On message:
parse JSON into a Pydantic `RnaSeqProcessRequestedEvent`; pull raw
data from SRA (`prefetch` + `fasterq-dump`); run `salmon quant` (or
`star + salmon`); write results to
`s3://gemma-rnaseq-results/<ticketId>/`; publish
`RnaSeqProcessCompletedEvent`. Failures (SRA error, alignment
crash, OOM) catch up to publish `RnaSeqProcessFailedEvent` with the
error string and a trimmed traceback.

### 2.4 Status updates back

- **Progress (optional)** — `RnaSeqProcessingProgressEvent` at
  well-defined milestones (download done, alignment 50%,
  quantification done). UI consumes; persistence ignores.
- **Terminal (required)** — exactly one
  `RnaSeqProcessCompletedEvent` or `RnaSeqProcessFailedEvent` per
  ticket. Ingestion idempotent on `(ticketId, terminal seen)`.

### 2.5 Inbound listener

```java
@Component
class RnaSeqResultIngester {

    private final ExpressionExperimentService eeService;
    private final TicketService ticketService;
    private final RnaSeqResultLoader loader;

    @ApplicationModuleListener
    public void onComplete(RnaSeqProcessCompletedEvent e) {
        ExpressionExperiment ee = eeService.loadOrFail(e.eeId());
        loader.attachVectors(ee, e.resultUri());           // result side effects
        ticketService.resolveTicket(
            e.ticketId(),
            "Pipeline completed: " + e.vectorCount() + " vectors loaded.",
            new RnaSeqProcessCompletedPayload(e.resultUri(), e.qcSummary())
        );
    }

    @ApplicationModuleListener
    public void onFail(RnaSeqProcessFailedEvent e) {
        ticketService.reopenWithFailure(
            e.ticketId(),
            e.errorMessage(),
            new RnaSeqProcessFailedPayload(e.errorMessage(), e.stackTrace())
        );
    }
}
```

The listener annotation gives us:

- Runs after the producer transaction commits.
- Runs on a separate thread (does not block the AMQP listener).
- Runs in its own transaction (`REQUIRES_NEW`).
- Persistent: its own `event_publication` row, completed only when
  the method returns. JVM restart in the middle replays.

---

## 3. Event schemas (records)

The full set of records the RNA-seq example needs. All immutable
records; Jackson serialises naturally. Stable shape across the
broker — see Section 7 for evolution rules.

```java
package ubic.gemma.core.pipeline.rnaseq;

import com.fasterxml.jackson.annotation.JsonAlias;
import org.springframework.modulith.events.Externalized;

import java.util.List;

/** Payload stored on Ticket on open. Mirrored in the Externalized event. */
public record RnaSeqRequestPayload(
        String sraAccession,
        List<String> sampleAccessions,
        String genomeBuild,
        Quantifier quantifier
) {}

public enum Quantifier { SALMON, STAR_SALMON, KALLISTO }

/** Request event — leaves the JVM. */
@Externalized("rnaseq-process-requested::rnaseq")
public record RnaSeqProcessRequestedEvent(
        long ticketId,
        long eeId,
        RnaSeqRequestPayload payload,
        int payloadVersion         // see Section 7
) {}

/** Optional progress; UI consumes, persistence ignores. */
public record RnaSeqProcessingProgressEvent(
        long ticketId,
        int percentComplete,
        String message
) {}

/** Terminal: success. */
@Externalized("rnaseq-process-completed::rnaseq")
public record RnaSeqProcessCompletedEvent(
        long ticketId,
        long eeId,
        String resultUri,           // e.g. s3://gemma-rnaseq-results/<ticketId>/
        int vectorCount,
        QcSummary qcSummary,
        int payloadVersion
) {
    public record QcSummary(
            @JsonAlias("mean_mapping_rate") double meanMappingRate,
            @JsonAlias("median_3prime_bias") double median3PrimeBias,
            int samplesPassed,
            int samplesFailed
    ) {}
}

/** Terminal: failure — ticket reopens with this payload attached. */
@Externalized("rnaseq-process-failed::rnaseq")
public record RnaSeqProcessFailedEvent(
        long ticketId,
        long eeId,
        String errorMessage,
        String stackTrace,
        int payloadVersion
) {}

/** Ticket-side payload that records the completion fact in the event log. */
public record RnaSeqProcessCompletedPayload(
        String resultUri,
        RnaSeqProcessCompletedEvent.QcSummary qcSummary
) {}

public record RnaSeqProcessFailedPayload(
        String errorMessage,
        String stackTrace
) {}
```

Notes:

- `payloadVersion` is the JSON-Schema discriminator. Bumped only when
  the wire format breaks backward compatibility. Pydantic shadow
  classes branch on it.
- Result side effects (attaching vectors, updating
  `CurationDetails`, refreshing search index) are NOT in the event —
  they happen in the listener. Keep events minimal facts.
- `@Externalized("topic::exchange")` — Modulith routes to RabbitMQ
  exchange `rnaseq`, routing key `rnaseq-process-requested`.

---

## 4. Generalization

The RNA-seq machinery is intentionally formulaic. Swapping it for
single-cell QC or variant calling is mechanical:

| Concept              | RNA-seq specific                        | General shape                          |
|----------------------|------------------------------------------|----------------------------------------|
| TicketType           | `RNASEQ_PROCESSING_REQUESTED`            | `<PIPELINE>_PROCESSING_REQUESTED`      |
| Request payload      | `RnaSeqRequestPayload`                   | `<Pipeline>RequestPayload`             |
| Request event        | `RnaSeqProcessRequestedEvent`            | `<Pipeline>ProcessRequestedEvent`      |
| Externalized topic   | `rnaseq-process-requested::rnaseq`       | `<pipeline>-process-requested::<pipeline>` |
| Progress event       | `RnaSeqProcessingProgressEvent`          | `<Pipeline>ProcessingProgressEvent`    |
| Result event         | `RnaSeqProcessCompletedEvent`            | `<Pipeline>ProcessCompletedEvent`      |
| Failure event        | `RnaSeqProcessFailedEvent`               | `<Pipeline>ProcessFailedEvent`         |
| Result handler       | `RnaSeqResultIngester`                   | `<Pipeline>ResultIngester`             |
| Result loader        | `RnaSeqResultLoader`                     | `<Pipeline>ResultLoader` (per-pipeline) |
| Tx ingestion idempotency key | `(ticketId, terminal seen)`     | same                                   |
| Storage              | `s3://gemma-rnaseq-results/<ticketId>/`  | `s3://gemma-<pipeline>-results/<ticketId>/` |

The Gemma-side recipe to add a new pipeline:

1. Add one value to `TicketType` (e.g. `VARIANTCALL_REQUESTED`).
2. Define 3 record event classes (Requested, Completed, Failed).
   Annotate the request + terminal events `@Externalized`.
3. Write one `@ApplicationModuleListener`-bearing class with two
   methods (onComplete, onFail).

That's it. No DAO changes, no schema migration (events ride
payload-JSON in the existing `event_publication` table; tickets
are already polymorphic on `TicketType`). Estimated ~120 LoC for
a new pipeline, most of it the result-loader (which is
pipeline-specific anyway).

The optional progress event is independent — add it only if the UI
wants a live progress bar.

---

## 5. Pipeline-side requirements

What the external worker must do, regardless of language /
orchestrator:

1. **Subscribe to the request topic.** RabbitMQ queue bound to the
   `<pipeline>` exchange with routing key `<pipeline>-process-requested`.
2. **Parse the JSON.** Match the Java record shape. Pydantic class
   below; equivalent dataclasses in Go / Rust / Node are trivial.
3. **Do the work.** Free choice: nf-core workflow, Snakemake, ad-hoc
   Python, an LLM agent in `gemma-curation-agents`.
4. **Idempotency: include the ticketId.** On rare duplicate
   deliveries (broker redelivery after worker crash mid-publish),
   the result-ingester on the Gemma side sees the ticket is already
   `RESOLVED` and no-ops. Workers do NOT need their own
   deduplication store.
5. **Publish progress + terminal events to the appropriate topics.**
   Exactly one terminal event per ticket. No silent abandons —
   wrap the worker loop in a try/except that always publishes a
   `<Pipeline>ProcessFailedEvent` on uncaught error.
6. **Format: JSON matching the Jackson record layout.** Field
   names in `camelCase` (matches Java record component names with
   default Jackson config). A Python Pydantic class shadows the
   Java record; or generate both sides from a shared JSON Schema
   under `gemma-rest/src/main/resources/event-schemas/`.

Minimal Python worker sketch using `aio-pika`:

```python
# worker.py — minimal event-driven RNA-seq worker
import asyncio
import json
import traceback

import aio_pika
from pydantic import BaseModel

class RnaSeqRequestPayload(BaseModel):
    sraAccession: str
    sampleAccessions: list[str]
    genomeBuild: str
    quantifier: str  # SALMON | STAR_SALMON | KALLISTO

class RnaSeqProcessRequestedEvent(BaseModel):
    ticketId: int
    eeId: int
    payload: RnaSeqRequestPayload
    payloadVersion: int

async def process(evt: RnaSeqProcessRequestedEvent) -> dict:
    # download SRA, run salmon, write results to S3 ...
    # returns dict matching RnaSeqProcessCompletedEvent shape
    return {
        "ticketId": evt.ticketId,
        "eeId": evt.eeId,
        "resultUri": f"s3://gemma-rnaseq-results/{evt.ticketId}/",
        "vectorCount": 42_000,
        "qcSummary": {
            "meanMappingRate": 0.92,
            "median3PrimeBias": 0.51,
            "samplesPassed": len(evt.payload.sampleAccessions),
            "samplesFailed": 0,
        },
        "payloadVersion": 1,
    }

async def main():
    conn = await aio_pika.connect_robust("amqp://gemma:gemma@rabbit/")
    ch = await conn.channel()
    await ch.set_qos(prefetch_count=1)

    ex = await ch.declare_exchange("rnaseq", aio_pika.ExchangeType.TOPIC, durable=True)
    q = await ch.declare_queue("rnaseq.worker", durable=True)
    await q.bind(ex, routing_key="rnaseq-process-requested")

    async with q.iterator() as it:
        async for msg in it:
            async with msg.process():
                evt = RnaSeqProcessRequestedEvent.model_validate_json(msg.body)
                try:
                    out = await process(evt)
                    await ex.publish(
                        aio_pika.Message(body=json.dumps(out).encode()),
                        routing_key="rnaseq-process-completed",
                    )
                except Exception as exc:
                    err = {
                        "ticketId": evt.ticketId,
                        "eeId": evt.eeId,
                        "errorMessage": str(exc),
                        "stackTrace": traceback.format_exc(),
                        "payloadVersion": 1,
                    }
                    await ex.publish(
                        aio_pika.Message(body=json.dumps(err).encode()),
                        routing_key="rnaseq-process-failed",
                    )

asyncio.run(main())
```

`prefetch_count=1` keeps slow alignment work fairly distributed
across worker replicas. `aio_pika.Message` with the default
`delivery_mode=PERSISTENT` survives broker restart.

For an nf-core wrapper: same shape, but `process()` becomes a
`subprocess.run(["nextflow", "run", "nf-core/rnaseq", ...])` and
the output dir is scraped for `multiqc_data/general_stats.json` to
fill `qcSummary`.

### 5.1 Workflow-tool integration patterns

Gemma's pipeline operations use **Nextflow**, **SLURM**, and **Luigi**.
The event pattern is workflow-tool-agnostic — the worker is just a small
bridge that subscribes to RabbitMQ and delegates to whichever tool fits:

**Nextflow / nf-core**:
```python
def process(payload, ticketId):
    work_dir = f"/scratch/gemma/{ticketId}"
    subprocess.run([
        "nextflow", "run", "nf-core/rnaseq",
        "--input", f"{work_dir}/samplesheet.csv",
        "--outdir", work_dir,
        "-profile", "singularity",
        "-resume",
    ], check=True)
    return scrape_multiqc(work_dir)
```

**SLURM**:
```python
def process(payload, ticketId):
    job_id = subprocess.check_output([
        "sbatch", "--parsable",
        "--job-name", f"gemma-{ticketId}",
        "--output", f"/scratch/gemma/{ticketId}/slurm-%j.out",
        "--export", f"GEMMA_TICKET_ID={ticketId}",
        "/opt/gemma/pipelines/rnaseq.sbatch",
    ]).decode().strip()
    # Poll squeue until job clears, OR register a SLURM completion-hook
    # script that publishes the terminal event back to RabbitMQ
    return wait_for_slurm(job_id)
```

The polling variant lives in the worker. The hook variant has the
SLURM job itself publish the terminal event — better for long jobs
that outlive worker processes. The hook script is one
`amqplib_publish.py` invocation in the sbatch script's epilogue.

**Luigi**:
```python
def process(payload, ticketId):
    luigi.build([
        ProcessRnaSeqTask(
            ticket_id=ticketId,
            sra_accession=payload["sraAccession"],
            sample_accessions=payload["sampleAccessions"],
            genome_build=payload["genomeBuild"],
        )
    ], workers=4, local_scheduler=False)  # talks to central scheduler
```

Luigi's task-completion hook (`@luigi.Task.event_handler(luigi.Event.SUCCESS)`)
or the `WrapperTask` pattern can publish the terminal event directly,
so the worker doesn't need to poll the scheduler.

**Mix-and-match**: a single Gemma worker can route per-payload to
different tools (e.g., `payload.runner == "nextflow"` → nf-core,
`payload.runner == "slurm"` → sbatch). Or run multiple worker fleets,
each pinned to one tool. Choice is operational — the event schema
doesn't care.

**Idempotency note specific to SLURM**: if a worker crashes after
`sbatch` returned a job id but before publishing started-event,
the SLURM job is still running unattended. Mitigation: SLURM
job's epilogue script ALWAYS publishes the terminal event using
the env-var `GEMMA_TICKET_ID` as the correlation key. Worker
crashes become harmless — terminal event still arrives at the
ingester even if its originating worker disappeared. (The same
pattern works for Luigi via its task event_handler hooks.)

---

## 6. Phased adoption

Same shape as `SPRING_MODULITH_RECCE.md` — small, reversible steps,
each with its own commit boundary.

### Phase 1 — Gemma side only, in-process events (~150 LoC)

Prerequisite: `SPRING_MODULITH_RECCE.md` Phase A landed (Modulith
on the classpath as test scope).

Promote `spring-modulith-starter-core` + `spring-modulith-events-jpa`
to compile scope (~5 lines of pom). Then:

- `TicketType.RNASEQ_PROCESSING_REQUESTED` enum value (1 line).
- Record event classes per Section 3 (~50 LoC).
- `@Audited` annotations on `TicketService.openTicket`,
  `resolveTicket`, `reopenWithFailure` (already in place for
  `openTicket`; just verify the others).
- `RnaSeqResultIngester` with two `@ApplicationModuleListener`
  methods (~40 LoC).
- A stub publisher and a stub in-JVM "worker" `@Component` that
  listens to `RnaSeqProcessRequestedEvent` and immediately fires a
  fake `RnaSeqProcessCompletedEvent` (~30 LoC, test-scope or
  feature-flagged).
- Integration test: open a ticket → assert ticket transitions to
  `RESOLVED` and result loader was called with the stub uri.

No RabbitMQ, no `@Externalized`, no out-of-process moving parts.
Validates the design end-to-end and shakes out the event_publication
+ Flyway migration before adding the broker.

### Phase 2 — cross-process: add RabbitMQ + `@Externalized`

- Add a RabbitMQ container to `docker-compose.yml` (Gemma uses
  Docker Compose for its dev / test environment already).
- Add `spring-modulith-events-amqp` dep and an
  `AmqpConfig` `@Configuration` declaring the `rnaseq` exchange.
- Annotate the request / completed / failed events `@Externalized`.
- Move the stub worker out of the JVM: it becomes the ~80-line
  `worker.py` from Section 5, run from a second container.
- Same integration test still passes (now exercises the broker
  bridge).

LoC delta: ~30 in `gemma-core`, ~80 in the new `gemma-rnaseq-worker`
sibling repo, plus container config.

### Phase 3 — real pipeline (nf-core/rnaseq)

- Replace the stub `process()` in `worker.py` with a wrapper that
  launches the real nf-core/rnaseq workflow against the parsed
  payload.
- Wire the curator UI's "process this experiment" button to
  `POST /tickets` with the right `TicketType` + payload.
- Production curator opens the ticket; nf-core runs; vectors land
  back on the EE; the ticket auto-closes.

This is the first user-visible delivery. Everything before it has
been infrastructure.

### Phase 4 — live UI fanout

- Add a WebSocket endpoint that fans out
  `RnaSeqProcessingProgressEvent` (and other event types) to
  authenticated curator sessions watching a given ticket.
- UI subscribes for the duration of a ticket-detail-page view.
- Curator-triggered re-runs from the UI: a button on a `FAILED` or
  `RESOLVED` ticket fires a new `RnaSeqProcessRequestedEvent`
  reusing the original payload.

---

## 7. Operational notes

- **RabbitMQ container in docker-compose.** Single node, default
  ports. No replication at Phase 2; revisit if volume grows.
- **Message durability.** `event_publication` table is the source of
  truth on the Gemma side — incomplete rows replay at JVM startup.
  Durable exchanges + queues + persistent messages on the worker
  side. If the broker loses an in-flight message, Gemma re-publishes
  on next restart.
- **At-least-once, not exactly-once.** Duplicate deliveries are
  possible (worker crash after publishing completion but before
  acking the request). Ingester is idempotent on `(ticketId, terminal
  seen)` — second arrival is a no-op.
- **Schema evolution.** Add fields nullable with sensible defaults
  (or `@JsonAlias` for renames). Bump `payloadVersion`; downstream
  Pydantic class branches on it. Never remove fields without a
  deprecation cycle of at least one full release accepting both
  shapes.
- **Failed events — two policies per pipeline.** *Reopen*: ticket
  goes back to `OPEN` with the failure payload; curator clicks
  "retry" which re-publishes. Default for transient errors (SRA
  throttling, OOM). *Hard fail*: ticket transitions to a permanent
  `FAILED` state (would need adding to `TicketState`); curator opens
  a new ticket to retry. Reserve for unrecoverable errors.
- **Event log retention.** Wire
  `CompletedEventPublications.deletePublicationsOlderThan(Duration)`
  to the existing nightly maintenance job (per
  `SPRING_MODULITH_RECCE.md` §5). Suggested: 90 days for successful,
  365 days for failed.
- **Broker security.** RabbitMQ users `gemma-publisher` (write
  request, read terminal) and `gemma-worker` (mirror), restricted
  by exchange + routing key. TLS at Phase 3.

---

## 8. Other pipelines the pattern applies to

Same machinery, different `TicketType` + record classes:

- **Single-cell QC.** `SINGLECELL_QC_REQUESTED`. Payload: cellranger
  / alevin-fry choice, reference, chemistry. Completed event carries
  per-sample knee-plot thresholds + ambient-RNA estimate. Worker is
  an nf-core/scrnaseq wrapper.
- **Variant calling.** `VARIANT_CALL_REQUESTED`. Payload: BAM URIs,
  reference, caller (DeepVariant / GATK / Strelka). Completed event
  carries VCF URI + per-sample call stats. Result loader writes into
  the variant model (separate work item).
- **Methylation.** `METHYLATION_PROCESSING_REQUESTED`. Payload: raw
  FASTQ URIs, reference, tool (Bismark / methylKit). Completed event
  carries bedGraph URI + global methylation summary.
- **LLM-based curation.** `LLM_CURATION_REQUESTED`. The "worker" is
  the `gemma-curation-agents` Python process. Payload: target EE id,
  prompt template version, model id. Completed event carries the
  proposed curation diff + a confidence score. Result handler writes
  the diff into the proposal table (see `AGENT_WRITEBACK_RECCE.md`).
  Identical pattern, just an LLM agent instead of Nextflow. Cleanest
  replacement for the existing `PUT /datasets/{id}/curationDetails`
  polling.

All four ride the same event_publication infrastructure, RabbitMQ
broker, and `Ticket` state machine. Adding the fifth is the same
3-event + 1-listener recipe.

---

## 9. Out of scope

- **DAG of tickets / workflow orchestration.** E.g. "after RNA-seq
  completes, auto-open a downstream DEA ticket". Phase 5 idea: a
  `parent_ticket_id` column on `Ticket` + a small listener that
  opens the child on the parent's `RESOLVED` event. Defer until at
  least two pipelines are in production.
- **Cancellation semantics.** Curator marks a ticket `CANCELLED`
  mid-run. Requires a `<Pipeline>ProcessCancelRequestedEvent`
  going out, worker honouring it (SIGTERM the Nextflow run), and
  a `<Pipeline>ProcessCancelledEvent` coming back. Same recipe
  shape; defer to Phase 4 with the live-progress WebSocket.
- **Multi-tenant brokers / cross-org routing.** One Gemma, one broker.
- **Replacing the `loader-*` packages.** This pattern is for
  *requested* runs (Gemma asked for it). Existing loaders (GEO,
  ArrayExpress) handle *unsolicited* arrivals and remain separate.

---

## 10. Recommendation

Phase 1 is cheap (~150 LoC, in-JVM, no broker) and validates the
design end-to-end with a stub worker. Land it on top of Modulith
Phase A; defer Phase 2 until one Phase-1 integration test has been
green for a week.

Biggest risk: **schema drift between the Java record and the
Pydantic shadow class**. Mitigation: generate both sides from a
shared JSON Schema under `gemma-rest/src/main/resources/
event-schemas/`, with a CI check that regenerates and diffs on every
PR. Cheaper than debugging "field renamed; quietly null on the other
side" post-deploy.
