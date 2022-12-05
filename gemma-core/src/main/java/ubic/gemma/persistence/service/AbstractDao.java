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
import org.hibernate.FlushMode;
import org.hibernate.LockOptions;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.metadata.ClassMetadata;
import org.springframework.util.Assert;
import ubic.gemma.model.common.Identifiable;

import javax.annotation.Nullable;
import javax.annotation.OverridingMethodsMustInvokeSuper;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * AbstractDao can find the generic type at runtime and simplify the code implementation of the BaseDao interface
 *
 * @author Anton, Nicolas
 */
public abstract class AbstractDao<T extends Identifiable> implements BaseDao<T> {

    protected static final Log log = LogFactory.getLog( AbstractDao.class );

    /**
     * Default batch size to reach before flushing and clearing the Hibernate session.
     * <p>
     * You should use {@link #AbstractDao(Class, SessionFactory, int)} to adjust this value to an optimal one for the
     * DAO. Large model should have a relatively small batch size to reduce memory usage.
     * <p>
     * See <a href="https://docs.jboss.org/hibernate/core/3.6/reference/en-US/html/batch.html">Chapter 15. Batch processing</a>
     * for more details.
     */
    private static final int DEFAULT_BATCH_SIZE = 100;

    protected final Class<? extends T> elementClass;

    private final SessionFactory sessionFactory;
    private final int batchSize;
    private final ClassMetadata classMetadata;

    protected AbstractDao( Class<? extends T> elementClass, SessionFactory sessionFactory ) {
        this( elementClass, sessionFactory, DEFAULT_BATCH_SIZE );
    }

    /**
     * @param batchSize a strictly positive batch size for creating, updating or deleting collection of entities. Use
     *                  {@link Integer#MAX_VALUE} to effectively disable batching and '1' to flush changes right away.
     */
    protected AbstractDao( Class<? extends T> elementClass, SessionFactory sessionFactory, int batchSize ) {
        Assert.notNull( sessionFactory.getClassMetadata( elementClass ), String.format( "%s is missing from Hibernate mapping.", elementClass.getName() ) );
        Assert.isTrue( batchSize >= 1, "Batch size must be greater or equal to 1." );
        this.sessionFactory = sessionFactory;
        this.elementClass = elementClass;
        this.batchSize = batchSize;
        this.classMetadata = sessionFactory.getClassMetadata( elementClass );
    }

