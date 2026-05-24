-- GEO scrape & preboard pipeline. Paul-approved 2026-05-23.
--
-- Adds:
--   1. GEO_SCRAPE_WATERMARK — append-only log of GEO scrape runs (one row per
--      scrape invocation). SCAN_FROM/SCAN_TO define the time window the scrape
--      covered; STATUS tracks lifecycle (IN_PROGRESS / COMPLETED / FAILED /
--      CANCELLED). The latest COMPLETED row's SCAN_TO is the high-water mark
--      the next scrape resumes from.
--   2. PREBOARDED_EXPERIMENT.MATCHED_CRITERIA (via INVESTIGATION) — JSON string
--      listing the matcher names that flagged this preboarded (e.g.
--      ["brain","tfperturb"]). Free-form JSON-as-string; the curation-UI
--      parses it client-side.

CREATE TABLE GEO_SCRAPE_WATERMARK (
    ID BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    SCANNED_AT DATETIME(3) NOT NULL,
    SCAN_FROM DATETIME(3) NULL,
    SCAN_TO DATETIME(3) NULL,
    RECORDS_SCANNED INT NOT NULL DEFAULT 0,
    RECORDS_MATCHED INT NOT NULL DEFAULT 0,
    CRITERIA_APPLIED VARCHAR(1024) NULL,
    STATUS VARCHAR(32) NOT NULL DEFAULT 'COMPLETED', -- COMPLETED, FAILED, CANCELLED, IN_PROGRESS
    ERROR_MESSAGE TEXT NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX GEO_SCRAPE_WATERMARK_SCANNED_AT_IDX ON GEO_SCRAPE_WATERMARK (SCANNED_AT);

-- Preboarded.matchedCriteria — JSON-as-string listing matcher names that
-- flagged the preboarded during a scrape. Lives on INVESTIGATION (single-table
-- inheritance) under the PREBOARDED_ prefix.
ALTER TABLE INVESTIGATION
    ADD COLUMN PREBOARDED_MATCHED_CRITERIA TEXT NULL;
