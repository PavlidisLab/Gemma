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

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDecisionManager;
import org.springframework.test.util.ReflectionTestUtils;
import ubic.gemma.core.analysis.service.ArrayDesignAnnotationService;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.Chromosome;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.designElement.CompositeSequenceService;
import ubic.gemma.persistence.service.genome.biosequence.BioSequenceService;
import ubic.gemma.persistence.service.genome.gene.GeneService;
import ubic.gemma.persistence.service.genome.sequenceAnalysis.BlatResultService;
import ubic.gemma.rest.util.args.CompositeSequenceArg;
import ubic.gemma.rest.util.args.CompositeSequenceArgService;
import ubic.gemma.rest.util.args.PlatformArg;
import ubic.gemma.rest.util.args.PlatformArgService;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;

/**
 * Pure-Mockito test for {@code GET /platforms/{platform}/elements/{probe}/pslTrack}, which replaces
 * the retired gemma-web {@code BlatResultTrackController} ({@code blatTrack.html?id=}).
 */
@ExtendWith(MockitoExtension.class)
public class PlatformsWebServicePslTrackTest {

    private static final String HOST = "https://gemma2.msl.ubc.ca";

    @Mock
    @SuppressWarnings("unused")
    private GeneService geneService;
    @Mock
    @SuppressWarnings("unused")
    private ArrayDesignService arrayDesignService;
    @Mock
    @SuppressWarnings("unused")
    private CompositeSequenceService compositeSequenceService;
    @Mock
    @SuppressWarnings("unused")
    private ArrayDesignAnnotationService annotationFileService;
    @Mock
    private PlatformArgService arrayDesignArgService;
    @Mock
    private CompositeSequenceArgService probeArgService;
    @Mock
    @SuppressWarnings("unused")
    private AccessDecisionManager accessDecisionManager;
    @Mock
    @SuppressWarnings("unused")
    private TicketsWebService ticketsWebService;
    @Mock
    private BioSequenceService bioSequenceService;
    @Mock
    private BlatResultService blatResultService;

    @InjectMocks
    private PlatformsWebService webService;

    private PlatformArg<?> platformArg;
    private CompositeSequenceArg<?> probeArg;
    private ArrayDesign platform;
    private CompositeSequence probe;
    private BioSequence bioSequence;

    @BeforeEach
    public void setUp() {
        ReflectionTestUtils.setField( webService, "hostUrl", HOST );
        platformArg = PlatformArg.valueOf( "42" );
        probeArg = CompositeSequenceArg.valueOf( "AFFX_a" );
        platform = new ArrayDesign();
        platform.setId( 42L );
        platform.setShortName( "GPL1355" );
        probe = new CompositeSequence();
        probe.setId( 9L );
        probe.setName( "AFFX_a" );
        bioSequence = BioSequence.Factory.newInstance();
        bioSequence.setId( 3L );
        bioSequence.setName( "AFFX_a" );
        bioSequence.setLength( 500L );

        when( arrayDesignArgService.getEntity( any( PlatformArg.class ) ) ).thenReturn( platform );
        when( probeArgService.getEntityWithPlatform( any( CompositeSequenceArg.class ), any( ArrayDesign.class ) ) )
                .thenReturn( probe );
    }

    private BlatResult alignment( @org.springframework.lang.Nullable String chromosome, int matches ) {
        BlatResult br = BlatResult.Factory.newInstance();
        br.setId( 5L );
        br.setQuerySequence( bioSequence );
        if ( chromosome != null ) {
            Taxon taxon = Taxon.Factory.newInstance();
            BioSequence chrSeq = BioSequence.Factory.newInstance();
            chrSeq.setLength( 248956422L );
            br.setTargetChromosome( Chromosome.Factory.newInstance( chromosome, null, chrSeq, taxon ) );
        }
        br.setMatches( matches );
        br.setMismatches( 2 );
        br.setRepMatches( 0 );
        br.setNs( 0 );
        br.setQueryGapCount( 0 );
        br.setQueryGapBases( 0 );
        br.setTargetGapCount( 0 );
        br.setTargetGapBases( 0 );
        br.setStrand( "+" );
        br.setQueryStart( 0 );
        br.setQueryEnd( 480 );
        br.setTargetStart( 100000L );
        br.setTargetEnd( 100500L );
        br.setBlockCount( 1 );
        br.setBlockSizes( "480," );
        br.setQueryStarts( "0," );
        br.setTargetStarts( "100000," );
        return br;
    }

