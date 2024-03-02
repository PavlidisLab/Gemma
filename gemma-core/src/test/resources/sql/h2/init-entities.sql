insert into AUDIT_TRAIL (ID)
values (1),
       (2);

insert into USER_GROUP (ID, AUDIT_TRAIL_FK, NAME, DESCRIPTION)
values (1, 1, 'Administrators', NULL),
       (2, 2, 'Users', NULL);

insert into CONTACT (ID, class, NAME, DESCRIPTION, EMAIL, LAST_NAME, USER_NAME, PASSWORD, PASSWORD_HINT, ENABLED,
                     SIGNUP_TOKEN, SIGNUP_TOKEN_DATESTAMP)
values (1, 'User', 'admin', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);

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

create index EE2C_VALUE on EXPRESSION_EXPERIMENT2CHARACTERISTIC (`VALUE`);
create index EE2C_CATEGORY on EXPRESSION_EXPERIMENT2CHARACTERISTIC (CATEGORY);
create index EE2C_VALUE_URI_VALUE on EXPRESSION_EXPERIMENT2CHARACTERISTIC (VALUE_URI, `VALUE`);
create index EE2C_CATEGORY_URI_CATEGORY_VALUE_URI_VALUE on EXPRESSION_EXPERIMENT2CHARACTERISTIC (CATEGORY_URI, CATEGORY, VALUE_URI, `VALUE`);
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
