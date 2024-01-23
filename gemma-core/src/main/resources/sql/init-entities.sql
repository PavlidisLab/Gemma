-- Initialize the database with some scraps of data. See also init-indices.sql and mysql-acegi-acl.sql.

-- hilo for generating IDs, under the MultipleHiLoPerTableGenerator method.
-- See http://blog.eyallupu.com/2011/01/hibernatejpa-identity-generators.html
drop table if exists hibernate_sequences;
create table hibernate_sequences (
	sequence_name VARCHAR(255) not null,
	sequence_next_hi_value BIGINT not null
);

delete from CONTACT;
delete from TAXON;
delete from EXTERNAL_DATABASE;
delete from AUDIT_TRAIL;

-- alter CHROMOSOME_FEATURE for case insensitive search
ALTER TABLE CHROMOSOME_FEATURE MODIFY OFFICIAL_SYMBOL varchar(255) default NULL;
ALTER TABLE CHROMOSOME_FEATURE MODIFY NAME varchar(255) default NULL;
ALTER TABLE CHROMOSOME_FEATURE MODIFY NCBI_GI varchar(255) default NULL;
ALTER TABLE CHROMOSOME_FEATURE MODIFY NCBI_GENE_ID  int(11) UNIQUE;
-- alter GENE_ALIAS for case insensitive search
ALTER TABLE GENE_ALIAS MODIFY ALIAS varchar(255) default NULL;


-- wider columns.
alter table BIO_SEQUENCE modify SEQUENCE LONGTEXT;
alter table JOB_INFO modify MESSAGES LONGTEXT;


-- all of these are used.
insert into AUDIT_TRAIL VALUES (1);
insert into AUDIT_TRAIL VALUES (2);
insert into AUDIT_TRAIL VALUES (3);


set @n:=now();

-- username=gemmaAgent: id = 2, password = 'XXXXXXXX', audit trail #2, using salt={username}
insert into CONTACT (ID, CLASS, NAME, LAST_NAME, USER_NAME, PASSWORD, ENABLED, EMAIL, PASSWORD_HINT) values (2, "User", "gemmaAgent",  "", "gemmaAgent", "2db458c67b4b52bba0184611c302c9c174ce8de4", 1, "pavlab-support@msl.ubc.ca", "hint");

-- username=administrator: id = 1, password = 'administrator', audit trail #1 using salt=username ('administrator')
insert into CONTACT (ID, CLASS, NAME, LAST_NAME, USER_NAME, PASSWORD, ENABLED, EMAIL, PASSWORD_HINT) values (1, "User", "administrator",  "", "administrator", "b7338dcc17d6b6c199a75540aab6d0506567b980", 1, "pavlab-support@msl.ubc.ca", "hint");

-- initialize the audit trails
insert into AUDIT_EVENT VALUES (1, @n, 'C', 'From init script', '', 1, NULL, 1);
insert into AUDIT_EVENT VALUES (2, @n, 'C', 'From init script', '', 1, NULL, 2);
insert into AUDIT_EVENT VALUES (3, @n, 'C', 'From init script', '', 1, NULL, 3);


-- Note that 'Administrators' is a constant set in AuthorityConstants. The names of these groups are defined in UserGroupDao.
insert into USER_GROUP (ID, NAME, DESCRIPTION, AUDIT_TRAIL_FK) VALUES (1, "Administrators", "Users with administrative rights", 1);
insert into USER_GROUP (ID, NAME, DESCRIPTION, AUDIT_TRAIL_FK) VALUES (2, "Users", "Default group for all authenticated users", 2);
insert into USER_GROUP (ID, NAME, DESCRIPTION, AUDIT_TRAIL_FK) VALUES (3, "Agents", "For 'autonomous' agents that run within the server context, such as scheduled tasks.", 3);
insert into GROUP_AUTHORITY (ID, AUTHORITY, GROUP_FK) VALUES (1, "ADMIN", 1);
insert into GROUP_AUTHORITY (ID, AUTHORITY, GROUP_FK) VALUES (2, "USER", 2);
insert into GROUP_AUTHORITY (ID, AUTHORITY, GROUP_FK) VALUES (3, "AGENT", 3);

