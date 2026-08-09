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
import ubic.gemma.model.common.auditAndSecurity.eventType.PreboardedPromotedEvent;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.PreboardedExperiment;
import ubic.gemma.model.expression.experiment.WorkflowState;
import ubic.gemma.persistence.service.common.auditAndSecurity.curation.AnnotationSetService;

import java.util.Date;
import java.util.List;

/**
 * Default {@link PreboardedExperimentService} implementation.
 *
 * <p>The promotion path is the substantive piece: it calls into
 * {@link AnnotationSetService#rebindInvestigation} to point every
 * {@code AnnotationSet} row at the loaded EE rather than the preboarded, then
 * advances both rows' workflow state. Audit events are emitted declaratively
 * via {@link Audited @Audited}. The create path delegates to
 * {@link PreboardedAuditService} so the proxy-intercepted return triggers the
 * aspect against the freshly constructed preboarded — direct annotation on
 * {@code createPreboarded} would not work because the {@code AuditedAspect}
 * locates the auditable target on the argument list (the new preboarded is
 * built inside the method body).</p>
 */
@Service
public class PreboardedExperimentServiceImpl implements PreboardedExperimentService {

    private final SessionFactory sessionFactory;
    private final AnnotationSetService annotationSetService;
    private final ExpressionExperimentService expressionExperimentService;
    private final PreboardedAuditService preboardedAuditService;

    @Autowired
    public PreboardedExperimentServiceImpl( SessionFactory sessionFactory,
            AnnotationSetService annotationSetService,
            ExpressionExperimentService expressionExperimentService,
            PreboardedAuditService preboardedAuditService ) {
        this.sessionFactory = sessionFactory;
        this.annotationSetService = annotationSetService;
        this.expressionExperimentService = expressionExperimentService;
        this.preboardedAuditService = preboardedAuditService;
    }

    @Override
    @Transactional
    public PreboardedExperiment createPreboarded( String accession,
            @Nullable String source,
            @Nullable String identifyingMetadata )
            throws AccessionAlreadyExistsException {
        Assert.hasText( accession, "accession must be non-blank." );
        // Reject if either a PreboardedExperiment OR an ExpressionExperiment
        // already carries this accession (handoff §"Required endpoints" 409).
        PreboardedExperiment existingPreboarded = findByAccession( accession );
        if ( existingPreboarded != null ) {
            throw new AccessionAlreadyExistsException( accession,
                    existingPreboarded.getId(), "preboarded" );
        }
        ExpressionExperiment existingEe = findExpressionExperimentByAccession( accession );
        if ( existingEe != null ) {
            throw new AccessionAlreadyExistsException( accession,
                    existingEe.getId(), "expression_experiment" );
        }

        PreboardedExperiment skel = new PreboardedExperiment();
        skel.setAccession( accession );
        if ( source != null && !source.isEmpty() ) {
            skel.setSource( source );
        }
        skel.setSourceMetadata( identifyingMetadata );
        skel.setName( "Preboarded:" + accession );
        skel.setWorkflowState( WorkflowState.Preboarded );
        skel.setWorkflowStateEnteredAt( new Date() );
        sessionFactory.getCurrentSession().persist( skel );
        sessionFactory.getCurrentSession().flush();

        // Audit emission via PreboardedAuditService co-bean so the @Audited
        // aspect can locate the freshly constructed skel on its argument list
        // (the AuditedAspect#findAuditable scan only inspects method args).
        preboardedAuditService.recordPreboardedCreated( skel, accession );
        return skel;
    }

    @Nullable
    @Override
    @Transactional(readOnly = true)
    public PreboardedExperiment load( Long id ) {
        if ( id == null ) return null;
        return ( PreboardedExperiment ) sessionFactory.getCurrentSession()
                .get( PreboardedExperiment.class, id );
    }

