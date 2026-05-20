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
package ubic.gemma.persistence.service.genome.gene;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.PhysicalLocation;
import ubic.gemma.model.genome.PhysicalLocationValueObject;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneProduct;
import ubic.gemma.model.genome.gene.GeneProductValueObject;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.persistence.service.genome.GeneDao;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Lightweight unit test for {@link GeneReadServiceImpl} that verifies each public method
 * delegates to the right {@link GeneDao} call without standing up a Spring context.
 * Covers the delegation contract; deeper integration coverage lives in
 * {@link GeneServiceTest} and {@code GeneDaoTest}.
 */
@ExtendWith(MockitoExtension.class)
public class GeneReadServiceTest {

    @Mock
    private GeneDao dao;

    @InjectMocks
    private GeneReadServiceImpl service;

    private Gene gene;

    @BeforeEach
    public void setUp() {
        gene = Gene.Factory.newInstance();
        gene.setId( 42L );
        gene.setOfficialSymbol( "FOO" );
    }

    @Test
    public void testFindByPhysicalLocationDelegates() {
        PhysicalLocation loc = PhysicalLocation.Factory.newInstance();
        Collection<Gene> hits = Collections.singletonList( gene );
        when( dao.find( loc ) ).thenReturn( hits );

        assertSame( hits, service.find( loc ) );
        verify( dao ).find( loc );
        verifyNoMoreInteractions( dao );
    }

    @Test
    public void testFindByAccessionDelegates() {
        ExternalDatabase src = ExternalDatabase.Factory.newInstance();
        when( dao.findByAccession( "X", src ) ).thenReturn( gene );

        assertSame( gene, service.findByAccession( "X", src ) );
        verify( dao ).findByAccession( "X", src );
        verifyNoMoreInteractions( dao );
    }

    @Test
    public void testFindByAccessionNullSourceDelegates() {
        when( dao.findByAccession( "X", null ) ).thenReturn( gene );

        assertSame( gene, service.findByAccession( "X", null ) );
        verify( dao ).findByAccession( "X", null );
    }

    @Test
    public void testFindByAliasDelegates() {
        Collection<Gene> hits = Collections.singletonList( gene );
        when( dao.findByAlias( "foo" ) ).thenReturn( hits );

        assertSame( hits, service.findByAlias( "foo" ) );
        verify( dao ).findByAlias( "foo" );
    }

    @Test
    public void testFindByEnsemblIdDelegates() {
        when( dao.findByEnsemblId( "ENSG1" ) ).thenReturn( gene );

        assertSame( gene, service.findByEnsemblId( "ENSG1" ) );
        verify( dao ).findByEnsemblId( "ENSG1" );
    }

    @Test
    public void testFindByNCBIIdDelegates() {
        when( dao.findByNcbiId( 99 ) ).thenReturn( gene );

        assertSame( gene, service.findByNCBIId( 99 ) );
        verify( dao ).findByNcbiId( 99 );
    }

    @Test
    public void testFindByNCBIIdValueObjectReturnsNullWhenMissing() {
        when( dao.findByNcbiId( 99 ) ).thenReturn( null );

        assertNull( service.findByNCBIIdValueObject( 99 ) );
    }

    @Test
    public void testFindByNCBIIdValueObjectWrapsGene() {
        gene.setNcbiGeneId( 99 );
        when( dao.findByNcbiId( 99 ) ).thenReturn( gene );

        GeneValueObject vo = service.findByNCBIIdValueObject( 99 );
        assertNotNull( vo );
    }

    @Test
    public void testFindByNcbiIdsBuildsVoMap() {
        Gene a = Gene.Factory.newInstance();
        a.setId( 1L );
        a.setNcbiGeneId( 10 );
        Gene b = Gene.Factory.newInstance();
        b.setId( 2L );
        b.setNcbiGeneId( 20 );
        Map<Integer, Gene> back = new HashMap<>();
        back.put( 10, a );
        back.put( 20, b );
        when( dao.findByNcbiIds( Arrays.asList( 10, 20 ) ) ).thenReturn( back );

        Map<Integer, GeneValueObject> out = service.findByNcbiIds( Arrays.asList( 10, 20 ) );
        assertEquals( 2, out.size() );
        assertNotNull( out.get( 10 ) );
        assertNotNull( out.get( 20 ) );
    }

    @Test
    public void testFindByOfficialNameDelegates() {
        Collection<Gene> hits = Collections.singletonList( gene );
        when( dao.findByOfficialName( "foo" ) ).thenReturn( hits );

        assertSame( hits, service.findByOfficialName( "foo" ) );
        verify( dao ).findByOfficialName( "foo" );
    }

    @Test
    public void testFindByOfficialSymbolWithTaxonDelegates() {
        Taxon t = Taxon.Factory.newInstance();
        when( dao.findByOfficialSymbol( "FOO", t ) ).thenReturn( gene );

        assertSame( gene, service.findByOfficialSymbol( "FOO", t ) );
        verify( dao ).findByOfficialSymbol( "FOO", t );
    }

