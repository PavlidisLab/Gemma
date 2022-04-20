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
package ubic.gemma.persistence.service.common.auditAndSecurity;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Repository;
import ubic.gemma.model.common.Auditable;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.AuditTrail;
import ubic.gemma.model.common.auditAndSecurity.AuditTrailImpl;
import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.persistence.service.AbstractDao;

import java.lang.reflect.Field;

/**
 * @author pavlidis
 * @see AuditTrailDao
 */
@Repository
public class AuditTrailDaoImpl extends AbstractDao<AuditTrail> implements AuditTrailDao {

    @Autowired
    public AuditTrailDaoImpl( SessionFactory sessionFactory ) {
        super( AuditTrail.class, sessionFactory );
    }

    @Override
    public AuditEvent addEvent( final Auditable auditable, final AuditEvent auditEvent ) {

        if ( auditEvent.getAction() == null ) {
            throw new IllegalArgumentException( "auditEvent was missing a required field" );
        }

        assert auditEvent.getDate() != null;

        if ( auditEvent.getPerformer() == null ) {
            User user = getUser(); // could be null, if anonymous.
            Field f = FieldUtils.getField( AuditEvent.class, "performer", true );
            assert f != null;
            try {
                f.set( auditEvent, user );
            } catch ( IllegalArgumentException | IllegalAccessException e ) {
                // shouldn't happen, but just in case...
                throw new RuntimeException( e );
            }
        }

        AuditTrail trail = auditable.getAuditTrail();

        if ( trail == null ) {
            /*
             * Note: this step should be done by the AuditAdvice when the entity was first created, so this is just
             * defensive.
             */
            log.warn(
                    "AuditTrail was null. It should have been initialized by the AuditAdvice when the entity was first created." );
            trail = AuditTrail.Factory.newInstance();
            auditable.setAuditTrail( trail );
        } else {

            /*
             * This assumes that nobody else in this session has modified this audit trail.
             */
            if ( trail.getId() != null )
                trail = ( AuditTrail ) this.getSessionFactory().getCurrentSession()
                        .get( AuditTrailImpl.class, trail.getId() );

        }

        trail.addEvent( auditEvent );

        this.getSessionFactory().getCurrentSession().saveOrUpdate( trail );

        auditable.setAuditTrail( trail );

        return auditEvent;
    }

    private String getPrincipalName() {
        Object obj = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        String username;
        if ( obj instanceof UserDetails ) {
            username = ( ( UserDetails ) obj ).getUsername();
        } else {
            username = obj.toString();
        }

        return username;
    }

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

        String queryString = "from User where userName=:userName";

        java.util.List<?> results = this.getSessionFactory().getCurrentSession().createQuery( queryString )
                .setParameter( "userName", name ).list();

        assert results.size() == 1;
        Object result = results.iterator().next();
        this.getSessionFactory().getCurrentSession().setReadOnly( result, true );
        return ( User ) result;
    }
}