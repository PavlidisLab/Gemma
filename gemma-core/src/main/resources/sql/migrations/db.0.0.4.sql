-- update to revised ACL setup. Here is the schema for the new tables. They are named distinctly so we don't clash with old tables during transition.

DROP TABLE IF EXISTS ACLENTRY;
DROP TABLE IF EXISTS ACLOBJECTIDENTITY;
DROP TABLE IF EXISTS ACLSID;

CREATE TABLE ACLOBJECTIDENTITY (
  ID                 BIGINT  NOT NULL AUTO_INCREMENT,
  OBJECT_ID          BIGINT  NOT NULL,
  OBJECT_CLASS       VARCHAR(255) CHARACTER SET latin1
  COLLATE latin1_swedish_ci  NOT NULL,
  ENTRIES_INHERITING TINYINT NOT NULL,
  OWNER_SID_FK       BIGINT  NOT NULL,
  PARENT_OBJECT_FK   BIGINT,
  PRIMARY KEY (ID)
);

CREATE TABLE ACLENTRY (
  ID                BIGINT  NOT NULL AUTO_INCREMENT,
  GRANTING          TINYINT NOT NULL,
  MASK              INTEGER NOT NULL,
  ACE_ORDER         INTEGER NOT NULL,
  SID_FK            BIGINT  NOT NULL,
  OBJECTIDENTITY_FK BIGINT,
  PRIMARY KEY (ID)
);

CREATE TABLE ACLSID (
  ID                BIGINT       NOT NULL AUTO_INCREMENT,
  class             VARCHAR(255) NOT NULL,
  PRINCIPAL         VARCHAR(255) CHARACTER SET latin1
  COLLATE latin1_swedish_ci UNIQUE,
  GRANTED_AUTHORITY VARCHAR(255) CHARACTER SET latin1
  COLLATE latin1_swedish_ci UNIQUE,
  PRIMARY KEY (ID)
);

-- old schema
--  `id` bigint(20) NOT NULL AUTO_INCREMENT,
--  `object_id_class` bigint(20) NOT NULL,
--  `object_id_identity` bigint(20) NOT NULL,
--  `parent_object` bigint(20) DEFAULT NULL,
--  `owner_sid` bigint(20) DEFAULT NULL,
--  `entries_inheriting` tinyint(4) DEFAULT NULL,


INSERT INTO ACLSID (ID, PRINCIPAL, class) SELECT
                                            id,
                                            sid,
                                            "PrincipalSid"
                                          FROM acl_sid os
                                          WHERE os.principal = 1;
INSERT INTO ACLSID (ID, GRANTED_AUTHORITY, class) SELECT
                                                    id,
                                                    sid,
                                                    "GrantedAuthoritySid"
                                                  FROM acl_sid os
                                                  WHERE os.principal = 0;


INSERT INTO ACLENTRY (ID, GRANTING, ACE_ORDER, OBJECTIDENTITY_FK, SID_FK, MASK)
  SELECT
    id,
    granting,
    ace_order,
    acl_object_identity,
    sid,
    mask
  FROM acl_entry;


INSERT INTO ACLOBJECTIDENTITY (ID, OBJECT_ID, ENTRIES_INHERITING, OWNER_SID_FK, PARENT_OBJECT_FK)
  SELECT
    id,
    object_id_identity,
    entries_inheriting,
    owner_sid,
    parent_object
  FROM acl_object_identity;

UPDATE ACLOBJECTIDENTITY aoi, acl_class ac, acl_object_identity oldaoi
SET aoi.OBJECT_CLASS = ac.class
WHERE ac.id = oldaoi.object_id_class AND aoi.id = oldaoi.id;

-- add indices and constraints
ALTER TABLE ACLENTRY
  ADD INDEX FK2FB5F83D3F8EF1C4 (SID_FK),
  ADD CONSTRAINT FK2FB5F83D3F8EF1C4 FOREIGN KEY (SID_FK) REFERENCES ACLSID (ID);

