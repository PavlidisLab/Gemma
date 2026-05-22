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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDecisionManager;
import ubic.gemma.core.analysis.service.ArrayDesignAnnotationService;
import ubic.gemma.model.analysis.sequence.GeneMappingSummary;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.CompositeSequenceValueObject;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.designElement.CompositeSequenceService;
import ubic.gemma.persistence.service.genome.gene.GeneService;
import ubic.gemma.rest.util.ResponseDataObject;
import ubic.gemma.rest.util.args.CompositeSequenceArg;
import ubic.gemma.rest.util.args.CompositeSequenceArgService;
import ubic.gemma.rest.util.args.PlatformArg;
import ubic.gemma.rest.util.args.PlatformArgService;

import java.util.Arrays;
import java.util.Collection;

import jakarta.ws.rs.NotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Pure-Mockito test for the
 * {@code GET /platforms/{platform}/elements/{probe}/mappingSummary} endpoint added
 * to {@link PlatformsWebService}. Replaces the legacy DWR
 * {@code CompositeSequenceController.getGeneMappingSummary} used by the gemma-web
 * gene-page Elements drill-down.
 */
@ExtendWith(MockitoExtension.class)
public class PlatformsWebServiceMappingSummaryTest {

    @Mock
    @SuppressWarnings("unused")
    private GeneService geneService;
    @Mock
    @SuppressWarnings("unused")
    private ArrayDesignService arrayDesignService;
    @Mock
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

    @InjectMocks
    private PlatformsWebService webService;

    private PlatformArg<?> platformArg;
    private CompositeSequenceArg<?> probeArg;
    private ArrayDesign platform;
    private CompositeSequence probe;

    @BeforeEach
    public void setUp() {
        platformArg = PlatformArg.valueOf( "42" );
        probeArg = CompositeSequenceArg.valueOf( "AFFX_a" );
        platform = new ArrayDesign();
        platform.setId( 42L );
        platform.setShortName( "GPL1355" );
        probe = new CompositeSequence();
        probe.setId( 9L );
        probe.setName( "AFFX_a" );
    }

    @Test
    public void mappingSummaryReturnsVoWithGeneMappingSummariesPopulated() {
        when( arrayDesignArgService.getEntity( any( PlatformArg.class ) ) ).thenReturn( platform );
        when( probeArgService.getEntityWithPlatform( any( CompositeSequenceArg.class ), any( ArrayDesign.class ) ) )
                .thenReturn( probe );

        CompositeSequenceValueObject vo = new CompositeSequenceValueObject();
        vo.setId( 9L );
        vo.setName( "AFFX_a" );
        GeneMappingSummary mapping = new GeneMappingSummary();
        vo.setGeneMappingSummaries( Arrays.asList( mapping ) );
        when( compositeSequenceService.loadValueObjectWithGeneMappingSummary( probe ) ).thenReturn( vo );

        ResponseDataObject<CompositeSequenceValueObject> response =
                webService.getPlatformElementMappingSummary( platformArg, probeArg );

        assertThat( response.getData() ).isSameAs( vo );
        Collection<GeneMappingSummary> summaries = response.getData().getGeneMappingSummaries();
        assertThat( summaries ).hasSize( 1 );
    }

    @Test
    public void mappingSummaryThrows404WhenServiceReturnsNull() {
        // Defensive: the service contract permits null (probe lookup miss after
        // platform-scoped resolve). Responders.respond(null) maps that to 404.
        when( arrayDesignArgService.getEntity( any( PlatformArg.class ) ) ).thenReturn( platform );
        when( probeArgService.getEntityWithPlatform( any( CompositeSequenceArg.class ), any( ArrayDesign.class ) ) )
                .thenReturn( probe );
        when( compositeSequenceService.loadValueObjectWithGeneMappingSummary( probe ) ).thenReturn( null );

        assertThatThrownBy( () -> webService.getPlatformElementMappingSummary( platformArg, probeArg ) )
                .isInstanceOf( NotFoundException.class );
    }
}
