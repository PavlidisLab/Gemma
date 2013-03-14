/*
 * The Gemma project.
 * 
 * Copyright (c) 2006 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package ubic.gemma.model.common.auditAndSecurity;

import org.hibernate.Query;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Repository;
import ubic.gemma.model.common.Auditable;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;

import java.util.Calendar;
import java.util.Collection;

/**
 * @see ubic.gemma.model.common.auditAndSecurity.AuditTrailDao
 * @author pavlidis
 * @version $Id$
 */
@Repository
public class AuditTrailDaoImpl extends HibernateDaoSupport implements AuditTrailDao {

    @Autowired
    public AuditTrailDaoImpl( SessionFactory sessionFactory ) {
        super.setSessionFactory( sessionFactory );
    }


    @Override
    public AuditEvent addEvent( final Auditable auditable, final AuditEvent auditEvent ) {

        if ( auditEvent.getAction() == null ) {
            throw new IllegalArgumentException( "auditEvent was missing a required field" );
        }

        if ( auditEvent.getDate() == null ) {
            auditEvent.setDate( Calendar.getInstance().getTime() );
        }

        if ( auditEvent.getPerformer() == null ) {
            User user = getUser(); // could be null, if anonymous.
            auditEvent.setPerformer( user );
        }

        // Hibernate.initialize( auditable );

        AuditTrail trail = auditable.getAuditTrail();

        if ( trail == null ) {
            /*
             * Note: this step should be done by the AuditAdvice when the entity was first created, so this is just
             * defensive.
             */
            logger.warn("AuditTrail was null. It should have been initialized by the AuditAdvice when the entity was first created.");
            trail = AuditTrail.Factory.newInstance();
            auditable.setAuditTrail( trail );
        }
//        else if ( auditable.getAuditTrail().getId() != null ) {
//            trail = ( AuditTrail ) this.getSession(false).get( AuditTrailImpl.class, auditable.getAuditTrail().getId() );
//            // Isn't it a no-op in this case? trail is already associated with current session.
//            //this.getSession(false).buildLockRequest( LockOptions.NONE ).lock( trail );
//        }

        trail.addEvent( auditEvent );

        this.getHibernateTemplate().saveOrUpdate( trail );

        auditable.setAuditTrail( trail );

        return auditEvent;
    }

    @Override
    public AuditTrail create( final AuditTrail auditTrail ) {
        if ( auditTrail == null ) {
            throw new IllegalArgumentException( "AuditTrail.create - 'auditTrail' can not be null" );
        }
        this.getHibernateTemplate().save( auditTrail );
        return auditTrail;
    }

    @Override
    public Collection<? extends AuditTrail> create( final Collection<? extends AuditTrail> entities ) {
        return create( entities );
    }

    @Override
    public AuditTrail load( final java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "AuditTrail.load - 'id' can not be null" );
        }
        final Object entity = this.getHibernateTemplate().get( AuditTrailImpl.class, id );
        return ( AuditTrail ) entity;
    }

    @Override
    public Collection<? extends AuditTrail> loadAll() {
        return this.getHibernateTemplate().loadAll( AuditTrailImpl.class );
    }

    @Override
    public void remove( java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "AuditTrail.remove - 'id' can not be null" );
        }
        AuditTrail entity = this.load( id );
        if ( entity != null ) {
            this.remove( entity );
        }
    }

    @Override
    public void update( AuditTrail auditTrail ) {
        if ( auditTrail == null ) {
            throw new IllegalArgumentException( "AuditTrail.update - 'auditTrail' can not be null" );
        }
        this.getHibernateTemplate().update( auditTrail );
    }


    @Override
    public void remove( Collection<? extends AuditTrail> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "AuditTrail.remove - 'entities' can not be null" );
        }
        this.getHibernateTemplate().deleteAll( entities );
    }

    @Override
    public void remove( AuditTrail auditTrail ) {
        if ( auditTrail == null ) {
            throw new IllegalArgumentException( "AuditTrail.remove - 'auditTrail' can not be null" );
        }
        this.getHibernateTemplate().delete( auditTrail );
    }

    @Override
    public void update( final Collection<? extends AuditTrail> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "AuditTrail.update - 'entities' can not be null" );
        }
        for ( AuditTrail auditTrail : entities ) {
            update( auditTrail );
        }
    }


    /**
     * FIXME this returns a list, but there is no particular ordering enforced?
     * 
     * @param entityClass
     * @param auditEventClass
     * @return
     */
    @Override
    public Collection<Auditable> getEntitiesWithEvent( Class<? extends Auditable> entityClass,
            Class<? extends AuditEventType> auditEventClass ) {

        String entityCanonicalName = entityClass.getName();
        entityCanonicalName = entityCanonicalName.endsWith( "Impl" ) ? entityClass.getName() : entityClass.getName()
                + "Impl";

        String eventCanonicalName = auditEventClass.getName();
        eventCanonicalName = eventCanonicalName.endsWith( "Impl" ) ? auditEventClass.getName() : auditEventClass
                .getName() + "Impl";

        String queryString = "select distinct auditableEntity from " + entityCanonicalName + " auditableEntity "
                + " inner join auditableEntity.auditTrail trail inner join trail.events auditEvents "
                + " inner join auditEvents.eventType et where et.class = " + eventCanonicalName;

        // FIXME add order by clause?

        /*
         * This might be the best place to embody rules that determine if the event is still 'live'.
         */

        Query queryObject = super.getSession(false).createQuery( queryString );
        return queryObject.list();
    }

    @Override
    public Collection<? extends AuditTrail> load( Collection<Long> ids ) {
        return this.getHibernateTemplate().findByNamedParam( "from  AuditTrailImpl where id in (:ids)", "ids", ids );
    }

    /**
     * @return
     */
    private String getPrincipalName() {
        Object obj = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        String username = null;
        if ( obj instanceof UserDetails ) {
            username = ( ( UserDetails ) obj ).getUsername();
        } else {
            username = obj.toString();
        }

        return username;
    }

    /**
     * @return
     */
    private User getUser() {
        String name = getPrincipalName();
        assert name != null; // might be anonymous

        /*
         * Note: this name is defined in the applicationContext-security.xml file. Normally audit events would not be
         * added by 'anonymous' using the methods in this class, but this allows the possibility.
         */
        if ( name.equals( "anonymous" ) ) {
            return null;
        }

        String queryString = "from UserImpl where userName=:userName";
        java.util.List<?> results = this.getHibernateTemplate().findByNamedParam( queryString, "userName", name );

        assert results.size() == 1;
        Object result = results.iterator().next();
        return ( User ) result;
    }

}