    @Override
    public Collection<T> create( Collection<T> entities ) {
        StopWatch stopWatch = StopWatch.createStarted();
        warnIfBatchingIsNotAdvisable( "remove", entities );
        Collection<T> results = new ArrayList<>( entities.size() );
        int i = 0;
        for ( T t : entities ) {
            results.add( this.create( t ) );
            if ( ++i % batchSize == 0 && isBatchingAdvisable() ) {
                flushAndClear();
                AbstractDao.log.debug( String.format( "Flushed and cleared after creating %d/%d %s entities.", i, entities.size(), elementClass ) );
            }
        }
        AbstractDao.log.debug( String.format( "Created %d %s entities in %s ms.", results.size(), elementClass.getSimpleName(), stopWatch.getTime( TimeUnit.MILLISECONDS ) ) );
        return results;
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public T create( T entity ) {
        this.getSessionFactory().getCurrentSession().persist( entity );
        AbstractDao.log.trace( String.format( "Created %s.", formatEntity( entity ) ) );
        return entity;
    }

    @Override
    public Collection<T> save( Collection<T> entities ) {
        StopWatch timer = StopWatch.createStarted();
        Collection<T> results = new ArrayList<>( entities.size() );
        int i = 0;
        for ( T entity : entities ) {
            results.add( this.save( entity ) );
            if ( ++i % batchSize == 0 && isBatchingAdvisable() ) {
                flushAndClear();
                AbstractDao.log.trace( String.format( "Flushed and cleared after saving %d/%d %s entities.", i, entities.size(), elementClass ) );
            }
        }
        AbstractDao.log.debug( String.format( "Saved %d entities in %d ms.", entities.size(), timer.getTime( TimeUnit.MILLISECONDS ) ) );
        return results;
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public T save( T entity ) {
        if ( entity.getId() == null ) {
            getSessionFactory().getCurrentSession().persist( entity );
            AbstractDao.log.trace( String.format( "Created %s.", formatEntity( entity ) ) );
            return entity;
        } else {
            //noinspection unchecked
            T result = ( T ) getSessionFactory().getCurrentSession().merge( entity );
            AbstractDao.log.trace( String.format( "Updated %s.", formatEntity( entity ) ) );
            return result;
        }
    }

    @Override
    public Collection<T> load( Collection<Long> ids ) {
        StopWatch timer = StopWatch.createStarted();
        if ( ids.isEmpty() ) {
            AbstractDao.log.trace( String.format( "Loading %s with an empty collection of IDs, returning an empty collection.", elementClass.getSimpleName() ) );
            return Collections.emptyList();
        }
        String idPropertyName = classMetadata.getIdentifierPropertyName();
        //noinspection unchecked
        List<T> results = this.getSessionFactory().getCurrentSession()
                .createCriteria( elementClass )
                .add( Restrictions.in( idPropertyName, new HashSet<>( ids ) ) )
                .list();
        AbstractDao.log.debug( String.format( "Loaded %d %s entities in %d ms.", results.size(), elementClass.getSimpleName(), timer.getTime( TimeUnit.MILLISECONDS ) ) );
        return results;
    }

    @Override
    public T load( Long id ) {
        // Don't use 'load' because if the object doesn't exist you can get an invalid proxy.
        //noinspection unchecked
        T result = ( T ) this.getSessionFactory().getCurrentSession().get( elementClass, id );
        AbstractDao.log.trace( String.format( String.format( "Loaded %s.", formatEntity( result ) ) ) );
        return result;
    }

    @Override
    public Collection<T> loadAll() {
        StopWatch timer = StopWatch.createStarted();
        //noinspection unchecked
        Collection<T> results = this.getSessionFactory().getCurrentSession().createCriteria( elementClass ).list();
        AbstractDao.log.debug( String.format( "Loaded all (%d) %s entities in %d ms.", results.size(), elementClass.getSimpleName(), timer.getTime( TimeUnit.MILLISECONDS ) ) );
        return results;
    }

    @Override
    public long countAll() {
        return ( Long ) this.getSessionFactory().getCurrentSession().createCriteria( elementClass )
                .setProjection( Projections.rowCount() )
                .uniqueResult();
    }

    @Override
    public void remove( Collection<T> entities ) {
        StopWatch timer = StopWatch.createStarted();
        warnIfBatchingIsNotAdvisable( "remove", entities );
        int i = 0;
        for ( T e : entities ) {
            this.remove( e );
            if ( ++i % batchSize == 0 && isBatchingAdvisable() ) {
                flushAndClear();
                AbstractDao.log.trace( String.format( "Flushed and cleared after removing %d/%d %s entities.", i, entities.size(), elementClass ) );
            }
        }
        AbstractDao.log.debug( String.format( "Removed %d entities in %d ms.", entities.size(), timer.getTime( TimeUnit.MILLISECONDS ) ) );
    }

    @Override
    public void remove( Long id ) {
        T entity = this.load( id );
        if ( entity != null ) {
            this.remove( entity );
        } else {
            AbstractDao.log.trace( String.format( "No %s entity with ID %d, no need to remove anything.", elementClass.getSimpleName(), id ) );
        }
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void remove( T entity ) {
        this.getSessionFactory().getCurrentSession().delete( entity );
        AbstractDao.log.trace( String.format( "Removed %s.", formatEntity( entity ) ) );
    }

    @Override
    public void removeAll() {
        StopWatch timer = StopWatch.createStarted();
        this.remove( this.loadAll() );
        AbstractDao.log.debug( String.format( "Removed all %s entities in %d ms.", elementClass.getSimpleName(), timer.getTime( TimeUnit.MILLISECONDS ) ) );
    }

    @Override
    public void update( Collection<T> entities ) {
        StopWatch timer = StopWatch.createStarted();
        warnIfBatchingIsNotAdvisable( "update", entities );
        int i = 0;
        for ( T entity : entities ) {
            this.update( entity );
            if ( ++i % batchSize == 0 && isBatchingAdvisable() ) {
                flushAndClear();
                AbstractDao.log.trace( String.format( "Flushed and cleared after updating %d/%d %s entities.", i, entities.size(), elementClass ) );
            }
        }
        AbstractDao.log.debug( String.format( "Updated %d %s entities in %d ms.", entities.size(), elementClass.getSimpleName(), timer.getTime( TimeUnit.MILLISECONDS ) ) );
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void update( T entity ) {
        this.getSessionFactory().getCurrentSession().update( entity );
    }

    @Override
    public T find( T entity ) {
        if ( entity.getId() != null ) {
            return this.load( entity.getId() );
        } else {
            AbstractDao.log.trace( String.format( "No persistent entity found for %s, returning null.", formatEntity( entity ) ) );
            return null;
        }
    }

    @Override
    public T findOrCreate( T entity ) {
        T found = this.find( entity );
        if ( found != null ) {
            return found;
        } else {
            AbstractDao.log.trace( String.format( "No persistent entity found for %s, creating a new one...", formatEntity( entity ) ) );
            return this.create( entity );
        }
    }

    protected SessionFactory getSessionFactory() {
        return sessionFactory;
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
    protected T findOneByProperty( String propertyName, Object propertyValue ) {
        //noinspection unchecked
        return ( T ) this.getSessionFactory().getCurrentSession()
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
        return this.getSessionFactory().getCurrentSession()
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
        return this.getSessionFactory().getCurrentSession()
                .createCriteria( this.elementClass )
                .add( Restrictions.in( propertyName, propertyValues ) )
                .list();
    }

    /**
     * Reattach an entity to the current persistence context.
     * <p>
     * This is a hack to avoid {@link org.hibernate.LazyInitializationException} when manipulating an unmanaged or
     * detached entity. If you need this, it means that the session scope does not encompass loading and updating the
     * entity, and can generally be better addressed by annotating a calling method with {@link org.springframework.transaction.annotation.Transactional}.
     * <p>
     * Note that this does not propagate to children entities even of lock cascading is set.
     */
    @Deprecated
    protected void reattach( T entity ) {
        this.getSessionFactory().getCurrentSession().buildLockRequest( LockOptions.NONE ).lock( entity );
        AbstractDao.log.trace( String.format( "Reattached %s using a noop lock.", formatEntity( entity ) ) );
    }

    /**
     * If you thought that {@link #remove(Identifiable)} was a bad idea, this is even worse.
     */
    @Deprecated
    protected void reattach( Object entity ) {
        this.getSessionFactory().getCurrentSession().buildLockRequest( LockOptions.NONE ).lock( entity );
        AbstractDao.log.trace( "Reattached unknown entity using a noop lock." );
    }

    /**
     * Flush pending changes to the persistent storage.
     */
    protected void flush() {
        this.getSessionFactory().getCurrentSession().flush();
    }

    /**
     * Flush pending changes and clear the session.
     * <p>
     * Use this carefully, cleared entities referenced later will be retrieved from the persistent storage.
     */
    protected void flushAndClear() {
        this.getSessionFactory().getCurrentSession().flush();
        this.getSessionFactory().getCurrentSession().clear();
    }

    /**
     * Emit a warning if the current flush mode does not allow batching the given collection.
     */
    private void warnIfBatchingIsNotAdvisable( String operation, Collection<?> entities ) {
        if ( entities.size() >= DEFAULT_BATCH_SIZE && !isBatchingAdvisable() ) {
            AbstractDao.log.warn( String.format( "Batching is not advisable with current flush mode %s, will proceed with %s on %d entities without invoking Session.flush() and Session.clear().",
                    getSessionFactory().getCurrentSession().getFlushMode(),
                    operation,
                    entities.size() ) );
        }
    }

    /**
     * Check if batching is currently advisable.
     * <p>
     * In certain cases, such as when the flush mode is set to {@link FlushMode#MANUAL}, we would want to prevent any
     * unintended flushes.
     */
    private boolean isBatchingAdvisable() {
        FlushMode flushMode = getSessionFactory().getCurrentSession().getFlushMode();
        return flushMode == FlushMode.AUTO || flushMode == FlushMode.ALWAYS;
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
