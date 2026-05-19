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
package ubic.gemma.persistence.service.expression.designElement;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ubic.gemma.model.association.BioSequence2GeneProduct;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.persistence.util.Slice;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Lightweight unit test for {@link CompositeSequenceReadServiceImpl} that verifies each
 * public method delegates to the right {@link CompositeSequenceDao} call without standing
 * up a Spring context. Covers the delegation contract; deeper integration coverage is
 * exercised through the DAO and facade-level tests.
 */
@RunWith(MockitoJUnitRunner.class)
public class CompositeSequenceReadServiceTest {

    @Mock
    private CompositeSequenceDao dao;

    @InjectMocks
    private CompositeSequenceReadServiceImpl service;

    private CompositeSequence cs;
    private ArrayDesign ad;
    private Gene gene;

    @Before
    public void setUp() {
        cs = CompositeSequence.Factory.newInstance();
        cs.setId( 42L );
        cs.setName( "probe-42" );

        ad = ArrayDesign.Factory.newInstance();
        ad.setId( 7L );

        gene = Gene.Factory.newInstance();
        gene.setId( 9L );
    }

    @Test
    public void testFindByBioSequenceDelegates() {
        BioSequence bs = BioSequence.Factory.newInstance();
        Collection<CompositeSequence> expected = Collections.singleton( cs );
        when( dao.findByBioSequence( bs ) ).thenReturn( expected );

        assertSame( expected, service.findByBioSequence( bs ) );
        verify( dao ).findByBioSequence( bs );
        verifyNoMoreInteractions( dao );
    }

    @Test
    public void testFindByBioSequenceNameDelegates() {
        Collection<CompositeSequence> expected = Collections.singleton( cs );
        when( dao.findByBioSequenceName( "seq-x" ) ).thenReturn( expected );

        assertSame( expected, service.findByBioSequenceName( "seq-x" ) );
        verify( dao ).findByBioSequenceName( "seq-x" );
        verifyNoMoreInteractions( dao );
    }

    @Test
    public void testFindByGeneDelegates() {
        Collection<CompositeSequence> expected = Collections.singleton( cs );
        when( dao.findByGene( gene, true ) ).thenReturn( expected );

        assertSame( expected, service.findByGene( gene, true ) );
        verify( dao ).findByGene( gene, true );
        verifyNoMoreInteractions( dao );
    }

    @Test
    public void testFindByGeneWithPlatformDelegates() {
        Collection<CompositeSequence> expected = Collections.singleton( cs );
        when( dao.findByGene( gene, ad, false ) ).thenReturn( expected );

        assertSame( expected, service.findByGene( gene, ad, false ) );
        verify( dao ).findByGene( gene, ad, false );
        verifyNoMoreInteractions( dao );
    }

    @Test
    public void testFindByGenesDelegates() {
        Collection<Gene> genes = Collections.singleton( gene );
        Map<Gene, Collection<CompositeSequence>> expected = new HashMap<>();
        expected.put( gene, Collections.singleton( cs ) );
        when( dao.findByGenes( genes, true ) ).thenReturn( expected );

        assertSame( expected, service.findByGenes( genes, true ) );
        verify( dao ).findByGenes( genes, true );
        verifyNoMoreInteractions( dao );
    }

    @Test
    public void testFindByGenesWithPlatformDelegates() {
        Collection<Gene> genes = Collections.singleton( gene );
        Map<Gene, Collection<CompositeSequence>> expected = new HashMap<>();
        when( dao.findByGenes( genes, ad, true ) ).thenReturn( expected );

        assertSame( expected, service.findByGenes( genes, ad, true ) );
        verify( dao ).findByGenes( genes, ad, true );
        verifyNoMoreInteractions( dao );
    }

    @Test
    public void testFindByNameDelegates() {
        Collection<CompositeSequence> expected = Collections.singleton( cs );
        when( dao.findByName( "probe-42" ) ).thenReturn( expected );

        assertSame( expected, service.findByName( "probe-42" ) );
        verify( dao ).findByName( "probe-42" );
        verifyNoMoreInteractions( dao );
    }

    @Test
    public void testFindByNameInPlatformDelegates() {
        when( dao.findByName( ad, "probe-42" ) ).thenReturn( cs );

        assertSame( cs, service.findByName( ad, "probe-42" ) );
        verify( dao ).findByName( ad, "probe-42" );
        verifyNoMoreInteractions( dao );
    }

    @Test
    public void testFindByNamesInArrayDesignsResolvesAndDedupes() {
        CompositeSequence csB = CompositeSequence.Factory.newInstance();
        csB.setId( 43L );
        csB.setName( "probe-43" );

        // Two array designs to verify cross-platform deduping by name.
        ArrayDesign ad2 = ArrayDesign.Factory.newInstance();
        ad2.setId( 8L );

        when( dao.findByName( ad, "probe-42" ) ).thenReturn( cs );
        when( dao.findByName( ad, "probe-43" ) ).thenReturn( null );
        when( dao.findByName( ad2, "probe-42" ) ).thenReturn( cs ); // duplicate name — must NOT be added again
        when( dao.findByName( ad2, "probe-43" ) ).thenReturn( csB );

        Collection<CompositeSequence> result = service.findByNamesInArrayDesigns(
                Arrays.asList( " probe-42 ", "probe-43" ), // leading/trailing whitespace stripped
                Arrays.asList( ad, ad2 ) );
        List<CompositeSequence> ordered = new ArrayList<>( result );
        assertEquals( 2, ordered.size() );
        assertSame( cs, ordered.get( 0 ) );
        assertSame( csB, ordered.get( 1 ) );
    }

