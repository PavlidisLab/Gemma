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
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.persistence.util.Slice;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Lightweight unit test for {@link FactorValueReadServiceImpl} that verifies each public
 * method delegates to the right {@link FactorValueDao} call without standing up a Spring
 * context. Covers the delegation contract.
 */
@RunWith(MockitoJUnitRunner.class)
public class FactorValueReadServiceTest {

    @Mock
    private FactorValueDao dao;

    @InjectMocks
    private FactorValueReadServiceImpl service;

    private FactorValue fv;

    @Before
    public void setUp() {
        fv = FactorValue.Factory.newInstance();
        fv.setId( 42L );
        ExperimentalFactor ef = ExperimentalFactor.Factory.newInstance();
        ef.setId( 7L );
        fv.setExperimentalFactor( ef );
    }

    @Test
    public void testLoadWithExperimentalFactorReturnsNullWhenMissing() {
        when( dao.load( 99L ) ).thenReturn( null );
        assertNull( service.loadWithExperimentalFactor( 99L ) );
        verify( dao ).load( 99L );
        verifyNoMoreInteractions( dao );
    }

    @Test
    public void testLoadWithExperimentalFactorDelegates() {
        when( dao.load( 42L ) ).thenReturn( fv );
        assertSame( fv, service.loadWithExperimentalFactor( 42L ) );
        verify( dao ).load( 42L );
        verifyNoMoreInteractions( dao );
    }

    @Test
    public void testLoadWithExperimentalFactorOrFailReturnsFv() {
        when( dao.load( 42L ) ).thenReturn( fv );
        FactorValue result = service.loadWithExperimentalFactorOrFail( 42L, IllegalStateException::new );
        assertSame( fv, result );
    }

    @Test
    public void testLoadWithExperimentalFactorOrFailThrowsWhenMissing() {
        when( dao.load( 99L ) ).thenReturn( null );
        IllegalStateException ex = assertThrows( IllegalStateException.class,
                () -> service.loadWithExperimentalFactorOrFail( 99L, IllegalStateException::new ) );
        // message format mirrors AbstractService#loadOrFail(Long, Function)
        assertEquals( "No " + FactorValue.class.getName() + " with ID 99.", ex.getMessage() );
    }

    @Test
    public void testGetExperimentalFactorCategoriesIgnoreAclsDelegates() {
        Collection<FactorValue> fvs = Arrays.asList( fv );
        Map<FactorValue, Characteristic> expected = new HashMap<>();
        when( dao.getExperimentalFactorCategories( fvs ) ).thenReturn( expected );
        assertSame( expected, service.getExperimentalFactorCategoriesIgnoreAcls( fvs ) );
        verify( dao ).getExperimentalFactorCategories( fvs );
        verifyNoMoreInteractions( dao );
    }

    @Test
    public void testGetExpressionExperimentsIgnoreAclsDelegates() {
        Collection<FactorValue> fvs = Arrays.asList( fv );
        Map<FactorValue, ExpressionExperiment> expected = new HashMap<>();
        when( dao.getExpressionExperimentsIgnoreAcls( fvs ) ).thenReturn( expected );
        assertSame( expected, service.getExpressionExperimentsIgnoreAcls( fvs ) );
        verify( dao ).getExpressionExperimentsIgnoreAcls( fvs );
        verifyNoMoreInteractions( dao );
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testLoadWithOldStyleCharacteristicsDelegates() {
        when( dao.loadWithOldStyleCharacteristics( 42L, true ) ).thenReturn( fv );
        assertSame( fv, service.loadWithOldStyleCharacteristics( 42L, true ) );
        verify( dao ).loadWithOldStyleCharacteristics( 42L, true );
        verifyNoMoreInteractions( dao );
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testLoadIdsWithNumberOfOldStyleCharacteristicsDelegates() {
        Set<Long> excluded = new HashSet<>( Arrays.asList( 1L, 2L ) );
        Map<Long, Integer> expected = new HashMap<>();
        expected.put( 10L, 3 );
        when( dao.loadIdsWithNumberOfOldStyleCharacteristics( excluded ) ).thenReturn( expected );
        assertSame( expected, service.loadIdsWithNumberOfOldStyleCharacteristics( excluded ) );
        verify( dao ).loadIdsWithNumberOfOldStyleCharacteristics( excluded );
        verifyNoMoreInteractions( dao );
    }

    @Test
    public void testLoadIgnoreAclsDelegates() {
        Set<Long> ids = new HashSet<>( Arrays.asList( 42L ) );
        Collection<FactorValue> expected = Arrays.asList( fv );
        when( dao.load( ids ) ).thenReturn( expected );
        assertSame( expected, service.loadIgnoreAcls( ids ) );
        verify( dao ).load( ids );
        verifyNoMoreInteractions( dao );
    }

    @Test
    public void testLoadAllOffsetLimitDelegates() {
        Slice<FactorValue> slice = new Slice<>( Collections.singletonList( fv ), null, 0, 10, 1L );
        when( dao.loadAll( 0, 10 ) ).thenReturn( slice );
        assertSame( slice, service.loadAll( 0, 10 ) );
        verify( dao ).loadAll( 0, 10 );
        verifyNoMoreInteractions( dao );
    }

    @Test
    public void testLoadAllIdsDelegates() {
        List<Long> expected = Arrays.asList( 42L, 43L );
        when( dao.loadAllIds() ).thenReturn( expected );
        assertSame( expected, service.loadAllIds() );
        verify( dao ).loadAllIds();
        verifyNoMoreInteractions( dao );
    }

    @Test
    public void testLoadAllIdsOffsetLimitDelegates() {
        Slice<Long> slice = new Slice<>( Arrays.asList( 42L ), null, 0, 10, 1L );
        when( dao.loadAllIds( 0, 10 ) ).thenReturn( slice );
        assertSame( slice, service.loadAllIds( 0, 10 ) );
        verify( dao ).loadAllIds( 0, 10 );
        verifyNoMoreInteractions( dao );
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testFindByValueStartingWithDelegates() {
        Collection<FactorValue> expected = Arrays.asList( fv );
        when( dao.findByValueStartingWith( "pref", 5 ) ).thenReturn( expected );
        assertSame( expected, service.findByValueStartingWith( "pref", 5 ) );
        verify( dao ).findByValueStartingWith( "pref", 5 );
        verifyNoMoreInteractions( dao );
    }

    @Test
    public void testLoadWithExperimentalFactorOrFailNeverLooksAtFactor_whenMissing() {
        when( dao.load( 99L ) ).thenReturn( null );
        assertThrows( IllegalStateException.class,
                () -> service.loadWithExperimentalFactorOrFail( 99L, IllegalStateException::new ) );
        verify( dao ).load( 99L );
        verify( dao, never() ).loadWithOldStyleCharacteristics( 99L, false );
    }
}
