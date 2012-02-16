/*
 * The Gemma project.
 * 
 * Copyright (c) 2006 University of British Columbia
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
package ubic.gemma.model.expression.bioAssay;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.Hibernate;
import org.hibernate.LockOptions;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import org.springframework.stereotype.Repository;

import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.util.BusinessKey;
import ubic.gemma.util.EntityUtils;

/**
 * @author pavlidis
 * @version $Id$
 */
@Repository
public class BioAssayDaoImpl extends HibernateDaoSupport implements BioAssayDao {

    private static Log log = LogFactory.getLog( BioAssayDaoImpl.class.getName() );

    @Autowired
    public BioAssayDaoImpl( SessionFactory sessionFactory ) {
        super.setSessionFactory( sessionFactory );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.bioAssay.BioAssayDaoBase#find(ubic.gemma.model.expression.bioAssay.BioAssay)
     */
    public BioAssay find( BioAssay bioAssay ) {
        try {
            Criteria queryObject = BusinessKey.createQueryObject( super.getSession(), bioAssay );

            java.util.List<?> results = queryObject.list();
            Object result = null;
            if ( results != null ) {
                if ( results.size() > 1 ) {
                    throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                            "More than one instance of '" + BioAssay.class.getName()
                                    + "' was found when executing query" );

                } else if ( results.size() == 1 ) {
                    result = results.iterator().next();
                }
            }
            return ( BioAssay ) result;
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.bioAssay.BioAssayDao#findByAccession(java.lang.String)
     */
    @Override
    public Collection<BioAssay> findByAccession( String accession ) {
        if ( StringUtils.isBlank( accession ) ) return new HashSet<BioAssay>();

        return this.getHibernateTemplate().findByNamedParam(
                "select distinct b from BioAssayImpl b inner join b.accession a where a.accession = :query", "query",
                accession );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.bioAssay.BioAssayDaoBase#findOrCreate(ubic.gemma.model.expression.bioAssay.BioAssay)
     */
    @Override
    public BioAssay findOrCreate( BioAssay bioAssay ) {
        if ( bioAssay == null || bioAssay.getName() == null ) {
            throw new IllegalArgumentException( "BioAssay was null or had no name : " + bioAssay );
        }
        BioAssay newBioAssay = find( bioAssay );
        if ( newBioAssay != null ) {
            if ( log.isDebugEnabled() ) log.debug( "Found existing bioAssay: " + newBioAssay );
            return newBioAssay;
        }
        if ( log.isDebugEnabled() ) log.debug( "Creating new bioAssay: " + bioAssay );
        return create( bioAssay );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.bioAssay.BioAssayDaoBase#handleThaw(ubic.gemma.model.expression.bioAssay.BioAssay)
     */
    public void handleThaw( final BioAssay bioAssay ) throws Exception {
        HibernateTemplate templ = this.getHibernateTemplate();
        templ.execute( new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
            public Object doInHibernate( org.hibernate.Session session ) throws org.hibernate.HibernateException {
                session.buildLockRequest( LockOptions.NONE ).lock( bioAssay );
                Hibernate.initialize( bioAssay.getArrayDesignUsed() );
                Hibernate.initialize( bioAssay.getDerivedDataFiles() );
                for ( BioMaterial bm : bioAssay.getSamplesUsed() ) {
                    session.buildLockRequest( LockOptions.NONE ).lock( bm );
                    Hibernate.initialize( bm );
                    Hibernate.initialize( bm.getBioAssaysUsedIn() );
                    Hibernate.initialize( bm.getFactorValues() );
                    session.evict( bm );
                }
                session.evict( bioAssay );
                return null;
            }
        } );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.bioAssay.BioAssayDao#thaw(java.util.Collection)
     */
    @SuppressWarnings("unchecked")
    public Collection<BioAssay> thaw( Collection<BioAssay> bioAssays ) {
        if ( bioAssays.isEmpty() ) return bioAssays;
        List<?> thawedBioassays = this.getHibernateTemplate().findByNamedParam(
                "select distinct b from BioAssayImpl b left join fetch b.arrayDesignUsed"
                        + " left join fetch b.derivedDataFiles left join fetch b.samplesUsed bm"
                        + " left join bm.factorValues left join bm.bioAssaysUsedIn left join fetch "
                        + " b.auditTrail at left join fetch at.events where b.id in (:ids) ", "ids",
                EntityUtils.getIds( bioAssays ) );
        return ( Collection<BioAssay> ) thawedBioassays;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.bioAssay.BioAssayDaoBase#handleCountAll()
     */
    protected Integer handleCountAll() throws Exception {
        final String query = "select count(*) from BioAssayImpl";
        try {
            org.hibernate.Query queryObject = super.getSession().createQuery( query );
            queryObject.setCacheable( true );
            return ( ( Long ) queryObject.iterate().next() ).intValue();
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    
    /**
     * @see ubic.gemma.model.expression.bioAssay.BioAssayDao#countAll()
     */
    public java.lang.Integer countAll() {
        try {
            return this.handleCountAll();
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.expression.bioAssay.BioAssayDao.countAll()' --> " + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.bioAssay.BioAssayDao#create(int, java.util.Collection)
     */
    public java.util.Collection<BioAssay> create( final int transform,
            final java.util.Collection<? extends BioAssay> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "BioAssay.create - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator<? extends BioAssay> entityIterator = entities.iterator(); entityIterator
                                .hasNext(); ) {
                            create( transform, entityIterator.next() );
                        }
                        return null;
                    }
                } );
        return ( Collection<BioAssay> ) entities;
    }

    /**
     * @see ubic.gemma.model.expression.bioAssay.BioAssayDao#create(int transform,
     *      ubic.gemma.model.expression.bioAssay.BioAssay)
     */
    public BioAssay create( final int transform, final ubic.gemma.model.expression.bioAssay.BioAssay bioAssay ) {
        if ( bioAssay == null ) {
            throw new IllegalArgumentException( "BioAssay.create - 'bioAssay' can not be null" );
        }
        this.getHibernateTemplate().save( bioAssay );
        return ( BioAssay ) this.transformEntity( transform, bioAssay );
    }

    /**
     * @see ubic.gemma.model.expression.bioAssay.BioAssayDao#create(java.util.Collection)
     */
    public java.util.Collection<BioAssay> create( final java.util.Collection<? extends BioAssay> entities ) {
        return create( TRANSFORM_NONE, entities );
    }

    /**
     * @see ubic.gemma.model.expression.bioAssay.BioAssayDao#create(ubic.gemma.model.expression.bioAssay.BioAssay)
     */
    public BioAssay create( ubic.gemma.model.expression.bioAssay.BioAssay bioAssay ) {
        return this.create( TRANSFORM_NONE, bioAssay );
    }

    /**
     * @see ubic.gemma.model.expression.bioAssay.BioAssayDao#find(int, java.lang.String,
     *      ubic.gemma.model.expression.bioAssay.BioAssay)
     */

    public BioAssay find( final int transform, final java.lang.String queryString,
            final ubic.gemma.model.expression.bioAssay.BioAssay bioAssay ) {
        java.util.List<String> argNames = new java.util.ArrayList<String>();
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add( bioAssay );
        argNames.add( "bioAssay" );
        java.util.Set results = new java.util.LinkedHashSet( this.getHibernateTemplate().findByNamedParam( queryString,
                argNames.toArray( new String[argNames.size()] ), args.toArray() ) );
        Object result = null;

        if ( results.size() > 1 ) {
            throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                    "More than one instance of 'ubic.gemma.model.expression.bioAssay.BioAssay"
                            + "' was found when executing query --> '" + queryString + "'" );
        } else if ( results.size() == 1 ) {
            result = results.iterator().next();
        }

        result = transformEntity( transform, ( ubic.gemma.model.expression.bioAssay.BioAssay ) result );
        return ( BioAssay ) result;
    }

    /**
     * @see ubic.gemma.model.expression.bioAssay.BioAssayDao#find(int, ubic.gemma.model.expression.bioAssay.BioAssay)
     */
    public BioAssay find( final int transform, final ubic.gemma.model.expression.bioAssay.BioAssay bioAssay ) {
        return this
                .find(
                        transform,
                        "from DatabaseEntryImpl where accession=databaseEntry.accession and externalDatabase=databaseEntry.externalDatabase",
                        bioAssay );
    }

    /**
     * @see ubic.gemma.model.expression.bioAssay.BioAssayDao#find(java.lang.String,
     *      ubic.gemma.model.expression.bioAssay.BioAssay)
     */
    public ubic.gemma.model.expression.bioAssay.BioAssay find( final java.lang.String queryString,
            final ubic.gemma.model.expression.bioAssay.BioAssay bioAssay ) {
        return this.find( TRANSFORM_NONE, queryString, bioAssay );
    }


    /**
     * @see ubic.gemma.model.expression.bioAssay.BioAssayDao#findBioAssayDimensions(int, java.lang.String,
     *      ubic.gemma.model.expression.bioAssay.BioAssay)
     */

    public java.util.Collection<BioAssayDimension> findBioAssayDimensions( final int transform,
            final java.lang.String queryString, final ubic.gemma.model.expression.bioAssay.BioAssay bioAssay ) {
        java.util.List<String> argNames = new java.util.ArrayList<String>();
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add( bioAssay );
        argNames.add( "bioAssay" );
        java.util.List results = this.getHibernateTemplate().findByNamedParam( queryString,
                argNames.toArray( new String[argNames.size()] ), args.toArray() );
        transformEntities( transform, results );
        return results;
    }

    /**
     * @see ubic.gemma.model.expression.bioAssay.BioAssayDao#findBioAssayDimensions(int,
     *      ubic.gemma.model.expression.bioAssay.BioAssay)
     */
    public java.util.Collection<BioAssayDimension> findBioAssayDimensions( final int transform,
            final ubic.gemma.model.expression.bioAssay.BioAssay bioAssay ) {
        return this.findBioAssayDimensions( transform,
                "select bad from BioAssayDimensionImpl bad inner join bad.bioAssays as ba where :bioAssay in ba",
                bioAssay );
    }

    /**
     * @see ubic.gemma.model.expression.bioAssay.BioAssayDao#findBioAssayDimensions(java.lang.String,
     *      ubic.gemma.model.expression.bioAssay.BioAssay)
     */
    public java.util.Collection<BioAssayDimension> findBioAssayDimensions( final java.lang.String queryString,
            final ubic.gemma.model.expression.bioAssay.BioAssay bioAssay ) {
        return this.findBioAssayDimensions( TRANSFORM_NONE, queryString, bioAssay );
    }

    /**
     * @see ubic.gemma.model.expression.bioAssay.BioAssayDao#findBioAssayDimensions(ubic.gemma.model.expression.bioAssay.BioAssay)
     */
    public java.util.Collection<BioAssayDimension> findBioAssayDimensions(
            ubic.gemma.model.expression.bioAssay.BioAssay bioAssay ) {
        return this.findBioAssayDimensions( TRANSFORM_NONE, bioAssay );
    }

    public BioAssay findOrCreate( int transform, BioAssay bioAssay ) {
        return this.findOrCreate( ( BioAssay ) this.transformEntity( transform, bioAssay ) );
    }

    /**
     * @see ubic.gemma.model.expression.bioAssay.BioAssayDao#findOrCreate(int, java.lang.String,
     *      ubic.gemma.model.expression.bioAssay.BioAssay)
     */

    public BioAssay findOrCreate( final int transform, final java.lang.String queryString,
            final ubic.gemma.model.expression.bioAssay.BioAssay bioAssay ) {
        java.util.List<String> argNames = new java.util.ArrayList<String>();
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add( bioAssay );
        argNames.add( "bioAssay" );
        java.util.Set results = new java.util.LinkedHashSet( this.getHibernateTemplate().findByNamedParam( queryString,
                argNames.toArray( new String[argNames.size()] ), args.toArray() ) );
        Object result = null;
        if ( results.size() > 1 ) {
            throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                    "More than one instance of 'ubic.gemma.model.expression.bioAssay.BioAssay"
                            + "' was found when executing query --> '" + queryString + "'" );
        } else if ( results.size() == 1 ) {
            result = results.iterator().next();
        }
        result = transformEntity( transform, ( ubic.gemma.model.expression.bioAssay.BioAssay ) result );
        return ( BioAssay ) result;
    }

    /**
     * @see ubic.gemma.model.expression.bioAssay.BioAssayDao#findOrCreate(java.lang.String,
     *      ubic.gemma.model.expression.bioAssay.BioAssay)
     */
    public ubic.gemma.model.expression.bioAssay.BioAssay findOrCreate( final java.lang.String queryString,
            final ubic.gemma.model.expression.bioAssay.BioAssay bioAssay ) {
        return this.findOrCreate( TRANSFORM_NONE, queryString, bioAssay );
    }

    public Collection<? extends BioAssay> load( Collection<Long> ids ) {
        return this.getHibernateTemplate().findByNamedParam( "from BioAssayImpl where id in (:ids)", "ids", ids );
    }

    /**
     * @see ubic.gemma.model.expression.bioAssay.BioAssayDao#load(int, java.lang.Long)
     */
    public BioAssay load( final int transform, final java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "BioAssay.load - 'id' can not be null" );
        }
        final Object entity = this.getHibernateTemplate().get( ubic.gemma.model.expression.bioAssay.BioAssayImpl.class,
                id );
        return ( BioAssay ) transformEntity( transform, ( ubic.gemma.model.expression.bioAssay.BioAssay ) entity );
    }

    /**
     * @see ubic.gemma.model.expression.bioAssay.BioAssayDao#load(java.lang.Long)
     */
    public BioAssay load( java.lang.Long id ) {
        return this.load( TRANSFORM_NONE, id );
    }

    /**
     * @see ubic.gemma.model.expression.bioAssay.BioAssayDao#loadAll()
     */
    public java.util.Collection<? extends BioAssay> loadAll() {
        return this.loadAll( TRANSFORM_NONE );
    }

    /**
     * @see ubic.gemma.model.expression.bioAssay.BioAssayDao#loadAll(int)
     */

    public java.util.Collection<? extends BioAssay> loadAll( final int transform ) {
        final java.util.Collection<? extends BioAssay> results = this.getHibernateTemplate().loadAll(
                ubic.gemma.model.expression.bioAssay.BioAssayImpl.class );
        this.transformEntities( transform, results );
        return results;
    }

    /**
     * @see ubic.gemma.model.expression.bioAssay.BioAssayDao#remove(java.lang.Long)
     */
    public void remove( java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "BioAssay.remove - 'id' can not be null" );
        }
        ubic.gemma.model.expression.bioAssay.BioAssay entity = this.load( id );
        if ( entity != null ) {
            this.remove( entity );
        }
    }

    /**
     * @see ubic.gemma.model.common.SecurableDao#remove(java.util.Collection)
     */
    public void remove( java.util.Collection<? extends BioAssay> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "BioAssay.remove - 'entities' can not be null" );
        }
        this.getHibernateTemplate().deleteAll( entities );
    }

    /**
     * @see ubic.gemma.model.expression.bioAssay.BioAssayDao#remove(ubic.gemma.model.expression.bioAssay.BioAssay)
     */
    public void remove( ubic.gemma.model.expression.bioAssay.BioAssay bioAssay ) {
        if ( bioAssay == null ) {
            throw new IllegalArgumentException( "BioAssay.remove - 'bioAssay' can not be null" );
        }
        this.getHibernateTemplate().delete( bioAssay );
    }

    /**
     * @see ubic.gemma.model.expression.bioAssay.BioAssayDao#thaw(ubic.gemma.model.expression.bioAssay.BioAssay)
     */
    public void thaw( final ubic.gemma.model.expression.bioAssay.BioAssay bioAssay ) {
        try {
            this.handleThaw( bioAssay );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.expression.bioAssay.BioAssayDao.thaw(ubic.gemma.model.expression.bioAssay.BioAssay bioAssay)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.common.SecurableDao#update(java.util.Collection)
     */

    public void update( final java.util.Collection<? extends BioAssay> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "BioAssay.update - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator<? extends BioAssay> entityIterator = entities.iterator(); entityIterator
                                .hasNext(); ) {
                            update( entityIterator.next() );
                        }
                        return null;
                    }
                } );
    }

    /**
     * @see ubic.gemma.model.expression.bioAssay.BioAssayDao#update(ubic.gemma.model.expression.bioAssay.BioAssay)
     */
    public void update( ubic.gemma.model.expression.bioAssay.BioAssay bioAssay ) {
        if ( bioAssay == null ) {
            throw new IllegalArgumentException( "BioAssay.update - 'bioAssay' can not be null" );
        }
        this.getHibernateTemplate().update( bioAssay );
    }

    /**
     * Transforms a collection of entities using the
     * {@link #transformEntity(int,ubic.gemma.model.expression.bioAssay.BioAssay)} method. This method does not
     * instantiate a new collection.
     * <p/>
     * This method is to be used internally only.
     * 
     * @param transform one of the constants declared in <code>ubic.gemma.model.expression.bioAssay.BioAssayDao</code>
     * @param entities the collection of entities to transform
     * @return the same collection as the argument, but this time containing the transformed entities
     * @see #transformEntity(int,ubic.gemma.model.expression.bioAssay.BioAssay)
     */

    protected void transformEntities( final int transform, final java.util.Collection<? extends BioAssay> entities ) {
        switch ( transform ) {
            case TRANSFORM_NONE: // fall-through
            default:
                // do nothing;
        }
    }

    /**
     * Allows transformation of entities into value objects (or something else for that matter), when the
     * <code>transform</code> flag is set to one of the constants defined in
     * <code>ubic.gemma.model.expression.bioAssay.BioAssayDao</code>, please note that the {@link #TRANSFORM_NONE}
     * constant denotes no transformation, so the entity itself will be returned. If the integer argument value is
     * unknown {@link #TRANSFORM_NONE} is assumed.
     * 
     * @param transform one of the constants declared in {@link ubic.gemma.model.expression.bioAssay.BioAssayDao}
     * @param entity an entity that was found
     * @return the transformed entity (i.e. new value object, etc)
     * @see #transformEntities(int,java.util.Collection)
     */
    protected Object transformEntity( final int transform, final ubic.gemma.model.expression.bioAssay.BioAssay entity ) {
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