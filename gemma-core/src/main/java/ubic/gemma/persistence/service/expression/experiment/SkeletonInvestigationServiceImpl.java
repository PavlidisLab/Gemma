/*
 * The Gemma project
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

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import ubic.gemma.core.security.audit.Audited;
import ubic.gemma.model.common.auditAndSecurity.eventType.SkeletonCreatedEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.SkeletonPromotedEvent;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.SkeletonInvestigation;
import ubic.gemma.model.expression.experiment.WorkflowState;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService;

import java.util.Date;
import java.util.List;

/**
 * Default {@link SkeletonInvestigationService} implementation.
 *
 * <p>The promotion path is the substantive piece: it calls into
 * {@link AgentProposalService#rebindInvestigation} to point every
 * {@code AgentProposal} row at the loaded EE rather than the skeleton, then
 * advances both rows' workflow state. Audit events are emitted declaratively
 * via {@link Audited @Audited} on the methods where the auditable target is
 * passed in (promote). The create path emits its event imperatively because
 * the auditable target is constructed in the method body and the
 * {@code AuditedAspect} can only locate auditables on the argument list
 * (see {@code AuditedAspect#findAuditable}).</p>
 */
@Service
public class SkeletonInvestigationServiceImpl implements SkeletonInvestigationService {

    private final SessionFactory sessionFactory;
    private final AgentProposalService agentProposalService;
    private final ExpressionExperimentService expressionExperimentService;
    private final AuditTrailService auditTrailService;

    @Autowired
    public SkeletonInvestigationServiceImpl( SessionFactory sessionFactory,
            AgentProposalService agentProposalService,
            ExpressionExperimentService expressionExperimentService,
            AuditTrailService auditTrailService ) {
        this.sessionFactory = sessionFactory;
        this.agentProposalService = agentProposalService;
        this.expressionExperimentService = expressionExperimentService;
        this.auditTrailService = auditTrailService;
    }

    @Override
    @Transactional
    public SkeletonInvestigation createSkeleton( String accession,
            @Nullable String source,
            @Nullable String identifyingMetadata )
            throws AccessionAlreadyExistsException {
        Assert.hasText( accession, "accession must be non-blank." );
        // Reject if either a SkeletonInvestigation OR an ExpressionExperiment
        // already carries this accession (handoff §"Required endpoints" 409).
        SkeletonInvestigation existingSkeleton = findByAccession( accession );
        if ( existingSkeleton != null ) {
            throw new AccessionAlreadyExistsException( accession,
                    existingSkeleton.getId(), "skeleton" );
        }
        ExpressionExperiment existingEe = findExpressionExperimentByAccession( accession );
        if ( existingEe != null ) {
            throw new AccessionAlreadyExistsException( accession,
                    existingEe.getId(), "expression_experiment" );
        }

        SkeletonInvestigation skel = new SkeletonInvestigation();
        skel.setAccession( accession );
        if ( source != null && !source.isEmpty() ) {
            skel.setSource( source );
        }
        skel.setIdentifyingMetadata( identifyingMetadata );
        skel.setName( "Skeleton:" + accession );
        skel.setWorkflowState( WorkflowState.Skeleton );
        skel.setWorkflowStateEnteredAt( new Date() );
        sessionFactory.getCurrentSession().persist( skel );
        sessionFactory.getCurrentSession().flush();

        // Imperative audit emission: the AuditedAspect can only locate
        // an Auditable target on the argument list, and `accession` (String)
        // is not auditable. The freshly persisted skeleton IS auditable;
        // emit the event directly so the same "one SkeletonCreatedEvent per
        // create" guarantee holds.
        //noinspection deprecation
        auditTrailService.addUpdateEvent( skel, SkeletonCreatedEvent.class,
                "Skeleton created for accession " + accession );
        return skel;
    }

    @Nullable
    @Override
    @Transactional(readOnly = true)
    public SkeletonInvestigation load( Long id ) {
        if ( id == null ) return null;
        return ( SkeletonInvestigation ) sessionFactory.getCurrentSession()
                .get( SkeletonInvestigation.class, id );
    }

    @Nullable
    @Override
    @Transactional(readOnly = true)
    public SkeletonInvestigation findByAccession( String accession ) {
        if ( accession == null ) return null;
        @SuppressWarnings("unchecked")
        List<SkeletonInvestigation> rows = sessionFactory.getCurrentSession()
                .createQuery( "from SkeletonInvestigation s where s.accession = :acc order by s.id asc" )
                .setParameter( "acc", accession )
                .setMaxResults( 1 )
                .list();
        return rows.isEmpty() ? null : rows.get( 0 );
    }

    @Override
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public List<SkeletonInvestigation> findAllByAccession( String accession ) {
        if ( accession == null ) return java.util.Collections.emptyList();
        return sessionFactory.getCurrentSession()
                .createQuery( "from SkeletonInvestigation s where s.accession = :acc order by s.id asc" )
                .setParameter( "acc", accession )
                .list();
    }

    @Nullable
    @Override
    @Transactional(readOnly = true)
    public ExpressionExperiment findExpressionExperimentByAccession( String accession ) {
        if ( accession == null ) return null;
        return expressionExperimentService.findOneByAccession( accession );
    }

    @Override
    @Transactional
    @Audited(value = SkeletonPromotedEvent.class,
            messageSpel = "'Skeleton#' + #skeleton.id + ' promoted to ExpressionExperiment#' + #ee.id"
                    + " + ' (proposals_rebound=' + #result.proposalsRebound + ')'")
    public PromotionResult promote( ExpressionExperiment ee, SkeletonInvestigation skeleton )
            throws SkeletonAlreadyPromotedException {
        Assert.notNull( skeleton, "skeleton must not be null." );
        Assert.notNull( ee, "ee must not be null." );
        if ( skeleton.getWorkflowState() == WorkflowState.Loaded
                || skeleton.getWorkflowState() == WorkflowState.Curate
                || skeleton.getWorkflowState() == WorkflowState.Process
                || skeleton.getWorkflowState() == WorkflowState.Audit
                || skeleton.getWorkflowState() == WorkflowState.Public ) {
            throw new SkeletonAlreadyPromotedException( skeleton.getId() );
        }

        // Rebind AgentProposal rows from skeleton -> ee. The promote endpoint
        // contract is "the historical AgentProposal rows accessible from the
        // EE; the audit trail intact" — rebind is the new-row + FK rebind
        // approach (see STATUS_PROPOSED_EXPERIMENT_WORKFLOW.md for the
        // trade-off discussion).
        int reboundCount = agentProposalService.rebindInvestigation( skeleton, ee );

        // Advance the skeleton's workflow state to Loaded (terminal marker;
        // the skeleton row is retained as history, no curatable artifacts
        // on it). The EE's workflow state likewise becomes Loaded if it
        // isn't already past it.
        Date now = new Date();
        skeleton.setWorkflowState( WorkflowState.Loaded );
        skeleton.setWorkflowStateEnteredAt( now );
        sessionFactory.getCurrentSession().update( skeleton );

        if ( ee.getWorkflowState() == null
                || ee.getWorkflowState() == WorkflowState.Discovery
                || ee.getWorkflowState() == WorkflowState.Candidate
                || ee.getWorkflowState() == WorkflowState.Skeleton ) {
            ee.setWorkflowState( WorkflowState.Loaded );
            ee.setWorkflowStateEnteredAt( now );
            sessionFactory.getCurrentSession().update( ee );
        }

        return new PromotionResult( skeleton.getId(), ee.getId(), reboundCount );
    }
}
