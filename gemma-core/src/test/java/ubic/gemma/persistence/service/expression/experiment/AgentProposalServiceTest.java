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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ubic.gemma.model.expression.experiment.AgentCurationKind;
import ubic.gemma.model.expression.experiment.AgentProposal;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure-Mockito unit tests for {@link AgentProposalServiceImpl}: the
 * idempotency-on-runId contract, the attach/find/count/rebind surface.
 * Aspect coverage lives in {@code AuditedAspectTest}.
 */
@ExtendWith(MockitoExtension.class)
public class AgentProposalServiceTest {

    @Mock
    private AgentProposalDao agentProposalDao;

    @InjectMocks
    private AgentProposalServiceImpl service;

    private ExpressionExperiment ee;

    @BeforeEach
    public void setUp() {
        ee = new ExpressionExperiment();
        ee.setId( 12345L );
    }

    @Test
    public void attach_freshRunIdInsertsAndFlagsCreated() {
        when( agentProposalDao.findByInvestigationAndKindAndRunId( ee, AgentCurationKind.PROPOSAL, "run-1" ) ).thenReturn( null );
        when( agentProposalDao.create( any( AgentProposal.class ) ) ).thenAnswer( inv -> {
            AgentProposal p = inv.getArgument( 0 );
            p.setId( 555L );
            return p;
        } );
        AgentProposalService.AttachedProposal r = service.attach( ee, "run-1", "0.8.0",
                "claude", new Date(), "{}" );
        assertThat( r.isCreated() ).isTrue();
        assertThat( r.getProposal().getId() ).isEqualTo( 555L );
        assertThat( r.getProposal().getRunId() ).isEqualTo( "run-1" );
        assertThat( r.getProposal().getInvestigation() ).isEqualTo( ee );
    }

    @Test
    public void attach_existingRunIdReturnsExistingAndFlagsNotCreated() {
        AgentProposal existing = new AgentProposal();
        existing.setId( 42L );
        existing.setRunId( "run-1" );
        existing.setInvestigation( ee );
        when( agentProposalDao.findByInvestigationAndKindAndRunId( ee, AgentCurationKind.PROPOSAL, "run-1" ) ).thenReturn( existing );

        AgentProposalService.AttachedProposal r = service.attach( ee, "run-1", "0.8.0",
                "claude", new Date(), "{}" );
        assertThat( r.isCreated() ).isFalse();
        assertThat( r.getProposal().getId() ).isEqualTo( 42L );
        // No insert on the idempotent retry path.
        verify( agentProposalDao, never() ).create( any( AgentProposal.class ) );
    }

    @Test
    public void attach_blankRunIdThrows() {
        assertThatThrownBy( () -> service.attach( ee, "", null, null, null, null ) )
                .isInstanceOf( IllegalArgumentException.class );
        assertThatThrownBy( () -> service.attach( ee, null, null, null, null, null ) )
                .isInstanceOf( IllegalArgumentException.class );
    }

    @Test
    public void attach_nullInvestigationThrows() {
        assertThatThrownBy( () -> service.attach( null, "run-1", null, null, null, null ) )
                .isInstanceOf( IllegalArgumentException.class );
    }

    @Test
    public void attach_defaultsRanAtToNowWhenNull() {
        when( agentProposalDao.findByInvestigationAndKindAndRunId( ee, AgentCurationKind.PROPOSAL, "run-2" ) ).thenReturn( null );
        when( agentProposalDao.create( any( AgentProposal.class ) ) ).thenAnswer( inv -> inv.getArgument( 0 ) );
        AgentProposalService.AttachedProposal r = service.attach( ee, "run-2", null, null, null, null );
        assertThat( r.getProposal().getRanAt() ).isNotNull();
    }

    @Test
    public void rebindInvestigation_delegatesToDao() {
        ExpressionExperiment to = new ExpressionExperiment();
        to.setId( 99L );
        when( agentProposalDao.rebindInvestigation( ee, to ) ).thenReturn( 3 );
        int n = service.rebindInvestigation( ee, to );
        assertThat( n ).isEqualTo( 3 );
        verify( agentProposalDao ).rebindInvestigation( ee, to );
    }

    @Test
    public void kindDefaultsToProposalOnCreate() {
        // Entity-level default: a freshly-constructed AgentProposal must carry
        // kind=PROPOSAL so legacy call sites (and the service.attach() path)
        // continue producing proposal rows without explicit setKind calls.
        AgentProposal fresh = new AgentProposal();
        assertThat( fresh.getKind() ).isEqualTo( AgentCurationKind.PROPOSAL );

        // Service path: attach() always sets kind=PROPOSAL on the persisted row.
        when( agentProposalDao.findByInvestigationAndKindAndRunId( ee, AgentCurationKind.PROPOSAL, "run-3" ) )
                .thenReturn( null );
        when( agentProposalDao.create( any( AgentProposal.class ) ) ).thenAnswer( inv -> inv.getArgument( 0 ) );
        AgentProposalService.AttachedProposal r = service.attach( ee, "run-3", null, null, null, null );
        assertThat( r.getProposal().getKind() ).isEqualTo( AgentCurationKind.PROPOSAL );
    }

    @Test
    public void findLatestByInvestigation_delegatesToDao() {
        AgentProposal latest = new AgentProposal();
        latest.setId( 7L );
        when( agentProposalDao.findLatestByInvestigation( ee ) ).thenReturn( latest );
        AgentProposal r = service.findLatestByInvestigation( ee );
        assertThat( r.getId() ).isEqualTo( 7L );
    }
}
