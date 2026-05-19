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
package ubic.gemma.persistence.service.analysis.expression.diff;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Lightweight unit test for {@link DifferentialExpressionAnalysisReadServiceImpl} that
 * verifies each public method delegates to the right DAO call without standing up a
 * Spring context. Covers the delegation contract for the simple {@code findBy*} cluster;
 * the heavier {@code thaw}, {@code thawFully}, and {@code findByExperimentIds} methods
 * involve Hibernate initialization and are covered by integration tests in
 * {@link DifferentialExpressionAnalysisServiceTest} and the dao-level tests.
 */
@RunWith(MockitoJUnitRunner.class)
public class DifferentialExpressionAnalysisReadServiceTest {

    @Mock
    private DifferentialExpressionAnalysisDao dao;

    @Mock
    private ExpressionAnalysisResultSetDao expressionAnalysisResultSetDao;

    @Mock
    private ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentDao expressionExperimentDao;

    private DifferentialExpressionAnalysisReadServiceImpl service;

    private DifferentialExpressionAnalysis dea;

    @Before
    public void setUp() {
        service = new DifferentialExpressionAnalysisReadServiceImpl( dao );
        // wire the @Autowired fields by reflection -- mirrors how Spring would inject them at runtime
        ReflectionTestUtils.setField( service, "expressionAnalysisResultSetDao", expressionAnalysisResultSetDao );
        ReflectionTestUtils.setField( service, "expressionExperimentDao", expressionExperimentDao );

        dea = DifferentialExpressionAnalysis.Factory.newInstance();
        dea.setId( 42L );
    }

    @Test
    public void testLoadWithExperimentAnalyzedReturnsNullWhenMissing() {
        when( dao.load( 99L ) ).thenReturn( null );
        assertNull( service.loadWithExperimentAnalyzed( 99L ) );
        verify( dao ).load( 99L );
        verifyNoMoreInteractions( dao );
    }

    @Test
    public void testFindByNameDelegates() {
        List<DifferentialExpressionAnalysis> results = Arrays.asList( dea );
        when( dao.findByName( "foo" ) ).thenReturn( results );

        assertSame( results, service.findByName( "foo" ) );
        verify( dao ).findByName( "foo" );
        verifyNoMoreInteractions( dao );
    }

    @Test
    public void testFindByFactorDelegates() {
        ExperimentalFactor ef = ExperimentalFactor.Factory.newInstance();
        Collection<DifferentialExpressionAnalysis> results = new HashSet<>( Arrays.asList( dea ) );
        when( dao.findByFactor( ef ) ).thenReturn( results );

        assertSame( results, service.findByFactor( ef ) );
        verify( dao ).findByFactor( ef );
        verifyNoMoreInteractions( dao );
    }

    @Test
    public void testFindExperimentsWithAnalysesDelegates() {
        Gene gene = Gene.Factory.newInstance();
        Collection<BioAssaySet> results = new HashSet<>();
        when( dao.findExperimentsWithAnalyses( gene ) ).thenReturn( results );

        assertSame( results, service.findExperimentsWithAnalyses( gene ) );
        verify( dao ).findExperimentsWithAnalyses( gene );
        verifyNoMoreInteractions( dao );
    }

    @Test
    public void testFindByExperimentAndAnalysisIdDelegates() {
        ExpressionExperiment ee = ExpressionExperiment.Factory.newInstance();
        when( dao.findByExperimentAndAnalysisId( ee, true, 7L ) ).thenReturn( dea );

        assertSame( dea, service.findByExperimentAndAnalysisId( ee, true, 7L ) );
        verify( dao ).findByExperimentAndAnalysisId( ee, true, 7L );
        verifyNoMoreInteractions( dao );
    }

    @Test
    public void testFindByExperimentDelegates() {
        ExpressionExperiment ee = ExpressionExperiment.Factory.newInstance();
        Collection<DifferentialExpressionAnalysis> results = new HashSet<>( Arrays.asList( dea ) );
        when( dao.findByExperiment( ee, false ) ).thenReturn( results );

        assertSame( results, service.findByExperiment( ee, false ) );
        verify( dao ).findByExperiment( ee, false );
        verifyNoMoreInteractions( dao );
    }

    @Test
    public void testFindByExperimentsDelegates() {
        Collection<ExpressionExperiment> ees = Collections.singletonList( ExpressionExperiment.Factory.newInstance() );
        when( dao.findByExperiments( ees, true ) ).thenReturn( Collections.emptyMap() );

        assertTrue( service.findByExperiments( ees, true ).isEmpty() );
        verify( dao ).findByExperiments( ees, true );
        verifyNoMoreInteractions( dao );
    }

    @Test
    public void testGetExperimentsWithAnalysisDelegates() {
        Collection<Long> ids = Arrays.asList( 1L, 2L, 3L );
        Collection<Long> result = Arrays.asList( 1L, 3L );
        when( dao.getExperimentsWithAnalysis( ids, false ) ).thenReturn( result );

        assertSame( result, service.getExperimentsWithAnalysis( ids, false ) );
        verify( dao ).getExperimentsWithAnalysis( ids, false );
        verifyNoMoreInteractions( dao );
    }

    @Test
    public void testCanDeleteDelegatesToResultSetDao() {
        when( expressionAnalysisResultSetDao.canDelete( dea ) ).thenReturn( true );

        assertTrue( service.canDelete( dea ) );
        verify( expressionAnalysisResultSetDao ).canDelete( dea );
        verifyNoMoreInteractions( expressionAnalysisResultSetDao );
    }

    @Test
    public void testCanDeleteReturnsFalse() {
        when( expressionAnalysisResultSetDao.canDelete( dea ) ).thenReturn( false );
        assertFalse( service.canDelete( dea ) );
        verify( expressionAnalysisResultSetDao ).canDelete( dea );
    }

    @Test
    public void testThawCollectionEmpty() {
        Collection<DifferentialExpressionAnalysis> result = service.thaw( Collections.emptyList() );
        assertTrue( result.isEmpty() );
    }
}
