insert into contact (CLASS, LAST_NAME, FIRST_NAME, MIDDLE_NAME) values ("PersonImpl", "nobody", "nobody", "nobody");
insert into contact (CLASS, EMAIL) values ("ContactImpl", "admin@gemma.org");    
insert into contact (CLASS, USER_NAME, PASSWORD, CONFIRM_PASSWORD) values ("UserImpl", "administrator", "cec6877e42b179b133c2a6c1285488755aefc071", "cec6877e42b179b133c2a6c1285488755aefc071");
insert into user_role (NAME, USER_NAME) values ("admin", "administrator");

insert into taxon (SCIENTIFIC_NAME,COMMON_NAME,NCBI_ID) values ("homo sapien","human","9606");
insert into taxon (SCIENTIFIC_NAME,COMMON_NAME,NCBI_ID) values ("mus musculus","mouse","10090"); 
insert into taxon (SCIENTIFIC_NAME,COMMON_NAME,NCBI_ID) values ("rattus","rat","10114");