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

import java.io.Serializable;

/**
 * Value object projection of {@link TicketTarget} for the REST surface
 * (Phase B-2 of {@code AUDIT_AS_WORKFLOW_RECCE.md}).
 *
 * @author paul
 */
@Data
public class TicketTargetValueObject implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private TicketTargetType targetType;
    private Long targetId;

    public TicketTargetValueObject() {
    }

    public static TicketTargetValueObject from( TicketTarget t ) {
        TicketTargetValueObject vo = new TicketTargetValueObject();
        vo.id = t.getId();
        vo.targetType = t.getTargetType();
        vo.targetId = t.getTargetId();
        return vo;
    }
}
