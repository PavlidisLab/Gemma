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

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import ubic.gemma.core.security.audit.AuditedConditional;
import ubic.gemma.model.analysis.Investigation;
import ubic.gemma.model.common.auditAndSecurity.curation.AnnotationSet;
import ubic.gemma.model.common.auditAndSecurity.curation.AnnotationSetRole;
import ubic.gemma.model.common.auditAndSecurity.curation.AnnotationSetSource;
import ubic.gemma.model.common.auditAndSecurity.curation.AnnotationSetSummaryValueObject;
import ubic.gemma.model.common.auditAndSecurity.eventType.AnnotationSetEvent;
import ubic.gemma.model.expression.experiment.AgentCurationKind;

/**
 * Default {@link AnnotationSetService} implementation.
 *
 * <p>{@link #attach} carries {@code @AuditedConditional}: an event is
 * emitted only on actual insert (the {@code result.created} flag), not
 * on the idempotent same-{@code (role, runId)} retry path. The audit
 * note carries the row id so the audit trail links to the row without
 * inlining the JSON payload.</p>
 */
@Service
public class AnnotationSetServiceImpl implements AnnotationSetService {

    private static final String DRAFT_RUN_ID_PREFIX = "draft-";

    /** Cap matching {@code ANNOTATION_SET.FINALIZED_NOTES VARCHAR(2048)}. */
    private static final int MAX_FINALIZED_NOTES_LENGTH = 2048;

    private final AnnotationSetDao annotationSetDao;

    @Autowired
    public AnnotationSetServiceImpl( AnnotationSetDao annotationSetDao ) {
        this.annotationSetDao = annotationSetDao;
    }

    /**
     * When a newly-attached set earns an audit event.
     * <p>
     * Not for a SNAPSHOT. Every audit event on a curatable sets {@code curationDetails.lastUpdated} to the event
     * date — see {@code AbstractCuratableDao#updateCurationDetailsFromAuditEvent}, which does it unconditionally
     * for every event type. A snapshot captures existing state and changes nothing, so an event there would make
     * a dataset look edited by the act of backing it up.
     * <p>
     * That is not cosmetic: {@code lastUpdated} is the optimistic-concurrency token the curation commit checks,
     * so taking a backup would 409 every in-flight curator draft on that dataset. The AnnotationSet row is
     * already its own record, carrying {@code createdAt}, {@code createdBy} and {@code runId}, so nothing is
     * lost by staying quiet.
     * <p>
     * PROPOSAL and DRAFT keep their event: an agent attaching a hypothesis, or a curator opening a buffer, is
     * activity on the dataset worth a trail entry.
     * <p>
     * COMMIT is quiet for the same reason and one more. A COMMIT row is minted inside the curation commit's own
     * transaction, and that commit has already emitted its own events and already moved {@code lastUpdated} — an
     * {@code AnnotationSetEvent} on top would be one more event for a commit that emits too many already, saying
     * nothing the design/tag events did not. The row records who applied the curation; the events record what was
     * applied.
     */
    private static final String ATTACH_AUDIT_WHEN =
            "#result != null and #result.created"
                    + " and #result.annotationSet.role.name() != 'SNAPSHOT'"
                    + " and #result.annotationSet.role.name() != 'COMMIT'";

    /**
     * Audit note for a newly-attached set, shared by both {@code attach} overloads.
     * <p>
     * Extracted to a constant so the two cannot drift: the deprecated overload self-invokes the other, which
     * means the aspect fires on whichever method the caller entered through (a same-class call bypasses the
     * proxy), so BOTH need the annotation and both must say the same thing. Reads everything off
     * {@code #result} rather than the parameters, so one expression fits both signatures.
     */
    private static final String ATTACH_AUDIT_MESSAGE = "'AnnotationSet#' + #result.annotationSet.id"
            + " + ' role=' + #result.annotationSet.role.dbValue"
            + " + ' source=' + #result.annotationSet.source.dbValue"
            + " + (#result.annotationSet.kind != null ? ' kind=' + #result.annotationSet.kind.dbValue : '')"
            + " + ' run=' + #result.annotationSet.runId"
            + " + (#result.annotationSet.agentName != null ? ' agent=' + #result.annotationSet.agentName : '')"
            + " + (#result.annotationSet.agentVersion != null ? ' version=' + #result.annotationSet.agentVersion : '')"
            + " + (#result.annotationSet.model != null ? ' model=' + #result.annotationSet.model : '')"
            + " + (#result.annotationSet.runSha != null ? ' sha=' + #result.annotationSet.runSha : '')";

