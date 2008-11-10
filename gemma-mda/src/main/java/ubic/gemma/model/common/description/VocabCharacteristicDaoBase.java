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

import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

/**
 * <p>
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.common.description.VocabCharacteristic</code>.
 * </p>
 * 
 * @see ubic.gemma.model.common.description.VocabCharacteristic
 */
public abstract class VocabCharacteristicDaoBase extends HibernateDaoSupport implements
        ubic.gemma.model.common.description.VocabCharacteristicDao {

    /**
     * @see ubic.gemma.model.common.description.VocabCharacteristicDao#create(int, java.util.Collection)
     */

    public java.util.Collection create( final int transform, final java.util.Collection entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "VocabCharacteristic.create - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback() {
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator entityIterator = entities.iterator(); entityIterator.hasNext(); ) {
                            create( transform,
                                    ( ubic.gemma.model.common.description.VocabCharacteristic ) entityIterator.next() );
                        }
                        return null;
                    }
                } );
        return entities;
    }

    /**
     * @see ubic.gemma.model.common.description.VocabCharacteristicDao#create(int transform,
     *      ubic.gemma.model.common.description.VocabCharacteristic)
     */
    public Object create( final int transform,
            final ubic.gemma.model.common.description.VocabCharacteristic vocabCharacteristic ) {
        if ( vocabCharacteristic == null ) {
            throw new IllegalArgumentException( "VocabCharacteristic.create - 'vocabCharacteristic' can not be null" );
        }
        this.getHibernateTemplate().save( vocabCharacteristic );
        return this.transformEntity( transform, vocabCharacteristic );
    }

    /**
     * @see ubic.gemma.model.common.description.VocabCharacteristicDao#create(java.util.Collection)
     */

    @SuppressWarnings( { "unchecked" })
    public java.util.Collection create( final java.util.Collection entities ) {
        return create( TRANSFORM_NONE, entities );
    }

    /**
     * @see ubic.gemma.model.common.description.VocabCharacteristicDao#create(ubic.gemma.model.common.description.VocabCharacteristic)
     */
    public VocabCharacteristic create( ubic.gemma.model.common.description.VocabCharacteristic vocabCharacteristic ) {
        return ( ubic.gemma.model.common.description.VocabCharacteristic ) this.create( TRANSFORM_NONE,
                vocabCharacteristic );
    }

    /**
     * @see ubic.gemma.model.common.description.CharacteristicDao#findByParentClass(java.lang.Class)
     */
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
    public java.util.Collection findByUri( final java.lang.String searchString ) {
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
    public java.util.Collection findByUri( final java.util.Collection uris ) {
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
    public java.util.Collection findByValue( final java.lang.String search ) {
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
    public java.util.Map getParents( final java.lang.Class parentClass, final java.util.Collection characteristics ) {
        try {
            return this.handleGetParents( parentClass, characteristics );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.common.description.CharacteristicDao.getParents(java.lang.Class parentClass, java.util.Collection characteristics)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.common.description.VocabCharacteristicDao#load(int, java.lang.Long)
     */

    public Object load( final int transform, final java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "VocabCharacteristic.load - 'id' can not be null" );
        }
        final Object entity = this.getHibernateTemplate().get(
                ubic.gemma.model.common.description.VocabCharacteristicImpl.class, id );
        return transformEntity( transform, ( ubic.gemma.model.common.description.VocabCharacteristic ) entity );
    }

    /**
     * @see ubic.gemma.model.common.description.VocabCharacteristicDao#load(java.lang.Long)
     */

    public VocabCharacteristic load( java.lang.Long id ) {
        return ( ubic.gemma.model.common.description.VocabCharacteristic ) this.load( TRANSFORM_NONE, id );
    }

    /**
     * @see ubic.gemma.model.common.description.VocabCharacteristicDao#loadAll()
     */

    @SuppressWarnings( { "unchecked" })
    public java.util.Collection loadAll() {
        return this.loadAll( TRANSFORM_NONE );
    }

    /**
     * @see ubic.gemma.model.common.description.VocabCharacteristicDao#loadAll(int)
     */

    public java.util.Collection loadAll( final int transform ) {
        final java.util.Collection results = this.getHibernateTemplate().loadAll(
                ubic.gemma.model.common.description.VocabCharacteristicImpl.class );
        this.transformEntities( transform, results );
        return results;
    }

    /**
     * @see ubic.gemma.model.common.description.VocabCharacteristicDao#remove(java.lang.Long)
     */

    public void remove( java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "VocabCharacteristic.remove - 'id' can not be null" );
        }
        ubic.gemma.model.common.description.VocabCharacteristic entity = ( ubic.gemma.model.common.description.VocabCharacteristic ) this
                .load( id );
        if ( entity != null ) {
            this.remove( entity );
        }
    }

    /**
     * @see ubic.gemma.model.common.SecurableDao#remove(java.util.Collection)
     */

    public void remove( java.util.Collection entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "VocabCharacteristic.remove - 'entities' can not be null" );
        }
        this.getHibernateTemplate().deleteAll( entities );
    }

    /**
     * @see ubic.gemma.model.common.description.VocabCharacteristicDao#remove(ubic.gemma.model.common.description.VocabCharacteristic)
     */
    public void remove( ubic.gemma.model.common.description.VocabCharacteristic vocabCharacteristic ) {
        if ( vocabCharacteristic == null ) {
            throw new IllegalArgumentException( "VocabCharacteristic.remove - 'vocabCharacteristic' can not be null" );
        }
        this.getHibernateTemplate().delete( vocabCharacteristic );
    }

    /**
     * @see ubic.gemma.model.common.SecurableDao#update(java.util.Collection)
     */

    public void update( final java.util.Collection entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "VocabCharacteristic.update - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback() {
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator entityIterator = entities.iterator(); entityIterator.hasNext(); ) {
                            update( ( ubic.gemma.model.common.description.VocabCharacteristic ) entityIterator.next() );
                        }
                        return null;
                    }
                } );
    }

    /**
     * @see ubic.gemma.model.common.description.VocabCharacteristicDao#update(ubic.gemma.model.common.description.VocabCharacteristic)
     */
    public void update( ubic.gemma.model.common.description.VocabCharacteristic vocabCharacteristic ) {
        if ( vocabCharacteristic == null ) {
            throw new IllegalArgumentException( "VocabCharacteristic.update - 'vocabCharacteristic' can not be null" );
        }
        this.getHibernateTemplate().update( vocabCharacteristic );
    }

    /**
     * Performs the core logic for {@link #findByParentClass(java.lang.Class)}
     */
    protected abstract java.util.Map handleFindByParentClass( java.lang.Class parentClass ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findByUri(java.lang.String)}
     */
    protected abstract java.util.Collection handleFindByUri( java.lang.String searchString ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findByUri(java.util.Collection)}
     */
    protected abstract java.util.Collection handleFindByUri( java.util.Collection uris ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findByValue(java.lang.String)}
     */
    protected abstract java.util.Collection handleFindByValue( java.lang.String search ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #getParents(java.lang.Class, java.util.Collection)}
     */
    protected abstract java.util.Map handleGetParents( java.lang.Class parentClass, java.util.Collection characteristics )
            throws java.lang.Exception;

    /**
     * Transforms a collection of entities using the
     * {@link #transformEntity(int,ubic.gemma.model.common.description.VocabCharacteristic)} method. This method does
     * not instantiate a new collection.
     * <p/>
     * This method is to be used internally only.
     * 
     * @param transform one of the constants declared in
     *        <code>ubic.gemma.model.common.description.VocabCharacteristicDao</code>
     * @param entities the collection of entities to transform
     * @return the same collection as the argument, but this time containing the transformed entities
     * @see #transformEntity(int,ubic.gemma.model.common.description.VocabCharacteristic)
     */

    protected void transformEntities( final int transform, final java.util.Collection entities ) {
        switch ( transform ) {
            case TRANSFORM_NONE: // fall-through
            default:
                // do nothing;
        }
    }

    /**
     * Allows transformation of entities into value objects (or something else for that matter), when the
     * <code>transform</code> flag is set to one of the constants defined in
     * <code>ubic.gemma.model.common.description.VocabCharacteristicDao</code>, please note that the
     * {@link #TRANSFORM_NONE} constant denotes no transformation, so the entity itself will be returned. If the integer
     * argument value is unknown {@link #TRANSFORM_NONE} is assumed.
     * 
     * @param transform one of the constants declared in
     *        {@link ubic.gemma.model.common.description.VocabCharacteristicDao}
     * @param entity an entity that was found
     * @return the transformed entity (i.e. new value object, etc)
     * @see #transformEntities(int,java.util.Collection)
     */
    protected Object transformEntity( final int transform,
            final ubic.gemma.model.common.description.VocabCharacteristic entity ) {
        Object target = null;
        if ( entity != null ) {
            switch ( transform ) {
                case TRANSFORM_NONE: // fall-through
                default:
                    target = entity;
            }
        }
        return target;
    }

}