    @Nullable
    @Override
    @Transactional(readOnly = true)
    public PreboardedExperiment findByAccession( String accession ) {
        if ( accession == null ) return null;
        @SuppressWarnings("unchecked")
        List<PreboardedExperiment> rows = sessionFactory.getCurrentSession()
                .createQuery( "from PreboardedExperiment s where s.accession = :acc order by s.id asc" )
                .setParameter( "acc", accession )
                .setMaxResults( 1 )
                .list();
        return rows.isEmpty() ? null : rows.get( 0 );
    }

    @Override
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public List<PreboardedExperiment> findAllByAccession( String accession ) {
        if ( accession == null ) return java.util.Collections.emptyList();
        return sessionFactory.getCurrentSession()
                .createQuery( "from PreboardedExperiment s where s.accession = :acc order by s.id asc" )
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
    @Audited(value = PreboardedPromotedEvent.class,
            messageSpel = "'Preboarded#' + #preboarded.id + ' promoted to ExpressionExperiment#' + #ee.id"
                    + " + ' (annotation_sets_rebound=' + #result.annotationSetsRebound + ')'")
    public PromotionResult promote( ExpressionExperiment ee, PreboardedExperiment preboarded )
            throws PreboardedAlreadyPromotedException {
        Assert.notNull( preboarded, "preboarded must not be null." );
        Assert.notNull( ee, "ee must not be null." );
        if ( preboarded.getWorkflowState() == WorkflowState.Loaded
                || preboarded.getWorkflowState() == WorkflowState.Curate
                || preboarded.getWorkflowState() == WorkflowState.Process
                || preboarded.getWorkflowState() == WorkflowState.Audit
                || preboarded.getWorkflowState() == WorkflowState.Public ) {
            throw new PreboardedAlreadyPromotedException( preboarded.getId() );
        }

        // Rebind AnnotationSet rows from preboarded -> ee. The promote endpoint
        // contract is "the historical annotation sets accessible from the EE;
        // the audit trail intact" — rebind is the new-row + FK rebind approach
        // (see STATUS_PROPOSED_EXPERIMENT_WORKFLOW.md for the trade-off
        // discussion).
        int reboundCount = annotationSetService.rebindInvestigation( preboarded, ee );

        // Advance the preboarded's workflow state to Loaded (terminal marker;
        // the preboarded row is retained as history, no curatable artifacts
        // on it). The EE's workflow state likewise becomes Loaded if it
        // isn't already past it.
        Date now = new Date();
        preboarded.setWorkflowState( WorkflowState.Loaded );
        preboarded.setWorkflowStateEnteredAt( now );
        sessionFactory.getCurrentSession().update( preboarded );

        boolean eeDirty = false;
        if ( ee.getWorkflowState() == null
                || ee.getWorkflowState() == WorkflowState.Discovery
                || ee.getWorkflowState() == WorkflowState.Candidate
                || ee.getWorkflowState() == WorkflowState.Preboarded ) {
            ee.setWorkflowState( WorkflowState.Loaded );
            ee.setWorkflowStateEnteredAt( now );
            eeDirty = true;
        }

        // Carry the preboarded's upstream-metadata payload forward; promotion used to drop it, so
        // whatever the scrape harvested was lost the moment the data landed.
        //
        // Never over the top of one the experiment already has. The import writes the schema-v1
        // document built from the parsed series — per-sample titles, the submitter's own
        // characteristic columns — whereas a preboarded carries only the smaller scrape-path payload
        // (schema version null). Overwriting would trade the richer document for the poorer one, and
        // silently, since both land in the same column.
        if ( ee.getSourceMetadata() == null && preboarded.getSourceMetadata() != null ) {
            ee.setSourceMetadata( preboarded.getSourceMetadata() );
            ee.setSourceMetadataSchemaVersion( preboarded.getSourceMetadataSchemaVersion() );
            eeDirty = true;
        }

        if ( eeDirty ) {
            sessionFactory.getCurrentSession().update( ee );
        }

        return new PromotionResult( preboarded.getId(), ee.getId(), reboundCount );
    }
}
