insert into CONTACT (CLASS, NAME, LAST_NAME, FIRST_NAME, MIDDLE_NAME) values ("PersonImpl", "person", "nobody", "nobody", "nobody");
insert into CONTACT (CLASS, NAME, USER_NAME, PASSWORD, CONFIRM_PASSWORD, ENABLED) values ("UserImpl", "user", "administrator", "cec6877e42b179b133c2a6c1285488755aefc071", "cec6877e42b179b133c2a6c1285488755aefc071", 1);
insert into CONTACT (CLASS, NAME, EMAIL) values ("edu.columbia.gemma.common.auditAndSecurity.ContactImpl", "contact", "admin@gemma.org");
-- insert into CONTACT (NAME, USER_NAME) values ("admin", "administrator");

insert into TAXON (SCIENTIFIC_NAME,COMMON_NAME,NCBI_ID) values ("homo sapien","human","9606");
insert into TAXON (SCIENTIFIC_NAME,COMMON_NAME,NCBI_ID) values ("mus musculus","mouse","10090"); 
insert into TAXON (SCIENTIFIC_NAME,COMMON_NAME,NCBI_ID) values ("rattus","rat","10114");

insert into EXTERNAL_DATABASE (NAME, DESCRIPTION, WEB_URI, FTP_URI) values ("PubMed", "PubMed database from NCBI", "http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?db=PubMed", "ftp://ftp.ncbi.nlm.nih.gov/pubmed/");
insert into EXTERNAL_DATABASE (NAME, DESCRIPTION,  WEB_URI, FTP_URI) values ("GO", "Gene Ontology database", "http://www.godatabase.org/dev/database/", "http://archive.godatabase.org");
