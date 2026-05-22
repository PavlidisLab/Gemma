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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ubic.gemma.model.common.description.AnnotationValueObject;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.persistence.service.genome.gene.GeneService;
import ubic.gemma.persistence.service.maintenance.TableMaintenanceUtil;
import ubic.gemma.rest.util.ResponseDataObject;
import ubic.gemma.rest.util.args.GeneArg;
import ubic.gemma.rest.util.args.GeneArgService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure-Mockito tests for the two gene-page additions on {@link GeneWebService}:
 * {@code GET /genes/{gene}/overview} (the fully-populated VO replacing the
 * legacy {@code loadGeneDetails} DWR) and {@code GET /genes/{gene}/homologues}.
 */
@ExtendWith(MockitoExtension.class)
public class GeneWebServiceOverviewTest {

    @Mock
    private GeneService geneService;
    @Mock
    private GeneArgService geneArgService;
    @Mock
    @SuppressWarnings("unused") // injected to satisfy @Autowired on the service
    private TableMaintenanceUtil tableMaintenanceUtil;

    @InjectMocks
    private GeneWebService webService;

    private Gene gene;
    private GeneArg<?> geneArg;

    @BeforeEach
    public void setUp() {
        gene = new Gene();
        gene.setId( 7L );
        gene.setOfficialSymbol( "BRCA1" );
        geneArg = GeneArg.valueOf( "BRCA1" );
    }

    /* ===== /genes/{gene}/overview ===== */

    @Test
    public void overviewReturnsFullyPopulatedVoWithGoTermCount() {
        GeneValueObject gvo = new GeneValueObject();
        gvo.setId( 7L );
        gvo.setOfficialSymbol( "BRCA1" );

        when( geneArgService.getEntity( any( GeneArg.class ) ) ).thenReturn( gene );
        when( geneService.loadFullyPopulatedValueObject( 7L ) ).thenReturn( gvo );
        AnnotationValueObject goTerm = new AnnotationValueObject();
        when( geneService.findGOTerms( 7L ) ).thenReturn( Arrays.asList( goTerm, new AnnotationValueObject() ) );

        ResponseDataObject<GeneValueObject> response = webService.getGeneOverview( geneArg );

        assertThat( response.getData() ).isSameAs( gvo );
        assertThat( response.getData().getNumGoTerms() ).isEqualTo( 2 );
    }

    @Test
    public void overviewRaceWithDeleteThrows404() {
        // getEntity resolves successfully (gene was present), but the fat-loader
        // call returns null (gene removed between the resolve and the load).
        when( geneArgService.getEntity( any( GeneArg.class ) ) ).thenReturn( gene );
        when( geneService.loadFullyPopulatedValueObject( 7L ) ).thenReturn( null );

        assertThatThrownBy( () -> webService.getGeneOverview( geneArg ) )
                .isInstanceOf( NotFoundException.class );
        verify( geneService, never() ).findGOTerms( any() );
    }

    /* ===== /genes/{gene}/homologues ===== */

    @Test
    public void homologuesReturnsListFromFullyPopulatedVo() {
        GeneValueObject gvo = new GeneValueObject();
        gvo.setId( 7L );

        GeneValueObject mouseHomolog = new GeneValueObject();
        mouseHomolog.setOfficialSymbol( "Brca1" );
        List<GeneValueObject> homologues = new ArrayList<>();
        homologues.add( mouseHomolog );
        gvo.setHomologues( homologues );

        when( geneArgService.getEntity( any( GeneArg.class ) ) ).thenReturn( gene );
        when( geneService.loadFullyPopulatedValueObject( 7L ) ).thenReturn( gvo );

        ResponseDataObject<Collection<GeneValueObject>> response = webService.getGeneHomologues( geneArg );

        assertThat( response.getData() ).hasSize( 1 );
        assertThat( response.getData().iterator().next().getOfficialSymbol() ).isEqualTo( "Brca1" );
    }

    @Test
    public void homologuesReturnsEmptyListWhenNoneRegistered() {
        GeneValueObject gvo = new GeneValueObject();
        gvo.setId( 7L );
        gvo.setHomologues( null );

        when( geneArgService.getEntity( any( GeneArg.class ) ) ).thenReturn( gene );
        when( geneService.loadFullyPopulatedValueObject( 7L ) ).thenReturn( gvo );

        ResponseDataObject<Collection<GeneValueObject>> response = webService.getGeneHomologues( geneArg );

        assertThat( response.getData() ).isEmpty();
    }

    @Test
    public void homologuesRaceWithDeleteThrows404() {
        when( geneArgService.getEntity( any( GeneArg.class ) ) ).thenReturn( gene );
        when( geneService.loadFullyPopulatedValueObject( 7L ) ).thenReturn( null );

        assertThatThrownBy( () -> webService.getGeneHomologues( geneArg ) )
                .isInstanceOf( NotFoundException.class );
    }
}
