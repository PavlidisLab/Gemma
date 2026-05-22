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

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.PreboardedExperiment;
import ubic.gemma.model.expression.experiment.WorkflowState;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure-Mockito unit tests for {@link PreboardedExperimentServiceImpl}: the
 * 409-on-existing logic in createPreboarded, the promotion FK-rebind +
 * workflow-state advance.
 */
@ExtendWith(MockitoExtension.class)
public class PreboardedExperimentServiceTest {

    @Mock
    private SessionFactory sessionFactory;
    @Mock
    private Session session;
    @Mock
    private AgentProposalService agentProposalService;
    @Mock
    private ExpressionExperimentService expressionExperimentService;
    @Mock
    private PreboardedAuditService preboardedAuditService;

    @InjectMocks
    private PreboardedExperimentServiceImpl service;

    @BeforeEach
    public void setUp() {
        lenient().when( sessionFactory.getCurrentSession() ).thenReturn( session );
    }

    @SuppressWarnings("unchecked")
    private Query<PreboardedExperiment> mockEmptyAccessionLookup() {
        Query<PreboardedExperiment> q = ( Query<PreboardedExperiment> ) org.mockito.Mockito.mock( Query.class );
        when( q.setParameter( anyString(), any() ) ).thenReturn( q );
        when( q.setMaxResults( org.mockito.ArgumentMatchers.anyInt() ) ).thenReturn( q );
        when( q.list() ).thenReturn( Collections.emptyList() );
        when( session.createQuery( anyString() ) ).thenReturn( ( Query ) q );
        return q;
    }

    @Test
    public void createPreboarded_freshAccessionPersistsAndEmitsEvent() throws Exception {
        mockEmptyAccessionLookup();
        when( expressionExperimentService.findOneByAccession( "GSE1" ) ).thenReturn( null );

        PreboardedExperiment result = service.createPreboarded( "GSE1", "GEO", "{\"title\":\"x\"}" );

        assertThat( result.getAccession() ).isEqualTo( "GSE1" );
        assertThat( result.getSource() ).isEqualTo( "GEO" );
        assertThat( result.getIdentifyingMetadata() ).contains( "title" );
        assertThat( result.getWorkflowState() ).isEqualTo( WorkflowState.Preboarded );
        verify( session ).persist( result );
        verify( preboardedAuditService ).recordPreboardedCreated( eq( result ), eq( "GSE1" ) );
    }

    @Test
    public void createPreboarded_defaultsSourceWhenNull() throws Exception {
        mockEmptyAccessionLookup();
        when( expressionExperimentService.findOneByAccession( anyString() ) ).thenReturn( null );

        PreboardedExperiment result = service.createPreboarded( "GSE2", null, null );
        assertThat( result.getSource() ).isEqualTo( "GEO" );
    }

    @Test
    public void createPreboarded_existingPreboardedThrows409() {
        PreboardedExperiment existing = new PreboardedExperiment();
        existing.setId( 11L );
        existing.setAccession( "GSE3" );
        @SuppressWarnings("unchecked")
        Query<PreboardedExperiment> q = ( Query<PreboardedExperiment> ) org.mockito.Mockito.mock( Query.class );
        when( q.setParameter( anyString(), any() ) ).thenReturn( q );
        when( q.setMaxResults( org.mockito.ArgumentMatchers.anyInt() ) ).thenReturn( q );
        when( q.list() ).thenReturn( Collections.singletonList( existing ) );
        when( session.createQuery( anyString() ) ).thenReturn( ( Query ) q );

        assertThatThrownBy( () -> service.createPreboarded( "GSE3", "GEO", null ) )
                .isInstanceOf( PreboardedExperimentService.AccessionAlreadyExistsException.class )
                .satisfies( ex -> {
                    PreboardedExperimentService.AccessionAlreadyExistsException e =
                            ( PreboardedExperimentService.AccessionAlreadyExistsException ) ex;
                    assertThat( e.getExistingId() ).isEqualTo( 11L );
                    assertThat( e.getExistingType() ).isEqualTo( "preboarded" );
                } );
    }

    @Test
    public void createPreboarded_existingExpressionExperimentThrows409() {
        mockEmptyAccessionLookup();
        ExpressionExperiment ee = new ExpressionExperiment();
        ee.setId( 22L );
        when( expressionExperimentService.findOneByAccession( "GSE4" ) ).thenReturn( ee );

        assertThatThrownBy( () -> service.createPreboarded( "GSE4", "GEO", null ) )
                .isInstanceOf( PreboardedExperimentService.AccessionAlreadyExistsException.class )
                .satisfies( ex -> {
                    PreboardedExperimentService.AccessionAlreadyExistsException e =
                            ( PreboardedExperimentService.AccessionAlreadyExistsException ) ex;
                    assertThat( e.getExistingId() ).isEqualTo( 22L );
                    assertThat( e.getExistingType() ).isEqualTo( "expression_experiment" );
                } );
    }

    @Test
    public void createPreboarded_blankAccessionThrows() {
        assertThatThrownBy( () -> service.createPreboarded( "", "GEO", null ) )
                .isInstanceOf( IllegalArgumentException.class );
        assertThatThrownBy( () -> service.createPreboarded( null, "GEO", null ) )
                .isInstanceOf( IllegalArgumentException.class );
    }

    @Test
    public void promote_rebindsProposalsAndAdvancesState() throws Exception {
        PreboardedExperiment skel = new PreboardedExperiment();
        skel.setId( 7L );
        skel.setWorkflowState( WorkflowState.Preboarded );

        ExpressionExperiment ee = new ExpressionExperiment();
        ee.setId( 99L );
        ee.setWorkflowState( WorkflowState.Preboarded );

        when( agentProposalService.rebindInvestigation( skel, ee ) ).thenReturn( 3 );

        PreboardedExperimentService.PromotionResult r = service.promote( ee, skel );

        assertThat( r.getPreboardedId() ).isEqualTo( 7L );
        assertThat( r.getEeId() ).isEqualTo( 99L );
        assertThat( r.getProposalsRebound() ).isEqualTo( 3 );
        assertThat( skel.getWorkflowState() ).isEqualTo( WorkflowState.Loaded );
        assertThat( ee.getWorkflowState() ).isEqualTo( WorkflowState.Loaded );
        verify( session ).update( skel );
        verify( session ).update( ee );
    }

    @Test
    public void promote_alreadyPromotedThrows() {
        PreboardedExperiment skel = new PreboardedExperiment();
        skel.setId( 8L );
        skel.setWorkflowState( WorkflowState.Loaded );
        ExpressionExperiment ee = new ExpressionExperiment();
        ee.setId( 100L );
        assertThatThrownBy( () -> service.promote( ee, skel ) )
                .isInstanceOf( PreboardedExperimentService.PreboardedAlreadyPromotedException.class );
    }

    @Test
    public void promote_doesNotRegressEeAlreadyCurated() throws Exception {
        PreboardedExperiment skel = new PreboardedExperiment();
        skel.setId( 9L );
        skel.setWorkflowState( WorkflowState.Preboarded );
        ExpressionExperiment ee = new ExpressionExperiment();
        ee.setId( 101L );
        ee.setWorkflowState( WorkflowState.Curate );

        when( agentProposalService.rebindInvestigation( skel, ee ) ).thenReturn( 0 );

        service.promote( ee, skel );
        // EE state must not be regressed by promotion.
        assertThat( ee.getWorkflowState() ).isEqualTo( WorkflowState.Curate );
    }
}