    @Test
    public void testFindByOfficialSymbolsBuildsVoMap() {
        Gene a = Gene.Factory.newInstance();
        a.setId( 1L );
        Map<String, Gene> back = new HashMap<>();
        back.put( "foo", a );
        when( dao.findByOfficialSymbols( Collections.singletonList( "foo" ), 9606L ) ).thenReturn( back );

        Map<String, GeneValueObject> out = service.findByOfficialSymbols( Collections.singletonList( "foo" ), 9606L );
        assertEquals( 1, out.size() );
        assertNotNull( out.get( "foo" ) );
    }

    @Test
    public void testGetCompositeSequenceCountDelegates() {
        when( dao.getCompositeSequenceCount( gene, true ) ).thenReturn( 7L );

        assertEquals( 7L, service.getCompositeSequenceCount( gene, true ) );
        verify( dao ).getCompositeSequenceCount( gene, true );
    }

    @Test
    public void testGetCompositeSequenceCountByIdDelegates() {
        when( dao.getCompositeSequenceCountById( 42L, false ) ).thenReturn( 3L );

        assertEquals( 3L, service.getCompositeSequenceCountById( 42L, false ) );
        verify( dao ).getCompositeSequenceCountById( 42L, false );
    }

    @Test
    public void testGetCompositeSequencesWithArrayDesignDelegates() {
        ArrayDesign ad = ArrayDesign.Factory.newInstance();
        Collection<CompositeSequence> css = Collections.emptyList();
        when( dao.getCompositeSequences( gene, ad, false ) ).thenReturn( css );

        assertSame( css, service.getCompositeSequences( gene, ad, false ) );
        verify( dao ).getCompositeSequences( gene, ad, false );
    }

    @Test
    public void testGetPhysicalLocationsValueObjectsNullGene() {
        List<PhysicalLocationValueObject> result = service.getPhysicalLocationsValueObjects( null );
        assertTrue( result.isEmpty() );
        verifyNoInteractions( dao );
    }

    @Test
    public void testGetProductsThrowsOnNullId() {
        assertThrows( IllegalArgumentException.class, () -> service.getProducts( null ) );
    }

    @Test
    public void testGetProductsThrowsWhenGeneMissing() {
        when( dao.load( 5L ) ).thenReturn( null );
        assertThrows( IllegalArgumentException.class, () -> service.getProducts( 5L ) );
    }

    @Test
    public void testGetProductsBuildsVos() {
        GeneProduct gp = GeneProduct.Factory.newInstance();
        gp.setName( "gp1" );
        Gene g = Gene.Factory.newInstance();
        g.setId( 5L );
        g.setProducts( new HashSet<>( Collections.singletonList( gp ) ) );
        when( dao.load( 5L ) ).thenReturn( g );

        Collection<GeneProductValueObject> out = service.getProducts( 5L );
        assertEquals( 1, out.size() );
    }

    @Test
    public void testLoadAllTaxonDelegates() {
        Taxon t = Taxon.Factory.newInstance();
        Collection<Gene> hits = Collections.singletonList( gene );
        when( dao.loadKnownGenes( t ) ).thenReturn( hits );

        assertSame( hits, service.loadAll( t ) );
        verify( dao ).loadKnownGenes( t );
    }

    @Test
    public void testLoadMicroRNAsDelegates() {
        Taxon t = Taxon.Factory.newInstance();
        Collection<Gene> hits = Collections.singletonList( gene );
        when( dao.getMicroRnaByTaxon( t ) ).thenReturn( hits );

        assertSame( hits, service.loadMicroRNAs( t ) );
        verify( dao ).getMicroRnaByTaxon( t );
    }

    @Test
    public void testLoadValueObjectByIdReturnsNullWhenMissing() {
        when( dao.load( 42L ) ).thenReturn( null );
        assertNull( service.loadValueObjectById( 42L ) );
    }

    @Test
    public void testThawDelegates() {
        when( dao.thaw( gene ) ).thenReturn( gene );
        assertSame( gene, service.thaw( gene ) );
        verify( dao ).thaw( gene );
    }

    @Test
    public void testThawLiteSingleDelegates() {
        when( dao.thawLite( gene ) ).thenReturn( gene );
        assertSame( gene, service.thawLite( gene ) );
        verify( dao ).thawLite( gene );
    }

    @Test
    public void testThawLiteCollectionDelegates() {
        Collection<Gene> hits = Collections.singletonList( gene );
        when( dao.thawLite( hits ) ).thenReturn( hits );
        assertSame( hits, service.thawLite( hits ) );
        verify( dao ).thawLite( hits );
    }

    @Test
    public void testThawAliasesDelegates() {
        when( dao.thawAliases( gene ) ).thenReturn( gene );
        assertSame( gene, service.thawAliases( gene ) );
        verify( dao ).thawAliases( gene );
    }

    @Test
    public void testThawLiterDelegates() {
        when( dao.thawLiter( gene ) ).thenReturn( gene );
        assertSame( gene, service.thawLiter( gene ) );
        verify( dao ).thawLiter( gene );
    }
}
