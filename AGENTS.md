# Codex Engineering Guide

## Purpose and Scope

This file is the repository-level operating contract for coding agents. It applies to the entire repository unless a
more specific `AGENTS.md` exists below the file being changed.

Build a production-grade, high-throughput document-processing service without weakening correctness, security, or
operability. Prefer small, evidence-based changes over speculative abstractions.

## Sources of Truth

Before changing behaviour, read the relevant code and tests, then consult:

1. `docs/requirements/document-ocr-service-functional-spec.md` for business behaviour and NFRs.
2. `docs/architecture/document-ocr-service-tech-spec.md` for architectural intent.
3. `README.md` for the service overview and operational model.
4. `pom.xml` and CI workflows for the actually configured toolchain.

Do not conceal contradictions between documents and implementation. Preserve current behaviour unless the task explicitly
authorises a change, and report material ambiguities rather than choosing a business rule silently.

Key constraints include files up to 50 MB, protected document data, durable asynchronous processing, at-least-once
delivery, and a target end-to-end processing latency of p95 <= 3 seconds per page. Treat the latency target as something
to measure under a documented workload, not as a claim inferred from unit tests.

## Working Agreement

- For review, explanation, diagnosis, or planning requests, inspect and report; do not edit unless asked.
- For build, change, or fix requests, make the smallest complete in-scope change and run relevant non-destructive
  checks.
- Inspect `git status` before editing. Preserve user-owned and unrelated changes, including staged files.
- Ask before destructive operations, external writes, expensive actions, or material scope expansion. Safe local reads,
  edits, builds, and tests do not need confirmation.
- Never use destructive Git commands to discard work. Do not commit, push, or open a pull request unless requested.
- Use `rg`/`rg --files` for discovery. Read the implementation and nearby tests before proposing a pattern.
- Do not add a dependency, infrastructure component, public endpoint, or configuration knob without a concrete need.
- When changing a dependency or framework API, verify compatibility from primary documentation and record the reason.
- Keep the final handoff concise: summarise behaviour changed, checks run, and remaining risks or unverified assumptions.

## Architecture and Boundaries

Use a conventional layered Spring MVC architecture under the service root. Use the following package structure; add a
new peer package only when the codebase has a concrete need that cannot be represented by one of these packages:

```text
config
entity
repository
service
exception
rest
├── api
├── controller
├── dto
└── exception
```

- **config**: Spring dependency wiring, typed configuration properties, external-client setup, executor configuration,
  and startup validation;
- **entity**: persistence entities and cohesive domain state, value types, invariants, and transition rules;
- **repository**: persistence access and bounded queries over entities;
- **service**: business use cases, workflow orchestration, transaction boundaries, integration orchestration, and
  business decisions;
- **exception**: application and domain exceptions that are independent of HTTP and other transports;
- **rest**: namespace for the HTTP adapter; do not place controllers or DTOs directly in the package root;
- **rest.api**: interfaces generated from, or kept aligned with, the OpenAPI contract;
- **rest.controller**: thin Spring MVC controllers that implement `rest.api` interfaces, validate transport input,
  delegate to one service use case, and explicitly map between DTOs and application/entity types;
- **rest.dto**: REST request and response payloads generated from, or kept aligned with, the OpenAPI contract; keep
  them separate from persistence entities and provider-specific models;
- **rest.exception**: HTTP exception mapping, `@ControllerAdvice`, and RFC 9457/`ProblemDetail` response construction.

The primary request flow is `rest.controller -> service -> repository`, with repositories persisting `entity` types.
The `config` package wires dependencies, `exception` represents transport-independent failures, and `rest.exception`
maps those failures to the HTTP contract. Controllers, repositories, configuration classes, and provider SDK
integrations must not contain business decisions. Entity types must not depend on Spring MVC, REST DTOs, Kafka, S3, or
AI provider types. Do not expose JPA entities or provider-specific models through application or API boundaries. Keep
mappings explicit; MapStruct is appropriate when it reduces repetitive mapping without hiding business logic.

In MVC terms, `entity`, `repository`, and `service` form the Model; `rest.controller` is the Controller; and `rest.dto`
is the external REST representation returned in place of a server-rendered View. The `rest.api` package defines the
contract implemented by controllers. Supporting packages must preserve these boundaries rather than bypass them.

Keep use cases cohesive. Avoid generic `Utils`, manager classes, static service locators, and interfaces created only to
mirror every concrete class. Introduce an abstraction at an external boundary or when there are genuine variants.

## Document Processing Pipeline

Model processing as explicit, observable, independently retryable stages:

`ingest -> store -> text detection/extraction -> OCR if required -> structured extraction -> validation -> persist -> publish`

- Stream uploads and downloads. Never materialise a 50 MB file, all rendered pages, or an unbounded OCR response in heap
  unless a measured design requires it.
- Validate size, detected content type, extension consistency, page count, and image dimensions before expensive work.
  Client-provided filenames and MIME types are untrusted.
- Store the original durably before asynchronous processing. Compute a checksum while streaming when deduplication or
  auditability requires it.
- Try extraction of an existing text layer before OCR. Do not OCR a digitally generated page without evidence that its
  text layer is absent or unusable.