    @Override
    @Transactional
    @AuditedConditional(value = AnnotationSetEvent.class,
            when = ATTACH_AUDIT_WHEN,
            messageSpel = ATTACH_AUDIT_MESSAGE)
    public AttachedAnnotationSet attach( Investigation investigation,
            AnnotationSetRole role,
            AnnotationSetSource source,
            @Nullable AgentCurationKind kind,
            @Nullable String runId,
            @Nullable String createdBy,
            @Nullable String agentVersion,
            @Nullable String model,
            @Nullable Date ranAt,
            @Nullable String payloadJson,
            @Nullable AnnotationSet parent ) {
        return attach( investigation, role, source, kind, runId, createdBy,
                new RunProvenance( agentVersion, model, null, null, ranAt ), payloadJson, parent );
    }

    @Override
    @Transactional
    @AuditedConditional(value = AnnotationSetEvent.class,
            when = ATTACH_AUDIT_WHEN,
            messageSpel = ATTACH_AUDIT_MESSAGE)
    public AttachedAnnotationSet attach( Investigation investigation,
            AnnotationSetRole role,
            AnnotationSetSource source,
            @Nullable AgentCurationKind kind,
            @Nullable String runId,
            @Nullable String createdBy,
            @Nullable RunProvenance runProvenance,
            @Nullable String payloadJson,
            @Nullable AnnotationSet parent ) {
        Assert.notNull( investigation, "Investigation must not be null." );
        Assert.notNull( role, "role must not be null." );
        Assert.notNull( source, "source must not be null." );
        String agentVersion = runProvenance != null ? runProvenance.getAgentVersion() : null;
        String model = runProvenance != null ? runProvenance.getModel() : null;
        Date ranAt = runProvenance != null ? runProvenance.getRanAt() : null;
        String effectiveRunId = resolveRunId( role, runId, createdBy );
        AnnotationSet existing = annotationSetDao.findByInvestigationAndRoleAndRunId(
                investigation, role, effectiveRunId );
        if ( existing != null ) {
            return new AttachedAnnotationSet( existing, false );
        }
        Date now = new Date();
        AnnotationSet a = new AnnotationSet();
        a.setInvestigation( investigation );
        a.setRole( role );
        a.setSource( source );
        a.setKind( kind );
        a.setRunId( effectiveRunId );
        a.setCreatedBy( createdBy );
        a.setCreatedAt( now );
        a.setUpdatedAt( now );
        a.setAgentVersion( agentVersion );
        a.setModel( model );
        if ( runProvenance != null ) {
            a.setRunSha( runProvenance.getRunSha() );
            a.setAgentName( runProvenance.getAgentName() );
        }
        a.setRanAt( ranAt != null ? ranAt : ( source == AnnotationSetSource.AGENT ? now : null ) );
        a.setPayloadJson( payloadJson );
        a.setParent( parent );
        AnnotationSet saved = annotationSetDao.create( a );
        return new AttachedAnnotationSet( saved, true );
    }

