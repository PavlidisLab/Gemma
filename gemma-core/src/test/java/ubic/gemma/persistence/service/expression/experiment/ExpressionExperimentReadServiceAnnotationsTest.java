/*
 * The Gemma project.
 *
 * Copyright (c) 2026 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 */
package ubic.gemma.persistence.service.expression.experiment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ubic.gemma.model.common.description.AnnotationValueObject;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.expression.experiment.Statement;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.Mockito.when;

/**
 * De-duplication contract of {@link ExpressionExperimentReadServiceImpl#getAnnotations(ExpressionExperiment, boolean)}.
 * <p>
 * The three sources it aggregates (experiment tags, factor-value statements, sample characteristics) overlap, so the
 * aggregate is de-duplicated. The key is the (category, term name) pair; the two shapes below pin what that must and
 * must not collapse. Both were measured on production first, so neither is hypothetical.
 */
@ExtendWith(MockitoExtension.class)
public class ExpressionExperimentReadServiceAnnotationsTest {

    private static final String CELL_TYPE_URI = "http://www.ebi.ac.uk/efo/EFO_0000324";
    private static final String ORGANISM_PART_URI = "http://www.ebi.ac.uk/efo/EFO_0000635";
    private static final String GENOTYPE_URI = "http://www.ebi.ac.uk/efo/EFO_0000513";
    private static final String CL_1001610 = "http://purl.obolibrary.org/obo/CL_1001610";
    private static final String EFO_0005168 = "http://www.ebi.ac.uk/efo/EFO_0005168";
    private static final String HAS_BACKGROUND_URI = "http://gemma.msl.ubc.ca/ont/TGEMO_00216";
    private static final String TGEMO_00174 = "http://gemma.msl.ubc.ca/ont/TGEMO_00174";

    @Mock
    private ExpressionExperimentDao expressionExperimentDao;

    @InjectMocks
    private ExpressionExperimentReadServiceImpl service;

    private ExpressionExperiment ee;

    @BeforeEach
    public void setUp() {
        ee = ExpressionExperiment.Factory.newInstance();
        ee.setId( 1L );
    }

    /**
     * Two experiment tags on the same term under different categories are two distinct curation claims, and both must
     * be returned. Measured on production: experiment 5402 stores {@code cell type} and {@code organism part} rows that
     * both point at {@code CL_1001610}, and the route returned only the first.
     */
    @Test
    public void getAnnotations_sameTermUnderTwoCategories_returnsBoth() {
        when( expressionExperimentDao.getExperimentAnnotations( ee, false ) ).thenReturn( Arrays.asList(
                tag( "cell type", CELL_TYPE_URI, "bone marrow hematopoietic cell", CL_1001610 ),
                tag( "organism part", ORGANISM_PART_URI, "bone marrow hematopoietic cell", CL_1001610 ) ) );

        Set<AnnotationValueObject> annotations = service.getAnnotations( ee, false );

        assertThat( annotations )
                .extracting( AnnotationValueObject::getClassName, AnnotationValueObject::getTermName )
                .containsExactlyInAnyOrder(
                        tuple( "cell type", "bone marrow hematopoietic cell" ),
                        tuple( "organism part", "bone marrow hematopoietic cell" ) );
    }

    /**
     * Exact duplicates — same category and same term — still collapse. That is what the aggregation of three
     * overlapping sources is for, and it is the behaviour the category-aware key must not lose.
     */
    @Test
    public void getAnnotations_sameTermUnderSameCategory_collapses() {
        when( expressionExperimentDao.getExperimentAnnotations( ee, false ) ).thenReturn( Arrays.asList(
                tag( "cell type", CELL_TYPE_URI, "bone marrow hematopoietic cell", CL_1001610 ),
                tag( "cell type", CELL_TYPE_URI, "Bone Marrow  Hematopoietic Cell", CL_1001610 ) ) );

        assertThat( service.getAnnotations( ee, false ) ).hasSize( 1 );
    }

    /**
     * Regression guard for the negative control measured on experiment 27103: two factor-value statements that share
     * a term URI <em>and</em> a category, differing only in their predicate/object, both come back. They survive
     * because the factor-value branch overrides {@code termName} with the formatted statement, so the two labels
     * differ — adding the category to the key must not start collapsing them.
     */
    @Test
    public void getAnnotations_sameTermAndCategoryDifferingStatement_returnsBoth() {
        Statement bare = statement( "wild type genotype", EFO_0005168, null, null, null );
        Statement decorated = statement( "wild type genotype", EFO_0005168, "has background", HAS_BACKGROUND_URI, "APP/PS1" );
        when( expressionExperimentDao.getFactorValueAnnotationsWithParents( ee ) ).thenReturn( Arrays.asList(
                factorValueRow( bare ),
                factorValueRow( decorated ) ) );

        Set<AnnotationValueObject> annotations = service.getAnnotations( ee, false );

        assertThat( annotations )
                .allSatisfy( a -> assertThat( a.getClassName() ).isEqualTo( "genotype" ) )
                .allSatisfy( a -> assertThat( a.getTermUri() ).isEqualTo( EFO_0005168 ) )
                .extracting( AnnotationValueObject::getTermName )
                .containsExactlyInAnyOrder( "wild type genotype", "wild type genotype has background APP/PS1" );
    }

    /**
     * The same tag reaching the aggregate from two different levels still collapses to one entry —
     * {@code objectClass} is not part of the de-duplication key.
     */
    @Test
    public void getAnnotations_sameTagAtExperimentAndSampleLevel_collapses() {
        when( expressionExperimentDao.getExperimentAnnotations( ee, false ) ).thenReturn( Collections.singletonList(
                tag( "organism part", ORGANISM_PART_URI, "bone marrow", "http://purl.obolibrary.org/obo/UBERON_0002371" ) ) );
        when( expressionExperimentDao.getBioMaterialAnnotations( ee, false ) ).thenReturn( Collections.singletonList(
                tag( "organism part", ORGANISM_PART_URI, "bone marrow", "http://purl.obolibrary.org/obo/UBERON_0002371" ) ) );

        Set<AnnotationValueObject> annotations = service.getAnnotations( ee, false );

        assertThat( annotations ).hasSize( 1 );
        assertThat( annotations ).extracting( AnnotationValueObject::getObjectClass )
                .containsExactly( "ExperimentTag" );
    }

    private static Characteristic tag( String category, String categoryUri, String value, String valueUri ) {
        Characteristic c = Characteristic.Factory.newInstance();
        c.setCategory( category );
        c.setCategoryUri( categoryUri );
        c.setValue( value );
        c.setValueUri( valueUri );
        return c;
    }

    private static Statement statement( String subject, String subjectUri, String predicate, String predicateUri, String object ) {
        Statement s = Statement.Factory.newInstance( "genotype", GENOTYPE_URI, subject, subjectUri );
        if ( predicate != null ) {
            s.setPredicate( predicate );
            s.setPredicateUri( predicateUri );
            s.setObject( object );
            s.setObjectUri( TGEMO_00174 );
        }
        return s;
    }

    private static Object[] factorValueRow( Statement s ) {
        ExperimentalFactor ef = ExperimentalFactor.Factory.newInstance();
        ef.setName( "genotype" );
        FactorValue fv = FactorValue.Factory.newInstance( ef );
        fv.getCharacteristics().add( s );
        return new Object[] { s, fv, ef };
    }

}
