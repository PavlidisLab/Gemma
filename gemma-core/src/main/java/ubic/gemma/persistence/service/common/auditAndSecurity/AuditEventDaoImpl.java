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

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.commons.lang3.time.StopWatch;
import org.hibernate.Hibernate;
import org.hibernate.Query;
import org.hibernate.SessionFactory;
import org.hibernate.persister.entity.SingleTableEntityPersister;
import org.hibernate.proxy.HibernateProxy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import ubic.gemma.model.common.Auditable;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.AuditTrail;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;
import ubic.gemma.persistence.service.AbstractDao;
import ubic.gemma.persistence.util.CommonQueries;

import java.util.*;

/**
 * @author pavlidis
 * @see ubic.gemma.model.common.auditAndSecurity.AuditEvent
 */
@Repository
public class AuditEventDaoImpl extends AbstractDao<AuditEvent> implements AuditEventDao {

    /**
     * Classes that we track for 'updated since'. This is used for "What's new" functionality.
     */
    private static final String[] AUDITABLES_TO_TRACK_FOR_WHATS_NEW = {
            "ubic.gemma.model.expression.arrayDesign.ArrayDesign",
            "ubic.gemma.model.expression.experiment.ExpressionExperiment" };

    @Autowired
    public AuditEventDaoImpl( SessionFactory sessionFactory ) {
        super( AuditEvent.class, sessionFactory );
    }

    @Override
    public List<AuditEvent> getEvents( final Auditable auditable ) {
        if ( auditable == null )
            throw new IllegalArgumentException( "Auditable cannot be null" );

        if ( auditable.getAuditTrail() == null ) {
            throw new IllegalStateException( "Auditable did not have an audit trail: " + auditable );
        }

        Long id = auditable.getAuditTrail().getId();
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession()
                .createQuery( "select e from AuditTrailImpl t join t.events e where t.id = :id order by e.date,e.id " )
                .setParameter( "id", id ).list();

    }

    @Override
    public AuditEvent getLastEvent( Auditable auditable, Class<? extends AuditEventType> type ) {
        return this.getLastEvent( auditable.getAuditTrail(), type );
    }

