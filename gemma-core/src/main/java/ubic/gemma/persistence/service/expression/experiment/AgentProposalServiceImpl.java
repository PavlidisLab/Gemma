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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import ubic.gemma.core.security.audit.AuditedConditional;
import ubic.gemma.model.analysis.Investigation;
import ubic.gemma.model.common.auditAndSecurity.eventType.AgentProposalEvent;
import ubic.gemma.model.expression.experiment.AgentCurationKind;
import ubic.gemma.model.expression.experiment.AgentCurationSummaryValueObject;
import ubic.gemma.model.expression.experiment.AgentProposal;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Default {@link AgentProposalService} implementation.
 *
 * <p>{@link #attach(Investigation, String, String, String, Date, String)}
 * carries {@code @AuditedConditional}: an event is emitted only on actual
 * insert (the {@code result.created} flag), not on the idempotent
 * same-runId retry path. The audit note carries the proposal id so the
 * audit trail links to the {@link AgentProposal} row without inlining the
 * JSON payload (the structured {@code AUDIT_EVENT.PAYLOAD} column is a
 * separate piece of work — see {@code STATUS_PROPOSED_EXPERIMENT_WORKFLOW.md}).</p>
 */
@Service
public class AgentProposalServiceImpl implements AgentProposalService {

    private final AgentProposalDao agentProposalDao;

    @Autowired
    public AgentProposalServiceImpl( AgentProposalDao agentProposalDao ) {
        this.agentProposalDao = agentProposalDao;
    }

    @Override
    @Transactional
    @AuditedConditional(value = AgentProposalEvent.class,
            when = "#result != null and #result.created",
            messageSpel = "'AgentProposal#' + #result.proposal.id"
                    + " + ' kind=' + #result.proposal.kind.dbValue"
                    + " + ' run=' + #runId"
                    + " + (#agentVersion != null ? ' agent=' + #agentVersion : '')"
                    + " + (#model != null ? ' model=' + #model : '')")
    public AttachedProposal attach( Investigation investigation, String runId,
            @Nullable String agentVersion,
            @Nullable String model,
            @Nullable Date ranAt,
            @Nullable String payloadJson ) {
        // Backwards-compat overload: kind defaults to PROPOSAL. Persists
        // inline rather than delegating to the kind-aware overload so the
        // AOP proxy fires the @AuditedConditional aspect on this entry point
        // (self-invocation would skip the proxy).
        return doAttach( investigation, AgentCurationKind.PROPOSAL, runId,
                agentVersion, model, ranAt, payloadJson );
    }

    @Override
    @Transactional
    @AuditedConditional(value = AgentProposalEvent.class,
            when = "#result != null and #result.created",
            messageSpel = "'AgentProposal#' + #result.proposal.id"
                    + " + ' kind=' + #result.proposal.kind.dbValue"
                    + " + ' run=' + #runId"
                    + " + (#agentVersion != null ? ' agent=' + #agentVersion : '')"
                    + " + (#model != null ? ' model=' + #model : '')")
    public AttachedProposal attach( Investigation investigation,
            @Nullable AgentCurationKind kind,
            String runId,
            @Nullable String agentVersion,
            @Nullable String model,
            @Nullable Date ranAt,
            @Nullable String payloadJson ) {
        return doAttach( investigation, kind != null ? kind : AgentCurationKind.PROPOSAL,
                runId, agentVersion, model, ranAt, payloadJson );
    }

    private AttachedProposal doAttach( Investigation investigation, AgentCurationKind kind, String runId,
            @Nullable String agentVersion, @Nullable String model,
            @Nullable Date ranAt, @Nullable String payloadJson ) {
        Assert.notNull( investigation, "Investigation must not be null." );
        Assert.hasText( runId, "runId must be non-blank." );
        AgentProposal existing = agentProposalDao.findByInvestigationAndKindAndRunId(
                investigation, kind, runId );
        if ( existing != null ) {
            // Idempotent retry; the @AuditedConditional predicate
            // (`#result.created`) suppresses event emission.
            return new AttachedProposal( existing, false );
        }
        AgentProposal p = new AgentProposal();
        p.setInvestigation( investigation );
        p.setKind( kind );
        p.setRunId( runId );
        p.setAgentVersion( agentVersion );
        p.setModel( model );
        p.setRanAt( ranAt != null ? ranAt : new Date() );
        p.setPayloadJson( payloadJson );
        AgentProposal saved = agentProposalDao.create( p );
        return new AttachedProposal( saved, true );
    }

    @Override
    @Transactional(readOnly = true)
    public List<AgentProposal> findByInvestigation( Investigation investigation ) {
        Assert.notNull( investigation, "Investigation must not be null." );
        return agentProposalDao.findByInvestigation( investigation );
    }

    @Override
    @Transactional(readOnly = true)
    public List<AgentCurationSummaryValueObject> findSummariesByInvestigation( Investigation investigation,
            @Nullable AgentCurationKind kindFilter ) {
        Assert.notNull( investigation, "Investigation must not be null." );
        return agentProposalDao.findSummariesByInvestigation( investigation, kindFilter );
    }

    @Nullable
    @Override
    @Transactional(readOnly = true)
    public AgentProposal findLatestByInvestigation( Investigation investigation ) {
        Assert.notNull( investigation, "Investigation must not be null." );
        return agentProposalDao.findLatestByInvestigation( investigation );
    }

    @Nullable
    @Override
    @Transactional(readOnly = true)
    public AgentProposal load( Long id ) {
        if ( id == null ) return null;
        return agentProposalDao.load( id );
    }

    @Override
    @Transactional(readOnly = true)
    public long countByInvestigation( Investigation investigation ) {
        Assert.notNull( investigation, "Investigation must not be null." );
        return agentProposalDao.countByInvestigation( investigation );
    }

    @Override
    @Transactional
    public int rebindInvestigation( Investigation from, Investigation to ) {
        Assert.notNull( from, "from must not be null." );
        Assert.notNull( to, "to must not be null." );
        return agentProposalDao.rebindInvestigation( from, to );
    }

    @Override
    @Transactional(readOnly = true)
    public List<AgentCurationSummaryValueObject> listSummaries( @Nullable AgentCurationKind kindFilter,
            @Nullable List<Long> investigationIds, int offset, int limit ) {
        return agentProposalDao.listSummaries( kindFilter, investigationIds, offset, limit );
    }

    @Override
    @Transactional(readOnly = true)
    public long countSummaries( @Nullable AgentCurationKind kindFilter,
            @Nullable List<Long> investigationIds ) {
        return agentProposalDao.countSummaries( kindFilter, investigationIds );
    }

    @Nullable
    @Override
    @Transactional
    public AgentProposal updateDisposition( Long id, String disposition, @Nullable String note ) {
        Assert.notNull( id, "id must not be null." );
        Assert.hasText( disposition, "disposition must be non-blank." );
        AgentProposal p = agentProposalDao.load( id );
        if ( p == null ) return null;
        p.setDisposition( disposition );
        p.setDispositionNote( note );
        p.setLastUpdated( new Date() );
        agentProposalDao.update( p );
        return p;
    }

    @Nullable
    @Override
    @Transactional
    public AgentProposal finalizeProposal( Long id ) {
        Assert.notNull( id, "id must not be null." );
        AgentProposal p = agentProposalDao.load( id );
        if ( p == null ) return null;
        if ( "FINALIZED".equals( p.getStatus() ) ) {
            // Idempotent: no state change, no lastUpdated stamp.
            return p;
        }
        Date now = new Date();
        p.setStatus( "FINALIZED" );
        p.setFinalizedAt( now );
        p.setLastUpdated( now );
        agentProposalDao.update( p );
        return p;
    }

    @Override
    @Transactional(readOnly = true)
    public long countSince( @Nullable Date since ) {
        return agentProposalDao.countSince( since );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> countByStatusSince( @Nullable Date since ) {
        return agentProposalDao.countByStatusSince( since );
    }

    @Override
    @Transactional(readOnly = true)
    public long countDistinctRunIdsSince( @Nullable Date since ) {
        return agentProposalDao.countDistinctRunIdsSince( since );
    }

    @Nullable
    @Override
    @Transactional(readOnly = true)
    public Date findLatestRanAt() {
        return agentProposalDao.findLatestRanAt();
    }

    @Nullable
    @Override
    @Transactional
    public AgentProposal reopenProposal( Long id ) {
        Assert.notNull( id, "id must not be null." );
        AgentProposal p = agentProposalDao.load( id );
        if ( p == null ) return null;
        if ( "REOPENED".equals( p.getStatus() ) ) {
            // Idempotent: no state change, no lastUpdated stamp.
            return p;
        }
        Date now = new Date();
        p.setStatus( "REOPENED" );
        p.setFinalizedAt( null );
        p.setLastUpdated( now );
        agentProposalDao.update( p );
        return p;
    }
}