- Treat OCR as CPU-bound. Run it on a dedicated bounded executor with a bounded queue and explicit
  rejection/backpressure.
- Virtual threads are suitable for high-concurrency blocking I/O, not for increasing OCR/CPU capacity. Never use an
  unbounded executor or the common `ForkJoinPool` for production pipeline work.
- Bound concurrency independently for OCR, LLM inference, storage, database, and Kafka. One slow dependency must not
  exhaust every request or worker resource.
- Every stage must be idempotent for the same document and attempt. Retries must not duplicate stored objects, state
  transitions, attributes, or events.
- Make state transitions explicit and atomic. Use optimistic locking or conditional updates to reject stale workers and
  illegal transitions.
- Apply deadlines at each remote boundary. Retry only transient failures, with capped exponential backoff and jitter.
  Avoid nested retries that multiply attempts across layers.
- Keep remote calls and CPU-heavy work outside database transactions. Transactions should be short and own only database
  consistency.
- Persist the aggregate change and outbox record atomically. Publish from the outbox; use stable event IDs/keys and
  design
  consumers for duplicate delivery.
- Treat OCR and LLM output as untrusted evidence. Require schema validation, domain validation, confidence/source
  metadata,
  and manual review for uncertain or inconsistent results. Valid JSON is not proof of factual correctness.

## Java and Spring Practices

- Target the Java version declared in `pom.xml` (currently Java 25). Do not rely on preview features unless explicitly
  enabled and justified.
- Prefer immutable values, small cohesive types, and constructor injection. Java records fit commands, results, events,
  and value carriers; they are generally unsuitable as JPA entities.
- Use precise domain types and names with units, such as `weightKg` or `Duration`. Use `BigDecimal` for decimal business
  quantities and `Instant` for timestamps. Define rounding and timezone behaviour at boundaries.
- Use `Optional` for an optional return value, not for fields, parameters, JPA attributes, or serialization models.
- Validate at the boundary, enforce invariants in the domain, and reject invalid state early. Never rely only on
  controller
  annotations for business correctness.
- Keep exception handling intentional: domain failures, validation failures, transient infrastructure failures, and
  programming defects must remain distinguishable. Do not catch `Exception` merely to log and continue.
- Preserve interrupt status and cancellation. Always close streams, temporary resources, and SDK responses with
  structured
  resource management.
- Avoid field injection, mutable global state, hidden blocking in parallel streams, and `CompletableFuture` without an
  explicit executor.
- Do not use Lombok `@Data` on entities. Keep entity equality/hash semantics explicit and independent of lazy relations.
- Keep configuration typed and validated. Secrets come from the runtime environment or secret store, never source files,
  examples with real values, logs, or tests.

## Persistence and Messaging

- Change the PostgreSQL schema only through forward Flyway migrations. Never edit a migration already applied outside a
  disposable local environment.
- Choose indexes from real access paths and query plans. Account for write amplification; do not add indexes
  speculatively.
- Prevent N+1 queries explicitly. Use projections, deliberate fetch plans, batching, and bounded pagination. Never load
  an
  unbounded result set.
- Keep relational columns for stable, constrained, frequently queried attributes. Use JSONB for variable extracted
  payloads
  and add GIN/expression indexes only for demonstrated query patterns.
- Use optimistic versioning for concurrently updated documents. Define ownership when multiple workers can update a row.
- Do not perform MinIO, OCR, LLM, Kafka, or HTTP calls while holding a database transaction or lock.
- Version API and event contracts. Make compatibility changes additive where possible; do not rename/remove fields
  silently.
- Kafka consumers must be idempotent. Offset acknowledgement must occur only after the durable business outcome is
  known.
  Configure poison-message handling explicitly; endless retry loops are forbidden.

## API and Security

- Follow Contract First for every external interface. A versioned contract committed to the repository is the source of
  truth; implementation code and framework annotations are not substitutes for it.
- Describe synchronous HTTP APIs with OpenAPI. Generate or validate `rest.api` interfaces and `rest.dto` models from the
  contract, and have `rest.controller` classes implement the generated API interfaces rather than defining an
  independent endpoint shape.
- Describe asynchronous Kafka channels, operations, messages, schemas, and bindings with AsyncAPI. Keep payload schemas
  compatible with the configured wire format and schema registry, and validate producers and consumers against the
  contract.
- Update the OpenAPI or AsyncAPI contract before or together with implementation changes. Run contract linting,
  generation, compatibility checks, and drift detection in Maven/CI. Review generated diffs; do not edit generated
  sources manually.
- Version contracts explicitly. Prefer additive, backward-compatible evolution; introduce a new API or event version
  for a necessary breaking change and document migration and deprecation behaviour.
- Keep REST controllers thin. Validate transport input, call one application use case, and map the result/error
  explicitly.
- Prefer asynchronous acceptance for long-running processing: persist work, return a stable document/job identifier, and
  expose status rather than holding the upload request open through OCR and LLM stages.
- Define idempotency semantics for write endpoints. Return consistent RFC 9457/`ProblemDetail` errors without stack
  traces,
  secret values, internal object keys, or document contents.
