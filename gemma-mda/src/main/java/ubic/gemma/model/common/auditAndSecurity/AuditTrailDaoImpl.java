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
/**
 * This is only generated once! It will never be overwritten.
 * You can (and have to!) safely modify it by hand.
 */
package ubic.gemma.model.common.auditAndSecurity;

import org.springframework.orm.hibernate3.HibernateTemplate;

import ubic.gemma.model.common.Auditable;

/**
 * @see ubic.gemma.model.common.auditAndSecurity.AuditTrail
 */
public class AuditTrailDaoImpl extends ubic.gemma.model.common.auditAndSecurity.AuditTrailDaoBase {
    /**
     * @see ubic.gemma.model.common.auditAndSecurity.AuditTrailDao#thaw(ubic.gemma.model.common.auditAndSecurity.AuditTrail)
     */
    protected void handleThaw( final ubic.gemma.model.common.auditAndSecurity.AuditTrail auditTrail ) {
        HibernateTemplate templ = this.getHibernateTemplate();
        templ.execute( new org.springframework.orm.hibernate3.HibernateCallback() {
            public Object doInHibernate( org.hibernate.Session session ) throws org.hibernate.HibernateException {
                session.update( auditTrail );
                auditTrail.getEvents().size();
                return null;
            }
        }, true );

    }

    @Override
    protected AuditEvent handleAddEvent( final Auditable auditable, final AuditEvent auditEvent ) throws Exception {
        if ( auditEvent.getPerformer() == null ) {
            // get the principal. Not sure how to do this here.
        }

        HibernateTemplate templ = this.getHibernateTemplate();
        templ.execute( new org.springframework.orm.hibernate3.HibernateCallback() {
            public Object doInHibernate( org.hibernate.Session session ) throws org.hibernate.HibernateException {
                session.persist( auditEvent );
                auditable.getAuditTrail().addEvent( auditEvent );
                session.update( auditable );
                return null;
            }
        }, true );

        assert auditEvent.getId() != null;
        return auditEvent;
    }

}