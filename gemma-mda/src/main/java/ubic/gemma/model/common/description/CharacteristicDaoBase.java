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
package ubic.gemma.model.common.description;

import java.util.Collection;

import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

/**
 * <p>
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.common.description.Characteristic</code>.
 * </p>
 * 
 * @see ubic.gemma.model.common.description.Characteristic
 */
public abstract class CharacteristicDaoBase extends HibernateDaoSupport implements
        ubic.gemma.model.common.description.CharacteristicDao {

    /**
     * @see ubic.gemma.model.common.description.CharacteristicDao#create(int, java.util.Collection)
     */
    @Override
    public java.util.Collection<? extends Characteristic> create(
            final java.util.Collection<? extends Characteristic> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "Characteristic.create - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    @Override
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator<? extends Characteristic> entityIterator = entities.iterator(); entityIterator
                                .hasNext(); ) {
                            create( entityIterator.next() );
                        }
                        return null;
                    }
                } );
        return entities;
    }

    @Override
    public Collection<? extends Characteristic> load( Collection<Long> ids ) {
        return this.getHibernateTemplate().findByNamedParam( "from CharacteristicImpl where id in (:ids)", "ids", ids );
    }

    /**
     * @see ubic.gemma.model.common.description.CharacteristicDao#create(int transform,
     *      ubic.gemma.model.common.description.Characteristic)
     */
    @Override
    public Characteristic create( final ubic.gemma.model.common.description.Characteristic characteristic ) {
        if ( characteristic == null ) {
            throw new IllegalArgumentException( "Characteristic.create - 'characteristic' can not be null" );
        }
        this.getHibernateTemplate().save( characteristic );
        return characteristic;
    }

    /**
     * @see ubic.gemma.model.common.description.CharacteristicDao#findByParentClass(java.lang.Class)
     */
    @Override
    public java.util.Map findByParentClass( final java.lang.Class parentClass ) {
        try {
            return this.handleFindByParentClass( parentClass );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.common.description.CharacteristicDao.findByParentClass(java.lang.Class parentClass)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.common.description.CharacteristicDao#findByUri(java.lang.String)
     */
    @Override
    public java.util.Collection<Characteristic> findByUri( final java.lang.String searchString ) {
        try {
            return this.handleFindByUri( searchString );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.common.description.CharacteristicDao.findByUri(java.lang.String searchString)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.common.description.CharacteristicDao#findByUri(java.util.Collection)
     */
    @Override
    public java.util.Collection<Characteristic> findByUri( final java.util.Collection<String> uris ) {
        try {
            return this.handleFindByUri( uris );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.common.description.CharacteristicDao.findByUri(java.util.Collection uris)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.common.description.CharacteristicDao#findByValue(java.lang.String)
     */
    @Override
    public java.util.Collection<Characteristic> findByValue( final java.lang.String search ) {
        try {
            return this.handleFindByValue( search );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.common.description.CharacteristicDao.findByValue(java.lang.String search)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.common.description.CharacteristicDao#getParents(java.lang.Class, java.util.Collection)
     */
    @Override
    public java.util.Map getParents( final java.lang.Class<?> parentClass,
            final java.util.Collection<Characteristic> characteristics ) {
        try {
            return this.handleGetParents( parentClass, characteristics );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.common.description.CharacteristicDao.getParents(java.lang.Class parentClass, java.util.Collection characteristics)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.common.description.CharacteristicDao#load(int, java.lang.Long)
     */

    @Override
    public Characteristic load( final java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "Characteristic.load - 'id' can not be null" );
        }
        final Object entity = this.getHibernateTemplate().get(
                ubic.gemma.model.common.description.CharacteristicImpl.class, id );
        return ( ubic.gemma.model.common.description.Characteristic ) entity;
    }

    /**
     * @see ubic.gemma.model.common.description.CharacteristicDao#loadAll(int)
     */

    @Override
    @SuppressWarnings("unchecked")
    public java.util.Collection<Characteristic> loadAll() {
        final java.util.Collection<?> results = this.getHibernateTemplate().loadAll(
                ubic.gemma.model.common.description.CharacteristicImpl.class );
        return ( Collection<Characteristic> ) results;
    }

    /**
     * @see ubic.gemma.model.common.description.CharacteristicDao#remove(java.lang.Long)
     */

    @Override
    public void remove( java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "Characteristic.remove - 'id' can not be null" );
        }
        ubic.gemma.model.common.description.Characteristic entity = this.load( id );
        if ( entity != null ) {
            this.remove( entity );
        }
    }

    /**
     * @see ubic.gemma.model.common.SecurableDao#remove(java.util.Collection)
     */

    @Override
    public void remove( java.util.Collection<? extends Characteristic> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "Characteristic.remove - 'entities' can not be null" );
        }
        this.getHibernateTemplate().deleteAll( entities );
    }

    /**
     * @see ubic.gemma.model.common.description.CharacteristicDao#remove(ubic.gemma.model.common.description.Characteristic)
     */
    @Override
    public void remove( ubic.gemma.model.common.description.Characteristic characteristic ) {
        if ( characteristic == null ) {
            throw new IllegalArgumentException( "Characteristic.remove - 'characteristic' can not be null" );
        }
        this.getHibernateTemplate().delete( characteristic );
    }

    /**
     * @see ubic.gemma.model.common.SecurableDao#update(java.util.Collection)
     */

    @Override
    public void update( final java.util.Collection<? extends Characteristic> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "Characteristic.update - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    @Override
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator<? extends Characteristic> entityIterator = entities.iterator(); entityIterator
                                .hasNext(); ) {
                            update( entityIterator.next() );
                        }
                        return null;
                    }
                } );
    }

    /**
     * @see ubic.gemma.model.common.description.CharacteristicDao#update(ubic.gemma.model.common.description.Characteristic)
     */
    @Override
    public void update( ubic.gemma.model.common.description.Characteristic characteristic ) {
        if ( characteristic == null ) {
            throw new IllegalArgumentException( "Characteristic.update - 'characteristic' can not be null" );
        }
        this.getHibernateTemplate().update( characteristic );
    }

    /**
     * Performs the core logic for {@link #findByParentClass(java.lang.Class)}
     */
    protected abstract java.util.Map handleFindByParentClass( java.lang.Class parentClass ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findByUri(java.lang.String)}
     */
    protected abstract java.util.Collection<Characteristic> handleFindByUri( java.lang.String searchString )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findByUri(java.util.Collection)}
     */
    protected abstract java.util.Collection<Characteristic> handleFindByUri( java.util.Collection<String> uris )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findByValue(java.lang.String)}
     */
    protected abstract java.util.Collection<Characteristic> handleFindByValue( java.lang.String search )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #getParents(java.lang.Class, java.util.Collection)}
     */
    protected abstract java.util.Map handleGetParents( java.lang.Class parentClass,
            java.util.Collection<Characteristic> characteristics ) throws java.lang.Exception;

}