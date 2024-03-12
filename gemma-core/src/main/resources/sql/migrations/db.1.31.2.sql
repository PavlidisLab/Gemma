-- drop the LEVEL from the primary key
alter table EXPRESSION_EXPERIMENT2CHARACTERISTIC
    drop primary key,
    add primary key (ID, EXPRESSION_EXPERIMENT_FK);