-- make admin in the admin group
insert into GROUP_MEMBERS (USER_GROUPS_FK, GROUP_MEMBERS_FK) VALUES (1, 1);

-- add admin to the user group (note that there is no need for a corresponding ACL entry)
insert into GROUP_MEMBERS (USER_GROUPS_FK, GROUP_MEMBERS_FK) VALUES (2, 1);

-- add agent to the agent group
insert into GROUP_MEMBERS (USER_GROUPS_FK, GROUP_MEMBERS_FK) VALUES (3, 2);

-- taxa
insert into TAXON (SCIENTIFIC_NAME,COMMON_NAME,NCBI_ID,IS_GENES_USABLE) values ("Homo sapiens","human","9606",1);
insert into TAXON (SCIENTIFIC_NAME,COMMON_NAME,NCBI_ID,IS_GENES_USABLE) values ("Mus musculus","mouse","10090",1);
insert into TAXON (SCIENTIFIC_NAME,COMMON_NAME,NCBI_ID,IS_GENES_USABLE) values ("Rattus norvegicus","rat","10116",1);
insert into TAXON (SCIENTIFIC_NAME,COMMON_NAME,NCBI_ID,IS_GENES_USABLE,SECONDARY_NCBI_ID) values ("Saccharomyces cerevisiae","yeast","4932",1,559292);
insert into TAXON (SCIENTIFIC_NAME,COMMON_NAME,NCBI_ID,IS_GENES_USABLE) values ("Danio rerio","zebrafish","7955",1);
insert into TAXON (SCIENTIFIC_NAME,COMMON_NAME,NCBI_ID,IS_GENES_USABLE) values ("Drosophila melanogaster","fly","7227",1);
insert into TAXON (SCIENTIFIC_NAME,COMMON_NAME,NCBI_ID,IS_GENES_USABLE) values ("Caenorhabditis elegans","worm","6239",1);

-- external databases

-- we need a procedure since we have to create an audit trail
-- silly, but this needs to be in a single line because sql-maven-plugin does not deal well with statements containing multiple semi-colons
create procedure add_external_database(in name varchar(255), in description text, in web_uri varchar(255), in ftp_uri varchar(255), in type varchar(255)) begin insert into AUDIT_TRAIL (ID) values (null); insert into EXTERNAL_DATABASE (NAME, DESCRIPTION, WEB_URI, FTP_URI, TYPE, AUDIT_TRAIL_FK) values (name, description, web_uri, ftp_uri, type, last_insert_id()); end;

