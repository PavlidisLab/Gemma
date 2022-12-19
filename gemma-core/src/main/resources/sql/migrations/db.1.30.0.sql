alter table CHARACTERISTIC
    add index VALUE_URI_VALUE (VALUE_URI, VALUE_URI);
-- redundant since CHARACTERISTIC_VALUE_URI_VALUE_IX is a prefix
alter table CHARACTERISTIC
    drop index valueUri;
-- remove empty value URIs as those need to be treated nicely with the new index
update CHARACTERISTIC
set VALUE_URI = NULL
where VALUE_URI = '';