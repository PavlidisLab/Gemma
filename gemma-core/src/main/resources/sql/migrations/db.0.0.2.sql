-- Changes for mean-variance relation storing
-- $Id$


alter table INVESTIGATION add column MEAN_VARIANCE_RELATION_FK BIGINT unique;

create table MEAN_VARIANCE_RELATION (ID BIGINT not null auto_increment, MEANS BLOB, VARIANCES BLOB, LOWESS_X BLOB, LOWESS_Y BLOB, primary key (ID));

alter table INVESTIGATION add index FKF2B9BAE23A897BBA (MEAN_VARIANCE_RELATION_FK), add constraint FKF2B9BAE23A897BBA foreign key (MEAN_VARIANCE_RELATION_FK) references MEAN_VARIANCE_RELATION (ID);
