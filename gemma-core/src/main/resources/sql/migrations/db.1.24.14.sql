-- starting to get rid of Compound

alter table COMPOUND drop column COMPOUND_INDICES_FK;
alter table COMPOUND drop column EXTERNAL_L_I_M_S_FK;
alter table COMPOUND drop column IS_SOLVENT;