ALTER TABLE ACLENTRY
  ADD INDEX ACL_ENTRY_OBJECTIDENTITY_FKC (OBJECTIDENTITY_FK),
  ADD CONSTRAINT ACL_ENTRY_OBJECTIDENTITY_FKC FOREIGN KEY (OBJECTIDENTITY_FK) REFERENCES ACLOBJECTIDENTITY (ID);

ALTER TABLE ACLOBJECTIDENTITY
  ADD INDEX FK988CEFE926291CD0 (OWNER_SID_FK),
  ADD CONSTRAINT FK988CEFE926291CD0 FOREIGN KEY (OWNER_SID_FK) REFERENCES ACLSID (ID);

ALTER TABLE ACLOBJECTIDENTITY
  ADD INDEX FK988CEFE95223826D (PARENT_OBJECT_FK),
  ADD CONSTRAINT FK988CEFE95223826D FOREIGN KEY (PARENT_OBJECT_FK) REFERENCES ACLOBJECTIDENTITY (ID);

ALTER TABLE ACLOBJECTIDENTITY
  ADD UNIQUE KEY acloid (OBJECT_CLASS, OBJECT_ID);

-- remove cruft from aclentry? These SecuredChild classes should not have their own ACEs.


-- items which have aces, but shouldn't and they don't have a parent. These are probably for entities that no longer exist, but have to check.
SELECT count(*)
FROM ACLENTRY e INNER JOIN ACLOBJECTIDENTITY o ON o.ID = e.OBJECTIDENTITY_FK
  LEFT JOIN BIO_ASSAY b ON b.ID = o.OBJECT_ID
WHERE
  o.OBJECT_CLASS = "ubic.gemma.model.expression.bioAssay.BioAssayImpl" AND o.PARENT_OBJECT_FK IS NULL AND b.ID IS NULL;
SELECT count(*)
FROM ACLENTRY e INNER JOIN ACLOBJECTIDENTITY o ON o.ID = e.OBJECTIDENTITY_FK
  LEFT JOIN EXPERIMENTAL_DESIGN b ON b.ID = o.OBJECT_ID
WHERE
  o.OBJECT_CLASS = "ubic.gemma.model.expression.experiment.ExperimentalDesignImpl" AND o.PARENT_OBJECT_FK IS NULL AND
  b.ID IS NULL;
SELECT count(*)
FROM ACLENTRY e INNER JOIN ACLOBJECTIDENTITY o ON o.ID = e.OBJECTIDENTITY_FK
  LEFT JOIN BIO_MATERIAL b ON b.ID = o.OBJECT_ID
WHERE o.OBJECT_CLASS = "ubic.gemma.model.expression.biomaterial.BioMaterialImpl" AND o.PARENT_OBJECT_FK IS NULL AND
      b.ID IS NULL;
SELECT count(*)
FROM ACLENTRY e INNER JOIN ACLOBJECTIDENTITY o ON o.ID = e.OBJECTIDENTITY_FK
  LEFT JOIN EXPERIMENTAL_FACTOR b ON b.ID = o.OBJECT_ID
WHERE
  o.OBJECT_CLASS = "ubic.gemma.model.expression.experiment.ExperimentalFactorImpl" AND o.PARENT_OBJECT_FK IS NULL AND
  b.ID IS NULL;
SELECT count(*)
FROM ACLENTRY e INNER JOIN ACLOBJECTIDENTITY o ON o.ID = e.OBJECTIDENTITY_FK
  LEFT JOIN FACTOR_VALUE b ON b.ID = o.OBJECT_ID
WHERE o.OBJECT_CLASS = "ubic.gemma.model.expression.experiment.FactorValueImpl" AND o.PARENT_OBJECT_FK IS NULL AND
      b.ID IS NULL;

SELECT count(*)
FROM ACLENTRY e INNER JOIN ACLOBJECTIDENTITY o ON o.ID = e.OBJECTIDENTITY_FK
  LEFT JOIN PHENOTYPE_ASSOCIATION b ON b.ID = o.OBJECT_ID
