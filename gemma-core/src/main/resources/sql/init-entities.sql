-- Add some indices that are not included in the generated gemma-ddl.sql.
-- Some of these are very important for performance

-- ACLSID_CLASS index removed: the new acl_sid schema (Spring Security canonical)
-- has a `principal` BIT column instead of a `class` varchar discriminator. With
-- only two possible values, an index is not useful.
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

-- Phase 2 Step 3 retired the gene-gene coexpression subsystem; the {HUMAN,MOUSE,RAT,OTHER}_GENE_COEXPRESSION
-- and {HUMAN,MOUSE,RAT,OTHER}_EXPERIMENT_COEXPRESSION tables are no longer created by Hibernate's hbm2ddl, so
-- the ALTER TABLE index/constraint statements that used to live here have been removed.

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

-- this table is created in the hibernate schema
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

-- note: constraint names cannot exceed 64 characters, so we cannot use the usual naming convention
-- indices for URIs are in vendor-specific SQL files
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