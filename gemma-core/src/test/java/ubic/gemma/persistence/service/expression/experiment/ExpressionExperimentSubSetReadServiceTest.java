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
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.expression.experiment.FactorValueValueObject;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Lightweight unit test for {@link ExpressionExperimentSubSetReadServiceImpl} that verifies
 * each public method delegates to the right {@link ExpressionExperimentDao} call without
 * standing up a Spring context. Covers the delegation contract; deeper integration coverage
 * lives in {@code ExpressionExperimentDaoTest}.
 */
@ExtendWith(MockitoExtension.class)
public class ExpressionExperimentSubSetReadServiceTest {

    @Mock
    private ExpressionExperimentDao dao;

    @Mock
    private ExpressionExperimentSubSetDao subSetDao;

    @InjectMocks
    private ExpressionExperimentSubSetReadServiceImpl service;

    private ExpressionExperiment ee;
    private BioAssayDimension bad;

    @BeforeEach
    public void setUp() {
        ee = ExpressionExperiment.Factory.newInstance();
        ee.setId( 1L );
        bad = BioAssayDimension.Factory.newInstance();
        bad.setId( 7L );
    }

    @Test
    public void getSubSetsWithBioAssays_singleEE_delegatesToDaoGetSubSets() {
        Collection<ExpressionExperimentSubSet> expected = Collections.emptyList();
        when( dao.getSubSets( ee ) ).thenReturn( expected );

        Collection<ExpressionExperimentSubSet> actual = service.getSubSetsWithBioAssays( ee );

        assertSame( expected, actual );
        verify( dao ).getSubSets( ee );
        verifyNoMoreInteractions( dao );
    }

    @Test
    public void getSubSetsWithBioAssays_batch_delegatesToDaoGetSubSetsByExpressionExperiments() {
        Map<ExpressionExperiment, Collection<ExpressionExperimentSubSet>> expected = new HashMap<>();
        Collection<ExpressionExperiment> input = Arrays.asList( ee );
        when( dao.getSubSetsByExpressionExperiments( input ) ).thenReturn( expected );

        Map<ExpressionExperiment, Collection<ExpressionExperimentSubSet>> actual = service.getSubSetsWithBioAssays( input );

        assertSame( expected, actual );
        verify( dao ).getSubSetsByExpressionExperiments( input );
        verifyNoMoreInteractions( dao );
    }

    @Test
    public void getSubSetsByDimension_delegatesToDao() {
        Map<BioAssayDimension, Set<ExpressionExperimentSubSet>> expected = new HashMap<>();
        when( dao.getSubSetsByDimension( ee ) ).thenReturn( expected );

        Map<BioAssayDimension, Set<ExpressionExperimentSubSet>> actual = service.getSubSetsByDimension( ee );

        assertSame( expected, actual );
        verify( dao ).getSubSetsByDimension( ee );
        verifyNoMoreInteractions( dao );
    }

    @Test
    public void getSubSets_byDimension_delegatesToDao() {
        Collection<ExpressionExperimentSubSet> expected = Collections.emptyList();
        when( dao.getSubSets( ee, bad ) ).thenReturn( expected );

        Collection<ExpressionExperimentSubSet> actual = service.getSubSets( ee, bad );

        assertSame( expected, actual );
        verify( dao ).getSubSets( ee, bad );
        verifyNoMoreInteractions( dao );
    }

    @Test
    public void getSubSetByIdWithCharacteristics_nullResult_passesThroughWithoutInit() {
        when( dao.getSubSetById( ee, 99L ) ).thenReturn( null );

        ExpressionExperimentSubSet actual = service.getSubSetByIdWithCharacteristics( ee, 99L );

        assertNull( actual );
        verify( dao ).getSubSetById( ee, 99L );
        verifyNoMoreInteractions( dao );
    }

    @Test
    public void getSubSetByIdWithCharacteristics_nonNullResult_initializesCharacteristics() {
        ExpressionExperimentSubSet subSet = ExpressionExperimentSubSet.Factory.newInstance( "test-subset", ee );
        subSet.setId( 42L );
        subSet.setCharacteristics( new HashSet<>() );
        when( dao.getSubSetById( ee, 42L ) ).thenReturn( subSet );

        ExpressionExperimentSubSet actual = service.getSubSetByIdWithCharacteristics( ee, 42L );

        assertSame( subSet, actual );
        // characteristics must not be null after the call (Hibernate.initialize is a no-op on a HashSet, but the path is exercised)
        assertNotNull( actual.getCharacteristics() );
        verify( dao ).getSubSetById( ee, 42L );
        verifyNoMoreInteractions( dao );
    }

