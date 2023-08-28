-- allow distinguishing characteristics from statements
alter table CHARACTERISTIC
    add column class                VARCHAR(255) after ID,
    add column PREDICATE            VARCHAR(255) after ORIGINAL_VALUE,
    add column PREDICATE_URI        VARCHAR(255) after PREDICATE,
    add column OBJECT_FK            BIGINT references CHARACTERISTIC (ID) after PREDICATE_URI,
    add column SECOND_PREDICATE     VARCHAR(255) after OBJECT_FK,
    add column SECOND_PREDICATE_URI VARCHAR(255) after SECOND_PREDICATE,
    add column SECOND_OBJECT_FK     BIGINT references CHARACTERISTIC (ID) after SECOND_PREDICATE_URI;
-- all characteristics in FV bags are statements
update CHARACTERISTIC
set class = 'Statement'
where FACTOR_VALUE_FK is not null;