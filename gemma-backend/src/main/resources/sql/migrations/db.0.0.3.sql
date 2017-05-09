-- remove unused field

alter table BIO_MATERIAL drop foreign key BIO_MATERIAL_MATERIAL_TYPE_FKC;
alter table BIO_MATERIAL drop column MATERIAL_TYPE_FK;
