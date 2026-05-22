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
import ubic.gemma.model.common.auditAndSecurity.eventType.PreboardingCreatedEvent;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.PreboardingExperiment;
import ubic.gemma.model.expression.experiment.WorkflowState;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService;

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
 * Pure-Mockito unit tests for {@link PreboardingExperimentServiceImpl}: the
 * 409-on-existing logic in createPreboarding, the promotion FK-rebind +
 * workflow-state advance.
 */
@ExtendWith(MockitoExtension.class)
public class PreboardingExperimentServiceTest {

    @Mock
    private SessionFactory sessionFactory;
    @Mock
    private Session session;
    @Mock
    private AgentProposalService agentProposalService;
    @Mock
    private ExpressionExperimentService expressionExperimentService;
    @Mock
    private AuditTrailService auditTrailService;

    @InjectMocks
    private PreboardingExperimentServiceImpl service;

    @BeforeEach
    public void setUp() {
        lenient().when( sessionFactory.getCurrentSession() ).thenReturn( session );
    }

    @SuppressWarnings("unchecked")
    private Query<PreboardingExperiment> mockEmptyAccessionLookup() {
        Query<PreboardingExperiment> q = ( Query<PreboardingExperiment> ) org.mockito.Mockito.mock( Query.class );
        when( q.setParameter( anyString(), any() ) ).thenReturn( q );
        when( q.setMaxResults( org.mockito.ArgumentMatchers.anyInt() ) ).thenReturn( q );
        when( q.list() ).thenReturn( Collections.emptyList() );
        when( session.createQuery( anyString() ) ).thenReturn( ( Query ) q );
        return q;
    }

    @Test
    public void createPreboarding_freshAccessionPersistsAndEmitsEvent() throws Exception {
        mockEmptyAccessionLookup();
        when( expressionExperimentService.findOneByAccession( "GSE1" ) ).thenReturn( null );

        PreboardingExperiment result = service.createPreboarding( "GSE1", "GEO", "{\"title\":\"x\"}" );

        assertThat( result.getAccession() ).isEqualTo( "GSE1" );
        assertThat( result.getSource() ).isEqualTo( "GEO" );
        assertThat( result.getIdentifyingMetadata() ).contains( "title" );
        assertThat( result.getWorkflowState() ).isEqualTo( WorkflowState.Preboarding );
        verify( session ).persist( result );
        verify( auditTrailService ).addUpdateEvent( eq( result ), eq( PreboardingCreatedEvent.class ),
                anyString() );
    }

    @Test
    public void createPreboarding_defaultsSourceWhenNull() throws Exception {
        mockEmptyAccessionLookup();
        when( expressionExperimentService.findOneByAccession( anyString() ) ).thenReturn( null );

        PreboardingExperiment result = service.createPreboarding( "GSE2", null, null );
        assertThat( result.getSource() ).isEqualTo( "GEO" );
    }

    @Test
    public void createPreboarding_existingPreboardingThrows409() {
        PreboardingExperiment existing = new PreboardingExperiment();
        existing.setId( 11L );
        existing.setAccession( "GSE3" );
        @SuppressWarnings("unchecked")
        Query<PreboardingExperiment> q = ( Query<PreboardingExperiment> ) org.mockito.Mockito.mock( Query.class );
        when( q.setParameter( anyString(), any() ) ).thenReturn( q );
        when( q.setMaxResults( org.mockito.ArgumentMatchers.anyInt() ) ).thenReturn( q );
        when( q.list() ).thenReturn( Collections.singletonList( existing ) );
        when( session.createQuery( anyString() ) ).thenReturn( ( Query ) q );

        assertThatThrownBy( () -> service.createPreboarding( "GSE3", "GEO", null ) )
                .isInstanceOf( PreboardingExperimentService.AccessionAlreadyExistsException.class )
                .satisfies( ex -> {
                    PreboardingExperimentService.AccessionAlreadyExistsException e =
                            ( PreboardingExperimentService.AccessionAlreadyExistsException ) ex;
                    assertThat( e.getExistingId() ).isEqualTo( 11L );
                    assertThat( e.getExistingType() ).isEqualTo( "preboarding" );
                } );
    }

    @Test
    public void createPreboarding_existingExpressionExperimentThrows409() {
        mockEmptyAccessionLookup();
        ExpressionExperiment ee = new ExpressionExperiment();
        ee.setId( 22L );
        when( expressionExperimentService.findOneByAccession( "GSE4" ) ).thenReturn( ee );

        assertThatThrownBy( () -> service.createPreboarding( "GSE4", "GEO", null ) )
                .isInstanceOf( PreboardingExperimentService.AccessionAlreadyExistsException.class )
                .satisfies( ex -> {
                    PreboardingExperimentService.AccessionAlreadyExistsException e =
                            ( PreboardingExperimentService.AccessionAlreadyExistsException ) ex;
                    assertThat( e.getExistingId() ).isEqualTo( 22L );
                    assertThat( e.getExistingType() ).isEqualTo( "expression_experiment" );
                } );
    }

    @Test
    public void createPreboarding_blankAccessionThrows() {
        assertThatThrownBy( () -> service.createPreboarding( "", "GEO", null ) )
                .isInstanceOf( IllegalArgumentException.class );
        assertThatThrownBy( () -> service.createPreboarding( null, "GEO", null ) )
                .isInstanceOf( IllegalArgumentException.class );
    }

    @Test
    public void promote_rebindsProposalsAndAdvancesState() throws Exception {
        PreboardingExperiment skel = new PreboardingExperiment();
        skel.setId( 7L );
        skel.setWorkflowState( WorkflowState.Preboarding );

        ExpressionExperiment ee = new ExpressionExperiment();
        ee.setId( 99L );
        ee.setWorkflowState( WorkflowState.Preboarding );

        when( agentProposalService.rebindInvestigation( skel, ee ) ).thenReturn( 3 );

        PreboardingExperimentService.PromotionResult r = service.promote( ee, skel );

        assertThat( r.getPreboardingId() ).isEqualTo( 7L );
        assertThat( r.getEeId() ).isEqualTo( 99L );
        assertThat( r.getProposalsRebound() ).isEqualTo( 3 );
        assertThat( skel.getWorkflowState() ).isEqualTo( WorkflowState.Loaded );
        assertThat( ee.getWorkflowState() ).isEqualTo( WorkflowState.Loaded );
        verify( session ).update( skel );
        verify( session ).update( ee );
    }

    @Test
    public void promote_alreadyPromotedThrows() {
        PreboardingExperiment skel = new PreboardingExperiment();
        skel.setId( 8L );
        skel.setWorkflowState( WorkflowState.Loaded );
        ExpressionExperiment ee = new ExpressionExperiment();
        ee.setId( 100L );
        assertThatThrownBy( () -> service.promote( ee, skel ) )
                .isInstanceOf( PreboardingExperimentService.PreboardingAlreadyPromotedException.class );
    }

    @Test
    public void promote_doesNotRegressEeAlreadyCurated() throws Exception {
        PreboardingExperiment skel = new PreboardingExperiment();
        skel.setId( 9L );
        skel.setWorkflowState( WorkflowState.Preboarding );
        ExpressionExperiment ee = new ExpressionExperiment();
        ee.setId( 101L );
        ee.setWorkflowState( WorkflowState.Curate );

        when( agentProposalService.rebindInvestigation( skel, ee ) ).thenReturn( 0 );

        service.promote( ee, skel );
        // EE state must not be regressed by promotion.
        assertThat( ee.getWorkflowState() ).isEqualTo( WorkflowState.Curate );
    }
}
