alter table bio_sequence add index name (name);
alter table expression_experiment add index name (name);
alter table database_entry add index acc_ex (accession, external_database_fk);
