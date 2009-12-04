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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.persister.entity.SingleTableEntityPersister;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.stereotype.Repository;

import ubic.gemma.model.common.Auditable;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;
import ubic.gemma.model.common.auditAndSecurity.eventType.OKStatusFlagEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.TroubleStatusFlagEvent;

/**
 * @see ubic.gemma.model.common.auditAndSecurity.AuditEvent
 * @author pavlidis
 * @version $Id$
 */
@Repository
public class AuditEventDaoImpl extends ubic.gemma.model.common.auditAndSecurity.AuditEventDaoBase {

    /**
     * Matches field in ehache.xml
     */
    private static final String AUDIT_EVENTS_QUERY_CACHE_REGION = "auditEvents-qc";

    private static Log log = LogFactory.getLog( AuditEventDaoImpl.class.getName() );

    /**
     * Classes that we track for 'updated since'. This is used for "What's new" functionality.
     */
    private static String[] AUDITABLES_TO_TRACK_FOR_WHATSNEW = {
    // "ubic.gemma.model.expression.analysis.ExpressionAnalysisImpl",
            "ubic.gemma.model.expression.arrayDesign.ArrayDesignImpl",
            // "ubic.gemma.model.common.description.BibliographicReferenceImpl",
            // "ubic.gemma.model.common.auditAndSecurity.ContactImpl",
            "ubic.gemma.model.expression.experiment.ExpressionExperimentImpl" };

    @Autowired
    public AuditEventDaoImpl( SessionFactory sessionFactory ) {
        super.setSessionFactory( sessionFactory );
    }

    /**
     * Note that this only returns selected classes of auditables.
     * 
     * @see ubic.gemma.model.common.auditAndSecurity.AuditEventDao#getNewSinceDate(java.util.Date)
     * @return Collection of Auditables
     */
    @Override
    @SuppressWarnings("unchecked")
    protected java.util.Collection<Auditable> handleGetNewSinceDate( java.util.Date date ) {
        Collection<Auditable> result = new HashSet<Auditable>();
        for ( String clazz : AUDITABLES_TO_TRACK_FOR_WHATSNEW ) {
            String queryString = "select distinct adb from "
                    + clazz
                    + " adb inner join adb.auditTrail atr inner join atr.events as ae where ae.date > :date and ae.action='C'";
            try {
                org.hibernate.Query queryObject = super.getSession().createQuery( queryString );
                queryObject.setParameter( "date", date );
                result.addAll( queryObject.list() );
            } catch ( org.hibernate.HibernateException ex ) {
                throw super.convertHibernateAccessException( ex );
            }
        }
        return result;
    }

    /**
     * Note that this only returns selected classes of auditables.
     * 
     * @see ubic.gemma.model.common.auditAndSecurity.AuditEventDao#getUpdatedSinceDate(java.util.Date)
     * @return Collection of Auditables
     */
    @Override
    @SuppressWarnings("unchecked")
    protected java.util.Collection handleGetUpdatedSinceDate( java.util.Date date ) {
        Collection<Auditable> result = new HashSet<Auditable>();
        for ( String clazz : AUDITABLES_TO_TRACK_FOR_WHATSNEW ) {
            String queryString = "select distinct adb from "
                    + clazz
                    + " adb inner join adb.auditTrail atr inner join atr.events as ae where ae.date > :date and ae.action='U'";
            try {
                org.hibernate.Query queryObject = super.getSession().createQuery( queryString );
                queryObject.setParameter( "date", date );
                result.addAll( queryObject.list() );
            } catch ( org.hibernate.HibernateException ex ) {
                throw super.convertHibernateAccessException( ex );
            }
        }
        return result;
    }

    @Override
    protected void handleThaw( final AuditEvent auditEvent ) throws Exception {
        this.getHibernateTemplate().executeWithNativeSession( new HibernateCallback<Object>() {
            public Object doInHibernate( Session session ) throws HibernateException {
                /*
                 * FIXME this check really won't work. This thaw will not operate correctly if the event isn't already
                 * associated with the session.
                 */
                if ( session.get( AuditEventImpl.class, auditEvent.getId() ) == null ) {
                    session.lock( auditEvent, LockMode.NONE );
                }
                Hibernate.initialize( auditEvent );
                Hibernate.initialize( auditEvent.getPerformer() );
                return null;
            }
        } );

    }

