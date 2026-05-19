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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Lightweight unit test for {@link ExpressionExperimentSubSetReadServiceImpl} that verifies
 * each public method delegates to the right {@link ExpressionExperimentDao} call without
 * standing up a Spring context. Covers the delegation contract; deeper integration coverage
 * lives in {@code ExpressionExperimentDaoTest}.
 */
@RunWith(MockitoJUnitRunner.class)
public class ExpressionExperimentSubSetReadServiceTest {

    @Mock
    private ExpressionExperimentDao dao;

    @InjectMocks
    private ExpressionExperimentSubSetReadServiceImpl service;

    private ExpressionExperiment ee;
    private BioAssayDimension bad;

    @Before
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
}
