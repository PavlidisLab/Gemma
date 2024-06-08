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

import org.apache.commons.lang3.time.StopWatch;
import org.hibernate.Query;
import org.hibernate.SessionFactory;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.persister.entity.SingleTableEntityPersister;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.util.Assert;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.Auditable;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;
import ubic.gemma.persistence.service.AbstractDao;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static ubic.gemma.persistence.util.QueryUtils.optimizeParameterList;

/**
 * @author pavlidis
 * @see ubic.gemma.model.common.auditAndSecurity.AuditEvent
 */
@Repository
public class AuditEventDaoImpl extends AbstractDao<AuditEvent> implements AuditEventDao {

    @Autowired
    public AuditEventDaoImpl( SessionFactory sessionFactory ) {
        super( AuditEvent.class, sessionFactory );
    }

    @Override
    public List<AuditEvent> getEvents( final Auditable auditable ) {
        Assert.notNull( auditable.getAuditTrail(), "Auditable did not have an audit trail: " + auditable );
        Assert.notNull( auditable.getAuditTrail().getId(), "Auditable did not have a persistent audit trail: " + auditable );
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession()
                .createQuery( "select e from AuditTrail t join t.events e where t = :at order by e.date,e.id " )
                .setParameter( "at", auditable.getAuditTrail() )
                .list();

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
    public <T extends Auditable> Map<T, AuditEvent> getLastEvents( Collection<T> auditables, Class<? extends AuditEventType> type ) {
        return getLastEvents( auditables, type, null );
    }

    @Override
    public <T extends Auditable> Map<T, AuditEvent> getLastEvents( Class<T> auditableClass, Class<? extends AuditEventType> type ) {
        return getLastEvents( auditableClass, type, null );
    }

    @Override
    public <T extends Auditable> Collection<T> getNewSinceDate( Class<T> auditableClass, Date date ) {
        String entityName = getSessionFactory().getClassMetadata( auditableClass ).getEntityName();
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession()
                .createQuery( "select adb from " + entityName + " adb "
                        + "join adb.auditTrail atr "
                        + "join atr.events as ae "
                        + "where ae.date >= :date and ae.action='C' "
                        + "group by adb" )
                .setParameter( "date", date )
                .list();
    }

    @Override
    public <T extends Auditable> Collection<T> getUpdatedSinceDate( Class<T> auditableClass, Date date ) {
        String entityName = getSessionFactory().getClassMetadata( auditableClass ).getEntityName();
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession()
                .createQuery( "select adb from " + entityName + " adb "
                        + "join adb.auditTrail atr "
                        + "join atr.events as ae "
                        + "where ae.date >= :date and ae.action='U' "
                        + "group by adb" )
                .setParameter( "date", date )
                .list();
    }

    public <T extends Auditable> Map<T, AuditEvent> getCreateEvents( final Collection<T> auditables ) {
        if ( auditables.isEmpty() ) {
            return Collections.emptyMap();
        }
        Map<T, AuditEvent> result = new HashMap<>( auditables.size() );
        final Map<Long, T> atMap = auditables.stream()
                .collect( Collectors.toMap( a -> a.getAuditTrail().getId(), Function.identity() ) );
        //noinspection unchecked
        List<Object[]> qr = this.getSessionFactory().getCurrentSession()
                .createQuery( "select trail.id, ae from AuditTrail trail join trail.events" +
                        " ae where trail.id in :trails and ae.action = 'C'" )
                .setParameterList( "trails", optimizeParameterList( atMap.keySet() ) ).list();
        for ( Object[] o : qr ) {
            Long t = ( Long ) o[0];
            AuditEvent e = ( AuditEvent ) o[1];
            T a = atMap.get( t );
            // only put the first create event encountered
            if ( result.putIfAbsent( a, e ) != null ) {
                log.warn( "Auditable has more than one creation event: " + a );
            }
        }
        return result;
    }

    private <T extends Auditable> Map<T, AuditEvent> getLastEvents( final Collection<T> auditables, Class<? extends AuditEventType> types, @Nullable Collection<Class<? extends AuditEventType>> excludedTypes ) {
        if ( auditables.isEmpty() ) {
            return Collections.emptyMap();
        }

        StopWatch timer = StopWatch.createStarted();

        Map<T, AuditEvent> result = new HashMap<>( auditables.size() );

        // getId() does not require proxy initialization, otherwise we might inadvertently initialize the audit trail
        final Map<Long, T> atMap = auditables.stream()
                .collect( Collectors.toMap( a -> a.getAuditTrail().getId(), Function.identity() ) );

        Set<Class<? extends AuditEventType>> classes = getClassHierarchy( types, excludedTypes );

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
                .setParameterList( "trails", optimizeParameterList( atMap.keySet() ) )
                .setParameterList( "classes", classes ); // optimizing this one is unnecessary

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

    private <T extends Auditable> Map<T, AuditEvent> getLastEvents( Class<T> auditableClass, Class<? extends AuditEventType> types, @Nullable Collection<Class<? extends AuditEventType>> excludedTypes ) {
        StopWatch timer = StopWatch.createStarted();

        // using a treeset to avoid initialization of proxies
        Map<T, AuditEvent> result = new TreeMap<>( Comparator.comparing( Auditable::getId ) );

        Set<Class<? extends AuditEventType>> classes = getClassHierarchy( types, excludedTypes );
        if ( classes.isEmpty() ) {
            throw new IllegalArgumentException( "No classes found" );
        }

        String entityName = getSessionFactory().getClassMetadata( auditableClass ).getEntityName();
        //language=HQL
        final String queryString = "select a.id, ae from " + entityName + " a  "
                + "join a.auditTrail trail "
                + "join trail.events ae "
                + "join fetch ae.eventType et " // fetching here prevents a separate select query
                + "where type(et) in :classes "
                // annoyingly, Hibernate does not select the latest event when grouping by trail, so we have to fetch
                // them all
                + "group by trail, ae "
                // latest by date or ID to break ties
                + "order by ae.date desc, ae.id desc";

        Query queryObject = this.getSessionFactory().getCurrentSession()
                .createQuery( queryString )
                .setParameterList( "classes", classes ); // optimizing this one is unnecessary

        List<?> qr = queryObject.list();
        for ( Object o : qr ) {
            Object[] ar = ( Object[] ) o;
            Long t = ( Long ) ar[0];
            AuditEvent e = ( AuditEvent ) ar[1];
            // only retain the first one which is the latest (by date or ID)
            //noinspection unchecked
            result.putIfAbsent( ( T ) getSessionFactory().getCurrentSession().load( auditableClass, t ), e );
        }

        timer.stop();
        if ( timer.getTime() > 500 ) {
            AbstractDao.log.info( String.format( "Last event of type %s (closure: %s) retrieved for %d items in %d ms",
                    types.getName(), classes.stream().map( Class::getName ).collect( Collectors.joining( ", " ) ),
                    result.keySet().size(), timer.getTime() ) );
        }

        return result;
    }

    /**
     * Determine the full set of AuditEventTypes that are needed (that is, subclasses of the given class)
     *
     * @param type Class
     * @param excludedTypes a list of types to exclude
     * @return A List of class names, including the given type.
     */
    private Set<Class<? extends AuditEventType>> getClassHierarchy( Class<? extends AuditEventType> type, @Nullable Collection<Class<? extends AuditEventType>> excludedTypes ) {
        // how to determine subclasses? There is no way to do this but the hibernate way.
        ClassMetadata classMetadata = this.getSessionFactory().getClassMetadata( type );
        if ( classMetadata instanceof SingleTableEntityPersister ) {
            Set<Class<? extends AuditEventType>> classes = new HashSet<>();
            // this includes the superclass, fully qualified
            String[] subclasses = ( ( SingleTableEntityPersister ) classMetadata ).getSubclassClosure();
            for ( String className : subclasses ) {
                try {
                    //noinspection unchecked
                    classes.add( ( Class<? extends AuditEventType> ) Class.forName( className ) );
                } catch ( ClassNotFoundException e ) {
                    log.error( String.format( "Failed to find subclass %s of %s, it will not be included in the query.",
                            className, type.getName() ), e );
                }
            }
            // remove all the types we don't want
            if ( excludedTypes != null ) {
                for ( Class<? extends AuditEventType> excludedType : excludedTypes ) {
                    classes.removeAll( getClassHierarchy( excludedType, null ) );
                    if ( classes.isEmpty() ) {
                        throw new IllegalStateException( "No event types are left after applying exclusions to " + type.getName() + "." );
                    }
                }
            }
            return classes;
        } else {
            return Collections.singleton( type );
        }
    }
}