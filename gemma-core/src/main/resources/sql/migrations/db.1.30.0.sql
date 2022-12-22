-- remove empty value URIs as those need to be treated nicely with the new index
update CHARACTERISTIC
set VALUE_URI = NULL
where VALUE_URI = '';
-- add an index on both VALUE_URI and VALUE (the valueUri index is redundant)
alter table CHARACTERISTIC
    add index VALUE_URI_VALUE (VALUE_URI, VALUE),
    drop index valueUri;
-- apply the same transformation to the CHARACTERISTIC_URI and CHARACTERISTIC
alter table CHARACTERISTIC
    add index CATEGORY_URI_CATEGORY (CATEGORY_URI, CATEGORY),
    drop index categoryUri;
update CHARACTERISTIC
set CATEGORY_URI = null
where CATEGORY_URI = '';