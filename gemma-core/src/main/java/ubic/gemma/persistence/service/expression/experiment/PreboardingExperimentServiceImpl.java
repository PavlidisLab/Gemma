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
import ubic.gemma.model.common.auditAndSecurity.eventType.PreboardingCreatedEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.PreboardingPromotedEvent;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.PreboardingExperiment;
import ubic.gemma.model.expression.experiment.WorkflowState;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService;

import java.util.Date;
import java.util.List;

/**
 * Default {@link PreboardingExperimentService} implementation.
 *
 * <p>The promotion path is the substantive piece: it calls into
 * {@link AgentProposalService#rebindInvestigation} to point every
 * {@code AgentProposal} row at the loaded EE rather than the preboarding, then
 * advances both rows' workflow state. Audit events are emitted declaratively
 * via {@link Audited @Audited} on the methods where the auditable target is
 * passed in (promote). The create path emits its event imperatively because
 * the auditable target is constructed in the method body and the
 * {@code AuditedAspect} can only locate auditables on the argument list
 * (see {@code AuditedAspect#findAuditable}).</p>
 */
@Service
public class PreboardingExperimentServiceImpl implements PreboardingExperimentService {

    private final SessionFactory sessionFactory;
    private final AgentProposalService agentProposalService;
    private final ExpressionExperimentService expressionExperimentService;
    private final AuditTrailService auditTrailService;

    @Autowired
    public PreboardingExperimentServiceImpl( SessionFactory sessionFactory,
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
    public PreboardingExperiment createPreboarding( String accession,
            @Nullable String source,
            @Nullable String identifyingMetadata )
            throws AccessionAlreadyExistsException {
        Assert.hasText( accession, "accession must be non-blank." );
        // Reject if either a PreboardingExperiment OR an ExpressionExperiment
        // already carries this accession (handoff §"Required endpoints" 409).
        PreboardingExperiment existingPreboarding = findByAccession( accession );
        if ( existingPreboarding != null ) {
            throw new AccessionAlreadyExistsException( accession,
                    existingPreboarding.getId(), "preboarding" );
        }
        ExpressionExperiment existingEe = findExpressionExperimentByAccession( accession );
        if ( existingEe != null ) {
            throw new AccessionAlreadyExistsException( accession,
                    existingEe.getId(), "expression_experiment" );
        }

        PreboardingExperiment skel = new PreboardingExperiment();
        skel.setAccession( accession );
        if ( source != null && !source.isEmpty() ) {
            skel.setSource( source );
        }
        skel.setIdentifyingMetadata( identifyingMetadata );
        skel.setName( "Preboarding:" + accession );
        skel.setWorkflowState( WorkflowState.Preboarding );
        skel.setWorkflowStateEnteredAt( new Date() );
        sessionFactory.getCurrentSession().persist( skel );
        sessionFactory.getCurrentSession().flush();

        // Imperative audit emission: the AuditedAspect can only locate
        // an Auditable target on the argument list, and `accession` (String)
        // is not auditable. The freshly persisted preboarding IS auditable;
        // emit the event directly so the same "one PreboardingCreatedEvent per
        // create" guarantee holds.
        //noinspection deprecation
        auditTrailService.addUpdateEvent( skel, PreboardingCreatedEvent.class,
                "Preboarding created for accession " + accession );
        return skel;
    }

    @Nullable
    @Override
    @Transactional(readOnly = true)
    public PreboardingExperiment load( Long id ) {
        if ( id == null ) return null;
        return ( PreboardingExperiment ) sessionFactory.getCurrentSession()
                .get( PreboardingExperiment.class, id );
    }

    @Nullable
    @Override
    @Transactional(readOnly = true)
    public PreboardingExperiment findByAccession( String accession ) {
        if ( accession == null ) return null;
        @SuppressWarnings("unchecked")
        List<PreboardingExperiment> rows = sessionFactory.getCurrentSession()
                .createQuery( "from PreboardingExperiment s where s.accession = :acc order by s.id asc" )
                .setParameter( "acc", accession )
                .setMaxResults( 1 )
                .list();
        return rows.isEmpty() ? null : rows.get( 0 );
    }

    @Override
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public List<PreboardingExperiment> findAllByAccession( String accession ) {
        if ( accession == null ) return java.util.Collections.emptyList();
        return sessionFactory.getCurrentSession()
                .createQuery( "from PreboardingExperiment s where s.accession = :acc order by s.id asc" )
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
    @Audited(value = PreboardingPromotedEvent.class,
            messageSpel = "'Preboarding#' + #preboarding.id + ' promoted to ExpressionExperiment#' + #ee.id"
                    + " + ' (proposals_rebound=' + #result.proposalsRebound + ')'")
    public PromotionResult promote( ExpressionExperiment ee, PreboardingExperiment preboarding )
            throws PreboardingAlreadyPromotedException {
        Assert.notNull( preboarding, "preboarding must not be null." );
        Assert.notNull( ee, "ee must not be null." );
        if ( preboarding.getWorkflowState() == WorkflowState.Loaded
                || preboarding.getWorkflowState() == WorkflowState.Curate
                || preboarding.getWorkflowState() == WorkflowState.Process
                || preboarding.getWorkflowState() == WorkflowState.Audit
                || preboarding.getWorkflowState() == WorkflowState.Public ) {
            throw new PreboardingAlreadyPromotedException( preboarding.getId() );
        }

        // Rebind AgentProposal rows from preboarding -> ee. The promote endpoint
        // contract is "the historical AgentProposal rows accessible from the
        // EE; the audit trail intact" — rebind is the new-row + FK rebind
        // approach (see STATUS_PROPOSED_EXPERIMENT_WORKFLOW.md for the
        // trade-off discussion).
        int reboundCount = agentProposalService.rebindInvestigation( preboarding, ee );

        // Advance the preboarding's workflow state to Loaded (terminal marker;
        // the preboarding row is retained as history, no curatable artifacts
        // on it). The EE's workflow state likewise becomes Loaded if it
        // isn't already past it.
        Date now = new Date();
        preboarding.setWorkflowState( WorkflowState.Loaded );
        preboarding.setWorkflowStateEnteredAt( now );
        sessionFactory.getCurrentSession().update( preboarding );

        if ( ee.getWorkflowState() == null
                || ee.getWorkflowState() == WorkflowState.Discovery
                || ee.getWorkflowState() == WorkflowState.Candidate
                || ee.getWorkflowState() == WorkflowState.Preboarding ) {
            ee.setWorkflowState( WorkflowState.Loaded );
            ee.setWorkflowStateEnteredAt( now );
            sessionFactory.getCurrentSession().update( ee );
        }

        return new PromotionResult( preboarding.getId(), ee.getId(), reboundCount );
    }
}
