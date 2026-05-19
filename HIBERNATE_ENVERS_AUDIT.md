# Hibernate Envers Audit

Hibernate Envers is **not in use** in Gemma. No `@Audited` annotations, no `AuditReader` references, no `hibernate-envers` dependency in any `pom.xml`, and no `envers` configuration in Spring/Hibernate properties.

Gemma's audit trail is a hand-rolled domain model (`AuditTrail` / `AuditEvent` entities with `AUDIT_TRAIL_FK` columns in HBM mappings), not Envers-backed revision history. No Hibernate-6 Envers integration work is required for the Phase 3 / Spring 6 modernization.
