--
-- V2 schema extras for MySQL: tables, columns, and indices that the Hibernate metadata model
-- does NOT emit but production + test code paths require. Mirrors the populator chain that the
-- pre-Flyway prod bootstrap applied via DatabaseSchemaPopulator("mysql"):
--   - sql/init-acls.sql           (the acl_class table + Spring Security audit columns on acl_entry)
--   - sql/init-entities.sql       (vendor-neutral indices + GENE2CS + EE2CHARACTERISTIC + EE2AD)
--   - sql/mysql/init-entities.sql (MySQL-specific composite indices on CHARACTERISTIC + EE2C with
--                                   (100)-prefix URI lengths to stay under MySQL's key-length cap)
-- The seed-data INSERTs from init-acls.sql / init-data-slim.sql live in V3.
--

-- ============================================================
-- From sql/init-acls.sql (schema portion only; INSERTs in V3).
-- ============================================================

-- Hibernate maps acl_sid / acl_object_identity / acl_entry from gsec's HBM files, but acl_class
-- is NOT Hibernate-mapped. JdbcMutableAclService needs it for createAcl()'s class-lookup INSERTs.
CREATE TABLE IF NOT EXISTS acl_class (
  id     BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
  class  VARCHAR(255) NOT NULL UNIQUE
);

-- Spring Security's canonical acl_entry has audit_success / audit_failure columns that gsec's
-- AclEntry domain type doesn't model. Hibernate's hbm2ddl=create therefore doesn't emit them.
-- Add them here so JdbcMutableAclService's INSERTs (which include these columns) work.
ALTER TABLE acl_entry ADD COLUMN audit_success BIT NOT NULL DEFAULT 0;
ALTER TABLE acl_entry ADD COLUMN audit_failure BIT NOT NULL DEFAULT 0;

-- ============================================================
-- From sql/init-entities.sql (vendor-neutral schema extras).
-- ============================================================

-- Indices that are not included in the generated DDL but are important for performance / test-equivalence
-- with prod. ACLSID_CLASS index intentionally omitted (new acl_sid has principal BIT, no class column).
alter table INVESTIGATION
    add index INVESTIGATION_CLASS (class);
alter table DATABASE_ENTRY
    add index acc_ex (ACCESSION, EXTERNAL_DATABASE_FK);
alter table CHROMOSOME_FEATURE
    add index CHROMOSOME_FEATURE_CLASS (class);
alter table CHROMOSOME_FEATURE
    add index symbol_tax (OFFICIAL_SYMBOL, TAXON_FK);
alter table AUDIT_EVENT_TYPE
    add index AUDIT_EVENT_TYPE_CLASS (class);
alter table ANALYSIS
    add index ANALYSIS_CLASS (class);
alter table ANALYSIS_RESULT_SET
    add index ANALYSIS_RESULT_CLASS (class);

alter table CHARACTERISTIC
    add index CHARACTERISTIC_CLASS (class);

alter table PROCESSED_EXPRESSION_DATA_VECTOR
    add index experimentProcessedVectorProbes (EXPRESSION_EXPERIMENT_FK, DESIGN_ELEMENT_FK);

alter table DIFFERENTIAL_EXPRESSION_ANALYSIS_RESULT
    add index resultSetProbes (RESULT_SET_FK, PROBE_FK);
alter table DIFFERENTIAL_EXPRESSION_ANALYSIS_RESULT
    add index probeResultSets (PROBE_FK, RESULT_SET_FK);

alter table CONTACT
    add index fullname (NAME, LAST_NAME);

-- denormalized table joining genes and compositeSequences; maintained by TableMaintenanceUtil.
create table GENE2CS
(
    GENE BIGINT not null,
    CS   BIGINT not null,
    AD   BIGINT not null,
    primary key (AD, CS, GENE)
);
alter table GENE2CS
    add constraint GENE2CS_ARRAY_DESIGN_FKC foreign key (AD) references ARRAY_DESIGN (ID) on update cascade on delete cascade;
alter table GENE2CS
    add constraint GENE2CS_CS_FKC foreign key (CS) references COMPOSITE_SEQUENCE (ID) on update cascade on delete cascade;
alter table GENE2CS
    add constraint GENE2CS_GENE_FKC foreign key (GENE) references CHROMOSOME_FEATURE (ID) on update cascade on delete cascade;

-- EXPRESSION_EXPERIMENT2CHARACTERISTIC: Hibernate emits a stub (ID + EXPRESSION_EXPERIMENT_FK only);
-- the denormalized population layout lives here. Drop the stub and replace.
drop table EXPRESSION_EXPERIMENT2CHARACTERISTIC;
create table EXPRESSION_EXPERIMENT2CHARACTERISTIC
(
    ID                                    bigint,
    NAME                                  varchar(255),
    DESCRIPTION                           text,
    CATEGORY                              varchar(255),
    CATEGORY_URI                          varchar(255),
    `VALUE`                               varchar(255),
    VALUE_URI                             varchar(255),
    PREDICATE                             varchar(255),
    PREDICATE_URI                         varchar(255),
    OBJECT                                varchar(255),
    OBJECT_URI                            varchar(255),
    SECOND_PREDICATE                      varchar(255),
    SECOND_PREDICATE_URI                  varchar(255),
    SECOND_OBJECT                         varchar(255),
    SECOND_OBJECT_URI                     varchar(255),
    ORIGINAL_VALUE                        varchar(255),
    EVIDENCE_CODE                         varchar(255),
    EXPRESSION_EXPERIMENT_FK              bigint,
    ACL_IS_AUTHENTICATED_ANONYMOUSLY_MASK int not null default 0,
    LEVEL                                 varchar(255),
    primary key (ID, EXPRESSION_EXPERIMENT_FK)
);

alter table EXPRESSION_EXPERIMENT2CHARACTERISTIC
    add constraint EE2C_CHARACTERISTIC_FKC foreign key (ID) references CHARACTERISTIC (ID) on update cascade on delete cascade;
alter table EXPRESSION_EXPERIMENT2CHARACTERISTIC
    add constraint EE2C_EXPRESSION_EXPERIMENT_FKC foreign key (EXPRESSION_EXPERIMENT_FK) references INVESTIGATION (id) on update cascade on delete cascade;

-- note: constraint names cannot exceed 64 characters, so we cannot use the usual naming convention.
-- URI-bearing composite indices live in the h2/ section further down.
create index EE2C_CATEGORY on EXPRESSION_EXPERIMENT2CHARACTERISTIC (CATEGORY);
create index EE2C_VALUE on EXPRESSION_EXPERIMENT2CHARACTERISTIC (`VALUE`);
create index EE2C_PREDICATE on EXPRESSION_EXPERIMENT2CHARACTERISTIC (PREDICATE);
create index EE2C_OBJECT on EXPRESSION_EXPERIMENT2CHARACTERISTIC (OBJECT);
create index EE2C_SECOND_PREDICATE on EXPRESSION_EXPERIMENT2CHARACTERISTIC (SECOND_PREDICATE);
create index EE2C_SECOND_OBJECT on EXPRESSION_EXPERIMENT2CHARACTERISTIC (SECOND_OBJECT);
create index EE2C_LEVEL on EXPRESSION_EXPERIMENT2CHARACTERISTIC (LEVEL);

create table EXPRESSION_EXPERIMENT2ARRAY_DESIGN
(
    EXPRESSION_EXPERIMENT_FK              bigint  not null,
    ARRAY_DESIGN_FK                       bigint  not null,
    -- indicate if the platform is original (see BioAssay.originalPlatform)
    IS_ORIGINAL_PLATFORM                  tinyint not null,
    -- the permission mask of the EE for the anonymous SID
    ACL_IS_AUTHENTICATED_ANONYMOUSLY_MASK int     not null default 0,
    primary key (EXPRESSION_EXPERIMENT_FK, ARRAY_DESIGN_FK, IS_ORIGINAL_PLATFORM)
);

alter table EXPRESSION_EXPERIMENT2ARRAY_DESIGN
    add constraint EE2AD_EXPRESSION_EXPERIMENT_FKC foreign key (EXPRESSION_EXPERIMENT_FK) references INVESTIGATION (id) on update cascade on delete cascade;
alter table EXPRESSION_EXPERIMENT2ARRAY_DESIGN
    add constraint EE2AD_ARRAY_DESIGN_FKC foreign key (ARRAY_DESIGN_FK) references ARRAY_DESIGN (ID) on update cascade on delete cascade;

create unique index CELL_LEVEL_CHARACTERISTICS_NAME on CELL_LEVEL_CHARACTERISTICS (SINGLE_CELL_DIMENSION_FK, NAME);

-- ============================================================

-- ============================================================
-- From sql/mysql/init-entities.sql (MySQL-specific URI-prefix indices).
-- ============================================================

-- no URI exceeds 100 characters in practice, so we only index a prefix
alter table CHARACTERISTIC
    add index CHARACTERISTIC_VALUE_URI_VALUE (VALUE_URI(100), `VALUE`);
alter table CHARACTERISTIC
    add index CHARACTERISTIC_CATEGORY_URI_CATEGORY_VALUE_URI_VALUE (CATEGORY_URI(100), CATEGORY, VALUE_URI(100), `VALUE`);
alter table CHARACTERISTIC
    add index CHARACTERISTIC_PREDICATE_URI_PREDICATE (PREDICATE_URI(100), PREDICATE);
alter table CHARACTERISTIC
    add index CHARACTERISTIC_OBJECT_URI_OBJECT (OBJECT_URI(100), OBJECT);
alter table CHARACTERISTIC
    add index CHARACTERISTIC_SECOND_PREDICATE_URI_SECOND_PREDICATE (SECOND_PREDICATE_URI(100), SECOND_PREDICATE);
alter table CHARACTERISTIC
    add index CHARACTERISTIC_SECOND_OBJECT_URI_SECOND_OBJECT (SECOND_OBJECT_URI(100), SECOND_OBJECT);

create index EE2C_CATEGORY_URI_CATEGORY_VALUE_URI_VALUE on EXPRESSION_EXPERIMENT2CHARACTERISTIC (CATEGORY_URI(100), CATEGORY, VALUE_URI(100), `VALUE`);
create index EE2C_VALUE_URI_VALUE on EXPRESSION_EXPERIMENT2CHARACTERISTIC (VALUE_URI(100), `VALUE`);
create index EE2C_PREDICATE_URI_PREDICATE on EXPRESSION_EXPERIMENT2CHARACTERISTIC (PREDICATE_URI(100), PREDICATE);
create index EE2C_OBJECT_URI_OBJECT on EXPRESSION_EXPERIMENT2CHARACTERISTIC (OBJECT_URI(100), OBJECT);
create index EE2C_SECOND_PREDICATE_URI_SECOND_PREDICATE on EXPRESSION_EXPERIMENT2CHARACTERISTIC (SECOND_PREDICATE_URI(100), SECOND_PREDICATE);
create index EE2C_SECOND_OBJECT_URI_SECOND_OBJECT on EXPRESSION_EXPERIMENT2CHARACTERISTIC (SECOND_OBJECT_URI(100), SECOND_OBJECT);
