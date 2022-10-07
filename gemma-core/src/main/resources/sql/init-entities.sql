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

-- username=gemmaAgent: id = 2, password = 'gemmaAgent', audit trail #2, using salt={username}
insert into CONTACT (ID, CLASS, NAME, LAST_NAME, USER_NAME, PASSWORD, ENABLED, EMAIL, PASSWORD_HINT) values (2, "User", "gemmaAgent",  "", "gemmaAgent", "a99c3785155e31ac8f9273537f14e9304cc22f20", 1, "pavlab-support@msl.ubc.ca", "hint");

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
insert into EXTERNAL_DATABASE (NAME, DESCRIPTION, WEB_URI, FTP_URI,  TYPE) values ("PubMed", "PubMed database from NCBI", "http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?db=PubMed", "ftp://ftp.ncbi.nlm.nih.gov/pubmed/", "LITERATURE");
insert into EXTERNAL_DATABASE (NAME, DESCRIPTION,  WEB_URI, FTP_URI, TYPE) values ("GO", "Gene Ontology database", "http://www.godatabase.org/dev/database/", "http://archive.godatabase.org", "ONTOLOGY");
insert into EXTERNAL_DATABASE (NAME, DESCRIPTION,  WEB_URI, FTP_URI, TYPE) values ("GEO", "Gene Expression Omnibus", "http://www.ncbi.nlm.nih.gov/geo/", "ftp://ftp.ncbi.nih.gov/pub/geo/DATA", "EXPRESSION");
insert into EXTERNAL_DATABASE (NAME, DESCRIPTION,  WEB_URI, FTP_URI, TYPE) values ("ArrayExpress", "EBI ArrayExpress", "http://www.ebi.ac.uk/arrayexpress/", "ftp://ftp.ebi.ac.uk/pub/databases/microarray/data/", "EXPRESSION");
insert into EXTERNAL_DATABASE (NAME, DESCRIPTION,  WEB_URI, FTP_URI, TYPE) values ("Genbank", "NCBI Genbank", "http://www.ncbi.nlm.nih.gov/Genbank/index.html", "ftp://ftp.ncbi.nih.gov/genbank/", "SEQUENCE");
insert into EXTERNAL_DATABASE (NAME, DESCRIPTION,  WEB_URI, FTP_URI, TYPE) values ("Entrez Gene", "NCBI Gene database", "http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?db=gene", "ftp://ftp.ncbi.nih.gov/gene/", "SEQUENCE");
insert into EXTERNAL_DATABASE (NAME, DESCRIPTION,  WEB_URI, FTP_URI, TYPE) values ("Ensembl", "EMBL - EBI/Sanger Institute genome annotations", "http://www.ensembl.org/", "ftp://ftp.ensembl.org/pub/", "GENOME");
insert into EXTERNAL_DATABASE (NAME, DESCRIPTION,  WEB_URI, FTP_URI, TYPE) values ("OBO_REL", "Open Biomedical Ontologies Relationships", "http://www.obofoundry.org/ro/", "", "ONTOLOGY");
insert into EXTERNAL_DATABASE (NAME, DESCRIPTION,  WEB_URI, FTP_URI, TYPE) values ("STRING", "STRING - Known and Predicted Protein-Protein Interactions", "http://string-db.org/version_8_2/newstring_cgi/show_network_section.pl?identifiers=", "", "PROTEIN");

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
    add foreign key GENE2CS_ARRAY_DESIGN_FKC (AD) references ARRAY_DESIGN (ID) on update cascade on delete cascade;
alter table GENE2CS
    add foreign key GENE2CS_CS_FKC (CS) references COMPOSITE_SEQUENCE (ID) on update cascade on delete cascade;
alter table GENE2CS
    add foreign key GENE2CS_GENE_FKC (GENE) references CHROMOSOME_FEATURE (ID) on update cascade on delete cascade;