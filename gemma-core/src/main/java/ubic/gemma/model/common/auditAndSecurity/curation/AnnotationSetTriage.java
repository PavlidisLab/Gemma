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
import jakarta.persistence.UniqueConstraint;
import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.Collection;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;

/**
 * One judge's standing ruling on how much an {@link AnnotationSet} matters.
 *
 * <p><b>A row per judgement, not a column per judge.</b> Columns on
 * {@link AnnotationSet} would hard-code the set of judges into the schema —
 * agent and curator today, a second-opinion reviewer or an external
 * collaborator tomorrow, each one a migration. Rows make the judge data, and
 * they answer questions columns cannot: has any human ever ruled on this, do
 * two curators disagree, which agent build gets overruled most.</p>
 *
 * <p><b>One standing judgement per judge</b> —
 * {@code UNIQUE(annotationSet, judgedBy)}, upserted when a judge changes their
 * mind. The question is what a judge currently thinks, not an append-only log
 * of every time they toggled. Drop the constraint if the toggle history ever
 * becomes the point.</p>
 *
 * <p><b>The effective verdict is the most recent judgement</b> —
 * {@link #effective(Collection)}. Deliberately not "curator outranks agent":
 * a curator ruling after the agent already wins by recency, and when two
 * curators disagree the later one wins, which is the same answer the rest of
 * this workflow gives to contention. {@link #judgeKind} still records who, so
 * ranking by role stays possible without re-deciding it here.</p>
 *
 * <p>There is no stored "pending": an un-triaged set has no row. See
 * {@link TriageVerdict}.</p>
 */
@Entity
@Table(name = "ANNOTATION_SET_TRIAGE",
        uniqueConstraints = @UniqueConstraint(name = "UK_ANNOTATION_SET_TRIAGE_SET_JUDGE",
                columnNames = { "ANNOTATION_SET_FK", "JUDGED_BY" }),
        indexes = @Index(name = "IDX_ANNOTATION_SET_TRIAGE_SET_JUDGED",
                columnList = "ANNOTATION_SET_FK,JUDGED_AT"))
public class AnnotationSetTriage extends AbstractIdentifiable {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "ANNOTATION_SET_FK", nullable = false, columnDefinition = "BIGINT",
            foreignKey = @ForeignKey(name = "FK_ANNOTATION_SET_TRIAGE_SET"))
    private AnnotationSet annotationSet;

    @Enumerated(EnumType.STRING)
    @Column(name = "TRIAGE", nullable = false, columnDefinition = "VARCHAR(16)")
    private TriageVerdict verdict;

    /**
     * Username for a person, agent run id for a run. {@code VARCHAR(255)}
     * rather than an FK to {@code CONTACT}, matching
     * {@link AnnotationSet#getCreatedBy()} — an agent run has no Contact row
     * and an external reviewer may not either.
     */
    @Column(name = "JUDGED_BY", nullable = false, columnDefinition = "VARCHAR(255)")
    private String judgedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "JUDGE_KIND", nullable = false, columnDefinition = "VARCHAR(16)")
    private TriageJudgeKind judgeKind;

    @Column(name = "JUDGED_AT", nullable = false, columnDefinition = "DATETIME(3)")
    private Date judgedAt;

    /**
     * Why. Free text, and the only place a {@link TriageVerdict#WontFix} can
     * say what it is declining to fix — the verdict alone records the decision
     * but not the reason, and this is the field a later reader needs.
     */
    @Column(name = "NOTE", columnDefinition = "VARCHAR(1024)")
    private String note;

    public AnnotationSetTriage() {
    }

    /**
     * The most recent ruling among {@code judgements}, or empty when nobody
     * has ruled.
     * <p>
     * Ties on {@link #judgedAt} break on id, so two judgements written in the
     * same millisecond still resolve to one answer rather than to whichever
     * the collection happened to yield first.
     */
    public static Optional<AnnotationSetTriage> effective( @Nullable Collection<AnnotationSetTriage> judgements ) {
        if ( judgements == null || judgements.isEmpty() ) {
            return Optional.empty();
        }
        return judgements.stream().max(
                Comparator.comparing( AnnotationSetTriage::getJudgedAt,
                                Comparator.nullsFirst( Comparator.naturalOrder() ) )
                        .thenComparing( AnnotationSetTriage::getId,
                                Comparator.nullsFirst( Comparator.naturalOrder() ) ) );
    }

    /**
     * @return whether a person has ruled on this set — the distinction between
     *         a reviewed set and one only a machine has seen.
     */
    public static boolean reviewedByHuman( @Nullable Collection<AnnotationSetTriage> judgements ) {
        return judgements != null && judgements.stream()
                .anyMatch( t -> t.getJudgeKind() == TriageJudgeKind.CURATOR );
    }

    public AnnotationSet getAnnotationSet() {
        return annotationSet;
    }

    public void setAnnotationSet( AnnotationSet annotationSet ) {
        this.annotationSet = annotationSet;
    }

    public TriageVerdict getVerdict() {
        return verdict;
    }

    public void setVerdict( TriageVerdict verdict ) {
        this.verdict = verdict;
    }

    public String getJudgedBy() {
        return judgedBy;
    }

    public void setJudgedBy( String judgedBy ) {
        this.judgedBy = judgedBy;
    }

    public TriageJudgeKind getJudgeKind() {
        return judgeKind;
    }

    public void setJudgeKind( TriageJudgeKind judgeKind ) {
        this.judgeKind = judgeKind;
    }

    public Date getJudgedAt() {
        return judgedAt;
    }

    public void setJudgedAt( Date judgedAt ) {
        this.judgedAt = judgedAt;
    }

    @Nullable
    public String getNote() {
        return note;
    }

    public void setNote( @Nullable String note ) {
        this.note = note;
    }

    /**
     * Constant, deliberately.
     * <p>
     * The business key here is {@code (annotationSet, judgedBy)}, and hashing
     * it would mean hashing {@link #annotationSet} — a {@code LAZY} proxy
     * whose {@code hashCode()} can force initialization outside a session. An
     * id-based hash is worse still: the id flips from null to a value on
     * persist, so a judgement added to a {@code HashSet} while transient ends
     * up in the wrong bucket and {@code contains()} then answers false for an
     * element that is in the set.
     * <p>
     * The cost is that one bucket degrades to a linear scan. A set carries a
     * handful of judgements — one per judge — so that is not measurable here.
     */
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    /**
     * By id once both sides have one, otherwise by the business key. The
     * annotation set is compared by id rather than by object so a lazy proxy
     * on either side does not have to be initialized to answer.
     */
    @Override
    public boolean equals( Object o ) {
        if ( this == o ) return true;
        if ( !( o instanceof AnnotationSetTriage ) ) return false;
        AnnotationSetTriage other = ( AnnotationSetTriage ) o;
        if ( getId() != null && other.getId() != null ) {
            return getId().equals( other.getId() );
        }
        return Objects.equals( annotationSetId(), other.annotationSetId() )
                && Objects.equals( judgedBy, other.judgedBy );
    }

    @Nullable
    private Long annotationSetId() {
        return annotationSet != null ? annotationSet.getId() : null;
    }
}
