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
package ubic.gemma.persistence.service.common.auditAndSecurity.curation;

import org.springframework.lang.Nullable;
import ubic.gemma.model.common.auditAndSecurity.curation.Curatable;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketTargetType;

import java.util.Date;

/**
 * Compatibility shim that exposes the legacy {@code CurationDetails} read API
 * ({@code needsAttention} / {@code troubled} / {@code lastUpdated}) on top of
 * the Phase B-1 {@link TicketService}. Per Decision 1 of
 * {@code AUDIT_AS_WORKFLOW_RECCE.md}, tickets REPLACE the {@code CurationDetails}
 * model; this interface is the migration on-ramp for the ~18 callers of
 * {@code curatable.getCurationDetails().getXxx()}.
 *
 * <h2>Mapping (read side)</h2>
 *
 * <ul>
 *   <li>{@link #needsAttention(TicketTargetType, Long) needsAttention} &harr;
 *       "an open ticket exists for this target with type
 *       {@code GENERIC}, {@code BATCH_INFO_NEEDED}, or {@code QUALITY_REVIEW}".</li>
 *   <li>{@link #troubled(TicketTargetType, Long) troubled} &harr;
 *       "an open ticket exists for this target with type
 *       {@code QUALITY_REVIEW}".</li>
 *   <li>{@link #lastUpdated(TicketTargetType, Long) lastUpdated} &harr;
 *       {@code max(TicketEvent.occurredAt)} across all open tickets for the
 *       target; {@code null} if no open tickets exist (caller should fall back
 *       to the entity's own audit trail).</li>
 *   <li>{@code curationNote} &mdash; deferred. Free-text notes do not map
 *       cleanly onto tickets; revisit when the write-side migration lands.</li>
 * </ul>
 *
 * <h2>Write side</h2>
 *
 * <p>The {@code setNeedsAttention(boolean)} / {@code setTroubled(boolean)}
 * counterparts are deliberately NOT exposed yet. Callers that want to flip
 * those flags should open or resolve tickets directly through
 * {@link TicketService#openTicket} and
 * {@link TicketService#transition}. A full write-side migration is queued for a
 * follow-up session (see {@code AUDIT_AS_WORKFLOW_RECCE.md} &sect; Decision 1).</p>
 *
 * @author paul
 * @deprecated this interface only exists as a compatibility shim while callers
 *             migrate off {@code Curatable#getCurationDetails()}. New code
 *             should query {@link TicketService} directly. The shim &mdash; and
 *             the underlying {@code CurationDetails} entity &mdash; will be
 *             retired once all callers move over.
 */
@Deprecated
public interface CurationDetailsService {

    /**
     * Whether the given target has at least one open ticket of a type that
     * historically tripped {@code curationDetails.needsAttention=true}
     * (i.e. {@code GENERIC}, {@code BATCH_INFO_NEEDED}, or
     * {@code QUALITY_REVIEW}). Tickets in terminal states
     * ({@code RESOLVED} / {@code CANCELLED}) are excluded.
     */
    boolean needsAttention( TicketTargetType targetType, Long targetId );

    /** Convenience overload &mdash; resolves target type from the entity. */
    boolean needsAttention( Curatable curatable );

    /**
     * Whether the given target has at least one open ticket of type
     * {@code QUALITY_REVIEW}. Mirrors the historical {@code troubled} flag at
     * read time only &mdash; this does NOT walk to parent array designs the
     * way {@code ExpressionExperimentService.isTroubled(ee)} does.
     */
    boolean troubled( TicketTargetType targetType, Long targetId );

    /** Convenience overload &mdash; resolves target type from the entity. */
    boolean troubled( Curatable curatable );

    /**
     * The most recent {@link ubic.gemma.model.common.auditAndSecurity.curation.TicketEvent#getOccurredAt() occurredAt}
     * across all open tickets for the target. Returns {@code null} when no
     * open tickets exist; callers that need a non-null timestamp should fall
     * back to the entity's audit trail (e.g.
     * {@code curatable.getAuditTrail().getLast().getDate()}).
     */
    @Nullable
    Date lastUpdated( TicketTargetType targetType, Long targetId );

    /** Convenience overload &mdash; resolves target type from the entity. */
    @Nullable
    Date lastUpdated( Curatable curatable );
}