    @SuppressWarnings("unchecked")
    private void stubAlignments( Collection<BlatResult> alignments ) {
        when( bioSequenceService.findByCompositeSequence( probe ) ).thenReturn( bioSequence );
        when( blatResultService.findByBioSequence( bioSequence ) ).thenReturn( alignments );
        when( blatResultService.thaw( anyCollection() ) ).thenAnswer( i -> i.getArgument( 0 ) );
    }

    @Test
    public void pslTrackIsServedAsPlainTextNamedAfterTheProbe() {
        stubAlignments( Collections.singletonList( alignment( "1", 470 ) ) );

        Response response = webService.getPlatformElementPslTrack( platformArg, probeArg, false );

        assertThat( response.getStatus() ).isEqualTo( 200 );
        assertThat( response.getMediaType() ).isEqualTo( PlatformsWebService.TEXT_PLAIN_UTF8_TYPE );
        assertThat( response.getHeaderString( "Content-Disposition" ) ).isNull();
        String track = ( String ) response.getEntity();
        assertThat( track ).startsWith( "## Generated by Gemma (" + HOST + ")\n" );
        assertThat( track ).contains( "browser position chr1:99000-101500\n" );
        assertThat( track ).contains( "track name=\"AFFX_a\"" );
    }

    /**
     * The whole point of keying on the probe rather than on a BLAT result id: every alignment of the
     * probe belongs in one track, so the user sees them together.
     */
    @Test
    public void everyAlignmentOfTheProbeLandsInTheOneTrack() {
        stubAlignments( Arrays.asList( alignment( "1", 470 ), alignment( "7", 300 ) ) );

        String track = ( String ) webService.getPlatformElementPslTrack( platformArg, probeArg, false ).getEntity();

        assertThat( track.split( "\n" ) ).hasSize( 5 ); // provenance, position, track, 2 psl lines
    }

    /**
     * An alignment with no target chromosome cannot be placed in the browser. It is dropped rather
     * than allowed to fail the whole track, so a probe with one bad alignment still renders.
     */
    @Test
    public void unplaceableAlignmentsAreDroppedRatherThanFailingTheTrack() {
        stubAlignments( Arrays.asList( alignment( null, 470 ), alignment( "7", 300 ) ) );

        String track = ( String ) webService.getPlatformElementPslTrack( platformArg, probeArg, false ).getEntity();

        assertThat( track.split( "\n" ) ).hasSize( 4 );
        assertThat( track ).contains( "browser position chr7:" );
    }

    @Test
    public void downloadServesAnAttachmentNamedForTheProbe() {
        stubAlignments( Collections.singletonList( alignment( "1", 470 ) ) );

        Response response = webService.getPlatformElementPslTrack( platformArg, probeArg, true );

        assertThat( response.getMediaType() ).isEqualTo( MediaType.APPLICATION_OCTET_STREAM_TYPE );
        assertThat( response.getHeaderString( "Content-Disposition" ) )
                .isEqualTo( "attachment; filename=\"AFFX_a.psl\"" );
    }

    @Test
    public void probeWithNoAlignmentsIsA404() {
        stubAlignments( Collections.emptyList() );

        assertThatThrownBy( () -> webService.getPlatformElementPslTrack( platformArg, probeArg, false ) )
                .isInstanceOf( NotFoundException.class )
                .hasMessageContaining( "AFFX_a" );
    }

    /**
     * Only unplaceable alignments is a 404 too -- there is nothing to show in the browser, and an
     * empty track would read as "aligned nowhere" rather than "cannot be placed".
     */
    @Test
    public void probeWithOnlyUnplaceableAlignmentsIsA404() {
        stubAlignments( Collections.singletonList( alignment( null, 470 ) ) );

        assertThatThrownBy( () -> webService.getPlatformElementPslTrack( platformArg, probeArg, false ) )
                .isInstanceOf( NotFoundException.class );
    }

    @Test
    public void probeWithNoBiologicalCharacteristicIsA404() {
        when( bioSequenceService.findByCompositeSequence( probe ) ).thenReturn( null );

        assertThatThrownBy( () -> webService.getPlatformElementPslTrack( platformArg, probeArg, false ) )
                .isInstanceOf( NotFoundException.class );
    }
}
