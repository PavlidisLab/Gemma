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
package ubic.gemma.persistence.service.common.auditAndSecurity.curation;

import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.core.util.test.BaseIntegrationTest5;
import ubic.gemma.model.common.auditAndSecurity.curation.AnnotationSet;
import ubic.gemma.model.common.auditAndSecurity.curation.AnnotationSetDisposition;
import ubic.gemma.model.common.auditAndSecurity.curation.AnnotationSetRole;
import ubic.gemma.model.common.auditAndSecurity.curation.AnnotationSetSource;
import ubic.gemma.model.common.auditAndSecurity.curation.FindingDisposition;
import ubic.gemma.model.common.auditAndSecurity.curation.TriageJudgeKind;
import ubic.gemma.model.expression.experiment.AgentCurationKind;
import ubic.gemma.model.expression.experiment.PreboardedExperiment;
import ubic.gemma.model.expression.experiment.WorkflowState;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Persistence contract for {@link AnnotationSetDisposition} — the
 * row-per-ruling, latest-wins model for a curator's verdict on ONE finding.
 *
 * <p>The properties worth pinning are the ones the two nearby designs would
 * NOT have given: that two findings on one set are ruled independently (a
 * set-level verdict cannot do this), and that a curator changing their mind
 * leaves BOTH rows (the {@code UNIQUE(set, judge)} upsert on
 * {@code ANNOTATION_SET_TRIAGE} would have destroyed the first).</p>
 */
@Transactional
public class AnnotationSetDispositionPersistenceIT extends BaseIntegrationTest5 {

    @Autowired
    private AnnotationSetService annotationSetService;

    @Autowired
    private AnnotationSetDispositionService dispositionService;

    @Autowired
    private SessionFactory sessionFactory;

    private AnnotationSet audit;

    @BeforeEach
    public void seed() {
        PreboardedExperiment preboarded = new PreboardedExperiment();
        preboarded.setAccession( "GSE-disp-it-" + UUID.randomUUID() );
        preboarded.setSource( "GEO" );
        preboarded.setName( "AnnotationSetDispositionIT preboarded" );
        preboarded.setWorkflowState( WorkflowState.Preboarded );
        sessionFactory.getCurrentSession().persist( preboarded );
        sessionFactory.getCurrentSession().flush();

        audit = annotationSetService.attach( preboarded, AnnotationSetRole.PROPOSAL,
                        AnnotationSetSource.AGENT, AgentCurationKind.AUDIT,
                        "run-" + UUID.randomUUID(), "agent-1", null, null, null,
                        "{\"findings\":[]}", null )
                .getAnnotationSet();
        sessionFactory.getCurrentSession().flush();
    }

    @Test
    @DisplayName("an un-ruled finding has no row at all -- absence is the state")
    public void unruled_hasNoRow() {
        assertThat( dispositionService.findBySet( audit ) ).isEmpty();
        assertThat( dispositionService.standingFor( audit ) ).isEmpty();
        assertThat( dispositionService.historyFor( audit, "f1" ) ).isEmpty();
    }

    @Test
    @DisplayName("two findings on ONE set are ruled independently -- what a set-level verdict cannot do")
    public void twoFindingsOnOneSet_ruledIndependently() {
        dispositionService.rule( audit, "f1", FindingDisposition.ACCEPTED,
                "alice", TriageJudgeKind.CURATOR, "right about the strain" );
        dispositionService.rule( audit, "f2", FindingDisposition.DISMISSED,
                "alice", TriageJudgeKind.CURATOR, "that tag is correct as it stands" );
        sessionFactory.getCurrentSession().flush();

        Map<String, AnnotationSetDisposition> standing = dispositionService.standingFor( audit );
        assertThat( standing ).hasSize( 2 );
        assertThat( standing.get( "f1" ).getDisposition() ).isEqualTo( FindingDisposition.ACCEPTED );
        assertThat( standing.get( "f2" ).getDisposition() ).isEqualTo( FindingDisposition.DISMISSED );
        assertThat( standing.get( "f2" ).getReason() ).isEqualTo( "that tag is correct as it stands" );
    }

