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
import java.util.Date;

/**
 * Value object projection of {@link TicketEvent} for the REST surface
 * (Phase B-2 of {@code AUDIT_AS_WORKFLOW_RECCE.md}).
 *
 * <p>The {@code actorId} / {@code actorName} pair is the minimum we surface
 * about the {@link ubic.gemma.model.common.auditAndSecurity.Contact} so we
 * don't accidentally leak the full Contact payload.</p>
 *
 * @author paul
 */
@Data
public class TicketEventValueObject implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private TicketEventType type;
    private Date occurredAt;

    @Nullable
    private Long actorId;

    @Nullable
    private String actorName;

    @Nullable
    private String payload;

    public TicketEventValueObject() {
    }

    public static TicketEventValueObject from( TicketEvent e ) {
        TicketEventValueObject vo = new TicketEventValueObject();
        vo.id = e.getId();
        vo.type = e.getType();
        vo.occurredAt = e.getOccurredAt();
        if ( e.getActor() != null ) {
            vo.actorId = e.getActor().getId();
            vo.actorName = e.getActor().getName();
        }
        vo.payload = e.getPayload();
        return vo;
    }
}
