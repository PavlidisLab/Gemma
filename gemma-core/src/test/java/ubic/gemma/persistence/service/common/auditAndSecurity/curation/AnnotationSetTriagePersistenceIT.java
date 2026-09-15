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
import ubic.gemma.model.common.auditAndSecurity.curation.AnnotationSetRole;
import ubic.gemma.model.common.auditAndSecurity.curation.AnnotationSetSource;
import ubic.gemma.model.common.auditAndSecurity.curation.AnnotationSetTriage;
import ubic.gemma.model.common.auditAndSecurity.curation.TriageJudgeKind;
import ubic.gemma.model.common.auditAndSecurity.curation.TriageVerdict;
import ubic.gemma.model.expression.experiment.AgentCurationKind;
import ubic.gemma.model.expression.experiment.PreboardedExperiment;
import ubic.gemma.model.expression.experiment.WorkflowState;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Persistence contract for {@link AnnotationSetTriage} — the row-per-judge
 * triage model.
 * <p>
 * The properties worth pinning are the ones a column-per-judge design would
 * not have had: that two judges coexist, that one judge has exactly one
 * standing ruling, and that "effective" means most recent rather than
 * curator-outranks-agent.
 */
@Transactional
public class AnnotationSetTriagePersistenceIT extends BaseIntegrationTest5 {

    @Autowired
    private AnnotationSetService annotationSetService;

    @Autowired
    private AnnotationSetTriageService triageService;

    @Autowired
    private SessionFactory sessionFactory;

    private AnnotationSet proposal;

    @BeforeEach
    public void seed() {
        PreboardedExperiment preboarded = new PreboardedExperiment();
        preboarded.setAccession( "GSE-triage-it-" + UUID.randomUUID() );
        preboarded.setSource( "GEO" );
        preboarded.setName( "AnnotationSetTriageIT preboarded" );
        preboarded.setWorkflowState( WorkflowState.Preboarded );
        sessionFactory.getCurrentSession().persist( preboarded );
        sessionFactory.getCurrentSession().flush();

        proposal = annotationSetService.attach( preboarded, AnnotationSetRole.PROPOSAL,
                AnnotationSetSource.AGENT, AgentCurationKind.PROPOSAL,
                "run-" + UUID.randomUUID(), "agent-1", null, null, null,
                "{\"factors\":[]}", null )
                .getAnnotationSet();
        sessionFactory.getCurrentSession().flush();
    }

    @Test
    @DisplayName("an un-triaged set has no row at all -- absence is the state")
    public void untriaged_hasNoRow() {
        assertThat( triageService.findBySet( proposal ) ).isEmpty();
        assertThat( triageService.effectiveFor( proposal ) ).isEmpty();
        assertThat( triageService.reviewedByHuman( proposal ) ).isFalse();
    }

    @Test
    @DisplayName("two judges coexist -- the thing a column per judge could not do")
    public void twoJudgesCoexist() {
        triageService.judge( proposal, TriageVerdict.MustFix, "triage-run-7", TriageJudgeKind.AGENT, null );
        triageService.judge( proposal, TriageVerdict.WontFix, "alice", TriageJudgeKind.CURATOR, "batch artifact" );
        sessionFactory.getCurrentSession().flush();

        List<AnnotationSetTriage> all = triageService.findBySet( proposal );
        assertThat( all ).hasSize( 2 );
        assertThat( all ).extracting( AnnotationSetTriage::getJudgedBy )
                .containsExactlyInAnyOrder( "triage-run-7", "alice" );
        // The agent's verdict is still on file, not overwritten by the curator's.
        assertThat( all ).extracting( AnnotationSetTriage::getVerdict )
                .containsExactlyInAnyOrder( TriageVerdict.MustFix, TriageVerdict.WontFix );
    }

    @Test
    @DisplayName("one standing ruling per judge -- a second call updates rather than appends")
    public void sameJudgeTwice_upsertsInPlace() {
        AnnotationSetTriage first = triageService.judge( proposal, TriageVerdict.MightFix,
                "alice", TriageJudgeKind.CURATOR, "not sure" );
        sessionFactory.getCurrentSession().flush();
        Long firstId = first.getId();

        AnnotationSetTriage second = triageService.judge( proposal, TriageVerdict.MustFix,
                "alice", TriageJudgeKind.CURATOR, "decided" );
        sessionFactory.getCurrentSession().flush();

        assertThat( second.getId() ).isEqualTo( firstId );
        assertThat( triageService.findBySet( proposal ) ).hasSize( 1 );
        assertThat( second.getVerdict() ).isEqualTo( TriageVerdict.MustFix );
        assertThat( second.getNote() ).isEqualTo( "decided" );
    }

