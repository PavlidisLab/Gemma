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
package ubic.gemma.rest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.common.protocol.Protocol;
import ubic.gemma.model.expression.arrayDesign.AlternateName;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.common.description.CharacteristicReadService;
import ubic.gemma.persistence.service.common.protocol.ProtocolReadService;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentSetService;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;
import ubic.gemma.rest.CompletionsWebService.CompletionValueObject;
import ubic.gemma.rest.util.ResponseDataObject;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Pure Mockito unit tests for {@link CompletionsWebService}. Verifies prefix matching,
 * dedup, limit clamping, and per-type fan-out without standing up Jersey.
 */
@ExtendWith(MockitoExtension.class)
public class CompletionsWebServiceTest {

    @Mock
    private TaxonService taxonService;
    @Mock
    private ArrayDesignService arrayDesignService;
    @Mock
    private ExpressionExperimentService expressionExperimentService;
    @Mock
    private ExpressionExperimentSetService expressionExperimentSetService;
    @Mock
    private ProtocolReadService protocolReadService;
    @Mock
    private CharacteristicReadService characteristicService;

    @InjectMocks
    private CompletionsWebService webService;

    // ---- taxa --------------------------------------------------------------

    @Test
    public void testTaxonCompletionsFanOut() {
        Taxon t = new Taxon();
        t.setId( 1L );
        t.setNcbiId( 9606 );
        t.setCommonName( "human" );
        t.setScientificName( "Homo sapiens" );
        when( taxonService.loadAll() ).thenReturn( Collections.singleton( t ) );

        ResponseDataObject<List<CompletionValueObject>> resp = webService.getTaxonCompletions( "", 50 );

        assertThat( resp.getData() )
                .extracting( CompletionValueObject::getValue )
                .containsExactly( "1", "9606", "human", "Homo sapiens" );
        assertThat( resp.getData() )
                .extracting( CompletionValueObject::getDescription )
                .containsOnly( "Homo sapiens" );
    }

    @Test
    public void testTaxonCompletionsCaseInsensitivePrefix() {
        Taxon human = new Taxon();
        human.setId( 1L );
        human.setCommonName( "human" );
        human.setScientificName( "Homo sapiens" );
        Taxon mouse = new Taxon();
        mouse.setId( 2L );
        mouse.setCommonName( "mouse" );
        mouse.setScientificName( "Mus musculus" );
        when( taxonService.loadAll() ).thenReturn( Arrays.asList( human, mouse ) );

        ResponseDataObject<List<CompletionValueObject>> resp = webService.getTaxonCompletions( "HU", 50 );

        assertThat( resp.getData() )
                .extracting( CompletionValueObject::getValue )
                .containsExactly( "human" );
    }

    @Test
    public void testTaxonCompletionsLimitClampedToMax() {
        // Request way above MAX_LIMIT — builder clamps; we don't return more than MAX_LIMIT.
        Taxon t = new Taxon();
        t.setId( 1L );
        t.setScientificName( "Homo sapiens" );
        when( taxonService.loadAll() ).thenReturn( Collections.singleton( t ) );

        ResponseDataObject<List<CompletionValueObject>> resp = webService.getTaxonCompletions( "", 10_000 );

        // Single taxon contributes 2 entries (id + scientificName); the clamp affects ceiling, not actual.
        assertThat( resp.getData() ).hasSize( 2 );
    }

    // ---- platforms ---------------------------------------------------------

    @Test
    public void testPlatformCompletionsIncludeAlternateNames() {
        ArrayDesign ad = ArrayDesign.Factory.newInstance();
        ad.setId( 10L );
        ad.setShortName( "GPL570" );
        ad.setName( "Affymetrix Human Genome U133 Plus 2.0 Array" );
        AlternateName altA = AlternateName.Factory.newInstance();
        altA.setName( "HG-U133_Plus_2" );
        AlternateName altB = AlternateName.Factory.newInstance();
        altB.setName( "U133_Plus_2.0" );
        ad.setAlternateNames( new LinkedHashSet<>( Arrays.asList( altA, altB ) ) );
        when( arrayDesignService.loadAll() ).thenReturn( Collections.singleton( ad ) );

        ResponseDataObject<List<CompletionValueObject>> resp = webService.getPlatformCompletions( "", 50, false );

        assertThat( resp.getData() )
                .extracting( CompletionValueObject::getValue )
                .containsExactlyInAnyOrder( "10", "GPL570",
                        "Affymetrix Human Genome U133 Plus 2.0 Array",
                        "HG-U133_Plus_2", "U133_Plus_2.0" );
    }

