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

import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.*;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.metadata.ClassMetadata;
import org.springframework.util.Assert;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.persistence.hibernate.HibernateUtils;
import ubic.gemma.persistence.util.QueryUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.OverridingMethodsMustInvokeSuper;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;
import static ubic.gemma.persistence.util.QueryUtils.batchParameterList;
import static ubic.gemma.persistence.util.QueryUtils.optimizeParameterList;

/**
 * AbstractDao can find the generic type at runtime and simplify the code implementation of the BaseDao interface
 *
 * @author Anton, Nicolas
 */
public abstract class AbstractDao<T extends Identifiable> implements BaseDao<T> {

    protected final Log log = LogFactory.getLog( getClass() );

    private final Class<? extends T> elementClass;
    private final SessionFactory sessionFactory;
    private final ClassMetadata classMetadata;
    private final int batchSize;

    protected AbstractDao( Class<? extends T> elementClass, SessionFactory sessionFactory ) {
        this( elementClass, sessionFactory, requireNonNull( sessionFactory.getClassMetadata( elementClass ),
                String.format( "%s is missing from Hibernate mapping.", elementClass.getName() ) ) );
    }

    /**
     * @param classMetadata the class metadata to use to retrieve information about {@link T}
     */
    protected AbstractDao( Class<? extends T> elementClass, SessionFactory sessionFactory, ClassMetadata classMetadata ) {
        Assert.isTrue( elementClass.isAssignableFrom( ( Class<?> ) classMetadata.getMappedClass() ),
                String.format( "The mapped class must be assignable from %s.", elementClass.getName() ) );
        Assert.notNull( classMetadata.getIdentifierPropertyName(), String.format( "%s does not have a ID.", elementClass.getName() ) );
        this.elementClass = elementClass;
        this.sessionFactory = sessionFactory;
        this.classMetadata = classMetadata;
        this.batchSize = HibernateUtils.getBatchSize( sessionFactory, classMetadata );
    }

    @Override
    public Class<? extends T> getElementClass() {
        return elementClass;
    }

    @Override
    public String getIdentifierPropertyName() {
        return this.classMetadata.getIdentifierPropertyName();
    }

    @Override
    public Collection<T> create( Collection<T> entities ) {
        boolean isDebugEnabled = log.isDebugEnabled();
        StopWatch timer;
        if ( isDebugEnabled ) {
            timer = StopWatch.createStarted();
        } else {
            timer = null;
        }
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
        Assert.isNull( entity.getId(), "Cannot create an already persistent entity." );
        sessionFactory.getCurrentSession().persist( entity );
        if ( log.isTraceEnabled() ) {
            log.trace( String.format( "Created %s.", formatEntity( entity ) ) );
        }
        return entity;
    }

