/*
 * The Gemma project.
 *
 * Copyright (c) 2006-2011 University of British Columbia
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
package ubic.gemma.persistence.service;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.EntityType;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Hibernate;
import org.hibernate.ObjectNotFoundException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.WrongClassException;
import org.hibernate.query.Query;
import org.springframework.util.Assert;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.persistence.hibernate.HibernateUtils;
import ubic.gemma.persistence.util.QueryUtils;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import javax.annotation.OverridingMethodsMustInvokeSuper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ubic.gemma.persistence.util.QueryUtils.batchParameterList;
import static ubic.gemma.persistence.util.QueryUtils.optimizeParameterList;

/**
 * AbstractDao finds the generic type at runtime and provides default
 * BaseDao implementations on top of {@link SessionFactory}.
 *
 * @author Anton, Nicolas
 */
public abstract class AbstractDao<T extends Identifiable> implements BaseDao<T> {

    protected final Log log = LogFactory.getLog( getClass() );

    private final Class<? extends T> elementClass;
    private final SessionFactory sessionFactory;
    private final String entityName;
    private final String identifierPropertyName;
    private final int batchSize;
    private final boolean useCursorFetchIfSupported;
    private final boolean isQueryStateless;

    protected AbstractDao( Class<? extends T> elementClass, SessionFactory sessionFactory ) {
        this.elementClass = elementClass;
        this.sessionFactory = sessionFactory;
        // A handful of synthetic DAOs (e.g. RawAndProcessedExpressionDataVectorDaoImpl) are typed on a
        // non-@Entity abstract supertype (BulkExpressionDataVector) so they can delegate to two real
        // sub-DAOs through a common return type. Such classes aren't in the JPA metamodel; fall back
        // to defaults rather than NPE. Every entity in Gemma's model uses "id" as the identifier
        // property anyway (Identifiable interface).
        EntityType<? extends T> entityType;
        try {
            entityType = sessionFactory.getMetamodel().entity( elementClass );
        } catch ( IllegalArgumentException e ) {
            entityType = null;
        }
        if ( entityType != null ) {
            this.entityName = entityType.getName();
            // single-ID entities only — composite IDs are not used in Gemma's model
            this.identifierPropertyName = entityType.getId( entityType.getIdType().getJavaType() ).getName();
            Assert.notNull( this.identifierPropertyName, String.format( "%s does not have an ID.", elementClass.getName() ) );
        } else {
            this.entityName = elementClass.getSimpleName();
            this.identifierPropertyName = "id";
        }
        this.batchSize = HibernateUtils.getBatchSize( elementClass, sessionFactory );
        this.useCursorFetchIfSupported = false;
        this.isQueryStateless = HibernateUtils.isStateless( elementClass, sessionFactory );
    }

    @Override
    public Class<? extends T> getElementClass() {
        return elementClass;
    }

    protected String getEntityName() {
        return this.entityName;
    }

    protected String getIdentifierPropertyName() {
        return this.identifierPropertyName;
    }