WHERE o.OBJECT_CLASS LIKE "ubic.gemma.model.%EvidenceImpl" AND o.PARENT_OBJECT_FK IS NULL AND b.ID IS NULL;

-- these are all zero.
SELECT count(*)
FROM ACLENTRY e INNER JOIN ACLOBJECTIDENTITY o ON o.ID = e.OBJECTIDENTITY_FK
  LEFT JOIN ANALYSIS b ON b.ID = o.OBJECT_ID
WHERE b.class = "DifferentialExpressionAnalysisImpl" AND
      o.OBJECT_CLASS = "ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisImpl" AND
      o.PARENT_OBJECT_FK IS NULL AND b.ID IS NULL;
SELECT count(*)
FROM ACLENTRY e INNER JOIN ACLOBJECTIDENTITY o ON o.ID = e.OBJECTIDENTITY_FK
  LEFT JOIN ANALYSIS b ON b.ID = o.OBJECT_ID
WHERE b.class = "ProbeCoexpressionAnalysisImpl" AND
      o.OBJECT_CLASS = "ubic.gemma.model.analysis.expression.coexpression.ProbeCoexpressionAnalysisImpl" AND
      o.PARENT_OBJECT_FK IS NULL AND b.ID IS NULL;
SELECT count(*)
FROM ACLENTRY e INNER JOIN ACLOBJECTIDENTITY o ON o.ID = e.OBJECTIDENTITY_FK
  LEFT JOIN ANALYSIS b ON b.ID = o.OBJECT_ID
WHERE b.class = "SampleCoexpressionAnalysisImpl" AND
      o.OBJECT_CLASS = "ubic.gemma.model.analysis.expression.coexpression.SampleCoexpressionAnalysisImpl" AND
      o.PARENT_OBJECT_FK IS NULL AND b.ID IS NULL;
SELECT count(*)
FROM ACLENTRY e INNER JOIN ACLOBJECTIDENTITY o ON o.ID = e.OBJECTIDENTITY_FK
  LEFT JOIN ANALYSIS b ON b.ID = o.OBJECT_ID
WHERE b.class = "PrincipalComponentAnalysisImpl" AND
      o.OBJECT_CLASS = "ubic.gemma.model.analysis.expression.pca.PrincipalComponentAnalysisImpl" AND
      o.PARENT_OBJECT_FK IS NULL AND b.ID IS NULL;

SET FOREIGN_KEY_CHECKS = 0;
DELETE e, o FROM ACLOBJECTIDENTITY o
  JOIN ACLENTRY e ON o.ID = e.OBJECTIDENTITY_FK
  LEFT JOIN BIO_ASSAY b ON b.ID = o.OBJECT_ID
WHERE
  o.OBJECT_CLASS = "ubic.gemma.model.expression.bioAssay.BioAssayImpl" AND o.PARENT_OBJECT_FK IS NULL AND b.ID IS NULL;
DELETE e, o FROM ACLOBJECTIDENTITY o
  JOIN ACLENTRY e ON o.ID = e.OBJECTIDENTITY_FK
  LEFT JOIN EXPERIMENTAL_DESIGN b ON b.ID = o.OBJECT_ID
WHERE
  o.OBJECT_CLASS = "ubic.gemma.model.expression.experiment.ExperimentalDesignImpl" AND o.PARENT_OBJECT_FK IS NULL AND
  b.ID IS NULL;
DELETE e, o FROM ACLOBJECTIDENTITY o
  JOIN ACLENTRY e ON o.ID = e.OBJECTIDENTITY_FK
  LEFT JOIN BIO_MATERIAL b ON b.ID = o.OBJECT_ID
WHERE o.OBJECT_CLASS = "ubic.gemma.model.expression.biomaterial.BioMaterialImpl" AND o.PARENT_OBJECT_FK IS NULL AND
      b.ID IS NULL;
