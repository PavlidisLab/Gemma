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
import java.util.Date;

/**
 * One row of {@code GET /tickets/search} &mdash; enough of a ticket to pick it out of a list with
 * confidence, and nothing more.
 *
 * <p>Deliberately not {@link TicketValueObject}: that carries the ticket's {@code targets} and
 * {@code events} collections, so rendering twenty picker rows for a ticket holding five hundred
 * targets would ship several hundred target rows the picker never draws. This VO carries
 * {@link #getTargetCount() targetCount} instead, counted by the database (see
 * {@code TicketDaoImpl.buildSearchHitHql}), and the caller fetches
 * {@code GET /tickets/{id}} when it actually wants the targets.</p>
 *
 * @author paul
 */
@Data
public class TicketSearchHitValueObject implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String title;
    private TicketState state;
    private TicketType type;

    /**
     * How many {@link TicketTarget}s the ticket holds. A count, never the targets themselves.
     */
    private long targetCount;

    private Date updatedAt;

    public TicketSearchHitValueObject() {
    }

    public TicketSearchHitValueObject( Long id, String title, TicketState state, TicketType type,
            long targetCount, Date updatedAt ) {
        this.id = id;
        this.title = title;
        this.state = state;
        this.type = type;
        this.targetCount = targetCount;
        this.updatedAt = updatedAt;
    }

    /**
     * Project one row of the {@code /tickets/search} HQL projection.
     * <p>
     * The columns are positional, so this and the query's select list have to agree. The order is
     * {@code id, title (the NAME column), state, type, targetCount, updatedAt}; the query builder
     * that emits it is {@code TicketDaoImpl.buildSearchHitHql}, and its select list is asserted
     * against this order in {@code TicketSearchQueryTest}.
     */
    public static TicketSearchHitValueObject fromRow( Object[] row ) {
        return new TicketSearchHitValueObject(
                ( Long ) row[0],
                ( String ) row[1],
                ( TicketState ) row[2],
                ( TicketType ) row[3],
                row[4] == null ? 0L : ( ( Number ) row[4] ).longValue(),
                ( Date ) row[5] );
    }
}
