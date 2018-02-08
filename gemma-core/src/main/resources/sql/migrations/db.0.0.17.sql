delete from AUDIT_EVENT where AUDIT_TRAIL_FK in (
  select AUDIT_TRAIL_FK from PROTOCOL where ID in
  (
    select PROTOCOL_FK from ANALYSIS where class='ProbeCoexpressionAnalysisImpl' or class='GeneCoexpressionAnalysisImpl'
  )
);

delete from AUDIT_TRAIL where ID in (
  select AUDIT_TRAIL_FK from PROTOCOL where ID in
  (
    select PROTOCOL_FK from ANALYSIS where class='ProbeCoexpressionAnalysisImpl' or class='GeneCoexpressionAnalysisImpl'
  )
);

delete from PROTOCOL where ID in (
  select PROTOCOL_FK from ANALYSIS where class='ProbeCoexpressionAnalysisImpl' or class='GeneCoexpressionAnalysisImpl'
);

-- Unused table
DROP TABLE IF EXISTS COEXPRESSION_PROBE;
DROP TABLE IF EXISTS HUMAN_GENE_CO_EXPRESSION;
DROP TABLE IF EXISTS RAT_GENE_CO_EXPRESSION;
DROP TABLE IF EXISTS MOUSE_GENE_CO_EXPRESSION;
DROP TABLE IF EXISTS OTHER_GENE_CO_EXPRESSION;

-- Non-existing instances
DELETE FROM ANALYSIS WHERE class='ProbeCoexpressionAnalysisImpl';
DELETE FROM ANALYSIS WHERE class='GeneCoexpressionAnalysisImpl';

-- Change by Paul, moved to this file from .12_postponed, since the changes in that file were already executed.
ALTER TABLE ANALYSIS_RESULT_SET DROP COLUMN QVALUE_THRESHOLD_FOR_STORAGE;
