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
package ubic.gemma.persistence.service.common.quantitationtype;

import org.hibernate.NonUniqueResultException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.QuantitationTypeValueObject;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.BulkExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.DataVector;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Lightweight unit test for {@link QuantitationTypeReadServiceImpl} that verifies each
 * public method delegates to the right {@link QuantitationTypeDao} call without standing
 * up a Spring context. Covers the delegation contract; deeper integration coverage lives
 * in {@link QuantitationTypeDaoTest}.
 */
@RunWith(MockitoJUnitRunner.class)
public class QuantitationTypeReadServiceTest {

    @Mock
    private QuantitationTypeDao dao;

    @InjectMocks
    private QuantitationTypeReadServiceImpl service;

    private QuantitationType qt;
    private ExpressionExperiment ee;

    @Before
    public void setUp() {
        qt = QuantitationType.Factory.newInstance();
        qt.setId( 7L );
        qt.setName( "RMA" );
        ee = ExpressionExperiment.Factory.newInstance();
        ee.setId( 1L );
    }

    @Test
    public void testGetVectorTypesDelegates() {
        Collection<Class<? extends DataVector>> types = Collections.singleton( RawExpressionDataVector.class );
        when( dao.getVectorTypes() ).thenReturn( types );

        assertSame( types, service.getVectorTypes() );
        verify( dao ).getVectorTypes();
        verifyNoMoreInteractions( dao );
    }

    @Test
    public void testFindByExpressionExperimentMapDelegates() {
        Map<Class<? extends DataVector>, Set<QuantitationType>> expected = Collections.emptyMap();
        when( dao.findByExpressionExperiment( ee ) ).thenReturn( expected );

        assertSame( expected, service.findByExpressionExperiment( ee ) );
        verify( dao ).findByExpressionExperiment( ee );
        verifyNoMoreInteractions( dao );
    }

    @Test
    public void testFindByExpressionExperimentSingleVectorTypeDelegates() {
        Collection<QuantitationType> qts = Collections.singleton( qt );
        when( dao.findByExpressionExperiment( ee, RawExpressionDataVector.class ) ).thenReturn( qts );

        assertSame( qts, service.findByExpressionExperiment( ee, RawExpressionDataVector.class ) );
        verify( dao ).findByExpressionExperiment( ee, RawExpressionDataVector.class );
        verifyNoMoreInteractions( dao );
    }

    @Test
    public void testFindByExpressionExperimentMultipleVectorTypesUnions() {
        QuantitationType other = QuantitationType.Factory.newInstance();
        other.setId( 8L );
        when( dao.findByExpressionExperiment( ee, RawExpressionDataVector.class ) ).thenReturn( Collections.singleton( qt ) );
        when( dao.findByExpressionExperiment( ee, ProcessedExpressionDataVector.class ) ).thenReturn( Collections.singleton( other ) );

        Collection<Class<? extends DataVector>> vts = Arrays.asList( RawExpressionDataVector.class, ProcessedExpressionDataVector.class );
        Collection<QuantitationType> result = service.findByExpressionExperiment( ee, vts );
        assertEquals( 2, result.size() );
        assertTrue( result.contains( qt ) );
        assertTrue( result.contains( other ) );
    }

    @Test
    public void testFindByExpressionExperimentAndDimensionDelegates() {
        BioAssayDimension dim = BioAssayDimension.Factory.newInstance();
        Collection<QuantitationType> qts = Collections.singleton( qt );
        when( dao.findByExpressionExperimentAndDimension( ee, dim ) ).thenReturn( qts );

        assertSame( qts, service.findByExpressionExperimentAndDimension( ee, dim ) );
        verify( dao ).findByExpressionExperimentAndDimension( ee, dim );
        verifyNoMoreInteractions( dao );
    }

    @Test
    public void testFindByExpressionExperimentAndDimensionWithVectorTypesDelegates() {
        BioAssayDimension dim = BioAssayDimension.Factory.newInstance();
        Collection<Class<? extends BulkExpressionDataVector>> vts = Collections.singleton( RawExpressionDataVector.class );
        Collection<QuantitationType> qts = Collections.singleton( qt );
        when( dao.findByExpressionExperimentAndDimension( ee, dim, vts ) ).thenReturn( qts );

        assertSame( qts, service.findByExpressionExperimentAndDimension( ee, dim, vts ) );
        verify( dao ).findByExpressionExperimentAndDimension( ee, dim, vts );
        verifyNoMoreInteractions( dao );
    }

    @Test
    public void testLoadValueObjectsWithExpressionExperimentDelegates() {
        Collection<QuantitationType> qts = Collections.singleton( qt );
        @SuppressWarnings("unchecked")
        List<QuantitationTypeValueObject> vos = mock( List.class );
        when( dao.loadValueObjectsWithExpressionExperiment( qts, ee ) ).thenReturn( vos );

        assertSame( vos, service.loadValueObjectsWithExpressionExperiment( qts, ee ) );
        verify( dao ).loadValueObjectsWithExpressionExperiment( qts, ee );
        verifyNoMoreInteractions( dao );
    }

    @Test
    public void testGetDataVectorTypeDelegates() {
        when( dao.getDataVectorType( qt ) ).thenReturn( (Class) RawExpressionDataVector.class );

        assertEquals( RawExpressionDataVector.class, service.getDataVectorType( qt ) );
        verify( dao ).getDataVectorType( qt );
        verifyNoMoreInteractions( dao );
    }