    @Test
    @DisplayName("append-only: a changed mind keeps BOTH rows, and the newer one stands")
    public void changedMind_keepsBothRows() {
        dispositionService.rule( audit, "f1", FindingDisposition.ACCEPTED,
                "alice", TriageJudgeKind.CURATOR, "first read" );
        dispositionService.rule( audit, "f1", FindingDisposition.DISMISSED,
                "alice", TriageJudgeKind.CURATOR, "second look -- the evidence quote is the wrong sample" );
        sessionFactory.getCurrentSession().flush();

        // The upsert used by ANNOTATION_SET_TRIAGE would leave one row here. The history is the
        // point: "accepted, then dismissed" is a different record from "dismissed outright".
        List<AnnotationSetDisposition> history = dispositionService.historyFor( audit, "f1" );
        assertThat( history ).hasSize( 2 );
        assertThat( history.get( 0 ).getDisposition() ).isEqualTo( FindingDisposition.DISMISSED );
        assertThat( history.get( 1 ).getDisposition() ).isEqualTo( FindingDisposition.ACCEPTED );

        Map<String, AnnotationSetDisposition> standing = dispositionService.standingFor( audit );
        assertThat( standing ).hasSize( 1 );
        assertThat( standing.get( "f1" ).getDisposition() ).isEqualTo( FindingDisposition.DISMISSED );
    }

    @Test
    @DisplayName("a same-millisecond tie resolves on id, not on row order")
    public void sameMillisecondTie_breaksOnId() {
        AnnotationSetDisposition first = dispositionService.rule( audit, "f1",
                FindingDisposition.ACCEPTED, "alice", TriageJudgeKind.CURATOR, null );
        AnnotationSetDisposition second = dispositionService.rule( audit, "f1",
                FindingDisposition.NEEDS_MORE_INFO, "alice", TriageJudgeKind.CURATOR,
                "which strain the congenic donor was" );
        // Force the collision the millisecond-precision column allows rather than waiting for
        // one: without the id tiebreaker the winner here is whichever row MySQL yields first.
        Date sameInstant = new Date();
        first.setDecidedAt( sameInstant );
        second.setDecidedAt( sameInstant );
        sessionFactory.getCurrentSession().flush();
        sessionFactory.getCurrentSession().clear();

        AnnotationSet reloaded = annotationSetService.load( audit.getId() );
        Map<String, AnnotationSetDisposition> standing = dispositionService.standingFor( reloaded );
        assertThat( standing.get( "f1" ).getId() ).isEqualTo( second.getId() );
        assertThat( standing.get( "f1" ).getDisposition() ).isEqualTo( FindingDisposition.NEEDS_MORE_INFO );
    }

    @Test
    @DisplayName("NEEDS_MORE_INFO -- the longest value -- survives the VARCHAR(16) column")
    public void longestEnumValue_roundTrips() {
        dispositionService.rule( audit, "f1", FindingDisposition.NEEDS_MORE_INFO,
                "alice", TriageJudgeKind.CURATOR, "the GEO record does not say" );
        sessionFactory.getCurrentSession().flush();
        sessionFactory.getCurrentSession().clear();

        AnnotationSet reloaded = annotationSetService.load( audit.getId() );
        assertThat( dispositionService.standingFor( reloaded ).get( "f1" ).getDisposition() )
                .isEqualTo( FindingDisposition.NEEDS_MORE_INFO );
        assertThat( FindingDisposition.NEEDS_MORE_INFO.getDbValue() ).isEqualTo( "needs_more_info" );
    }

