alter table GEEQ drop foreign key FK_1LAST_RUN_EVENT;
alter table GEEQ drop column LAST_RUN_EVENT_FK;

alter table GEEQ drop foreign key FK_2LAST_MANUAL_OVERRIDE_EVENT;
alter table GEEQ drop column LAST_MANUAL_OVERRIDE_EVENT_FK;

alter table GEEQ drop foreign key FK_3LAST_BATCH_EFFECT_CHANGE_EVENT;
alter table GEEQ drop column LAST_BATCH_EFFECT_CHANGE_EVENT_FK;

alter table GEEQ drop foreign key FK_4LAST_BATCH_CONFOUND_CHANGE_EVENT;
alter table GEEQ drop column LAST_BATCH_CONFOUND_CHANGE_EVENT_FK;
