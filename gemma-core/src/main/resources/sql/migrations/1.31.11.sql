alter table CHARACTERISTIC
    add index CHARACTERISTIC_PREDICATE (PREDICATE),
    add index CHARACTERISTIC_OBJECT (OBJECT),
    add index CHARACTERISTIC_SECOND_PREDICATE (SECOND_PREDICATE),
    add index CHARACTERISTIC_SECOND_OBJECT (SECOND_OBJECT);