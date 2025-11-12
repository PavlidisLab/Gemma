alter table QUANTITATION_TYPE
    add column IS_AGGREGATED tinyint not null default false;

select *
from QUANTITATION_TYPE
         join RAW_EXPRESSION_DATA_VECTOR on QUANTITATION_TYPE.ID = RAW_EXPRESSION_DATA_VECTOR.QUANTITATION_TYPE_FK
where NAME like '%aggregated by%';