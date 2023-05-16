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
drop table if exists GENE2CS;
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

drop table if exists EXPRESSION_EXPERIMENT2CHARACTERISTIC;
create table EXPRESSION_EXPERIMENT2CHARACTERISTIC
(
    ID                       bigint,
    NAME                     varchar(255),
    DESCRIPTION              text,
    CATEGORY                 varchar(255),
    CATEGORY_URI             varchar(255),
    `VALUE`                  varchar(255),
    VALUE_URI                varchar(255),
    ORIGINAL_VALUE           varchar(255),
    EVIDENCE_CODE            varchar(255),
    EXPRESSION_EXPERIMENT_FK bigint,
    LEVEL                    varchar(255),
    primary key (ID, EXPRESSION_EXPERIMENT_FK, LEVEL)
);

create index EE2C_VALUE on EXPRESSION_EXPERIMENT2CHARACTERISTIC (`VALUE`);
create index EE2C_CATEGORY on EXPRESSION_EXPERIMENT2CHARACTERISTIC (CATEGORY);
create index EE2C_VALUE_URI_VALUE on EXPRESSION_EXPERIMENT2CHARACTERISTIC (VALUE_URI, `VALUE`);
create index EE2C_CATEGORY_URI_CATEGORY_VALUE_URI_VALUE on EXPRESSION_EXPERIMENT2CHARACTERISTIC (CATEGORY_URI, CATEGORY, VALUE_URI, `VALUE`);
create index EE2C_LEVEL on EXPRESSION_EXPERIMENT2CHARACTERISTIC (LEVEL);