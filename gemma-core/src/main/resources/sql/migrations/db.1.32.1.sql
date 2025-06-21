-- this is a follow-up migration to db.1.32.0.sql when the production database will be completely migrated and the 1.32.0 released
alter table BIO_ASSAY_DIMENSION
    drop column NAME,
    drop column DESCRIPTION;
alter table BIO_ASSAY_DIMENSION
    modify column IS_MERGED TINYINT not null;
alter table MEAN_VARIANCE_RELATION
    modify column MEANS mediumblob not null,
    modify column VARIANCES mediumblob not null;
