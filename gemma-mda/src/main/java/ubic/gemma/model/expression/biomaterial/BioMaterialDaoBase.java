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
package ubic.gemma.model.expression.biomaterial;

import java.util.Collection;
import java.util.Iterator;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

/**
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.expression.biomaterial.BioMaterial</code>.
 * 
 * @see ubic.gemma.model.expression.biomaterial.BioMaterial
 * @version $id$
 */
public abstract class BioMaterialDaoBase extends HibernateDaoSupport implements BioMaterialDao {

    /**
     * @see ubic.gemma.model.expression.biomaterial.BioMaterialDao#copy(ubic.gemma.model.expression.biomaterial.BioMaterial)
     */
    @Override
    public BioMaterial copy( final BioMaterial bioMaterial ) {
        try {
            return this.handleCopy( bioMaterial );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.expression.biomaterial.BioMaterialDao.copy(ubic.gemma.model.expression.biomaterial.BioMaterial bioMaterial)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.biomaterial.BioMaterialDao#countAll()
     */
    @Override
    public java.lang.Integer countAll() {
        try {
            return this.handleCountAll();
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.expression.biomaterial.BioMaterialDao.countAll()' --> " + th,
                    th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.biomaterial.BioMaterialDao#create(int transform,
     *      ubic.gemma.model.expression.biomaterial.BioMaterial)
     */
    @Override
    public BioMaterial create( final BioMaterial bioMaterial ) {
        if ( bioMaterial == null ) {
            throw new IllegalArgumentException( "BioMaterial.create - 'bioMaterial' can not be null" );
        }
        this.getHibernateTemplate().save( bioMaterial );
        return bioMaterial;
    }

    /**
     * @see ubic.gemma.model.expression.biomaterial.BioMaterialDao#create(int, java.util.Collection)
     */
    @Override
    public java.util.Collection<? extends BioMaterial> create(
            final java.util.Collection<? extends BioMaterial> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "BioMaterial.create - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession( new HibernateCallback<Object>() {
            @Override
            public Object doInHibernate( Session session ) throws HibernateException {
                for ( java.util.Iterator<? extends BioMaterial> entityIterator = entities.iterator(); entityIterator
                        .hasNext(); ) {
                    create( entityIterator.next() );
                }
                return null;
            }
        } );
        return entities;
    }

    /**
     * @see ubic.gemma.model.expression.biomaterial.BioMaterialDao#load(int, java.lang.Long)
     */

    @Override
    public BioMaterial load( final java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "BioMaterial.load - 'id' can not be null" );
        }
        final Object entity = this.getHibernateTemplate().get( BioMaterialImpl.class, id );
        return ( BioMaterial ) entity;
    }

    /**
     * @see ubic.gemma.model.expression.biomaterial.BioMaterialDao#load(java.util.Collection)
     */
    @Override
    public Collection<BioMaterial> load( final java.util.Collection<Long> ids ) {
        try {
            return this.handleLoad( ids );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.expression.biomaterial.BioMaterialDao.load(java.util.Collection ids)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.biomaterial.BioMaterialDao#loadAll(int)
     */

    @Override
    public Collection<? extends BioMaterial> loadAll() {
        final java.util.Collection<? extends BioMaterial> results = this.getHibernateTemplate().loadAll(
                BioMaterialImpl.class );
        return results;
    }

    /**
     * @see ubic.gemma.model.expression.biomaterial.BioMaterialDao#remove(ubic.gemma.model.expression.biomaterial.BioMaterial)
     */
    @Override
    public void remove( BioMaterial bioMaterial ) {
        if ( bioMaterial == null ) {
            throw new IllegalArgumentException( "BioMaterial.remove - 'bioMaterial' can not be null" );
        }
        this.getHibernateTemplate().delete( bioMaterial );
    }

    /**
     * @see ubic.gemma.model.common.SecurableDao#remove(java.util.Collection)
     */

    @Override
    public void remove( Collection<? extends BioMaterial> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "BioMaterial.remove - 'entities' can not be null" );
        }
        this.getHibernateTemplate().deleteAll( entities );
    }

    /**
     * @see ubic.gemma.model.expression.biomaterial.BioMaterialDao#remove(java.lang.Long)
     */

    @Override
    public void remove( java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "BioMaterial.remove - 'id' can not be null" );
        }
        BioMaterial entity = this.load( id );
        if ( entity != null ) {
            this.remove( entity );
        }
    }

    /**
     * @see ubic.gemma.model.expression.biomaterial.BioMaterialDao#update(ubic.gemma.model.expression.biomaterial.BioMaterial)
     */
    @Override
    public void update( BioMaterial bioMaterial ) {
        if ( bioMaterial == null ) {
            throw new IllegalArgumentException( "BioMaterial.update - 'bioMaterial' can not be null" );
        }
        this.getHibernateTemplate().update( bioMaterial );
    }

    /**
     * @see ubic.gemma.model.common.SecurableDao#update(java.util.Collection)
     */

    @Override
    public void update( final Collection<? extends BioMaterial> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "BioMaterial.update - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession( new HibernateCallback<BioMaterial>() {
            @Override
            public BioMaterial doInHibernate( Session session ) throws HibernateException {
                for ( Iterator<? extends BioMaterial> entityIterator = entities.iterator(); entityIterator.hasNext(); ) {
                    update( entityIterator.next() );
                }
                return null;
            }
        } );
    }

    /**
     * Performs the core logic for {@link #copy(ubic.gemma.model.expression.biomaterial.BioMaterial)}
     */
    protected abstract BioMaterial handleCopy( BioMaterial bioMaterial ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #countAll()}
     */
    protected abstract Integer handleCountAll() throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #load(java.util.Collection)}
     */
    protected abstract Collection<BioMaterial> handleLoad( java.util.Collection<Long> ids ) throws Exception;

}