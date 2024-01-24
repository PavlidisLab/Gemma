-- add permission masks for all common group SIDs
alter table EXPRESSION_EXPERIMENT2CHARACTERISTIC
    add column ACL_IS_AUTHENTICATED_ANONYMOUSLY_MASK INTEGER not null default 0;