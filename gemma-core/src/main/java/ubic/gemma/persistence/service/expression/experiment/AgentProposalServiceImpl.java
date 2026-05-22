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
import ubic.gemma.model.expression.experiment.AgentProposal;

import java.util.Date;
import java.util.List;

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
            messageSpel = "'AgentProposal#' + #result.proposal.id + ' run=' + #runId"
                    + " + (#agentVersion != null ? ' agent=' + #agentVersion : '')"
                    + " + (#model != null ? ' model=' + #model : '')")
    public AttachedProposal attach( Investigation investigation, String runId,
            @Nullable String agentVersion,
            @Nullable String model,
            @Nullable Date ranAt,
            @Nullable String payloadJson ) {
        Assert.notNull( investigation, "Investigation must not be null." );
        Assert.hasText( runId, "runId must be non-blank." );
        // Step 1 of AgentCuration unification: the proposal path is always
        // kind=PROPOSAL. The audit-creation entry point (kind=AUDIT) ships in
        // step 3 — see handoffs/RECCE_AGENT_CURATION_UNIFICATION.md.
        AgentProposal existing = agentProposalDao.findByInvestigationAndKindAndRunId(
                investigation, AgentCurationKind.PROPOSAL, runId );
        if ( existing != null ) {
            // Idempotent retry; the @AuditedConditional predicate
            // (`#result.created`) suppresses event emission.
            return new AttachedProposal( existing, false );
        }
        AgentProposal p = new AgentProposal();
        p.setInvestigation( investigation );
        p.setKind( AgentCurationKind.PROPOSAL );
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
}
