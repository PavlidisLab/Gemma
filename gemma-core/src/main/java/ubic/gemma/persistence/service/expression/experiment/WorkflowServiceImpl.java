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
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 */
package ubic.gemma.persistence.service.expression.experiment;

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import ubic.gemma.core.security.audit.AuditedConditional;
import ubic.gemma.model.analysis.Investigation;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.WorkflowStateChangedEvent;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.WorkflowState;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditEventService;
import ubic.gemma.persistence.util.Slice;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Default {@link WorkflowService} implementation.
 *
 * <p>{@link #advance(Investigation, WorkflowState, String, Long)} is the
 * sole audit-event emitter; it carries
 * {@link AuditedConditional @AuditedConditional} so the surrounding aspect
 * handles the audit-row insert ONLY on real transitions. The SpEL
 * predicate inspects {@code #result.previousState != #result.currentState}
 * to skip emission on idempotent no-ops -- the handoff is explicit that
 * idempotent PUTs emit NO audit event.</p>
 */
@Service
public class WorkflowServiceImpl implements WorkflowService {

    private final AuditEventService auditEventService;
    private final SessionFactory sessionFactory;

    @Autowired
    public WorkflowServiceImpl( AuditEventService auditEventService, SessionFactory sessionFactory ) {
        this.auditEventService = auditEventService;
        this.sessionFactory = sessionFactory;
    }

    @Override
    @Transactional(readOnly = true)
    public WorkflowState getCurrentState( Investigation investigation ) {
        Assert.notNull( investigation, "Investigation must not be null." );
        WorkflowState ws = investigation.getWorkflowState();
        return ws != null ? ws : WorkflowState.Loaded;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditEvent> getHistory( Investigation investigation ) {
        Assert.notNull( investigation, "Investigation must not be null." );
        List<AuditEvent> all = auditEventService.getEventsWithType( investigation );
        List<AuditEvent> filtered = new ArrayList<>();
        for ( AuditEvent e : all ) {
            if ( e.getEventType() instanceof WorkflowStateChangedEvent ) {
                filtered.add( e );
            }
        }
        // events from getEventsWithType are not guaranteed ordered; sort oldest-first by date
        filtered.sort( ( a, b ) -> {
            Date da = a.getDate();
            Date db = b.getDate();
            if ( da == null && db == null ) return 0;
            if ( da == null ) return -1;
            if ( db == null ) return 1;
            return da.compareTo( db );
        } );
        return filtered;
    }

    @Override
    @Transactional
    @AuditedConditional(value = WorkflowStateChangedEvent.class,
            when = "#result != null and #result.previousState != #result.currentState",
            messageSpel = "'Workflow ' + #result.previousState + ' -> ' + #result.currentState"
                    + " + (#reason != null and #reason.length() > 0 ? ': ' + #reason : '')"
                    + " + (#ticketId != null ? ' (ticket=' + #ticketId + ')' : '')")
    public WorkflowTransition advance( Investigation dataset, WorkflowState targetState,
            @Nullable String reason, @Nullable Long ticketId ) {
        Assert.notNull( dataset, "Dataset must not be null." );
        Assert.notNull( targetState, "Target state must not be null." );
        // TODO(skeleton-integration): when SkeletonInvestigation lands, this
        // method's body works unchanged because Investigation is the common
        // supertype. The REST layer is what currently narrows to EE.
        WorkflowState current = dataset.getWorkflowState();
        if ( current == null ) {
            current = WorkflowState.Loaded;
        }
        if ( current == targetState ) {
            // Idempotent no-op: no row mutation. The @AuditedConditional
            // predicate (previousState != currentState) suppresses the event.
            return new WorkflowTransition( dataset.getId(), current, current,
                    dataset.getWorkflowStateEnteredAt(), null );
        }
        if ( !current.canTransitionTo( targetState ) ) {
            throw new DisallowedWorkflowTransitionException( current, targetState,
                    current.allowedNextStates() );
        }
        Date now = new Date();
        dataset.setWorkflowState( targetState );
        dataset.setWorkflowStateEnteredAt( now );
        sessionFactory.getCurrentSession().update( dataset );
        // The aspect appends the audit row after this returns; the audit-event
        // id is not reachable from inside the method body, so the returned
        // transition omits it. The REST GET /workflow handler derives it
        // separately via getHistory if needed.
        return new WorkflowTransition( dataset.getId(), current, targetState, now, null );
    }

    @Override
    @Transactional(readOnly = true)
    public Slice<WorkflowQueueEntry> queue( WorkflowState state,
            @Nullable String datasetType,
            @Nullable String assignee,
            @Nullable Date since,
            int offset, int limit ) {
        Assert.notNull( state, "State must not be null." );
        // TODO(skeleton-integration): when SkeletonInvestigation lands the
        // query needs a UNION over both subclasses. For now we only serve
        // ExpressionExperiment and report dataset_type=expression_experiment.
        if ( datasetType != null
                && !"expression_experiment".equalsIgnoreCase( datasetType ) ) {
            // Honor the filter literally: a request for skeleton_investigation
            // returns empty until the subclass lands rather than silently
            // returning EEs.
            return new Slice<>( new ArrayList<>(), null, offset, limit, 0L );
        }
        // TODO(ticket-integration): the assignee filter would join over the
        // open-ticket assignee for the dataset. Until that join lands, honor
        // it literally by returning empty when the caller asked for a
        // specific assignee.
        if ( assignee != null && !assignee.isEmpty() ) {
            return new Slice<>( new ArrayList<>(), null, offset, limit, 0L );
        }

        StringBuilder hql = new StringBuilder(
                "select ee from ExpressionExperiment ee where ee.workflowState = :state" );
        if ( since != null ) {
            hql.append( " and ee.workflowStateEnteredAt >= :since" );
        }
        // oldest-first so the queue is naturally "what's been waiting longest".
        // Tie-break by id for deterministic pagination; "nulls last" excluded
        // because not all dialects accept it (legacy rows w/ null enteredAt
        // fall to the end via the id tiebreak after a pre-pass on enteredAt).
        hql.append( " order by ee.workflowStateEnteredAt asc, ee.id asc" );

        @SuppressWarnings("unchecked")
        org.hibernate.query.Query<ExpressionExperiment> q =
                ( org.hibernate.query.Query<ExpressionExperiment> ) sessionFactory.getCurrentSession()
                        .createQuery( hql.toString() );
        q.setParameter( "state", state );
        if ( since != null ) {
            q.setParameter( "since", since );
        }
        if ( offset > 0 ) q.setFirstResult( offset );
        if ( limit > 0 ) q.setMaxResults( limit );
        List<ExpressionExperiment> rows = q.list();

        StringBuilder ch = new StringBuilder(
                "select count(ee) from ExpressionExperiment ee where ee.workflowState = :state" );
        if ( since != null ) ch.append( " and ee.workflowStateEnteredAt >= :since" );
        org.hibernate.query.Query<?> cq = sessionFactory.getCurrentSession().createQuery( ch.toString() );
        cq.setParameter( "state", state );
        if ( since != null ) cq.setParameter( "since", since );
        Long total = ( Long ) cq.uniqueResult();

        List<WorkflowQueueEntry> entries = new ArrayList<>( rows.size() );
        for ( ExpressionExperiment ee : rows ) {
            String accession = null;
            DatabaseEntry de = ee.getAccession();
            if ( de != null ) {
                accession = de.getAccession();
            }
            WorkflowQueueEntry entry = new WorkflowQueueEntry(
                    ee.getId(),
                    "expression_experiment",
                    accession,
                    ee.getWorkflowStateEnteredAt() );
            // TODO(ticket-integration): populate currentAssignee + ticketCountOpen
            // once the Ticket-layer projection has a settled contract. The
            // current first cut leaves them null / 0 so the queue response is
            // honest about not joining over the ticket table.
            entries.add( entry );
        }

        return new Slice<>( entries, null, offset, limit, total );
    }
}