    @Override
    public Collection<T> create( Collection<T> entities ) {
        boolean isDebugEnabled = log.isDebugEnabled();
        StopWatch timer = isDebugEnabled ? StopWatch.createStarted() : null;
        Collection<T> results = new ArrayList<>( entities.size() );
        for ( T t : entities ) {
            results.add( this.create( t ) );
        }
        if ( isDebugEnabled ) {
            log.debug( String.format( "Created %d %s entities in %s ms.", results.size(), elementClass.getSimpleName(), timer.getTime( TimeUnit.MILLISECONDS ) ) );
        }
        return results;
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public T create( T entity ) {
        if ( entity.getId() != null ) {
            // Hibernate 6 cascade-via-persist + early-flush patterns (e.g. ExpressionPersister
            // walking a parent's collection where the parent was already persisted with cascade)
            // can leave an entity already in the session before the caller's explicit create()
            // call. Treat as a no-op rather than fail an Assert that was correct under earlier
            // Hibernate but is overly strict now. Trace-log to keep the diagnostic value of the
            // old assertion.
            if ( log.isTraceEnabled() ) {
                log.trace( String.format( "create() called on already-persistent %s; treating as no-op.", formatEntity( entity ) ) );
            }
            return entity;
        }
        sessionFactory.getCurrentSession().persist( entity );
        if ( log.isTraceEnabled() ) {
            log.trace( String.format( "Created %s.", formatEntity( entity ) ) );
        }
        return entity;
    }

    @Override
    public Collection<T> save( Collection<T> entities ) {
        boolean isDebugEnabled = log.isDebugEnabled();
        StopWatch timer = isDebugEnabled ? StopWatch.createStarted() : null;
        Collection<T> results = new ArrayList<>( entities.size() );
        for ( T entity : entities ) {
            results.add( this.save( entity ) );
        }
        if ( isDebugEnabled ) {
            log.debug( String.format( "Saved %d entities in %d ms.", entities.size(), timer.getTime( TimeUnit.MILLISECONDS ) ) );
        }
        return results;
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public T save( T entity ) {
        if ( entity.getId() == null ) {
            return create( entity );
        } else {
            //noinspection unchecked
            T result = ( T ) sessionFactory.getCurrentSession().merge( entity );
            if ( log.isTraceEnabled() ) {
                log.trace( String.format( "Updated %s.", formatEntity( entity ) ) );
            }
            return result;
        }
    }

    @Override
    public Collection<T> load( Collection<Long> ids ) {
        boolean isDebugEnabled = log.isDebugEnabled();
        if ( ids.isEmpty() ) {
            return Collections.emptyList();
        }
        StopWatch timer = isDebugEnabled ? StopWatch.createStarted() : null;

        List<T> results = new ArrayList<>( ids.size() );

        boolean sortById = false;
        Set<Long> unloadedIds = new HashSet<>();
        for ( Long id : ids ) {
            //noinspection unchecked
            T entity = ( T ) sessionFactory.getCurrentSession().getReference( elementClass, id );
            if ( Hibernate.isInitialized( entity ) ) {
                results.add( entity );
                sortById = true;
            } else {
                unloadedIds.add( id );
            }
        }

        if ( batchSize != -1 && unloadedIds.size() > batchSize ) {
            for ( Collection<Long> batch : batchParameterList( unloadedIds, batchSize ) ) {
                results.addAll( loadByIds( batch ) );
            }
        } else if ( !unloadedIds.isEmpty() ) {
            results.addAll( loadByIds( optimizeParameterList( unloadedIds ) ) );
        }

        if ( sortById ) {
            results.sort( Comparator.comparing( Identifiable::getId ) );
        }

        if ( isDebugEnabled ) {
            log.debug( String.format( "Loaded %d %s entities in %d ms.", results.size(), elementClass.getSimpleName(), timer.getTime( TimeUnit.MILLISECONDS ) ) );
        }

        return results;
    }

    private List<T> loadByIds( Collection<Long> ids ) {
        Session session = sessionFactory.getCurrentSession();
        CriteriaBuilder cb = session.getCriteriaBuilder();
        //noinspection unchecked
        CriteriaQuery<T> cq = ( CriteriaQuery<T> ) cb.createQuery( elementClass );
        //noinspection unchecked
        Root<T> root = ( Root<T> ) cq.from( elementClass );
        cq.select( root ).where( root.get( identifierPropertyName ).in( ids ) );
        return session.createQuery( cq ).getResultList();
    }

    @Override
    public T load( Long id ) {
        try {
            //noinspection unchecked
            T result = ( T ) sessionFactory.getCurrentSession().get( elementClass, id );
            if ( log.isTraceEnabled() ) {
                log.trace( String.format( "Loaded %s.", formatEntity( result ) ) );
            }
            return result;
        } catch ( WrongClassException e ) {
            log.warn( "Wrong class for ID " + id + ", will return null.", e );
            return null;
        }
    }

    @Override
    public Collection<T> loadAll() {
        boolean isDebugEnabled = log.isDebugEnabled();
        StopWatch timer = isDebugEnabled ? StopWatch.createStarted() : null;
        Session session = sessionFactory.getCurrentSession();
        CriteriaBuilder cb = session.getCriteriaBuilder();
        //noinspection unchecked
        CriteriaQuery<T> cq = ( CriteriaQuery<T> ) cb.createQuery( elementClass );
        //noinspection unchecked
        cq.select( ( Root<T> ) cq.from( elementClass ) );
        List<T> results = session.createQuery( cq ).getResultList();
        if ( isDebugEnabled ) {
            log.debug( String.format( "Loaded all (%d) %s entities in %d ms.", results.size(), elementClass.getSimpleName(), timer.getTime( TimeUnit.MILLISECONDS ) ) );
        }
        return results;
    }

    @Override
    public Collection<T> loadReference( Collection<Long> ids ) {
        Collection<T> results = ids.stream()
                .distinct().sorted()
                .map( this::loadReference )
                .collect( Collectors.toList() );
        if ( log.isDebugEnabled() ) {
            log.debug( String.format( "Loaded references to %d %s entities.", results.size(), elementClass.getSimpleName() ) );
        }
        return results;
    }

    @NonNull
    @Override
    public T loadReference( Long id ) {
        //noinspection unchecked
        T entity = ( T ) sessionFactory.getCurrentSession().getReference( elementClass, id );
        if ( log.isTraceEnabled() ) {
            log.trace( String.format( "Loaded reference to %s.", formatEntity( entity ) ) );
        }
        return entity;
    }

    @NonNull
    @Override
    public T reload( T entity ) {
        Assert.notNull( entity.getId(), "Cannot reload a transient entity." );
        Long id = entity.getId();
        //noinspection unchecked
        entity = ( T ) sessionFactory.getCurrentSession().get( elementClass, id );
        if ( entity == null ) {
            throw new ObjectNotFoundException( id, elementClass.getName() );
        }
        if ( log.isTraceEnabled() ) {
            log.trace( String.format( "Reloaded %s.", formatEntity( entity ) ) );
        }
        return entity;
    }

    @NonNull
    @Override
    public Collection<T> reload( Collection<T> entities ) {
        StopWatch timer = StopWatch.createStarted();
        List<T> results = entities.stream()
                .map( this::reload )
                .collect( Collectors.toList() );
        if ( log.isDebugEnabled() ) {
            log.debug( String.format( "Reloaded %d %s entities in %d ms.", results.size(), elementClass.getSimpleName(), timer.getTime( TimeUnit.MILLISECONDS ) ) );
        }
        return results;
    }

    @Override
    public long countAll() {
        Session session = sessionFactory.getCurrentSession();
        CriteriaBuilder cb = session.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery( Long.class );
        cq.select( cb.count( cq.from( elementClass ) ) );
        return session.createQuery( cq ).getSingleResult();
    }

    @Override
    public Stream<T> streamAll() {
        return streamAll( false );
    }

    @Override
    public Stream<T> streamAll( boolean createNewSession ) {
        return QueryUtils.createStream( getSessionFactory(),
                session -> {
                    CriteriaBuilder cb = session.getCriteriaBuilder();
                    //noinspection unchecked
                    CriteriaQuery<T> cq = ( CriteriaQuery<T> ) cb.createQuery( elementClass );
                    //noinspection unchecked
                    cq.select( ( Root<T> ) cq.from( elementClass ) );
                    //noinspection unchecked
                    return QueryUtils.stream( session.createQuery( cq ),
                            ( Class<T> ) elementClass,
                            batchSize,
                            useCursorFetchIfSupported,
                            isQueryStateless );
                }, createNewSession );
    }

    /**
     * Produce a stream over a {@link Query} with a new session if desired.
     */
    protected <U> Stream<U> streamQuery( Function<Session, Query<?>> queryCreator, Class<U> resultType, int fetchSize, boolean useCursorFetchIfSupported, boolean isStateless, boolean createNewSession ) {
        if ( createNewSession ) {
            Session session = openSession();
            try {
                return QueryUtils.stream( queryCreator.apply( session ), resultType, fetchSize, useCursorFetchIfSupported, isStateless )
                        .onClose( session::close );
            } catch ( Exception e ) {
                session.close();
                throw e;
            }
        } else {
            return QueryUtils.stream( queryCreator.apply( sessionFactory.getCurrentSession() ), resultType, fetchSize, useCursorFetchIfSupported, isStateless );
        }
    }

    /**
     * Open a session that inherits the current session's properties.
     */
    private Session openSession() {
        Session currentSession = sessionFactory.getCurrentSession();
        Session session = sessionFactory.openSession();
        session.setDefaultReadOnly( currentSession.isDefaultReadOnly() );
        session.setCacheMode( currentSession.getCacheMode() );
        session.setHibernateFlushMode( currentSession.getHibernateFlushMode() );
        return session;
    }

    @Override
    public void remove( Collection<T> entities ) {
        StopWatch timer = StopWatch.createStarted();
        for ( T e : entities ) {
            this.remove( e );
        }
        if ( log.isDebugEnabled() ) {
            log.debug( String.format( "Removed %d entities in %d ms.", entities.size(), timer.getTime( TimeUnit.MILLISECONDS ) ) );
        }
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void remove( T entity ) {
        Assert.notNull( entity.getId(), "Cannot delete a transient entity." );
        sessionFactory.getCurrentSession().remove( entity );
        if ( log.isTraceEnabled() ) {
            log.trace( String.format( "Removed %s.", formatEntity( entity ) ) );
        }
    }

    @Override
    public void update( Collection<T> entities ) {
        boolean isDebugEnabled = log.isDebugEnabled();
        StopWatch timer = isDebugEnabled ? StopWatch.createStarted() : null;
        for ( T entity : entities ) {
            this.update( entity );
        }
        if ( isDebugEnabled ) {
            log.debug( String.format( "Updated %d %s entities in %d ms.", entities.size(), elementClass.getSimpleName(), timer.getTime( TimeUnit.MILLISECONDS ) ) );
        }
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void update( T entity ) {
        Assert.notNull( entity.getId(), "Cannot update a transient entity." );
        sessionFactory.getCurrentSession().merge( entity );
        if ( log.isTraceEnabled() ) {
            log.trace( String.format( "Updated %s.", formatEntity( entity ) ) );
        }
    }

    @Override
    public T find( T entity ) {
        if ( entity.getId() != null ) {
            return this.load( entity.getId() );
        } else {
            return null;
        }
    }

    @Override
    public T findOrCreate( T entity ) {
        T found = this.find( entity );
        return found != null ? found : this.create( entity );
    }

    protected final SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    protected final int getBatchSize() {
        return batchSize;
    }

    @Nullable
    protected T findOneByProperty( String propertyName, Object propertyValue ) {
        Session session = sessionFactory.getCurrentSession();
        CriteriaBuilder cb = session.getCriteriaBuilder();
        //noinspection unchecked
        CriteriaQuery<T> cq = ( CriteriaQuery<T> ) cb.createQuery( elementClass );
        //noinspection unchecked
        Root<T> root = ( Root<T> ) cq.from( elementClass );
        cq.select( root ).where( cb.equal( root.get( propertyName ), propertyValue ) );
        return session.createQuery( cq ).uniqueResult();
    }

    @Nullable
    protected Long findIdByProperty( String propertyName, Object propertyValue ) {
        Session session = sessionFactory.getCurrentSession();
        CriteriaBuilder cb = session.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery( Long.class );
        Root<?> root = cq.from( elementClass );
        cq.select( root.get( identifierPropertyName ).as( Long.class ) )
                .where( cb.equal( root.get( propertyName ), propertyValue ) );
        return session.createQuery( cq ).uniqueResult();
    }

    protected List<T> findByProperty( String propertyName, Object propertyValue ) {
        Session session = sessionFactory.getCurrentSession();
        CriteriaBuilder cb = session.getCriteriaBuilder();
        //noinspection unchecked
        CriteriaQuery<T> cq = ( CriteriaQuery<T> ) cb.createQuery( elementClass );
        //noinspection unchecked
        Root<T> root = ( Root<T> ) cq.from( elementClass );
        cq.select( root ).where( cb.equal( root.get( propertyName ), propertyValue ) );
        return session.createQuery( cq ).getResultList();
    }

    protected List<T> findByPropertyIn( String propertyName, Collection<?> propertyValues ) {
        if ( propertyValues.isEmpty() ) {
            return Collections.emptyList();
        }
        Session session = sessionFactory.getCurrentSession();
        CriteriaBuilder cb = session.getCriteriaBuilder();
        //noinspection unchecked
        CriteriaQuery<T> cq = ( CriteriaQuery<T> ) cb.createQuery( elementClass );
        //noinspection unchecked
        Root<T> root = ( Root<T> ) cq.from( elementClass );
        cq.select( root ).where( root.get( propertyName ).in( propertyValues ) );
        return session.createQuery( cq ).getResultList();
    }

    private String formatEntity( @Nullable T entity ) {
        if ( entity == null ) {
            return String.format( "null %s", elementClass.getSimpleName() );
        } else if ( entity.getId() == null ) {
            return String.format( "transient %s entity", elementClass.getSimpleName() );
        } else if ( sessionFactory.getCurrentSession().contains( entity ) ) {
            return String.format( "persistent %s entity with ID %d", elementClass.getSimpleName(), entity.getId() );
        } else {
            return String.format( "detached %s entity with ID %d", elementClass.getSimpleName(), entity.getId() );
        }
    }
}
