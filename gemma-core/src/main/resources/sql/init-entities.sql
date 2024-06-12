-- Add some indices that are not included in the generated gemma-ddl.sql.
-- Some of these are very important for performance

alter table ACLSID
    add index ACLSID_CLASS (class);
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

-- should remove the FIRST_GENE_FK and SECOND_GENE_FK indices, but they get given 'random' names.
-- Drop the second_gene_fk constraint.
-- alter table HUMAN_GENE_COEXPRESSION drop foreign key FKF9E6557F21D58F19;
-- alter table MOUSE_GENE_COEXPRESSION drop foreign key FKFC61C4F721D58F19;
-- alter table RAT_GENE_COEXPRESSION drop foreign key FKDE59FC7721D58F19;
-- alter table OTHER_GENE_COEXPRESSION drop foreign key FK74B9A3E221D58F19;

alter table HUMAN_GENE_COEXPRESSION
    add index hfgsg (FIRST_GENE_FK, SECOND_GENE_FK);
alter table MOUSE_GENE_COEXPRESSION
    add index mfgsg (FIRST_GENE_FK, SECOND_GENE_FK);
alter table RAT_GENE_COEXPRESSION
    add index rfgsg (FIRST_GENE_FK, SECOND_GENE_FK);
alter table OTHER_GENE_COEXPRESSION
    add index ofgsg (FIRST_GENE_FK, SECOND_GENE_FK);

-- same for these, should drop the key for EXPERIMENT_FK, manually
alter table HUMAN_EXPERIMENT_COEXPRESSION
    add index ECL1EFK (EXPERIMENT_FK, GENE1_FK, GENE2_FK);
alter table HUMAN_EXPERIMENT_COEXPRESSION
    add constraint ECL1EFK foreign key (EXPERIMENT_FK) references INVESTIGATION (ID);
alter table MOUSE_EXPERIMENT_COEXPRESSION
    add index ECL2EFK (EXPERIMENT_FK, GENE1_FK, GENE2_FK);
alter table MOUSE_EXPERIMENT_COEXPRESSION
    add constraint ECL2EFK foreign key (EXPERIMENT_FK) references INVESTIGATION (ID);
alter table RAT_EXPERIMENT_COEXPRESSION
    add index ECL3EFK (EXPERIMENT_FK, GENE1_FK, GENE2_FK);
alter table RAT_EXPERIMENT_COEXPRESSION
    add constraint ECL3EFK foreign key (EXPERIMENT_FK) references INVESTIGATION (ID);
alter table OTHER_EXPERIMENT_COEXPRESSION
    add index ECL4EFK (EXPERIMENT_FK, GENE1_FK, GENE2_FK);
alter table OTHER_EXPERIMENT_COEXPRESSION
    add constraint ECL4EFK foreign key (EXPERIMENT_FK) references INVESTIGATION (ID);

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
create index EE2C_VALUE on EXPRESSION_EXPERIMENT2CHARACTERISTIC (`VALUE`);
create index EE2C_CATEGORY on EXPRESSION_EXPERIMENT2CHARACTERISTIC (CATEGORY);
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
