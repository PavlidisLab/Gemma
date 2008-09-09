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
insert into AUDIT_TRAIL VALUES (); 
insert into AUDIT_TRAIL VALUES (); 
insert into AUDIT_TRAIL VALUES (); 
insert into AUDIT_TRAIL VALUES ();  
insert into AUDIT_TRAIL VALUES (); 
insert into AUDIT_TRAIL VALUES (); 
insert into AUDIT_TRAIL VALUES (); 
insert into AUDIT_TRAIL VALUES ();  
insert into AUDIT_TRAIL VALUES (); 
insert into AUDIT_TRAIL VALUES (); 
insert into AUDIT_TRAIL VALUES (); 

-- username=administrator: primary key = 1, password = test, audit trail #1, role #1
insert into CONTACT (CLASS, NAME, LAST_NAME, USER_NAME, PASSWORD, ENABLED, AUDIT_TRAIL_FK, EMAIL, PASSWORD_HINT) values ("UserImpl", "nobody",  "nobody", "administrator", "1ee223e4d9a7c2bf81996941705435d7a43bee9a", 1, 1, "admin@gemma.org", "hint");
insert into USER_ROLE (NAME, USER_NAME, USERS_FK ) values ("admin", "administrator", 1 );

-- username=test: primary key = 2, password = test, audit trail #2, role #2
insert into CONTACT (CLASS, NAME, LAST_NAME, USER_NAME, PASSWORD, ENABLED, AUDIT_TRAIL_FK, EMAIL, PASSWORD_HINT) values ("UserImpl", "test", "test", "test", "1ee223e4d9a7c2bf81996941705435d7a43bee9a", 1, 2, "test@gemma.org", "hint");
insert into USER_ROLE (NAME, USER_NAME, USERS_FK ) values ("user", "test", 2 );

-- contact, audit trail #3.
insert into CONTACT (CLASS, NAME, EMAIL, AUDIT_TRAIL_FK) values ("ContactImpl", "admin", "another.admin@gemma.org", 3);

-- taxa
insert into TAXON (SCIENTIFIC_NAME,COMMON_NAME,NCBI_ID) values ("Homo sapiens","human","9606");
insert into TAXON (SCIENTIFIC_NAME,COMMON_NAME,NCBI_ID) values ("Mus musculus","mouse","10090"); 
insert into TAXON (SCIENTIFIC_NAME,COMMON_NAME,NCBI_ID) values ("Rattus norvegicus","rat","10116");

-- external databases
insert into EXTERNAL_DATABASE (NAME, DESCRIPTION, WEB_URI, FTP_URI, AUDIT_TRAIL_FK, TYPE) values ("PubMed", "PubMed database from NCBI", "http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?db=PubMed", "ftp://ftp.ncbi.nlm.nih.gov/pubmed/", 4, "LITERATURE");
insert into EXTERNAL_DATABASE (NAME, DESCRIPTION,  WEB_URI, FTP_URI, AUDIT_TRAIL_FK, TYPE) values ("GO", "Gene Ontology database", "http://www.godatabase.org/dev/database/", "http://archive.godatabase.org", 5, "ONTOLOGY");
insert into EXTERNAL_DATABASE (NAME, DESCRIPTION,  WEB_URI, FTP_URI, AUDIT_TRAIL_FK, TYPE) values ("GEO", "Gene Expression Omnibus", "http://www.ncbi.nlm.nih.gov/geo/", "ftp://ftp.ncbi.nih.gov/pub/geo/DATA", 6, "EXPRESSION");
insert into EXTERNAL_DATABASE (NAME, DESCRIPTION,  WEB_URI, FTP_URI, AUDIT_TRAIL_FK, TYPE) values ("ArrayExpress", "EBI ArrayExpress", "http://www.ebi.ac.uk/arrayexpress/", "ftp://ftp.ebi.ac.uk/pub/databases/microarray/data/", 7, "EXPRESSION");
insert into EXTERNAL_DATABASE (NAME, DESCRIPTION,  WEB_URI, FTP_URI, AUDIT_TRAIL_FK, TYPE) values ("Genbank", "NCBI Genbank", "http://www.ncbi.nlm.nih.gov/Genbank/index.html", "ftp://ftp.ncbi.nih.gov/genbank/", 8, "SEQUENCE");
insert into EXTERNAL_DATABASE (NAME, DESCRIPTION,  WEB_URI, FTP_URI, AUDIT_TRAIL_FK, TYPE) values ("Entrez Gene", "NCBI Gene database", "http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?db=gene", "ftp://ftp.ncbi.nih.gov/gene/", 9, "SEQUENCE");
insert into EXTERNAL_DATABASE (NAME, DESCRIPTION,  WEB_URI, FTP_URI, AUDIT_TRAIL_FK, TYPE) values ("Ensembl", "EMBL - EBI/Sanger Institute genome annotations", "http://www.ensembl.org/", "ftp://ftp.ensembl.org/pub/", 10, "GENOME");
insert into EXTERNAL_DATABASE (NAME, DESCRIPTION,  WEB_URI, FTP_URI, AUDIT_TRAIL_FK, TYPE) values ("OBO_REL", "Open Biomedical Ontologies Relationships", "http://www.obofoundry.org/ro/", "", 11, "ONTOLOGY");


-- denormalized table joining genes and compositeSequences
create table GENE2CS (
	GENE BIGINT not null, 
	CS BIGINT not null, 
	GTYPE CHAR(25) not null,
	INDEX USING HASH (GENE),
	INDEX USING HASH (CS),
	INDEX USING HASH (GTYPE)
) ENGINE=MEMORY;

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



