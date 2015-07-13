create table GEEQ (ID INT unsigned not null auto_increment, SUITABILITY DOUBLE, QUALITY DOUBLE, primary key (ID));
alter table INVESTIGATION add column GEEQ_FK INT unsigned UNIQUE;
alter table INVESTIGATION add index GEEQINX (GEEQ_FK), add constraint GEEQINXF foreign key (GEEQ_FK) references GEEQ (ID);
