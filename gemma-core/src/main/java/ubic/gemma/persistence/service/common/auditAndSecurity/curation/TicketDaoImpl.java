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

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;
import ubic.gemma.model.common.auditAndSecurity.Contact;
import ubic.gemma.model.common.auditAndSecurity.curation.Ticket;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketEvent;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketPriority;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketState;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketTargetType;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketType;
import ubic.gemma.persistence.service.AbstractDao;
import ubic.gemma.persistence.util.Cursor;
import ubic.gemma.persistence.util.CursorPage;
import ubic.gemma.persistence.util.Sort;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Hibernate implementation of {@link TicketDao}. Mirrors the lightweight CRUD
 * + a-couple-of-finder shape of {@code BlacklistedEntityDaoImpl}.
 *
 * @author paul
 */
@Repository
public class TicketDaoImpl extends AbstractDao<Ticket> implements TicketDao {

    @Autowired
    public TicketDaoImpl( SessionFactory sessionFactory ) {
        super( Ticket.class, sessionFactory );
    }

    @Override
    public List<Ticket> findOpenForTarget( TicketTargetType targetType, Long targetId ) {
        //noinspection unchecked
        return ( List<Ticket> ) this.getSessionFactory().getCurrentSession().createQuery(
                        "select distinct t from Ticket t "
                                + "join t.targets tt "
                                + "where tt.targetType = :tt "
                                + "and tt.targetId = :tid "
                                + "and t.state in :openStates" )
                .setParameter( "tt", targetType )
                .setParameter( "tid", targetId )
                .setParameterList( "openStates", Arrays.asList( TicketState.OPEN, TicketState.IN_PROGRESS ) )
                .list();
    }

    @Override
    public List<Ticket> findAssignedTo( Contact assignee ) {
        //noinspection unchecked
        return ( List<Ticket> ) this.getSessionFactory().getCurrentSession().createQuery(
                        "select t from Ticket t where t.assignee = :a order by t.updatedAt desc" )
                .setParameter( "a", assignee )
                .list();
    }

    @Override
    public List<Ticket> findTickets( boolean openOnly, @Nullable Long assigneeId, @Nullable TicketPriority priority, int offset, int limit ) {
        return findTickets( openOnly, assigneeId, priority, null, null, null, null, offset, limit );
    }

    @Override
    public List<Ticket> findTickets( boolean openOnly, @Nullable Long assigneeId, @Nullable TicketPriority priority,
            @Nullable TicketType type, @Nullable TicketState state, @Nullable TicketTargetType targetType,
            @Nullable Date updatedSince, int offset, int limit ) {
        StringBuilder hql = new StringBuilder( "select " );
        if ( targetType != null ) {
            hql.append( "distinct " );
        }
        hql.append( "t from Ticket t" );
        if ( targetType != null ) {
            hql.append( " join t.targets tt" );
        }
        hql.append( " where 1=1" );
        appendTicketFilters( hql, openOnly, assigneeId, priority, type, state, targetType, updatedSince );
        hql.append( " order by t.updatedAt desc" );
        org.hibernate.query.Query<?> q = this.getSessionFactory().getCurrentSession().createQuery( hql.toString() );
        bindTicketFilters( q, openOnly, assigneeId, priority, type, state, targetType, updatedSince );
        if ( offset > 0 ) {
            q.setFirstResult( offset );
        }
        if ( limit > 0 ) {
            q.setMaxResults( limit );
        }
        //noinspection unchecked
        return ( List<Ticket> ) q.list();
    }