DELETE e, o FROM ACLOBJECTIDENTITY o
  JOIN ACLENTRY e ON o.ID = e.OBJECTIDENTITY_FK
  LEFT JOIN EXPERIMENTAL_FACTOR b ON b.ID = o.OBJECT_ID
WHERE
  o.OBJECT_CLASS = "ubic.gemma.model.expression.experiment.ExperimentalFactorImpl" AND o.PARENT_OBJECT_FK IS NULL AND
  b.ID IS NULL;
DELETE e, o FROM ACLOBJECTIDENTITY o
  JOIN ACLENTRY e ON o.ID = e.OBJECTIDENTITY_FK
  LEFT JOIN FACTOR_VALUE b ON b.ID = o.OBJECT_ID
WHERE o.OBJECT_CLASS = "ubic.gemma.model.expression.experiment.FactorValueImpl" AND o.PARENT_OBJECT_FK IS NULL AND
      b.ID IS NULL;
SET FOREIGN_KEY_CHECKS = 1;

-- checking the previous to make sure they don't exist in the entity table. These should return 0.
SELECT count(*)
FROM ACLENTRY e INNER JOIN ACLOBJECTIDENTITY o ON o.ID = e.OBJECTIDENTITY_FK
  INNER JOIN BIO_ASSAY ba ON ba.ID = o.OBJECT_ID
WHERE o.OBJECT_CLASS = "ubic.gemma.model.expression.bioAssay.BioAssayImpl" AND o.PARENT_OBJECT_FK IS NULL;
SELECT count(*)
FROM ACLENTRY e INNER JOIN ACLOBJECTIDENTITY o ON o.ID = e.OBJECTIDENTITY_FK
  INNER JOIN EXPERIMENTAL_DESIGN ed ON ed.ID = o.OBJECT_ID
WHERE o.OBJECT_CLASS = "ubic.gemma.model.expression.experiment.ExperimentalDesignImpl" AND o.PARENT_OBJECT_FK IS NULL;
SELECT count(*)
FROM ACLENTRY e INNER JOIN ACLOBJECTIDENTITY o ON o.ID = e.OBJECTIDENTITY_FK
  INNER JOIN BIO_MATERIAL bm ON bm.ID = o.OBJECT_ID
WHERE o.OBJECT_CLASS = "ubic.gemma.model.expression.biomaterial.BioMaterialImpl" AND o.PARENT_OBJECT_FK IS NULL;
SELECT count(*)
FROM ACLENTRY e INNER JOIN ACLOBJECTIDENTITY o ON o.ID = e.OBJECTIDENTITY_FK
  INNER JOIN EXPERIMENTAL_FACTOR ef ON ef.ID = o.OBJECT_ID
WHERE o.OBJECT_CLASS = "ubic.gemma.model.expression.experiment.ExperimentalFactorImpl" AND o.PARENT_OBJECT_FK IS NULL;
SELECT count(*)
FROM ACLENTRY e INNER JOIN ACLOBJECTIDENTITY o ON o.ID = e.OBJECTIDENTITY_FK
  INNER JOIN FACTOR_VALUE fv ON fv.ID = o.OBJECT_ID
WHERE o.OBJECT_CLASS = "ubic.gemma.model.expression.experiment.FactorValueImpl" AND o.PARENT_OBJECT_FK IS NULL;
SELECT count(*)
FROM ACLENTRY e INNER JOIN ACLOBJECTIDENTITY o ON o.ID = e.OBJECTIDENTITY_FK
  INNER JOIN ANALYSIS a ON a.ID = o.OBJECT_ID
WHERE o.OBJECT_CLASS = "ubic.gemma.model.analysis.expression.coexpression.SampleCoexpressionAnalysisImpl" AND
      o.PARENT_OBJECT_FK IS NULL;
SELECT count(*)
FROM ACLENTRY e INNER JOIN ACLOBJECTIDENTITY o ON o.ID = e.OBJECTIDENTITY_FK
  INNER JOIN ANALYSIS a ON a.ID = o.OBJECT_ID
