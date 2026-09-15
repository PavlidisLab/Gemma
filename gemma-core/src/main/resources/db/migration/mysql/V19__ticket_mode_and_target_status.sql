-- Fill the gap between Gemma 2.0's Ticket entity and the curation-UI / agent
-- contract: TICKET needs a MODE column (MANUAL | AUTO advance), TICKET_TARGET
-- needs a STATUS column (NOT_DONE | UNDERWAY | DONE per-target progress).
--
-- Both columns get NOT NULL with safe defaults so existing rows backfill
-- without explicit UPDATEs:
--   - MANUAL is the conservative default — existing tickets continue to require
--     explicit curator action for each next step.
--   - NOT_DONE on TICKET_TARGET preserves the prior implicit semantic (a target
--     exists in the work set; no progress recorded).
--
-- Column widths mirror the existing PRIORITY (VARCHAR(16)) and TARGET_TYPE
-- (VARCHAR(32)) conventions — the enum strings are short.

ALTER TABLE TICKET
    ADD COLUMN MODE VARCHAR(16) NOT NULL DEFAULT 'MANUAL';

ALTER TABLE TICKET_TARGET
    ADD COLUMN STATUS VARCHAR(16) NOT NULL DEFAULT 'NOT_DONE';