    /**
     * Build the WHERE-clause fragments shared by the offset and cursor variants of
     * {@code findTickets} / {@code countTickets}. When {@code state} is non-null it
     * overrides {@code openOnly} (a passed {@code state} pins the predicate to a
     * single value rather than the OPEN/IN_PROGRESS pair).
     */
    private static void appendTicketFilters( StringBuilder hql, boolean openOnly,
            @Nullable Long assigneeId, @Nullable TicketPriority priority,
            @Nullable TicketType type, @Nullable TicketState state,
            @Nullable TicketTargetType targetType, @Nullable Date updatedSince ) {
        if ( state != null ) {
            hql.append( " and t.state = :state" );
        } else if ( openOnly ) {
            hql.append( " and t.state in :openStates" );
        }
        if ( assigneeId != null ) {
            hql.append( " and t.assignee.id = :assigneeId" );
        }
        if ( priority != null ) {
            hql.append( " and t.priority = :priority" );
        }
        if ( type != null ) {
            hql.append( " and t.type = :type" );
        }
        if ( targetType != null ) {
            hql.append( " and tt.targetType = :targetType" );
        }
        if ( updatedSince != null ) {
            hql.append( " and t.updatedAt >= :updatedSince" );
        }
    }

    /** Bind parameters matching {@link #appendTicketFilters}. */
    private static void bindTicketFilters( org.hibernate.query.Query<?> q, boolean openOnly,
            @Nullable Long assigneeId, @Nullable TicketPriority priority,
            @Nullable TicketType type, @Nullable TicketState state,
            @Nullable TicketTargetType targetType, @Nullable Date updatedSince ) {
        if ( state != null ) {
            q.setParameter( "state", state );
        } else if ( openOnly ) {
            q.setParameterList( "openStates", Arrays.asList( TicketState.OPEN, TicketState.IN_PROGRESS ) );
        }
        if ( assigneeId != null ) {
            q.setParameter( "assigneeId", assigneeId );
        }
        if ( priority != null ) {
            q.setParameter( "priority", priority );
        }
        if ( type != null ) {
            q.setParameter( "type", type );
        }
        if ( targetType != null ) {
            q.setParameter( "targetType", targetType );
        }
        if ( updatedSince != null ) {
            q.setParameter( "updatedSince", updatedSince );
        }
    }

    @Override
    public CursorPage<Ticket> findTicketsByCursor( boolean openOnly, @Nullable Long assigneeId,
            @Nullable TicketPriority priority, @Nullable Cursor cursor, int limit ) {
        return findTicketsByCursor( openOnly, assigneeId, priority, null, null, null, null, cursor, limit );
    }

