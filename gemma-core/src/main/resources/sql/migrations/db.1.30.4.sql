alter table CHARACTERISTIC
    add column class VARCHAR(255) after ID;
alter table CHARACTERISTIC
    add index class (class);