    @Test
    public void testPlatformCompletionsGenericFlagSwitchesSource() {
        when( arrayDesignService.loadAllGenericGenePlatforms() ).thenReturn( Collections.emptyList() );

        ResponseDataObject<List<CompletionValueObject>> resp = webService.getPlatformCompletions( "", 50, true );

        assertThat( resp.getData() ).isEmpty();
    }

    @Test
    public void testPlatformCompletionsPrefixOnShortName() {
        ArrayDesign ad = ArrayDesign.Factory.newInstance();
        ad.setId( 10L );
        ad.setShortName( "GPL570" );
        ad.setName( "U133 Plus 2.0" );
        when( arrayDesignService.loadAll() ).thenReturn( Collections.singleton( ad ) );

        ResponseDataObject<List<CompletionValueObject>> resp = webService.getPlatformCompletions( "GPL", 50, false );

        assertThat( resp.getData() )
                .extracting( CompletionValueObject::getValue )
                .containsExactly( "GPL570" );
    }

    // ---- protocols ---------------------------------------------------------

    @Test
    public void testProtocolCompletionsDedup() {
        Protocol p = Protocol.Factory.newInstance();
        p.setId( 7L );
        p.setName( "Some Protocol" );
        when( protocolReadService.loadAllUniqueByName() ).thenReturn( Collections.singletonList( p ) );

        ResponseDataObject<List<CompletionValueObject>> resp = webService.getProtocolCompletions( "", 50 );

        assertThat( resp.getData() )
                .extracting( CompletionValueObject::getValue )
                .containsExactly( "7", "Some Protocol" );
    }

    // ---- dataset groups ----------------------------------------------------

    @Test
    public void testDatasetGroupCompletions() {
        ExpressionExperimentSet g = new ExpressionExperimentSet();
        g.setId( 100L );
        g.setName( "brain-aging" );
        when( expressionExperimentSetService.loadAll() ).thenReturn( Collections.singleton( g ) );

        ResponseDataObject<List<CompletionValueObject>> resp = webService.getDatasetGroupCompletions( "brain", 50 );

        assertThat( resp.getData() )
                .extracting( CompletionValueObject::getValue )
                .containsExactly( "brain-aging" );
    }

    // ---- datasets ----------------------------------------------------------

    @Test
    public void testDatasetCompletionsHonourLimit() {
        TreeMap<String, String> ids = new TreeMap<>();
        ids.put( "GSE100", "GSE100" );
        ids.put( "GSE101", "GSE101" );
        ids.put( "GSE102", "GSE102" );
        ids.put( "GSE103", "GSE103" );
        when( expressionExperimentService.loadAllIdentifiersAndName( false ) ).thenReturn( ids );

        ResponseDataObject<List<CompletionValueObject>> resp = webService.getDatasetCompletions( "GSE10", 2 );

        assertThat( resp.getData() ).hasSize( 2 );
        assertThat( resp.getData() )
                .extracting( CompletionValueObject::getValue )
                .containsExactly( "GSE100", "GSE101" );
    }

    @Test
    public void testDatasetCompletionsEmptyPrefixReturnsAll() {
        TreeMap<String, String> ids = new TreeMap<>();
        ids.put( "GSE1", "GSE1" );
        ids.put( "GSE2", "GSE2" );
        when( expressionExperimentService.loadAllIdentifiersAndName( false ) ).thenReturn( ids );

        ResponseDataObject<List<CompletionValueObject>> resp = webService.getDatasetCompletions( "", 50 );

        assertThat( resp.getData() )
                .extracting( CompletionValueObject::getValue )
                .containsExactlyInAnyOrder( "GSE1", "GSE2" );
    }

