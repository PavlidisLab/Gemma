alter table QUANTITATION_TYPE
    add column IS_AGGREGATED tinyint not null default false;
-- migrate all the raw QTs that are aggregated
update QUANTITATION_TYPE
set IS_AGGREGATED = true
where NAME like '%aggregated by%'
  and ID in (select distinct QUANTITATION_TYPE_FK
             from RAW_EXPRESSION_DATA_VECTOR);
alter table QUANTITATION_TYPE
    modify column NAME varchar(255) not null;
create table PROCESSED_EXPRESSION_DATA_VECTOR_NUMBER_OF_CELLS
(
    ID              BIGINT   not null,
    NUMBER_OF_CELLS LONGBLOB not null,
    primary key (ID)
);
alter table PROCESSED_EXPRESSION_DATA_VECTOR_NUMBER_OF_CELLS
    add index PROCESSED_EXPRESSION_DATA_VECTOR_FKC (ID),
    add constraint PROCESSED_EXPRESSION_DATA_VECTOR_FKC foreign key (ID) references PROCESSED_EXPRESSION_DATA_VECTOR (ID);
create table RAW_EXPRESSION_DATA_VECTOR_NUMBER_OF_CELLS
(
    ID              BIGINT   not null,
    NUMBER_OF_CELLS LONGBLOB not null,
    primary key (ID)
);
alter table RAW_EXPRESSION_DATA_VECTOR_NUMBER_OF_CELLS
    add index RAW_EXPRESSION_DATA_VECTOR_FKC (ID),
    add constraint RAW_EXPRESSION_DATA_VECTOR_FKC foreign key (ID) references RAW_EXPRESSION_DATA_VECTOR (ID);