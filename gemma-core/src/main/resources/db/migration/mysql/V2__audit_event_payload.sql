-- Phase A of the audit migration (AUDIT_SYSTEM_AUDIT.md):
-- introduce a JSON payload column on AUDIT_EVENT so @Audited-annotated
-- service methods can attach typed, structured per-event data alongside
-- the existing NOTE / DETAIL strings.
--
-- MySQL JSON: validated by the engine at write time. Nullable: existing
-- rows (and any future row whose @Audited method took no AuditEventPayload
-- parameter) leave it NULL.
ALTER TABLE `AUDIT_EVENT` ADD COLUMN `PAYLOAD` JSON NULL;
