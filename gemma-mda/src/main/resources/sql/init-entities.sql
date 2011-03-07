-- Initialize the database with some scraps of data. See also init-indices.sql and mysql-acegi-acl.sql.

delete from CONTACT;
delete from TAXON;
delete from EXTERNAL_DATABASE;
delete from AUDIT_TRAIL;

-- alter CHROMOSOME_FEATURE for case insensitive search
ALTER TABLE CHROMOSOME_FEATURE MODIFY OFFICIAL_SYMBOL varchar(255) character set latin1 default NULL;
ALTER TABLE CHROMOSOME_FEATURE MODIFY NAME varchar(255) character set latin1 default NULL;
ALTER TABLE CHROMOSOME_FEATURE MODIFY NCBI_ID varchar(255) character set latin1 default NULL;
-- alter GENE_ALIAS for case insensitive search
ALTER TABLE GENE_ALIAS MODIFY ALIAS varchar(255) character set latin1 default NULL;

-- wider columns.
alter table BIO_SEQUENCE modify SEQUENCE LONGTEXT;
alter table JOB_INFO modify MESSAGES LONGTEXT;


-- all of these are used.
insert into AUDIT_TRAIL VALUES (1); 
insert into AUDIT_TRAIL VALUES (2); 
insert into AUDIT_TRAIL VALUES (3); 
insert into AUDIT_TRAIL VALUES (4);  
insert into AUDIT_TRAIL VALUES (5); 
insert into AUDIT_TRAIL VALUES (6); 
insert into AUDIT_TRAIL VALUES (7); 
insert into AUDIT_TRAIL VALUES (8);  
insert into AUDIT_TRAIL VALUES (9); 
insert into AUDIT_TRAIL VALUES (10);  
insert into AUDIT_TRAIL VALUES (11);  
insert into AUDIT_TRAIL VALUES (12);  
insert into AUDIT_TRAIL VALUES (13);  
insert into AUDIT_TRAIL VALUES (14);


-- username=administrator: id = 1, password = 'administrator', audit trail #1 using salt=username ('administrator')
insert into CONTACT (ID, CLASS, NAME, LAST_NAME, USER_NAME, PASSWORD, ENABLED, AUDIT_TRAIL_FK, EMAIL, PASSWORD_HINT) values (1, "UserImpl", "administrator",  "", "administrator", "b7338dcc17d6b6c199a75540aab6d0506567b980", 1, 1, "gemma@chibi.ubc.ca", "hint");

-- initialize the audit trails 
set @n:=now();
insert into AUDIT_EVENT VALUES (1, @n, 'C', 'From init script', '', 1, NULL, 1); 
insert into AUDIT_EVENT VALUES (2, @n, 'C', 'From init script', '', 1, NULL, 2); 
insert into AUDIT_EVENT VALUES (3, @n, 'C', 'From init script', '', 1, NULL, 3);  
insert into AUDIT_EVENT VALUES (4, @n, 'C', 'From init script', '', 1, NULL, 4); 
insert into AUDIT_EVENT VALUES (5, @n, 'C', 'From init script', '', 1, NULL, 5); 
insert into AUDIT_EVENT VALUES (6, @n, 'C', 'From init script', '', 1, NULL, 6); 
insert into AUDIT_EVENT VALUES (7, @n, 'C', 'From init script', '', 1, NULL, 7); 
insert into AUDIT_EVENT VALUES (8, @n, 'C', 'From init script', '', 1, NULL, 8); 
insert into AUDIT_EVENT VALUES (9, @n, 'C', 'From init script', '', 1, NULL, 9); 
insert into AUDIT_EVENT VALUES (10, @n, 'C', 'From init script', '', 1, NULL, 10); 
insert into AUDIT_EVENT VALUES (11, @n, 'C', 'From init script', '', 1, NULL, 11); 
insert into AUDIT_EVENT VALUES (12, @n, 'C', 'From init script', '', 1, NULL, 12); 
insert into AUDIT_EVENT VALUES (13, @n, 'C', 'From init script', '', 1, NULL, 13);
insert into AUDIT_EVENT VALUES (14, @n, 'C', 'From init script', '', 1, NULL, 14);


-- username=gemmaAgent: id = 2, password = 'gemmaAgent', audit trail #2, using salt={username}
insert into CONTACT (ID, CLASS, NAME, LAST_NAME, USER_NAME, PASSWORD, ENABLED, AUDIT_TRAIL_FK, EMAIL, PASSWORD_HINT) values (2, "UserImpl", "gemmaAgent",  "", "gemmaAgent", "a99c3785155e31ac8f9273537f14e9304cc22f20", 1, 2, "gemma@chibi.ubc.ca", "hint");

