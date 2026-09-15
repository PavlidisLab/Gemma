-- BIO_ASSAY gains what was extracted and how the library was made.
--
-- Gemma stored NO library metadata: BIO_ASSAY carried SEQUENCE_READ_COUNT / _LENGTH /
-- _PAIRED_READS and nothing about what was pulled out of the sample or how it was
-- selected. GEO gives all three per sample and the importer kept none of them -- the
-- molecule became a CHARACTERISTIC 'molecular entity' on the BIOMATERIAL, and
-- library_selection and library_strategy were dropped (cab, 2026-09-05, checked against
-- INFORMATION_SCHEMA on gemd).
--
-- The biomaterial was the wrong home. Paul: "the biomaterial is the cells/tissue we got
-- the RNA from, not the RNA", and "the assay is 'we took that sample and did something to
-- it to get expression measurements'". The extraction is part of that doing.
--
-- It is also structural, not tidy: one biomaterial can yield two molecules. In CITE-seq
-- the same cells give the RNA readout and the protein one, and on prod that is real --
-- 181 genomic_DNA rows over 9 experiments, 80 protein rows over 3, all multimodal designs.
-- On the biomaterial that needs a duplicated biomaterial, which is a lie about the sample.
--
-- 🛑 EXTRACTED_MOLECULE is not a substitute for LIBRARY_SELECTION and neither is the other.
-- Paul, 2026-08-31: "total RNA ... is potentially misleading because there's often still a
-- poly-A selection step". The molecule says what went in, the selection says what was kept,
-- and they disagree routinely.
--
-- All three are NULL for existing rows and for anything not from GEO. Nothing is backfilled
-- here: the biomaterial characteristics that carry this today are cab's to migrate, and a
-- guess written now would be indistinguishable from an imported fact later.

ALTER TABLE BIO_ASSAY
    ADD COLUMN EXTRACTED_MOLECULE VARCHAR(32) NULL,
    ADD COLUMN LIBRARY_SELECTION  VARCHAR(255) NULL,
    ADD COLUMN LIBRARY_STRATEGY   VARCHAR(255) NULL;
