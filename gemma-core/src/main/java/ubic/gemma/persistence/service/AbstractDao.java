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

import lombok.NonNull;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.FlushMode;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import ubic.gemma.model.common.Identifiable;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AbstractDao can find the generic type at runtime and simplify the code implementation of the BaseDao interface
 *
 * @author Anton, Nicolas
 */
public abstract class AbstractDao<T extends Identifiable> extends HibernateDaoSupport implements BaseDao<T> {

    /**
     * @deprecated define your own logger instead
     */
    @Deprecated
    protected static final Log log = LogFactory.getLog( AbstractDao.class );

    /**
     * Batch size to reach before flushing the Hibernate session.
     *
     * See https://docs.jboss.org/hibernate/core/3.6/reference/en-US/html/batch.html for more details.
     */
    private static final int BATCH_SIZE = 100;

    protected final Class<T> elementClass;

    protected AbstractDao( Class<T> elementClass, SessionFactory sessionFactory ) {
        super.setSessionFactory( sessionFactory );
        this.elementClass = elementClass;
    }

    @Override
    public List<T> create( @NonNull Collection<T> entities ) {
        List<T> result = new ArrayList<>( entities.size() );
        int i = 0;
        for ( T t : entities ) {
            result.add( this.create( t ) );
            if ( ++i % BATCH_SIZE == 0 ) {
                this.getSessionFactory().getCurrentSession().flush();
                this.getSessionFactory().getCurrentSession().clear();
            }
        }
        return result;
    }

    @Override
    public T create( @NonNull T entity ) {
        Serializable id = this.getSessionFactory().getCurrentSession().save( entity );
        if ( id == null ) {
            throw new IllegalStateException( "ID received for " + entity + " is null." );
        }
        if ( !id.equals( entity.getId() ) ) {
            throw new IllegalStateException( "ID received for " + entity + " differs from " + entity.getId() + ": " + id + "." );
        }
        return entity;
    }

    @Override
    public List<T> load( @NonNull Collection<Long> ids ) {
        // ensure that IDs are unique so that elements cannot be repeated in different partitions
        ids = new HashSet<>( ids );
        //noinspection unchecked
        return ( List<T> ) this.getSessionFactory().getCurrentSession()
                .createCriteria( elementClass )
                .add( Restrictions.in( "id", ids ) )
                .list();
    }

    @SuppressWarnings("unchecked")
    @Override
    public T load( Long id ) {
        // Don't use 'load' because if the object doesn't exist you can get an invalid proxy.
        //noinspection unchecked
        return id == null ? null : ( T ) this.getSessionFactory().getCurrentSession().get( elementClass, id );
    }

    @Override
    public List<T> loadAll() {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createCriteria( elementClass ).list();
    }

    @Override
    public long countAll() {
        return ( Long ) this.getSessionFactory().getCurrentSession()
                .createCriteria( elementClass )
                .setProjection( Projections.rowCount() )
                .uniqueResult();
    }

    @Override
    public void remove( @NonNull Collection<T> entities ) {
        int i = 0;
        for ( T e : entities ) {
            this.remove( e );
            if ( ++i % BATCH_SIZE == 0 ) {
                this.getSessionFactory().getCurrentSession().flush();
                this.getSessionFactory().getCurrentSession().clear();
            }
        }
    }

    @Override
    public void remove( @NonNull Long id ) {
        this.remove( this.load( id ) );
    }

    @Override
    public void remove( @NonNull T entity ) {
        this.getSessionFactory().getCurrentSession().delete( entity );
    }

    @Override
    public void removeAll() {
        this.remove( this.loadAll() );
    }

    @Override
    public void update( @NonNull Collection<T> entities ) {
        int i = 0;
        for ( T entity : entities ) {
            this.update( entity );
            if ( ++i % BATCH_SIZE == 0 ) {
                this.getSessionFactory().getCurrentSession().flush();
                this.getSessionFactory().getCurrentSession().clear();
            }
        }
    }

    @Override
    public void update( @NonNull T entity ) {
        this.getSessionFactory().getCurrentSession().update( entity );
    }

    @Override
    public T find( @NonNull T entity ) {
        return this.load( entity.getId() );
    }

    @Override
    public T findOrCreate( @NonNull T entity ) {
        T found = this.find( entity );
        return found == null ? this.create( entity ) : found;
    }

    /**
     * This implementation applies {@link #thaw(T)} to each passed entities.
     */
    @Override
    public void thaw( @NonNull List<T> entities ) {
        for ( T entity : entities ) {
            this.thaw( entity );
        }
    }

    @Override
    public void thaw( @NonNull T entity ) {
        // does nothing in particular otherwise, specifics of thawing depends on the implementation
    }

    /**
     * Does a like-match case-insensitive search on given property and its value.
     *
     * @param  propertyName  the name of property to be matched.
     * @param  propertyValue the value to look for.
     * @return an entity whose property first like-matched the given value.
     */
    @SuppressWarnings("unchecked")
    protected T findOneByStringProperty( @NonNull String propertyName, @NonNull String propertyValue ) {
        //noinspection unchecked
        return ( T ) this.getSessionFactory().getCurrentSession().createCriteria( this.elementClass )
                .add( Restrictions.ilike( propertyName, propertyValue ) )
                .setMaxResults( 1 )
                .uniqueResult();
    }

    /**
     * Does a like-match case-insensitive search on given property and its value.
     *
     * @param  propertyName  the name of property to be matched.
     * @param  propertyValue the value to look for.
     * @return a list of entities whose properties like-matched the given value.
     */
    @SuppressWarnings("SameParameterValue") // Better for general use
    protected List<T> findByStringProperty( @NonNull String propertyName, @NonNull String propertyValue ) {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createCriteria( this.elementClass )
                .add( Restrictions.ilike( propertyName, propertyValue ) )
                .list();
    }

    /**
     * Lists all entities whose given property matches the given value.
     *
     * @param  propertyName  the name of property to be matched.
     * @param  propertyValue the value to look for.
     * @return a list of entities whose properties matched the given value.
     */
    @SuppressWarnings("unchecked")
    protected T findOneByProperty( @NonNull String propertyName, Object propertyValue ) {

        /*
         * Disable flush to avoid NonNullability constraint failures, etc. prematurely when running this during object
         * creation. This effectively makes this method read-only even in a read-write context. (the same setup might be
         * needed for other methods)
         */
        FlushMode fm = this.getSessionFactory().getCurrentSession().getFlushMode();
        this.getSessionFactory().getCurrentSession().setFlushMode( FlushMode.MANUAL );
        Criteria criteria = this.getSessionFactory().getCurrentSession().createCriteria( this.elementClass )
                .add( propertyValue == null ? Restrictions.isNull( propertyName ) : Restrictions.eq( propertyName, propertyValue ) )
                .setMaxResults( 1 );
        //noinspection unchecked
        T result = ( T ) criteria.uniqueResult();
        this.getSessionFactory().getCurrentSession().setFlushMode( fm );
        return result;
    }

    /**
     * Does a search on given property and its value.
     *
     * @param  propertyName  the name of property to be matched.
     * @param  propertyValue the value to look for.
     * @return an entity whose property first matched the given value.
     */
    protected List<T> findByProperty( @NonNull String propertyName, Object propertyValue ) {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createCriteria( this.elementClass )
                .add( propertyValue == null ? Restrictions.isNull( propertyName ) : Restrictions.eq( propertyName, propertyValue ) )
                .list();
    }
}
