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
import ubic.gemma.model.common.auditAndSecurity.eventType.SkeletonCreatedEvent;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.SkeletonInvestigation;
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
 * Pure-Mockito unit tests for {@link SkeletonInvestigationServiceImpl}: the
 * 409-on-existing logic in createSkeleton, the promotion FK-rebind +
 * workflow-state advance.
 */
@ExtendWith(MockitoExtension.class)
public class SkeletonInvestigationServiceTest {

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
    private SkeletonInvestigationServiceImpl service;

    @BeforeEach
    public void setUp() {
        lenient().when( sessionFactory.getCurrentSession() ).thenReturn( session );
    }

    @SuppressWarnings("unchecked")
    private Query<SkeletonInvestigation> mockEmptyAccessionLookup() {
        Query<SkeletonInvestigation> q = ( Query<SkeletonInvestigation> ) org.mockito.Mockito.mock( Query.class );
        when( q.setParameter( anyString(), any() ) ).thenReturn( q );
        when( q.setMaxResults( org.mockito.ArgumentMatchers.anyInt() ) ).thenReturn( q );
        when( q.list() ).thenReturn( Collections.emptyList() );
        when( session.createQuery( anyString() ) ).thenReturn( ( Query ) q );
        return q;
    }

    @Test
    public void createSkeleton_freshAccessionPersistsAndEmitsEvent() throws Exception {
        mockEmptyAccessionLookup();
        when( expressionExperimentService.findOneByAccession( "GSE1" ) ).thenReturn( null );

        SkeletonInvestigation result = service.createSkeleton( "GSE1", "GEO", "{\"title\":\"x\"}" );

        assertThat( result.getAccession() ).isEqualTo( "GSE1" );
        assertThat( result.getSource() ).isEqualTo( "GEO" );
        assertThat( result.getIdentifyingMetadata() ).contains( "title" );
        assertThat( result.getWorkflowState() ).isEqualTo( WorkflowState.Skeleton );
        verify( session ).persist( result );
        verify( auditTrailService ).addUpdateEvent( eq( result ), eq( SkeletonCreatedEvent.class ),
                anyString() );
    }

    @Test
    public void createSkeleton_defaultsSourceWhenNull() throws Exception {
        mockEmptyAccessionLookup();
        when( expressionExperimentService.findOneByAccession( anyString() ) ).thenReturn( null );

        SkeletonInvestigation result = service.createSkeleton( "GSE2", null, null );
        assertThat( result.getSource() ).isEqualTo( "GEO" );
    }

    @Test
    public void createSkeleton_existingSkeletonThrows409() {
        SkeletonInvestigation existing = new SkeletonInvestigation();
        existing.setId( 11L );
        existing.setAccession( "GSE3" );
        @SuppressWarnings("unchecked")
        Query<SkeletonInvestigation> q = ( Query<SkeletonInvestigation> ) org.mockito.Mockito.mock( Query.class );
        when( q.setParameter( anyString(), any() ) ).thenReturn( q );
        when( q.setMaxResults( org.mockito.ArgumentMatchers.anyInt() ) ).thenReturn( q );
        when( q.list() ).thenReturn( Collections.singletonList( existing ) );
        when( session.createQuery( anyString() ) ).thenReturn( ( Query ) q );

        assertThatThrownBy( () -> service.createSkeleton( "GSE3", "GEO", null ) )
                .isInstanceOf( SkeletonInvestigationService.AccessionAlreadyExistsException.class )
                .satisfies( ex -> {
                    SkeletonInvestigationService.AccessionAlreadyExistsException e =
                            ( SkeletonInvestigationService.AccessionAlreadyExistsException ) ex;
                    assertThat( e.getExistingId() ).isEqualTo( 11L );
                    assertThat( e.getExistingType() ).isEqualTo( "skeleton" );
                } );
    }

    @Test
    public void createSkeleton_existingExpressionExperimentThrows409() {
        mockEmptyAccessionLookup();
        ExpressionExperiment ee = new ExpressionExperiment();
        ee.setId( 22L );
        when( expressionExperimentService.findOneByAccession( "GSE4" ) ).thenReturn( ee );

        assertThatThrownBy( () -> service.createSkeleton( "GSE4", "GEO", null ) )
                .isInstanceOf( SkeletonInvestigationService.AccessionAlreadyExistsException.class )
                .satisfies( ex -> {
                    SkeletonInvestigationService.AccessionAlreadyExistsException e =
                            ( SkeletonInvestigationService.AccessionAlreadyExistsException ) ex;
                    assertThat( e.getExistingId() ).isEqualTo( 22L );
                    assertThat( e.getExistingType() ).isEqualTo( "expression_experiment" );
                } );
    }

    @Test
    public void createSkeleton_blankAccessionThrows() {
        assertThatThrownBy( () -> service.createSkeleton( "", "GEO", null ) )
                .isInstanceOf( IllegalArgumentException.class );
        assertThatThrownBy( () -> service.createSkeleton( null, "GEO", null ) )
                .isInstanceOf( IllegalArgumentException.class );
    }

    @Test
    public void promote_rebindsProposalsAndAdvancesState() throws Exception {
        SkeletonInvestigation skel = new SkeletonInvestigation();
        skel.setId( 7L );
        skel.setWorkflowState( WorkflowState.Skeleton );

        ExpressionExperiment ee = new ExpressionExperiment();
        ee.setId( 99L );
        ee.setWorkflowState( WorkflowState.Skeleton );

        when( agentProposalService.rebindInvestigation( skel, ee ) ).thenReturn( 3 );

        SkeletonInvestigationService.PromotionResult r = service.promote( ee, skel );

        assertThat( r.getSkeletonId() ).isEqualTo( 7L );
        assertThat( r.getEeId() ).isEqualTo( 99L );
        assertThat( r.getProposalsRebound() ).isEqualTo( 3 );
        assertThat( skel.getWorkflowState() ).isEqualTo( WorkflowState.Loaded );
        assertThat( ee.getWorkflowState() ).isEqualTo( WorkflowState.Loaded );
        verify( session ).update( skel );
        verify( session ).update( ee );
    }

    @Test
    public void promote_alreadyPromotedThrows() {
        SkeletonInvestigation skel = new SkeletonInvestigation();
        skel.setId( 8L );
        skel.setWorkflowState( WorkflowState.Loaded );
        ExpressionExperiment ee = new ExpressionExperiment();
        ee.setId( 100L );
        assertThatThrownBy( () -> service.promote( ee, skel ) )
                .isInstanceOf( SkeletonInvestigationService.SkeletonAlreadyPromotedException.class );
    }

    @Test
    public void promote_doesNotRegressEeAlreadyCurated() throws Exception {
        SkeletonInvestigation skel = new SkeletonInvestigation();
        skel.setId( 9L );
        skel.setWorkflowState( WorkflowState.Skeleton );
        ExpressionExperiment ee = new ExpressionExperiment();
        ee.setId( 101L );
        ee.setWorkflowState( WorkflowState.Curate );

        when( agentProposalService.rebindInvestigation( skel, ee ) ).thenReturn( 0 );

        service.promote( ee, skel );
        // EE state must not be regressed by promotion.
        assertThat( ee.getWorkflowState() ).isEqualTo( WorkflowState.Curate );
    }
}
