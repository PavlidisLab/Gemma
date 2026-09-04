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
import ubic.gemma.model.common.auditAndSecurity.curation.CurationDecision;
import ubic.gemma.model.common.auditAndSecurity.curation.CurationDecisionScope;
import ubic.gemma.model.common.auditAndSecurity.curation.CurationDecisionType;
import ubic.gemma.model.common.auditAndSecurity.curation.TriageJudgeKind;
import ubic.gemma.model.expression.experiment.AgentCurationKind;
import ubic.gemma.model.expression.experiment.PreboardedExperiment;
import ubic.gemma.model.expression.experiment.WorkflowState;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Persistence contract for {@link CurationDecision} -- the record of a change a
 * curator ruled must NOT be made.
 *
 * <p>The properties worth pinning are the ones that decide whether the row can
 * do its job: that a refusal survives without anything to commit, that lifting
 * one keeps the original "no", and that a ruling on one item does not silently
 * reverse a ruling on the whole key it belongs to.</p>
 */
@Transactional
public class CurationDecisionPersistenceIT extends BaseIntegrationTest5 {

    @Autowired
    private CurationDecisionService decisionService;

    @Autowired
    private AnnotationSetService annotationSetService;

    @Autowired
    private SessionFactory sessionFactory;

    private PreboardedExperiment experiment;

    @BeforeEach
    public void seed() {
        experiment = new PreboardedExperiment();
        experiment.setAccession( "GSE-decision-it-" + UUID.randomUUID() );
        experiment.setSource( "GEO" );
        experiment.setName( "CurationDecisionIT preboarded" );
        experiment.setWorkflowState( WorkflowState.Preboarded );
        sessionFactory.getCurrentSession().persist( experiment );
        sessionFactory.getCurrentSession().flush();
    }

    @Test
    @DisplayName("a refusal survives with nothing to commit -- the whole content is the no")
    public void refusal_standsOnItsOwn() {
        CurationDecision d = decisionService.decide( experiment, CurationDecisionType.REFUSED,
                CurationDecisionScope.ITEM, "tag:strain/cba/j", null,
                "the strain is already carried by the genotype factor", "alice",
                TriageJudgeKind.CURATOR );
        sessionFactory.getCurrentSession().flush();

        assertThat( d.getId() ).isNotNull();
        assertThat( d.getAnnotationSet() ).as( "no proposal need exist" ).isNull();
        assertThat( decisionService.standingFor( experiment ) ).singleElement()
                .extracting( CurationDecision::getDecision ).isEqualTo( CurationDecisionType.REFUSED );
    }

    @Test
    @DisplayName("lifting a refusal keeps the original no -- append-only, latest wins")
    public void liftingARefusal_keepsTheOriginal() {
        decisionService.decide( experiment, CurationDecisionType.REFUSED,
                CurationDecisionScope.ITEM, "tag:strain/cba/j", null,
                "duplicated by the genotype factor", "alice", TriageJudgeKind.CURATOR );
        decisionService.decide( experiment, CurationDecisionType.ALLOWED,
                CurationDecisionScope.ITEM, "tag:strain/cba/j", null,
                "the genotype factor was dropped, so the tag is needed after all", "alice",
                TriageJudgeKind.CURATOR );
        sessionFactory.getCurrentSession().flush();

        // Deleting the "no" would lose why it was refused, which is what a later reader needs in
        // order to judge whether the reversal was right.
        List<CurationDecision> history = decisionService.historyForKey( experiment, "tag:strain/cba/j" );
        assertThat( history ).hasSize( 2 );
        assertThat( history.get( 0 ).getDecision() ).isEqualTo( CurationDecisionType.ALLOWED );
        assertThat( history.get( 1 ).getDecision() ).isEqualTo( CurationDecisionType.REFUSED );
        assertThat( history.get( 1 ).getReason() ).isEqualTo( "duplicated by the genotype factor" );

        // ... and "refused, then lifted" is visible as allowed rather than vanishing, because it
        // is a different state from never having been ruled on.
        assertThat( decisionService.standingFor( experiment ) ).singleElement()
                .extracting( CurationDecision::getDecision ).isEqualTo( CurationDecisionType.ALLOWED );
    }

