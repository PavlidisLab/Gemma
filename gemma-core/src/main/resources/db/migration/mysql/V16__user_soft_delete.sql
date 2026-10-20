-- Soft-delete columns for the User subclass (single-table CONTACT, discriminator='User').
--
-- The admin /admin/users surface needs DELETE semantics that survive ACL sids,
-- audit-event authorship FKs, and other references to the user row. Hard-deleting
-- a user that owns any auditable entity would orphan those references. Instead
-- the DELETE endpoint sets DELETED_AT + DELETED_BY and flips ENABLED to 0.
-- The row stays around to satisfy FK constraints; GET /admin/users filters out
-- DELETED_AT IS NOT NULL by default.
--
-- Columns:
--   DELETED_AT  — UTC timestamp the soft-delete happened. NULL = active.
--   DELETED_BY  — username of the admin who issued the soft-delete. NULL = active.
--                 Kept as a free-form string (not a FK) so admin churn doesn't
--                 cascade-update this column.

ALTER TABLE CONTACT
    ADD COLUMN DELETED_AT DATETIME(3) NULL,
    ADD COLUMN DELETED_BY VARCHAR(255) NULL;

-- Lookup index for the default "active users" listing.
CREATE INDEX CONTACT_DELETED_AT_IDX ON CONTACT (DELETED_AT);
