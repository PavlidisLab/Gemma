delete from CONTACT;
delete from TAXON;
delete from EXTERNAL_DATABASE;

--administrator
insert into CONTACT (CLASS, NAME, LAST_NAME, FIRST_NAME, MIDDLE_NAME, USER_NAME, PASSWORD, CONFIRM_PASSWORD, ENABLED) values ("UserImpl", "nobody", "nobody", "nobody", "nobody", "administrator", "cec6877e42b179b133c2a6c1285488755aefc071", "cec6877e42b179b133c2a6c1285488755aefc071", 1);
insert into CONTACT (CLASS, NAME, EMAIL) values ("edu.columbia.gemma.common.auditAndSecurity.ContactImpl", "admin", "admin@gemma.org");
insert into USER_ROLE (NAME, USER_NAME) values ("admin", "administrator");

--Users

--pavlab
insert into CONTACT (CLASS, NAME, LAST_NAME, FIRST_NAME, MIDDLE_NAME, USER_NAME, PASSWORD, CONFIRM_PASSWORD, ENABLED) values ("UserImpl", "pavlab", "lastname", "firstname", "pavlab_middlename", "pavlab", "7478822cc546bb339c7cd198ca96df2b7a50cf5c", "7478822cc546bb339c7cd198ca96df2b7a50cf5c", 1);
insert into USER_ROLE (NAME, USER_NAME) values ("user", "pavlab");

--Paul Pavlidis
insert into CONTACT (CLASS, NAME, LAST_NAME, FIRST_NAME, MIDDLE_NAME, USER_NAME, PASSWORD, CONFIRM_PASSWORD, ENABLED) values ("UserImpl", "pavlidis", "Pavlidis", "Paul", "<middlename>", "pavlidis", "7478822cc546bb339c7cd198ca96df2b7a50cf5c", "7478822cc546bb339c7cd198ca96df2b7a50cf5c", 1);
insert into USER_ROLE (NAME, USER_NAME) values ("user", "pavlidis");

--Anshu Sinha
insert into CONTACT (CLASS, NAME, LAST_NAME, FIRST_NAME, MIDDLE_NAME, USER_NAME, PASSWORD, CONFIRM_PASSWORD, ENABLED) values ("UserImpl", "sinha", "Sinha", "Anshu", "<middlename>", "sinha", "7478822cc546bb339c7cd198ca96df2b7a50cf5c", "7478822cc546bb339c7cd198ca96df2b7a50cf5c", 1);
insert into USER_ROLE (NAME, USER_NAME) values ("user", "sinha");

--Kiran Keshav
insert into CONTACT (CLASS, NAME, LAST_NAME, FIRST_NAME, MIDDLE_NAME, USER_NAME, PASSWORD, CONFIRM_PASSWORD, ENABLED) values ("UserImpl", "keshav", "Keshav", "Kiran", "Dattatri", "keshav", "7478822cc546bb339c7cd198ca96df2b7a50cf5c", "7478822cc546bb339c7cd198ca96df2b7a50cf5c", 1);
insert into USER_ROLE (NAME, USER_NAME) values ("user", "keshav");


insert into TAXON (SCIENTIFIC_NAME,COMMON_NAME,NCBI_ID) values ("Homo sapiens","human","9606");
insert into TAXON (SCIENTIFIC_NAME,COMMON_NAME,NCBI_ID) values ("Mus musculus","mouse","10090"); 
insert into TAXON (SCIENTIFIC_NAME,COMMON_NAME,NCBI_ID) values ("rattus","rat","10114");

insert into EXTERNAL_DATABASE (NAME, DESCRIPTION, WEB_URI, FTP_URI) values ("PubMed", "PubMed database from NCBI", "http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?db=PubMed", "ftp://ftp.ncbi.nlm.nih.gov/pubmed/");
insert into EXTERNAL_DATABASE (NAME, DESCRIPTION,  WEB_URI, FTP_URI) values ("GO", "Gene Ontology database", "http://www.godatabase.org/dev/database/", "http://archive.godatabase.org");
