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
package ubic.gemma.rest;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ubic.gemma.model.expression.experiment.AgentCurationKind;
import ubic.gemma.model.expression.experiment.AgentCurationSummaryValueObject;
import ubic.gemma.model.expression.experiment.AgentProposal;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.persistence.service.expression.experiment.AgentProposalService;
import ubic.gemma.rest.util.args.DatasetArg;
import ubic.gemma.rest.util.args.DatasetArgService;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Pure-Mockito unit tests for {@link CurationWebService}. Exercises the
 * {@code ?kind} / {@code ?shape} REST contract added in step 3 of the
 * AgentCuration unification (see
 * {@code handoffs/RECCE_AGENT_CURATION_UNIFICATION.md} §6).
 */
@ExtendWith(MockitoExtension.class)
public class CurationWebServiceTest {

    @Mock
    private DatasetArgService datasetArgService;
    @Mock
    private AuditTrailService auditTrailService;
    @Mock
    private AgentProposalService agentProposalService;
    @Mock
    private DatasetArg<?> datasetArg;

    @InjectMocks
    private CurationWebService webService;

    private ExpressionExperiment ee;

    @BeforeEach
    public void setUp() {
        ee = new ExpressionExperiment();
        ee.setId( 12345L );
        lenient().when( datasetArgService.getEntity( any() ) ).thenReturn( ee );
    }

    /* ===== ?shape=meta path ===== */

    @Test
    public void list_shapeMeta_returnsSummaryRowsWithoutPayload() {
        AgentCurationSummaryValueObject vo = new AgentCurationSummaryValueObject(
                7L, AgentCurationKind.PROPOSAL, "run-1", "0.8.0",
                "claude", new Date(), 12345L, 8192L );
        when( agentProposalService.findSummariesByInvestigation( ee, null ) )
                .thenReturn( Collections.singletonList( vo ) );

        Response resp = webService.listCurationProposals( datasetArg, "all", "meta" );
        assertThat( resp.getStatus() ).isEqualTo( 200 );
        @SuppressWarnings("unchecked")
        List<CurationWebService.CurationProposalSummaryResponse> rows =
                ( List<CurationWebService.CurationProposalSummaryResponse> ) resp.getEntity();
        assertThat( rows ).hasSize( 1 );
        CurationWebService.CurationProposalSummaryResponse r = rows.get( 0 );
        assertThat( r.proposalId ).isEqualTo( 7L );
        assertThat( r.datasetId ).isEqualTo( 12345L );
        assertThat( r.kind ).isEqualTo( "proposal" );
        assertThat( r.payloadSize ).isEqualTo( 8192L );
    }

    @Test
    public void list_shapeMetaWithKindAudit_filtersByAudit() {
        when( agentProposalService.findSummariesByInvestigation( ee, AgentCurationKind.AUDIT ) )
                .thenReturn( Collections.emptyList() );
        Response resp = webService.listCurationProposals( datasetArg, "audit", "meta" );
        assertThat( resp.getStatus() ).isEqualTo( 200 );
    }

    /* ===== ?shape=full (default) path ===== */

    @Test
    public void list_shapeFullByDefault_returnsFullRowsWithPayload() {
        AgentProposal p = new AgentProposal();
        p.setId( 4L );
        p.setRunId( "run-1" );
        p.setKind( AgentCurationKind.PROPOSAL );
        p.setPayloadJson( "{\"x\":1}" );
        p.setInvestigation( ee );
        when( agentProposalService.findByInvestigation( ee ) )
                .thenReturn( Collections.singletonList( p ) );

        Response resp = webService.listCurationProposals( datasetArg, null, null );
        assertThat( resp.getStatus() ).isEqualTo( 200 );
        @SuppressWarnings("unchecked")
        List<CurationWebService.CurationProposalResponse> rows =
                ( List<CurationWebService.CurationProposalResponse> ) resp.getEntity();
        assertThat( rows ).hasSize( 1 );
        assertThat( rows.get( 0 ).payloadJson ).isEqualTo( "{\"x\":1}" );
        assertThat( rows.get( 0 ).kind ).isEqualTo( "proposal" );
    }