    @Test
    public void testGetDataVectorTypesAggregates() {
        QuantitationType other = QuantitationType.Factory.newInstance();
        other.setId( 8L );
        when( dao.getDataVectorType( qt ) ).thenReturn( (Class) RawExpressionDataVector.class );
        when( dao.getDataVectorType( other ) ).thenReturn( (Class) ProcessedExpressionDataVector.class );

        Map<QuantitationType, Class<? extends DataVector>> result = service.getDataVectorTypes( Arrays.asList( qt, other ) );
        assertEquals( 2, result.size() );
        assertEquals( RawExpressionDataVector.class, result.get( qt ) );
        assertEquals( ProcessedExpressionDataVector.class, result.get( other ) );
    }

    @Test
    public void testGetMappedDataVectorTypeDelegates() {
        Collection<Class<? extends DataVector>> mapped = Collections.singleton( RawExpressionDataVector.class );
        when( dao.getMappedDataVectorTypes( DataVector.class ) ).thenReturn( mapped );

        assertSame( mapped, service.getMappedDataVectorType( DataVector.class ) );
        verify( dao ).getMappedDataVectorTypes( DataVector.class );
        verifyNoMoreInteractions( dao );
    }

    @Test
    public void testLoadByIdDelegates() {
        when( dao.loadById( 7L, ee ) ).thenReturn( qt );

        assertSame( qt, service.loadById( 7L, ee ) );
        verify( dao ).loadById( 7L, ee );
        verifyNoMoreInteractions( dao );
    }

    @Test
    public void testLoadByIdAndVectorTypeDelegates() {
        when( dao.loadByIdAndVectorType( 7L, ee, RawExpressionDataVector.class ) ).thenReturn( qt );

        assertSame( qt, service.loadByIdAndVectorType( 7L, ee, RawExpressionDataVector.class ) );
        verify( dao ).loadByIdAndVectorType( 7L, ee, RawExpressionDataVector.class );
        verifyNoMoreInteractions( dao );
    }

    @Test
    public void testReloadDelegates() {
        when( dao.reload( qt ) ).thenReturn( qt );

        assertSame( qt, service.reload( qt ) );
        verify( dao ).reload( qt );
        verifyNoMoreInteractions( dao );
    }

    @Test
    public void testFindWrapsSingletonVectorType() {
        when( dao.find( eq( ee ), eq( qt ), eq( Collections.singleton( RawExpressionDataVector.class ) ) ) ).thenReturn( qt );

        assertSame( qt, service.find( ee, qt, RawExpressionDataVector.class ) );
        verify( dao ).find( ee, qt, Collections.singleton( RawExpressionDataVector.class ) );
        verifyNoMoreInteractions( dao );
    }

    @Test
    public void testFindByNameDefaultsToRawAndDelegates() throws Exception {
        when( dao.findByNameAndVectorType( ee, "RMA", RawExpressionDataVector.class ) ).thenReturn( qt );

        assertSame( qt, service.findByName( ee, "RMA" ) );
        verify( dao ).findByNameAndVectorType( ee, "RMA", RawExpressionDataVector.class );
        verifyNoMoreInteractions( dao );
    }

    @Test
    public void testFindByNameWrapsNonUniqueResultException() {
        when( dao.findByNameAndVectorType( ee, "RMA", RawExpressionDataVector.class ) )
                .thenThrow( new NonUniqueResultException( 2 ) );

        NonUniqueQuantitationTypeByNameException ex = assertThrows(
                NonUniqueQuantitationTypeByNameException.class,
                () -> service.findByName( ee, "RMA" ) );
        assertNotNull( ex.getMessage() );
        assertTrue( ex.getMessage().contains( "RMA" ) );
    }

    @Test
    public void testFindByNameAndVectorTypeDelegates() throws Exception {
        when( dao.findByNameAndVectorType( ee, "RMA", ProcessedExpressionDataVector.class ) ).thenReturn( qt );

        assertSame( qt, service.findByNameAndVectorType( ee, "RMA", ProcessedExpressionDataVector.class ) );
        verify( dao ).findByNameAndVectorType( ee, "RMA", ProcessedExpressionDataVector.class );
        verifyNoMoreInteractions( dao );
    }

    @Test
    public void testFindByNameAndVectorTypeWrapsNonUniqueResultException() {
        when( dao.findByNameAndVectorType( ee, "RMA", ProcessedExpressionDataVector.class ) )
                .thenThrow( new NonUniqueResultException( 2 ) );

        NonUniqueQuantitationTypeByNameException ex = assertThrows(
                NonUniqueQuantitationTypeByNameException.class,
                () -> service.findByNameAndVectorType( ee, "RMA", ProcessedExpressionDataVector.class ) );
        assertNotNull( ex.getMessage() );
    }

    @Test
    public void testFindAllByNameAndVectorTypeDelegates() {
        Collection<QuantitationType> qts = new HashSet<>( Collections.singleton( qt ) );
        when( dao.findAllByNameAndVectorType( ee, "RMA", RawExpressionDataVector.class ) ).thenReturn( qts );

        assertSame( qts, service.findAllByNameAndVectorType( ee, "RMA", RawExpressionDataVector.class ) );
        verify( dao ).findAllByNameAndVectorType( ee, "RMA", RawExpressionDataVector.class );
        verifyNoMoreInteractions( dao );
    }
}