    @Override
    @Transactional
    public AnnotationSet upsertDraft( Investigation investigation,
            String createdBy,
            String payloadJson,
            @Nullable String parkedElements,
            @Nullable AnnotationSet parent ) {
        Assert.notNull( investigation, "Investigation must not be null." );
        Assert.hasText( createdBy, "createdBy must be non-blank for a DRAFT." );
        Assert.notNull( payloadJson, "payloadJson must not be null for a DRAFT." );
        String runId = DRAFT_RUN_ID_PREFIX + createdBy;
        AnnotationSet existing = annotationSetDao.findByInvestigationAndRoleAndRunId(
                investigation, AnnotationSetRole.DRAFT, runId );
        Date now = new Date();
        if ( existing != null ) {
            existing.setPayloadJson( payloadJson );
            existing.setParkedElements( parkedElements );
            existing.setUpdatedAt( now );
            if ( parent != null && existing.getParent() == null ) {
                existing.setParent( parent );
            }
            annotationSetDao.update( existing );
            return existing;
        }
        AnnotationSet a = new AnnotationSet();
        a.setInvestigation( investigation );
        a.setRole( AnnotationSetRole.DRAFT );
        a.setSource( AnnotationSetSource.CURATOR );
        a.setRunId( runId );
        a.setCreatedBy( createdBy );
        a.setCreatedAt( now );
        a.setUpdatedAt( now );
        a.setPayloadJson( payloadJson );
        a.setParkedElements( parkedElements );
        a.setParent( parent );
        return annotationSetDao.create( a );
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnnotationSet> findByInvestigation( Investigation investigation,
            @Nullable AnnotationSetRole roleFilter ) {
        Assert.notNull( investigation, "Investigation must not be null." );
        return annotationSetDao.findByInvestigation( investigation, roleFilter );
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnnotationSetSummaryValueObject> findSummariesByInvestigation( Investigation investigation,
            @Nullable AnnotationSetRole roleFilter ) {
        Assert.notNull( investigation, "Investigation must not be null." );
        return annotationSetDao.findSummariesByInvestigation( investigation, roleFilter );
    }

    @Nullable
    @Override
    @Transactional(readOnly = true)
    public AnnotationSet findLatestByInvestigation( Investigation investigation,
            @Nullable AnnotationSetRole roleFilter ) {
        Assert.notNull( investigation, "Investigation must not be null." );
        return annotationSetDao.findLatestByInvestigation( investigation, roleFilter );
    }

    @Nullable
    @Override
    @Transactional(readOnly = true)
    public AnnotationSet load( Long id ) {
        if ( id == null ) return null;
        return annotationSetDao.load( id );
    }

    @Nullable
    @Override
    @Transactional(readOnly = true)
    public AnnotationSet findByInvestigationAndRoleAndRunId( Investigation investigation,
            AnnotationSetRole role, String runId ) {
        Assert.notNull( investigation, "Investigation must not be null." );
        Assert.notNull( role, "role must not be null." );
        Assert.hasText( runId, "runId must be non-blank." );
        return annotationSetDao.findByInvestigationAndRoleAndRunId( investigation, role, runId );
    }

    @Override
    @Transactional(readOnly = true)
    public long countByInvestigation( Investigation investigation,
            @Nullable AnnotationSetRole roleFilter ) {
        Assert.notNull( investigation, "Investigation must not be null." );
        return annotationSetDao.countByInvestigation( investigation, roleFilter );
    }

    @Override
    @Transactional
    public int rebindInvestigation( Investigation from, Investigation to ) {
        Assert.notNull( from, "from must not be null." );
        Assert.notNull( to, "to must not be null." );
        return annotationSetDao.rebindInvestigation( from, to );
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnnotationSetSummaryValueObject> listSummaries( @Nullable AnnotationSetRole roleFilter,
            @Nullable AnnotationSetSource sourceFilter,
            @Nullable String createdByFilter,
            @Nullable List<Long> investigationIds, int offset, int limit,
            @Nullable AnnotationSetDao.SummarySort sort, boolean descending ) {
        return annotationSetDao.listSummaries( roleFilter, sourceFilter, createdByFilter,
                investigationIds, offset, limit, sort, descending );
    }

    @Override
    @Transactional(readOnly = true)
    public long countSummaries( @Nullable AnnotationSetRole roleFilter,
            @Nullable AnnotationSetSource sourceFilter,
            @Nullable String createdByFilter,
            @Nullable List<Long> investigationIds ) {
        return annotationSetDao.countSummaries( roleFilter, sourceFilter, createdByFilter,
                investigationIds );
    }

    @Nullable
    @Override
    @Transactional
    public AnnotationSet finalizeSet( Long id, @Nullable String finalizedBy, @Nullable String notes ) {
        Assert.notNull( id, "id must not be null." );
        AnnotationSet a = annotationSetDao.load( id );
        if ( a == null ) return null;
        String trimmedNotes = truncateNotes( notes );
        if ( a.getFinalizedAt() != null ) {
            // Already closed. Re-stamping the timestamp would rewrite when the decision was
            // made, so it stays; but a note that arrives now is recorded rather than dropped,
            // since a caller cannot tell a silently discarded sentence from a stored one.
            if ( trimmedNotes != null && !trimmedNotes.equals( a.getFinalizedNotes() ) ) {
                a.setFinalizedNotes( trimmedNotes );
                a.setUpdatedAt( new Date() );
                annotationSetDao.update( a );
            }
            return a;
        }
        Date now = new Date();
        a.setFinalizedAt( now );
        a.setFinalizedBy( finalizedBy );
        a.setFinalizedNotes( trimmedNotes );
        a.setUpdatedAt( now );
        annotationSetDao.update( a );
        return a;
    }

    @Nullable
    @Override
    @Transactional
    public AnnotationSet reopenSet( Long id ) {
        Assert.notNull( id, "id must not be null." );
        AnnotationSet a = annotationSetDao.load( id );
        if ( a == null ) return null;
        if ( a.getFinalizedAt() == null ) {
            return a;
        }
        Date now = new Date();
        a.setFinalizedAt( null );
        a.setFinalizedBy( null );
        // The note explains one closure; keeping it would attach those words to the next one.
        a.setFinalizedNotes( null );
        a.setUpdatedAt( now );
        annotationSetDao.update( a );
        return a;
    }

    @Nullable
    @Override
    @Transactional
    public AnnotationSet updateProvenance( Long id, RunProvenance provenance ) {
        Assert.notNull( id, "id must not be null." );
        Assert.notNull( provenance, "provenance must not be null." );
        AnnotationSet a = annotationSetDao.load( id );
        if ( a == null ) return null;
        if ( provenance.getAgentVersion() != null ) {
            a.setAgentVersion( blankToNull( provenance.getAgentVersion() ) );
        }
        if ( provenance.getModel() != null ) {
            a.setModel( blankToNull( provenance.getModel() ) );
        }
        if ( provenance.getRunSha() != null ) {
            a.setRunSha( blankToNull( provenance.getRunSha() ) );
        }
        if ( provenance.getAgentName() != null ) {
            a.setAgentName( blankToNull( provenance.getAgentName() ) );
        }
        if ( provenance.getRanAt() != null ) {
            a.setRanAt( provenance.getRanAt() );
        }
        a.setUpdatedAt( new Date() );
        annotationSetDao.update( a );
        return a;
    }

    @Nullable
    private static String blankToNull( String s ) {
        return s.trim().isEmpty() ? null : s;
    }

    /**
     * Trim, blank-to-null, and cut to {@code FINALIZED_NOTES VARCHAR(2048)}.
     * <p>
     * Truncated rather than rejected, matching how a triage note is handled:
     * losing the tail of an explanation is a smaller harm than refusing a
     * closure the curator has already decided on, and the closure is the part
     * that matters.
     */
    @Nullable
    private static String truncateNotes( @Nullable String notes ) {
        if ( notes == null ) {
            return null;
        }
        String trimmed = notes.trim();
        if ( trimmed.isEmpty() ) {
            return null;
        }
        return trimmed.length() <= MAX_FINALIZED_NOTES_LENGTH
                ? trimmed : trimmed.substring( 0, MAX_FINALIZED_NOTES_LENGTH );
    }

    @Override
    @Transactional
    public boolean delete( Long id ) {
        if ( id == null ) return false;
        AnnotationSet a = annotationSetDao.load( id );
        if ( a == null ) return false;
        annotationSetDao.remove( a );
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public long countSince( @Nullable Date since, @Nullable AnnotationSetRole roleFilter ) {
        return annotationSetDao.countSince( since, roleFilter );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<AnnotationSetRole, Long> countByRoleSince( @Nullable Date since ) {
        return annotationSetDao.countByRoleSince( since );
    }

    @Override
    @Transactional(readOnly = true)
    public long countDistinctRunIdsSince( @Nullable Date since,
            @Nullable AnnotationSetRole roleFilter ) {
        return annotationSetDao.countDistinctRunIdsSince( since, roleFilter );
    }

    @Nullable
    @Override
    @Transactional(readOnly = true)
    public Date findLatestCreatedAt( @Nullable AnnotationSetRole roleFilter ) {
        return annotationSetDao.findLatestCreatedAt( roleFilter );
    }

    private String resolveRunId( AnnotationSetRole role, @Nullable String runId,
            @Nullable String createdBy ) {
        if ( runId != null && !runId.isBlank() ) {
            return runId;
        }
        switch ( role ) {
            case DRAFT:
                Assert.hasText( createdBy,
                        "createdBy must be non-blank when role=DRAFT and runId is null." );
                return DRAFT_RUN_ID_PREFIX + createdBy;
            case SNAPSHOT:
                return UUID.randomUUID().toString();
            case PROPOSAL:
            case COMMIT:
            default:
                // PROPOSAL and COMMIT both name a run that really happened on the producing side.
                // Synthesizing an id here would invent provenance rather than record it, and a
                // synthesized id is indistinguishable from a real one once it is in the column.
                // Any role added later lands here too, which is the safe direction to fail.
                throw new IllegalArgumentException(
                        "runId must be supplied for role=" + role );
        }
    }
}
