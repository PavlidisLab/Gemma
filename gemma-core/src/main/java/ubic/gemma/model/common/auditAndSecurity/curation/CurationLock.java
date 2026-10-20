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
import ubic.gemma.model.analysis.Investigation;
import org.hibernate.annotations.OnDeleteAction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Date;

/**
 * An advisory, steal-able claim on a dataset's curation.
 *
 * <p>One row per <em>currently locked</em> dataset, deleted on release. The
 * primary key is the investigation itself, so a dataset cannot be locked
 * twice.</p>
 *
 * <p>🛑 <b>Advisory, and it must stay that way.</b> The correctness guarantee
 * for concurrent curation writes is the optimistic-concurrency token —
 * {@code PUT /datasets/{id}/curation} checks {@code baseline.lastModified} and
 * returns 409 when the dataset moved. This lock exists so that 409 rarely
 * fires and so a curator can see who else is working. It gates exactly one
 * thing: sign-off, which is the destructive act. Editing and committing stay
 * advisory. If the baseline check is ever removed because "the lock handles
 * it", that is the bug.</p>
 *
 * <p>🛑 <b>Never write this through a curatable update path, and never emit an
 * audit event for it.</b> Any audit event sets
 * {@code curationDetails.lastUpdated}
 * ({@code AbstractCuratableDao.updateCurationDetailsFromAuditEvent}), and that
 * field is the concurrency token above — so taking a lock would 409 every
 * in-flight draft on the dataset. That is the bug {@code bebe778980} fixed for
 * snapshots. {@link #stolenFrom} / {@link #stolenAt} are the record of a
 * steal, for the same reason the SNAPSHOT row is its own record of a
 * capture.</p>
 *
 * <p><b>Stealing is always permitted.</b> It loses no work: the displaced
 * curator's DRAFT is a separate {@link AnnotationSet} row and is untouched, so
 * the cost of a steal is that the displaced curator's next commit 409s on a
 * stale baseline and they re-sync.</p>
 *
 * <p><b>Expiry is not swept.</b> An acquire treats {@link #expiresAt} in the
 * past as free and overwrites the row, so an abandoned tab frees itself with
 * no cleanup job to run, monitor and forget.</p>
 */
