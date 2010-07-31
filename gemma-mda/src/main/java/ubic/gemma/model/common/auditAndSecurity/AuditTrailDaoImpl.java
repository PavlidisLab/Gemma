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

import java.util.Calendar;
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Repository;

import ubic.gemma.model.common.Auditable;

/**
 * @see ubic.gemma.model.common.auditAndSecurity.AuditTrailDao
 * @author pavlidis
 * @version $Id$
 */
@Repository
public class AuditTrailDaoImpl extends ubic.gemma.model.common.auditAndSecurity.AuditTrailDaoBase {

    private static Log log = LogFactory.getLog( AuditTrailDaoImpl.class.getName() );

    @Autowired
    public AuditTrailDaoImpl( SessionFactory sessionFactory ) {
        super.setSessionFactory( sessionFactory );
    }

    @SuppressWarnings("unchecked")
    public Collection<? extends AuditTrail> load( Collection<Long> ids ) {
        return this.getHibernateTemplate().findByNamedParam( "from  AuditTrailImpl where id in (:ids)", "ids", ids );
    }

    /**
     * 
     */
    @Override
    protected AuditEvent handleAddEvent( final Auditable auditable, final AuditEvent auditEvent ) throws Exception {

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

        this.getHibernateTemplate().execute( new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
            public Object doInHibernate( org.hibernate.Session session ) throws org.hibernate.HibernateException {

                try {
                    session.lock( auditable, LockMode.NONE ); // always a bit dicey. We can get a non-unique
                    // object
                    // exception. Session.get won't work right if this is a proxy... etc.

                    if ( !Hibernate.isInitialized( auditable ) ) Hibernate.initialize( auditable );

                    /*
                     * Note: this step should be done by the AuditAdvice when the entity was first created, so this is
                     * just defensive.
                     */
                    if ( auditable.getAuditTrail() == null ) {
                        auditable.setAuditTrail( AuditTrail.Factory.newInstance() );
                    }

                    auditable.getAuditTrail().addEvent( auditEvent );

                    /*
                     * When the session is flushed at the close of the transaction, we'll get the change
                     */
                } catch ( HibernateException e ) {
                    log.warn( "Failed to update audit event on" + auditable + ": " + e.getMessage() + "; event was: "
                            + auditEvent );
                }
                return null;
            }
        } );

        return auditEvent;
    }

    /**
     * 
     */
    @Override
    protected void handleThaw( final Auditable auditable ) {
        if ( auditable == null ) return;
        HibernateTemplate templ = this.getHibernateTemplate();
        templ.executeWithNativeSession( new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
            public Object doInHibernate( org.hibernate.Session session ) throws org.hibernate.HibernateException {
                session.update( auditable );
                if ( auditable.getAuditTrail() == null ) return null;
                if ( auditable.getAuditTrail().getEvents() == null ) return null;
                thaw( auditable.getAuditTrail() );
                session.evict( auditable );
                return null;
            }
        } );

    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.AuditTrailDao#thaw(ubic.gemma.model.common.auditAndSecurity.AuditTrail)
     */
    @Override
    protected void handleThaw( final ubic.gemma.model.common.auditAndSecurity.AuditTrail auditTrail ) {
        HibernateTemplate templ = this.getHibernateTemplate();
        templ.executeWithNativeSession( new org.springframework.orm.hibernate3.HibernateCallback<AuditTrail>() {
            public AuditTrail doInHibernate( org.hibernate.Session session ) throws org.hibernate.HibernateException {
                session.lock( auditTrail, LockMode.NONE );
                Hibernate.initialize( auditTrail );
                if ( auditTrail.getEvents() == null ) return null;
                for ( AuditEvent ae : auditTrail.getEvents() ) {
                    Hibernate.initialize( ae );
                    if ( ae.getPerformer() != null ) {
                        User performer = ( User ) session.get( UserImpl.class, ae.getPerformer().getId() );
                        Hibernate.initialize( performer );
                        session.evict( performer );
                    } else {
                        /*
                         * This can happen if was the result of an anonymous user's actions.
                         */
                        log.debug( "No performer for audit event: id=" + ae.getId() + " - anonymous?" );
                    }
                }
                session.evict( auditTrail );
                return null;
            }
        } );

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
    @SuppressWarnings("unchecked")
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

        String queryString = "from ContactImpl where userName=:userName";
        java.util.List results = this.getHibernateTemplate().findByNamedParam( queryString, "userName", name );

        assert results.size() == 1;
        Object result = results.iterator().next();
        return ( User ) result;
    }

}