    @Test
    public void testFindByNamesInArrayDesignsReturnsNullWhenAllUnknown() {
        when( dao.findByName( ad, "nope" ) ).thenReturn( null );
        assertNull( service.findByNamesInArrayDesigns( Collections.singletonList( "nope" ), Collections.singletonList( ad ) ) );
    }

    @Test
    public void testGetGenesCollectionDelegates() {
        Collection<CompositeSequence> css = Collections.singleton( cs );
        Map<CompositeSequence, Collection<Gene>> expected = new HashMap<>();
        when( dao.getGenes( css, true ) ).thenReturn( expected );

        assertSame( expected, service.getGenes( css, true ) );
        verify( dao ).getGenes( css, true );
        verifyNoMoreInteractions( dao );
    }

    @Test
    public void testGetGenesSingleUsesSliceWithUnboundedLimit() {
        // Use a Mockito mock since Slice's constructors aren't trivial to call from a unit test.
        @SuppressWarnings("unchecked")
        Slice<Gene> slice = org.mockito.Mockito.mock( Slice.class );
        when( dao.getGenes( cs, 0, -1, true ) ).thenReturn( slice );

        Collection<Gene> result = service.getGenes( cs, true );
        assertSame( slice, result );
        verify( dao ).getGenes( cs, 0, -1, true );
        verifyNoMoreInteractions( dao );
    }

    @Test
    public void testGetGenesSlicedDelegates() {
        @SuppressWarnings("unchecked")
        Slice<Gene> slice = org.mockito.Mockito.mock( Slice.class );
        when( dao.getGenes( cs, 10, 20, false ) ).thenReturn( slice );

        assertSame( slice, service.getGenes( cs, 10, 20, false ) );
        verify( dao ).getGenes( cs, 10, 20, false );
        verifyNoMoreInteractions( dao );
    }

    @Test
    public void testGetGenesWithSpecificityDelegates() {
        Collection<CompositeSequence> css = Collections.singleton( cs );
        Map<CompositeSequence, Collection<BioSequence2GeneProduct>> expected = new HashMap<>();
        when( dao.getGenesWithSpecificity( css ) ).thenReturn( expected );

        assertSame( expected, service.getGenesWithSpecificity( css ) );
        verify( dao ).getGenesWithSpecificity( css );
        verifyNoMoreInteractions( dao );
    }

    @Test
    public void testGetRawSummaryDelegates() {
        Collection<CompositeSequence> css = Collections.singleton( cs );
        Collection<Object[]> expected = Collections.singletonList( new Object[] { 1, "x" } );
        when( dao.getRawSummary( css ) ).thenReturn( expected );

        assertSame( expected, service.getRawSummary( css ) );
        verify( dao ).getRawSummary( css );
        verifyNoMoreInteractions( dao );
    }

    @Test
    public void testGetRawSummaryArrayDesignDelegates() {
        Collection<Object[]> expected = Collections.emptyList();
        when( dao.getRawSummary( ad, 50 ) ).thenReturn( expected );

        assertSame( expected, service.getRawSummary( ad, 50 ) );
        verify( dao ).getRawSummary( ad, 50 );
        verifyNoMoreInteractions( dao );
    }

    @Test
    public void testThawCollectionReloadsByIdsAndThaws() {
        Collection<CompositeSequence> input = new LinkedHashSet<>( Collections.singletonList( cs ) );
        Collection<CompositeSequence> reloaded = Collections.singletonList( cs );
        when( dao.load( Collections.singleton( 42L ) ) ).thenReturn( reloaded );

        Collection<CompositeSequence> result = service.thaw( input );
        assertSame( reloaded, result );
        verify( dao ).load( Collections.singleton( 42L ) );
        verify( dao ).thaw( reloaded );
        verifyNoMoreInteractions( dao );
    }

    @Test
    public void testThawSingleReloadsAndThaws() {
        when( dao.load( 42L ) ).thenReturn( cs );

        CompositeSequence result = service.thaw( cs );
        assertSame( cs, result );
        verify( dao ).load( 42L );
        verify( dao ).thaw( cs );
        verifyNoMoreInteractions( dao );
    }

    @Test
    public void testThawSingleThrowsWhenMissing() {
        when( dao.load( 42L ) ).thenReturn( null );

        NullPointerException ex = assertThrows( NullPointerException.class, () -> service.thaw( cs ) );
        assertTrue( ex.getMessage().contains( "42" ) );
        verify( dao ).load( 42L );
        verify( dao, never() ).thaw( cs );
    }
}
