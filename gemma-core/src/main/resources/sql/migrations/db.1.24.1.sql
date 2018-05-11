-- data model revisions and cleanups. 4/2018

-- make retracted a field of BibliographicReference rather than a join
alter table BIBLIOGRAPHIC_REFERENCE
  add COLUMN RETRACTED TINYINT DEFAULT 0;
update BIBLIOGRAPHIC_REFERENCE b, PUBLICATION_TYPE p
set b.RETRACTED = 1
where p.BIBLIOGRAPHIC_REFERENCE_FK = b.ID and p.TYPE = "Retracted Publication";
drop table PUBLICATION_TYPE;

-- Remove unused audit events for ChromosomeFeature
start transaction;
set foreign_key_checks=0;
delete a, t from AUDIT_EVENT a inner join CHROMOSOME_FEATURE c ON c.AUDIT_TRAIL_FK = a.AUDIT_TRAIL_FK
  inner join AUDIT_TRAIL t ON t.ID = a.AUDIT_TRAIL_FK;

alter table CHROMOSOME_FEATURE
  DROP COLUMN AUDIT_TRAIL_FK;
set foreign_key_checks=1;
commit;

-- remove Investigators from Investigation
start transaction;
set foreign_key_checks=0;
delete c, i from CONTACT c INNER JOIN INVESTIGATORS i ON i.INVESTIGATORS_FK = c.ID;
drop table INVESTIGATORS;
set foreign_key_checks=1;
commit;

-- make more entities non-auditable
start transaction;
set foreign_key_checks=0;

delete a, t from AUDIT_EVENT a inner join BIO_MATERIAL c ON c.AUDIT_TRAIL_FK = a.AUDIT_TRAIL_FK
  inner join AUDIT_TRAIL t ON t.ID = a.AUDIT_TRAIL_FK;
delete a, t from AUDIT_EVENT a inner join PROTOCOL c ON c.AUDIT_TRAIL_FK = a.AUDIT_TRAIL_FK
  inner join AUDIT_TRAIL t ON t.ID = a.AUDIT_TRAIL_FK;
delete a, t from AUDIT_EVENT a inner join EXTERNAL_DATABASE c ON c.AUDIT_TRAIL_FK = a.AUDIT_TRAIL_FK
  inner join AUDIT_TRAIL t ON t.ID = a.AUDIT_TRAIL_FK;
delete a, t from AUDIT_EVENT a inner join BIBLIOGRAPHIC_REFERENCE c ON c.AUDIT_TRAIL_FK = a.AUDIT_TRAIL_FK
  inner join AUDIT_TRAIL t ON t.ID = a.AUDIT_TRAIL_FK;
delete a, t from AUDIT_EVENT a inner join CONTACT c ON c.AUDIT_TRAIL_FK = a.AUDIT_TRAIL_FK
  inner join AUDIT_TRAIL t ON t.ID = a.AUDIT_TRAIL_FK;
delete a, t from AUDIT_EVENT a inner join BIO_ASSAY c ON c.AUDIT_TRAIL_FK = a.AUDIT_TRAIL_FK
  inner join AUDIT_TRAIL t ON t.ID = a.AUDIT_TRAIL_FK;
delete a, t from AUDIT_EVENT a inner join TREATMENT c ON c.AUDIT_TRAIL_FK = a.AUDIT_TRAIL_FK
  inner join AUDIT_TRAIL t ON t.ID = a.AUDIT_TRAIL_FK;

set foreign_key_checks=1;
commit;

start transaction;
set foreign_key_checks=0;
delete a, t from AUDIT_EVENT a inner join CHARACTERISTIC c ON c.AUDIT_TRAIL_FK = a.AUDIT_TRAIL_FK
  inner join AUDIT_TRAIL t ON t.ID = a.AUDIT_TRAIL_FK;
delete a, t from AUDIT_EVENT a inner join ANALYSIS c ON c.AUDIT_TRAIL_FK = a.AUDIT_TRAIL_FK
  inner join AUDIT_TRAIL t ON t.ID = a.AUDIT_TRAIL_FK;
