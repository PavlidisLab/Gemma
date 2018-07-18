-- make nullable
alter table TAXON MODIFY IS_SPECIES tinyint(4);

-- later:
alter table TAXON DROP COLUMN PARENT_TAXON_FK;
alter table TAXON DROP COLUMN IS_SPECIES;
alter table TAXON DROP COLUMN ABBREVIATION;
