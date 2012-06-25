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
package ubic.gemma.model.expression.bioAssayData;

/**
 * <p>
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.expression.bioAssayData.DesignElementDataVector</code>.
 * </p>
 * 
 * @see ubic.gemma.model.expression.bioAssayData.DesignElementDataVector
 */
public abstract class DesignElementDataVectorDaoBase<T extends DesignElementDataVector> extends
        org.springframework.orm.hibernate3.support.HibernateDaoSupport implements
        ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorDao<T> {

    /**
     * @see ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorDao#countAll()
     */
    @Override
    public java.lang.Integer countAll() {
        return this.handleCountAll();
    }

    /**
     * @see ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorDao#create(int, java.util.Collection)
     */
    @Override
    public java.util.Collection<T> create( final java.util.Collection<T> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "DesignElementDataVector.create - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    @Override
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator<T> entityIterator = entities.iterator(); entityIterator.hasNext(); ) {
                            create( entityIterator.next() );
                        }
                        return null;
                    }
                } );
        return entities;
    }

    /**
     * @see ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorDao#create(ubic.gemma.model.expression.bioAssayData.DesignElementDataVector)
     */
    @Override
    public T create( T designElementDataVector ) {
        if ( designElementDataVector == null ) {
            throw new IllegalArgumentException(
                    "DesignElementDataVector.create - 'designElementDataVector' can not be null" );
        }
        this.getHibernateTemplate().save( designElementDataVector );
        return designElementDataVector;
    }

    /**
     * @see ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorDao#remove(java.lang.Long)
     */

    @Override
    public void remove( java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "DesignElementDataVector.remove - 'id' can not be null" );
        }
        T entity = this.load( id );
        if ( entity != null ) {
            this.remove( entity );
        }
    }

    /**
     * @see ubic.gemma.model.expression.bioAssayData.DataVectorDao#remove(java.util.Collection)
     */

    @Override
    public void remove( java.util.Collection<T> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "DesignElementDataVector.remove - 'entities' can not be null" );
        }
        this.getHibernateTemplate().deleteAll( entities );
    }

    /**
     * @see ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorDao#remove(ubic.gemma.model.expression.bioAssayData.DesignElementDataVector)
     */
    @Override
    public void remove( T designElementDataVector ) {
        if ( designElementDataVector == null ) {
            throw new IllegalArgumentException(
                    "DesignElementDataVector.remove - 'designElementDataVector' can not be null" );
        }
        this.getHibernateTemplate().delete( designElementDataVector );
    }

    /**
     * @see ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorDao#thaw(java.util.Collection)
     */
    @Override
    public void thaw( final java.util.Collection<? extends DesignElementDataVector> designElementDataVectors ) {
        this.handleThaw( designElementDataVectors );
    }

    /**
     * @see ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorDao#thaw(ubic.gemma.model.expression.bioAssayData.DesignElementDataVector)
     */
    @Override
    public void thaw( final T designElementDataVector ) {
        this.handleThaw( designElementDataVector );
    }

    /**
     * @see ubic.gemma.model.expression.bioAssayData.DataVectorDao#update(java.util.Collection)
     */

    @Override
    public void update( final java.util.Collection<T> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "DesignElementDataVector.update - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    @Override
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator<T> entityIterator = entities.iterator(); entityIterator.hasNext(); ) {
                            update( entityIterator.next() );
                        }
                        return null;
                    }
                } );
    }

    /**
     * @see ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorDao#update(ubic.gemma.model.expression.bioAssayData.DesignElementDataVector)
     */
    @Override
    public void update( T designElementDataVector ) {
        if ( designElementDataVector == null ) {
            throw new IllegalArgumentException(
                    "DesignElementDataVector.update - 'designElementDataVector' can not be null" );
        }
        this.getHibernateTemplate().update( designElementDataVector );
    }

    /**
     * Performs the core logic for {@link #countAll()}
     */
    protected abstract java.lang.Integer handleCountAll();

    /**
     * Performs the core logic for {@link #thaw(java.util.Collection)}
     */
    protected abstract void handleThaw( java.util.Collection<? extends DesignElementDataVector> designElementDataVectors );

    /**
     * Performs the core logic for {@link #thaw(ubic.gemma.model.expression.bioAssayData.DesignElementDataVector)}
     */
    protected abstract void handleThaw( T designElementDataVector );

}