alter table QUANTITATION_TYPE
    add column IS_AGGREGATED tinyint not null default false;
-- migrate all the raw QTs that are aggregated
update QUANTITATION_TYPE
set IS_AGGREGATED = true
where NAME like '%aggregated by%'
  and ID in (select distinct QUANTITATION_TYPE_FK
             from RAW_EXPRESSION_DATA_VECTOR);