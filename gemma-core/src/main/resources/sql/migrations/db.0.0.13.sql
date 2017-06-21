-- updates for Gemma 1.11

UPDATE CONTACT SET `class` = 'User' WHERE class='UserImpl';
UPDATE CONTACT SET `class` = 'Person' WHERE class='PersonImpl';
UPDATE CONTACT SET `class` = 'Contact' WHERE class='ContactImpl';

-- same changes should have been also made on the ACLOBJECTIDENTITY table - rectified in db.0.0.14
