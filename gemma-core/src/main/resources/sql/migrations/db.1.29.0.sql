-- make external database auditable (initially nullable, but we'll make it non-nullable afterward
alter table EXTERNAL_DATABASE
    add column AUDIT_TRAIL_FK BIGINT UNIQUE references AUDIT_TRAIL (ID);

-- insert one audit trail for each existing external database
start transaction;
insert into AUDIT_TRAIL
select NULL
from EXTERNAL_DATABASE
where AUDIT_TRAIL_FK is NULL;
-- update FKs relative to the last insert ID (which is actually the first insert ID in the above query)
-- offset has to start at -1 because it will be zero after the first increment
SET @FIRST_AUDIT_TRAIL_ID = last_insert_id();
SET @OFFSET = -1;
update EXTERNAL_DATABASE
set EXTERNAL_DATABASE.AUDIT_TRAIL_FK = @FIRST_AUDIT_TRAIL_ID + (@OFFSET := @OFFSET + 1)
where EXTERNAL_DATABASE.AUDIT_TRAIL_FK is NULL;
commit;

-- make audit trail non-null now that all EDs are auditable
alter table EXTERNAL_DATABASE
    modify column AUDIT_TRAIL_FK BIGINT NOT NULL;

-- make name unique and non-nullable
-- add columns for the Versioned interface
alter table EXTERNAL_DATABASE
    modify column name varchar(255) not null unique,
    add column RELEASE_VERSION VARCHAR(255),
    add column RELEASE_URL     VARCHAR(255),
    add column LAST_UPDATED    DATETIME;

-- make audit trail non-null in all models
alter table INVESTIGATION modify column AUDIT_TRAIL_FK BIGINT not null;
alter table EXPRESSION_EXPERIMENT_SET modify column AUDIT_TRAIL_FK BIGINT not null;
alter table PHENOTYPE_ASSOCIATION modify column AUDIT_TRAIL_FK BIGINT not null;
alter table USER_GROUP modify column AUDIT_TRAIL_FK BIGINT not null;
alter table ARRAY_DESIGN modify column AUDIT_TRAIL_FK BIGINT not null;
alter table GENE_SET modify column AUDIT_TRAIL_FK BIGINT not null;

-- allow external databases to have related databases
alter table EXTERNAL_DATABASE
    add column EXTERNAL_DATABASE_FK BIGINT after DATABASE_SUPPLIER_FK;
alter table EXTERNAL_DATABASE
    add constraint EXTERNAL_DATABASE_FKC foreign key (EXTERNAL_DATABASE_FK) references EXTERNAL_DATABASE (ID);