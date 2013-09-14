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
package ubic.gemma.persistence;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;

import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

import ubic.gemma.util.EntityUtils;

/**
 * AbstractDao can find the generic type at runtime and simplify the code implementation of the BaseDao interface
 * 
 * @author Anton, Nicolas
 * @version $Id$
 */
public abstract class AbstractDao<T> extends HibernateDaoSupport implements BaseDao<T> {

    // generic class
    private Class<T> elementClass;

    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected AbstractDao( Class elementClass ) {
        assert elementClass.isAssignableFrom( elementClass );
        this.elementClass = elementClass;
    }

    // Other way we could use to access generic types at runtime in Java (might be useful later)
    /*
     * @SuppressWarnings("unchecked") protected AbstractDao() { Class<?> cl = getClass();
     * 
     * if ( Object.class.getSimpleName().equals( cl.getSuperclass().getSimpleName() ) ) { throw new
     * IllegalArgumentException( "Default constructor does not support direct instantiation" ); }
     * 
     * while ( !AbstractDao.class.getSimpleName().equals( cl.getSuperclass().getSimpleName() ) ) { // case of multiple
     * inheritance, we are trying to get the first available generic info if ( cl.getGenericSuperclass() instanceof
     * ParameterizedType ) { break; } cl = cl.getSuperclass(); }
     * 
     * if ( cl.getGenericSuperclass() instanceof ParameterizedType ) { elementClass = ( Class<T> ) ( ( ParameterizedType
     * ) cl.getGenericSuperclass() ).getActualTypeArguments()[0]; } }
     */

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.persistence.BaseDao#create(java.util.Collection)
     */
    @Override
    public Collection<? extends T> create( Collection<? extends T> entities ) {
        nullCheck( entities );
        if ( entities.isEmpty() ) return entities;
        int i = 0;
        for ( T t : entities ) {
            this.create( t );
            if ( ++i % 100 == 0 ) this.getSessionFactory().getCurrentSession().flush();
        }

        return entities;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.persistence.BaseDao#create(java.lang.Object)
     */
    @Override
    public T create( T entity ) {
        nullCheck( entity );
        Serializable id = this.getSessionFactory().getCurrentSession().save( entity );
        assert EntityUtils.getId( entity ) != null;
        assert id.equals( EntityUtils.getId( entity ) );

        return entity;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.persistence.BaseDao#load(java.util.Collection)
     */
    @Override
    public Collection<T> load( Collection<Long> ids ) {
        if ( ids.isEmpty() ) return new HashSet<T>();
        return this.getSessionFactory().getCurrentSession()
                .createQuery( "from   " + elementClass.getSimpleName() + " where id in (:ids)" )
                .setParameterList( "ids", ids ).list();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.persistence.BaseDao#load(java.lang.Long)
     */
    @Override
    public T load( Long id ) {
        nullCheck( id );
        T entity = ( T ) this.getSessionFactory().getCurrentSession().load( elementClass, id );
        return entity;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.persistence.BaseDao#loadAll()
     */
    @Override
    public Collection<T> loadAll() {
        return this.getHibernateTemplate().loadAll( elementClass );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.persistence.BaseDao#remove(java.util.Collection)
     */
    @Override
    public void remove( Collection<? extends T> entities ) {
        nullCheck( entities );
        if ( entities.isEmpty() ) return;
        this.getHibernateTemplate().deleteAll( entities );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.persistence.BaseDao#remove(java.lang.Long)
     */
    @Override
    public void remove( Long id ) {
        nullCheck( id );
        this.getHibernateTemplate().delete( this.load( id ) );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.persistence.BaseDao#remove(java.lang.Object)
     */
    @Override
    public void remove( T entity ) {
        nullCheck( entity );
        this.getHibernateTemplate().delete( entity );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.persistence.BaseDao#update(java.util.Collection)
     */
    @Override
    public void update( Collection<? extends T> entities ) {
        for ( T entity : entities ) {
            this.update( entity );
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.persistence.BaseDao#update(java.lang.Object)
     */
    @Override
    public void update( T entity ) {
        nullCheck( entity );
        this.getHibernateTemplate().update( entity );
    }

    /**
     * @param entity
     */
    private void nullCheck( Object entity ) {
        if ( entity == null ) throw new IllegalArgumentException( "Entity cannot be null" );
    }

}
