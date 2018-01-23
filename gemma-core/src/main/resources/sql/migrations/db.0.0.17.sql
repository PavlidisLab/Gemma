delete from AUDIT_EVENT where AUDIT_TRAIL_FK in (
  select AUDIT_TRAIL_FK from PROTOCOL where ID in
  (
    select PROTOCOL_FK from ANALYSIS where class='ProbeCoexpressionAnalysisImpl'
  )
);

delete from AUDIT_TRAIL where ID in (
  select AUDIT_TRAIL_FK from PROTOCOL where ID in
  (
    select PROTOCOL_FK from ANALYSIS where class='ProbeCoexpressionAnalysisImpl'
  )
);

delete from PROTOCOL where ID in (
  select PROTOCOL_FK from ANALYSIS where class='ProbeCoexpressionAnalysisImpl'
);

-- Unused table
DROP TABLE IF EXISTS COEXPRESSION_PROBE;

-- Non-existing instances
DELETE FROM ANALYSIS WHERE class='ProbeCoexpressionAnalysisImpl';
DELETE FROM ANALYSIS WHERE class='GeneCoexpressionAnalysisImpl';

-- Change by Paul, moved to this file from .12_postponed, since the changes in that file were already executed.
ALTER TABLE ANALYSIS_RESULT_SET DROP COLUMN QVALUE_THRESHOLD_FOR_STORAGE;
