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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.common.quantitationtype.QuantitationTypeService;
import ubic.gemma.persistence.service.expression.bioAssayData.BioAssayDimensionService;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Lightweight unit test for {@link ExpressionExperimentDataVectorServiceImpl} that verifies
 * delegation to the {@link ExpressionExperimentDao} and the create-if-needed orchestration of
 * {@link BioAssayDimensionService} and {@link QuantitationTypeService}. Mirrors
 * {@link ExpressionExperimentSubSetReadServiceTest} in shape.
 */
@ExtendWith(MockitoExtension.class)
public class ExpressionExperimentDataVectorServiceTest {

    @Mock
    private ExpressionExperimentDao dao;

    @Mock
    private BioAssayDimensionService bioAssayDimensionService;

    @Mock
    private QuantitationTypeService quantitationTypeService;

    private ExpressionExperimentDataVectorServiceImpl service;

    @BeforeEach
    public void setUp() {
        service = new ExpressionExperimentDataVectorServiceImpl( dao, bioAssayDimensionService, quantitationTypeService );
    }

    @Test
    public void testReplaceAllRawDataVectorsCreatesTransientCollaborators() {
        ExpressionExperiment ee = new ExpressionExperiment();
        ee.setId( 1L );
        QuantitationType qt = new QuantitationType();
        qt.setIsPreferred( true );
        BioAssayDimension bad = new BioAssayDimension();
        bad.setBioAssays( Collections.singletonList( new BioAssay() ) );
        ArrayDesign ad = new ArrayDesign();
        when( bioAssayDimensionService.findOrCreate( bad ) ).thenReturn( bad );
        when( quantitationTypeService.create( qt, RawExpressionDataVector.class ) ).thenReturn( qt );

        Set<RawExpressionDataVector> vectors = createRawVectors( ee, qt, bad, ad );
        service.replaceAllRawDataVectors( ee, vectors );

        verify( bioAssayDimensionService ).findOrCreate( bad );
        verify( quantitationTypeService ).create( qt, RawExpressionDataVector.class );
        verify( dao ).addRawDataVectors( ee, qt, vectors );
    }

    @Test
    public void testReplaceAllRawDataVectorsRejectsMultiplePreferredQts() {
        ExpressionExperiment ee = new ExpressionExperiment();
        ee.setId( 1L );
        QuantitationType qt1 = new QuantitationType();
        qt1.setName( "qt1" );
        qt1.setIsPreferred( true );
        QuantitationType qt2 = new QuantitationType();
        qt2.setName( "qt2" );
        qt2.setIsPreferred( true );
        BioAssayDimension bad = new BioAssayDimension();
        ArrayDesign ad = new ArrayDesign();
        Set<RawExpressionDataVector> vectors = new HashSet<>();
        vectors.addAll( createRawVectors( ee, qt1, bad, ad ) );
        vectors.addAll( createRawVectors( ee, qt2, bad, ad ) );

        assertThatThrownBy( () -> service.replaceAllRawDataVectors( ee, vectors ) )
                .isInstanceOf( IllegalArgumentException.class );
        verifyNoInteractions( dao );
    }

    @Test
    public void testReplaceAllRawDataVectorsRejectsEmptyInput() {
        ExpressionExperiment ee = new ExpressionExperiment();
        ee.setId( 1L );
        assertThatThrownBy( () -> service.replaceAllRawDataVectors( ee, Collections.emptySet() ) )
                .isInstanceOf( UnsupportedOperationException.class );
        verifyNoInteractions( dao );
    }

    @Test
    public void testGetRawDataVectorsDelegates() {
        ExpressionExperiment ee = new ExpressionExperiment();
        QuantitationType qt = new QuantitationType();
        service.getRawDataVectors( ee, qt );
        verify( dao ).getRawDataVectors( ee, qt );
    }

    @Test
    public void testGetPreferredRawDataVectorsDelegates() {
        ExpressionExperiment ee = new ExpressionExperiment();
        service.getPreferredRawDataVectors( ee );
        verify( dao ).getPreferredRawDataVectors( ee );
    }

    @Test
    public void testRemoveAllRawDataVectorsDelegates() {
        ExpressionExperiment ee = new ExpressionExperiment();
        service.removeAllRawDataVectors( ee );
        verify( dao ).removeAllRawDataVectors( ee );
    }

    @Test
    public void testRemoveRawDataVectorsTwoArgDefaultsKeepDimensionFalse() {
        ExpressionExperiment ee = new ExpressionExperiment();
        QuantitationType qt = new QuantitationType();
        service.removeRawDataVectors( ee, qt );
        verify( dao ).removeRawDataVectors( ee, qt, false );
    }

    @Test
    public void testRemoveProcessedDataVectorsDelegates() {
        ExpressionExperiment ee = new ExpressionExperiment();
        service.removeProcessedDataVectors( ee );
        verify( dao ).removeProcessedDataVectors( ee );
    }

    private Set<RawExpressionDataVector> createRawVectors( ExpressionExperiment ee, QuantitationType qt, BioAssayDimension bad, ArrayDesign ad ) {
        Set<RawExpressionDataVector> vectors = new HashSet<>();
        for ( int i = 0; i < 10; i++ ) {
            RawExpressionDataVector v = new RawExpressionDataVector();
            v.setExpressionExperiment( ee );
            v.setQuantitationType( qt );
            v.setBioAssayDimension( bad );
            CompositeSequence cs = new CompositeSequence();
            cs.setName( "cs" + i );
            cs.setArrayDesign( ad );
            v.setDesignElement( cs );
            vectors.add( v );
        }
        return vectors;
    }
}
