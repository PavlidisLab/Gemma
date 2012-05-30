-- Used by test: GeoConverterTest.
-- $Id$
insert ignore into TAXON (SCIENTIFIC_NAME,COMMON_NAME,ABBREVIATION,NCBI_ID,IS_SPECIES,PARENT_TAXON_FK,IS_GENES_USABLE) values ("Salmo salar","atlantic salmon","ssal","8030",1,null,0);
insert ignore into TAXON (SCIENTIFIC_NAME,COMMON_NAME,ABBREVIATION,NCBI_ID,IS_SPECIES,PARENT_TAXON_FK,IS_GENES_USABLE) values ("Oncorhynchus mykiss","rainbow trout","omyk","8022",1,null,0);
insert ignore into TAXON (SCIENTIFIC_NAME,COMMON_NAME,ABBREVIATION,NCBI_ID,IS_SPECIES,PARENT_TAXON_FK,IS_GENES_USABLE) values ("Coregonus clupeaformis","whitefish","cclu","59861",1,null,0);
insert ignore into TAXON (SCIENTIFIC_NAME,COMMON_NAME,NCBI_ID,IS_SPECIES,PARENT_TAXON_FK,IS_GENES_USABLE) values ("Oncorhynchus kisutch","coho salmon","8019",1,null,0);
insert ignore into TAXON (SCIENTIFIC_NAME,COMMON_NAME,NCBI_ID,IS_SPECIES,PARENT_TAXON_FK,IS_GENES_USABLE) values ("Oncorhynchus gorbuscha","pink salmon","8017",1,null,0);
insert ignore into TAXON (SCIENTIFIC_NAME,COMMON_NAME,NCBI_ID,IS_SPECIES,PARENT_TAXON_FK,IS_GENES_USABLE) values ("Oncorhynchus nerka","sockeye salmon","8023",1,null,0);
insert ignore into TAXON (SCIENTIFIC_NAME,COMMON_NAME,ABBREVIATION,NCBI_ID,IS_SPECIES,PARENT_TAXON_FK,IS_GENES_USABLE) values ("Oncorhynchus tshawytscha","chinook salmon","otsh","74940",1,null,0);
insert ignore into TAXON (SCIENTIFIC_NAME,COMMON_NAME,NCBI_ID,IS_SPECIES,PARENT_TAXON_FK,IS_GENES_USABLE) values ("Oncorhynchus keta","chum salmon","8018",1,null,0);
insert ignore into TAXON (SCIENTIFIC_NAME,COMMON_NAME,ABBREVIATION,NCBI_ID,IS_SPECIES,PARENT_TAXON_FK,IS_GENES_USABLE) values ("Osmerus mordax","rainbow smelt","omor","8014",1,null,0);
insert ignore into TAXON (SCIENTIFIC_NAME,COMMON_NAME,NCBI_ID,IS_SPECIES,IS_GENES_USABLE) values ("Salmonidae","salmonid","8015",0,1);
-- this gets loaded previously without this information during tests.
update TAXON set ABBREVIATION="ssal", COMMON_NAME="atlantic salmon", NCBI_ID="8030", IS_GENES_USABLE=0, IS_SPECIES=1 WHERE SCIENTIFIC_NAME="Salmo salar";
update TAXON set ABBREVIATION="omyk", COMMON_NAME="rainbow trout", NCBI_ID="8022", IS_GENES_USABLE=0, IS_SPECIES=1 WHERE SCIENTIFIC_NAME="Oncorhynchus mykiss";
update TAXON set ABBREVIATION="cclu", COMMON_NAME="whitefish", NCBI_ID="59861", IS_GENES_USABLE=0, IS_SPECIES=1 WHERE SCIENTIFIC_NAME="Coregonus clupeaformis";
update TAXON set ABBREVIATION="omor", COMMON_NAME="rainbow smelt", NCBI_ID="8014", IS_GENES_USABLE=0, IS_SPECIES=1 WHERE SCIENTIFIC_NAME="Osmerus mordax";
