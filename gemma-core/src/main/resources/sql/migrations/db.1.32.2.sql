alter table EXPRESSION_EXPERIMENT_SET add column ACCESSION_FK BIGINT;
alter table EXPRESSION_EXPERIMENT_SET add constraint ACCESSION_FK unique (ACCESSION_FK);
alter table EXPRESSION_EXPERIMENT_SET add index EXPRESSION_EXPERIMENT_SET_ACCESSION_FKC (ACCESSION_FK), add constraint EXPRESSION_EXPERIMENT_SET_ACCESSION_FKC foreign key (ACCESSION_FK) references DATABASE_ENTRY (ID);

alter table CELL_LEVEL_CHARACTERISTICS
    add column NAME        varchar(255) after ID,
    add column DESCRIPTION text after NAME;
-- do not allow more than one CLC with the same name for a given SCD, unless the name is null
create unique index CELL_LEVEL_CHARACTERISTICS_NAME on CELL_LEVEL_CHARACTERISTICS (SINGLE_CELL_DIMENSION_FK, NAME);

update BIO_ASSAY set IS_OUTLIER = false where IS_OUTLIER is null;
alter table BIO_ASSAY
    modify column IS_OUTLIER TINYINT not null default false;