-- insert new db we need to track various things
call add_external_database ('PubMed', 'PubMed database from NCBI', 'https://www.ncbi.nlm.nih.gov/entrez/query.fcgi?db=PubMed', 'ftp://ftp.ncbi.nlm.nih.gov/pubmed/', 'LITERATURE');
-- call add_external_database('GO', 'Gene Ontology database', 'https://www.godatabase.org/dev/database/', 'https://archive.godatabase.org', 'ONTOLOGY');
call add_external_database('GEO', 'Gene Expression Omnibus', 'https://www.ncbi.nlm.nih.gov/geo/', 'ftp://ftp.ncbi.nih.gov/pub/geo/DATA', 'EXPRESSION');
call add_external_database('ArrayExpress', 'EBI ArrayExpress', 'https://www.ebi.ac.uk/arrayexpress/', 'ftp://ftp.ebi.ac.uk/pub/databases/microarray/data/', 'EXPRESSION');
call add_external_database('Genbank', 'NCBI Genbank', 'https://www.ncbi.nlm.nih.gov/Genbank/index.html', 'ftp://ftp.ncbi.nih.gov/genbank/', 'SEQUENCE');
call add_external_database('Entrez Gene', 'NCBI Gene database', 'https://www.ncbi.nlm.nih.gov/entrez/query.fcgi?db=gene', 'ftp://ftp.ncbi.nih.gov/gene/', 'SEQUENCE');
call add_external_database('Ensembl', 'EMBL - EBI/Sanger Institute genome annotations', 'https://www.ensembl.org/', 'ftp://ftp.ensembl.org/pub/', 'GENOME');
call add_external_database('OBO_REL', 'Open Biomedical Ontologies Relationships', 'https://www.obofoundry.org/ro/', NULL, 'ONTOLOGY');
call add_external_database('STRING', 'STRING - Known and Predicted Protein-Protein Interactions', 'https://string-db.org/version_8_2/newstring_cgi/show_network_section.pl?identifiers=', NULL, 'PROTEIN');
call add_external_database('hg18', NULL, '', NULL, 'SEQUENCE');
call add_external_database('hg19', NULL, '', NULL, 'SEQUENCE');
call add_external_database('hg38', NULL, '', NULL, 'SEQUENCE');
call add_external_database('mm8', NULL, '', NULL, 'SEQUENCE');
call add_external_database('mm9', NULL, '', NULL, 'SEQUENCE');
call add_external_database('mm10', NULL, '', NULL, 'SEQUENCE');
call add_external_database('mm39', NULL, '', NULL, 'SEQUENCE');
call add_external_database('rn4', NULL, '', NULL, 'SEQUENCE');
call add_external_database('rn6', NULL, '', NULL, 'SEQUENCE');
call add_external_database('rn7', NULL, '', NULL, 'SEQUENCE');
call add_external_database('hg18 annotations', NULL, 'https://hgdownload.cse.ucsc.edu/goldenpath/hg18/database/', NULL, 'OTHER');
call add_external_database('hg19 annotations', NULL, 'https://hgdownload.cse.ucsc.edu/goldenpath/hg19/database/', NULL, 'OTHER');
call add_external_database('hg38 annotations', NULL, 'https://hgdownload.cse.ucsc.edu/goldenpath/hg38/database/', NULL, 'OTHER');
call add_external_database('mm8 annotations', NULL, 'https://hgdownload.cse.ucsc.edu/goldenpath/mm8/database/', NULL, 'OTHER');
call add_external_database('mm9 annotations', NULL, 'https://hgdownload.cse.ucsc.edu/goldenpath/mm9/database/', NULL, 'OTHER');
call add_external_database('mm10 annotations', NULL, 'https://hgdownload.cse.ucsc.edu/goldenpath/mm10/database/', NULL, 'OTHER');
call add_external_database('mm39 annotations', NULL, 'https://hgdownload.cse.ucsc.edu/goldenpath/mm39/database/', NULL, 'OTHER');
call add_external_database('rn4 annotations', NULL, 'https://hgdownload.cse.ucsc.edu/goldenpath/rn4/database/', NULL, 'OTHER');
call add_external_database('rn6 annotations', NULL, 'https://hgdownload.cse.ucsc.edu/goldenpath/rn6/database/', NULL, 'OTHER');
call add_external_database('rn7 annotations', NULL, 'https://hgdownload.cse.ucsc.edu/goldenpath/rn7/database/', NULL, 'OTHER');
call add_external_database('hg38 RNA-Seq annotations', NULL, NULL, NULL, 'OTHER');
call add_external_database('mm10 RNA-Seq annotations', NULL, NULL, NULL, 'OTHER');
call add_external_database('mm39 RNA-Seq annotations', NULL, NULL, NULL, 'OTHER');
call add_external_database('rn7 RNA-Seq annotations', NULL, NULL, NULL, 'OTHER');
call add_external_database('gene', NULL, NULL, 'https://ftp.ncbi.nih.gov/gene/DATA/gene_info.gz', 'OTHER');
call add_external_database('go', NULL, NULL, 'https://ftp.ncbi.nih.gov/gene/DATA/gene2go.gz', 'ONTOLOGY');
call add_external_database('multifunctionality', NULL, NULL, NULL, 'OTHER');
call add_external_database('gene2cs', NULL, NULL, NULL, 'OTHER');

