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

import java.util.Collection;

/**
 * <p>
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.expression.bioAssayData.BioAssayDataVector</code>.
 * </p>
 * 
 * @see ubic.gemma.model.expression.bioAssayData.BioAssayDataVector
 */
public abstract class BioAssayDataVectorDaoBase extends org.springframework.orm.hibernate3.support.HibernateDaoSupport
        implements ubic.gemma.model.expression.bioAssayData.BioAssayDataVectorDao {

    /**
     * @see ubic.gemma.model.expression.bioAssayData.BioAssayDataVectorDao#create(int, java.util.Collection)
     */
    public java.util.Collection<? extends BioAssayDataVector> create(
            final java.util.Collection<? extends BioAssayDataVector> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "BioAssayDataVector.create - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator entityIterator = entities.iterator(); entityIterator.hasNext(); ) {
                            create( ( ubic.gemma.model.expression.bioAssayData.BioAssayDataVector ) entityIterator
                                    .next() );
                        }
                        return null;
                    }
                } );
        return entities;
    }
    
    
    public Collection<? extends BioAssayDataVector > load( Collection<Long> ids ) {
        return this.getHibernateTemplate().findByNamedParam( "from BioAssayDataVectorImpl where id in (:ids)", "ids", ids );
    }

    /**
     * @see ubic.gemma.model.expression.bioAssayData.BioAssayDataVectorDao#create(int transform,
     *      ubic.gemma.model.expression.bioAssayData.BioAssayDataVector)
     */
    public BioAssayDataVector create(
            final ubic.gemma.model.expression.bioAssayData.BioAssayDataVector bioAssayDataVector ) {
        if ( bioAssayDataVector == null ) {
            throw new IllegalArgumentException( "BioAssayDataVector.create - 'bioAssayDataVector' can not be null" );
        }
        this.getHibernateTemplate().save( bioAssayDataVector );
        return bioAssayDataVector;
    }

    /**
     * @see ubic.gemma.model.expression.bioAssayData.BioAssayDataVectorDao#find(int, java.lang.String,
     *      ubic.gemma.model.expression.bioAssayData.BioAssayDataVector)
     */
    
    public BioAssayDataVector find( final java.lang.String queryString,
            final ubic.gemma.model.expression.bioAssayData.BioAssayDataVector bioAssayDataVector ) {
        java.util.List<String> argNames = new java.util.ArrayList<String>();
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add( bioAssayDataVector );
        argNames.add( "bioAssayDataVector" );
        java.util.Set results = new java.util.LinkedHashSet( this.getHibernateTemplate().findByNamedParam( queryString,
                argNames.toArray( new String[argNames.size()] ), args.toArray() ) );
        Object result = null;

        if ( results.size() > 1 ) {
            throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                    "More than one instance of 'ubic.gemma.model.expression.bioAssayData.BioAssayDataVector"
                            + "' was found when executing query --> '" + queryString + "'" );
        } else if ( results.size() == 1 ) {
            result = results.iterator().next();
        }

        return ( BioAssayDataVector ) result;
    }

    /**
     * @see ubic.gemma.model.expression.bioAssayData.BioAssayDataVectorDao#find(int,
     *      ubic.gemma.model.expression.bioAssayData.BioAssayDataVector)
     */
    public BioAssayDataVector find( final ubic.gemma.model.expression.bioAssayData.BioAssayDataVector bioAssayDataVector ) {
        return this
                .find(

                        "from ubic.gemma.model.expression.bioAssayData.BioAssayDataVector as bioAssayDataVector where bioAssayDataVector.bioAssayDataVector = :bioAssayDataVector",
                        bioAssayDataVector );
    }

    /**
     * @see ubic.gemma.model.expression.bioAssayData.BioAssayDataVectorDao#findOrCreate(int, java.lang.String,
     *      ubic.gemma.model.expression.bioAssayData.BioAssayDataVector)
     */
    
    public BioAssayDataVector findOrCreate( final java.lang.String queryString,
            final ubic.gemma.model.expression.bioAssayData.BioAssayDataVector bioAssayDataVector ) {
        java.util.List<String> argNames = new java.util.ArrayList<String>();
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add( bioAssayDataVector );
        argNames.add( "bioAssayDataVector" );
        java.util.Set results = new java.util.LinkedHashSet( this.getHibernateTemplate().findByNamedParam( queryString,
                argNames.toArray( new String[argNames.size()] ), args.toArray() ) );
        Object result = null;

        if ( results.size() > 1 ) {
            throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                    "More than one instance of 'ubic.gemma.model.expression.bioAssayData.BioAssayDataVector"
                            + "' was found when executing query --> '" + queryString + "'" );
        } else if ( results.size() == 1 ) {
            result = results.iterator().next();
        }

        return ( BioAssayDataVector ) result;
    }

    /**
     * @see ubic.gemma.model.expression.bioAssayData.BioAssayDataVectorDao#findOrCreate(int,
     *      ubic.gemma.model.expression.bioAssayData.BioAssayDataVector)
     */
    
    public BioAssayDataVector findOrCreate(
            final ubic.gemma.model.expression.bioAssayData.BioAssayDataVector bioAssayDataVector ) {
        return this
                .findOrCreate(

                        "from ubic.gemma.model.expression.bioAssayData.BioAssayDataVector as bioAssayDataVector where bioAssayDataVector.bioAssayDataVector = :bioAssayDataVector",
                        bioAssayDataVector );
    }

    /**
     * @see ubic.gemma.model.expression.bioAssayData.BioAssayDataVectorDao#load(int, java.lang.Long)
     */
    public BioAssayDataVector load( final java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "BioAssayDataVector.load - 'id' can not be null" );
        }
        final Object entity = this.getHibernateTemplate().get(
                ubic.gemma.model.expression.bioAssayData.BioAssayDataVectorImpl.class, id );
        return ( ubic.gemma.model.expression.bioAssayData.BioAssayDataVector ) entity;
    }

    /**
     * @see ubic.gemma.model.expression.bioAssayData.BioAssayDataVectorDao#loadAll(int)
     */
    
    public java.util.Collection<? extends BioAssayDataVector> loadAll() {
        final java.util.Collection results = this.getHibernateTemplate().loadAll(
                ubic.gemma.model.expression.bioAssayData.BioAssayDataVectorImpl.class );

        return results;
    }

    /**
     * @see ubic.gemma.model.expression.bioAssayData.BioAssayDataVectorDao#remove(java.lang.Long)
     */
    public void remove( java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "BioAssayDataVector.remove - 'id' can not be null" );
        }
        ubic.gemma.model.expression.bioAssayData.BioAssayDataVector entity = this.load( id );
        if ( entity != null ) {
            this.remove( entity );
        }
    }

    /**
     * @see ubic.gemma.model.expression.bioAssayData.DataVectorDao#remove(java.util.Collection)
     */
    public void remove( java.util.Collection<? extends BioAssayDataVector> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "BioAssayDataVector.remove - 'entities' can not be null" );
        }
        this.getHibernateTemplate().deleteAll( entities );
    }

    /**
     * @see ubic.gemma.model.expression.bioAssayData.BioAssayDataVectorDao#remove(ubic.gemma.model.expression.bioAssayData.BioAssayDataVector)
     */
    public void remove( ubic.gemma.model.expression.bioAssayData.BioAssayDataVector bioAssayDataVector ) {
        if ( bioAssayDataVector == null ) {
            throw new IllegalArgumentException( "BioAssayDataVector.remove - 'bioAssayDataVector' can not be null" );
        }
        this.getHibernateTemplate().delete( bioAssayDataVector );
    }

    /**
     * @see ubic.gemma.model.expression.bioAssayData.DataVectorDao#update(java.util.Collection)
     */
    public void update( final java.util.Collection entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "BioAssayDataVector.update - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator entityIterator = entities.iterator(); entityIterator.hasNext(); ) {
                            update( ( ubic.gemma.model.expression.bioAssayData.BioAssayDataVector ) entityIterator
                                    .next() );
                        }
                        return null;
                    }
                } );
    }

    /**
     * @see ubic.gemma.model.expression.bioAssayData.BioAssayDataVectorDao#update(ubic.gemma.model.expression.bioAssayData.BioAssayDataVector)
     */
    public void update( ubic.gemma.model.expression.bioAssayData.BioAssayDataVector bioAssayDataVector ) {
        if ( bioAssayDataVector == null ) {
            throw new IllegalArgumentException( "BioAssayDataVector.update - 'bioAssayDataVector' can not be null" );
        }
        this.getHibernateTemplate().update( bioAssayDataVector );
    }

}