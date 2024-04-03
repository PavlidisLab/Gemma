-- Slim version of init-data.sql for unit tests

insert into AUDIT_TRAIL (ID)
values (1),
       (2);

insert into USER_GROUP (ID, AUDIT_TRAIL_FK, NAME, DESCRIPTION)
values (1, 1, 'Administrators', NULL),
       (2, 2, 'Users', NULL);

insert into CONTACT (ID, class, NAME, DESCRIPTION, EMAIL, LAST_NAME, USER_NAME, PASSWORD, PASSWORD_HINT, ENABLED,
                     SIGNUP_TOKEN, SIGNUP_TOKEN_DATESTAMP)
values (1, 'User', 'admin', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);