    @Override
    public CursorPage<Ticket> findTicketsByCursor( boolean openOnly, @Nullable Long assigneeId,
            @Nullable TicketPriority priority, @Nullable TicketType type, @Nullable TicketState state,
            @Nullable TicketTargetType targetType, @Nullable Date updatedSince,
            @Nullable Cursor cursor, int limit ) {
        // Step 1o: keyset pagination over /tickets. Mirrors the offset-mode shape of
        // findTickets but with single-component +id sort enforced (the offset variant
        // sorts by t.updatedAt desc for human-readable dashboards; cursor mode trades
        // that for a stable, indexed keyset walk pending the phase-B index audit).
        // Fetches limit+1 to detect hasMore; no COUNT(*) per request.
        if ( limit <= 0 ) {
            throw new IllegalArgumentException( "Cursor page limit must be > 0." );
        }
        final String expectedSortSpec = "+id";
        if ( cursor != null ) {
            if ( !expectedSortSpec.equals( cursor.getSortSpec() ) ) {
                throw new IllegalArgumentException( "Cursor sort spec '" + cursor.getSortSpec()
                        + "' does not match the requested sort '" + expectedSortSpec + "'." );
            }
            Object[] key = cursor.getKeyTuple();
            if ( key.length != 1 ) {
                throw new IllegalArgumentException( "Cursor key tuple must have exactly 1 component for sort '"
                        + expectedSortSpec + "'; got " + key.length + "." );
            }
        }
        boolean backward = cursor != null && cursor.getDirection() == Cursor.Direction.BACKWARD;
        Long lastSeenId = null;
        if ( cursor != null ) {
            try {
                lastSeenId = ( ( Number ) cursor.getKeyTuple()[0] ).longValue();
            } catch ( ClassCastException e ) {
                throw new IllegalArgumentException( "Cursor key component must be numeric for sort '"
                        + expectedSortSpec + "'.", e );
            }
        }

        StringBuilder hql = new StringBuilder( "select " );
        if ( targetType != null ) {
            hql.append( "distinct " );
        }
        hql.append( "t from Ticket t" );
        if ( targetType != null ) {
            hql.append( " join t.targets tt" );
        }
        hql.append( " where 1=1" );
        appendTicketFilters( hql, openOnly, assigneeId, priority, type, state, targetType, updatedSince );
        if ( lastSeenId != null ) {
            // forward: id > x; backward: id < x (id-asc client-visible order). When backward,
            // we reverse the order in the driver query and reverse the returned page below.
            hql.append( backward ? " and t.id < :lastSeenId" : " and t.id > :lastSeenId" );
        }
        // backward cursor: order by id DESC in the driver query, reverse the returned page.
        hql.append( backward ? " order by t.id desc" : " order by t.id asc" );

        org.hibernate.query.Query<?> q = this.getSessionFactory().getCurrentSession().createQuery( hql.toString() );
        bindTicketFilters( q, openOnly, assigneeId, priority, type, state, targetType, updatedSince );
        if ( lastSeenId != null ) {
            q.setParameter( "lastSeenId", lastSeenId );
        }
        q.setMaxResults( limit + 1 );
        //noinspection unchecked
        List<Ticket> data = ( List<Ticket> ) q.list();

        boolean hasMore = data.size() > limit;
        if ( hasMore ) {
            data = new ArrayList<>( data.subList( 0, limit ) );
        } else {
            data = new ArrayList<>( data );
        }
        if ( backward ) {
            Collections.reverse( data );
        }

        String nextCursor = null;
        String prevCursor = null;
        if ( !data.isEmpty() ) {
            Ticket last = data.get( data.size() - 1 );
            Ticket first = data.get( 0 );
            // emit nextCursor only when there's another page in the forward direction
            if ( backward || hasMore ) {
                nextCursor = new Cursor( expectedSortSpec, new Object[] { last.getId() }, Cursor.Direction.FORWARD ).encode();
            }
            // emit prevCursor whenever we have a cursor (at least one page is behind us)
            if ( cursor != null ) {
                prevCursor = new Cursor( expectedSortSpec, new Object[] { first.getId() }, Cursor.Direction.BACKWARD ).encode();
            }
        }

        Sort idSort = Sort.by( null, "id", Sort.Direction.ASC, Sort.NullMode.LAST, "id" );
        return new CursorPage<>( data, idSort, limit, nextCursor, prevCursor, null );
    }

