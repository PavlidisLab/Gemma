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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Repository;

import ubic.gemma.model.common.Auditable;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;

/**
 * @see ubic.gemma.model.common.auditAndSecurity.AuditTrailDao
 * @author pavlidis
 * @version $Id$
 */
@Repository
public class AuditTrailDaoImpl extends AuditTrailDaoBase {

    @Autowired
    public AuditTrailDaoImpl( SessionFactory sessionFactory ) {
        super.setSessionFactory( sessionFactory );
    }

    @Override
    public Collection<? extends AuditTrail> load( Collection<Long> ids ) {
        return this.getHibernateTemplate().findByNamedParam( "from  AuditTrailImpl where id in (:ids)", "ids", ids );
    }

    /**
     * 
     */
    @Override
    protected AuditEvent handleAddEvent( final Auditable auditable, final AuditEvent auditEvent ) {

        Auditable aprime = ( Auditable ) this.getSession().get( auditable.getClass(), auditable.getId() );

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

        /*
         * Note: this step should be done by the AuditAdvice when the entity was first created, so this is just
         * defensive.
         */
        if ( aprime.getAuditTrail() == null ) {
            aprime.setAuditTrail( AuditTrail.Factory.newInstance() );
        }

        aprime.getAuditTrail().addEvent( auditEvent );

        this.getHibernateTemplate().saveOrUpdate( aprime.getAuditTrail() );

        return auditEvent;
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

    /**
     * @param entityClass
     * @param auditEventClass
     * @return
     */
    @Override
    public java.util.Collection<Auditable> getEntitiesWithEvent( Class<? extends Auditable> entityClass,
            Class<? extends AuditEventType> auditEventClass ) {

        String entityCanonicalName = entityClass.getName();
        entityCanonicalName = entityCanonicalName.endsWith( "Impl" ) ? entityClass.getName() : entityClass.getName()
                + "Impl";

        String eventCanonicalName = auditEventClass.getName();
        eventCanonicalName = eventCanonicalName.endsWith( "Impl" ) ? auditEventClass.getName() : auditEventClass
                .getName() + "Impl";

        Collection<Auditable> result = new ArrayList<Auditable>();
        String queryString = "select distinct auditableEntity from " + entityCanonicalName + " auditableEntity "
                + " inner join auditableEntity.auditTrail trail " + " inner join trail.events auditEvents "
                + " inner join auditEvents.eventType et where et.class = " + eventCanonicalName;
        try {
            org.hibernate.Query queryObject = super.getSession().createQuery( queryString );
            result.addAll( queryObject.list() );
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
        return result;
    }

}