    @Test
    public void getSubSetsByFactorValue_emptySubSets_returnsEmptyMap() {
        when( dao.getSubSets( ee, bad ) ).thenReturn( Collections.emptyList() );

        Map<?, ?> actual = service.getSubSetsByFactorValue( ee, bad );

        assertEquals( 0, actual.size() );
        verify( dao ).getSubSets( ee, bad );
        verifyNoMoreInteractions( dao );
    }

    // -----------------------------------------------------------------------
    // Tests for methods migrated from ExpressionExperimentSubSetServiceImpl
    // -----------------------------------------------------------------------

    @Test
    public void loadSubSet_delegatesToSubSetDaoLoad() {
        ExpressionExperimentSubSet ss = ExpressionExperimentSubSet.Factory.newInstance( "ss", ee );
        ss.setId( 11L );
        when( subSetDao.load( 11L ) ).thenReturn( ss );

        ExpressionExperimentSubSet actual = service.loadSubSet( 11L );

        assertSame( ss, actual );
        verify( subSetDao ).load( 11L );
        verifyNoMoreInteractions( subSetDao );
    }

    @Test
    public void loadSubSetWithBioAssays_delegatesToSubSetDao() {
        ExpressionExperimentSubSet ss = ExpressionExperimentSubSet.Factory.newInstance( "ss", ee );
        when( subSetDao.loadWithBioAssays( 12L ) ).thenReturn( ss );

        ExpressionExperimentSubSet actual = service.loadSubSetWithBioAssays( 12L );

        assertSame( ss, actual );
        verify( subSetDao ).loadWithBioAssays( 12L );
        verifyNoMoreInteractions( subSetDao );
    }

    @Test
    public void findByBioAssayIn_delegatesToSubSetDao() {
        Collection<BioAssay> input = Collections.emptyList();
        Collection<ExpressionExperimentSubSet> expected = Collections.emptyList();
        when( subSetDao.findByBioAssayIn( input ) ).thenReturn( expected );

        Collection<ExpressionExperimentSubSet> actual = service.findByBioAssayIn( input );

        assertSame( expected, actual );
        verify( subSetDao ).findByBioAssayIn( input );
        verifyNoMoreInteractions( subSetDao );
    }

    @Test
    public void getFactorValuesUsed_entityAndFactor_delegatesToSubSetDao() {
        ExpressionExperimentSubSet ss = ExpressionExperimentSubSet.Factory.newInstance( "ss", ee );
        ExperimentalFactor ef = ExperimentalFactor.Factory.newInstance();
        Collection<FactorValue> expected = Collections.emptyList();
        when( subSetDao.getFactorValuesUsed( ss, ef ) ).thenReturn( expected );

        Collection<FactorValue> actual = service.getFactorValuesUsed( ss, ef );

        assertSame( expected, actual );
        verify( subSetDao ).getFactorValuesUsed( ss, ef );
        verifyNoMoreInteractions( subSetDao );
    }

    @Test
    public void getFactorValuesUsedAsVO_emptyResult_returnsEmptyCollection() {
        when( subSetDao.getFactorValuesUsed( 33L, 44L ) ).thenReturn( Collections.emptyList() );

        Collection<FactorValueValueObject> actual = service.getFactorValuesUsedAsVO( 33L, 44L );

        assertNotNull( actual );
        assertTrue( actual.isEmpty() );
        verify( subSetDao ).getFactorValuesUsed( 33L, 44L );
        verifyNoMoreInteractions( subSetDao );
    }

    @Test
    public void getArrayDesignsUsed_delegatesToSubSetDao() {
        ExpressionExperimentSubSet ss = ExpressionExperimentSubSet.Factory.newInstance( "ss", ee );
        Collection<ArrayDesign> expected = Collections.emptyList();
        when( subSetDao.getArrayDesignsUsed( ss ) ).thenReturn( expected );

        Collection<ArrayDesign> actual = service.getArrayDesignsUsed( ss );

        assertSame( expected, actual );
        verify( subSetDao ).getArrayDesignsUsed( ss );
        verifyNoMoreInteractions( subSetDao );
    }
}
