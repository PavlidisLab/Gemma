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
package ubic.gemma.model.common.auditAndSecurity.curation;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import ubic.gemma.model.analysis.Investigation;
import ubic.gemma.model.common.AbstractIdentifiable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * <h2>A curator's standing ruling that a change must NOT be made — or may be.</h2>
 *
 * <p>A refusal has nothing to commit: the whole content is the "no". Before
 * this, a curator who ruled against a proposed edit could only delete the
 * proposal, losing the fact that they had considered it, or leave it, where it
 * reads as pending forever. Measured on the curation ledger: 29 of 821 rulings
 * are refusals ({@code reject_add}, {@code reject_factor}, {@code reject_drop},
 * {@code reject_all}) and no API verb expressed any of them.</p>
 *
 * <p>🛑 <b>Keyed on CONTENT, never on a proposed item's id.</b> The purpose is
 * to stop the same edit being proposed again next quarter, and next quarter's
 * proposal is a new item with a new id — so a ruling keyed on the item in front
 * of the curator can never match the thing it is meant to prevent, and would
 * quietly decay into an audit record. {@link #getDecisionKey()} is therefore a
 * description of WHAT was refused, computed by the producer, and
 * {@link CurationDecisionScope} says how wide it reaches.</p>
 *
 * <p><b>Gemma does not interpret the key.</b> It is opaque here exactly as
 * {@link AnnotationSetDisposition#getTargetId()} is: the side that understands
 * the curation vocabulary computes and matches it, which keeps that vocabulary
 * out of this schema so a new kind of refusal is not a new migration.
 * So Gemma RECORDS a refusal and cannot ENFORCE one. A commit that violates a
 * standing refusal is not rejected here; the proposing side is where the
 * meaning lives and where the gate belongs.</p>
 *
 * <p><b>Per experiment.</b> A ruling that applies corpus-wide is a CONVENTION —
 * "a strain is not a genotype" — and belongs in the curation rules the agent
 * reads, not here: it has no dataset to attach to, no ACL scope, and nothing in
 * Gemma would ever read it back.</p>
 *
 * <p><b>Append-only, latest wins</b>, like {@link AnnotationSetDisposition}. A
 * curator who lifts a refusal writes an {@link CurationDecisionType#ALLOWED}
 * row rather than deleting the "no", so why it was refused in the first place
 * survives the reversal.</p>
 */
@Entity
@Table(name = "CURATION_DECISION",
        indexes = @Index(name = "IDX_CURATION_DECISION_INV_KEY",
                columnList = "INVESTIGATION_FK,DECISION_KEY,DECIDED_AT"))
public class CurationDecision extends AbstractIdentifiable {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "INVESTIGATION_FK", nullable = false, columnDefinition = "BIGINT",
            foreignKey = @ForeignKey(name = "FK_CURATION_DECISION_INVESTIGATION"))
    private Investigation investigation;

    @Enumerated(EnumType.STRING)
    @Column(name = "DECISION", nullable = false, columnDefinition = "VARCHAR(16)")
    private CurationDecisionType decision;

    @Enumerated(EnumType.STRING)
    @Column(name = "DECISION_SCOPE", nullable = false, columnDefinition = "VARCHAR(16)")
    private CurationDecisionScope scope;

    /**
     * WHAT was ruled on, in the producer's own terms — a tag's category and
     * value, a factor key, whatever identifies the change rather than the
     * instance of it. Opaque here; Gemma never parses it.
     * <p>
     * Null only for {@link CurationDecisionScope#PROPOSAL}, which names an
     * annotation set instead.
     */
    @Column(name = "DECISION_KEY", columnDefinition = "VARCHAR(255)")
    private String decisionKey;

    /**
     * The proposal this answered, when there was one. Most rulings have none —
     * measured on the ledger, only 21 of 821 name an originating finding, the
     * rest coming from chat, corpus sweeps and recovery — so this is nullable
     * and a decision stands on its own without it.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "ANNOTATION_SET_FK", columnDefinition = "BIGINT",
            foreignKey = @ForeignKey(name = "FK_CURATION_DECISION_ANNOTATION_SET"))
    private AnnotationSet annotationSet;

    /**
     * Why — REQUIRED, unlike the reason on a disposition. A refusal has no
     * other content: without it the row says a change was rejected and gives a
     * later reader nothing to decide whether the rejection still applies, which
     * is the whole reason the row is kept.
     */
    @Column(name = "REASON", nullable = false, columnDefinition = "VARCHAR(1024)")
    private String reason;

    /**
     * Username for a person, agent run id for a run — as elsewhere in curation,
     * {@code VARCHAR(255)} rather than an FK to {@code CONTACT}.
     */
    @Column(name = "DECIDED_BY", nullable = false, columnDefinition = "VARCHAR(255)")
    private String decidedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "JUDGE_KIND", nullable = false, columnDefinition = "VARCHAR(16)")
    private TriageJudgeKind judgeKind;

    @Column(name = "DECIDED_AT", nullable = false, columnDefinition = "DATETIME(3)")
    private Date decidedAt;

    /**
     * Most recent first, id breaking a same-millisecond tie — the same ordering
     * the DAO applies in SQL.
     */
    public static final Comparator<CurationDecision> NEWEST_FIRST =
            Comparator.comparing( CurationDecision::getDecidedAt,
                            Comparator.nullsLast( Comparator.reverseOrder() ) )
                    .thenComparing( CurationDecision::getId,
                            Comparator.nullsLast( Comparator.reverseOrder() ) );

    public CurationDecision() {
    }

    /**
     * The standing decision under each key — the most recent per
     * {@link #standingKey()}.
     *
     * @param decisions the rows to fold, in any order
     * @return the standing decisions, most recent first
     */
    public static List<CurationDecision> standing( @Nullable Collection<CurationDecision> decisions ) {
        if ( decisions == null ) {
            return new ArrayList<>();
        }
        Map<String, CurationDecision> byKey = new LinkedHashMap<>();
        List<CurationDecision> newestFirst = decisions.stream()
                .sorted( NEWEST_FIRST )
                .collect( Collectors.toList() );
        for ( CurationDecision d : newestFirst ) {
            byKey.putIfAbsent( d.standingKey(), d );
        }
        return new ArrayList<>( byKey.values() );
    }

    /**
     * What this ruling supersedes: another ruling of the same scope on the same
     * key, or on the same proposal.
     * <p>
     * 🛑 The scope is part of the key. A ruling on one item does NOT supersede a
     * ruling on the whole key it belongs to, nor the reverse — they are
     * decisions of different breadth, and a curator who refuses a whole factor
     * key has not thereby reversed an earlier ruling on one value of it.
     *
     * @return the key this decision stands under
     */
    public String standingKey() {
        String subject = scope == CurationDecisionScope.PROPOSAL
                ? String.valueOf( annotationSet != null ? annotationSet.getId() : null )
                : decisionKey;
        return ( scope != null ? scope.name() : "?" ) + " " + subject;
    }

    public Investigation getInvestigation() {
        return investigation;
    }

    public void setInvestigation( Investigation investigation ) {
        this.investigation = investigation;
    }

    public CurationDecisionType getDecision() {
        return decision;
    }

    public void setDecision( CurationDecisionType decision ) {
        this.decision = decision;
    }

    public CurationDecisionScope getScope() {
        return scope;
    }

    public void setScope( CurationDecisionScope scope ) {
        this.scope = scope;
    }

    @Nullable
    public String getDecisionKey() {
        return decisionKey;
    }

    public void setDecisionKey( @Nullable String decisionKey ) {
        this.decisionKey = decisionKey;
    }

    @Nullable
    public AnnotationSet getAnnotationSet() {
        return annotationSet;
    }

    public void setAnnotationSet( @Nullable AnnotationSet annotationSet ) {
        this.annotationSet = annotationSet;
    }

    public String getReason() {
        return reason;
    }

    public void setReason( String reason ) {
        this.reason = reason;
    }

    public String getDecidedBy() {
        return decidedBy;
    }

    public void setDecidedBy( String decidedBy ) {
        this.decidedBy = decidedBy;
    }

    public TriageJudgeKind getJudgeKind() {
        return judgeKind;
    }

    public void setJudgeKind( TriageJudgeKind judgeKind ) {
        this.judgeKind = judgeKind;
    }

    public Date getDecidedAt() {
        return decidedAt;
    }

    public void setDecidedAt( Date decidedAt ) {
        this.decidedAt = decidedAt;
    }

    /**
     * Constant, deliberately — the same choice, for the same reasons, as
     * {@link AnnotationSetTriage#hashCode()}: hashing the business key would
     * mean hashing a {@code LAZY} proxy, and an id-based hash moves the object
     * between buckets when it is persisted.
     */
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    /**
     * By id once both sides have one, otherwise by
     * {@code (investigation, scope, key, decidedBy, decidedAt)}. The
     * investigation is compared by id so a lazy proxy need not be initialized,
     * and {@code decidedAt} is included because these rows are append-only: a
     * curator's second ruling on one key must not compare equal to their first.
     */
    @Override
    public boolean equals( Object o ) {
        if ( this == o ) return true;
        if ( !( o instanceof CurationDecision ) ) return false;
        CurationDecision other = ( CurationDecision ) o;
        if ( getId() != null && other.getId() != null ) {
            return getId().equals( other.getId() );
        }
        return Objects.equals( investigationId(), other.investigationId() )
                && scope == other.scope
                && Objects.equals( decisionKey, other.decisionKey )
                && Objects.equals( decidedBy, other.decidedBy )
                && Objects.equals( decidedAt, other.decidedAt );
    }

    @Nullable
    private Long investigationId() {
        return investigation != null ? investigation.getId() : null;
    }
}
