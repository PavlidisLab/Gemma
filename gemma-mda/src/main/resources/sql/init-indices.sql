-- Add some indices that are not included in the generated gemma-ddl.sql. Some of these are very important to performance
-- $Id$
alter table BIO_SEQUENCE add index name (NAME);
alter table ALTERNATE_NAME add index name (NAME);
alter table INVESTIGATION add index name (NAME);
alter table INVESTIGATION add INDEX shortname (SHORT_NAME);
alter table INVESTIGATION add INDEX class (class);
alter table DATABASE_ENTRY add index acc_ex (ACCESSION, EXTERNAL_DATABASE_FK);
alter table CHROMOSOME_FEATURE add index symbol_tax (OFFICIAL_SYMBOL, TAXON_FK);
alter table CHROMOSOME_FEATURE add index ncbigeneid (NCBI_GENE_ID);
alter table CHROMOSOME_FEATURE add index ncbigi (NCBI_GI);
alter table CHROMOSOME_FEATURE add index previous_ncbiid (PREVIOUS_NCBI_ID);
alter table CHROMOSOME_FEATURE add index ensemblid (ENSEMBL_ID);
alter table CHROMOSOME_FEATURE add index name (NAME);
alter table CHROMOSOME_FEATURE add index class (class);
alter table CHROMOSOME_FEATURE add index type (TYPE);
alter table GENE_ALIAS add index `alias` (`ALIAS`);
alter table COMPOSITE_SEQUENCE add index name (NAME);
alter table PHYSICAL_LOCATION ADD INDEX BIN_KEY (BIN);
alter table AUDIT_EVENT_TYPE ADD INDEX class (class);
alter table ANALYSIS ADD INDEX class (class);
alter table CHARACTERISTIC ADD INDEX value (VALUE);
alter table CHARACTERISTIC ADD INDEX category (CATEGORY);
alter table CHARACTERISTIC ADD INDEX valueUri (VALUE_URI);
alter table CHARACTERISTIC ADD INDEX categoryUri (CATEGORY_URI);
alter table GENE_SET ADD INDEX name (NAME);
alter table PROCESSED_EXPRESSION_DATA_VECTOR ADD INDEX experimentProcessedVectorProbes (EXPRESSION_EXPERIMENT_FK,DESIGN_ELEMENT_FK);
alter table DIFFERENTIAL_EXPRESSION_ANALYSIS_RESULT ADD INDEX resultSetProbes (RESULT_SET_FK,PROBE_FK);
alter table DIFFERENTIAL_EXPRESSION_ANALYSIS_RESULT ADD INDEX probeResultSets (PROBE_FK,RESULT_SET_FK);
alter table TAXON ADD INDEX taxonncbiid (NCBI_ID);
alter table TAXON ADD INDEX taxonsecondncbiid (SECONDARY_NCBI_ID);
alter table TAXON ADD INDEX taxoncommonname (COMMON_NAME);
alter table TAXON ADD INDEX taxonscientificname (SCIENTIFIC_NAME);
alter table LOCAL_FILE ADD INDEX REMOTE_URL (REMOTE_U_R_L);
alter table CONTACT add INDEX fullname (NAME, LAST_NAME);

-- should delete the FIRST_GENE_FK and SECOND_GENE_FK indices, but they get given 'random' names. 
-- Drop the second_gene_fk constraint.
--alter table HUMAN_GENE_COEXPRESSION drop foreign key FKF9E6557F21D58F19;
--alter table MOUSE_GENE_COEXPRESSION drop foreign key FKFC61C4F721D58F19;
--alter table RAT_GENE_COEXPRESSION drop foreign key FKDE59FC7721D58F19;
--alter table OTHER_GENE_COEXPRESSION drop foreign key FK74B9A3E221D58F19;

alter table HUMAN_GENE_COEXPRESSION add index hfgsg (FIRST_GENE_FK,SECOND_GENE_FK);
alter table MOUSE_GENE_COEXPRESSION add index mfgsg (FIRST_GENE_FK,SECOND_GENE_FK);
alter table RAT_GENE_COEXPRESSION add index rfgsg (FIRST_GENE_FK,SECOND_GENE_FK);
alter table OTHER_GENE_COEXPRESSION add index ofgsg (FIRST_GENE_FK,SECOND_GENE_FK);

-- same for these, should drop the key for EXPERIMENT_FK, manually
alter table HUMAN_EXPERIMENT_COEXPRESSION add index ECL1EFK (EXPERIMENT_FK, GENE1_FK, GENE2_FK), add constraint ECL1EFK foreign key (EXPERIMENT_FK) references INVESTIGATION (ID);
alter table MOUSE_EXPERIMENT_COEXPRESSION add index ECL2EFK (EXPERIMENT_FK, GENE1_FK, GENE2_FK), add constraint ECL2EFK foreign key (EXPERIMENT_FK) references INVESTIGATION (ID);
alter table RAT_EXPERIMENT_COEXPRESSION add index ECL3EFK (EXPERIMENT_FK, GENE1_FK, GENE2_FK), add constraint ECL3EFK foreign key (EXPERIMENT_FK) references INVESTIGATION (ID);
alter table OTHER_EXPERIMENT_COEXPRESSION add index ECL4EFK (EXPERIMENT_FK, GENE1_FK, GENE2_FK), add constraint ECL4EFK foreign key (EXPERIMENT_FK) references INVESTIGATION (ID);

-- candidates for removal
alter table DIFFERENTIAL_EXPRESSION_ANALYSIS_RESULT ADD INDEX corrpvalbin (CORRECTED_P_VALUE_BIN);
alter table HIT_LIST_SIZE ADD INDEX direction (DIRECTION);