drop procedure add_external_database;

create procedure add_external_database_relation(in parent_name varchar(255), in child_name varchar(255)) begin select @parent_id := ID from EXTERNAL_DATABASE where name = parent_name; update EXTERNAL_DATABASE set EXTERNAL_DATABASE_FK = @parent_id where NAME = child_name; end;

call add_external_database_relation('hg38', 'hg38 annotations');
call add_external_database_relation('hg19', 'hg19 annotations');
call add_external_database_relation('hg18', 'hg18 annotations');
call add_external_database_relation('mm39', 'mm39 annotations');
call add_external_database_relation('mm10', 'mm10 annotations');
call add_external_database_relation('mm9', 'mm9 annotations');
call add_external_database_relation('mm8', 'mm8 annotations');
call add_external_database_relation('rn7', 'rn7 annotations');
call add_external_database_relation('rn6', 'rn4 annotations');
call add_external_database_relation('rn4', 'rn6 annotations');

call add_external_database_relation('hg38', 'hg38 RNA-Seq annotations');
call add_external_database_relation('mm39', 'mm39 RNA-Seq annotations');
call add_external_database_relation('mm10', 'mm10 RNA-Seq annotations');
call add_external_database_relation('rn7', 'rn7 RNA-Seq annotations');

drop procedure add_external_database_relation;

-- denormalized table joining genes and compositeSequences; maintained by TableMaintenanceUtil.
drop table if exists GENE2CS;
create table GENE2CS
(
    GENE BIGINT not null,
    CS   BIGINT not null,
    AD   BIGINT not null,
    primary key (AD, CS, GENE)
);
alter table GENE2CS
    add constraint GENE2CS_ARRAY_DESIGN_FKC foreign key (AD) references ARRAY_DESIGN (ID) on update cascade on delete cascade;
alter table GENE2CS
    add constraint GENE2CS_CS_FKC foreign key (CS) references COMPOSITE_SEQUENCE (ID) on update cascade on delete cascade;
alter table GENE2CS
    add constraint GENE2CS_GENE_FKC foreign key (GENE) references CHROMOSOME_FEATURE (ID) on update cascade on delete cascade;

drop table if exists EXPRESSION_EXPERIMENT2CHARACTERISTIC;
create table EXPRESSION_EXPERIMENT2CHARACTERISTIC
(
    ID                       bigint,
    NAME                     varchar(255),
    DESCRIPTION              text,
    CATEGORY                 varchar(255),
    CATEGORY_URI             varchar(255),
    VALUE                    varchar(255),
    VALUE_URI                varchar(255),
    ORIGINAL_VALUE           varchar(255),
    EVIDENCE_CODE            varchar(255),
    EXPRESSION_EXPERIMENT_FK bigint,
    LEVEL                    varchar(255),
    primary key (ID, EXPRESSION_EXPERIMENT_FK, LEVEL)
);

-- note: constraint names cannot exceed 64 characters, so we cannot use the usual naming convention
-- no URI exceeds 100 characters in practice, so we only index a prefix
alter table EXPRESSION_EXPERIMENT2CHARACTERISTIC
    add constraint EE2C_CHARACTERISTIC_FKC foreign key (ID) references CHARACTERISTIC (ID) on update cascade on delete cascade,
    add constraint EE2C_EXPRESSION_EXPERIMENT_FKC foreign key (EXPRESSION_EXPERIMENT_FK) references INVESTIGATION (id) on update cascade on delete cascade,
    add index EE2C_VALUE (VALUE),
    add index EE2C_CATEGORY (CATEGORY),
    add index EE2C_VALUE_URI_VALUE (VALUE_URI(100), VALUE),
    add index EE2C_CATEGORY_URI_CATEGORY_VALUE_URI_VALUE (CATEGORY_URI(100), CATEGORY, VALUE_URI(100), VALUE),
    add index EE2C_LEVEL (LEVEL);