-- Note that 'Administrators' is a constant set in AuthorityConstants.
insert into USER_GROUP (ID, NAME, DESCRIPTION, AUDIT_TRAIL_FK) VALUES (1, "Administrators", "Users with administrative rights", 3);
insert into USER_GROUP (ID, NAME, DESCRIPTION, AUDIT_TRAIL_FK) VALUES (2, "Users", "Default group for all authenticated users", 4);
insert into USER_GROUP (ID, NAME, DESCRIPTION, AUDIT_TRAIL_FK) VALUES (3, "Agents", "For 'autonomous' agents that run within the server context, such as scheduled tasks.", 5);
insert into GROUP_AUTHORITY (ID, AUTHORITY, GROUP_FK) VALUES (1, "ADMIN", 1);
insert into GROUP_AUTHORITY (ID, AUTHORITY, GROUP_FK) VALUES (2, "USER", 2);
insert into GROUP_AUTHORITY (ID, AUTHORITY, GROUP_FK) VALUES (3, "AGENT", 3);
insert into GROUP_MEMBERS (USER_GROUPS_FK, GROUP_MEMBERS_FK) VALUES (1, 1);
insert into GROUP_MEMBERS (USER_GROUPS_FK, GROUP_MEMBERS_FK) VALUES (3, 2);






-- taxa
insert into TAXON (SCIENTIFIC_NAME,COMMON_NAME,NCBI_ID,IS_SPECIES,IS_GENES_USABLE) values ("Homo sapiens","human","9606",1,1);
insert into TAXON (SCIENTIFIC_NAME,COMMON_NAME,NCBI_ID,IS_SPECIES,IS_GENES_USABLE) values ("Mus musculus","mouse","10090",1,1); 
insert into TAXON (SCIENTIFIC_NAME,COMMON_NAME,NCBI_ID,IS_SPECIES,IS_GENES_USABLE) values ("Rattus norvegicus","rat","10116",1,1);
insert into TAXON (SCIENTIFIC_NAME,COMMON_NAME,NCBI_ID,IS_SPECIES,IS_GENES_USABLE) values ("Saccharomyces cerevisiae","yeast","4932",1,0);
insert into TAXON (SCIENTIFIC_NAME,COMMON_NAME,NCBI_ID,IS_SPECIES,IS_GENES_USABLE) values ("Danio rerio","zebrafish","7955",1,0); 
insert into TAXON (SCIENTIFIC_NAME,COMMON_NAME,NCBI_ID,IS_SPECIES,IS_GENES_USABLE) values ("Drosophila melanogaster","fly","7227",1,0);
insert into TAXON (SCIENTIFIC_NAME,COMMON_NAME,NCBI_ID,IS_SPECIES,IS_GENES_USABLE) values ("Caenorhabditis elegans","worm","6239",1,0);


-- external databases
insert into EXTERNAL_DATABASE (NAME, DESCRIPTION, WEB_URI, FTP_URI, AUDIT_TRAIL_FK, TYPE) values ("PubMed", "PubMed database from NCBI", "http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?db=PubMed", "ftp://ftp.ncbi.nlm.nih.gov/pubmed/", 6, "LITERATURE");
insert into EXTERNAL_DATABASE (NAME, DESCRIPTION,  WEB_URI, FTP_URI, AUDIT_TRAIL_FK, TYPE) values ("GO", "Gene Ontology database", "http://www.godatabase.org/dev/database/", "http://archive.godatabase.org", 7, "ONTOLOGY");
insert into EXTERNAL_DATABASE (NAME, DESCRIPTION,  WEB_URI, FTP_URI, AUDIT_TRAIL_FK, TYPE) values ("GEO", "Gene Expression Omnibus", "http://www.ncbi.nlm.nih.gov/geo/", "ftp://ftp.ncbi.nih.gov/pub/geo/DATA", 8, "EXPRESSION");
insert into EXTERNAL_DATABASE (NAME, DESCRIPTION,  WEB_URI, FTP_URI, AUDIT_TRAIL_FK, TYPE) values ("ArrayExpress", "EBI ArrayExpress", "http://www.ebi.ac.uk/arrayexpress/", "ftp://ftp.ebi.ac.uk/pub/databases/microarray/data/", 9, "EXPRESSION");
insert into EXTERNAL_DATABASE (NAME, DESCRIPTION,  WEB_URI, FTP_URI, AUDIT_TRAIL_FK, TYPE) values ("Genbank", "NCBI Genbank", "http://www.ncbi.nlm.nih.gov/Genbank/index.html", "ftp://ftp.ncbi.nih.gov/genbank/", 10, "SEQUENCE");
insert into EXTERNAL_DATABASE (NAME, DESCRIPTION,  WEB_URI, FTP_URI, AUDIT_TRAIL_FK, TYPE) values ("Entrez Gene", "NCBI Gene database", "http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?db=gene", "ftp://ftp.ncbi.nih.gov/gene/", 11, "SEQUENCE");
insert into EXTERNAL_DATABASE (NAME, DESCRIPTION,  WEB_URI, FTP_URI, AUDIT_TRAIL_FK, TYPE) values ("Ensembl", "EMBL - EBI/Sanger Institute genome annotations", "http://www.ensembl.org/", "ftp://ftp.ensembl.org/pub/", 12, "GENOME");
insert into EXTERNAL_DATABASE (NAME, DESCRIPTION,  WEB_URI, FTP_URI, AUDIT_TRAIL_FK, TYPE) values ("OBO_REL", "Open Biomedical Ontologies Relationships", "http://www.obofoundry.org/ro/", "", 13, "ONTOLOGY");
insert into EXTERNAL_DATABASE (NAME, DESCRIPTION,  WEB_URI, FTP_URI, AUDIT_TRAIL_FK, TYPE) values ("STRING", "STRING - Known and Predicted Protein-Protein Interactions", "http://string-db.org/version_8_2/newstring_cgi/show_network_section.pl?identifiers=", "", 14, "PROTEIN");