    @Test
    @DisplayName("an ITEM ruling does not supersede a KEY ruling on the same key")
    public void scopeIsPartOfTheKey() {
        // reject_factor gates a whole key including siblings not yet proposed; a later ruling on
        // one item under it is a narrower decision, not a reversal of the broader one.
        decisionService.decide( experiment, CurationDecisionType.REFUSED,
                CurationDecisionScope.KEY, "factor:genotype", null,
                "this key is not to be proposed for this experiment", "alice",
                TriageJudgeKind.CURATOR );
        decisionService.decide( experiment, CurationDecisionType.ALLOWED,
                CurationDecisionScope.ITEM, "factor:genotype", null,
                "this one value is fine", "alice", TriageJudgeKind.CURATOR );
        sessionFactory.getCurrentSession().flush();

        List<CurationDecision> standing = decisionService.standingFor( experiment );
        assertThat( standing ).hasSize( 2 );
        assertThat( standing ).extracting( CurationDecision::getScope )
                .containsExactlyInAnyOrder( CurationDecisionScope.KEY, CurationDecisionScope.ITEM );
    }

    @Test
    @DisplayName("a reason is required -- a refusal with no reason records nothing usable")
    public void reasonIsRequired() {
        assertThatThrownBy( () -> decisionService.decide( experiment, CurationDecisionType.REFUSED,
                CurationDecisionScope.ITEM, "tag:strain/cba/j", null, "   ", "alice",
                TriageJudgeKind.CURATOR ) )
                .isInstanceOf( IllegalArgumentException.class )
                .hasMessageContaining( "reason is required" );

        assertThat( decisionService.findByInvestigation( experiment ) ).isEmpty();
    }

    @Test
    @DisplayName("scope and subject have to agree")
    public void scopeAndSubject_mustAgree() {
        // An ITEM or KEY ruling with no key has no subject, so it could never be matched against a
        // later proposal -- which is the only thing it exists to do.
        assertThatThrownBy( () -> decisionService.decide( experiment, CurationDecisionType.REFUSED,
                CurationDecisionScope.KEY, null, null, "because", "alice", TriageJudgeKind.CURATOR ) )
                .isInstanceOf( IllegalArgumentException.class )
                .hasMessageContaining( "decisionKey" );

        // A PROPOSAL ruling names the set it answers instead of a key.
        assertThatThrownBy( () -> decisionService.decide( experiment, CurationDecisionType.REFUSED,
                CurationDecisionScope.PROPOSAL, null, null, "all of it", "alice",
                TriageJudgeKind.CURATOR ) )
                .isInstanceOf( IllegalArgumentException.class )
                .hasMessageContaining( "annotation set" );
    }

    @Test
    @DisplayName("a proposal-scoped refusal names the set it answered")
    public void proposalScoped_namesTheSet() {
        AnnotationSet proposal = annotationSetService.attach( experiment, AnnotationSetRole.PROPOSAL,
                        AnnotationSetSource.AGENT, AgentCurationKind.PROPOSAL,
                        "run-" + UUID.randomUUID(), "agent-1", null, null, null, "{}", null )
                .getAnnotationSet();
        sessionFactory.getCurrentSession().flush();

        CurationDecision d = decisionService.decide( experiment, CurationDecisionType.REFUSED,
                CurationDecisionScope.PROPOSAL, null, proposal,
                "none of this pass is right for this experiment", "alice", TriageJudgeKind.CURATOR );
        sessionFactory.getCurrentSession().flush();

        assertThat( d.getAnnotationSet() ).isNotNull();
        assertThat( d.getAnnotationSet().getId() ).isEqualTo( proposal.getId() );
        assertThat( decisionService.standingFor( experiment ) ).singleElement()
                .extracting( CurationDecision::getScope ).isEqualTo( CurationDecisionScope.PROPOSAL );
    }

    @Test
    @DisplayName("the reason survives a reload -- it is the part a later reader needs")
    public void reasonSurvivesReload() {
        decisionService.decide( experiment, CurationDecisionType.REFUSED,
                CurationDecisionScope.KEY, "factor:genotype", null,
                "a strain is not a genotype for this experiment", "alice", TriageJudgeKind.CURATOR );
        sessionFactory.getCurrentSession().flush();
        sessionFactory.getCurrentSession().clear();

        List<CurationDecision> standing = decisionService.standingFor( experiment );
        assertThat( standing ).singleElement()
                .extracting( CurationDecision::getReason )
                .isEqualTo( "a strain is not a genotype for this experiment" );
    }
}