    @Override
    public CursorPage<Ticket> findOpenForTargetByCursor( TicketTargetType targetType, Long targetId,
            @Nullable Cursor cursor, int limit ) {
        // Step 1p: keyset pagination over /datasets/{dataset}/tickets and
        // /platforms/{platform}/tickets. Mirrors findOpenForTarget's scope
        // (non-terminal tickets matching the (targetType, targetId) pair via
        // the t.targets collection) but adds id-asc ordering and an
        // id > lastSeenId / id < lastSeenId predicate when a cursor is supplied.
        // Single-component +id sort enforced (cursor DAO restricts cursors to
        // id-only sorts until the phase-B index audit lands). Fetches limit+1
        // to detect hasMore without a separate COUNT(*).
        if ( limit <= 0 ) {
            throw new IllegalArgumentException( "Cursor page limit must be > 0." );
        }
        final String expectedSortSpec = "+id";
        if ( cursor != null ) {
            if ( !expectedSortSpec.equals( cursor.getSortSpec() ) ) {
                throw new IllegalArgumentException( "Cursor sort spec '" + cursor.getSortSpec()
                        + "' does not match the requested sort '" + expectedSortSpec + "'." );
            }
            Object[] key = cursor.getKeyTuple();
            if ( key.length != 1 ) {
                throw new IllegalArgumentException( "Cursor key tuple must have exactly 1 component for sort '"
                        + expectedSortSpec + "'; got " + key.length + "." );
            }
        }
        boolean backward = cursor != null && cursor.getDirection() == Cursor.Direction.BACKWARD;
        Long lastSeenId = null;
        if ( cursor != null ) {
            try {
                lastSeenId = ( ( Number ) cursor.getKeyTuple()[0] ).longValue();
            } catch ( ClassCastException e ) {
                throw new IllegalArgumentException( "Cursor key component must be numeric for sort '"
                        + expectedSortSpec + "'.", e );
            }
        }

        StringBuilder hql = new StringBuilder( "select distinct t from Ticket t "
                + "join t.targets tt "
                + "where tt.targetType = :tt "
                + "and tt.targetId = :tid "
                + "and t.state in :openStates" );
        if ( lastSeenId != null ) {
            // forward: id > x; backward: id < x (id-asc client-visible order). When backward,
            // we reverse the order in the driver query and reverse the returned page below.
            hql.append( backward ? " and t.id < :lastSeenId" : " and t.id > :lastSeenId" );
        }
        // backward cursor: order by id DESC in the driver query, reverse the returned page.
        hql.append( backward ? " order by t.id desc" : " order by t.id asc" );

        org.hibernate.query.Query<?> q = this.getSessionFactory().getCurrentSession().createQuery( hql.toString() )
                .setParameter( "tt", targetType )
                .setParameter( "tid", targetId )
                .setParameterList( "openStates", Arrays.asList( TicketState.OPEN, TicketState.IN_PROGRESS ) );
        if ( lastSeenId != null ) {
            q.setParameter( "lastSeenId", lastSeenId );
        }
        q.setMaxResults( limit + 1 );
        //noinspection unchecked
        List<Ticket> data = ( List<Ticket> ) q.list();

        boolean hasMore = data.size() > limit;
        if ( hasMore ) {
            data = new ArrayList<>( data.subList( 0, limit ) );
        } else {
            data = new ArrayList<>( data );
        }
        if ( backward ) {
            Collections.reverse( data );
        }

        String nextCursor = null;
        String prevCursor = null;
        if ( !data.isEmpty() ) {
            Ticket last = data.get( data.size() - 1 );
            Ticket first = data.get( 0 );
            // emit nextCursor only when there's another page in the forward direction
            if ( backward || hasMore ) {
                nextCursor = new Cursor( expectedSortSpec, new Object[] { last.getId() }, Cursor.Direction.FORWARD ).encode();
            }
            // emit prevCursor whenever we have a cursor (at least one page is behind us)
            if ( cursor != null ) {
                prevCursor = new Cursor( expectedSortSpec, new Object[] { first.getId() }, Cursor.Direction.BACKWARD ).encode();
            }
        }

        Sort idSort = Sort.by( null, "id", Sort.Direction.ASC, Sort.NullMode.LAST, "id" );
        return new CursorPage<>( data, idSort, limit, nextCursor, prevCursor, null );
    }

    @Override
    public long countTickets( boolean openOnly, @Nullable Long assigneeId, @Nullable TicketPriority priority ) {
        return countTickets( openOnly, assigneeId, priority, null, null, null, null );
    }

    @Override
    public long countTickets( boolean openOnly, @Nullable Long assigneeId, @Nullable TicketPriority priority,
            @Nullable TicketType type, @Nullable TicketState state, @Nullable TicketTargetType targetType,
            @Nullable Date updatedSince ) {
        StringBuilder hql = new StringBuilder( "select count(" );
        // when joining the target collection we count DISTINCT tickets to avoid fan-out
        hql.append( targetType != null ? "distinct t" : "t" );
        hql.append( ") from Ticket t" );
        if ( targetType != null ) {
            hql.append( " join t.targets tt" );
        }
        hql.append( " where 1=1" );
        appendTicketFilters( hql, openOnly, assigneeId, priority, type, state, targetType, updatedSince );
        org.hibernate.query.Query<?> q = this.getSessionFactory().getCurrentSession().createQuery( hql.toString() );
        bindTicketFilters( q, openOnly, assigneeId, priority, type, state, targetType, updatedSince );
        return ( Long ) q.uniqueResult();
    }

