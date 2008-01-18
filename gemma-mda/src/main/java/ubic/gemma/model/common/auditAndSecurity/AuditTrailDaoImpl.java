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

import org.acegisecurity.context.SecurityContextHolder;
import org.acegisecurity.userdetails.UserDetails;
import org.hibernate.Hibernate; 
import org.springframework.orm.hibernate3.HibernateTemplate;

import ubic.gemma.model.common.Auditable;

/**
 * @see ubic.gemma.model.common.auditAndSecurity.AuditTrailDao
 * @author pavlidis
 * @version $Id$
 */
public class AuditTrailDaoImpl extends ubic.gemma.model.common.auditAndSecurity.AuditTrailDaoBase {

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.AuditTrailDao#thaw(ubic.gemma.model.common.auditAndSecurity.AuditTrail)
     */
    @Override
    protected void handleThaw( final ubic.gemma.model.common.auditAndSecurity.AuditTrail auditTrail ) {
        HibernateTemplate templ = this.getHibernateTemplate();
        templ.execute( new org.springframework.orm.hibernate3.HibernateCallback() {
            public Object doInHibernate( org.hibernate.Session session ) throws org.hibernate.HibernateException {
                Hibernate.initialize( auditTrail );
                if ( auditTrail.getEvents() == null ) return null;
                auditTrail.getEvents().size();
                session.evict( auditTrail );
                return null;
            }
        }, true );

    }

    /**
     * 
     */
    @Override
    protected void handleThaw( final Auditable auditable ) {
        if ( auditable == null ) return;
        HibernateTemplate templ = this.getHibernateTemplate();
        templ.execute( new org.springframework.orm.hibernate3.HibernateCallback() {
            public Object doInHibernate( org.hibernate.Session session ) throws org.hibernate.HibernateException {
                session.update( auditable );
                if ( auditable.getAuditTrail() == null ) return null;
                if ( auditable.getAuditTrail().getEvents() == null ) return null;

                auditable.getAuditTrail().getEvents().size();
                for ( AuditEvent event : auditable.getAuditTrail().getEvents() ) {
                    if ( event == null ) continue;
                    session.update( event );
                    if ( event.getEventType() != null ) session.update( event.getEventType() );
                }
                session.evict( auditable );
                return null;
            }
        }, true );

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

        HibernateTemplate templ = this.getHibernateTemplate();
        templ.execute( new org.springframework.orm.hibernate3.HibernateCallback() {
            public Object doInHibernate( org.hibernate.Session session ) throws org.hibernate.HibernateException {
            //    session.update( auditable );
                if ( !Hibernate.isInitialized( auditable ) ) Hibernate.initialize( auditable );
                session.persist( auditEvent );
                auditable.getAuditTrail().addEvent( auditEvent );
             //   session.update( auditable );
                session.flush();
                session.evict( auditable );
                return null;
            }
        }, false );

        assert auditEvent.getId() != null;
        assert auditable.getAuditTrail().getEvents().size() > 0;
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

        String queryString = "from ContactImpl where userName=:userName";
        java.util.List results = this.getHibernateTemplate().findByNamedParam( queryString, "userName", name );

        assert results.size() == 1;
        Object result = results.iterator().next();
        return ( User ) result;
    }

}