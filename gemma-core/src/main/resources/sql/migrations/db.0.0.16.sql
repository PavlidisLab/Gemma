alter table BIO_ASSAY add column METADATA text null;
alter table INVESTIGATION add column METADATA text null;
alter table INVESTIGATION add column BATCH_EFFECT VARCHAR(255) null;
alter table INVESTIGATION add column BATCH_CONFOUND VARCHAR(255) null;
