alter table EXPERIMENTAL_FACTOR
    add column IS_AUTO_GENERATED tinyint not null default false;
update EXPERIMENTAL_FACTOR
    join CHARACTERISTIC C on C.ID = EXPERIMENTAL_FACTOR.CATEGORY_FK = C.ID
set IS_AUTO_GENERATED = true
where C.CATEGORY = 'batch'
   or C.CATEGORY = 'cell type';