@Entity
@Table(name = "CURATION_LOCK")
public class CurationLock implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * The locked investigation, and the primary key. Shares the row's
     * identity rather than carrying a surrogate id: "which dataset is this a
     * lock on" and "which lock is this" are the same question.
     * <p>
     * Typed {@link Investigation}, matching both the FK target
     * ({@code INVESTIGATION(ID)}) and {@link AnnotationSet#getInvestigation()}
     * — a preboarded experiment is an Investigation but not an
     * ExpressionExperiment, and curation starts before promotion.
     */
    /**
     * The primary key, derived from {@link #investigation} by {@code @MapsId} rather than declared
     * on the association itself.
     * <p>
     * With {@code @Id} on the {@code @OneToOne}, Hibernate treats the identifier type as the entity
     * and warns HHH000038/HHH000039 that the composite-id class overrides neither equals() nor
     * hashCode() — on every startup. Supplying those on the entity is the wrong answer here: an id
     * that flips from null to a value on persist is the hashCode footgun this codebase has been bitten
     * by. A derived Long identifier removes the composite id altogether, and makes
     * {@code session.get( CurationLock.class, ee.getId() )} correct by construction rather than by
     * coincidence — that call already passed a Long against an identifier declared as an
     * Investigation.
     * <p>
     * The table is unchanged: INVESTIGATION_FK remains the single primary-key column, so no
     * migration goes with this.
     */
    @Id
    private Long investigationId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "INVESTIGATION_FK", nullable = false, columnDefinition = "BIGINT",
            foreignKey = @ForeignKey(name = "FK_CURATION_LOCK_INVESTIGATION"))
    private Investigation investigation;

    /**
     * Whatever identity the holder authenticated as. {@code VARCHAR(255)}
     * rather than an FK to {@code CONTACT}, matching
     * {@link AnnotationSet#getCreatedBy()} — an FK would make the lock
     * un-writable for any identity without a Gemma {@code Contact} row.
     */
    @Column(name = "LOCKED_BY", nullable = false, columnDefinition = "VARCHAR(255)")
    private String lockedBy;

    @Column(name = "LOCKED_AT", nullable = false, columnDefinition = "DATETIME(3)")
    private Date lockedAt;

    /**
     * When the claim lapses. Refreshed by curator activity (each draft
     * autosave), so working holds the lease and walking away releases it —
     * one signal rather than an explicit unlock nobody remembers.
     */
    @Column(name = "EXPIRES_AT", nullable = false, columnDefinition = "DATETIME(3)")
    private Date expiresAt;

    /** Previous holder, when this row was taken by a steal. */
    @Column(name = "STOLEN_FROM", columnDefinition = "VARCHAR(255)")
    private String stolenFrom;

    @Column(name = "STOLEN_AT", columnDefinition = "DATETIME(3)")
    private Date stolenAt;

    /**
     * What is holding this, when the holder is a job rather than a person.
     * <p>
     * A blocked curator has to choose between waiting and stealing, and {@link #lockedBy} alone cannot tell
     * them: an agent acting via {@code ?onBehalfOf=} records the CURATOR there, which is right, and leaves
     * nothing naming the run. Written at acquire time because a batch takes its locks BEFORE doing the work,
     * so joining to the holder's draft would answer only once the answer stopped being needed.
     * <p>
     * Null for a person. Matches {@code ANNOTATION_SET.RUN_ID}, so the same run is the same string in both
     * places -- {@code adhoc-decision-ticket}, {@code category-policy-rebuild-2026-08-09}.
     */
    @Column(name = "RUN_ID", columnDefinition = "VARCHAR(255)")
    private String runId;

    /** Which agent, when the holder is one. Null for a person. Matches {@code ANNOTATION_SET.AGENT_NAME}. */
    @Column(name = "AGENT_NAME", columnDefinition = "VARCHAR(255)")
    private String agentName;

    public CurationLock() {
    }

    @Nullable
    public String getRunId() {
        return runId;
    }

    public void setRunId( @Nullable String runId ) {
        this.runId = runId;
    }

    @Nullable
    public String getAgentName() {
        return agentName;
    }

    public void setAgentName( @Nullable String agentName ) {
        this.agentName = agentName;
    }

    /** @return the primary key, which is the locked investigation's id. */
    @Nullable
    public Long getInvestigationId() {
        return investigationId;
    }

    public Investigation getInvestigation() {
        return investigation;
    }

    public void setInvestigation( Investigation investigation ) {
        this.investigation = investigation;
    }

    public String getLockedBy() {
        return lockedBy;
    }

    public void setLockedBy( String lockedBy ) {
        this.lockedBy = lockedBy;
    }

    public Date getLockedAt() {
        return lockedAt;
    }

    public void setLockedAt( Date lockedAt ) {
        this.lockedAt = lockedAt;
    }

    public Date getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt( Date expiresAt ) {
        this.expiresAt = expiresAt;
    }

    @Nullable
    public String getStolenFrom() {
        return stolenFrom;
    }

    public void setStolenFrom( @Nullable String stolenFrom ) {
        this.stolenFrom = stolenFrom;
    }

    @Nullable
    public Date getStolenAt() {
        return stolenAt;
    }

    public void setStolenAt( @Nullable Date stolenAt ) {
        this.stolenAt = stolenAt;
    }

    /**
     * @param now the reference instant, passed in rather than read from the
     *            clock so callers can evaluate a batch of locks against one
     *            consistent moment
     * @return whether the claim has lapsed. An expired lock is treated as free
     *         by an acquire; nothing sweeps it.
     */
    public boolean isExpired( Date now ) {
        return expiresAt == null || expiresAt.before( now );
    }

    /**
     * @return whether {@code username} currently holds this lock — false once
     *         it has expired, so a returning curator re-acquires rather than
     *         silently resuming a lapsed claim someone else may have taken.
     */
    public boolean isHeldBy( @Nullable String username, Date now ) {
        return username != null && username.equals( lockedBy ) && !isExpired( now );
    }
}
