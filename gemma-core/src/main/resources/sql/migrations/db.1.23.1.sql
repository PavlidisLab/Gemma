## start naming files to reference the release version and git tag at which point the code will require the changes in this file

update CHROMOSOME_FEATURE set class = "GeneProduct" where class = "GeneProductImpl";
alter table TAXON drop COLUMN `UNIGENE_PREFIX`, drop COLUMN `SWISS_PROT_SUFFIX`;