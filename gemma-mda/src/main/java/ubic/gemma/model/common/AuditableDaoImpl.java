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
package ubic.gemma.model.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.persister.entity.SingleTableEntityPersister;

import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.AuditTrail;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;

/**
 * @author pavlidis
 * @version $Id$
 * @see ubic.gemma.model.common.Auditable
 */
public class AuditableDaoImpl extends ubic.gemma.model.common.AuditableDaoBase {
    private Log log = LogFactory.getLog( this.getClass() );

    /**
     * This is basically a thaw method.
     */
    @Override
    public Collection handleGetAuditEvents( final Auditable auditable ) {
        if ( auditable == null ) throw new IllegalArgumentException( "Auditable cannot be null" );
        return ( Collection ) getHibernateTemplate().execute(
                new org.springframework.orm.hibernate3.HibernateCallback() {
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        return auditable.getAuditTrail().getEvents();
                    }
                } );

    }

    @Override
    protected ubic.gemma.model.common.auditAndSecurity.AuditEvent handleGetLastAuditEvent( final Auditable auditable,
            AuditEventType type ) throws java.lang.Exception {
        return handleGetLastAuditEvent( auditable.getAuditTrail(), type );
    }

    /**
     * @param auditTrail
     * @param type
     * @return
     * @throws java.lang.Exception
     */
    protected ubic.gemma.model.common.auditAndSecurity.AuditEvent handleGetLastAuditEvent( final AuditTrail auditTrail,
            AuditEventType type ) throws java.lang.Exception {

        /*
         * For the = operator to work in hibernate the class or class name can't be passed in as a parameter :type -
         * also queryObject.setParameter("type", type.getClass()); doesn't work. Although technically this is now
         * vunerable to an sql injection attack, it seems moot as an attacker would have to have access to the JVM to
         * inject a malformed AuditEventType class name and if they had access to the JVM then sql injection is the
         * least of our worries. The real annoyance here is dealing with subclasses of event types.
         */

        List<String> classes = getClassHierarchy( type.getClass() );

        final String queryString = "select event " + "from ubic.gemma.model.common.auditAndSecurity.AuditTrail trail "
                + "inner join fetch trail.events event inner join event.eventType et inner join fetch event.performer "
                + "where trail = :trail " + "and et.class in (" + StringUtils.join( classes, "," )
                + ") order by event.date desc ";

        try {
            org.hibernate.Query queryObject = super.getSession( false ).createQuery( queryString );
            queryObject.setCacheable( true );
            queryObject.setCacheRegion( "auditEvents" );

            queryObject.setParameter( "trail", auditTrail );
            queryObject.setMaxResults( 1 );

            Collection results = queryObject.list();

            if ( results == null || results.isEmpty() ) return null;

            return ( AuditEvent ) results.iterator().next();

        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }

    }

    /**
     * Determine the full set of AuditEventTypes that are needed (that is, subclasses of the given class)
     * 
     * @param type Class
     * @return A List of class names, including the given type.
     */
    protected List<String> getClassHierarchy( Class type ) {
        List<String> classes = new ArrayList<String>();
        classes.add( type.getCanonicalName() );

        // how to determine subclasses? There is no way to do this but the hibernate way.
        SingleTableEntityPersister classMetadata = ( SingleTableEntityPersister ) this.getSessionFactory()
                .getClassMetadata( type );
        if ( classMetadata.hasSubclasses() ) {
            String[] subclasses = classMetadata.getSubclassClosure(); // this includes the superclass, fully qualified
            // names.
            classes.clear();
            for ( String string : subclasses ) {
                string = string.replaceFirst( ".+\\.", "" );
                classes.add( string );
            }
        }
        return classes;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.common.AuditableDaoBase#handleGetLastAuditEvent(java.util.Collection,
     *      ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Map<Auditable, AuditEvent> handleGetLastAuditEvent( Collection auditables, AuditEventType type )
            throws Exception {

        Map<AuditTrail, Auditable> atmap = new HashMap<AuditTrail, Auditable>();
        for ( Auditable a : ( Collection<Auditable> ) auditables ) {
            atmap.put( a.getAuditTrail(), a );
        }

        List<String> classes = getClassHierarchy( type.getClass() );

        final String queryString = "select trail, event from ubic.gemma.model.common.auditAndSecurity.AuditTrail trail "
                + "inner join trail.events event inner join event.eventType et where trail in (:trails) "
                + "and et.class in (" + StringUtils.join( classes, "," ) + ") order by event.date desc ";

        Map<Auditable, AuditEvent> result = new HashMap<Auditable, AuditEvent>();
        try {
            org.hibernate.Query queryObject = super.getSession( false ).createQuery( queryString );
            queryObject.setCacheable( true );
            queryObject.setCacheRegion( "auditEvents" );
            queryObject.setParameterList( "trails", atmap.keySet() );

            List qr = queryObject.list();
            if ( qr == null || qr.isEmpty() ) return result;

            for ( Object o : qr ) {
                Object[] ar = ( Object[] ) o;
                AuditTrail t = ( AuditTrail ) ar[0];
                AuditEvent e = ( AuditEvent ) ar[1];

                // only one event per object, please - the most recent.
                if ( result.containsKey( atmap.get( t ) ) ) continue;

                result.put( atmap.get( t ), e );
            }

        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Map<Class, Map<Auditable, AuditEvent>> handleGetLastTypedAuditEvents( Collection auditables )
            throws Exception {

        Map<AuditTrail, Auditable> atmap = new HashMap<AuditTrail, Auditable>();
        for ( Auditable a : ( Collection<Auditable> ) auditables ) {
            atmap.put( a.getAuditTrail(), a );
        }

        final String queryString = "select trail,event,et from ubic.gemma.model.common.auditAndSecurity.AuditTrail trail "
                + "inner join fetch rail.events event inner join event.eventType et inner join fetch event.performer "
                + "where trail in (:trails) order by event.date desc ";

        Map<Class, Map<Auditable, AuditEvent>> result = new HashMap<Class, Map<Auditable, AuditEvent>>();
        try {
            org.hibernate.Query queryObject = super.getSession( false ).createQuery( queryString );
            queryObject.setCacheable( true );
            queryObject.setCacheRegion( "auditEvents" );
            queryObject.setParameter( "trails", atmap.keySet() );

            List qr = queryObject.list();
            if ( qr == null || qr.isEmpty() ) return result;

            for ( Object o : qr ) {
                Object[] ar = ( Object[] ) o;
                AuditTrail t = ( AuditTrail ) ar[0];
                AuditEvent e = ( AuditEvent ) ar[1];
                AuditEventType ty = ( AuditEventType ) ar[2];

                /*
                 * Careful with subclasses. The key in the hashtable should really only be the superclass.
                 */
                if ( !result.containsKey( ty.getClass() ) ) {
                    result.put( ty.getClass(), new HashMap<Auditable, AuditEvent>() );
                }
                Map<Auditable, AuditEvent> amap = result.get( ty.getClass() );

                // only one event per object, please - the most recent.
                if ( amap.containsKey( atmap.get( t ) ) ) continue;

                amap.put( atmap.get( t ), e );
            }

        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }

        return result;
    }
}