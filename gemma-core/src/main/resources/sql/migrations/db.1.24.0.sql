ALTER TABLE ANALYSIS
  DROP FOREIGN KEY FKF19622DCE87E2E5B,
  DROP KEY FKF19622DCE87E2E5B,
  DROP COLUMN SAMPLE_COEXPRESSION_MATRIX_FK,

  ADD COLUMN SAMPLE_COEXPRESSION_MATRIX_RAW_FK BIGINT(20) DEFAULT NULL,
  ADD COLUMN SAMPLE_COEXPRESSION_MATRIX_REG_FK BIGINT(20) DEFAULT NULL,

  ADD INDEX KEY_COEX_MATRIX_RAW(SAMPLE_COEXPRESSION_MATRIX_RAW_FK),
  ADD INDEX KEY_COEX_MATRIX_REG(SAMPLE_COEXPRESSION_MATRIX_REG_FK),

  ADD CONSTRAINT FK_COEX_MATRIX_RAW FOREIGN KEY (SAMPLE_COEXPRESSION_MATRIX_RAW_FK) REFERENCES `SAMPLE_COEXPRESSION_MATRIX` (`ID`),
  ADD CONSTRAINT FK_COEX_MATRIX_REG FOREIGN KEY (SAMPLE_COEXPRESSION_MATRIX_REG_FK) REFERENCES `SAMPLE_COEXPRESSION_MATRIX` (`ID`);

-- Fix bug introduced in db.0.0.12
ALTER TABLE CURATION_DETAILS
  DROP FOREIGN KEY TROUBLE_AUDIT_EVENT_FK,
  DROP KEY note_audit_event_fk_1,
  DROP FOREIGN KEY CURATION_DETAILS_ibfk_1,
  DROP FOREIGN KEY CURATION_DETAILS_ibfk_2,
  DROP FOREIGN KEY CURATION_DETAILS_ibfk_3;

ALTER TABLE CURATION_DETAILS
  ADD CONSTRAINT FK_TROUBLE_EVENT FOREIGN KEY (TROUBLE_AUDIT_EVENT_FK) REFERENCES AUDIT_EVENT (ID),
  ADD CONSTRAINT FK_ATTENTION_EVENT FOREIGN KEY (ATTENTION_AUDIT_EVENT_FK) REFERENCES AUDIT_EVENT (ID),
  ADD CONSTRAINT FK_NOTE_EVENT FOREIGN KEY (NOTE_AUDIT_EVENT_FK) REFERENCES AUDIT_EVENT (ID);