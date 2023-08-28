-- allow distinguishing characteristics from statements
alter table CHARACTERISTIC
    add column class                VARCHAR(255) after ID,
    add column PREDICATE            VARCHAR(255) after ORIGINAL_VALUE,
    add column PREDICATE_URI        VARCHAR(255) after PREDICATE,
    add column OBJECT_FK            BIGINT after PREDICATE_URI,
    add column SECOND_PREDICATE     VARCHAR(255) after OBJECT_FK,
    add column SECOND_PREDICATE_URI VARCHAR(255) after SECOND_PREDICATE,
    add column SECOND_OBJECT_FK     BIGINT after SECOND_PREDICATE_URI;
alter table CHARACTERISTIC
    add constraint CHARACTERISTIC_OBJECT_FKC foreign key (OBJECT_FK) references CHARACTERISTIC (ID),
    add constraint CHARACTERISTIC_SECOND_OBJECT_FKC foreign key (SECOND_OBJECT_FK) references CHARACTERISTIC (ID)
-- all characteristics in FV bags are statements
update CHARACTERISTIC
set class = 'Statement'
where FACTOR_VALUE_FK is not null;