WHERE o.OBJECT_CLASS = "ubic.gemma.model.analysis.expression.pca.PrincipalComponentAnalysisImpl" AND
      o.PARENT_OBJECT_FK IS NULL;

-- these are kind of messed up.
SELECT count(*)
FROM ACLENTRY e INNER JOIN ACLOBJECTIDENTITY o ON o.ID = e.OBJECTIDENTITY_FK
  INNER JOIN ANALYSIS a ON a.ID = o.OBJECT_ID
WHERE o.OBJECT_CLASS = "ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisImpl" AND
      o.PARENT_OBJECT_FK IS NULL;
SELECT count(*)
FROM ACLENTRY e INNER JOIN ACLOBJECTIDENTITY o ON o.ID = e.OBJECTIDENTITY_FK
  INNER JOIN ANALYSIS a ON a.ID = o.OBJECT_ID
WHERE o.OBJECT_CLASS = "ubic.gemma.model.analysis.expression.coexpression.ProbeCoexpressionAnalysisImpl" AND
      o.PARENT_OBJECT_FK IS NULL;


SELECT *
FROM ACLOBJECTIDENTITY o, ACLOBJECTIDENTITY po, ANALYSIS a
WHERE
  a.ID = o.OBJECT_ID AND
  a.EXPERIMENT_ANALYZED_FK = po.OBJECT_ID
  AND po.OBJECT_CLASS = "ubic.gemma.model.expression.experiment.ExpressionExperimentImpl"
  AND o.OBJECT_CLASS = "ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisImpl"
  AND a.class = "DifferentialExpressionAnalysisImpl"
  AND o.PARENT_OBJECT_FK IS NULL;

SELECT *
FROM ACLOBJECTIDENTITY o, ACLOBJECTIDENTITY po, ANALYSIS a
WHERE
  a.ID = o.OBJECT_ID AND
  a.EXPERIMENT_ANALYZED_FK = po.OBJECT_ID
  AND po.OBJECT_CLASS = "ubic.gemma.model.expression.experiment.ExpressionExperimentImpl"
  AND o.OBJECT_CLASS = "ubic.gemma.model.analysis.expression.pca.PrincipalComponentAnalysisImpl"
  AND a.class = "PrincipalComponentAnalysisImpl"
  AND o.PARENT_OBJECT_FK IS NULL;

SELECT *
FROM ACLOBJECTIDENTITY o, ACLOBJECTIDENTITY po, ANALYSIS a
WHERE
  a.ID = o.OBJECT_ID AND
  a.EXPERIMENT_ANALYZED_FK = po.OBJECT_ID
  AND po.OBJECT_CLASS = "ubic.gemma.model.expression.experiment.ExpressionExperimentImpl"
  AND o.OBJECT_CLASS = "ubic.gemma.model.analysis.expression.coexpression.ProbeCoexpressionAnalysisImpl"
  AND a.class = "ProbeCoexpressionAnalysisImpl"
  AND o.PARENT_OBJECT_FK IS NULL;


UPDATE ACLOBJECTIDENTITY o, ACLOBJECTIDENTITY po, ANALYSIS a
SET o.PARENT_OBJECT_FK = po.ID
WHERE
  a.ID = o.OBJECT_ID AND
  a.EXPERIMENT_ANALYZED_FK = po.OBJECT_ID
  AND po.OBJECT_CLASS = "ubic.gemma.model.expression.experiment.ExpressionExperimentImpl"
  AND o.OBJECT_CLASS = "ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisImpl"
  AND a.class = "DifferentialExpressionAnalysisImpl"
  AND o.PARENT_OBJECT_FK IS NULL;

UPDATE ACLOBJECTIDENTITY o, ACLOBJECTIDENTITY po, ANALYSIS a
SET o.PARENT_OBJECT_FK = po.ID
WHERE
  a.ID = o.OBJECT_ID AND
  a.EXPERIMENT_ANALYZED_FK = po.OBJECT_ID
  AND po.OBJECT_CLASS = "ubic.gemma.model.expression.experiment.ExpressionExperimentImpl"
  AND o.OBJECT_CLASS = "ubic.gemma.model.analysis.expression.coexpression.ProbeCoexpressionAnalysisImpl"
  AND a.class = "ProbeCoexpressionAnalysisImpl"
  AND o.PARENT_OBJECT_FK IS NULL;

