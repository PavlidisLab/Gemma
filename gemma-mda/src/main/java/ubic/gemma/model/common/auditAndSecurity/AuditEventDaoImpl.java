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
import java.util.Map.Entry;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
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
import org.hibernate.proxy.HibernateProxy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.stereotype.Repository;

import ubic.gemma.model.common.Auditable;
import ubic.gemma.model.common.auditAndSecurity.eventType.ArrayDesignGeneMappingEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;
import ubic.gemma.model.common.auditAndSecurity.eventType.OKStatusFlagEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.TroubleStatusFlagEvent;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.util.CommonQueries;

/**
 * @see ubic.gemma.model.common.auditAndSecurity.AuditEvent
 * @author pavlidis
 * @version $Id$
 */
@Repository
public class AuditEventDaoImpl extends ubic.gemma.model.common.auditAndSecurity.AuditEventDaoBase {

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
        if ( auditEvent == null ) return;
        this.getHibernateTemplate().execute( new HibernateCallback<Object>() {
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
     * Determine the full set of AuditEventTypes that are needed (that is, subclasses of the given classes)
     * 
     * @param types
     * @return
     */
    private List<String> getClassHierarchy( Collection<Class<? extends AuditEventType>> types ) {
        List<String> classes = new ArrayList<String>();
        for ( Class<? extends AuditEventType> t : types ) {
            classes.addAll( getClassHierarchy( t ) );
        }
        return classes;
    }

    /**
     * Determine the full set of AuditEventTypes that are needed (that is, subclasses of the given class)
     * 
     * @param type Class
     * @return A List of class names, including the given type.
     */
    private List<String> getClassHierarchy( Class<? extends AuditEventType> type ) {
        List<String> classes = new ArrayList<String>();
        classes.add( getImplClass( type ) );

        // how to determine subclasses? There is no way to do this but the hibernate way.
        SingleTableEntityPersister classMetadata = ( SingleTableEntityPersister ) this.getSessionFactory()
                .getClassMetadata( getImplClass( type ) );
        if ( classMetadata == null ) return classes;

        if ( classMetadata.hasSubclasses() ) {
            String[] subclasses = classMetadata.getSubclassClosure(); // this includes the superclass, fully qualified
            // names.
            classes.clear();
            for ( String string : subclasses ) {
                // strip qualification to leave BlabablImpl
                string = string.replaceFirst( ".+\\.", "" );
                classes.add( string );
            }
        }
        return classes;
    }

    private String getImplClass( Class<? extends AuditEventType> type ) {
        String canonicalName = type.getName();
        return canonicalName.endsWith( "Impl" ) ? type.getName() : type.getName() + "Impl";
    }

    @Override
    protected List<AuditEvent> handleGetEvents( final Auditable auditable ) {
        if ( auditable == null ) throw new IllegalArgumentException( "Auditable cannot be null" );

        this.getHibernateTemplate().execute( new HibernateCallback<Object>() {
            public Object doInHibernate( Session session ) throws HibernateException {
                session.lock( auditable, LockMode.NONE );
                Hibernate.initialize( auditable );
                Hibernate.initialize( auditable.getAuditTrail() );
                Hibernate.initialize( auditable.getAuditTrail().getEvents() );
                for ( AuditEvent ae : auditable.getAuditTrail().getEvents() ) {
                    thaw( ae );
                }
                return null;
            }
        } );

        return ( List<AuditEvent> ) auditable.getAuditTrail().getEvents();
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

        if ( type.equals( ArrayDesignGeneMappingEvent.class ) ) {
            log.info( "woah" );
        }

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
            queryObject.setParameter( "trail", auditTrail );
            queryObject.setMaxResults( 1 );

            Collection<AuditEvent> results = queryObject.list();

            if ( results == null || results.isEmpty() ) return null;

            AuditEvent result = results.iterator().next();
            result.getPerformer(); // Hit performer to make hibernate initialize it.
            return result;

        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }

    }

    /*
     * (non-Javadoc)
     * 
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

        org.hibernate.Query queryObject = super.getSession().createQuery( queryString );
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

        timer.stop();
        if ( timer.getTime() > 500 ) {
            log.info( "Last event of type " + type.getSimpleName() + " retrieved for " + auditables.size()
                    + " items in " + timer.getTime() + "ms" );
        }

        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.common.auditAndSecurity.AuditEventDao#getLastEvents(java.util.Collection,
     * java.util.Collection)
     */
    public Map<Class<? extends AuditEventType>, Map<Auditable, AuditEvent>> getLastEvents(
            Collection<? extends Auditable> auditables, Collection<Class<? extends AuditEventType>> types ) {
        StopWatch timer = new StopWatch();
        timer.start();

        Map<Class<? extends AuditEventType>, Map<Auditable, AuditEvent>> results = new HashMap<Class<? extends AuditEventType>, Map<Auditable, AuditEvent>>();
        if ( auditables.size() == 0 ) return results;

        for ( Class<? extends AuditEventType> t : types ) {
            results.put( t, new HashMap<Auditable, AuditEvent>() );
        }

        final Map<AuditTrail, Auditable> atmap = getAuditTrailMap( auditables );

        List<String> classes = getClassHierarchy( types );

        final String queryString = "select et, trail, event from ubic.gemma.model.common.auditAndSecurity.AuditTrail trail "
                + "inner join trail.events event inner join event.eventType et inner join fetch event.performer where trail in (:trails) "
                + "and et.class in (" + StringUtils.join( classes, "," ) + ") order by event.date desc ";

        org.hibernate.Query queryObject = super.getSession().createQuery( queryString );
        queryObject.setParameterList( "trails", atmap.keySet() );

        List<?> qr = queryObject.list();

        for ( Object o : qr ) {
            Object[] ar = ( Object[] ) o;
            AuditEventType ty = ( AuditEventType ) ar[0];
            AuditTrail t = ( AuditTrail ) ar[1];
            AuditEvent e = ( AuditEvent ) ar[2];

            /*
             * This is a bit inefficient. Loop needed because returned type is Impl (and probably a proxy). But probably
             * query is the bottleneck.
             */
            for ( Class<? extends AuditEventType> ti : types ) {
                if ( ti.isAssignableFrom( ty.getClass() ) ) {
                    // FIXME we need to distinguish subclasses of validation events, for example.
                    Map<Auditable, AuditEvent> innerMap = results.get( ti );

                    assert innerMap != null;

                    // only one event per type
                    if ( !innerMap.containsKey( atmap.get( t ) ) ) {
                        innerMap.put( atmap.get( t ), e );
                    }
                    break;
                }
            }
        }

        timer.stop();
        if ( timer.getTime() > 1000 ) {
            log.info( "Last events retrieved for  " + types.size() + " different types for " + auditables.size()
                    + " items in " + timer.getTime() + "ms" );
        }

        return results;
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

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.common.auditAndSecurity.AuditEventDao#getLastOutstandingTroubleEvent(java.util.Collection)
     */
    public AuditEvent getLastOutstandingTroubleEvent( Collection<AuditEvent> events ) {
        return getLastOutstandingTroubleEventNoSort( events );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.common.auditAndSecurity.AuditEventDao#getLastOutstandingTroubleEvents(java.util.Collection)
     */
    public Map<Auditable, AuditEvent> getLastOutstandingTroubleEvents( Collection<? extends Auditable> auditables ) {
        Collection<Class<? extends AuditEventType>> types = new HashSet<Class<? extends AuditEventType>>();
        types.add( TroubleStatusFlagEvent.class );
        types.add( OKStatusFlagEvent.class );
        Map<Class<? extends AuditEventType>, Map<Auditable, AuditEvent>> lastEvents = this.getLastEvents( auditables,
                types );

        Map<Auditable, AuditEvent> results = new HashMap<Auditable, AuditEvent>();

        if ( !lastEvents.containsKey( TroubleStatusFlagEvent.class ) ) {
            return results;
        }

        /*
         * Common case: check if auditables are EEs.
         */

        Collection<ExpressionExperiment> ees = new HashSet<ExpressionExperiment>();

        for ( Auditable a : auditables ) {

            Map<Auditable, AuditEvent> trouble = lastEvents.get( TroubleStatusFlagEvent.class );

            if ( !trouble.containsKey( a ) ) {
                // no trouble! but we should check the array designs later.
                if ( a instanceof ExpressionExperiment ) {
                    ees.add( ( ExpressionExperiment ) a );
                }
                continue;
            }

            AuditEvent t = trouble.get( a );

            Map<Auditable, AuditEvent> ok = lastEvents.get( OKStatusFlagEvent.class );

            if ( ok.containsKey( a ) ) {
                // do we have a more recent ok event?
                AuditEvent o = ok.get( a );
                if ( o.getDate().after( t.getDate() ) ) {
                    continue;
                }

                results.put( a, t );

            } else {
                results.put( a, t );
            }

        }

        if ( !ees.isEmpty() ) {
            Map<ArrayDesign, Collection<ExpressionExperiment>> ads = CommonQueries.getArrayDesignsUsed( ees, this
                    .getSession() );

            Map<Auditable, AuditEvent> arrayDesignTrouble = getLastOutstandingTroubleEvents( ads.keySet() );

            for ( Entry<Auditable, AuditEvent> e : arrayDesignTrouble.entrySet() ) {
                for ( ExpressionExperiment ee : ads.get( e.getKey() ) ) {
                    results.put( ee, e.getValue() );
                }
            }
            results.putAll( arrayDesignTrouble );

        }

        return results;

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
            Class<? extends Auditable> clazz = a.getClass();

            /*
             * proxy?
             */
            String clazzName = clazz.getName();
            if ( a instanceof HibernateProxy ) {
                clazzName = ( ( HibernateProxy ) a ).getHibernateLazyInitializer().getEntityName();
            }

            if ( !clazzmap.containsKey( clazzName ) ) {
                clazzmap.put( clazzName, new HashSet<Auditable>() );
            }
            clazzmap.get( clazzName ).add( a );
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

    @Override
    public boolean hasEvent( Auditable a, Class<? extends AuditEventType> type ) {
        return this.getLastEvent( a, type ) != null;
    }

    @Override
    public void retainHavingEvent( final Collection<? extends Auditable> a, final Class<? extends AuditEventType> type ) {

        CollectionUtils.filter( a, new Predicate() {
            @Override
            public boolean evaluate( Object arg0 ) {
                return hasEvent( ( Auditable ) arg0, type );
            }
        } );

    }

    @Override
    public void retainLackingEvent( final Collection<? extends Auditable> a, final Class<? extends AuditEventType> type ) {
        CollectionUtils.filter( a, new Predicate() {
            @Override
            public boolean evaluate( Object arg0 ) {
                return !hasEvent( ( Auditable ) arg0, type );
            }
        } );
    }

}