    // ---- ontology terms ----------------------------------------------------

    @Test
    public void testOntologyTermCompletionsExpandToTermId() {
        TreeMap<String, String> uriToLabel = new TreeMap<>();
        uriToLabel.put( "http://purl.obolibrary.org/obo/CL_0000084", "T cell" );
        when( characteristicService.findValueGroupedByValueUri( null, true, false, true, -1 ) )
                .thenReturn( uriToLabel );

        ResponseDataObject<List<CompletionValueObject>> resp = webService.getOntologyTermCompletions( "", 50 );

        assertThat( resp.getData() )
                .extracting( CompletionValueObject::getValue )
                .contains( "http://purl.obolibrary.org/obo/CL_0000084", "CL:0000084" );
    }

    @Test
    public void testOntologyTermCompletionsPrefixMatchesOboId() {
        TreeMap<String, String> uriToLabel = new TreeMap<>();
        uriToLabel.put( "http://purl.obolibrary.org/obo/CL_0000084", "T cell" );
        uriToLabel.put( "http://purl.obolibrary.org/obo/UBERON_0002107", "liver" );
        when( characteristicService.findValueGroupedByValueUri( null, true, false, true, -1 ) )
                .thenReturn( uriToLabel );

        ResponseDataObject<List<CompletionValueObject>> resp = webService.getOntologyTermCompletions( "CL:", 50 );

        assertThat( resp.getData() )
                .extracting( CompletionValueObject::getValue )
                .containsExactly( "CL:0000084" );
    }

    /**
     * 🛑 The picker must not hand out a term the read path is going to rewrite.
     * <p>
     * The corpus stores {@code CLO_0007365} "LNCAP cell" and every read serves
     * {@code CLO_0037116} "LNCaP cell". Suggesting the stored spelling is how new rows in the
     * retired form kept being written: the curator picks it, it is saved verbatim, and the design
     * tab renders the other one back. Reported by uib as one value rendering two names, 2026-08-28.
     */
    @Test
    public void testTheRetiredSpellingIsNotSuggested() {
        TreeMap<String, String> uriToLabel = new TreeMap<>();
        uriToLabel.put( "http://purl.obolibrary.org/obo/CLO_0007365", "LNCAP cell" );
        when( characteristicService.findValueGroupedByValueUri( null, true, false, true, -1 ) )
                .thenReturn( uriToLabel );

        ResponseDataObject<List<CompletionValueObject>> resp = webService.getOntologyTermCompletions( "", 50 );

        assertThat( resp.getData() )
                .extracting( CompletionValueObject::getValue )
                .contains( "http://purl.obolibrary.org/obo/CLO_0037116", "CLO:0037116" )
                .doesNotContain( "http://purl.obolibrary.org/obo/CLO_0007365", "CLO:0007365" );
        assertThat( resp.getData() )
                .extracting( CompletionValueObject::getDescription )
                .as( "the label moves with the URI" )
                .contains( "LNCaP cell" );
    }

    /**
     * ...and typing the retired id still finds the term. Matching on the canonical spelling alone
     * would answer a well-formed lookup with nothing at all.
     */
    @Test
    public void testTypingTheRetiredIdStillFindsTheTerm() {
        TreeMap<String, String> uriToLabel = new TreeMap<>();
        uriToLabel.put( "http://purl.obolibrary.org/obo/CLO_0007365", "LNCAP cell" );
        when( characteristicService.findValueGroupedByValueUri( null, true, false, true, -1 ) )
                .thenReturn( uriToLabel );

        ResponseDataObject<List<CompletionValueObject>> resp = webService.getOntologyTermCompletions( "CLO:0007365", 50 );

        assertThat( resp.getData() )
                .extracting( CompletionValueObject::getValue )
                .containsExactly( "CLO:0037116" );
    }
}
