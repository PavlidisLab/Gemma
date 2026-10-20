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
package ubic.gemma.persistence.service.expression.biomaterial;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Lightweight unit test for {@link BioMaterialReadServiceImpl} that verifies each public
 * method delegates to the right {@link BioMaterialDao} call without standing up a Spring
 * context. Covers the delegation contract; deeper integration coverage lives in
 * {@link BioMaterialDaoTest}.
 */
@ExtendWith(MockitoExtension.class)
public class BioMaterialReadServiceTest {

    @Mock
    private BioMaterialDao dao;

    @InjectMocks
    private BioMaterialReadServiceImpl service;

    private BioMaterial bm;

    @BeforeEach
    public void setUp() {
        bm = BioMaterial.Factory.newInstance();
        bm.setId( 42L );
    }

    @Test
    public void testCopyDelegates() {
        BioMaterial copy = BioMaterial.Factory.newInstance();
        when( dao.copy( bm ) ).thenReturn( copy );

        assertSame( copy, service.copy( bm ) );
        verify( dao ).copy( bm );
        verifyNoMoreInteractions( dao );
    }

    @Test
    public void testFindSubBioMaterialsDelegates() {
        java.util.List<BioMaterial> subs = Arrays.asList( bm );
        when( dao.findSubBioMaterials( bm, true ) ).thenReturn( subs );

        assertSame( subs, service.findSubBioMaterials( bm, true ) );
        verify( dao ).findSubBioMaterials( bm, true );
        verifyNoMoreInteractions( dao );
    }

    @Test
    public void testFindSiblingsReturnsEmptyWhenNoSource() {
        // bm has no sourceBioMaterial -> empty
        Collection<BioMaterial> result = service.findSiblings( bm );
        assertTrue( result.isEmpty() );
        verifyNoInteractions( dao );
    }

    @Test
    public void testFindSiblingsRemovesSelf() {
        BioMaterial source = BioMaterial.Factory.newInstance();
        source.setId( 100L );
        bm.setSourceBioMaterial( source );

        BioMaterial sibling = BioMaterial.Factory.newInstance();
        sibling.setId( 43L );

        // dao returns a mutable List containing both bm and sibling
        java.util.List<BioMaterial> subs = new java.util.ArrayList<>();
        subs.add( bm );
        subs.add( sibling );
        when( dao.findSubBioMaterials( source, true ) ).thenReturn( subs );

        Collection<BioMaterial> result = service.findSiblings( bm );
        assertEquals( 1, result.size() );
        assertTrue( result.contains( sibling ) );
        assertFalse( result.contains( bm ) );
    }

    @Test
    public void testFindByExperimentDelegates() {
        ExpressionExperiment ee = ExpressionExperiment.Factory.newInstance();
        Collection<BioMaterial> bms = new HashSet<>( Arrays.asList( bm ) );
        when( dao.findByExperiment( ee ) ).thenReturn( bms );

        assertSame( bms, service.findByExperiment( ee ) );
        verify( dao ).findByExperiment( ee );
        verifyNoMoreInteractions( dao );
    }

    @Test
    public void testFindByFactorDelegates() {
        ExperimentalFactor ef = ExperimentalFactor.Factory.newInstance();
        Collection<BioMaterial> bms = new HashSet<>( Arrays.asList( bm ) );
        when( dao.findByFactor( ef ) ).thenReturn( bms );

        assertSame( bms, service.findByFactor( ef ) );
        verify( dao ).findByFactor( ef );
        verifyNoMoreInteractions( dao );
    }

    @Test
    public void testLoadAndThawOrFailReturnsBmWhenFound() {
        when( dao.load( 42L ) ).thenReturn( bm );

        BioMaterial result = service.loadAndThawOrFail( 42L, IllegalArgumentException::new, "missing" );
        assertSame( bm, result );
    }

    @Test
    public void testLoadAndThawOrFailThrowsWhenMissing() {
        when( dao.load( 99L ) ).thenReturn( null );

        IllegalArgumentException ex = assertThrows( IllegalArgumentException.class,
                () -> service.loadAndThawOrFail( 99L, IllegalArgumentException::new, "missing bm 99" ) );
        assertEquals( "missing bm 99", ex.getMessage() );
    }

    @Test
    public void testGetExpressionExperimentsDelegates() {
        Map<BioMaterial, Map<BioAssay, ExpressionExperiment>> expected = new HashMap<>();
        when( dao.load( 42L ) ).thenReturn( bm );
        when( dao.getExpressionExperiments( bm ) ).thenReturn( expected );

        Map<BioMaterial, Map<BioAssay, ExpressionExperiment>> result = service.getExpressionExperiments( bm );
        assertSame( expected, result );
        verify( dao ).load( 42L );
        verify( dao ).getExpressionExperiments( bm );
    }

    @Test
    public void testThawCollectionEmpty() {
        Collection<BioMaterial> result = service.thaw( Collections.emptyList() );
        assertTrue( result.isEmpty() );
        verifyNoInteractions( dao );
    }
}