UPDATE ACLOBJECTIDENTITY o, ACLOBJECTIDENTITY po, ANALYSIS a
SET o.PARENT_OBJECT_FK = po.ID
WHERE
  a.ID = o.OBJECT_ID AND
  a.EXPERIMENT_ANALYZED_FK = po.OBJECT_ID
  AND po.OBJECT_CLASS = "ubic.gemma.model.expression.experiment.ExpressionExperimentImpl"
  AND o.OBJECT_CLASS = "ubic.gemma.model.analysis.expression.pca.PrincipalComponentAnalysisImpl"
  AND a.class = "PrincipalComponentAnalysisImpl"
  AND o.PARENT_OBJECT_FK IS NULL;

-- mismatches between value in the acl table and the actual object class
SELECT *
FROM ACLOBJECTIDENTITY o, ACLOBJECTIDENTITY po, ANALYSIS a, ACLENTRY e
WHERE
  a.ID = o.OBJECT_ID
  AND e.OBJECTIDENTITY_FK = o.ID
  AND a.EXPERIMENT_ANALYZED_FK = po.OBJECT_ID
  AND po.OBJECT_CLASS = "ubic.gemma.model.expression.experiment.ExpressionExperimentImpl"
  AND o.OBJECT_CLASS = "ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisImpl"
  AND a.class <> "DifferentialExpressionAnalysisImpl"
  AND o.PARENT_OBJECT_FK IS NULL;

SET FOREIGN_KEY_CHECKS = 0;
DELETE e, o FROM ACLOBJECTIDENTITY o, ACLOBJECTIDENTITY po, ANALYSIS a, ACLENTRY e
WHERE
  a.ID = o.OBJECT_ID
  AND e.OBJECTIDENTITY_FK = o.ID
  AND a.EXPERIMENT_ANALYZED_FK = po.OBJECT_ID
  AND po.OBJECT_CLASS = "ubic.gemma.model.expression.experiment.ExpressionExperimentImpl"
  AND o.OBJECT_CLASS = "ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisImpl"
  AND a.class <> "DifferentialExpressionAnalysisImpl"
  AND o.PARENT_OBJECT_FK IS NULL;
SET FOREIGN_KEY_CHECKS = 1;

SELECT *
FROM ACLOBJECTIDENTITY o, ACLOBJECTIDENTITY po, ANALYSIS a
WHERE
  a.ID = o.OBJECT_ID AND
  a.EXPERIMENT_ANALYZED_FK = po.OBJECT_ID
  AND po.OBJECT_CLASS = "ubic.gemma.model.expression.experiment.ExpressionExperimentImpl"
  AND o.OBJECT_CLASS = "ubic.gemma.model.analysis.expression.coexpression.ProbeCoexpressionAnalysisImpl"
  AND a.class <> "ProbeCoexpressionAnalysisImpl"
  AND o.PARENT_OBJECT_FK IS NULL;

DELETE e, o FROM ACLOBJECTIDENTITY o, ACLOBJECTIDENTITY po, ANALYSIS a, ACLENTRY e
WHERE
  a.ID = o.OBJECT_ID
  AND e.OBJECTIDENTITY_FK = o.ID
  AND a.EXPERIMENT_ANALYZED_FK = po.OBJECT_ID
  AND po.OBJECT_CLASS = "ubic.gemma.model.expression.experiment.ExpressionExperimentImpl"
  AND o.OBJECT_CLASS = "ubic.gemma.model.analysis.expression.coexpression.ProbeCoexpressionAnalysisImpl"
  AND a.class <> "ProbeCoexpressionAnalysisImpl"
  AND o.PARENT_OBJECT_FK IS NULL;