    @Override
    public CursorPage<TicketEvent> findEventsByCursor( Ticket ticket, @Nullable Cursor cursor, int limit ) {
        // Step 1r: keyset pagination over /tickets/{id}/events. Mirrors the scope of
        // iterating Ticket.events (every TicketEvent whose ticket FK matches the
        // supplied ticket) but switches the order from the legacy occurredAt sort
        // (HBM order-by="OCCURRED_AT") to a single-component +id sort so the cursor
        // DAO restriction (id-only keyset, pending the phase-B index audit) is
        // honoured. TicketEvents are append-only with monotonically increasing
        // occurredAt timestamps, so id-asc tracks occurredAt-asc in practice.
        org.springframework.util.Assert.notNull( ticket, "Ticket cannot be null." );
        org.springframework.util.Assert.notNull( ticket.getId(), "Ticket must have a persistent id." );
        if ( limit <= 0 ) {
            throw new IllegalArgumentException( "Cursor page limit must be > 0." );
        }
        final String expectedSortSpec = "+id";
        if ( cursor != null ) {
            if ( !expectedSortSpec.equals( cursor.getSortSpec() ) ) {
                throw new IllegalArgumentException( "Cursor sort spec '" + cursor.getSortSpec()
                        + "' does not match the requested sort '" + expectedSortSpec + "'." );
            }
            Object[] key = cursor.getKeyTuple();
            if ( key.length != 1 ) {
                throw new IllegalArgumentException( "Cursor key tuple must have exactly 1 component for sort '"
                        + expectedSortSpec + "'; got " + key.length + "." );
            }
        }
        boolean backward = cursor != null && cursor.getDirection() == Cursor.Direction.BACKWARD;
        Long lastSeenId = null;
        if ( cursor != null ) {
            try {
                lastSeenId = ( ( Number ) cursor.getKeyTuple()[0] ).longValue();
            } catch ( ClassCastException e ) {
                throw new IllegalArgumentException( "Cursor key component must be numeric for sort '"
                        + expectedSortSpec + "'.", e );
            }
        }

        StringBuilder hql = new StringBuilder( "select e from TicketEvent e where e.ticket = :t" );
        if ( lastSeenId != null ) {
            // forward: id > x; backward: id < x (id-asc client-visible order). When backward,
            // we reverse the order in the driver query and reverse the returned page below.
            hql.append( backward ? " and e.id < :lastSeenId" : " and e.id > :lastSeenId" );
        }
        // backward cursor: order by id DESC in the driver query, reverse the returned page.
        hql.append( backward ? " order by e.id desc" : " order by e.id asc" );

        org.hibernate.query.Query<?> q = this.getSessionFactory().getCurrentSession().createQuery( hql.toString() )
                .setParameter( "t", ticket );
        if ( lastSeenId != null ) {
            q.setParameter( "lastSeenId", lastSeenId );
        }
        q.setMaxResults( limit + 1 );
        //noinspection unchecked
        List<TicketEvent> data = ( List<TicketEvent> ) q.list();

        boolean hasMore = data.size() > limit;
        if ( hasMore ) {
            data = new ArrayList<>( data.subList( 0, limit ) );
        } else {
            data = new ArrayList<>( data );
        }
        if ( backward ) {
            Collections.reverse( data );
        }

        String nextCursor = null;
        String prevCursor = null;
        if ( !data.isEmpty() ) {
            TicketEvent last = data.get( data.size() - 1 );
            TicketEvent first = data.get( 0 );
            // emit nextCursor only when there's another page in the forward direction
            if ( backward || hasMore ) {
                nextCursor = new Cursor( expectedSortSpec, new Object[] { last.getId() }, Cursor.Direction.FORWARD ).encode();
            }
            // emit prevCursor whenever we have a cursor (at least one page is behind us)
            if ( cursor != null ) {
                prevCursor = new Cursor( expectedSortSpec, new Object[] { first.getId() }, Cursor.Direction.BACKWARD ).encode();
            }
        }

        Sort idSort = Sort.by( null, "id", Sort.Direction.ASC, Sort.NullMode.LAST, "id" );
        return new CursorPage<>( data, idSort, limit, nextCursor, prevCursor, null );
    }
}
