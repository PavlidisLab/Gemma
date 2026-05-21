-- PERF_PROBE_REPORT_ROUND4 finding B1/B2: 30+ HQLs in ExpressionExperimentDaoImpl
-- scan the 12.6M-row SINGLE_CELL_EXPRESSION_DATA_VECTOR table just to group-by
-- the (EE, QT, SingleCellDimension) tuple. This link table denormalizes the
-- relationship so those queries become single-row index lookups instead.
--
-- Prod cardinality at migration-design time (gemd 2026-05-21):
--   SCEDV rows:                 12,656,565
--   distinct EEs with SC data:        523
--   distinct (EE, QT):                528   (5 EEs have 2 QTs)
--   distinct (EE, QT, SCD):           528   (no EE has alternate SCDs today)
--
-- Pure-additive: this commit only CREATEs the table + backfills it from
-- existing SCEDV rows. No callers exist yet; consumers migrate in a
-- follow-up commit. The Java listener that maintains the table on SC
-- vector insert/delete is in the same follow-up.

CREATE TABLE SINGLE_CELL_DIMENSION_EXPERIMENT (
    ID                              BIGINT       NOT NULL AUTO_INCREMENT,
    EXPRESSION_EXPERIMENT_FK        BIGINT       NOT NULL,
    QUANTITATION_TYPE_FK            BIGINT       NOT NULL,
    SINGLE_CELL_DIMENSION_FK        BIGINT       NOT NULL,
    PRIMARY KEY (ID),
    CONSTRAINT FK_SCDE_EXPRESSION_EXPERIMENT
        FOREIGN KEY (EXPRESSION_EXPERIMENT_FK) REFERENCES INVESTIGATION (ID),
    CONSTRAINT FK_SCDE_QUANTITATION_TYPE
        FOREIGN KEY (QUANTITATION_TYPE_FK) REFERENCES QUANTITATION_TYPE (ID),
    CONSTRAINT FK_SCDE_SINGLE_CELL_DIMENSION
        FOREIGN KEY (SINGLE_CELL_DIMENSION_FK) REFERENCES SINGLE_CELL_DIMENSION (ID),
    -- Today (2026-05-21) every (EE, QT) maps to exactly one SCD; the unique
    -- key reflects that. If curators later add alternate dimensions per QT,
    -- relax this to a plain index in a separate migration.
    UNIQUE KEY UK_SCDE_EE_QT (EXPRESSION_EXPERIMENT_FK, QUANTITATION_TYPE_FK),
    INDEX IDX_SCDE_SCD (SINGLE_CELL_DIMENSION_FK)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- Backfill from existing SCEDV rows. Distinct tuple count expected: ~528
-- rows; entire backfill runs in well under a second against the live SCEDV
-- index (the (EE_FK) leading index is already present and selective).
INSERT INTO SINGLE_CELL_DIMENSION_EXPERIMENT
    (EXPRESSION_EXPERIMENT_FK, QUANTITATION_TYPE_FK, SINGLE_CELL_DIMENSION_FK)
SELECT DISTINCT
    EXPRESSION_EXPERIMENT_FK,
    QUANTITATION_TYPE_FK,
    SINGLE_CELL_DIMENSION_FK
FROM SINGLE_CELL_EXPRESSION_DATA_VECTOR;