    /**
     * Determine the full set of AuditEventTypes that are needed (that is, subclasses of the given class)
     * 
     * @param type Class
     * @return A List of class names, including the given type.
     */
    protected List<String> getClassHierarchy( Class<? extends AuditEventType> type ) {
        List<String> classes = new ArrayList<String>();
        classes.add( getImplClass( type ) );

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

    private String getImplClass( Class<? extends AuditEventType> type ) {
        String canonicalName = type.getCanonicalName();
        return canonicalName.endsWith( "Impl" ) ? type.getCanonicalName() : type.getCanonicalName() + "Impl";
    }

    @Override
    protected List<AuditEvent> handleGetEvents( final Auditable auditable ) {
        if ( auditable == null ) throw new IllegalArgumentException( "Auditable cannot be null" );
        return getHibernateTemplate().execute(
                new org.springframework.orm.hibernate3.HibernateCallback<List<AuditEvent>>() {
                    public List<AuditEvent> doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        session.lock( auditable, LockMode.NONE );
                        Hibernate.initialize( auditable );
                        Hibernate.initialize( auditable.getAuditTrail() );
                        // It really is a list, even if andromda refused to declare it thusly.
                        List<AuditEvent> events = ( List<AuditEvent> ) auditable.getAuditTrail().getEvents();
                        Hibernate.initialize( events );
                        for ( AuditEvent auditEvent : events ) {
                            Hibernate.initialize( auditEvent );
                            Hibernate.initialize( auditEvent.getPerformer() );
                        }
                        return events;
                    }
                } );
    }