    @Override
    public Map<Class<? extends AuditEventType>, Map<Auditable, AuditEvent>> getLastEvents(
            Collection<? extends Auditable> auditables, Collection<Class<? extends AuditEventType>> types ) {
        StopWatch timer = new StopWatch();
        timer.start();

        Map<Class<? extends AuditEventType>, Map<Auditable, AuditEvent>> results = new HashMap<>();
        if ( auditables.size() == 0 )
            return results;

        for ( Class<? extends AuditEventType> t : types ) {
            results.put( t, new HashMap<Auditable, AuditEvent>() );
        }

        final Map<AuditTrail, Auditable> atMap = this.getAuditTrailMap( auditables );

        List<String> classes = this.getClassHierarchy( types );

        //language=HQL
        final String queryString = "select et, trail, event from AuditTrailImpl trail "
                + "inner join trail.events event inner join event.eventType et inner join fetch event.performer where trail in (:trails) "
                + "and et.class in (:classes) order by event.date desc, event.id desc ";

        Query queryObject = this.getSessionFactory().getCurrentSession().createQuery( queryString );
        queryObject.setParameterList( "trails", atMap.keySet() );
        queryObject.setParameterList( "classes", classes );

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
                    Map<Auditable, AuditEvent> innerMap = results.get( ti );

                    assert innerMap != null;

                    // only replace event if its date is more recent.
                    Auditable ae = atMap.get( t );
                    if ( !innerMap.containsKey( ae ) || innerMap.get( ae ).getDate().compareTo( e.getDate() ) < 0 ) {
                        innerMap.put( atMap.get( t ), e );
                    }
                    break;
                }
            }
        }

        timer.stop();
        if ( timer.getTime() > 1000 ) {
            AbstractDao.log.info(
                    "Last events retrieved for  " + types.size() + " different types for " + auditables.size()
                            + " items in " + timer.getTime() + "ms" );
        }

        return results;
    }

    /**
     * Note that this only returns selected classes of auditables.
     *
     * @param date date
     * @return Collection of Auditables
     * @see AuditEventDao#getNewSinceDate(java.util.Date)
     */
    @Override
    public Collection<Auditable> getNewSinceDate( Date date ) {
        Collection<Auditable> result = new HashSet<>();
        for ( String clazz : AuditEventDaoImpl.AUDITABLES_TO_TRACK_FOR_WHATS_NEW ) {
            String queryString = "select distinct adb from " + clazz
                    + " adb inner join adb.auditTrail atr inner join atr.events as ae where ae.date > :date and ae.action='C'";
            this.tryAddAllToResult( result, queryString, date );
        }
        return result;
    }

    /**
     * Note that this only returns selected classes of auditables.
     *
     * @param date date
     * @return Collection of Auditables
     * @see AuditEventDao#getUpdatedSinceDate(Date)
     */
    @Override
    public Collection<Auditable> getUpdatedSinceDate( Date date ) {
        Collection<Auditable> result = new HashSet<>();
        for ( String clazz : AuditEventDaoImpl.AUDITABLES_TO_TRACK_FOR_WHATS_NEW ) {
            String queryString = "select distinct adb from " + clazz
                    + " adb inner join adb.auditTrail atr inner join atr.events as ae where ae.date > :date and ae.action='U'";
            this.tryAddAllToResult( result, queryString, date );
        }
        return result;
    }

    @Override
    public boolean hasEvent( Auditable a, Class<? extends AuditEventType> type ) {
        return this.getLastEvent( a, type ) != null;
    }

    @Override
    public void retainHavingEvent( final Collection<? extends Auditable> a,
            final Class<? extends AuditEventType> type ) {

        final Map<Auditable, AuditEvent> events = this.getLastEvent( a, type );

        CollectionUtils.filter( a, events::containsKey );

    }

    @Override
    public void retainLackingEvent( final Collection<? extends Auditable> a,
            final Class<? extends AuditEventType> type ) {
        StopWatch timer = new StopWatch();
        timer.start();
        final Map<Auditable, AuditEvent> events = this.getLastEvent( a, type );
        AbstractDao.log.info( "Phase I: " + timer.getTime() + "ms" );

        CollectionUtils.filter( a, ( Predicate<Auditable> ) arg0 -> !events.containsKey( arg0 ) );

    }

    private Map<Auditable, AuditEvent> getLastEvent( final Collection<? extends Auditable> auditables,
            Class<? extends AuditEventType> type ) {

        Map<Auditable, AuditEvent> result = new HashMap<>();
        if ( auditables.size() == 0 )
            return result;

        final Map<AuditTrail, Auditable> atMap = this.getAuditTrailMap( auditables );

        List<String> classes = this.getClassHierarchy( type );

        //language=HQL
        final String queryString = "select trail, ae from AuditTrailImpl trail "
                + "inner join trail.events ae inner join ae.eventType et inner join fetch ae.performer where trail in (:trails) "
                + "and et.class in (:classes) order by ae.date desc, ae.id desc ";

        StopWatch timer = new StopWatch();
        timer.start();

        Collection<AuditTrail> batch = new ArrayList<>();
        int batchSize = 100;

        for ( AuditTrail at : atMap.keySet() ) {
            batch.add( at );

            if ( batch.size() == batchSize ) {
                org.hibernate.Query queryObject = this.getSessionFactory().getCurrentSession()
                        .createQuery( queryString );
                queryObject.setParameterList( "trails", batch );
                queryObject.setParameterList( "classes", classes );
                queryObject.setReadOnly( true );

                List<?> qr = queryObject.list();
                if ( qr == null || qr.isEmpty() ) {
                    batch.clear();
                    continue;
                }

                this.putAllQrs( result, qr, atMap );
                batch.clear();
            }
        }

        if ( !batch.isEmpty() ) {
            org.hibernate.Query queryObject = this.getSessionFactory().getCurrentSession().createQuery( queryString );
            queryObject.setParameterList( "trails", batch ); // if too many will fail.
            queryObject.setParameterList( "classes", classes );
            queryObject.setReadOnly( true );

            List<?> qr = queryObject.list();
            if ( qr == null || qr.isEmpty() )
                return result;

            this.putAllQrs( result, qr, atMap );
        }

        timer.stop();
        if ( timer.getTime() > 500 ) {
            AbstractDao.log.info(
                    "Last event of type " + type.getSimpleName() + " retrieved for " + auditables.size() + " items in "
                            + timer.getTime() + "ms" );
        }

        return result;
    }

    private void tryAddAllToResult( Collection<Auditable> result, String queryString, Date date ) {
        org.hibernate.Query queryObject = this.getSessionFactory().getCurrentSession().createQuery( queryString );
        queryObject.setParameter( "date", date );
        //noinspection unchecked
        result.addAll( queryObject.list() );
    }

    private void putAllQrs( Map<Auditable, AuditEvent> result, List<?> qr, Map<AuditTrail, Auditable> atMap ) {
        for ( Object o : qr ) {
            Object[] ar = ( Object[] ) o;
            AuditTrail t = ( AuditTrail ) ar[0];
            AuditEvent e = ( AuditEvent ) ar[1];

            // only one event per object, please - the most recent.
            if ( result.containsKey( atMap.get( t ) ) )
                continue;

            result.put( atMap.get( t ), e );
        }
    }

    private AuditEvent getLastEvent( final AuditTrail auditTrail, Class<? extends AuditEventType> type ) {

        /*
         * For the = operator to work in hibernate the class or class name can't be passed in as a parameter :type -
         * also queryObject.setParameter("type", type.getClass()); doesn't work. Although technically this is now
         * vulnerable to an sql injection attack, it seems moot as an attacker would have to have access to the JVM to
         * inject a malformed AuditEventType class name and if they had access to the JVM then sql injection is the
         * least of our worries. The real annoyance here is dealing with subclasses of event types. [FIXME is this relevant?]
         */

        List<String> classes = this.getClassHierarchy( type );

        if ( classes.size() == 0 ) {
            return null;
        }

        //language=HQL
        final String queryString = "select event from AuditTrailImpl trail "
                + "inner join trail.events event inner join event.eventType et inner join fetch event.performer "
                + "fetch all properties where trail = :trail and et.class in (:classes) " + "order by event.date desc ";

        org.hibernate.Query queryObject = this.getSessionFactory().getCurrentSession().createQuery( queryString );
        queryObject.setCacheable( true );
        queryObject.setReadOnly( true );
        queryObject.setParameter( "trail", auditTrail );
        queryObject.setParameterList( "classes", classes );
        queryObject.setMaxResults( 1 );

        //noinspection unchecked
        Collection<AuditEvent> results = queryObject.list();

        if ( results == null || results.isEmpty() )
            return null;

        AuditEvent result = results.iterator().next();
        Hibernate.initialize( result.getPerformer() );
        return result;

    }

    /**
     * Essential thaw the auditables to the point we get the AuditTrail proxies for them.
     *
     * @param auditables auditables
     * @return map of audit trails to auditables
     */
    @SuppressWarnings("unchecked")
    private Map<AuditTrail, Auditable> getAuditTrailMap( final Collection<? extends Auditable> auditables ) {

        /*
         * This is the fastest way I've found to thaw the audit trails of a whole bunch of auditables. Because Auditable
         * is not mapped, we have to query for each class separately ... just in case the user has passed a
         * heterogeneous collection.
         */
        final Map<AuditTrail, Auditable> atMap = new HashMap<>();
        Map<String, Collection<Auditable>> classMap = new HashMap<>();
        for ( Auditable a : auditables ) {
            Class<? extends Auditable> clazz = a.getClass();

            /*
             * proxy?
             */
            String clazzName = clazz.getName();
            if ( a instanceof HibernateProxy ) {
                clazzName = ( ( HibernateProxy ) a ).getHibernateLazyInitializer().getEntityName();
            }

            if ( !classMap.containsKey( clazzName ) ) {
                classMap.put( clazzName, new HashSet<Auditable>() );
            }
            classMap.get( clazzName ).add( a );
        }

        StopWatch timer = new StopWatch();
        timer.start();

        for ( String clazz : classMap.keySet() ) {
            final String trailQuery = "select a, a.auditTrail from " + clazz + " a where a in (:auditables) ";
            List<?> res = getSessionFactory().getCurrentSession()
                    .createQuery( trailQuery )
                    .setParameter( "auditables", classMap.get( clazz ) )
                    .list();
            for ( Object o : res ) {
                Object[] ar = ( Object[] ) o;
                AuditTrail t = ( AuditTrail ) ar[1];
                Auditable a = ( Auditable ) ar[0];
                atMap.put( t, a );
            }

            timer.stop();
            if ( timer.getTime() > 1000 ) {
                AbstractDao.log.info(
                        "Audit trails retrieved for " + auditables.size() + " " + clazz + " items in " + timer.getTime()
                                + "ms" );
            }
            timer.reset();
            timer.start();

        }
        return atMap;
    }

    /**
     * Determine the full set of AuditEventTypes that are needed (that is, subclasses of the given class)
     *
     * @param type Class
     * @return A List of class names, including the given type.
     */
    private List<String> getClassHierarchy( Class<? extends AuditEventType> type ) {
        List<String> classes = new ArrayList<>();
        classes.add( type.getSimpleName() );

        // how to determine subclasses? There is no way to do this but the hibernate way.
        SingleTableEntityPersister classMetadata = ( SingleTableEntityPersister ) this.getSessionFactory()
                .getClassMetadata( type.getName() );
        if ( classMetadata == null )
            return classes;

        CommonQueries.addSubclasses( classes, classMetadata );
        return classes;
    }

    /**
     * Determine the full set of AuditEventTypes that are needed (that is, subclasses of the given classes)
     *
     * @param types types
     * @return list of types
     */
    private List<String> getClassHierarchy( Collection<Class<? extends AuditEventType>> types ) {
        List<String> classes = new ArrayList<>();
        for ( Class<? extends AuditEventType> t : types ) {
            classes.addAll( this.getClassHierarchy( t ) );
        }
        return classes;
    }

}