    @Override
    public Collection<T> save( Collection<T> entities ) {
        boolean isDebugEnabled = log.isDebugEnabled();
        StopWatch timer;
        if ( isDebugEnabled ) {
            timer = StopWatch.createStarted();
        } else {
            timer = null;
        }
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

    /**
     * This implementation is temporary and attempts to best replicate the behaviour of loading entities by multiple IDs
     * introduced in Hibernate 5. <a href="https://thorben-janssen.com/fetch-multiple-entities-id-hibernate/">Read more about this</a>.
     */
    @Override
    public Collection<T> load( Collection<Long> ids ) {
        boolean isDebugEnabled = log.isDebugEnabled();

        if ( ids.isEmpty() ) {
            if ( isDebugEnabled ) {
                log.trace( String.format( "Loading %s with an empty collection of IDs, returning an empty collection.", elementClass.getSimpleName() ) );
            }
            return Collections.emptyList();
        }
        StopWatch timer;
        if ( isDebugEnabled ) {
            timer = StopWatch.createStarted();
        } else {
            timer = null;
        }
        String idPropertyName = classMetadata.getIdentifierPropertyName();

        List<T> results = new ArrayList<>( ids.size() );

        boolean sortById = false;
        Set<Long> unloadedIds = new HashSet<>();
        for ( Long id : ids ) {
            //noinspection unchecked
            T entity = ( T ) sessionFactory.getCurrentSession().load( elementClass, id );
            if ( Hibernate.isInitialized( entity ) ) {
                results.add( entity );
                sortById = true;
            } else {
                unloadedIds.add( id );
            }
        }

        if ( batchSize != -1 && unloadedIds.size() > batchSize ) {
            for ( Collection<Long> batch : batchParameterList( unloadedIds, batchSize ) ) {
                //noinspection unchecked
                results.addAll( sessionFactory.getCurrentSession()
                        .createCriteria( elementClass )
                        .add( Restrictions.in( idPropertyName, batch ) )
                        .list() );
            }
        } else if ( !unloadedIds.isEmpty() ) {
            //noinspection unchecked
            results.addAll( sessionFactory.getCurrentSession()
                    .createCriteria( elementClass )
                    .add( Restrictions.in( idPropertyName, optimizeParameterList( unloadedIds ) ) )
                    .list() );
        }

        if ( sortById ) {
            results.sort( Comparator.comparing( Identifiable::getId ) );
        }

        if ( isDebugEnabled ) {
            log.debug( String.format( "Loaded %d %s entities in %d ms.", results.size(), elementClass.getSimpleName(), timer.getTime( TimeUnit.MILLISECONDS ) ) );
        }

        return results;
    }

    @Override
    public T load( Long id ) {
        try {
            // Don't use 'load' because if the object doesn't exist you can get an invalid proxy.
            //noinspection unchecked
            T result = ( T ) sessionFactory.getCurrentSession().get( elementClass, id );
            if ( log.isTraceEnabled() ) {
                log.trace( String.format( String.format( "Loaded %s.", formatEntity( result ) ) ) );
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
        StopWatch timer;
        if ( isDebugEnabled ) {
            timer = StopWatch.createStarted();
        } else {
            timer = null;
        }
        //noinspection unchecked
        Collection<T> results = sessionFactory.getCurrentSession().createCriteria( elementClass ).list();
        if ( isDebugEnabled ) {
            log.debug( String.format( "Loaded all (%d) %s entities in %d ms.", results.size(), elementClass.getSimpleName(), timer.getTime( TimeUnit.MILLISECONDS ) ) );
        }
        return results;
    }

    @Override
    public Collection<T> loadReference( Collection<Long> ids ) {
        Collection<T> results = ids.stream()
                .distinct().sorted() // this will make the output appear similar to load(Collection)
                .map( this::loadReference )
                .collect( Collectors.toList() ); // no HashSet here because otherwise proxies would get initialized
        if ( log.isDebugEnabled() ) {
            log.debug( String.format( "Loaded references to %d %s entities.", results.size(), elementClass.getSimpleName() ) );
        }
        return results;
    }

    @Nonnull
    @Override
    public T loadReference( Long id ) {
        //noinspection unchecked
        T entity = ( T ) sessionFactory.getCurrentSession().load( elementClass, id );
        if ( log.isTraceEnabled() ) {
            log.trace( String.format( "Loaded reference to %s.", formatEntity( entity ) ) );
        }
        return entity;
    }

    @Nonnull
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

    @Nonnull
    @Override
    public Collection<T> reload( Collection<T> entities ) {
        // TODO: implement batch reloading like for #load(Collection)
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
        return ( Long ) sessionFactory.getCurrentSession().createCriteria( elementClass )
                .setProjection( Projections.rowCount() )
                .uniqueResult();
    }

    @Override
    public Stream<T> streamAll() {
        return streamAll( false );
    }

    @Override
    public Stream<T> streamAll( boolean createNewSession ) {
        return QueryUtils.createStream( getSessionFactory(), session -> QueryUtils.stream( session.createCriteria( elementClass ), batchSize ), createNewSession );
    }

    /**
     * Produce a stream over a {@link Query} with a new session if desired.
     * @param createNewSession if true, a new session is created and will be closed when the stream is closed. Be
     *                         extremely careful with the resulting stream. Use a try-with-resources block to ensure
     *                         the session is closed properly.
     * @see QueryUtils#stream(Query, int)
     */
    protected <U> Stream<U> streamQuery( Function<Session, Query> queryCreator, boolean createNewSession ) {
        if ( createNewSession ) {
            Session session = openSession();
            try {
                return QueryUtils.<U>stream( queryCreator.apply( session ), batchSize )
                        .onClose( session::close );
            } catch ( Exception e ) {
                session.close();
                throw e;
            }
        } else {
            return QueryUtils.stream( queryCreator.apply( sessionFactory.getCurrentSession() ), batchSize );
        }
    }

    /**
     * Open a session that inherits the current session's properties.
     * <p>
     * Be extremely careful when opening a new session because it is not managed by the session factory and will not be
     * closed automatically.
     */
    private Session openSession() {
        Session currentSession = sessionFactory.getCurrentSession();
        Session session = sessionFactory.openSession();
        session.setDefaultReadOnly( currentSession.isDefaultReadOnly() );
        session.setCacheMode( currentSession.getCacheMode() );
        session.setFlushMode( currentSession.getFlushMode() );
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
    public void remove( Long id ) {
        T entity = this.load( id );
        if ( entity != null ) {
            this.remove( entity );
        } else if ( log.isDebugEnabled() ) {
            log.debug( String.format( "No %s entity with ID %d, no need to remove anything.", elementClass.getSimpleName(), id ) );
        }
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void remove( T entity ) {
        Assert.notNull( entity.getId(), "Cannot delete a transient entity." );
        sessionFactory.getCurrentSession().delete( entity );
        if ( log.isTraceEnabled() ) {
            log.trace( String.format( "Removed %s.", formatEntity( entity ) ) );
        }
    }

    @Override
    public void update( Collection<T> entities ) {
        boolean isDebugEnabled = log.isDebugEnabled();
        StopWatch timer;
        if ( isDebugEnabled ) {
            timer = StopWatch.createStarted();
        } else {
            timer = null;
        }
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
        sessionFactory.getCurrentSession().update( entity );
        if ( log.isTraceEnabled() ) {
            log.trace( String.format( "Updated %s.", formatEntity( entity ) ) );
        }
    }

    @Override
    public T find( T entity ) {
        if ( entity.getId() != null ) {
            return this.load( entity.getId() );
        } else {
            if ( log.isTraceEnabled() ) {
                log.trace( String.format( "No persistent entity found for %s, returning null.", formatEntity( entity ) ) );
            }
            return null;
        }
    }

    @Override
    public T findOrCreate( T entity ) {
        T found = this.find( entity );
        if ( found != null ) {
            return found;
        } else {
            if ( log.isTraceEnabled() ) {
                log.trace( String.format( "No persistent entity found for %s, creating a new one...", formatEntity( entity ) ) );
            }
            return this.create( entity );
        }
    }

    protected final SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    protected final int getBatchSize() {
        return batchSize;
    }

    /**
     * Retrieve one entity whose given property matches the given value.
     * <p>
     * Note: the property should have a unique index, otherwise a {@link org.hibernate.NonUniqueResultException} will be
     * raised.
     *
     * @param  propertyName  the name of property to be matched.
     * @param  propertyValue the value to look for.
     * @return an entity whose property matched the given value
     */
    @Nullable
    protected T findOneByProperty( String propertyName, Object propertyValue ) {
        //noinspection unchecked
        return ( T ) sessionFactory.getCurrentSession()
                .createCriteria( this.elementClass )
                .add( Restrictions.eq( propertyName, propertyValue ) )
                .uniqueResult();
    }

    /**
     * Does a search on given property and its value.
     *
     * @param  propertyName  the name of property to be matched.
     * @param  propertyValue the value to look for.
     * @return an entity whose property first matched the given value.
     */
    protected List<T> findByProperty( String propertyName, Object propertyValue ) {
        //noinspection unchecked
        return sessionFactory.getCurrentSession()
                .createCriteria( this.elementClass )
                .add( Restrictions.eq( propertyName, propertyValue ) )
                .list();
    }

    /**
     * Perform a search on a given property and all its possible values.
     */
    protected List<T> findByPropertyIn( String propertyName, Collection<?> propertyValues ) {
        if ( propertyValues.isEmpty() ) {
            return Collections.emptyList();
        }
        //noinspection unchecked
        return sessionFactory.getCurrentSession()
                .createCriteria( this.elementClass )
                .add( Restrictions.in( propertyName, propertyValues ) )
                .list();
    }

    private String formatEntity( @Nullable T entity ) {
        if ( entity == null ) {
            return String.format( "null %s", elementClass.getSimpleName() );
        } else if ( entity.getId() == null ) {
            return String.format( String.format( "transient %s entity", elementClass.getSimpleName() ) );
        } else if ( sessionFactory.getCurrentSession().contains( entity ) ) {
            return String.format( String.format( "persistent %s entity with ID %d", elementClass.getSimpleName(), entity.getId() ) );
        } else {
            return String.format( String.format( "detached %s entity with ID %d", elementClass.getSimpleName(), entity.getId() ) );
        }
    }
}
