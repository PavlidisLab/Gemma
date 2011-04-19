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

import ubic.gemma.model.genome.gene.GeneValueObject;

/**
 * <p>
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.genome.PredictedGene</code>.
 * </p>
 * 
 * @see ubic.gemma.model.genome.PredictedGene
 */
public abstract class PredictedGeneDaoBase extends ubic.gemma.model.genome.ChromosomeFeatureDaoImpl<PredictedGene>
        implements ubic.gemma.model.genome.PredictedGeneDao {

    /**
     * This anonymous transformer is designed to transform entities or report query results (which result in an array of
     * objects) to {@link ubic.gemma.model.genome.gene.GeneValueObject} using the Jakarta Commons-Collections
     * Transformation API.
     */
    protected org.apache.commons.collections.Transformer GENEVALUEOBJECT_TRANSFORMER = new org.apache.commons.collections.Transformer() {
        public Object transform( Object input ) {
            Object result = null;
            if ( input instanceof ubic.gemma.model.genome.PredictedGene ) {
                result = toGeneValueObject( ( ubic.gemma.model.genome.PredictedGene ) input );
            } else if ( input instanceof Object[] ) {
                result = toGeneValueObject( ( Object[] ) input );
            }
            return result;
        }
    };

    protected final org.apache.commons.collections.Transformer GeneValueObjectToEntityTransformer = new org.apache.commons.collections.Transformer() {
        public Object transform( Object input ) {
            return geneValueObjectToEntity( ( ubic.gemma.model.genome.gene.GeneValueObject ) input );
        }
    };

    /**
     * @see ubic.gemma.model.genome.PredictedGeneDao#create(int, java.util.Collection)
     */

    public java.util.Collection create( final int transform, final java.util.Collection entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "PredictedGene.create - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator entityIterator = entities.iterator(); entityIterator.hasNext(); ) {
                            create( transform, ( ubic.gemma.model.genome.PredictedGene ) entityIterator.next() );
                        }
                        return null;
                    }
                } );
        return entities;
    }

    /**
     * @see ubic.gemma.model.genome.PredictedGeneDao#create(int transform, ubic.gemma.model.genome.PredictedGene)
     */
    public Object create( final int transform, final ubic.gemma.model.genome.PredictedGene predictedGene ) {
        if ( predictedGene == null ) {
            throw new IllegalArgumentException( "PredictedGene.create - 'predictedGene' can not be null" );
        }
        this.getHibernateTemplate().save( predictedGene );
        return this.transformEntity( transform, predictedGene );
    }

    /**
     * @see ubic.gemma.model.genome.PredictedGeneDao#create(java.util.Collection)
     */

    public java.util.Collection create( final java.util.Collection entities ) {
        return create( TRANSFORM_NONE, entities );
    }

    /**
     * @see ubic.gemma.model.genome.PredictedGeneDao#create(ubic.gemma.model.genome.PredictedGene)
     */
    public PredictedGene create( ubic.gemma.model.genome.PredictedGene predictedGene ) {
        return ( ubic.gemma.model.genome.PredictedGene ) this.create( TRANSFORM_NONE, predictedGene );
    }

    /**
     * @see ubic.gemma.model.genome.PredictedGeneDao#find(int, java.lang.String, ubic.gemma.model.genome.Gene)
     */

    public Object find( final int transform, final java.lang.String queryString, final ubic.gemma.model.genome.Gene gene ) {
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

        result = transformEntity( transform, ( ubic.gemma.model.genome.PredictedGene ) result );
        return result;
    }

    /**
     * @see ubic.gemma.model.genome.PredictedGeneDao#find(int, ubic.gemma.model.genome.Gene)
     */

    public Object find( final int transform, final ubic.gemma.model.genome.Gene gene ) {
        return this.find( transform,
                "from ubic.gemma.model.genome.PredictedGene as predictedGene where predictedGene.gene = :gene", gene );
    }

    /**
     * @see ubic.gemma.model.genome.PredictedGeneDao#find(java.lang.String, ubic.gemma.model.genome.Gene)
     */

    public ubic.gemma.model.genome.Gene find( final java.lang.String queryString,
            final ubic.gemma.model.genome.Gene gene ) {
        return ( ubic.gemma.model.genome.Gene ) this.find( TRANSFORM_NONE, queryString, gene );
    }

    /**
     * @see ubic.gemma.model.genome.PredictedGeneDao#find(ubic.gemma.model.genome.Gene)
     */

    public ubic.gemma.model.genome.Gene find( ubic.gemma.model.genome.Gene gene ) {
        return ( ubic.gemma.model.genome.Gene ) this.find( TRANSFORM_NONE, gene );
    }

    /**
     * @see ubic.gemma.model.genome.PredictedGeneDao#findByNcbiId(int, java.lang.String)
     */

    public java.util.Collection findByNcbiId( final int transform, final java.lang.String ncbiId ) {
        return this.findByNcbiId( transform, "from GeneImpl g where g.ncbiId = :ncbiId", ncbiId );
    }

    /**
     * @see ubic.gemma.model.genome.PredictedGeneDao#findByNcbiId(int, java.lang.String, java.lang.String)
     */

    public java.util.Collection findByNcbiId( final int transform, final java.lang.String queryString,
            final java.lang.String ncbiId ) {
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
     * @see ubic.gemma.model.genome.PredictedGeneDao#findByNcbiId(java.lang.String)
     */

    public java.util.Collection findByNcbiId( java.lang.String ncbiId ) {
        return this.findByNcbiId( TRANSFORM_NONE, ncbiId );
    }

    /**
     * @see ubic.gemma.model.genome.PredictedGeneDao#findByNcbiId(java.lang.String, java.lang.String)
     */

    @Override
    public java.util.Collection findByNcbiId( final java.lang.String queryString, final java.lang.String ncbiId ) {
        return this.findByNcbiId( TRANSFORM_NONE, queryString, ncbiId );
    }

    /**
     * @see ubic.gemma.model.genome.PredictedGeneDao#findByOfficalSymbol(int, java.lang.String)
     */

    public java.util.Collection findByOfficalSymbol( final int transform, final java.lang.String officialSymbol ) {
        return this.findByOfficalSymbol( transform,
                "from GeneImpl g where g.officialSymbol=:officialSymbol order by g.officialName", officialSymbol );
    }

    /**
     * @see ubic.gemma.model.genome.PredictedGeneDao#findByOfficalSymbol(int, java.lang.String, java.lang.String)
     */

    public java.util.Collection findByOfficalSymbol( final int transform, final java.lang.String queryString,
            final java.lang.String officialSymbol ) {
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
     * @see ubic.gemma.model.genome.PredictedGeneDao#findByOfficalSymbol(java.lang.String)
     */

    public java.util.Collection findByOfficalSymbol( java.lang.String officialSymbol ) {
        return this.findByOfficalSymbol( TRANSFORM_NONE, officialSymbol );
    }

    /**
     * @see ubic.gemma.model.genome.PredictedGeneDao#findByOfficalSymbol(java.lang.String, java.lang.String)
     */

    public java.util.Collection findByOfficalSymbol( final java.lang.String queryString,
            final java.lang.String officialSymbol ) {
        return this.findByOfficalSymbol( TRANSFORM_NONE, queryString, officialSymbol );
    }

    /**
     * @see ubic.gemma.model.genome.PredictedGeneDao#findByOfficialName(int, java.lang.String)
     */

    public java.util.Collection findByOfficialName( final int transform, final java.lang.String officialName ) {
        return this.findByOfficialName( transform,
                "from GeneImpl g where g.officialName=:officialName order by g.officialName", officialName );
    }

    /**
     * @see ubic.gemma.model.genome.PredictedGeneDao#findByOfficialName(int, java.lang.String, java.lang.String)
     */

    public java.util.Collection findByOfficialName( final int transform, final java.lang.String queryString,
            final java.lang.String officialName ) {
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
     * @see ubic.gemma.model.genome.PredictedGeneDao#findByOfficialName(java.lang.String)
     */

    public java.util.Collection findByOfficialName( java.lang.String officialName ) {
        return this.findByOfficialName( TRANSFORM_NONE, officialName );
    }

    /**
     * @see ubic.gemma.model.genome.PredictedGeneDao#findByOfficialName(java.lang.String, java.lang.String)
     */

    public java.util.Collection findByOfficialName( final java.lang.String queryString,
            final java.lang.String officialName ) {
        return this.findByOfficialName( TRANSFORM_NONE, queryString, officialName );
    }

    /**
     * @see ubic.gemma.model.genome.PredictedGeneDao#findByOfficialSymbolInexact(int, java.lang.String)
     */

    public java.util.Collection findByOfficialSymbolInexact( final int transform, final java.lang.String officialSymbol ) {
        return this
                .findByOfficialSymbolInexact( transform,
                        "from GeneImpl g where g.officialSymbol like :officialSymbol order by g.officialSymbol",
                        officialSymbol );
    }

    /**
     * @see ubic.gemma.model.genome.PredictedGeneDao#findByOfficialSymbolInexact(int, java.lang.String,
     *      java.lang.String)
     */

    public java.util.Collection findByOfficialSymbolInexact( final int transform, final java.lang.String queryString,
            final java.lang.String officialSymbol ) {
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
     * @see ubic.gemma.model.genome.PredictedGeneDao#findByOfficialSymbolInexact(java.lang.String)
     */

    public java.util.Collection findByOfficialSymbolInexact( java.lang.String officialSymbol ) {
        return this.findByOfficialSymbolInexact( TRANSFORM_NONE, officialSymbol );
    }

    /**
     * @see ubic.gemma.model.genome.PredictedGeneDao#findByOfficialSymbolInexact(java.lang.String, java.lang.String)
     */

    public java.util.Collection findByOfficialSymbolInexact( final java.lang.String queryString,
            final java.lang.String officialSymbol ) {
        return this.findByOfficialSymbolInexact( TRANSFORM_NONE, queryString, officialSymbol );
    }

    /**
     * @see ubic.gemma.model.genome.PredictedGeneDao#findByPhysicalLocation(int, java.lang.String,
     *      ubic.gemma.model.genome.PhysicalLocation)
     */

    @Override
    public java.util.Collection findByPhysicalLocation( final int transform, final java.lang.String queryString,
            final ubic.gemma.model.genome.PhysicalLocation location ) {
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
     * @see ubic.gemma.model.genome.PredictedGeneDao#findByPhysicalLocation(int,
     *      ubic.gemma.model.genome.PhysicalLocation)
     */

    @Override
    public java.util.Collection findByPhysicalLocation( final int transform,
            final ubic.gemma.model.genome.PhysicalLocation location ) {
        return this.findByPhysicalLocation( transform,
                "from ubic.gemma.model.genome.PredictedGene as predictedGene where predictedGene.location = :location",
                location );
    }

    /**
     * @see ubic.gemma.model.genome.PredictedGeneDao#findByPhysicalLocation(java.lang.String,
     *      ubic.gemma.model.genome.PhysicalLocation)
     */

    @Override
    public java.util.Collection findByPhysicalLocation( final java.lang.String queryString,
            final ubic.gemma.model.genome.PhysicalLocation location ) {
        return this.findByPhysicalLocation( TRANSFORM_NONE, queryString, location );
    }

    /**
     * @see ubic.gemma.model.genome.PredictedGeneDao#findByPhysicalLocation(ubic.gemma.model.genome.PhysicalLocation)
     */

    @Override
    public java.util.Collection findByPhysicalLocation( ubic.gemma.model.genome.PhysicalLocation location ) {
        return this.findByPhysicalLocation( TRANSFORM_NONE, location );
    }

    /**
     * @see ubic.gemma.model.genome.PredictedGeneDao#findOrCreate(int, java.lang.String, ubic.gemma.model.genome.Gene)
     */

    public Object findOrCreate( final int transform, final java.lang.String queryString,
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

        result = transformEntity( transform, ( ubic.gemma.model.genome.PredictedGene ) result );
        return result;
    }

    /**
     * @see ubic.gemma.model.genome.PredictedGeneDao#findOrCreate(int, ubic.gemma.model.genome.Gene)
     */

    public Object findOrCreate( final int transform, final ubic.gemma.model.genome.Gene gene ) {
        return this.findOrCreate( transform,
                "from ubic.gemma.model.genome.PredictedGene as predictedGene where predictedGene.gene = :gene", gene );
    }

    /**
     * @see ubic.gemma.model.genome.PredictedGeneDao#findOrCreate(java.lang.String, ubic.gemma.model.genome.Gene)
     */

    public ubic.gemma.model.genome.Gene findOrCreate( final java.lang.String queryString,
            final ubic.gemma.model.genome.Gene gene ) {
        return ( ubic.gemma.model.genome.Gene ) this.findOrCreate( TRANSFORM_NONE, queryString, gene );
    }

    /**
     * @see ubic.gemma.model.genome.PredictedGeneDao#findOrCreate(ubic.gemma.model.genome.Gene)
     */

    public ubic.gemma.model.genome.Gene findOrCreate( ubic.gemma.model.genome.Gene gene ) {
        return ( ubic.gemma.model.genome.Gene ) this.findOrCreate( TRANSFORM_NONE, gene );
    }

    public abstract PredictedGene geneValueObjectToEntity( GeneValueObject geneValueObject );

    /**
     * @see ubic.gemma.model.genome.GeneDao#geneValueObjectToEntity(ubic.gemma.model.genome.gene.GeneValueObject,
     *      ubic.gemma.model.genome.Gene)
     */
    public void geneValueObjectToEntity( ubic.gemma.model.genome.gene.GeneValueObject source,
            ubic.gemma.model.genome.PredictedGene target, boolean copyIfNull ) {
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

    public Collection<? extends PredictedGene> load( Collection<Long> ids ) {
        return this.getHibernateTemplate().findByNamedParam( "from PredictedGeneImpl where id in (:ids)", "ids", ids );
    }

    /**
     * @see ubic.gemma.model.genome.PredictedGeneDao#load(int, java.lang.Long)
     */

    public Object load( final int transform, final java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "PredictedGene.load - 'id' can not be null" );
        }
        final Object entity = this.getHibernateTemplate().get( ubic.gemma.model.genome.PredictedGeneImpl.class, id );
        return transformEntity( transform, ( ubic.gemma.model.genome.PredictedGene ) entity );
    }

    /**
     * @see ubic.gemma.model.genome.PredictedGeneDao#load(java.lang.Long)
     */

    public PredictedGene load( java.lang.Long id ) {
        return ( ubic.gemma.model.genome.PredictedGene ) this.load( TRANSFORM_NONE, id );
    }

    /**
     * @see ubic.gemma.model.genome.PredictedGeneDao#loadAll()
     */

    public java.util.Collection loadAll() {
        return this.loadAll( TRANSFORM_NONE );
    }

    /**
     * @see ubic.gemma.model.genome.PredictedGeneDao#loadAll(int)
     */

    public java.util.Collection loadAll( final int transform ) {
        final java.util.Collection results = this.getHibernateTemplate().loadAll(
                ubic.gemma.model.genome.PredictedGeneImpl.class );
        this.transformEntities( transform, results );
        return results;
    }

    /**
     * @see ubic.gemma.model.genome.PredictedGeneDao#remove(java.lang.Long)
     */

    public void remove( java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "PredictedGene.remove - 'id' can not be null" );
        }
        ubic.gemma.model.genome.PredictedGene entity = this.load( id );
        if ( entity != null ) {
            this.remove( entity );
        }
    }

    /**
     * @see ubic.gemma.model.common.SecurableDao#remove(java.util.Collection)
     */

    public void remove( java.util.Collection entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "PredictedGene.remove - 'entities' can not be null" );
        }
        this.getHibernateTemplate().deleteAll( entities );
    }

    /**
     * @see ubic.gemma.model.genome.PredictedGeneDao#remove(ubic.gemma.model.genome.PredictedGene)
     */
    public void remove( ubic.gemma.model.genome.PredictedGene predictedGene ) {
        if ( predictedGene == null ) {
            throw new IllegalArgumentException( "PredictedGene.remove - 'predictedGene' can not be null" );
        }
        this.getHibernateTemplate().delete( predictedGene );
    }

    /**
     * @see ubic.gemma.model.genome.GeneDao#toGeneValueObject(ubic.gemma.model.genome.Gene)
     */
    public ubic.gemma.model.genome.gene.GeneValueObject toGeneValueObject(
            final ubic.gemma.model.genome.PredictedGene entity ) {
        final ubic.gemma.model.genome.gene.GeneValueObject target = new ubic.gemma.model.genome.gene.GeneValueObject();
        toGeneValueObject( entity, target );
        return target;
    }

    /**
     * @see ubic.gemma.model.genome.GeneDao#toGeneValueObject(ubic.gemma.model.genome.Gene,
     *      ubic.gemma.model.genome.gene.GeneValueObject)
     */
    public void toGeneValueObject( ubic.gemma.model.genome.PredictedGene source,
            ubic.gemma.model.genome.gene.GeneValueObject target ) {
        target.setId( source.getId() );
        target.setName( source.getName() );
        target.setNcbiId( source.getNcbiId() );
        target.setOfficialSymbol( source.getOfficialSymbol() );
        target.setOfficialName( source.getOfficialName() );
        target.setDescription( source.getDescription() );
        target.setAliases( GeneValueObject.getAliasStrings( source.getAliases()) );
    }

    /**
     * @see ubic.gemma.model.genome.GeneDao#toGeneValueObjectCollection(java.util.Collection)
     */
    public final void toGeneValueObjectCollection( java.util.Collection entities ) {
        if ( entities != null ) {
            org.apache.commons.collections.CollectionUtils.transform( entities, GENEVALUEOBJECT_TRANSFORMER );
        }
    }

    /**
     * @see ubic.gemma.model.common.SecurableDao#update(java.util.Collection)
     */

    public void update( final java.util.Collection entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "PredictedGene.update - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator entityIterator = entities.iterator(); entityIterator.hasNext(); ) {
                            update( ( ubic.gemma.model.genome.PredictedGene ) entityIterator.next() );
                        }
                        return null;
                    }
                } );
    }

    /**
     * @see ubic.gemma.model.genome.PredictedGeneDao#update(ubic.gemma.model.genome.PredictedGene)
     */
    public void update( ubic.gemma.model.genome.PredictedGene predictedGene ) {
        if ( predictedGene == null ) {
            throw new IllegalArgumentException( "PredictedGene.update - 'predictedGene' can not be null" );
        }
        this.getHibernateTemplate().update( predictedGene );
    }

    /**
     * Default implementation for transforming the results of a report query into a value object. This implementation
     * exists for convenience reasons only. It needs only be overridden in the {@link GeneDaoImpl} class if you intend
     * to use reporting queries.
     * 
     * @see ubic.gemma.model.genome.GeneDao#toGeneValueObject(ubic.gemma.model.genome.Gene)
     */
    protected ubic.gemma.model.genome.gene.GeneValueObject toGeneValueObject( Object[] row ) {
        ubic.gemma.model.genome.gene.GeneValueObject target = null;
        if ( row != null ) {
            final int numberOfObjects = row.length;
            for ( int ctr = 0; ctr < numberOfObjects; ctr++ ) {
                final Object object = row[ctr];
                if ( object instanceof ubic.gemma.model.genome.PredictedGene ) {
                    target = toGeneValueObject( ( ubic.gemma.model.genome.PredictedGene ) object );
                    break;
                }
            }
        }
        return target;
    }

    /**
     * Transforms a collection of entities using the {@link #transformEntity(int,ubic.gemma.model.genome.PredictedGene)}
     * method. This method does not instantiate a new collection.
     * <p/>
     * This method is to be used internally only.
     * 
     * @param transform one of the constants declared in <code>ubic.gemma.model.genome.PredictedGeneDao</code>
     * @param entities the collection of entities to transform
     * @return the same collection as the argument, but this time containing the transformed entities
     * @see #transformEntity(int,ubic.gemma.model.genome.PredictedGene)
     */

    @Override
    protected void transformEntities( final int transform, final java.util.Collection entities ) {
        switch ( transform ) {
            case ubic.gemma.model.genome.GeneDao.TRANSFORM_GENEVALUEOBJECT:
                toGeneValueObjectCollection( entities );
                break;
            case TRANSFORM_NONE: // fall-through
            default:
                // do nothing;
        }
    }

    /**
     * Allows transformation of entities into value objects (or something else for that matter), when the
     * <code>transform</code> flag is set to one of the constants defined in
     * <code>ubic.gemma.model.genome.PredictedGeneDao</code>, please note that the {@link #TRANSFORM_NONE} constant
     * denotes no transformation, so the entity itself will be returned. If the integer argument value is unknown
     * {@link #TRANSFORM_NONE} is assumed.
     * 
     * @param transform one of the constants declared in {@link ubic.gemma.model.genome.PredictedGeneDao}
     * @param entity an entity that was found
     * @return the transformed entity (i.e. new value object, etc)
     * @see #transformEntities(int,java.util.Collection)
     */
    protected Object transformEntity( final int transform, final ubic.gemma.model.genome.PredictedGene entity ) {
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