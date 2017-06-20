-- ! execute before updating to version after db.0.0.14 !

-- Change by Paul, moved to this file from .12_postponed, since the changes in that file were already executed.
ALTER TABLE ANALYSIS_RESULT_SET DROP COLUMN QVALUE_THRESHOLD_FOR_STORAGE;