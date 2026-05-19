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
import ubic.gemma.model.common.auditAndSecurity.curation.TicketPriority;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketState;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketTargetType;
import ubic.gemma.persistence.service.AbstractDao;

import java.util.Arrays;
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
        StringBuilder hql = new StringBuilder( "select t from Ticket t where 1=1" );
        if ( openOnly ) {
            hql.append( " and t.state in :openStates" );
        }
        if ( assigneeId != null ) {
            hql.append( " and t.assignee.id = :assigneeId" );
        }
        if ( priority != null ) {
            hql.append( " and t.priority = :priority" );
        }
        hql.append( " order by t.updatedAt desc" );
        org.hibernate.query.Query<?> q = this.getSessionFactory().getCurrentSession().createQuery( hql.toString() );
        if ( openOnly ) {
            q.setParameterList( "openStates", Arrays.asList( TicketState.OPEN, TicketState.IN_PROGRESS ) );
        }
        if ( assigneeId != null ) {
            q.setParameter( "assigneeId", assigneeId );
        }
        if ( priority != null ) {
            q.setParameter( "priority", priority );
        }
        if ( offset > 0 ) {
            q.setFirstResult( offset );
        }
        if ( limit > 0 ) {
            q.setMaxResults( limit );
        }
        //noinspection unchecked
        return ( List<Ticket> ) q.list();
    }

    @Override
    public long countTickets( boolean openOnly, @Nullable Long assigneeId, @Nullable TicketPriority priority ) {
        StringBuilder hql = new StringBuilder( "select count(t) from Ticket t where 1=1" );
        if ( openOnly ) {
            hql.append( " and t.state in :openStates" );
        }
        if ( assigneeId != null ) {
            hql.append( " and t.assignee.id = :assigneeId" );
        }
        if ( priority != null ) {
            hql.append( " and t.priority = :priority" );
        }
        org.hibernate.query.Query<?> q = this.getSessionFactory().getCurrentSession().createQuery( hql.toString() );
        if ( openOnly ) {
            q.setParameterList( "openStates", Arrays.asList( TicketState.OPEN, TicketState.IN_PROGRESS ) );
        }
        if ( assigneeId != null ) {
            q.setParameter( "assigneeId", assigneeId );
        }
        if ( priority != null ) {
            q.setParameter( "priority", priority );
        }
        return ( Long ) q.uniqueResult();
    }
}
