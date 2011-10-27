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
package ubic.gemma.model.genome;

import java.util.Collection;

import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

/**
 * <p>
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.genome.ProbeAlignedRegion</code>.
 * </p>
 * 
 * @see ubic.gemma.model.genome.ProbeAlignedRegion
 */
public abstract class ProbeAlignedRegionDaoBase extends HibernateDaoSupport implements
        ubic.gemma.model.genome.ProbeAlignedRegionDao {

    /**
     * @see ubic.gemma.model.genome.ProbeAlignedRegionDao#create(int, java.util.Collection)
     */

    public java.util.Collection<? extends ProbeAlignedRegion> create(
            final java.util.Collection<? extends ProbeAlignedRegion> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "ProbeAlignedRegion.create - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator<? extends ProbeAlignedRegion> entityIterator = entities.iterator(); entityIterator
                                .hasNext(); ) {
                            create( entityIterator.next() );
                        }
                        return null;
                    }
                } );
        return entities;
    }

    /**
     * @see ubic.gemma.model.genome.ProbeAlignedRegionDao#create(int transform,
     *      ubic.gemma.model.genome.ProbeAlignedRegion)
     */
    public ProbeAlignedRegion create( final ubic.gemma.model.genome.ProbeAlignedRegion probeAlignedRegion ) {
        if ( probeAlignedRegion == null ) {
            throw new IllegalArgumentException( "ProbeAlignedRegion.create - 'probeAlignedRegion' can not be null" );
        }
        this.getHibernateTemplate().save( probeAlignedRegion );
        return ( ProbeAlignedRegion ) probeAlignedRegion;
    }

    /**
     * @see ubic.gemma.model.genome.ProbeAlignedRegionDao#find(int, java.lang.String, ubic.gemma.model.genome.Gene)
     */

    public ProbeAlignedRegion find( final java.lang.String queryString, final ubic.gemma.model.genome.Gene gene ) {
        java.util.List<String> argNames = new java.util.ArrayList<String>();
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add( gene );
        argNames.add( "gene" );
        java.util.Set results = new java.util.LinkedHashSet( this.getHibernateTemplate().findByNamedParam( queryString,
                argNames.toArray( new String[argNames.size()] ), args.toArray() ) );
        Object result = null;

        if ( results.size() > 1 ) {
            throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                    "More than one instance of 'ubic.gemma.model.genome.Gene"
                            + "' was found when executing query --> '" + queryString + "'" );
        } else if ( results.size() == 1 ) {
            result = results.iterator().next();
        }

        return ( ProbeAlignedRegion ) result;
    }

    /**
     * @see ubic.gemma.model.genome.ProbeAlignedRegionDao#find(int, java.lang.String,
     *      ubic.gemma.model.genome.sequenceAnalysis.BlatResult)
     */

    public java.util.Collection<ProbeAlignedRegion> find( final java.lang.String queryString,
            final ubic.gemma.model.genome.sequenceAnalysis.BlatResult blatResult ) {
        java.util.List<String> argNames = new java.util.ArrayList<String>();
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add( blatResult );
        argNames.add( "blatResult" );
        java.util.List results = this.getHibernateTemplate().findByNamedParam( queryString,
                argNames.toArray( new String[argNames.size()] ), args.toArray() );
        return results;
    }

    /**
     * @see ubic.gemma.model.genome.ProbeAlignedRegionDao#find(int, ubic.gemma.model.genome.Gene)
     */
    public ProbeAlignedRegion find( final ubic.gemma.model.genome.Gene gene ) {
        return this
                .find( "from ubic.gemma.model.genome.ProbeAlignedRegion as probeAlignedRegion where probeAlignedRegion.gene = :gene",
                        gene );
    }

    /**
     * @see ubic.gemma.model.genome.ProbeAlignedRegionDao#find(int, ubic.gemma.model.genome.sequenceAnalysis.BlatResult)
     */
    public java.util.Collection<ProbeAlignedRegion> find(
            final ubic.gemma.model.genome.sequenceAnalysis.BlatResult blatResult ) {
        return this
                .find( "from ubic.gemma.model.genome.ProbeAlignedRegion as probeAlignedRegion where probeAlignedRegion.blatResult = :blatResult",
                        blatResult );
    }

    /**
     * @see ubic.gemma.model.genome.ProbeAlignedRegionDao#findByNcbiId(int, java.lang.String)
     */
    public java.util.Collection<ProbeAlignedRegion> findByNcbiId( final java.lang.String ncbiId ) {
        return this.findByNcbiId( "from GeneImpl g where g.ncbiId = :ncbiId", ncbiId );
    }

    /**
     * @see ubic.gemma.model.genome.ProbeAlignedRegionDao#findByNcbiId(int, java.lang.String, java.lang.String)
     */

    public java.util.Collection<ProbeAlignedRegion> findByNcbiId( final java.lang.String queryString,
            final java.lang.String ncbiId ) {
        java.util.List<String> argNames = new java.util.ArrayList<String>();
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add( ncbiId );
        argNames.add( "ncbiId" );
        java.util.List results = this.getHibernateTemplate().findByNamedParam( queryString,
                argNames.toArray( new String[argNames.size()] ), args.toArray() );
        return results;
    }

    /**
     * @see ubic.gemma.model.genome.ProbeAlignedRegionDao#findByOfficalSymbol(int, java.lang.String)
     */
    public java.util.Collection<ProbeAlignedRegion> findByOfficalSymbol( final java.lang.String officialSymbol ) {
        return this.findByOfficalSymbol(
                "from GeneImpl g where g.officialSymbol=:officialSymbol order by g.officialName", officialSymbol );
    }

    /**
     * @see ubic.gemma.model.genome.ProbeAlignedRegionDao#findByOfficalSymbol(int, java.lang.String, java.lang.String)
     */

    public java.util.Collection<ProbeAlignedRegion> findByOfficalSymbol( final java.lang.String queryString,
            final java.lang.String officialSymbol ) {
        java.util.List<String> argNames = new java.util.ArrayList<String>();
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add( officialSymbol );
        argNames.add( "officialSymbol" );
        java.util.List results = this.getHibernateTemplate().findByNamedParam( queryString,
                argNames.toArray( new String[argNames.size()] ), args.toArray() );
        return results;
    }

    /**
     * @see ubic.gemma.model.genome.ProbeAlignedRegionDao#findByOfficialName(int, java.lang.String)
     */
    public java.util.Collection<ProbeAlignedRegion> findByOfficialName( final java.lang.String officialName ) {
        return this.findByOfficialName( "from GeneImpl g where g.officialName=:officialName order by g.officialName",
                officialName );
    }

    /**
     * @see ubic.gemma.model.genome.ProbeAlignedRegionDao#findByOfficialName(int, java.lang.String, java.lang.String)
     */

    public java.util.Collection<ProbeAlignedRegion> findByOfficialName( final java.lang.String queryString,
            final java.lang.String officialName ) {
        java.util.List<String> argNames = new java.util.ArrayList<String>();
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add( officialName );
        argNames.add( "officialName" );
        java.util.List results = this.getHibernateTemplate().findByNamedParam( queryString,
                argNames.toArray( new String[argNames.size()] ), args.toArray() );
        return results;
    }

    /**
     * @see ubic.gemma.model.genome.ProbeAlignedRegionDao#findByOfficialSymbolInexact(int, java.lang.String)
     */
    public java.util.Collection<ProbeAlignedRegion> findByOfficialSymbolInexact( final java.lang.String officialSymbol ) {
        return this
                .findByOfficialSymbolInexact(
                        "from GeneImpl g where g.officialSymbol like :officialSymbol order by g.officialSymbol",
                        officialSymbol );
    }

    /**
     * @see ubic.gemma.model.genome.ProbeAlignedRegionDao#findByOfficialSymbolInexact(int, java.lang.String,
     *      java.lang.String)
     */

    public java.util.Collection<ProbeAlignedRegion> findByOfficialSymbolInexact( final java.lang.String queryString,
            final java.lang.String officialSymbol ) {
        java.util.List<String> argNames = new java.util.ArrayList<String>();
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add( officialSymbol );
        argNames.add( "officialSymbol" );
        java.util.List results = this.getHibernateTemplate().findByNamedParam( queryString,
                argNames.toArray( new String[argNames.size()] ), args.toArray() );
        return results;
    }

    /**
     * @see ubic.gemma.model.genome.ProbeAlignedRegionDao#findByPhysicalLocation(int, java.lang.String,
     *      ubic.gemma.model.genome.PhysicalLocation)
     */

    public java.util.Collection<ProbeAlignedRegion> findByPhysicalLocation( final java.lang.String queryString,
            final ubic.gemma.model.genome.PhysicalLocation location ) {
        java.util.List<String> argNames = new java.util.ArrayList<String>();
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add( location );
        argNames.add( "location" );
        java.util.List results = this.getHibernateTemplate().findByNamedParam( queryString,
                argNames.toArray( new String[argNames.size()] ), args.toArray() );
        return results;
    }

    /**
     * @see ubic.gemma.model.genome.ProbeAlignedRegionDao#findByPhysicalLocation(int,
     *      ubic.gemma.model.genome.PhysicalLocation)
     */
    public java.util.Collection<ProbeAlignedRegion> findByPhysicalLocation(
            final ubic.gemma.model.genome.PhysicalLocation location ) {
        return this
                .findByPhysicalLocation(

                        "from ubic.gemma.model.genome.ProbeAlignedRegion as probeAlignedRegion where probeAlignedRegion.location = :location",
                        location );
    }

    /**
     * @see ubic.gemma.model.genome.ProbeAlignedRegionDao#findOrCreate(int, java.lang.String,
     *      ubic.gemma.model.genome.Gene)
     */

    public ProbeAlignedRegion findOrCreate( final java.lang.String queryString, final ubic.gemma.model.genome.Gene gene ) {
        java.util.List<String> argNames = new java.util.ArrayList<String>();
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add( gene );
        argNames.add( "gene" );
        java.util.Set results = new java.util.LinkedHashSet( this.getHibernateTemplate().findByNamedParam( queryString,
                argNames.toArray( new String[argNames.size()] ), args.toArray() ) );
        ProbeAlignedRegion result = null;

        if ( results.size() > 1 ) {
            throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                    "More than one instance of 'ubic.gemma.model.genome.Gene"
                            + "' was found when executing query --> '" + queryString + "'" );
        } else if ( results.size() == 1 ) {
            result = ( ProbeAlignedRegion ) results.iterator().next();
        }

        return result;
    }

    /**
     * @see ubic.gemma.model.genome.ProbeAlignedRegionDao#findOrCreate(int, ubic.gemma.model.genome.Gene)
     */
    public Object findOrCreate( final ubic.gemma.model.genome.Gene gene ) {
        return this.findOrCreate(

        "from ubic.gemma.model.genome.ProbeAlignedRegion as probeAlignedRegion where probeAlignedRegion.gene = :gene",
                gene );
    }

    public Collection<? extends ProbeAlignedRegion> load( Collection<Long> ids ) {
        return this.getHibernateTemplate().findByNamedParam( "from ProbeAlignedRegionImpl where id in (:ids)", "ids",
                ids );
    }

    /**
     * @see ubic.gemma.model.genome.ProbeAlignedRegionDao#load(int, java.lang.Long)
     */

    public ProbeAlignedRegion load( final java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "ProbeAlignedRegion.load - 'id' can not be null" );
        }
        final Object entity = this.getHibernateTemplate()
                .get( ubic.gemma.model.genome.ProbeAlignedRegionImpl.class, id );
        return ( ubic.gemma.model.genome.ProbeAlignedRegion ) entity;
    }

    /**
     * @see ubic.gemma.model.genome.ProbeAlignedRegionDao#loadAll(int)
     */

    public java.util.Collection<? extends ProbeAlignedRegion> loadAll() {
        final java.util.Collection<? extends ProbeAlignedRegion> results = this.getHibernateTemplate().loadAll(
                ubic.gemma.model.genome.ProbeAlignedRegionImpl.class );
        return results;
    }

    /**
     * @see ubic.gemma.model.genome.ProbeAlignedRegionDao#remove(java.lang.Long)
     */

    public void remove( java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "ProbeAlignedRegion.remove - 'id' can not be null" );
        }
        ubic.gemma.model.genome.ProbeAlignedRegion entity = this.load( id );
        if ( entity != null ) {
            this.remove( entity );
        }
    }

    /**
     * @see ubic.gemma.model.common.SecurableDao#remove(java.util.Collection)
     */

    public void remove( java.util.Collection<? extends ProbeAlignedRegion> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "ProbeAlignedRegion.remove - 'entities' can not be null" );
        }
        this.getHibernateTemplate().deleteAll( entities );
    }

    /**
     * @see ubic.gemma.model.genome.ProbeAlignedRegionDao#remove(ubic.gemma.model.genome.ProbeAlignedRegion)
     */
    public void remove( ubic.gemma.model.genome.ProbeAlignedRegion probeAlignedRegion ) {
        if ( probeAlignedRegion == null ) {
            throw new IllegalArgumentException( "ProbeAlignedRegion.remove - 'probeAlignedRegion' can not be null" );
        }
        this.getHibernateTemplate().delete( probeAlignedRegion );
    }

    /**
     * @see ubic.gemma.model.common.SecurableDao#update(java.util.Collection)
     */

    public void update( final java.util.Collection<? extends ProbeAlignedRegion> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "ProbeAlignedRegion.update - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator<? extends ProbeAlignedRegion> entityIterator = entities.iterator(); entityIterator
                                .hasNext(); ) {
                            update( entityIterator.next() );
                        }
                        return null;
                    }
                } );
    }

    /**
     * @see ubic.gemma.model.genome.ProbeAlignedRegionDao#update(ubic.gemma.model.genome.ProbeAlignedRegion)
     */
    public void update( ubic.gemma.model.genome.ProbeAlignedRegion probeAlignedRegion ) {
        if ( probeAlignedRegion == null ) {
            throw new IllegalArgumentException( "ProbeAlignedRegion.update - 'probeAlignedRegion' can not be null" );
        }
        this.getHibernateTemplate().update( probeAlignedRegion );
    }

}