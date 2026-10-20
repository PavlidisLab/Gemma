-- drop table if exists ACLHIBERNATESEQUENCES;
-- create table ACLHIBERNATESEQUENCES (
--     sequence_name VARCHAR(255) not null,
--     sequence_next_hi_value BIGINT not null
-- );

alter table ACLOBJECTIDENTITY add unique key acloid (OBJECT_CLASS,OBJECT_ID);
alter table ACLOBJECTIDENTITY add key objectclasskey (OBJECT_CLASS);
alter table ACLOBJECTIDENTITY add key oidkey (OBJECT_ID);
