/*
 * The Gemma project.
 * 
 * Copyright (c) 2006-2007 University of British Columbia
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

import java.util.Collection;
import java.util.HashSet;

import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

/* AbstractDao can find the generic type at runtime and simplify the code
 * implementation of the BaseDao interface
 */
public abstract class AbstractDao<T> extends HibernateDaoSupport {

    // generic class
    private Class<T> elementClass;

    protected AbstractDao( Class elementClass ) {
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

    public Class<T> getElementClass() {
        return elementClass;
    }

    public Collection<? extends T> create( Collection<? extends T> entities ) {
        this.getHibernateTemplate().saveOrUpdateAll( entities );
        return entities;
    }

    public T create( T entity ) {
        this.getHibernateTemplate().save( entity );
        return entity;
    }

    @SuppressWarnings("unchecked")
    public Collection<T> load( Collection<Long> ids ) {
        Collection<T> result = new HashSet<T>();
        for ( Long id : ids ) {
            Object loaded = this.load( id );
            if ( loaded != null ) {
                result.add( ( T ) loaded );
            }
        }
        return result;
    }

    public T load( Long id ) {
        T entity = this.getHibernateTemplate().get( getElementClass(), id );
        return entity;
    }

    public Collection<T> loadAll() {
        return this.getHibernateTemplate().loadAll( getElementClass() );
    }

    public void remove( Collection<? extends T> entities ) {
        this.getHibernateTemplate().deleteAll( entities );
    }

    public void remove( Long id ) {
        this.getHibernateTemplate().delete( this.load( id ) );
    }

    public void remove( T entity ) {
        if ( entity == null ) return;
        this.getHibernateTemplate().delete( entity );
    }

    public void update( Collection<? extends T> entities ) {
        for ( T entity : entities ) {
            this.update( entity );
        }
    }

    public void update( T entity ) {
        this.getHibernateTemplate().update( entity );

    }
}
