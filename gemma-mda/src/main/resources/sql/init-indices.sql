alter table BIO_SEQUENCE add index name (NAME);
alter table INVESTIGATION add index name (NAME);
alter table DATABASE_ENTRY add index acc_ex (ACCESSION, EXTERNAL_DATABASE_FK);
alter table DATABASE_ENTRY add index class (class);
alter table CHROMOSOME_FEATURE add index symbol_tax (OFFICIAL_SYMBOL, TAXON_FK);
alter table CHROMOSOME_FEATURE add index ncbiid (NCBI_ID);
alter table CHROMOSOME_FEATURE add index name (NAME);
alter table GENE_ALIAS add index `alias` (`ALIAS`);
alter table DESIGN_ELEMENT add index name (NAME);