    @Test
    public void list_shapeFull_filtersByKind() {
        AgentProposal pProp = new AgentProposal();
        pProp.setId( 1L );
        pProp.setRunId( "run-prop" );
        pProp.setKind( AgentCurationKind.PROPOSAL );
        pProp.setInvestigation( ee );
        AgentProposal pAudit = new AgentProposal();
        pAudit.setId( 2L );
        pAudit.setRunId( "run-aud" );
        pAudit.setKind( AgentCurationKind.AUDIT );
        pAudit.setInvestigation( ee );
        when( agentProposalService.findByInvestigation( ee ) )
                .thenReturn( Arrays.asList( pProp, pAudit ) );

        Response resp = webService.listCurationProposals( datasetArg, "audit", "full" );
        @SuppressWarnings("unchecked")
        List<CurationWebService.CurationProposalResponse> rows =
                ( List<CurationWebService.CurationProposalResponse> ) resp.getEntity();
        assertThat( rows ).hasSize( 1 );
        assertThat( rows.get( 0 ).kind ).isEqualTo( "audit" );
    }

    /* ===== Param parsing ===== */

    @Test
    public void list_unknownKindThrows400() {
        assertThatThrownBy( () -> webService.listCurationProposals( datasetArg, "garbage", null ) )
                .isInstanceOf( BadRequestException.class );
    }

    @Test
    public void list_unknownShapeThrows400() {
        assertThatThrownBy( () -> webService.listCurationProposals( datasetArg, null, "garbage" ) )
                .isInstanceOf( BadRequestException.class );
    }

    /* ===== POST kind support ===== */

    @Test
    public void submit_kindAuditPersistsAuditRow() {
        AgentProposal saved = new AgentProposal();
        saved.setId( 99L );
        saved.setKind( AgentCurationKind.AUDIT );
        saved.setRunId( "run-A" );
        saved.setInvestigation( ee );
        when( agentProposalService.attach( eq( ee ), eq( AgentCurationKind.AUDIT ), eq( "run-A" ),
                any(), any(), any(), any() ) )
                .thenReturn( new AgentProposalService.AttachedProposal( saved, true ) );

        CurationWebService.CurationProposalRequest req = new CurationWebService.CurationProposalRequest();
        req.runId = "run-A";
        req.kind = "audit";
        Response resp = webService.submitCurationProposal( datasetArg, req );
        assertThat( resp.getStatus() ).isEqualTo( 201 );
        CurationWebService.CurationProposalResponse body =
                ( CurationWebService.CurationProposalResponse ) resp.getEntity();
        assertThat( body.kind ).isEqualTo( "audit" );
    }

    @Test
    public void submit_missingKindDefaultsToProposal() {
        AgentProposal saved = new AgentProposal();
        saved.setId( 100L );
        saved.setKind( AgentCurationKind.PROPOSAL );
        saved.setRunId( "run-B" );
        saved.setInvestigation( ee );
        when( agentProposalService.attach( eq( ee ), eq( AgentCurationKind.PROPOSAL ), eq( "run-B" ),
                any(), any(), any(), any() ) )
                .thenReturn( new AgentProposalService.AttachedProposal( saved, true ) );

        CurationWebService.CurationProposalRequest req = new CurationWebService.CurationProposalRequest();
        req.runId = "run-B";
        // kind omitted
        Response resp = webService.submitCurationProposal( datasetArg, req );
        assertThat( resp.getStatus() ).isEqualTo( 201 );
        assertThat( ( ( CurationWebService.CurationProposalResponse ) resp.getEntity() ).kind )
                .isEqualTo( "proposal" );
    }

    @Test
    public void submit_unknownKindThrows400() {
        CurationWebService.CurationProposalRequest req = new CurationWebService.CurationProposalRequest();
        req.runId = "run-X";
        req.kind = "nonsense";
        assertThatThrownBy( () -> webService.submitCurationProposal( datasetArg, req ) )
                .isInstanceOf( BadRequestException.class );
    }

    @Test
    public void submit_blankRunIdThrows400() {
        CurationWebService.CurationProposalRequest req = new CurationWebService.CurationProposalRequest();
        assertThatThrownBy( () -> webService.submitCurationProposal( datasetArg, req ) )
                .isInstanceOf( BadRequestException.class );
        // Also guard the null-body path.
        assertThatThrownBy( () -> webService.submitCurationProposal( datasetArg, null ) )
                .isInstanceOf( BadRequestException.class );
    }
}
