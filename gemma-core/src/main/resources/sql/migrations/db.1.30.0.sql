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

insert into EXPRESSION_EXPERIMENT2CHARACTERISTIC
select C.ID, C.NAME, C.DESCRIPTION, C.CATEGORY, C.CATEGORY_URI, C.VALUE, C.VALUE_URI, C.ORIGINAL_VALUE, C.EVIDENCE_CODE, I.ID, 'ubic.gemma.model.expression.experiment.ExpressionExperiment' as LEVEL
from INVESTIGATION I
         join CHARACTERISTIC C on I.ID = C.INVESTIGATION_FK
where I.class = 'ExpressionExperiment'
union
select C.ID, C.NAME, C.DESCRIPTION, C.CATEGORY, C.CATEGORY_URI, C.VALUE, C.VALUE_URI, C.ORIGINAL_VALUE, C.EVIDENCE_CODE, I.ID, 'ubic.gemma.model.expression.biomaterial.BioMaterial' as LEVEL
from INVESTIGATION I
         join BIO_ASSAY BA on I.ID = BA.EXPRESSION_EXPERIMENT_FK
         join BIO_MATERIAL BM on BA.SAMPLE_USED_FK = BM.ID
         join BIO_MATERIAL_FACTOR_VALUES BMFV on BM.ID = BMFV.BIO_MATERIALS_FK
         join FACTOR_VALUE FV on BMFV.FACTOR_VALUES_FK = FV.ID
         join CHARACTERISTIC C on FV.ID = C.FACTOR_VALUE_FK
where I.class = 'ExpressionExperiment'
union
select C.ID, C.NAME, C.DESCRIPTION, C.CATEGORY, C.CATEGORY_URI, C.VALUE, C.VALUE_URI, C.ORIGINAL_VALUE, C.EVIDENCE_CODE, I.ID, 'ubic.gemma.model.expression.experiment.ExperimentalDesign' as LEVEL
from INVESTIGATION I
         join EXPERIMENTAL_DESIGN
              on I.EXPERIMENTAL_DESIGN_FK = EXPERIMENTAL_DESIGN.ID
         join EXPERIMENTAL_FACTOR EF
              on EXPERIMENTAL_DESIGN.ID = EF.EXPERIMENTAL_DESIGN_FK
         join FACTOR_VALUE FV on FV.EXPERIMENTAL_FACTOR_FK = EF.ID
         join CHARACTERISTIC C on FV.ID = C.FACTOR_VALUE_FK
where I.class = 'ExpressionExperiment'