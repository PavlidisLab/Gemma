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

import ubic.gemma.model.genome.gene.GeneValueObject;

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

    protected final org.apache.commons.collections.Transformer GeneValueObjectToEntityTransformer = new org.apache.commons.collections.Transformer() {
        public Object transform( Object input ) {
            return geneValueObjectToEntity( ( ubic.gemma.model.genome.gene.GeneValueObject ) input );
        }
    };

    /**
     * @see ubic.gemma.model.genome.ProbeAlignedRegionDao#create(int, java.util.Collection)
     */

    public java.util.Collection<? extends ProbeAlignedRegion> create( final int transform,
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
                            create( transform, entityIterator.next() );
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
    public ProbeAlignedRegion create( final int transform,
            final ubic.gemma.model.genome.ProbeAlignedRegion probeAlignedRegion ) {
        if ( probeAlignedRegion == null ) {
            throw new IllegalArgumentException( "ProbeAlignedRegion.create - 'probeAlignedRegion' can not be null" );
        }
        this.getHibernateTemplate().save( probeAlignedRegion );
        return ( ProbeAlignedRegion ) this.transformEntity( transform, probeAlignedRegion );
    }

    /**
     * @see ubic.gemma.model.genome.ProbeAlignedRegionDao#create(java.util.Collection)
     */
    public java.util.Collection<? extends ProbeAlignedRegion> create(
            final java.util.Collection<? extends ProbeAlignedRegion> entities ) {
        return create( TRANSFORM_NONE, entities );
    }

    /**
     * @see ubic.gemma.model.genome.ProbeAlignedRegionDao#create(ubic.gemma.model.genome.ProbeAlignedRegion)
     */
    public ProbeAlignedRegion create( ubic.gemma.model.genome.ProbeAlignedRegion probeAlignedRegion ) {
        return this.create( TRANSFORM_NONE, probeAlignedRegion );
    }

    /**
     * @see ubic.gemma.model.genome.ProbeAlignedRegionDao#find(int, java.lang.String, ubic.gemma.model.genome.Gene)
     */

    public ProbeAlignedRegion find( final int transform, final java.lang.String queryString,
            final ubic.gemma.model.genome.Gene gene ) {
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

        result = transformEntity( transform, ( ubic.gemma.model.genome.ProbeAlignedRegion ) result );
        return ( ProbeAlignedRegion ) result;
    }

    /**
     * @see ubic.gemma.model.genome.ProbeAlignedRegionDao#find(int, java.lang.String,
     *      ubic.gemma.model.genome.sequenceAnalysis.BlatResult)
     */

    public java.util.Collection<ProbeAlignedRegion> find( final int transform, final java.lang.String queryString,
            final ubic.gemma.model.genome.sequenceAnalysis.BlatResult blatResult ) {
        java.util.List<String> argNames = new java.util.ArrayList<String>();
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add( blatResult );
        argNames.add( "blatResult" );
        java.util.List results = this.getHibernateTemplate().findByNamedParam( queryString,
                argNames.toArray( new String[argNames.size()] ), args.toArray() );
        transformEntities( transform, results );
        return results;
    }

    /**
     * @see ubic.gemma.model.genome.ProbeAlignedRegionDao#find(int, ubic.gemma.model.genome.Gene)
     */
    public ProbeAlignedRegion find( final int transform, final ubic.gemma.model.genome.Gene gene ) {
        return this
                .find(
                        transform,
                        "from ubic.gemma.model.genome.ProbeAlignedRegion as probeAlignedRegion where probeAlignedRegion.gene = :gene",
                        gene );
    }

    /**
     * @see ubic.gemma.model.genome.ProbeAlignedRegionDao#find(int, ubic.gemma.model.genome.sequenceAnalysis.BlatResult)
     */
    public java.util.Collection<ProbeAlignedRegion> find( final int transform,
            final ubic.gemma.model.genome.sequenceAnalysis.BlatResult blatResult ) {
        return this
                .find(
                        transform,
                        "from ubic.gemma.model.genome.ProbeAlignedRegion as probeAlignedRegion where probeAlignedRegion.blatResult = :blatResult",
                        blatResult );
    }

    /**
     * @see ubic.gemma.model.genome.ProbeAlignedRegionDao#find(java.lang.String, ubic.gemma.model.genome.Gene)
     */
    public ubic.gemma.model.genome.Gene find( final java.lang.String queryString,
            final ubic.gemma.model.genome.Gene gene ) {
        return this.find( TRANSFORM_NONE, queryString, gene );
    }

    /**
     * @see ubic.gemma.model.genome.ProbeAlignedRegionDao#find(java.lang.String,
     *      ubic.gemma.model.genome.sequenceAnalysis.BlatResult)
     */
    public java.util.Collection<ProbeAlignedRegion> find( final java.lang.String queryString,
            final ubic.gemma.model.genome.sequenceAnalysis.BlatResult blatResult ) {
        return this.find( TRANSFORM_NONE, queryString, blatResult );
    }

    /**
     * @see ubic.gemma.model.genome.ProbeAlignedRegionDao#find(ubic.gemma.model.genome.Gene)
     */

    public ubic.gemma.model.genome.Gene find( ubic.gemma.model.genome.Gene gene ) {
        return this.find( TRANSFORM_NONE, gene );
    }

    /**
     * @see ubic.gemma.model.genome.ProbeAlignedRegionDao#find(ubic.gemma.model.genome.sequenceAnalysis.BlatResult)
     */
    public java.util.Collection<ProbeAlignedRegion> find( ubic.gemma.model.genome.sequenceAnalysis.BlatResult blatResult ) {
        return this.find( TRANSFORM_NONE, blatResult );
    }

    /**
     * @see ubic.gemma.model.genome.ProbeAlignedRegionDao#findByNcbiId(int, java.lang.String)
     */
    public java.util.Collection<ProbeAlignedRegion> findByNcbiId( final int transform, final java.lang.String ncbiId ) {
        return this.findByNcbiId( transform, "from GeneImpl g where g.ncbiId = :ncbiId", ncbiId );
    }

    /**
     * @see ubic.gemma.model.genome.ProbeAlignedRegionDao#findByNcbiId(int, java.lang.String, java.lang.String)
     */

    public java.util.Collection<ProbeAlignedRegion> findByNcbiId( final int transform,
            final java.lang.String queryString, final java.lang.String ncbiId ) {
        java.util.List<String> argNames = new java.util.ArrayList<String>();
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add( ncbiId );
        argNames.add( "ncbiId" );
        java.util.List results = this.getHibernateTemplate().findByNamedParam( queryString,
                argNames.toArray( new String[argNames.size()] ), args.toArray() );
        transformEntities( transform, results );
        return results;
    }

    /**
     * @see ubic.gemma.model.genome.ProbeAlignedRegionDao#findByNcbiId(java.lang.String)
     */
    public java.util.Collection<ProbeAlignedRegion> findByNcbiId( java.lang.String ncbiId ) {
        return this.findByNcbiId( TRANSFORM_NONE, ncbiId );
    }

    /**
     * @see ubic.gemma.model.genome.ProbeAlignedRegionDao#findByNcbiId(java.lang.String, java.lang.String)
     */
    public java.util.Collection<ProbeAlignedRegion> findByNcbiId( final java.lang.String queryString,
            final java.lang.String ncbiId ) {
        return this.findByNcbiId( TRANSFORM_NONE, queryString, ncbiId );
    }

    /**
     * @see ubic.gemma.model.genome.ProbeAlignedRegionDao#findByOfficalSymbol(int, java.lang.String)
     */
    public java.util.Collection<ProbeAlignedRegion> findByOfficalSymbol( final int transform,
            final java.lang.String officialSymbol ) {
        return this.findByOfficalSymbol( transform,
                "from GeneImpl g where g.officialSymbol=:officialSymbol order by g.officialName", officialSymbol );
    }

    /**
     * @see ubic.gemma.model.genome.ProbeAlignedRegionDao#findByOfficalSymbol(int, java.lang.String, java.lang.String)
     */

    public java.util.Collection<ProbeAlignedRegion> findByOfficalSymbol( final int transform,
            final java.lang.String queryString, final java.lang.String officialSymbol ) {
        java.util.List<String> argNames = new java.util.ArrayList<String>();
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add( officialSymbol );
        argNames.add( "officialSymbol" );
        java.util.List results = this.getHibernateTemplate().findByNamedParam( queryString,
                argNames.toArray( new String[argNames.size()] ), args.toArray() );
        transformEntities( transform, results );
        return results;
    }

    /**
     * @see ubic.gemma.model.genome.ProbeAlignedRegionDao#findByOfficalSymbol(java.lang.String)
     */

    public java.util.Collection<ProbeAlignedRegion> findByOfficalSymbol( java.lang.String officialSymbol ) {
        return this.findByOfficalSymbol( TRANSFORM_NONE, officialSymbol );
    }

    /**
     * @see ubic.gemma.model.genome.ProbeAlignedRegionDao#findByOfficalSymbol(java.lang.String, java.lang.String)
     */
    public java.util.Collection<ProbeAlignedRegion> findByOfficalSymbol( final java.lang.String queryString,
            final java.lang.String officialSymbol ) {
        return this.findByOfficalSymbol( TRANSFORM_NONE, queryString, officialSymbol );
    }

    /**
     * @see ubic.gemma.model.genome.ProbeAlignedRegionDao#findByOfficialName(int, java.lang.String)
     */
    public java.util.Collection<ProbeAlignedRegion> findByOfficialName( final int transform,
            final java.lang.String officialName ) {
        return this.findByOfficialName( transform,
                "from GeneImpl g where g.officialName=:officialName order by g.officialName", officialName );
    }

    /**
     * @see ubic.gemma.model.genome.ProbeAlignedRegionDao#findByOfficialName(int, java.lang.String, java.lang.String)
     */

    public java.util.Collection<ProbeAlignedRegion> findByOfficialName( final int transform,
            final java.lang.String queryString, final java.lang.String officialName ) {
        java.util.List<String> argNames = new java.util.ArrayList<String>();
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add( officialName );
        argNames.add( "officialName" );
        java.util.List results = this.getHibernateTemplate().findByNamedParam( queryString,
                argNames.toArray( new String[argNames.size()] ), args.toArray() );
        transformEntities( transform, results );
        return results;
    }

    /**
     * @see ubic.gemma.model.genome.ProbeAlignedRegionDao#findByOfficialName(java.lang.String)
     */

    public java.util.Collection<ProbeAlignedRegion> findByOfficialName( java.lang.String officialName ) {
        return this.findByOfficialName( TRANSFORM_NONE, officialName );
    }

    /**
     * @see ubic.gemma.model.genome.ProbeAlignedRegionDao#findByOfficialName(java.lang.String, java.lang.String)
     */
    public java.util.Collection<ProbeAlignedRegion> findByOfficialName( final java.lang.String queryString,
            final java.lang.String officialName ) {
        return this.findByOfficialName( TRANSFORM_NONE, queryString, officialName );
    }

    /**
     * @see ubic.gemma.model.genome.ProbeAlignedRegionDao#findByOfficialSymbolInexact(int, java.lang.String)
     */
    public java.util.Collection<ProbeAlignedRegion> findByOfficialSymbolInexact( final int transform,
            final java.lang.String officialSymbol ) {
        return this
                .findByOfficialSymbolInexact( transform,
                        "from GeneImpl g where g.officialSymbol like :officialSymbol order by g.officialSymbol",
                        officialSymbol );
    }

    /**
     * @see ubic.gemma.model.genome.ProbeAlignedRegionDao#findByOfficialSymbolInexact(int, java.lang.String,
     *      java.lang.String)
     */

    public java.util.Collection<ProbeAlignedRegion> findByOfficialSymbolInexact( final int transform,
            final java.lang.String queryString, final java.lang.String officialSymbol ) {
        java.util.List<String> argNames = new java.util.ArrayList<String>();
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add( officialSymbol );
        argNames.add( "officialSymbol" );
        java.util.List results = this.getHibernateTemplate().findByNamedParam( queryString,
                argNames.toArray( new String[argNames.size()] ), args.toArray() );
        transformEntities( transform, results );
        return results;
    }

    /**
     * @see ubic.gemma.model.genome.ProbeAlignedRegionDao#findByOfficialSymbolInexact(java.lang.String)
     */

    public java.util.Collection<ProbeAlignedRegion> findByOfficialSymbolInexact( java.lang.String officialSymbol ) {
        return this.findByOfficialSymbolInexact( TRANSFORM_NONE, officialSymbol );
    }

    /**
     * @see ubic.gemma.model.genome.ProbeAlignedRegionDao#findByOfficialSymbolInexact(java.lang.String,
     *      java.lang.String)
     */
    public java.util.Collection<ProbeAlignedRegion> findByOfficialSymbolInexact( final java.lang.String queryString,
            final java.lang.String officialSymbol ) {
        return this.findByOfficialSymbolInexact( TRANSFORM_NONE, queryString, officialSymbol );
    }

    /**
     * @see ubic.gemma.model.genome.ProbeAlignedRegionDao#findByPhysicalLocation(int, java.lang.String,
     *      ubic.gemma.model.genome.PhysicalLocation)
     */

    public java.util.Collection<ProbeAlignedRegion> findByPhysicalLocation( final int transform,
            final java.lang.String queryString, final ubic.gemma.model.genome.PhysicalLocation location ) {
        java.util.List<String> argNames = new java.util.ArrayList<String>();
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add( location );
        argNames.add( "location" );
        java.util.List results = this.getHibernateTemplate().findByNamedParam( queryString,
                argNames.toArray( new String[argNames.size()] ), args.toArray() );
        transformEntities( transform, results );
        return results;
    }

    /**
     * @see ubic.gemma.model.genome.ProbeAlignedRegionDao#findByPhysicalLocation(int,
     *      ubic.gemma.model.genome.PhysicalLocation)
     */
    public java.util.Collection<ProbeAlignedRegion> findByPhysicalLocation( final int transform,
            final ubic.gemma.model.genome.PhysicalLocation location ) {
        return this
                .findByPhysicalLocation(
                        transform,
                        "from ubic.gemma.model.genome.ProbeAlignedRegion as probeAlignedRegion where probeAlignedRegion.location = :location",
                        location );
    }

    /**
     * @see ubic.gemma.model.genome.ProbeAlignedRegionDao#findByPhysicalLocation(java.lang.String,
     *      ubic.gemma.model.genome.PhysicalLocation)
     */
    public java.util.Collection<ProbeAlignedRegion> findByPhysicalLocation( final java.lang.String queryString,
            final ubic.gemma.model.genome.PhysicalLocation location ) {
        return this.findByPhysicalLocation( TRANSFORM_NONE, queryString, location );
    }

    /**
     * @see ubic.gemma.model.genome.ProbeAlignedRegionDao#findByPhysicalLocation(ubic.gemma.model.genome.PhysicalLocation)
     */

    public java.util.Collection<ProbeAlignedRegion> findByPhysicalLocation(
            ubic.gemma.model.genome.PhysicalLocation location ) {
        return this.findByPhysicalLocation( TRANSFORM_NONE, location );
    }

    /**
     * @see ubic.gemma.model.genome.ProbeAlignedRegionDao#findOrCreate(int, java.lang.String,
     *      ubic.gemma.model.genome.Gene)
     */

    public ProbeAlignedRegion findOrCreate( final int transform, final java.lang.String queryString,
            final ubic.gemma.model.genome.Gene gene ) {
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

        result = ( ProbeAlignedRegion ) transformEntity( transform, result );
        return result;
    }

    /**
     * @see ubic.gemma.model.genome.ProbeAlignedRegionDao#findOrCreate(int, ubic.gemma.model.genome.Gene)
     */
    public Object findOrCreate( final int transform, final ubic.gemma.model.genome.Gene gene ) {
        return this
                .findOrCreate(
                        transform,
                        "from ubic.gemma.model.genome.ProbeAlignedRegion as probeAlignedRegion where probeAlignedRegion.gene = :gene",
                        gene );
    }

    /**
     * @see ubic.gemma.model.genome.ProbeAlignedRegionDao#findOrCreate(java.lang.String, ubic.gemma.model.genome.Gene)
     */
    public ubic.gemma.model.genome.Gene findOrCreate( final java.lang.String queryString,
            final ubic.gemma.model.genome.Gene gene ) {
        return this.findOrCreate( TRANSFORM_NONE, queryString, gene );
    }

    /**
     * @see ubic.gemma.model.genome.ProbeAlignedRegionDao#findOrCreate(ubic.gemma.model.genome.Gene)
     */

    public ubic.gemma.model.genome.Gene findOrCreate( ubic.gemma.model.genome.Gene gene ) {
        return ( ubic.gemma.model.genome.Gene ) this.findOrCreate( TRANSFORM_NONE, gene );
    }

    public abstract ProbeAlignedRegion geneValueObjectToEntity( GeneValueObject geneValueObject );

    /**
     * @see ubic.gemma.model.genome.GeneDao#geneValueObjectToEntity(ubic.gemma.model.genome.gene.GeneValueObject,
     *      ubic.gemma.model.genome.Gene)
     */
    public void geneValueObjectToEntity( ubic.gemma.model.genome.gene.GeneValueObject source,
            ubic.gemma.model.genome.ProbeAlignedRegion target, boolean copyIfNull ) {
        if ( copyIfNull || source.getOfficialSymbol() != null ) {
            target.setOfficialSymbol( source.getOfficialSymbol() );
        }
        if ( copyIfNull || source.getOfficialName() != null ) {
            target.setOfficialName( source.getOfficialName() );
        }
        if ( copyIfNull || source.getNcbiId() != null ) {
            target.setNcbiId( source.getNcbiId() );
        }
        if ( copyIfNull || source.getName() != null ) {
            target.setName( source.getName() );
        }
        if ( copyIfNull || source.getDescription() != null ) {
            target.setDescription( source.getDescription() );
        }
    }

    /**
     * @see ubic.gemma.model.genome.GeneDao#geneValueObjectToEntityCollection(java.util.Collection)
     */

    public final void geneValueObjectToEntityCollection( java.util.Collection instances ) {
        if ( instances != null ) {
            for ( final java.util.Iterator iterator = instances.iterator(); iterator.hasNext(); ) {
                // - remove an objects that are null or not of the correct instance
                if ( !( iterator.next() instanceof ubic.gemma.model.genome.gene.GeneValueObject ) ) {
                    iterator.remove();
                }
            }
            org.apache.commons.collections.CollectionUtils.transform( instances, GeneValueObjectToEntityTransformer );
        }
    }

    public Collection<? extends ProbeAlignedRegion> load( Collection<Long> ids ) {
        return this.getHibernateTemplate().findByNamedParam( "from ProbeAlignedRegionImpl where id in (:ids)", "ids",
                ids );
    }

    /**
     * @see ubic.gemma.model.genome.ProbeAlignedRegionDao#load(int, java.lang.Long)
     */

    public Object load( final int transform, final java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "ProbeAlignedRegion.load - 'id' can not be null" );
        }
        final Object entity = this.getHibernateTemplate()
                .get( ubic.gemma.model.genome.ProbeAlignedRegionImpl.class, id );
        return transformEntity( transform, ( ubic.gemma.model.genome.ProbeAlignedRegion ) entity );
    }

    /**
     * @see ubic.gemma.model.genome.ProbeAlignedRegionDao#load(java.lang.Long)
     */

    public ProbeAlignedRegion load( java.lang.Long id ) {
        return ( ubic.gemma.model.genome.ProbeAlignedRegion ) this.load( TRANSFORM_NONE, id );
    }

    /**
     * @see ubic.gemma.model.genome.ProbeAlignedRegionDao#loadAll()
     */
    public java.util.Collection<? extends ProbeAlignedRegion> loadAll() {
        return this.loadAll( TRANSFORM_NONE );
    }

    /**
     * @see ubic.gemma.model.genome.ProbeAlignedRegionDao#loadAll(int)
     */

    public java.util.Collection<? extends ProbeAlignedRegion> loadAll( final int transform ) {
        final java.util.Collection<? extends ProbeAlignedRegion> results = this.getHibernateTemplate().loadAll(
                ubic.gemma.model.genome.ProbeAlignedRegionImpl.class );
        this.transformEntities( transform, results );
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
     * @see ubic.gemma.model.genome.GeneDao#toGeneValueObject(ubic.gemma.model.genome.Gene)
     */
    public ubic.gemma.model.genome.gene.GeneValueObject toGeneValueObject(
            final ubic.gemma.model.genome.ProbeAlignedRegion entity ) {
        final ubic.gemma.model.genome.gene.GeneValueObject target = new ubic.gemma.model.genome.gene.GeneValueObject();
        toGeneValueObject( entity, target );
        return target;
    }

    /**
     * @see ubic.gemma.model.genome.GeneDao#toGeneValueObject(ubic.gemma.model.genome.Gene,
     *      ubic.gemma.model.genome.gene.GeneValueObject)
     */
    public void toGeneValueObject( ubic.gemma.model.genome.ProbeAlignedRegion source,
            ubic.gemma.model.genome.gene.GeneValueObject target ) {
        target.setId( source.getId() );
        target.setName( source.getName() );
        target.setNcbiId( source.getNcbiId() );
        target.setOfficialSymbol( source.getOfficialSymbol() );
        target.setOfficialName( source.getOfficialName() );
        target.setDescription( source.getDescription() );
        target.setAliases( GeneValueObject.getAliasStrings( source ) );
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

    /**
     * Transforms a collection of entities using the
     * {@link #transformEntity(int,ubic.gemma.model.genome.ProbeAlignedRegion)} method. This method does not instantiate
     * a new collection.
     * <p/>
     * This method is to be used internally only.
     * 
     * @param transform one of the constants declared in <code>ubic.gemma.model.genome.ProbeAlignedRegionDao</code>
     * @param entities the collection of entities to transform
     * @return the same collection as the argument, but this time containing the transformed entities
     * @see #transformEntity(int,ubic.gemma.model.genome.ProbeAlignedRegion)
     */

    protected void transformEntities( final int transform,
            final java.util.Collection<? extends ProbeAlignedRegion> entities ) {
        // no op, remove this
    }

    /**
     * Allows transformation of entities into value objects (or something else for that matter), when the
     * <code>transform</code> flag is set to one of the constants defined in
     * <code>ubic.gemma.model.genome.ProbeAlignedRegionDao</code>, please note that the {@link #TRANSFORM_NONE} constant
     * denotes no transformation, so the entity itself will be returned. If the integer argument value is unknown
     * {@link #TRANSFORM_NONE} is assumed.
     * 
     * @param transform one of the constants declared in {@link ubic.gemma.model.genome.ProbeAlignedRegionDao}
     * @param entity an entity that was found
     * @return the transformed entity (i.e. new value object, etc)
     * @see #transformEntities(int,java.util.Collection)
     */
    protected Object transformEntity( final int transform, final ubic.gemma.model.genome.ProbeAlignedRegion entity ) {
        Object target = null;
        if ( entity != null ) {
            switch ( transform ) {
                case ubic.gemma.model.genome.GeneDao.TRANSFORM_GENEVALUEOBJECT:
                    target = toGeneValueObject( entity );
                    break;
                case TRANSFORM_NONE: // fall-through
                default:
                    target = entity;
            }
        }
        return target;
    }

}