    @Test
    @DisplayName("a finalized set refuses a ruling rather than dating one after its own summary")
    public void finalizedSet_refusesRuling() {
        annotationSetService.finalizeSet( audit.getId(), "alice", "closed out" );
        sessionFactory.getCurrentSession().flush();

        assertThatThrownBy( () -> dispositionService.rule( audit, "f1",
                FindingDisposition.ACCEPTED, "alice", TriageJudgeKind.CURATOR, null ) )
                .isInstanceOf( IllegalStateException.class )
                .hasMessageContaining( "reopen" );

        // ... and taking rulings again is exactly what reopening restores.
        annotationSetService.reopenSet( audit.getId() );
        sessionFactory.getCurrentSession().flush();
        assertThat( dispositionService.rule( audit, "f1", FindingDisposition.ACCEPTED,
                "alice", TriageJudgeKind.CURATOR, null ) ).isNotNull();
    }

    @Test
    @DisplayName("the batched read keys by set and omits sets nobody has ruled on")
    public void batchedRead_omitsUnruledSets() {
        AnnotationSet second = annotationSetService.attach( audit.getInvestigation(),
                        AnnotationSetRole.PROPOSAL, AnnotationSetSource.AGENT, AgentCurationKind.AUDIT,
                        "run-" + UUID.randomUUID(), "agent-2", null, null, null, "{}", null )
                .getAnnotationSet();
        dispositionService.rule( audit, "f1", FindingDisposition.ACCEPTED,
                "alice", TriageJudgeKind.CURATOR, null );
        sessionFactory.getCurrentSession().flush();

        Map<Long, Map<String, AnnotationSetDisposition>> batched =
                dispositionService.standingForIds( Arrays.asList( audit.getId(), second.getId() ) );
        assertThat( batched ).containsOnlyKeys( audit.getId() );
        assertThat( batched.get( audit.getId() ) ).containsOnlyKeys( "f1" );
    }

    @Test
    @DisplayName("needs_more_info without a reason is refused -- it would not differ from no ruling")
    public void needsMoreInfoWithoutReason_isRefused() {
        assertThatThrownBy( () -> dispositionService.rule( audit, "f1",
                FindingDisposition.NEEDS_MORE_INFO, "alice", TriageJudgeKind.CURATOR, null ) )
                .isInstanceOf( IllegalArgumentException.class )
                .hasMessageContaining( "needs_more_info" );

        // A blank one is the same thing wearing whitespace.
        assertThatThrownBy( () -> dispositionService.rule( audit, "f1",
                FindingDisposition.NEEDS_MORE_INFO, "alice", TriageJudgeKind.CURATOR, "   " ) )
                .isInstanceOf( IllegalArgumentException.class );

        // Nothing was written by either attempt.
        assertThat( dispositionService.findBySet( audit ) ).isEmpty();
    }

    @Test
    @DisplayName("the other two values still take a ruling with no reason, and keep one when given")
    public void acceptedAndDismissed_reasonStaysOptional() {
        // 🛑 The producing side treats a reason on these two as an error; Gemma deliberately does
        // not. This field's stated purpose is that a DISMISSED says what was wrong with the
        // finding -- that is what lets the agent stop emitting it -- so refusing it here would
        // forbid the case the column exists for.
        assertThat( dispositionService.rule( audit, "f1", FindingDisposition.ACCEPTED,
                "alice", TriageJudgeKind.CURATOR, null ).getReason() ).isNull();
        assertThat( dispositionService.rule( audit, "f2", FindingDisposition.DISMISSED,
                "alice", TriageJudgeKind.CURATOR, "the evidence quote is the wrong sample" )
                .getReason() ).isEqualTo( "the evidence quote is the wrong sample" );
    }

    @Test
    @DisplayName("a blank reason is stored as null so \"did they say why\" has one answer")
    public void blankReason_normalizesToNull() {
        AnnotationSetDisposition d = dispositionService.rule( audit, "f1",
                FindingDisposition.ACCEPTED, "alice", TriageJudgeKind.CURATOR, "   " );
        assertThat( d.getReason() ).isNull();
    }
}
