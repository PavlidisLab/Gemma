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
import org.springframework.stereotype.Repository;
import ubic.gemma.model.common.auditAndSecurity.Contact;
import ubic.gemma.model.common.auditAndSecurity.curation.Ticket;
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
}
