alter table EXPERIMENTAL_FACTOR
    add column IS_AUTO_GENERATED tinyint not null default false;
update EXPERIMENTAL_FACTOR
set IS_AUTO_GENERATED = true
where CATEGORY = 'batch'
   or CATEGORY = 'cell type';