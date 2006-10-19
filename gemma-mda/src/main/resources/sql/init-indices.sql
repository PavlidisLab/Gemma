alter table BIO_SEQUENCE add index name (NAME);
alter table EXPRESSION_EXPERIMENT add index name (NAME);
alter table DATABASE_ENTRY add index acc_ex (ACCESSION, EXTERNAL_DATABASE_FK);
alter table CHROMOSOME_FEATURE add index symbol_tax (OFFICIAL_SYMBOL, TAXON_FK);
alter table CHROMOSOME_FEATURE add index ncbiid (NCBI_ID);
alter table GENE_ALIAS add index `alias` (`ALIAS`);
