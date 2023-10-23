alter table INVESTIGATION
    modify column BATCH_EFFECT VARCHAR(255),
    add column BATCH_EFFECT_STATISTICS TEXT after BATCH_EFFECT;
update INVESTIGATION
set BATCH_EFFECT_STATISTICS = BATCH_EFFECT,
    BATCH_EFFECT            = 'BATCH_EFFECT_FAILURE'
where BATCH_EFFECT like 'This data set may have a batch artifact%';
alter table CHROMOSOME_FEATURE
    add column DUMMY TINYINT not null default false;