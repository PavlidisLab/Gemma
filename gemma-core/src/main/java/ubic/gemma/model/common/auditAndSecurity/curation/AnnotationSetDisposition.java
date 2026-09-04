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
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * <h2>One ruling on one finding inside an {@link AnnotationSet}.</h2>
 *
 * <p>A curator working an audit rules on each finding in turn —
 * {@link FindingDisposition#ACCEPTED} / {@link FindingDisposition#DISMISSED} /
 * {@link FindingDisposition#NEEDS_MORE_INFO}, with a reason. The finding is
 * named by {@link #targetId}, which is the producer's own {@code target_id}
 * from the payload; Gemma does not parse the payload and so cannot validate
 * it.</p>
 *
 * <p>🛑 <b>Set, finding and element are three scopes and this is the middle
 * one.</b> {@link AnnotationSetTriage} rules on the whole set;
 * {@link CurationDraftDispositions.Disposition} classifies one element within
 * one draft and is derived rather than stored. All three read like
 * dispositions and none of them substitutes for another.</p>
 *
 * <p><b>Append-only, latest wins</b> — deliberately the opposite choice from
 * {@link AnnotationSetTriage}, which keys {@code UNIQUE(set, judge)} and
 * upserts. There, the question is what a judge currently thinks about a whole
 * set. Here the sequence is itself the record: a finding that was accepted,
 * then dismissed after a second look, is a different history from one
 * dismissed outright, and an audit's value depends on being able to see that a
 * ruling moved. {@link #latestPerTarget(Collection)} applies the latest-wins
 * fold on read.</p>
 *
 * <p>{@link #judgeKind} reuses {@link TriageJudgeKind} rather than declaring a
 * second enum with the same two values. Its name says triage and its meaning —
 * did a person or a machine decide this — is the same question in both
 * places.</p>
 *
 * <p>No unique constraint, so nothing here stops two rows with the same
 * {@code (set, targetId, decidedBy)}. That is the point.</p>
 */
@Entity
@Table(name = "ANNOTATION_SET_DISPOSITION",
        indexes = @Index(name = "IDX_ANNOTATION_SET_DISPOSITION_SET_TARGET",
                columnList = "ANNOTATION_SET_FK,TARGET_ID,DECIDED_AT"))
public class AnnotationSetDisposition extends AbstractIdentifiable {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "ANNOTATION_SET_FK", nullable = false, columnDefinition = "BIGINT",
            foreignKey = @ForeignKey(name = "FK_ANNOTATION_SET_DISPOSITION_SET"))
    private AnnotationSet annotationSet;

    /**
     * Which finding, in the producer's own numbering. Opaque here: the payload
     * is stored as JSON text and never parsed, so an id naming no finding is
     * accepted and only the producer can tell.
     */
    @Column(name = "TARGET_ID", nullable = false, columnDefinition = "VARCHAR(255)")
    private String targetId;

    @Enumerated(EnumType.STRING)
    @Column(name = "DISPOSITION", nullable = false, columnDefinition = "VARCHAR(16)")
    private FindingDisposition disposition;

    /**
     * Username for a person, agent run id for a run — matching
     * {@link AnnotationSet#getCreatedBy()}, and {@code VARCHAR(255)} rather
     * than an FK to {@code CONTACT} for the same reason: an agent run has no
     * Contact row.
     */
    @Column(name = "DECIDED_BY", nullable = false, columnDefinition = "VARCHAR(255)")
    private String decidedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "JUDGE_KIND", nullable = false, columnDefinition = "VARCHAR(16)")
    private TriageJudgeKind judgeKind;

    @Column(name = "DECIDED_AT", nullable = false, columnDefinition = "DATETIME(3)")
    private Date decidedAt;

    /**
     * Why. The disposition records the decision; without this a
     * {@link FindingDisposition#DISMISSED} does not say what was wrong with
     * the finding, and that is what the agent needs in order to stop emitting
     * it.
     */
    @Column(name = "REASON", columnDefinition = "VARCHAR(1024)")
    private String reason;

    public AnnotationSetDisposition() {
    }

    /**
     * The standing ruling for each finding — the most recent row per
     * {@link #targetId}.
     * <p>
     * Ties on {@link #decidedAt} break on id, so two rulings written in the
     * same millisecond still resolve to one answer rather than to whichever
     * the collection happened to yield first.
     *
     * @return target id -> its latest ruling, in most-recent-first order.
     *         Findings nobody has ruled on are absent rather than mapped to
     *         null, so {@code containsKey} answers "has this been ruled on".
     */
    public static Map<String, AnnotationSetDisposition> latestPerTarget(
            @Nullable Collection<AnnotationSetDisposition> rulings ) {
        Map<String, AnnotationSetDisposition> out = new LinkedHashMap<>();
        if ( rulings == null ) {
            return out;
        }
        List<AnnotationSetDisposition> newestFirst = rulings.stream()
                .sorted( NEWEST_FIRST )
                .collect( Collectors.toList() );
        for ( AnnotationSetDisposition d : newestFirst ) {
            out.putIfAbsent( d.getTargetId(), d );
        }
        return out;
    }

    /**
     * Most recent first, id breaking a same-millisecond tie. The same ordering
     * the DAO applies in SQL, kept here so an in-memory fold cannot disagree
     * with a query.
     */
    public static final Comparator<AnnotationSetDisposition> NEWEST_FIRST =
            Comparator.comparing( AnnotationSetDisposition::getDecidedAt,
                            Comparator.nullsLast( Comparator.reverseOrder() ) )
                    .thenComparing( AnnotationSetDisposition::getId,
                            Comparator.nullsLast( Comparator.reverseOrder() ) );

    public AnnotationSet getAnnotationSet() {
        return annotationSet;
    }

    public void setAnnotationSet( AnnotationSet annotationSet ) {
        this.annotationSet = annotationSet;
    }

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId( String targetId ) {
        this.targetId = targetId;
    }

    public FindingDisposition getDisposition() {
        return disposition;
    }

    public void setDisposition( FindingDisposition disposition ) {
        this.disposition = disposition;
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

    @Nullable
    public String getReason() {
        return reason;
    }

    public void setReason( @Nullable String reason ) {
        this.reason = reason;
    }

    /**
     * Constant, deliberately — the same choice, for the same reasons, as
     * {@link AnnotationSetTriage#hashCode()}.
     * <p>
     * Hashing the business key would mean hashing {@link #annotationSet}, a
     * {@code LAZY} proxy whose {@code hashCode()} can force initialization
     * outside a session. An id-based hash is worse: the id flips from null to
     * a value on persist, so a ruling added to a {@code HashSet} while
     * transient lands in the wrong bucket and {@code contains()} then answers
     * false for an element that is in the set.
     * <p>
     * The cost is one bucket degrading to a linear scan. An audit carries tens
     * of findings, not thousands.
     */
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    /**
     * By id once both sides have one, otherwise by
     * {@code (set, targetId, decidedBy, decidedAt)}. The annotation set is
     * compared by id rather than by object so a lazy proxy on either side does
     * not have to be initialized to answer.
     * <p>
     * The fallback includes {@link #decidedAt} because these rows are
     * append-only: without it, a curator's second ruling on the same finding
     * would compare equal to their first and silently collapse in a
     * {@code Set}.
     */
    @Override
    public boolean equals( Object o ) {
        if ( this == o ) return true;
        if ( !( o instanceof AnnotationSetDisposition ) ) return false;
        AnnotationSetDisposition other = ( AnnotationSetDisposition ) o;
        if ( getId() != null && other.getId() != null ) {
            return getId().equals( other.getId() );
        }
        return Objects.equals( annotationSetId(), other.annotationSetId() )
                && Objects.equals( targetId, other.targetId )
                && Objects.equals( decidedBy, other.decidedBy )
                && Objects.equals( decidedAt, other.decidedAt );
    }

    @Nullable
    private Long annotationSetId() {
        return annotationSet != null ? annotationSet.getId() : null;
    }
}
