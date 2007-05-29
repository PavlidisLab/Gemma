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
insert into EXTERNAL_DATABASE (NAME, DESCRIPTION, WEB_URI, FTP_URI, AUDIT_TRAIL_FK) values ("PubMed", "PubMed database from NCBI", "http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?db=PubMed", "ftp://ftp.ncbi.nlm.nih.gov/pubmed/", 4);
insert into EXTERNAL_DATABASE (NAME, DESCRIPTION,  WEB_URI, FTP_URI, AUDIT_TRAIL_FK) values ("GO", "Gene Ontology database", "http://www.godatabase.org/dev/database/", "http://archive.godatabase.org", 5);
insert into EXTERNAL_DATABASE (NAME, DESCRIPTION,  WEB_URI, FTP_URI, AUDIT_TRAIL_FK) values ("GEO", "Gene Expression Omnibus", "http://www.ncbi.nlm.nih.gov/geo/", "ftp://ftp.ncbi.nih.gov/pub/geo/DATA", 6);
insert into EXTERNAL_DATABASE (NAME, DESCRIPTION,  WEB_URI, FTP_URI, AUDIT_TRAIL_FK) values ("ArrayExpress", "EBI ArrayExpress", "http://www.ebi.ac.uk/arrayexpress/", "ftp://ftp.ebi.ac.uk/pub/databases/microarray/data/", 7);
insert into EXTERNAL_DATABASE (NAME, DESCRIPTION,  WEB_URI, FTP_URI, AUDIT_TRAIL_FK) values ("Genbank", "NCBI Genbank", "http://www.ncbi.nlm.nih.gov/Genbank/index.html", "ftp://ftp.ncbi.nih.gov/genbank/", 8);
insert into EXTERNAL_DATABASE (NAME, DESCRIPTION,  WEB_URI, FTP_URI, AUDIT_TRAIL_FK) values ("Entrez Gene", "NCBI Gene database", "http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?db=gene", "ftp://ftp.ncbi.nih.gov/gene/", 9);
insert into AUDIT_TRAIL VALUES (); 
insert into EXTERNAL_DATABASE (NAME, DESCRIPTION,  WEB_URI, FTP_URI, AUDIT_TRAIL_FK) values ("OBO_REL", "Open Biomedical Ontologies Relationships", "http://www.obofoundry.org/ro/", "", 10);


-- denormalized table joining genes and compositeSequences
create table GENE2CS (
	GENE BIGINT not null, 
	CS BIGINT not null, 
	key (GENE),
	key(CS)
	) ENGINE=INNODB;
	
-- denormalize probe2probe coexpressions

alter table HUMAN_PROBE_CO_EXPRESSION add column FIRST_DESIGN_ELEMENT_FK bigint(20), add column SECOND_DESIGN_ELEMENT_FK bigint(20), add column EXPRESSION_EXPERIMENT_FK bigint(20);
alter table MOUSE_PROBE_CO_EXPRESSION add column FIRST_DESIGN_ELEMENT_FK bigint(20), add column SECOND_DESIGN_ELEMENT_FK bigint(20), add column EXPRESSION_EXPERIMENT_FK bigint(20);
alter table RAT_PROBE_CO_EXPRESSION add column FIRST_DESIGN_ELEMENT_FK bigint(20), add column SECOND_DESIGN_ELEMENT_FK bigint(20), add column EXPRESSION_EXPERIMENT_FK bigint(20);
alter table OTHER_PROBE_CO_EXPRESSION add column FIRST_DESIGN_ELEMENT_FK bigint(20), add column SECOND_DESIGN_ELEMENT_FK bigint(20), add column EXPRESSION_EXPERIMENT_FK bigint(20);