UPDATE ACLOBJECTIDENTITY
SET PARENT_OBJECT_FK = 1305
WHERE ID = 1032323 AND PARENT_OBJECT_FK IS NULL;

-- items which have aces, but shouldn't, and they have a parent object in the acl tables.
SELECT count(*)
FROM ACLENTRY e INNER JOIN ACLOBJECTIDENTITY o ON o.ID = e.OBJECTIDENTITY_FK
WHERE o.OBJECT_CLASS = "ubic.gemma.model.expression.bioAssay.BioAssayImpl" AND o.PARENT_OBJECT_FK IS NOT NULL;
SELECT count(*)
FROM ACLENTRY e INNER JOIN ACLOBJECTIDENTITY o ON o.ID = e.OBJECTIDENTITY_FK
WHERE
  o.OBJECT_CLASS = "ubic.gemma.model.expression.experiment.ExperimentalDesignImpl" AND o.PARENT_OBJECT_FK IS NOT NULL;
SELECT count(*)
FROM ACLENTRY e INNER JOIN ACLOBJECTIDENTITY o ON o.ID = e.OBJECTIDENTITY_FK
WHERE o.OBJECT_CLASS = "ubic.gemma.model.expression.biomaterial.BioMaterialImpl" AND o.PARENT_OBJECT_FK IS NOT NULL;
SELECT count(*)
FROM ACLENTRY e INNER JOIN ACLOBJECTIDENTITY o ON o.ID = e.OBJECTIDENTITY_FK
WHERE
  o.OBJECT_CLASS = "ubic.gemma.model.expression.experiment.ExperimentalFactorImpl" AND o.PARENT_OBJECT_FK IS NOT NULL;
SELECT count(*)
FROM ACLENTRY e INNER JOIN ACLOBJECTIDENTITY o ON o.ID = e.OBJECTIDENTITY_FK
WHERE o.OBJECT_CLASS = "ubic.gemma.model.expression.experiment.FactorValueImpl" AND o.PARENT_OBJECT_FK IS NOT NULL;
SELECT count(*)
FROM ACLENTRY e INNER JOIN ACLOBJECTIDENTITY o ON o.ID = e.OBJECTIDENTITY_FK
WHERE o.OBJECT_CLASS = "ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisImpl" AND
      o.PARENT_OBJECT_FK IS NOT NULL;
SELECT count(*)
FROM ACLENTRY e INNER JOIN ACLOBJECTIDENTITY o ON o.ID = e.OBJECTIDENTITY_FK
WHERE o.OBJECT_CLASS = "ubic.gemma.model.analysis.expression.coexpression.ProbeCoexpressionAnalysisImpl" AND
      o.PARENT_OBJECT_FK IS NOT NULL;
SELECT count(*)
FROM ACLENTRY e INNER JOIN ACLOBJECTIDENTITY o ON o.ID = e.OBJECTIDENTITY_FK
WHERE o.OBJECT_CLASS = "ubic.gemma.model.analysis.expression.coexpression.SampleCoexpressionAnalysisImpl" AND
      o.PARENT_OBJECT_FK IS NOT NULL;
SELECT count(*)
FROM ACLENTRY e INNER JOIN ACLOBJECTIDENTITY o ON o.ID = e.OBJECTIDENTITY_FK
WHERE o.OBJECT_CLASS = "ubic.gemma.model.analysis.expression.pca.PrincipalComponentAnalysisImpl" AND
      o.PARENT_OBJECT_FK IS NOT NULL;

-- remove ones where the parent object is okay - don't need aces for these securedchildren.
DELETE e FROM ACLENTRY e INNER JOIN ACLOBJECTIDENTITY o ON o.ID = e.OBJECTIDENTITY_FK
WHERE o.OBJECT_CLASS = "ubic.gemma.model.expression.bioAssay.BioAssayImpl" AND o.PARENT_OBJECT_FK IS NOT NULL;
DELETE e FROM ACLENTRY e INNER JOIN ACLOBJECTIDENTITY o ON o.ID = e.OBJECTIDENTITY_FK
WHERE
  o.OBJECT_CLASS = "ubic.gemma.model.expression.experiment.ExperimentalDesignImpl" AND o.PARENT_OBJECT_FK IS NOT NULL;