set foreign_key_checks=1;
commit;

start transaction;
set foreign_key_checks=0;
alter table BIO_MATERIAL DROP FOREIGN KEY FK198C0A9E39A05453;
alter table BIO_MATERIAL
  DROP COLUMN AUDIT_TRAIL_FK;
set foreign_key_checks=1;
commit;

alter table PROTOCOL
  DROP COLUMN AUDIT_TRAIL_FK;

alter table ANALYSIS DROP FOREIGN KEY AUDITABLE_AUDIT_TRAIL_FKC;
alter table ANALYSIS
  DROP COLUMN AUDIT_TRAIL_FK;


  alter table EXTERNAL_DATABASE
  DROP COLUMN AUDIT_TRAIL_FK;

alter table BIBLIOGRAPHIC_REFERENCE
  DROP COLUMN AUDIT_TRAIL_FK;

alter table CONTACT
  DROP COLUMN AUDIT_TRAIL_FK;
alter table BIO_ASSAY
  DROP COLUMN AUDIT_TRAIL_FK;
alter table TREATMENT
  DROP COLUMN AUDIT_TRAIL_FK;

alter table CHARACTERISTIC drop FOREIGN KEY CHARACTERISTIC_AUDIT_TRAIL_FKC;
alter table CHARACTERISTIC
  DROP COLUMN AUDIT_TRAIL_FK;
set foreign_key_checks=1;
commit;

-- remove more unneded tables

drop table CHARTEMP; -- tmp
drop table TMP_GENES_TO_REMOVE; -- tmp
drop table TMP_GENE_PRODUCTS_TO_REMOVE; -- tmp
drop table PARAMETERIZABLE; -- not in data model
alter table BIO_ASSAY drop foreign key BIO_ASSAY_RAW_DATA_FILE_FKC;
alter table BIO_ASSAY
  DROP COLUMN RAW_DATA_FILE_FK;
alter table INVESTIGATION DROP FOREIGN KEY FKF2B9BAE272C71E73;
alter table INVESTIGATION
  DROP COLUMN RAW_DATA_FILE_FK;

-- removed from data model
alter table BIBLIOGRAPHIC_REFERENCE drop foreign key BIBLIOGRAPHIC_REFERENCE_FULL_TEXT_P_D_F_FKC;
alter table BIBLIOGRAPHIC_REFERENCE
  DROP COLUMN FULL_TEXT_P_D_F_FK,
  DROP COLUMN FULL_TEXT_PDF_FK;
drop table SOURCE_FILES;
drop table LOCAL_FILE;

alter table TREATMENT
  drop FOREIGN KEY TREATMENT_ACTION_FKC;
delete c from CHARACTERISTIC c inner join TREATMENT t ON t.ACTION_FK = c.ID;

alter table TREATMENT DROP foreign key TREATMENT_ACTION_FKC;
alter table TREATMENT drop foreign key TREATMENT_ACTION_MEASUREMENT_FKC;
alter table TREATMENT
  DROP COLUMN ACTION_FK,
  DROP COLUMN ACTION_MEASUREMENT_FK;


-- Misc
alter table CHROMOSOME_FEATURE DROP FOREIGN KEY CHROMOSOME_FEATURE_CYTOGENIC_LOCATION_FKC;
alter table CHROMOSOME_FEATURE
  DROP COLUMN TYPE,
  DROP COLUMN CDS_PHYSICAL_LOCATION_FK,
  DROP COLUMN METHOD,
  DROP COLUMN CYTOGENIC_LOCATION_FK; -- removed from data model

alter table GENE2GO_ASSOCIATION drop column SOURCE_FK, drop column SOURCE_ANALYSIS_FK;

drop table CYTOGENETIC_LOCATION; -- not in data model
alter table PROTOCOL drop foreign key FKF3B07E98136C38E3, DROP COLUMN U_R_I, drop column TYPE_FK;

drop table PERFORMERS;
drop table PROTOCOL_APPLICATION; -- not in data model
