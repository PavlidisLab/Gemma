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

import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

/**
 * <p>
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.expression.bioAssayData.BioAssayDimension</code>.
 * </p>
 * 
 * @see ubic.gemma.model.expression.bioAssayData.BioAssayDimension
 */
public abstract class BioAssayDimensionDaoBase extends HibernateDaoSupport implements
        ubic.gemma.model.expression.bioAssayData.BioAssayDimensionDao {

    /**
     * @see ubic.gemma.model.expression.bioAssayData.BioAssayDimensionDao#create(int, java.util.Collection)
     */
    @Override
    public java.util.Collection<? extends BioAssayDimension> create(
            final java.util.Collection<? extends BioAssayDimension> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "BioAssayDimension.create - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    @Override
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator<? extends BioAssayDimension> entityIterator = entities.iterator(); entityIterator
                                .hasNext(); ) {
                            create( entityIterator.next() );
                        }
                        return null;
                    }
                } );
        return entities;
    }

    /**
     * @see ubic.gemma.model.expression.bioAssayData.BioAssayDimensionDao#create(int transform,
     *      ubic.gemma.model.expression.bioAssayData.BioAssayDimension)
     */
    @Override
    public BioAssayDimension create( final ubic.gemma.model.expression.bioAssayData.BioAssayDimension bioAssayDimension ) {
        if ( bioAssayDimension == null ) {
            throw new IllegalArgumentException( "BioAssayDimension.create - 'bioAssayDimension' can not be null" );
        }
        this.getHibernateTemplate().save( bioAssayDimension );
        return bioAssayDimension;
    }

    /**
     * @see ubic.gemma.model.expression.bioAssayData.BioAssayDimensionDao#load(int, java.lang.Long)
     */

    @Override
    public BioAssayDimension load( final java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "BioAssayDimension.load - 'id' can not be null" );
        }
        final Object entity = this.getHibernateTemplate().get(
                ubic.gemma.model.expression.bioAssayData.BioAssayDimensionImpl.class, id );
        return ( ubic.gemma.model.expression.bioAssayData.BioAssayDimension ) entity;
    }

    /**
     * @see ubic.gemma.model.expression.bioAssayData.BioAssayDimensionDao#loadAll(int)
     */

    @Override
    public java.util.Collection<? extends BioAssayDimension> loadAll() {
        final java.util.Collection<? extends BioAssayDimension> results = this.getHibernateTemplate().loadAll(
                ubic.gemma.model.expression.bioAssayData.BioAssayDimensionImpl.class );
        return results;
    }

    /**
     * @see ubic.gemma.model.expression.bioAssayData.BioAssayDimensionDao#remove(java.lang.Long)
     */

    @Override
    public void remove( java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "BioAssayDimension.remove - 'id' can not be null" );
        }
        ubic.gemma.model.expression.bioAssayData.BioAssayDimension entity = this.load( id );
        if ( entity != null ) {
            this.remove( entity );
        }
    }

    /**
     * @see ubic.gemma.model.common.SecurableDao#remove(java.util.Collection)
     */

    @Override
    public void remove( java.util.Collection<? extends BioAssayDimension> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "BioAssayDimension.remove - 'entities' can not be null" );
        }
        this.getHibernateTemplate().deleteAll( entities );
    }

    /**
     * @see ubic.gemma.model.expression.bioAssayData.BioAssayDimensionDao#remove(ubic.gemma.model.expression.bioAssayData.BioAssayDimension)
     */
    @Override
    public void remove( ubic.gemma.model.expression.bioAssayData.BioAssayDimension bioAssayDimension ) {
        if ( bioAssayDimension == null ) {
            throw new IllegalArgumentException( "BioAssayDimension.remove - 'bioAssayDimension' can not be null" );
        }
        this.getHibernateTemplate().delete( bioAssayDimension );
    }

    /**
     * @see ubic.gemma.model.common.SecurableDao#update(java.util.Collection)
     */

    @Override
    public void update( final java.util.Collection<? extends BioAssayDimension> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "BioAssayDimension.update - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    @Override
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator<? extends BioAssayDimension> entityIterator = entities.iterator(); entityIterator
                                .hasNext(); ) {
                            update( entityIterator.next() );
                        }
                        return null;
                    }
                } );
    }

    /**
     * @see ubic.gemma.model.expression.bioAssayData.BioAssayDimensionDao#update(ubic.gemma.model.expression.bioAssayData.BioAssayDimension)
     */
    @Override
    public void update( ubic.gemma.model.expression.bioAssayData.BioAssayDimension bioAssayDimension ) {
        if ( bioAssayDimension == null ) {
            throw new IllegalArgumentException( "BioAssayDimension.update - 'bioAssayDimension' can not be null" );
        }
        this.getHibernateTemplate().update( bioAssayDimension );
    }

}