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
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.common.description.VocabCharacteristic</code>.
 * 
 * @see ubic.gemma.model.common.description.VocabCharacteristic
 */
public abstract class VocabCharacteristicDaoBase extends HibernateDaoSupport implements
        ubic.gemma.model.common.description.VocabCharacteristicDao {

    /**
     * @see ubic.gemma.model.common.description.VocabCharacteristicDao#create(int, java.util.Collection)
     */

    @Override
    public java.util.Collection create( final java.util.Collection entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "VocabCharacteristic.create - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    @Override
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator entityIterator = entities.iterator(); entityIterator.hasNext(); ) {
                            create( ( ubic.gemma.model.common.description.VocabCharacteristic ) entityIterator.next() );
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
    @Override
    public VocabCharacteristic create( final ubic.gemma.model.common.description.VocabCharacteristic vocabCharacteristic ) {
        if ( vocabCharacteristic == null ) {
            throw new IllegalArgumentException( "VocabCharacteristic.create - 'vocabCharacteristic' can not be null" );
        }
        this.getHibernateTemplate().save( vocabCharacteristic );
        return vocabCharacteristic;
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
    @Override
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
    @Override
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
    @Override
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

    @Override
    public VocabCharacteristic load( final java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "VocabCharacteristic.load - 'id' can not be null" );
        }
        final Object entity = this.getHibernateTemplate().get(
                ubic.gemma.model.common.description.VocabCharacteristicImpl.class, id );
        return ( ubic.gemma.model.common.description.VocabCharacteristic ) entity;
    }

    /**
     * @see ubic.gemma.model.common.description.VocabCharacteristicDao#loadAll(int)
     */

    @Override
    public java.util.Collection loadAll() {
        final java.util.Collection results = this.getHibernateTemplate().loadAll(
                ubic.gemma.model.common.description.VocabCharacteristicImpl.class );
        return results;
    }

    /**
     * @see ubic.gemma.model.common.description.VocabCharacteristicDao#remove(java.lang.Long)
     */

    @Override
    public void remove( java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "VocabCharacteristic.remove - 'id' can not be null" );
        }
        ubic.gemma.model.common.description.VocabCharacteristic entity = this.load( id );
        if ( entity != null ) {
            this.remove( entity );
        }
    }

    /**
     * @see ubic.gemma.model.common.SecurableDao#remove(java.util.Collection)
     */

    @Override
    public void remove( java.util.Collection entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "VocabCharacteristic.remove - 'entities' can not be null" );
        }
        this.getHibernateTemplate().deleteAll( entities );
    }

    /**
     * @see ubic.gemma.model.common.description.VocabCharacteristicDao#remove(ubic.gemma.model.common.description.VocabCharacteristic)
     */
    @Override
    public void remove( ubic.gemma.model.common.description.VocabCharacteristic vocabCharacteristic ) {
        if ( vocabCharacteristic == null ) {
            throw new IllegalArgumentException( "VocabCharacteristic.remove - 'vocabCharacteristic' can not be null" );
        }
        this.getHibernateTemplate().delete( vocabCharacteristic );
    }

    /**
     * @see ubic.gemma.model.common.SecurableDao#update(java.util.Collection)
     */

    @Override
    public void update( final java.util.Collection entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "VocabCharacteristic.update - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    @Override
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
    @Override
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

}