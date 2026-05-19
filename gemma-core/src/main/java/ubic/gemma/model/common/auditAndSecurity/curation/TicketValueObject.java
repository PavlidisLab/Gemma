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
package ubic.gemma.model.common.auditAndSecurity.curation;

import lombok.Data;
import org.springframework.lang.Nullable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * Value object projection of {@link Ticket} for the REST surface (Phase B-2
 * of {@code AUDIT_AS_WORKFLOW_RECCE.md}). Embeds the ticket's
 * {@link TicketTargetValueObject targets} and (optionally) its
 * {@link TicketEventValueObject event log}.
 *
 * <p>Events are populated by the "single ticket" endpoint; the list endpoint
 * leaves the events collection empty for payload economy.</p>
 *
 * @author paul
 */
@Data
public class TicketValueObject implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String title;
    private TicketType type;
    private TicketState state;
    private TicketPriority priority;

    @Nullable
    private Date dueDate;

    @Nullable
    private Long reporterId;

    @Nullable
    private String reporterName;

    @Nullable
    private Long assigneeId;

    @Nullable
    private String assigneeName;

    private Date createdAt;
    private Date updatedAt;

    @Nullable
    private String externalIssueUrl;
    private ExternalIssueSyncState externalIssueSyncState;

    private List<TicketTargetValueObject> targets = new ArrayList<>();
    private List<TicketEventValueObject> events = new ArrayList<>();

    public TicketValueObject() {
    }

    /**
     * Project the ticket WITHOUT its event log (cheap, for list views).
     * Targets are always included.
     */
    public static TicketValueObject from( Ticket t ) {
        return from( t, false );
    }

    /**
     * Project the ticket; include events iff {@code includeEvents}.
     * Events are sorted by {@code occurredAt} ascending.
     */
    public static TicketValueObject from( Ticket t, boolean includeEvents ) {
        TicketValueObject vo = new TicketValueObject();
        vo.id = t.getId();
        vo.title = t.getTitle();
        vo.type = t.getType();
        vo.state = t.getState();
        vo.priority = t.getPriority();
        vo.dueDate = t.getDueDate();
        if ( t.getReporter() != null ) {
            vo.reporterId = t.getReporter().getId();
            vo.reporterName = t.getReporter().getName();
        }
        if ( t.getAssignee() != null ) {
            vo.assigneeId = t.getAssignee().getId();
            vo.assigneeName = t.getAssignee().getName();
        }
        vo.createdAt = t.getCreatedAt();
        vo.updatedAt = t.getUpdatedAt();
        vo.externalIssueUrl = t.getExternalIssueUrl();
        vo.externalIssueSyncState = t.getExternalIssueSyncState();
        if ( t.getTargets() != null ) {
            for ( TicketTarget tt : t.getTargets() ) {
                vo.targets.add( TicketTargetValueObject.from( tt ) );
            }
        }
        if ( includeEvents && t.getEvents() != null ) {
            List<TicketEventValueObject> evs = new ArrayList<>( t.getEvents().size() );
            for ( TicketEvent e : t.getEvents() ) {
                evs.add( TicketEventValueObject.from( e ) );
            }
            evs.sort( Comparator.comparing( TicketEventValueObject::getOccurredAt,
                    Comparator.nullsLast( Comparator.naturalOrder() ) ) );
            vo.events = evs;
        }
        return vo;
    }
}