DELETE e FROM ACLENTRY e INNER JOIN ACLOBJECTIDENTITY o ON o.ID = e.OBJECTIDENTITY_FK
WHERE o.OBJECT_CLASS = "ubic.gemma.model.expression.biomaterial.BioMaterialImpl" AND o.PARENT_OBJECT_FK IS NOT NULL;
DELETE e FROM ACLENTRY e INNER JOIN ACLOBJECTIDENTITY o ON o.ID = e.OBJECTIDENTITY_FK
WHERE
  o.OBJECT_CLASS = "ubic.gemma.model.expression.experiment.ExperimentalFactorImpl" AND o.PARENT_OBJECT_FK IS NOT NULL;
DELETE e FROM ACLENTRY e INNER JOIN ACLOBJECTIDENTITY o ON o.ID = e.OBJECTIDENTITY_FK
WHERE o.OBJECT_CLASS = "ubic.gemma.model.expression.experiment.FactorValueImpl" AND o.PARENT_OBJECT_FK IS NOT NULL;
DELETE e FROM ACLENTRY e INNER JOIN ACLOBJECTIDENTITY o ON o.ID = e.OBJECTIDENTITY_FK
WHERE o.OBJECT_CLASS = "ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisImpl" AND
      o.PARENT_OBJECT_FK IS NOT NULL;
DELETE e FROM ACLENTRY e INNER JOIN ACLOBJECTIDENTITY o ON o.ID = e.OBJECTIDENTITY_FK
WHERE o.OBJECT_CLASS = "ubic.gemma.model.analysis.expression.coexpression.ProbeCoexpressionAnalysisImpl" AND
      o.PARENT_OBJECT_FK IS NOT NULL;

UPDATE ACLOBJECTIDENTITY
SET ENTRIES_INHERITING = 1
WHERE OBJECT_CLASS = "ubic.gemma.model.expression.experiment.FactorValueImpl";
UPDATE ACLOBJECTIDENTITY
SET ENTRIES_INHERITING = 1
WHERE OBJECT_CLASS = "ubic.gemma.model.expression.experiment.ExperimentalDesignImpl";
UPDATE ACLOBJECTIDENTITY
SET ENTRIES_INHERITING = 1
WHERE OBJECT_CLASS = "ubic.gemma.model.analysis.expression.pca.PrincipalComponentAnalysisImpl";
UPDATE ACLOBJECTIDENTITY
SET ENTRIES_INHERITING = 1
WHERE OBJECT_CLASS = "ubic.gemma.model.analysis.expression.coexpression.ProbeCoexpressionAnalysisImpl";
UPDATE ACLOBJECTIDENTITY
SET ENTRIES_INHERITING = 1
WHERE OBJECT_CLASS = "ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisImpl";
UPDATE ACLOBJECTIDENTITY
SET ENTRIES_INHERITING = 1
WHERE OBJECT_CLASS = "ubic.gemma.model.expression.experiment.ExperimentalFactorImpl";

-- later ...
DROP TABLE acl_permission;
DROP TABLE acl_object_identity;
DROP TABLE acl_entry;
DROP TABLE acl_class;
DROP TABLE acl_sid;

-- remove unused data from CONTACT table.
ALTER TABLE CONTACT
  DROP COLUMN U_R_L;
ALTER TABLE CONTACT
  DROP COLUMN ADDRESS;
ALTER TABLE CONTACT
  DROP COLUMN PHONE;
ALTER TABLE CONTACT
  DROP COLUMN TOLL_FREE_PHONE;
ALTER TABLE CONTACT
  DROP COLUMN FAX;
ALTER TABLE CONTACT
  DROP FOREIGN KEY ORGANIZATION_PARENT_FKC;
ALTER TABLE CONTACT
  DROP COLUMN PARENT_FK;