    @Test
    @DisplayName("the effective ruling is the most recent, not the curator's by rank")
    public void effectiveIsMostRecent_notCuratorByRank() {
        // Curator rules first, agent second. If precedence were by role the
        // curator would win; it is by recency, so the agent does.
        AnnotationSetTriage curator = triageService.judge( proposal, TriageVerdict.WontFix,
                "alice", TriageJudgeKind.CURATOR, null );
        curator.setJudgedAt( new Date( System.currentTimeMillis() - 60_000L ) );
        triageService.judge( proposal, TriageVerdict.MustFix, "triage-run-7", TriageJudgeKind.AGENT, null );
        sessionFactory.getCurrentSession().flush();

        Optional<AnnotationSetTriage> effective = triageService.effectiveFor( proposal );
        assertThat( effective ).isPresent();
        assertThat( effective.get().getJudgedBy() ).isEqualTo( "triage-run-7" );
        assertThat( effective.get().getVerdict() ).isEqualTo( TriageVerdict.MustFix );
    }

    @Test
    @DisplayName("reviewedByHuman distinguishes a reviewed set from a machine-seen one")
    public void reviewedByHuman_onlyWhenACuratorRuled() {
        triageService.judge( proposal, TriageVerdict.MustFix, "triage-run-7", TriageJudgeKind.AGENT, null );
        sessionFactory.getCurrentSession().flush();
        assertThat( triageService.reviewedByHuman( proposal ) ).isFalse();

        triageService.judge( proposal, TriageVerdict.Fine, "alice", TriageJudgeKind.CURATOR, null );
        sessionFactory.getCurrentSession().flush();
        assertThat( triageService.reviewedByHuman( proposal ) ).isTrue();
    }

    @Test
    @DisplayName("withdrawing the only ruling returns the set to un-triaged")
    public void withdraw_returnsToUntriaged() {
        triageService.judge( proposal, TriageVerdict.Fine, "alice", TriageJudgeKind.CURATOR, null );
        sessionFactory.getCurrentSession().flush();

        assertThat( triageService.withdraw( proposal, "alice" ) ).isTrue();
        sessionFactory.getCurrentSession().flush();
        assertThat( triageService.effectiveFor( proposal ) ).isEmpty();
        // Withdrawing again is not an error; it reports that nothing was there.
        assertThat( triageService.withdraw( proposal, "alice" ) ).isFalse();
    }

    @Test
    @DisplayName("the batched effective lookup omits sets nobody ruled on")
    public void effectiveForIds_omitsUnruledSets() {
        AnnotationSet second = annotationSetService.attach( proposal.getInvestigation(),
                AnnotationSetRole.PROPOSAL, AnnotationSetSource.AGENT, AgentCurationKind.PROPOSAL,
                "run-" + UUID.randomUUID(), "agent-2", null, null, null,
                "{\"factors\":[]}", null )
                .getAnnotationSet();
        triageService.judge( proposal, TriageVerdict.MustFix, "alice", TriageJudgeKind.CURATOR, null );
        sessionFactory.getCurrentSession().flush();

        Map<Long, AnnotationSetTriage> map = triageService.effectiveForIds(
                Arrays.asList( proposal.getId(), second.getId() ) );
        assertThat( map ).containsKey( proposal.getId() );
        // Absent rather than mapped to null, so containsKey answers "has anyone ruled".
        assertThat( map ).doesNotContainKey( second.getId() );
    }

    @Test
    @DisplayName("a note longer than the column is truncated, not rejected")
    public void overlongNoteIsTruncated() {
        String longNote = String.join( "", java.util.Collections.nCopies( 200, "0123456789" ) );
        AnnotationSetTriage t = triageService.judge( proposal, TriageVerdict.WontFix,
                "alice", TriageJudgeKind.CURATOR, longNote );
        sessionFactory.getCurrentSession().flush();
        // Losing the tail of an explanation beats refusing a ruling already made.
        assertThat( t.getNote() ).hasSize( 1024 );
        assertThat( t.getVerdict() ).isEqualTo( TriageVerdict.WontFix );
    }
}
