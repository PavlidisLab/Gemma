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
import org.hibernate.criterion.Restrictions;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import ubic.gemma.persistence.service.genome.taxon.TaxonServiceImpl;
import ubic.gemma.persistence.util.EntityUtils;

import java.io.Serializable;
import java.util.Collection;

/**
 * AbstractDao can find the generic type at runtime and simplify the code implementation of the BaseDao interface
 *
 * @author Anton, Nicolas
 */
public abstract class AbstractDao<T> extends HibernateDaoSupport implements BaseDao<T> {

    protected static final Log log = LogFactory.getLog( TaxonServiceImpl.class );

    private Class<T> elementClass;

    /* ********************************
     * Constructors
     * ********************************/

    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected AbstractDao( Class elementClass ) {
        assert elementClass.isAssignableFrom( elementClass );
        this.elementClass = elementClass;
    }

    /* ********************************
     * Public methods
     * ********************************/

    @Override
    public Collection<? extends T> create( Collection<? extends T> entities ) {
        int i = 0;
        for ( T t : entities ) {
            this.create( t );
            if ( ++i % 100 == 0 )
                this.getSession().flush();
        }

        return entities;
    }

    @Override
    public T create( T entity ) {
        Serializable id = this.getSession().save( entity );
        assert EntityUtils.getId( entity ) != null;
        assert id.equals( EntityUtils.getId( entity ) );
        return entity;
    }

    @Override
    public Collection<T> load( Collection<Long> ids ) {
        //noinspection unchecked
        return this.getSession().createQuery( "from   " + elementClass.getSimpleName() + " where id in (:ids)" )
                .setParameterList( "ids", ids ).list();
    }

    @Override
    public T load( Long id ) {
        // Don't use 'load' because if the object doesn't exist you can get an invalid proxy.
        //noinspection unchecked
        return ( T ) this.getSession().get( elementClass, id );
    }

    @Override
    public Collection<T> loadAll() {
        //noinspection unchecked
        return this.getSession().createCriteria( elementClass ).list();
    }

    @Override
    public void remove( Collection<? extends T> entities ) {
        for ( T e : entities ) {
            this.remove( e );
        }
    }

    @Override
    public void remove( Long id ) {
        this.remove( this.load( id ) );
    }

    @Override
    public void remove( T entity ) {
        this.getSession().delete( entity );
    }

    @Override
    public void update( Collection<? extends T> entities ) {
        for ( T entity : entities ) {
            this.update( entity );
        }
    }

    @Override
    public void update( T entity ) {
        this.getSession().update( entity );
    }

    /* ********************************
     * Protected methods
     * ********************************/

    /**
     * Does a like-match case insensitive search on given property and its value.
     *
     * @param propertyName  the name of property to be matched.
     * @param propertyValue the value to look for.
     * @return a Taxon whose property first like-matched the given value.
     */
    protected T findOneByStringProperty( String propertyName, String propertyValue ) {
        Criteria criteria = this.getSessionFactory().getCurrentSession().createCriteria( this.elementClass );
        criteria.add( Restrictions.ilike( propertyName, propertyValue ) );
        criteria.setMaxResults( 1 );
        //noinspection unchecked
        return ( T ) criteria.uniqueResult();
    }

}
