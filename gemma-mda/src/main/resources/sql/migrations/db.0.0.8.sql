-- changes for new coexpression setup.

drop table if exists HUMAN_GENE_COEXPRESSION;
drop table if exists MOUSE_GENE_COEXPRESSION;
drop table if exists RAT_GENE_COEXPRESSION;
drop table if exists OTHER_GENE_COEXPRESSION;

create table HUMAN_GENE_COEXPRESSION (ID BIGINT not null, POSITIVE TINYINT, SUPPORT INTEGER, FIRST_GENE_FK BIGINT not null, SECOND_GENE_FK BIGINT not null, SUPPORT_DETAILS_FK BIGINT, primary key (ID));
create table MOUSE_GENE_COEXPRESSION (ID BIGINT not null, POSITIVE TINYINT, SUPPORT INTEGER, FIRST_GENE_FK BIGINT not null, SECOND_GENE_FK BIGINT not null, SUPPORT_DETAILS_FK BIGINT, primary key (ID));
create table RAT_GENE_COEXPRESSION (ID BIGINT not null, POSITIVE TINYINT, SUPPORT INTEGER, FIRST_GENE_FK BIGINT not null, SECOND_GENE_FK BIGINT not null, SUPPORT_DETAILS_FK BIGINT, primary key (ID));
create table OTHER_GENE_COEXPRESSION (ID BIGINT not null, POSITIVE TINYINT, SUPPORT INTEGER, FIRST_GENE_FK BIGINT not null, SECOND_GENE_FK BIGINT not null, SUPPORT_DETAILS_FK BIGINT, primary key (ID));

-- we don't really need the second gene constraint, and it adds an index.
alter table HUMAN_GENE_COEXPRESSION add constraint FKF9E6557F90452155 foreign key (FIRST_GENE_FK) references CHROMOSOME_FEATURE (ID);
alter table HUMAN_GENE_COEXPRESSION add index FKF9E6557FC02BF5B4 (SUPPORT_DETAILS_FK), add constraint FKF9E6557FC02BF5B4 foreign key (SUPPORT_DETAILS_FK) references HUMAN_LINK_SUPPORT_DETAILS (ID);
alter table MOUSE_GENE_COEXPRESSION add constraint FKFC61C4F790452155 foreign key (FIRST_GENE_FK) references CHROMOSOME_FEATURE (ID);
alter table MOUSE_GENE_COEXPRESSION add index FKFC61C4F7C02BF5B4 (SUPPORT_DETAILS_FK), add constraint FKFC61C4F7C02BF5B4 foreign key (SUPPORT_DETAILS_FK) references MOUSE_LINK_SUPPORT_DETAILS (ID);
alter table RAT_GENE_COEXPRESSION add constraint FKDE59FC7790452155 foreign key (FIRST_GENE_FK) references CHROMOSOME_FEATURE (ID);
alter table RAT_GENE_COEXPRESSION add index FKDE59FC77C02BF5B4 (SUPPORT_DETAILS_FK), add constraint FKDE59FC77C02BF5B4 foreign key (SUPPORT_DETAILS_FK) references RAT_LINK_SUPPORT_DETAILS (ID);
alter table OTHER_GENE_COEXPRESSION  add constraint FK74B9A3E290452155 foreign key (FIRST_GENE_FK) references CHROMOSOME_FEATURE (ID);
alter table OTHER_GENE_COEXPRESSION add index FK74B9A3E2C02BF5B4 (SUPPORT_DETAILS_FK), add constraint FK74B9A3E2C02BF5B4 foreign key (SUPPORT_DETAILS_FK) references OTHER_LINK_SUPPORT_DETAILS (ID);

-- "MySQL requires that foreign key columns be indexed" we still get the SECOND_GENE_FK index even though didn't add it explicitly.
-- http://dev.mysql.com/doc/refman/5.0/en/constraint-foreign-key.html
alter table HUMAN_GENE_COEXPRESSION add index hfgsg (FIRST_GENE_FK,SECOND_GENE_FK);
alter table MOUSE_GENE_COEXPRESSION add index mfgsg (FIRST_GENE_FK,SECOND_GENE_FK);
alter table RAT_GENE_COEXPRESSION add index rfgsg (FIRST_GENE_FK,SECOND_GENE_FK);
alter table OTHER_GENE_COEXPRESSION add index ofgsg (FIRST_GENE_FK,SECOND_GENE_FK);

drop table if exists HUMAN_EXPERIMENT_COEXPRESSION;
drop table if exists MOUSE_EXPERIMENT_COEXPRESSION;
drop table if exists RAT_EXPERIMENT_COEXPRESSION;
drop table if exists OTHER_EXPERIMENT_COEXPRESSION;