    /**
     * @param auditTrail
     * @param type
     * @return
     * @throws java.lang.Exception
     */
    @SuppressWarnings("unchecked")
    protected ubic.gemma.model.common.auditAndSecurity.AuditEvent handleGetLastEvent( final AuditTrail auditTrail,
            Class<? extends AuditEventType> type ) {

        /*
         * For the = operator to work in hibernate the class or class name can't be passed in as a parameter :type -
         * also queryObject.setParameter("type", type.getClass()); doesn't work. Although technically this is now
         * vunerable to an sql injection attack, it seems moot as an attacker would have to have access to the JVM to
         * inject a malformed AuditEventType class name and if they had access to the JVM then sql injection is the
         * least of our worries. The real annoyance here is dealing with subclasses of event types.
         */

        List<String> classes = getClassHierarchy( type );

        if ( classes.size() == 0 ) {
            return null;
        }

        final String queryString = "select event from AuditTrailImpl trail "
                + "inner join trail.events event inner join event.eventType et inner join fetch event.performer "
                + "where trail = :trail " + "and et.class in (" + StringUtils.join( classes, "," )
                + ") order by event.date desc ";

        try {
            org.hibernate.Query queryObject = super.getSession().createQuery( queryString );
            queryObject.setCacheable( true );
            queryObject.setCacheRegion( AUDIT_EVENTS_QUERY_CACHE_REGION );

            queryObject.setParameter( "trail", auditTrail );
            queryObject.setMaxResults( 1 );

            Collection<AuditEvent> results = queryObject.list();

            if ( results == null || results.isEmpty() ) return null;

            return results.iterator().next();

        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }

    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.model.common.AuditableDaoBase#handleGetLastAuditEvent(java.util.Collection,
     * ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Map<Auditable, AuditEvent> handleGetLastEvent( final Collection<? extends Auditable> auditables,
            Class<? extends AuditEventType> type ) {

        Map<Auditable, AuditEvent> result = new HashMap<Auditable, AuditEvent>();
        if ( auditables.size() == 0 ) return result;

        final Map<AuditTrail, Auditable> atmap = getAuditTrailMap( auditables );

        List<String> classes = getClassHierarchy( type );

        final String queryString = "select trail, event from ubic.gemma.model.common.auditAndSecurity.AuditTrail trail "
                + "inner join trail.events event inner join event.eventType et inner join fetch event.performer where trail in (:trails) "
                + "and et.class in (" + StringUtils.join( classes, "," ) + ") order by event.date desc ";

        StopWatch timer = new StopWatch();
        timer.start();
        // note: this is fast.

        try {
            org.hibernate.Query queryObject = super.getSession().createQuery( queryString );
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
        if ( timer.getTime() > 500 ) {
            log.info( "Last event of type " + type.getSimpleName() + " retrieved for " + auditables.size()
                    + " items in " + timer.getTime() + "ms" );
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Map<Class<? extends AuditEventType>, Map<Auditable, AuditEvent>> handleGetLastTypedAuditEvents(
            Collection<? extends Auditable> auditables ) {

        Map<AuditTrail, Auditable> atmap = new HashMap<AuditTrail, Auditable>();
        for ( Auditable a : ( Collection<Auditable> ) auditables ) {
            atmap.put( a.getAuditTrail(), a );
        }

        final String queryString = "select trail,event,et from ubic.gemma.model.common.auditAndSecurity.AuditTrail trail "
                + "inner join fetch trail.events event inner join event.eventType et inner join fetch event.performer "
                + "where trail in (:trails) order by event.date desc ";

        Map<Class<? extends AuditEventType>, Map<Auditable, AuditEvent>> result = new HashMap<Class<? extends AuditEventType>, Map<Auditable, AuditEvent>>();
        try {
            org.hibernate.Query queryObject = super.getSession().createQuery( queryString );
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

    /**
     * @param events
     * @return
     */
    public AuditEvent getLastOutstandingTroubleEvent( Collection<AuditEvent> events ) {
        return getLastOutstandingTroubleEventNoSort( events );
    }

    /**
     * @param events
     * @return
     */
    private AuditEvent getLastOutstandingTroubleEventNoSort( Collection<AuditEvent> events ) {
        AuditEvent lastTroubleEvent = null;
        AuditEvent lastOKEvent = null;
        for ( AuditEvent event : events ) {
            if ( event.getEventType() == null ) {
                continue;
            } else if ( OKStatusFlagEvent.class.isAssignableFrom( event.getEventType().getClass() ) ) {
                if ( lastOKEvent == null || lastOKEvent.getDate().before( event.getDate() ) ) lastOKEvent = event;
            } else if ( TroubleStatusFlagEvent.class.isAssignableFrom( event.getEventType().getClass() ) ) {
                if ( lastTroubleEvent == null || lastTroubleEvent.getDate().before( event.getDate() ) )
                    lastTroubleEvent = event;
            }
        }
        if ( lastTroubleEvent != null )
            if ( lastOKEvent == null || lastOKEvent.getDate().before( lastTroubleEvent.getDate() ) )
                return lastTroubleEvent;
        return null;
    }

    /**
     * Essential thaw the auditables to the point we get the AuditTrail proxies for them.
     * 
     * @param auditables
     * @return
     */
    @SuppressWarnings("unchecked")
    private Map<AuditTrail, Auditable> getAuditTrailMap( final Collection<? extends Auditable> auditables ) {

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

        StopWatch timer = new StopWatch();
        timer.start();

        for ( String clazz : clazzmap.keySet() ) {
            final String trailQuery = "select a, a.auditTrail from " + clazz + " a where a in (:auditables) ";
            this.getHibernateTemplate().setCacheQueries( true );
            this.getHibernateTemplate().setQueryCacheRegion( "org.hibernate.cache.StandardQueryCache" );
            List res = this.getHibernateTemplate().findByNamedParam( trailQuery, "auditables", clazzmap.get( clazz ) );
            for ( Object o : res ) {
                Object[] ar = ( Object[] ) o;
                AuditTrail t = ( AuditTrail ) ar[1];
                Auditable a = ( Auditable ) ar[0];
                atmap.put( t, a );
            }

            timer.stop();
            if ( timer.getTime() > 1000 ) {
                log.info( "Audit trails retrieved for " + auditables.size() + " " + clazz + " items in "
                        + timer.getTime() + "ms" );
            }
            timer.reset();
            timer.start();

        }
        return atmap;
    }

    @Override
    protected AuditEvent handleGetLastEvent( Auditable auditable, Class<? extends AuditEventType> type ) {
        return this.handleGetLastEvent( auditable.getAuditTrail(), type );
    }

}