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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.FlushMode;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Restrictions;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.persistence.service.genome.taxon.TaxonServiceImpl;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * AbstractDao can find the generic type at runtime and simplify the code implementation of the BaseDao interface
 *
 * @author Anton, Nicolas
 */
@Transactional
public abstract class AbstractDao<T extends Identifiable> extends HibernateDaoSupport implements BaseDao<T> {

    protected static final Log log = LogFactory.getLog( TaxonServiceImpl.class );

    protected final Class<T> elementClass;

    protected AbstractDao( Class<T> elementClass, SessionFactory sessionFactory ) {
        super.setSessionFactory( sessionFactory );
        this.elementClass = elementClass;
    }

    @Override
    public Collection<T> create( Collection<T> entities ) {
        int i = 0;
        for ( T t : entities ) {
            this.create( t );
            if ( ++i % 100 == 0 )
                this.getSessionFactory().getCurrentSession().flush();
        }
        return entities;
    }

    @Override
    public T create( T entity ) {
        Serializable id = this.getSessionFactory().getCurrentSession().save( entity );
        assert entity.getId() != null : "No ID received for " + entity;
        assert id.equals( entity.getId() );
        return entity;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<T> load( Collection<Long> ids ) {
        if ( ids.isEmpty() ) {
            return Collections.emptyList();
        }
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession()
                .createQuery( //language=none // Prevents unresolvable missing value warnings.
                        "from " + elementClass.getSimpleName() + " e where e.id in (:ids)" )
                .setParameterList( "ids", ids ).list();
    }

    @SuppressWarnings("unchecked")
    @Override
    @Transactional(readOnly = true)
    public T load( Long id ) {
        // Don't use 'load' because if the object doesn't exist you can get an invalid proxy.
        //noinspection unchecked
        return id == null ? null : ( T ) this.getSessionFactory().getCurrentSession().get( elementClass, id );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<T> loadAll() {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createCriteria( elementClass ).list();
    }

    @Override
    public Integer countAll() {
        return ( ( Long ) this.getSessionFactory().getCurrentSession()
                .createQuery( //language=none // Prevents unresolvable missing value warnings.
                        "select count(*) from " + elementClass.getSimpleName() )
                .uniqueResult() ).intValue();
    }

    @Override
    public void remove( Collection<T> entities ) {
        for ( T e : entities ) {
            this.remove( e );
        }
    }

    @Override
    public void remove( Long id ) {
        if ( id == null ) throw new IllegalArgumentException( "Id cannot be null" );
        this.remove( this.load( id ) );
    }

    @Override
    public void remove( T entity ) {
        if ( entity == null ) throw new IllegalArgumentException( "Entity cannot be null" );
        this.getSessionFactory().getCurrentSession().delete( entity );
    }

    @Override
    @Transactional
    public void update( Collection<T> entities ) {
        for ( T entity : entities ) {
            this.update( entity );
        }
    }

    @Override
    @Transactional
    public void update( T entity ) {
        if ( entity == null ) throw new IllegalArgumentException( "Entity cannot be null" );
        this.getSessionFactory().getCurrentSession().update( entity );
    }

    @Override
    @Transactional(readOnly = true)
    public T find( T entity ) {
        if ( entity == null ) throw new IllegalArgumentException( "Entity cannot be null" );
        return this.load( entity.getId() );
    }

    @Override
    @Transactional
    public T findOrCreate( T entity ) {
        if ( entity == null ) throw new IllegalArgumentException( "Entity cannot be null" );
        T found = this.find( entity );
        return found == null ? this.create( entity ) : found;
    }

    /**
     * Does a like-match case insensitive search on given property and its value.
     *
     * @param  propertyName  the name of property to be matched.
     * @param  propertyValue the value to look for.
     * @return an entity whose property first like-matched the given value.
     */
    @SuppressWarnings("unchecked")
    protected T findOneByStringProperty( String propertyName, String propertyValue ) {
        Criteria criteria = this.getSessionFactory().getCurrentSession().createCriteria( this.elementClass );
        criteria.add( Restrictions.ilike( propertyName, propertyValue ) );
        criteria.setMaxResults( 1 );
        //noinspection unchecked
        return ( T ) criteria.uniqueResult();
    }

    /**
     * Does a like-match case insensitive search on given property and its value.
     *
     * @param  propertyName  the name of property to be matched.
     * @param  propertyValue the value to look for.
     * @return a list of entities whose properties like-matched the given value.
     */
    @SuppressWarnings("SameParameterValue") // Better for general use
    protected List<T> findByStringProperty( String propertyName, String propertyValue ) {
        Criteria criteria = this.getSessionFactory().getCurrentSession().createCriteria( this.elementClass );
        criteria.add( Restrictions.ilike( propertyName, propertyValue ) );
        //noinspection unchecked
        return criteria.list();
    }

    /**
     * Lists all entities whose given property matches the given value.
     *
     * @param  propertyName  the name of property to be matched.
     * @param  propertyValue the value to look for.
     * @return a list of entities whose properties matched the given value.
     */
    @SuppressWarnings("unchecked")
    protected T findOneByProperty( String propertyName, Object propertyValue ) {

        /*
         * Disable flush to avoid NonNullability constraint failures, etc. prematurely when running this during object
         * creation. This effectively makes this method read-only even in a read-write context. (the same setup might be
         * needed for other methods)
         */
        FlushMode fm = this.getSessionFactory().getCurrentSession().getFlushMode();
        this.getSessionFactory().getCurrentSession().setFlushMode( FlushMode.MANUAL );
        Criteria criteria = this.getSessionFactory().getCurrentSession().createCriteria( this.elementClass );
        criteria.add( Restrictions.eq( propertyName, propertyValue ) );
        criteria.setMaxResults( 1 );

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
    protected List<T> findByProperty( String propertyName, Object propertyValue ) {
        Criteria criteria = this.getSessionFactory().getCurrentSession().createCriteria( this.elementClass );
        criteria.add( Restrictions.eq( propertyName, propertyValue ) );
        //noinspection unchecked
        return criteria.list();
    }

}
