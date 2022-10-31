-- make external database auditable
alter table EXTERNAL_DATABASE
    add column AUDIT_TRAIL_FK BIGINT UNIQUE references AUDIT_TRAIL (ID);

start transaction;
-- insert one audit trail for each existing external database
insert into AUDIT_TRAIL
select NULL
from EXTERNAL_DATABASE
where AUDIT_TRAIL_FK is NULL;

-- update FKs relative to the last insert ID (which is actually the first insert ID in the above query)
-- offset has to start at -1 because it will be zero after the first increment
SET @FIRST_AUDIT_TRAIL_ID = last_insert_id();
SET @OFFSET = -1;
update EXTERNAL_DATABASE
set EXTERNAL_DATABASE.AUDIT_TRAIL_FK = @FIRST_AUDIT_TRAIL_ID + (@OFFSET := @OFFSET + 1)
where EXTERNAL_DATABASE.AUDIT_TRAIL_FK is NULL;
commit;

-- make audit trail non-null now that all EDs are auditable
alter table EXTERNAL_DATABASE
    modify column AUDIT_TRAIL_FK BIGINT NOT NULL;

-- add columns for the Versioned interface
alter table EXTERNAL_DATABASE
    add column RELEASE_VERSION VARCHAR(255),
    add column RELEASE_URL     VARCHAR(255),
    add column LAST_UPDATED    DATETIME;

create procedure add_external_database(in name varchar(255), in web_uri varchar(255), in ftp_uri varchar(255),
                                       in type varchar(255))
begin
    insert into AUDIT_TRAIL (ID) values (null);
    insert into EXTERNAL_DATABASE (NAME, WEB_URI, FTP_URI, TYPE, AUDIT_TRAIL_FK)
    values (name, web_uri, ftp_uri, type, last_insert_id());
end;

-- insert new db we need to track various things
start transaction;
call add_external_database('hg18 annotations', 'https://hgdownload.cse.ucsc.edu/goldenpath/hg18/database/', NULL, 'OTHER');
call add_external_database('hg19 annotations', 'https://hgdownload.cse.ucsc.edu/goldenpath/hg19/database/', NULL, 'OTHER');
call add_external_database('hg38 annotations', 'https://hgdownload.cse.ucsc.edu/goldenpath/hg38/database/', NULL, 'OTHER');
call add_external_database('mm8 annotations', 'https://hgdownload.cse.ucsc.edu/goldenpath/mm8/database/', NULL, 'OTHER');
call add_external_database('mm9 annotations', 'https://hgdownload.cse.ucsc.edu/goldenpath/mm9/database/', NULL, 'OTHER');
call add_external_database('mm10 annotations', 'https://hgdownload.cse.ucsc.edu/goldenpath/mm10/database/', NULL, 'OTHER');
call add_external_database('mm11 annotations', 'https://hgdownload.cse.ucsc.edu/goldenpath/mm11/database/', NULL, 'OTHER');
call add_external_database('mm39 annotations', 'https://hgdownload.cse.ucsc.edu/goldenpath/mm39/database/', NULL, 'OTHER');
call add_external_database('rn4 annotations', 'https://hgdownload.cse.ucsc.edu/goldenpath/rn4/database/', NULL, 'OTHER');
call add_external_database('rn6 annotations', 'https://hgdownload.cse.ucsc.edu/goldenpath/rn6/database/', NULL, 'OTHER');
call add_external_database('rn7 annotations', 'https://hgdownload.cse.ucsc.edu/goldenpath/rn7/database/', NULL, 'OTHER');
call add_external_database('hg18 sequence alignments', NULL, NULL, 'OTHER');
call add_external_database('hg19 sequence alignments', NULL, NULL, 'OTHER');
call add_external_database('hg38 sequence alignments', NULL, NULL, 'OTHER');
call add_external_database('mm8 sequence alignments', NULL, NULL, 'OTHER');
call add_external_database('mm9 sequence alignments', NULL, NULL, 'OTHER');
call add_external_database('mm10 sequence alignments', NULL, NULL, 'OTHER');
call add_external_database('mm11 sequence alignments', NULL, NULL, 'OTHER');
call add_external_database('mm39 sequence alignments', NULL, NULL, 'OTHER');
call add_external_database('rn4 sequence alignments', NULL, NULL, 'OTHER');
call add_external_database('rn6 sequence alignments', NULL, NULL, 'OTHER');
call add_external_database('rn7 sequence alignments', NULL, NULL, 'OTHER');
call add_external_database('hg37 RNA-Seq annotations', NULL, NULL, 'OTHER');
call add_external_database('mm10 RNA-Seq annotations', 'https://www.ncbi.nlm.nih.gov/genome/annotation_euk/Mus_musculus/108/', NULL, 'OTHER');
call add_external_database('rn6 RNA-Seq annotations', 'https://www.ncbi.nlm.nih.gov/genome/annotation_euk/Rattus_norvegicus/106/', NULL, 'OTHER');
call add_external_database('gene', NULL, 'https://ftp.ncbi.nih.gov/gene/DATA/gene_info.gz', 'OTHER');
call add_external_database('go', NULL, 'https://ftp.ncbi.nih.gov/gene/DATA/gene2go.gz', 'OTHER');
call add_external_database('multifunctionality', NULL, NULL, 'OTHER');
call add_external_database('gene2cs', NULL, NULL, 'OTHER');
commit;

drop procedure add_external_database;

alter table EXTERNAL_DATABASE
    modify column name varchar(255) not null unique;