create table HUMAN_EXPERIMENT_COEXPRESSION (ID BIGINT not null, EXPERIMENT_FK BIGINT not null, LINK_FK BIGINT not null, GENE1_FK BIGINT not null, GENE2_FK BIGINT not null, primary key (ID));
create table MOUSE_EXPERIMENT_COEXPRESSION (ID BIGINT not null, EXPERIMENT_FK BIGINT not null, LINK_FK BIGINT not null, GENE1_FK BIGINT not null, GENE2_FK BIGINT not null, primary key (ID));
create table RAT_EXPERIMENT_COEXPRESSION (ID BIGINT not null, EXPERIMENT_FK BIGINT not null, LINK_FK BIGINT not null, GENE1_FK BIGINT not null, GENE2_FK BIGINT not null, primary key (ID));
create table OTHER_EXPERIMENT_COEXPRESSION (ID BIGINT not null, EXPERIMENT_FK BIGINT not null, LINK_FK BIGINT not null, GENE1_FK BIGINT not null, GENE2_FK BIGINT not null, primary key (ID));

alter table HUMAN_EXPERIMENT_COEXPRESSION add index ECL1EFK (EXPERIMENT_FK, GENE1_FK, GENE2_FK), add constraint ECL1EFK foreign key (EXPERIMENT_FK) references INVESTIGATION (ID);
alter table MOUSE_EXPERIMENT_COEXPRESSION add index ECL2EFK (EXPERIMENT_FK, GENE1_FK, GENE2_FK), add constraint ECL2EFK foreign key (EXPERIMENT_FK) references INVESTIGATION (ID);
alter table RAT_EXPERIMENT_COEXPRESSION add index ECL3EFK (EXPERIMENT_FK, GENE1_FK, GENE2_FK), add constraint ECL3EFK foreign key (EXPERIMENT_FK) references INVESTIGATION (ID);
alter table OTHER_EXPERIMENT_COEXPRESSION add index ECL4EFK (EXPERIMENT_FK, GENE1_FK, GENE2_FK), add constraint ECL4EFK foreign key (EXPERIMENT_FK) references INVESTIGATION (ID);

set foreign_key_checks=0;
drop table if exists HUMAN_LINK_SUPPORT_DETAILS;
drop table if exists MOUSE_LINK_SUPPORT_DETAILS;
drop table if exists RAT_LINK_SUPPORT_DETAILS;
drop table if exists OTHER_LINK_SUPPORT_DETAILS;

create table HUMAN_LINK_SUPPORT_DETAILS (ID BIGINT not null, BYTES MEDIUMBLOB not null, primary key (ID));
create table MOUSE_LINK_SUPPORT_DETAILS (ID BIGINT not null, BYTES MEDIUMBLOB not null, primary key (ID));
create table RAT_LINK_SUPPORT_DETAILS (ID BIGINT not null, BYTES MEDIUMBLOB not null, primary key (ID));
create table OTHER_LINK_SUPPORT_DETAILS (ID BIGINT not null, BYTES MEDIUMBLOB not null, primary key (ID));
set foreign_key_checks=1;

drop table if exists GENE_COEX_TESTED_IN;
create table GENE_COEX_TESTED_IN (ID BIGINT not null, NUM_TESTS SMALLINT not null, BYTES MEDIUMBLOB not null, primary key (ID));

drop table if exists COEXPRESSION_NODE_DEGREE;
create table COEXPRESSION_NODE_DEGREE (GENE_ID BIGINT not null, LINK_COUNTS MEDIUMBLOB not null, REL_LINK_RANKS MEDIUMBLOB not null,primary key (GENE_ID));

drop table if exists GENE_COEX_GENES;
create table GENE_COEX_GENES (ID BIGINT not null, BYTES MEDIUMBLOB not null, primary key (ID));



-- later: remove unused data.

drop table HUMAN_PROBE_CO_EXPRESSION;
drop table MOUSE_PROBE_CO_EXPRESSION;
drop table RAT_PROBE_CO_EXPRESSION;
drop table OTHER_PROBE_CO_EXPRESSION;


--This column is not needed as we killed the mult-experiment analysis concept. (it only existed for coexpression)
alter table ANALYSIS drop column EXPRESSION_EXPERIMENT_SET_ANALYZED_FK;

-- not used
alter table ANALYSIS drop column STRINGENCY; 
alter table ANALYSIS drop column VIEWED_ANALYSIS_FK; 
alter table ANALYSIS drop column ENABLED;

-- eventually...
drop table HUMAN_GENE_CO_EXPRESSION;
drop table MOUSE_GENE_CO_EXPRESSION;
drop table RAT_GENE_CO_EXPRESSION;
drop table OTHER_GENE_CO_EXPRESSION;
drop table GENE_COEXPRESSION_NODE_DEGREE;