update HUMAN_PROBE_CO_EXPRESSION p, DESIGN_ELEMENT_DATA_VECTOR dedvFirst, DESIGN_ELEMENT_DATA_VECTOR dedvSecond SET p.EXPRESSION_EXPERIMENT_FK=dedvFirst.EXPRESSION_EXPERIMENT_FK, p.FIRST_DESIGN_ELEMENT_FK=dedvFirst.DESIGN_ELEMENT_FK, p.SECOND_DESIGN_ELEMENT_FK=dedvSecond.DESIGN_ELEMENT_FK WHERE p.FIRST_VECTOR_FK=dedvFirst.ID AND p.SECOND_VECTOR_FK=dedvSecond.ID;
update MOUSE_PROBE_CO_EXPRESSION p, DESIGN_ELEMENT_DATA_VECTOR dedvFirst, DESIGN_ELEMENT_DATA_VECTOR dedvSecond SET p.EXPRESSION_EXPERIMENT_FK=dedvFirst.EXPRESSION_EXPERIMENT_FK, p.FIRST_DESIGN_ELEMENT_FK=dedvFirst.DESIGN_ELEMENT_FK, p.SECOND_DESIGN_ELEMENT_FK=dedvSecond.DESIGN_ELEMENT_FK WHERE p.FIRST_VECTOR_FK=dedvFirst.ID AND p.SECOND_VECTOR_FK=dedvSecond.ID;
update RAT_PROBE_CO_EXPRESSION p, DESIGN_ELEMENT_DATA_VECTOR dedvFirst, DESIGN_ELEMENT_DATA_VECTOR dedvSecond SET p.EXPRESSION_EXPERIMENT_FK=dedvFirst.EXPRESSION_EXPERIMENT_FK, p.FIRST_DESIGN_ELEMENT_FK=dedvFirst.DESIGN_ELEMENT_FK, p.SECOND_DESIGN_ELEMENT_FK=dedvSecond.DESIGN_ELEMENT_FK WHERE p.FIRST_VECTOR_FK=dedvFirst.ID AND p.SECOND_VECTOR_FK=dedvSecond.ID;
update OTHER_PROBE_CO_EXPRESSION p, DESIGN_ELEMENT_DATA_VECTOR dedvFirst, DESIGN_ELEMENT_DATA_VECTOR dedvSecond SET p.EXPRESSION_EXPERIMENT_FK=dedvFirst.EXPRESSION_EXPERIMENT_FK, p.FIRST_DESIGN_ELEMENT_FK=dedvFirst.DESIGN_ELEMENT_FK, p.SECOND_DESIGN_ELEMENT_FK=dedvSecond.DESIGN_ELEMENT_FK WHERE p.FIRST_VECTOR_FK=dedvFirst.ID AND p.SECOND_VECTOR_FK=dedvSecond.ID;

alter table HUMAN_PROBE_CO_EXPRESSION add index firstToSecondElement (FIRST_DESIGN_ELEMENT_FK,SECOND_DESIGN_ELEMENT_FK,EXPRESSION_EXPERIMENT_FK,QUANTITATION_TYPE_FK), add index EEkey (EXPRESSION_EXPERIMENT_FK), add index secondElement (SECOND_DESIGN_ELEMENT_FK);
alter table MOUSE_PROBE_CO_EXPRESSION add index firstToSecondElement (FIRST_DESIGN_ELEMENT_FK,SECOND_DESIGN_ELEMENT_FK,EXPRESSION_EXPERIMENT_FK,QUANTITATION_TYPE_FK), add index EEkey (EXPRESSION_EXPERIMENT_FK), add index secondElement (SECOND_DESIGN_ELEMENT_FK);
alter table RAT_PROBE_CO_EXPRESSION add index firstToSecondElement (FIRST_DESIGN_ELEMENT_FK,SECOND_DESIGN_ELEMENT_FK,EXPRESSION_EXPERIMENT_FK,QUANTITATION_TYPE_FK), add index EEkey (EXPRESSION_EXPERIMENT_FK), add index secondElement (SECOND_DESIGN_ELEMENT_FK);
alter table OTHER_PROBE_CO_EXPRESSION add index firstToSecondElement (FIRST_DESIGN_ELEMENT_FK,SECOND_DESIGN_ELEMENT_FK,EXPRESSION_EXPERIMENT_FK,QUANTITATION_TYPE_FK), add index EEkey (EXPRESSION_EXPERIMENT_FK), add index secondElement (SECOND_DESIGN_ELEMENT_FK);

--- Triggers to fill in denormalized fields

