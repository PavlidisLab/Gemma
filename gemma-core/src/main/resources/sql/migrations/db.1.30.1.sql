alter table CONTRAST_RESULT
    modify column DIFFERENTIAL_EXPRESSION_ANALYSIS_RESULT_FK bigint not null;
alter table BIO_ASSAY
    modify column SEQUENCE_READ_COUNT bigint;