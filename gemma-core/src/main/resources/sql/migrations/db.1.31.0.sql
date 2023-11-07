alter table INVESTIGATION
    modify column BATCH_EFFECT VARCHAR(255),
    add column BATCH_EFFECT_STATISTICS TEXT after BATCH_EFFECT;
update INVESTIGATION
set BATCH_EFFECT_STATISTICS = BATCH_EFFECT,
    BATCH_EFFECT            = 'BATCH_EFFECT_FAILURE'
where BATCH_EFFECT like 'This data set may have a batch artifact%';
alter table CHROMOSOME_FEATURE
    add column DUMMY TINYINT not null default false;
alter table BIO_ASSAY
    modify column SAMPLE_USED_FK BIGINT not null;
-- allow distinguishing characteristics from statements
alter table CHARACTERISTIC
    add column MIGRATED_TO_STATEMENT TINYINT not null default false after ORIGINAL_VALUE,
    add column PREDICATE             VARCHAR(255) after MIGRATED_TO_STATEMENT,
    add column PREDICATE_URI         VARCHAR(255) after PREDICATE,
    add column OBJECT                VARCHAR(255) after PREDICATE_URI,
    add column OBJECT_URI            VARCHAR(255) after OBJECT,
    add column SECOND_PREDICATE      VARCHAR(255) after OBJECT_URI,
    add column SECOND_PREDICATE_URI  VARCHAR(255) after SECOND_PREDICATE,
    add column SECOND_OBJECT         VARCHAR(255) after SECOND_PREDICATE_URI,
    add column SECOND_OBJECT_URI     VARCHAR(255) after SECOND_OBJECT;
-- add indices we need for querying triplets
alter table CHARACTERISTIC
    add index CHARACTERISTIC_PREDICATE_URI_PREDICATE (PREDICATE_URI(100), PREDICATE),
    add index CHARACTERISTIC_OBJECT_URI_OBJECT (OBJECT_URI(100), OBJECT),
    add index CHARACTERISTIC_SECOND_PREDICATE_URI_SECOND_PREDICATE (SECOND_PREDICATE_URI(100), SECOND_PREDICATE),
    add index CHARACTERISTIC_SECOND_OBJECT_URI_SECOND_OBJECT (SECOND_OBJECT_URI(100), SECOND_OBJECT);
alter table FACTOR_VALUE
    add column NEEDS_ATTENTION TINYINT not null default false;