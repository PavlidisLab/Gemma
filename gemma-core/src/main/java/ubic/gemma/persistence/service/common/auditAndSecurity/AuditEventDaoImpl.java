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
import org.hibernate.query.Query;
import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.util.Assert;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.Auditable;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;
import ubic.gemma.persistence.service.AbstractDao;
import ubic.gemma.persistence.util.Cursor;
import ubic.gemma.persistence.util.CursorPage;
import ubic.gemma.persistence.util.IdentifiableUtils;
import ubic.gemma.persistence.util.Sort;

import org.springframework.lang.Nullable;
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
                .createQuery( "select e from AuditTrail t join t.events e where t = :at order by e.date, e.id " )
                .setParameter( "at", auditable.getAuditTrail() )
                .list();
    }

    @Override
    public List<AuditEvent> getEventsWithType( Auditable auditable ) {
        Assert.notNull( auditable.getAuditTrail(), "Auditable did not have an audit trail: " + auditable );
        Assert.notNull( auditable.getAuditTrail().getId(), "Auditable did not have a persistent audit trail: " + auditable );
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession()
                .createQuery( "select e from AuditTrail t join t.events e where t = :at and e.eventType is not null order by e.date, e.id " )
                .setParameter( "at", auditable.getAuditTrail() )
                .list();
    }

    @Override
    public CursorPage<AuditEvent> getEventsByCursor( Auditable auditable, @org.springframework.lang.Nullable Cursor cursor, int limit ) {
        // Step 1q: keyset pagination over /datasets/{dataset}/auditEvents (and any
        // future entity-typed audit-event listings). Mirrors getEvents()'s scope
        // (events on the auditable's AuditTrail) but switches the sort from the
        // legacy (date, id) ordering to a single-component +id sort so the cursor
        // DAO restriction (id-only keyset, pending the phase-B index audit) is
        // honoured. AuditEvents are append-only over time, so id-asc tracks
        // date-asc in practice.
        Assert.notNull( auditable.getAuditTrail(), "Auditable did not have an audit trail: " + auditable );
        Assert.notNull( auditable.getAuditTrail().getId(), "Auditable did not have a persistent audit trail: " + auditable );
        if ( limit <= 0 ) {
            throw new IllegalArgumentException( "Cursor page limit must be > 0." );
        }
        final String expectedSortSpec = "+id";
        if ( cursor != null ) {
            if ( !expectedSortSpec.equals( cursor.getSortSpec() ) ) {
                throw new IllegalArgumentException( "Cursor sort spec '" + cursor.getSortSpec()
                        + "' does not match the requested sort '" + expectedSortSpec + "'." );
            }
            Object[] key = cursor.getKeyTuple();
            if ( key.length != 1 ) {
                throw new IllegalArgumentException( "Cursor key tuple must have exactly 1 component for sort '"
                        + expectedSortSpec + "'; got " + key.length + "." );
            }
        }
        boolean backward = cursor != null && cursor.getDirection() == Cursor.Direction.BACKWARD;
        Long lastSeenId = null;
        if ( cursor != null ) {
            try {
                lastSeenId = ( ( Number ) cursor.getKeyTuple()[0] ).longValue();
            } catch ( ClassCastException e ) {
                throw new IllegalArgumentException( "Cursor key component must be numeric for sort '"
                        + expectedSortSpec + "'.", e );
            }
        }

        StringBuilder hql = new StringBuilder( "select e from AuditTrail t join t.events e where t = :at" );
        if ( lastSeenId != null ) {
            // forward: id > x; backward: id < x (id-asc client-visible order). When backward,
            // we reverse the order in the driver query and reverse the returned page below.
            hql.append( backward ? " and e.id < :lastSeenId" : " and e.id > :lastSeenId" );
        }
        // backward cursor: order by id DESC in the driver query, reverse the returned page.
        hql.append( backward ? " order by e.id desc" : " order by e.id asc" );

        org.hibernate.query.Query<?> q = this.getSessionFactory().getCurrentSession().createQuery( hql.toString() )
                .setParameter( "at", auditable.getAuditTrail() );
        if ( lastSeenId != null ) {
            q.setParameter( "lastSeenId", lastSeenId );
        }
        q.setMaxResults( limit + 1 );
        //noinspection unchecked
        List<AuditEvent> data = ( List<AuditEvent> ) q.list();

        boolean hasMore = data.size() > limit;
        if ( hasMore ) {
            data = new ArrayList<>( data.subList( 0, limit ) );
        } else {
            data = new ArrayList<>( data );
        }
        if ( backward ) {
            Collections.reverse( data );
        }

        String nextCursor = null;
        String prevCursor = null;
        if ( !data.isEmpty() ) {
            AuditEvent last = data.get( data.size() - 1 );
            AuditEvent first = data.get( 0 );
            // emit nextCursor only when there's another page in the forward direction
            if ( backward || hasMore ) {
                nextCursor = new Cursor( expectedSortSpec, new Object[] { last.getId() }, Cursor.Direction.FORWARD ).encode();
            }
            // emit prevCursor whenever we have a cursor (at least one page is behind us)
            if ( cursor != null ) {
                prevCursor = new Cursor( expectedSortSpec, new Object[] { first.getId() }, Cursor.Direction.BACKWARD ).encode();
            }
        }

        Sort idSort = Sort.by( null, "id", Sort.Direction.ASC, Sort.NullMode.LAST, "id" );
        return new CursorPage<>( data, idSort, limit, nextCursor, prevCursor, null );
    }

    @Nullable
    @Override
    public AuditEvent getLastEvent( Auditable auditable ) {
        return getLastEvents( Collections.singleton( auditable ), null, null ).get( auditable );
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
        String entityName = ubic.gemma.persistence.hibernate.HibernateUtils.getEntityName( getSessionFactory(), auditableClass );
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
        String entityName = ubic.gemma.persistence.hibernate.HibernateUtils.getEntityName( getSessionFactory(), auditableClass );
        // "Updated" = received any typed AuditEvent in the window. We intentionally do NOT filter on
        // ae.action='U' (generic auto-UPDATE) because that machinery is being retired in Phase C of the
        // audit migration (see AUDIT_SYSTEM_AUDIT.md Section 5, risk #1). Filtering on
        // ae.eventType IS NOT NULL keeps semantic updates (typed events emitted explicitly by services)
        // and naturally excludes generic auto-UPDATE rows (eventType=null) and creation events
        // (action='C', also eventType=null). Output shape is unchanged: one auditable per row.
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession()
                .createQuery( "select adb from " + entityName + " adb "
                        + "join adb.auditTrail atr "
                        + "join atr.events as ae "
                        + "where ae.date >= :date and ae.eventType is not null "
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

    private <T extends Auditable> Map<T, AuditEvent> getLastEvents( final Collection<T> auditables, @Nullable Class<? extends AuditEventType> types, @Nullable Collection<Class<? extends AuditEventType>> excludedTypes ) {
        if ( auditables.isEmpty() ) {
            return Collections.emptyMap();
        }

        StopWatch timer = StopWatch.createStarted();

        Map<T, AuditEvent> result = new HashMap<>( auditables.size() );

        // getId() does not require proxy initialization, otherwise we might inadvertently initialize the audit trail
        final Map<Long, T> atMap = auditables.stream()
                .collect( Collectors.toMap( a -> a.getAuditTrail().getId(), Function.identity() ) );

        Set<Class<? extends AuditEventType>> classes;
        if ( types != null ) {
            classes = getClassHierarchy( types, excludedTypes );
        } else {
            classes = null;
        }

        //language=HQL
        final String queryString = "select trail.id, ae from AuditTrail trail "
                + "join trail.events ae "
                + "join fetch ae.eventType et " // fetching here prevents a separate select query
                + "where trail.id in :trails "
                + ( classes != null ? "and type(et) in :classes " : "" )
                // annoyingly, Hibernate does not select the latest event when grouping by trail, so we have to fetch
                // them all
                + "group by trail, ae "
                // latest by date or ID to break ties
                + "order by ae.date desc, ae.id desc";

        Query queryObject = this.getSessionFactory().getCurrentSession()
                .createQuery( queryString )
                .setParameterList( "trails", optimizeParameterList( atMap.keySet() ) );

        if ( classes != null ) {
            queryObject.setParameterList( "classes", classes ); // optimizing this one is unnecessary
        }

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
            log.info( String.format( "Last event%s retrieved for %d items in %d ms",
                    types != null ? " of type " + types.getName() + "(closure: " + classes.stream().map( Class::getName ).collect( Collectors.joining( ", " ) ) + ")" : "",
                    auditables.size(), timer.getTime() ) );
        }

        return result;
    }

    private <T extends Auditable> Map<T, AuditEvent> getLastEvents( Class<T> auditableClass, Class<? extends AuditEventType> types, @Nullable Collection<Class<? extends AuditEventType>> excludedTypes ) {
        StopWatch timer = StopWatch.createStarted();

        // using a treeset to avoid initialization of proxies
        Map<T, AuditEvent> result = new TreeMap<>( Comparator.comparing( IdentifiableUtils::getRequiredId ) );

        Set<Class<? extends AuditEventType>> classes = getClassHierarchy( types, excludedTypes );
        if ( classes.isEmpty() ) {
            throw new IllegalArgumentException( "No classes found" );
        }

        String entityName = ubic.gemma.persistence.hibernate.HibernateUtils.getEntityName( getSessionFactory(), auditableClass );

        // Whole-corpus path (e.g. ArrayDesignReportServiceImpl, ExpressionExperimentReportServiceImpl
        // dashboards, scheduled stats). The legacy form pulled every matching event into the JVM
        // and reduced to per-trail max in Java — 1.5 M event rows transferred for ~25k EEs in the
        // worst case (PERF_PROBE_REPORT.md probe 3b, 8.7 s wall). The rewrite issues a SQL-side
        // aggregate that returns exactly one (auditable.id, winning event.id) tuple per trail —
        // the date-max followed by id-max-on-tied-date pair — then a second query loads the
        // winning events with eventType joined. Tie-breaker on equal timestamps: MAX(id) wins (a
        // bulk-insert can give two events on the same trail the same DATETIME(3) and the legacy
        // Java-side `order by date desc, id desc` already picked the larger id; this preserves
        // that semantics).
        //
        // Step 1: per-trail aggregate. Returns (auditable.id, winning event.id) — one row per
        // matching trail. Uses a correlated MAX(date) subquery and breaks ties by MAX(id).
        //language=HQL
        final String idsQuery = "select a.id, max(ae.id) from " + entityName + " a "
                + "join a.auditTrail trail "
                + "join trail.events ae "
                + "join ae.eventType et "
                + "where type(et) in :classes "
                + "and ae.date = ( "
                + "  select max(ae2.date) from " + entityName + " a2 "
                + "  join a2.auditTrail trail2 "
                + "  join trail2.events ae2 "
                + "  join ae2.eventType et2 "
                + "  where a2 = a and type(et2) in :classes "
                + ") "
                + "group by a.id";

        @SuppressWarnings("unchecked")
        List<Object[]> idPairs = this.getSessionFactory().getCurrentSession()
                .createQuery( idsQuery )
                .setParameterList( "classes", classes )
                .list();

        if ( idPairs.isEmpty() ) {
            return result;
        }

        // Step 2: hydrate the winning events with eventType joined. One row per auditable.
        List<Long> eventIds = new ArrayList<>( idPairs.size() );
        for ( Object[] row : idPairs ) {
            eventIds.add( ( Long ) row[1] );
        }

        //language=HQL
        final String eventsQuery = "select ae from AuditEvent ae "
                + "join fetch ae.eventType "
                + "where ae.id in :ids";

        @SuppressWarnings("unchecked")
        List<AuditEvent> events = this.getSessionFactory().getCurrentSession()
                .createQuery( eventsQuery )
                .setParameterList( "ids", optimizeParameterList( eventIds ) )
                .list();

        Map<Long, AuditEvent> eventById = new HashMap<>( events.size() );
        for ( AuditEvent ae : events ) {
            eventById.put( ae.getId(), ae );
        }

        for ( Object[] row : idPairs ) {
            Long auditableId = ( Long ) row[0];
            Long winningEventId = ( Long ) row[1];
            AuditEvent ae = eventById.get( winningEventId );
            if ( ae == null ) {
                continue; // shouldn't happen; defensive
            }
            //noinspection unchecked
            result.put( ( T ) getSessionFactory().getCurrentSession().getReference( auditableClass, auditableId ), ae );
        }

        timer.stop();
        if ( timer.getTime() > 500 ) {
            log.info( String.format( "Last event of type %s (closure: %s) retrieved for %d items in %d ms",
                    types.getName(), classes.stream().map( Class::getName ).collect( Collectors.joining( ", " ) ),
                    result.keySet().size(), timer.getTime() ) );
        }

        return result;
    }

    /**
     * Determine the full set of AuditEventTypes that are needed (that is, subclasses of the given class)
     *
     * @param type          Class
     * @param excludedTypes a list of types to exclude
     * @return A List of class names, including the given type.
     */
    private Set<Class<? extends AuditEventType>> getClassHierarchy( Class<? extends AuditEventType> type, @Nullable Collection<Class<? extends AuditEventType>> excludedTypes ) {
        // Hibernate 6 path: walk MappingMetamodel for the subclass entity names.
        EntityPersister persister;
        try {
            persister = ( ( SessionFactoryImplementor ) getSessionFactory() ).getMappingMetamodel()
                    .getEntityDescriptor( type.getName() );
        } catch ( IllegalArgumentException e ) {
            persister = null;
        }
        if ( persister != null ) {
            Set<Class<? extends AuditEventType>> classes = new HashSet<>();
            for ( String className : persister.getEntityMetamodel().getSubclassEntityNames() ) {
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