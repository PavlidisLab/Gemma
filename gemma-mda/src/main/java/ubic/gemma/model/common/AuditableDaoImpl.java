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
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;
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

    private static Log log = LogFactory.getLog( AuditableDaoImpl.class.getName() );

    /**
     * This is basically a thaw method.
     */
    @Override
    public Collection handleGetAuditEvents( final Auditable auditable ) {
        if ( auditable == null ) throw new IllegalArgumentException( "Auditable cannot be null" );
        return ( Collection ) getHibernateTemplate().execute(
                new org.springframework.orm.hibernate3.HibernateCallback() {
                    @SuppressWarnings("unused")
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
                + "inner join trail.events event inner join event.eventType et inner join fetch event.performer "
                + "where trail.id = :trail " + "and et.class in (" + StringUtils.join( classes, "," )
                + ") order by event.date desc ";

        try {
            org.hibernate.Query queryObject = super.getSession( false ).createQuery( queryString );
            queryObject.setCacheable( true );
            queryObject.setCacheRegion( "auditEvents" );

            queryObject.setParameter( "trail", auditTrail.getId() );
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
        if ( classMetadata == null ) return classes;

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
    protected Map<Auditable, AuditEvent> handleGetLastAuditEvent( final Collection auditables, AuditEventType type )
            throws Exception {

        Map<Auditable, AuditEvent> result = new HashMap<Auditable, AuditEvent>();
        if ( auditables.size() == 0 ) return result;

        StopWatch timer = new StopWatch();
        timer.start();

        final Map<AuditTrail, Auditable> atmap = getAuditTrailMap( auditables );

        timer.stop();
        if ( timer.getTime() > 1000 ) {
            log.info( "Audit trails retrieved for " + auditables.size() + " items in " + timer.getTime() + "ms" );
        }

        List<String> classes = getClassHierarchy( type.getClass() );

        final String queryString = "select trail, event from ubic.gemma.model.common.auditAndSecurity.AuditTrail trail "
                + "inner join trail.events event inner join event.eventType et inner join fetch event.performer where trail in (:trails) "
                + "and et.class in (" + StringUtils.join( classes, "," ) + ") order by event.date desc ";

        timer.reset();
        timer.start();

        try {
            org.hibernate.Query queryObject = super.getSession( false ).createQuery( queryString );
            queryObject.setCacheable( true );
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
        timer.stop();
        if ( timer.getTime() > 1000 ) {
            log.info( "Last event of type " + type.getClass().getSimpleName() + " retrieved for " + auditables.size()
                    + " items in " + timer.getTime() + "ms" );
        }

        return result;
    }

    /**
     * Essential thaw the auditables to the point we get the AuditTrail proxies for them.
     * 
     * @param auditables
     * @return
     */
    @SuppressWarnings("unchecked")
    private Map<AuditTrail, Auditable> getAuditTrailMap( final Collection auditables ) {
        /*
         * This is the fastest way I've found to thaw the audit trails of a whole bunch of auditables. Because Auditable
         * is not mapped, we have to query for each class separately ... just in case the user has passed a
         * heterogeneous collection.
         */
        final Map<AuditTrail, Auditable> atmap = new HashMap<AuditTrail, Auditable>();
        Map<String, Collection<Auditable>> clazzmap = new HashMap<String, Collection<Auditable>>();
        for ( Auditable a : ( Collection<Auditable> ) auditables ) {
            if ( !clazzmap.containsKey( a.getClass().getSimpleName() ) ) {
                clazzmap.put( a.getClass().getSimpleName(), new HashSet<Auditable>() );
            }
            clazzmap.get( a.getClass().getSimpleName() ).add( a );
        }

        // this is the actual thaw, done in one select.
        for ( String clazz : clazzmap.keySet() ) {
            final String trailQuery = "select a, a.auditTrail from " + clazz + " a where a in (:auditables) ";
            List res = this.getHibernateTemplate().findByNamedParam( trailQuery, "auditables", clazzmap.get( clazz ) );
            for ( Object o : res ) {
                Object[] ar = ( Object[] ) o;
                AuditTrail t = ( AuditTrail ) ar[1];
                Auditable a = ( Auditable ) ar[0];
                atmap.put( t, a );
            }
        }
        return atmap;
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
                + "inner join fetch trail.events event inner join event.eventType et inner join fetch event.performer "
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