-- redefine command delimiter so we can enter triggers containing ';'
DELIMITER |
CREATE TRIGGER humanCoexpTrigger BEFORE INSERT ON HUMAN_PROBE_CO_EXPRESSION
  FOR EACH ROW BEGIN  
    select dedvFirst.EXPRESSION_EXPERIMENT_FK, dedvFirst.DESIGN_ELEMENT_FK, dedvSecond.DESIGN_ELEMENT_FK INTO @ee, @firstDEDV, @secondDEDV FROM DESIGN_ELEMENT_DATA_VECTOR dedvFirst, DESIGN_ELEMENT_DATA_VECTOR dedvSecond WHERE NEW.FIRST_VECTOR_FK=dedvFirst.ID AND NEW.SECOND_VECTOR_FK=dedvSecond.ID;
    SET NEW.EXPRESSION_EXPERIMENT_FK = @ee;
    SET NEW.FIRST_DESIGN_ELEMENT_FK = @firstDEDV;
    SET NEW.SECOND_DESIGN_ELEMENT_FK = @secondDEDV;
   END;
|
CREATE TRIGGER mouseCoexpTrigger BEFORE INSERT ON MOUSE_PROBE_CO_EXPRESSION
  FOR EACH ROW BEGIN  
    select dedvFirst.EXPRESSION_EXPERIMENT_FK, dedvFirst.DESIGN_ELEMENT_FK, dedvSecond.DESIGN_ELEMENT_FK INTO @ee, @firstDEDV, @secondDEDV FROM DESIGN_ELEMENT_DATA_VECTOR dedvFirst, DESIGN_ELEMENT_DATA_VECTOR dedvSecond WHERE NEW.FIRST_VECTOR_FK=dedvFirst.ID AND NEW.SECOND_VECTOR_FK=dedvSecond.ID;
    SET NEW.EXPRESSION_EXPERIMENT_FK = @ee;
    SET NEW.FIRST_DESIGN_ELEMENT_FK = @firstDEDV;
    SET NEW.SECOND_DESIGN_ELEMENT_FK = @secondDEDV;
   END;
|
CREATE TRIGGER ratCoexpTrigger BEFORE INSERT ON RAT_PROBE_CO_EXPRESSION
  FOR EACH ROW BEGIN  
    select dedvFirst.EXPRESSION_EXPERIMENT_FK, dedvFirst.DESIGN_ELEMENT_FK, dedvSecond.DESIGN_ELEMENT_FK INTO @ee, @firstDEDV, @secondDEDV FROM DESIGN_ELEMENT_DATA_VECTOR dedvFirst, DESIGN_ELEMENT_DATA_VECTOR dedvSecond WHERE NEW.FIRST_VECTOR_FK=dedvFirst.ID AND NEW.SECOND_VECTOR_FK=dedvSecond.ID;
    SET NEW.EXPRESSION_EXPERIMENT_FK = @ee;
    SET NEW.FIRST_DESIGN_ELEMENT_FK = @firstDEDV;
    SET NEW.SECOND_DESIGN_ELEMENT_FK = @secondDEDV;
   END;
|

DELIMITER ;
DELIMITER |

CREATE TRIGGER otherCoexpTrigger BEFORE INSERT ON OTHER_PROBE_CO_EXPRESSION
  FOR EACH ROW BEGIN  
    select dedvFirst.EXPRESSION_EXPERIMENT_FK, dedvFirst.DESIGN_ELEMENT_FK, dedvSecond.DESIGN_ELEMENT_FK INTO @ee, @firstDEDV, @secondDEDV FROM DESIGN_ELEMENT_DATA_VECTOR dedvFirst, DESIGN_ELEMENT_DATA_VECTOR dedvSecond WHERE NEW.FIRST_VECTOR_FK=dedvFirst.ID AND NEW.SECOND_VECTOR_FK=dedvSecond.ID;
    SET NEW.EXPRESSION_EXPERIMENT_FK = @ee;
    SET NEW.FIRST_DESIGN_ELEMENT_FK = @firstDEDV;
    SET NEW.SECOND_DESIGN_ELEMENT_FK = @secondDEDV;
   END;
|
-- restore delimiter.
DELIMITER ;