-- denormalized table joining genes and compositeSequences
create table GENE2CS (
	GENE BIGINT not null, 
	CS BIGINT not null, 
	GTYPE CHAR(25) not null,
	INDEX USING HASH (GENE),
	INDEX USING HASH (CS),
	INDEX USING HASH (GTYPE)
) ENGINE=InnoDB;

-- denormalize probe2probe coexpressions
-- see init-triggers for triggers that populate these denormalized fields
alter table HUMAN_PROBE_CO_EXPRESSION add column FIRST_DESIGN_ELEMENT_FK bigint(20), add column SECOND_DESIGN_ELEMENT_FK bigint(20);
alter table MOUSE_PROBE_CO_EXPRESSION add column FIRST_DESIGN_ELEMENT_FK bigint(20), add column SECOND_DESIGN_ELEMENT_FK bigint(20);
alter table RAT_PROBE_CO_EXPRESSION add column FIRST_DESIGN_ELEMENT_FK bigint(20), add column SECOND_DESIGN_ELEMENT_FK bigint(20);
alter table OTHER_PROBE_CO_EXPRESSION add column FIRST_DESIGN_ELEMENT_FK bigint(20), add column SECOND_DESIGN_ELEMENT_FK bigint(20);

update HUMAN_PROBE_CO_EXPRESSION p, PROCESSED_EXPRESSION_DATA_VECTOR dedvFirst, PROCESSED_EXPRESSION_DATA_VECTOR dedvSecond SET p.FIRST_DESIGN_ELEMENT_FK=dedvFirst.DESIGN_ELEMENT_FK, p.SECOND_DESIGN_ELEMENT_FK=dedvSecond.DESIGN_ELEMENT_FK WHERE p.FIRST_VECTOR_FK=dedvFirst.ID AND p.SECOND_VECTOR_FK=dedvSecond.ID;
update MOUSE_PROBE_CO_EXPRESSION p, PROCESSED_EXPRESSION_DATA_VECTOR dedvFirst, PROCESSED_EXPRESSION_DATA_VECTOR dedvSecond SET p.FIRST_DESIGN_ELEMENT_FK=dedvFirst.DESIGN_ELEMENT_FK, p.SECOND_DESIGN_ELEMENT_FK=dedvSecond.DESIGN_ELEMENT_FK WHERE p.FIRST_VECTOR_FK=dedvFirst.ID AND p.SECOND_VECTOR_FK=dedvSecond.ID;
update RAT_PROBE_CO_EXPRESSION p, PROCESSED_EXPRESSION_DATA_VECTOR dedvFirst, PROCESSED_EXPRESSION_DATA_VECTOR dedvSecond SET p.FIRST_DESIGN_ELEMENT_FK=dedvFirst.DESIGN_ELEMENT_FK, p.SECOND_DESIGN_ELEMENT_FK=dedvSecond.DESIGN_ELEMENT_FK WHERE p.FIRST_VECTOR_FK=dedvFirst.ID AND p.SECOND_VECTOR_FK=dedvSecond.ID;
update OTHER_PROBE_CO_EXPRESSION p, PROCESSED_EXPRESSION_DATA_VECTOR dedvFirst, PROCESSED_EXPRESSION_DATA_VECTOR dedvSecond SET p.FIRST_DESIGN_ELEMENT_FK=dedvFirst.DESIGN_ELEMENT_FK, p.SECOND_DESIGN_ELEMENT_FK=dedvSecond.DESIGN_ELEMENT_FK WHERE p.FIRST_VECTOR_FK=dedvFirst.ID AND p.SECOND_VECTOR_FK=dedvSecond.ID;

alter table HUMAN_PROBE_CO_EXPRESSION add index EEkey (EXPRESSION_EXPERIMENT_FK, FIRST_DESIGN_ELEMENT_FK);
alter table MOUSE_PROBE_CO_EXPRESSION add index EEkey (EXPRESSION_EXPERIMENT_FK, FIRST_DESIGN_ELEMENT_FK);
alter table RAT_PROBE_CO_EXPRESSION add index EEkey (EXPRESSION_EXPERIMENT_FK, FIRST_DESIGN_ELEMENT_FK);
alter table OTHER_PROBE_CO_EXPRESSION add index EEkey (EXPRESSION_EXPERIMENT_FK, FIRST_DESIGN_ELEMENT_FK);