- Authorise access per document/order, not merely per endpoint. Default to denial; do not weaken OAuth2 rules for
  convenience.
- Keep object storage private. Presigned URLs must be short-lived, least-privilege, and scoped to one object and
  operation.
- Defend parsers against malformed files, decompression bombs, excessive pages/pixels, embedded content, and path
  traversal.
  Limit Tika/OCR work before parsing and isolate native processes where feasible.
- Treat documents, extracted text, prompts, model responses, names, addresses, identifiers, and object keys as
  sensitive.
  Do not log or attach them to metrics/traces. Redact structured logs and minimise data sent to external AI providers.
- Never expose secrets or tokens in source, fixtures, exception messages, command output, or final responses.

## Performance Engineering

- Establish a baseline before optimising. State the workload: document types and sizes, pages, scan resolution,
  text-layer
  ratio, concurrency, warm-up, hardware, OCR/model versions, and dependency latency.
- Measure throughput together with p50/p95/p99 latency, error/timeout/manual-review rate, CPU, allocation/GC,
  heap/native
  memory, executor queue depth, connection pools, and downstream saturation.
- Instrument every pipeline stage with Micrometer timers and outcome counters. Use bounded tags such as stage, document
  type,
  and outcome; never use document/order IDs, filenames, exception messages, or user values as metric labels.
- Use JFR and a profiler to find CPU/allocation/lock bottlenecks. Use JMH for isolated JVM microbenchmarks and an
  end-to-end representative workload for service claims. A faster microbenchmark does not prove lower service p95.
- Optimise the measured bottleneck. Prefer algorithmic and I/O reductions before caches, custom pools, or JVM flags.
- Treat queueing as part of latency. Backpressure, admission control, bounded queues, and overload behaviour must be
  tested.
- Any hot-path change must include before/after evidence or a repeatable benchmark plan. Reject improvements that trade
  correctness, accuracy, or resilience for an unreported latency gain.

## Observability and Operations

- Use structured logs with correlation/trace IDs, document surrogate ID, stage, attempt, duration, and sanitised
  outcome.
- Record stage latency, queue wait, throughput, retries, timeouts, circuit state, outbox lag, consumer lag,
  manual-review
  rate, extraction confidence, and token/cost usage where applicable.
- Logs answer what happened to one job; metrics answer whether the system is healthy; traces show cross-stage critical
  paths.
  Do not substitute one signal for all three.
- Health checks must distinguish liveness from readiness. Liveness must not depend on remote services; readiness should
  fail only when this instance cannot safely accept work.
- Every background worker needs a graceful shutdown path: stop intake, finish or safely abandon bounded in-flight work,
  persist retryable state, and release native/temp resources.

## Testing Strategy

- Test behaviour, invariants, failure modes, and contracts rather than implementation details.
- Use fast unit tests for domain policies and state transitions; Spring slice tests for HTTP/persistence boundaries; and
  Testcontainers integration tests for PostgreSQL, Kafka, and other infrastructure whose semantics matter.
- Do not mock a database, broker, object store, or model response when the behaviour under test depends on its actual
  transaction, serialization, retry, or compatibility semantics.
- Add tests for duplicate delivery, stale concurrent updates, retry exhaustion, timeouts, cancellation, partial
  failures,
  outbox recovery, oversized/malformed files, authorisation, and sensitive-data redaction.
- Maintain versioned, sanitised golden documents and expected outputs for OCR/structured extraction. Track
  precision/recall
  and manual-review rate; never replace an accuracy regression test with a schema-only assertion.
- Keep tests deterministic: fixed clocks/IDs/seeds, controlled executors, and bounded waits. Do not use arbitrary
  sleeps.
- A bug fix requires a regression test that fails for the original defect when practical.
- JaCoCo's 70% branch threshold is a build floor, not a target. Do not dilute assertions, add exclusions, or lower
  quality
  gates to make CI pass.

## Build and Validation

Use the Maven Wrapper and the same verification path as CI.

```powershell
.\mvnw.cmd --batch-mode --no-transfer-progress clean verify
```

On Unix-like systems:

```bash
./mvnw --batch-mode --no-transfer-progress clean verify
```

During iteration, run the narrowest relevant test first, then full `verify` before handoff when feasible. Do not skip
tests,
quality gates, or annotation processing. If a check cannot run because Docker, Java, native OCR, a model, credentials,
or
network access is unavailable, report exactly what ran and what remains unverified.

## Definition of Done

A change is complete only when:

- behaviour matches the request and relevant specifications;
- architectural boundaries and security controls remain intact;
- happy path, edge cases, and material failure/concurrency modes are tested;
- migrations and API/event changes are backward-compatible or explicitly documented, with the authoritative OpenAPI,
  AsyncAPI, or equivalent contract and generated artifacts updated in the same change;
- logs, metrics, traces, and runbooks are updated when operational behaviour changes;
- performance-sensitive changes include evidence or a reproducible measurement plan;
- the relevant Maven checks pass, and the final diff contains no unrelated edits, secrets, generated output, or debug
  code;
- the handoff states what changed, what was verified, and any remaining risk.
