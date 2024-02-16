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
import org.hibernate.Query;
import org.hibernate.SessionFactory;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.persister.entity.SingleTableEntityPersister;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import ubic.gemma.model.common.Auditable;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;
import ubic.gemma.persistence.service.AbstractDao;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

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
                .createQuery( "select e from AuditTrail t join t.events e where t.id = :id order by e.date,e.id " )
                .setParameter( "id", id ).list();

    }

    @Override
    public AuditEvent getLastEvent( Auditable auditable, Class<? extends AuditEventType> type ) {
        return getLastEvents( Collections.singleton( auditable ), type, null ).get( auditable );
    }

    @Override
    public AuditEvent getLastEvent( Auditable auditable, Class<? extends AuditEventType> type, Collection<Class<? extends AuditEventType>> excludedTypes ) {
        return getLastEvents( Collections.singleton( auditable ), type, excludedTypes ).get( auditable );
    }

    @Override
    public Map<Class<? extends AuditEventType>, Map<Auditable, AuditEvent>> getLastEventsByType(
            Collection<? extends Auditable> auditables, Collection<Class<? extends AuditEventType>> types ) {
        Map<Class<? extends AuditEventType>, Map<Auditable, AuditEvent>> results = new HashMap<>();
        for ( Class<? extends AuditEventType> ti : types ) {
            Map<Auditable, AuditEvent> results2 = getLastEvents( auditables, ti, null );
            results.put( ti, results2.entrySet().stream()
                    .filter( e -> ti.isAssignableFrom( e.getValue().getEventType().getClass() ) )
                    .collect( Collectors.toMap( Map.Entry::getKey, Map.Entry::getValue ) ) );
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

        final Map<Auditable, AuditEvent> events = this.getLastEvents( a, type, null );

        CollectionUtils.filter( a, events::containsKey );

    }

    @Override
    public void retainLackingEvent( final Collection<? extends Auditable> a,
            final Class<? extends AuditEventType> type ) {
        StopWatch timer = new StopWatch();
        timer.start();
        final Map<Auditable, AuditEvent> events = this.getLastEvents( a, type, null );
        AbstractDao.log.info( "Phase I: " + timer.getTime() + "ms" );

        CollectionUtils.filter( a, ( Predicate<Auditable> ) arg0 -> !events.containsKey( arg0 ) );

    }

    public Map<Auditable, AuditEvent> getCreateEvents( final Collection<? extends Auditable> auditables ) {

        if ( auditables.isEmpty() ) {
            return Collections.emptyMap();
        }

        Map<Auditable, AuditEvent> result = new HashMap<>( auditables.size() );

        final Map<Long, Auditable> atMap = auditables.stream()
                .collect( Collectors.toMap( a -> a.getAuditTrail().getId(), Function.identity() ) );

        final String queryString = "select trail.id, ae from AuditTrail trail join trail.events" +
                " ae where trail.id in :trails and ae.action = 'C'";
        Query queryObject = this.getSessionFactory().getCurrentSession()
                .createQuery( queryString )
                .setParameterList( "trails", atMap.keySet() );
        List<?> qr = queryObject.list();
        for ( Object o : qr ) {
            Object[] ar = ( Object[] ) o;
            Long t = ( Long ) ar[0];
            AuditEvent e = ( AuditEvent ) ar[1];
            result.put( atMap.get( t ), e );
        }
        return result;
    }

    private Map<Auditable, AuditEvent> getLastEvents( final Collection<? extends Auditable> auditables, Class<? extends AuditEventType> types, @Nullable Collection<Class<? extends AuditEventType>> excludedTypes ) {
        if ( auditables.isEmpty() ) {
            return Collections.emptyMap();
        }

        StopWatch timer = StopWatch.createStarted();

        Map<Auditable, AuditEvent> result = new HashMap<>( auditables.size() );

        // getId() does not require proxy initialization, otherwise we might inadvertently initialize the audit trail
        final Map<Long, Auditable> atMap = auditables.stream()
                .collect( Collectors.toMap( a -> a.getAuditTrail().getId(), Function.identity() ) );

        Set<Class<?>> classes = getClassHierarchy( types );

        // remove all the types we don't want
        if ( excludedTypes != null ) {
            for ( Class<? extends AuditEventType> excludedType : excludedTypes ) {
                classes.removeAll( getClassHierarchy( excludedType ) );
            }
        }

        //language=HQL
        final String queryString = "select trail.id, ae from AuditTrail trail "
                + "join trail.events ae "
                + "join fetch ae.eventType et " // fetching here prevents a separate select query
                + "where trail.id in :trails and type(et) in :classes "
                // annoyingly, Hibernate does not select the latest event when grouping by trail, so we have to fetch
                // them all
                + "group by trail, ae "
                // latest by date or ID to break ties
                + "order by ae.date desc, ae.id desc";

        Query queryObject = this.getSessionFactory().getCurrentSession()
                .createQuery( queryString )
                .setParameterList( "trails", atMap.keySet() )
                .setParameterList( "classes", classes );

        List<?> qr = queryObject.list();
        for ( Object o : qr ) {
            Object[] ar = ( Object[] ) o;
            Long t = ( Long ) ar[0];
            AuditEvent e = ( AuditEvent ) ar[1];
            // only retain the first one which is the latest (by date or ID)
            result.putIfAbsent( atMap.get( t ), e );
        }

        timer.stop();
        if ( timer.getTime() > 500 ) {
            AbstractDao.log.info( String.format( "Last event of type %s (closure: %s) retrieved for %d items in %d ms",
                    types.getName(), classes.stream().map( Class::getName ).collect( Collectors.joining( ", " ) ),
                    auditables.size(), timer.getTime() ) );
        }

        return result;
    }

    private void tryAddAllToResult( Collection<Auditable> result, String queryString, Date date ) {
        org.hibernate.Query queryObject = this.getSessionFactory().getCurrentSession().createQuery( queryString );
        queryObject.setParameter( "date", date );
        //noinspection unchecked
        result.addAll( queryObject.list() );
    }

    /**
     * Determine the full set of AuditEventTypes that are needed (that is, subclasses of the given class)
     *
     * @param type Class
     * @return A List of class names, including the given type.
     */
    private Set<Class<?>> getClassHierarchy( Class<? extends AuditEventType> type ) {
        // how to determine subclasses? There is no way to do this but the hibernate way.
        ClassMetadata classMetadata = this.getSessionFactory().getClassMetadata( type );
        if ( classMetadata instanceof SingleTableEntityPersister ) {
            Set<Class<?>> classes = new HashSet<>();
            // this includes the superclass, fully qualified
            String[] subclasses = ( ( SingleTableEntityPersister ) classMetadata ).getSubclassClosure();
            for ( String className : subclasses ) {
                try {
                    classes.add( Class.forName( className ) );
                } catch ( ClassNotFoundException e ) {
                    log.error( String.format( "Failed to find subclass %s of %s, it will not be included in the query."
                            , className, type.getName() ), e );
                }
            }
            return classes;
        } else {
            return Collections.singleton( type );
        }
    }
}