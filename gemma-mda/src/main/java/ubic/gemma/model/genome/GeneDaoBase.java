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
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.model.analysis.expression.coexpression.CoexpressionCollectionValueObject;
import ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionCache;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.genome.gene.GeneValueObject;

/**
 * <p>
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.genome.Gene</code>.
 * </p>
 * 
 * @see ubic.gemma.model.genome.Gene
 */
public abstract class GeneDaoBase extends ubic.gemma.model.genome.ChromosomeFeatureDaoImpl<Gene> implements
        ubic.gemma.model.genome.GeneDao {

    @Autowired
    private Probe2ProbeCoexpressionCache probe2ProbeCoexpressionCache;

    /**
     * This anonymous transformer is designed to transform entities or report query results (which result in an array of
     * objects) to {@link ubic.gemma.model.genome.gene.GeneValueObject} using the Jakarta Commons-Collections
     * Transformation API.
     */
    protected org.apache.commons.collections.Transformer GENEVALUEOBJECT_TRANSFORMER = new org.apache.commons.collections.Transformer() {
        public Object transform( Object input ) {
            Object result = null;
            if ( input instanceof ubic.gemma.model.genome.Gene ) {
                result = toGeneValueObject( ( ubic.gemma.model.genome.Gene ) input );
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
     * @see ubic.gemma.model.genome.GeneDao#countAll()
     */
    public java.lang.Integer countAll() {
        try {
            return this.handleCountAll();
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException( "Error performing 'ubic.gemma.model.genome.GeneDao.countAll()' --> "
                    + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.genome.GeneDao#create(int, java.util.Collection)
     */
    public java.util.Collection<? extends Gene> create( final int transform,
            final java.util.Collection<? extends Gene> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "Gene.create - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator entityIterator = entities.iterator(); entityIterator.hasNext(); ) {
                            create( transform, ( ubic.gemma.model.genome.Gene ) entityIterator.next() );
                        }
                        return null;
                    }
                } );
        return entities;
    }

    /**
     * @see ubic.gemma.model.genome.GeneDao#create(int transform, ubic.gemma.model.genome.Gene)
     */
    public Object create( final int transform, final ubic.gemma.model.genome.Gene gene ) {
        if ( gene == null ) {
            throw new IllegalArgumentException( "Gene.create - 'gene' can not be null" );
        }
        this.getHibernateTemplate().save( gene );
        return this.transformEntity( transform, gene );
    }

    /**
     * @see ubic.gemma.model.genome.GeneDao#create(java.util.Collection)
     */
    public java.util.Collection<? extends Gene> create( final java.util.Collection<? extends Gene> entities ) {
        return create( TRANSFORM_NONE, entities );
    }

    /**
     * @see ubic.gemma.model.genome.GeneDao#create(ubic.gemma.model.genome.Gene)
     */
    public Gene create( ubic.gemma.model.genome.Gene gene ) {
        return ( ubic.gemma.model.genome.Gene ) this.create( TRANSFORM_NONE, gene );
    }

    /**
     * @see ubic.gemma.model.genome.GeneDao#find(int, java.lang.String, ubic.gemma.model.genome.Gene)
     */

    public Gene find( final int transform, final java.lang.String queryString, final ubic.gemma.model.genome.Gene gene ) {
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
        result = transformEntity( transform, ( ubic.gemma.model.genome.Gene ) result );
        return ( Gene ) result;
    }

    /**
     * @see ubic.gemma.model.genome.GeneDao#find(int, ubic.gemma.model.genome.Gene)
     */
    public Gene find( final int transform, final ubic.gemma.model.genome.Gene gene ) {
        return this.find( transform, "from ubic.gemma.model.genome.Gene as gene where gene.gene = :gene", gene );
    }

    /**
     * @see ubic.gemma.model.genome.GeneDao#find(java.lang.String, ubic.gemma.model.genome.Gene)
     */
    public ubic.gemma.model.genome.Gene find( final java.lang.String queryString,
            final ubic.gemma.model.genome.Gene gene ) {
        return this.find( TRANSFORM_NONE, queryString, gene );
    }

    /**
     * @see ubic.gemma.model.genome.GeneDao#find(ubic.gemma.model.genome.Gene)
     */
    public ubic.gemma.model.genome.Gene find( ubic.gemma.model.genome.Gene gene ) {
        return this.find( TRANSFORM_NONE, gene );
    }

    /**
     * @see ubic.gemma.model.genome.GeneDao#findByAccession(java.lang.String,
     *      ubic.gemma.model.common.description.ExternalDatabase)
     */
    public ubic.gemma.model.genome.Gene findByAccession( final java.lang.String accession,
            final ubic.gemma.model.common.description.ExternalDatabase source ) {
        try {
            return this.handleFindByAccession( accession, source );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.genome.GeneDao.findByAccession(java.lang.String accession, ubic.gemma.model.common.description.ExternalDatabase source)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.genome.GeneDao#findByAlias(java.lang.String)
     */
    public java.util.Collection<Gene> findByAlias( final java.lang.String search ) {
        try {
            return this.handleFindByAlias( search );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.genome.GeneDao.findByAlias(java.lang.String search)' --> " + th,
                    th );
        }
    }

    /**
     * @see ubic.gemma.model.genome.GeneDao#findByNcbiId(int, java.lang.String)
     */

    public java.util.Collection<Gene> findByNcbiId( final int transform, final java.lang.String ncbiId ) {
        return this.findByNcbiId( transform, "from GeneImpl g where g.ncbiId = :ncbiId", ncbiId );
    }

    /**
     * @see ubic.gemma.model.genome.GeneDao#findByNcbiId(int, java.lang.String, java.lang.String)
     */

    public java.util.Collection<Gene> findByNcbiId( final int transform, final java.lang.String queryString,
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
     * @see ubic.gemma.model.genome.GeneDao#findByNcbiId(java.lang.String)
     */

    public java.util.Collection<Gene> findByNcbiId( java.lang.String ncbiId ) {
        return this.findByNcbiId( TRANSFORM_NONE, ncbiId );
    }

    /**
     * @see ubic.gemma.model.genome.GeneDao#findByNcbiId(java.lang.String, java.lang.String)
     */

    @Override
    public java.util.Collection<Gene> findByNcbiId( final java.lang.String queryString, final java.lang.String ncbiId ) {
        return this.findByNcbiId( TRANSFORM_NONE, queryString, ncbiId );
    }

    /**
     * @see ubic.gemma.model.genome.GeneDao#findByOfficalSymbol(int, java.lang.String)
     */
    public java.util.Collection<Gene> findByOfficalSymbol( final int transform, final java.lang.String officialSymbol ) {
        return this.findByOfficalSymbol( transform,
                "from GeneImpl g where g.officialSymbol=:officialSymbol order by g.officialName", officialSymbol );
    }

    /**
     * @see ubic.gemma.model.genome.GeneDao#findByOfficalSymbol(int, java.lang.String, java.lang.String)
     */
    public java.util.Collection<Gene> findByOfficalSymbol( final int transform, final java.lang.String queryString,
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
     * @see ubic.gemma.model.genome.GeneDao#findByOfficalSymbol(java.lang.String)
     */
    public java.util.Collection<Gene> findByOfficalSymbol( java.lang.String officialSymbol ) {
        return this.findByOfficalSymbol( TRANSFORM_NONE, officialSymbol );
    }

    /**
     * @see ubic.gemma.model.genome.GeneDao#findByOfficalSymbol(java.lang.String, java.lang.String)
     */
    public java.util.Collection<Gene> findByOfficalSymbol( final java.lang.String queryString,
            final java.lang.String officialSymbol ) {
        return this.findByOfficalSymbol( TRANSFORM_NONE, queryString, officialSymbol );
    }

    /**
     * @see ubic.gemma.model.genome.GeneDao#findByOfficialName(int, java.lang.String)
     */
    public java.util.Collection<Gene> findByOfficialName( final int transform, final java.lang.String officialName ) {
        return this.findByOfficialName( transform,
                "from GeneImpl g where g.officialName=:officialName order by g.officialName", officialName );
    }

    /**
     * @see ubic.gemma.model.genome.GeneDao#findByOfficialName(int, java.lang.String, java.lang.String)
     */

    public java.util.Collection<Gene> findByOfficialName( final int transform, final java.lang.String queryString,
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
     * @see ubic.gemma.model.genome.GeneDao#findByOfficialName(java.lang.String)
     */
    public java.util.Collection<Gene> findByOfficialName( java.lang.String officialName ) {
        return this.findByOfficialName( TRANSFORM_NONE, officialName );
    }

    /**
     * @see ubic.gemma.model.genome.GeneDao#findByOfficialName(java.lang.String, java.lang.String)
     */
    public java.util.Collection<Gene> findByOfficialName( final java.lang.String queryString,
            final java.lang.String officialName ) {
        return this.findByOfficialName( TRANSFORM_NONE, queryString, officialName );
    }

    /**
     * @see ubic.gemma.model.genome.GeneDao#findByOfficialSymbol(java.lang.String, ubic.gemma.model.genome.Taxon)
     */
    public ubic.gemma.model.genome.Gene findByOfficialSymbol( final java.lang.String symbol,
            final ubic.gemma.model.genome.Taxon taxon ) {
        try {
            return this.handleFindByOfficialSymbol( symbol, taxon );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.genome.GeneDao.findByOfficialSymbol(java.lang.String symbol, ubic.gemma.model.genome.Taxon taxon)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.genome.GeneDao#findByOfficialSymbolInexact(int, java.lang.String)
     */
    public java.util.Collection<Gene> findByOfficialSymbolInexact( final int transform,
            final java.lang.String officialSymbol ) {
        return this
                .findByOfficialSymbolInexact( transform,
                        "from GeneImpl g where g.officialSymbol like :officialSymbol order by g.officialSymbol",
                        officialSymbol );
    }

    /**
     * @see ubic.gemma.model.genome.GeneDao#findByOfficialSymbolInexact(int, java.lang.String, java.lang.String)
     */
    public java.util.Collection<Gene> findByOfficialSymbolInexact( final int transform,
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
     * @see ubic.gemma.model.genome.GeneDao#findByOfficialSymbolInexact(java.lang.String)
     */
    public java.util.Collection<Gene> findByOfficialSymbolInexact( java.lang.String officialSymbol ) {
        return this.findByOfficialSymbolInexact( TRANSFORM_NONE, officialSymbol );
    }

    /**
     * @see ubic.gemma.model.genome.GeneDao#findByOfficialSymbolInexact(java.lang.String, java.lang.String)
     */
    public java.util.Collection<Gene> findByOfficialSymbolInexact( final java.lang.String queryString,
            final java.lang.String officialSymbol ) {
        return this.findByOfficialSymbolInexact( TRANSFORM_NONE, queryString, officialSymbol );
    }

    /**
     * @see ubic.gemma.model.genome.GeneDao#findByPhysicalLocation(int, java.lang.String,
     *      ubic.gemma.model.genome.PhysicalLocation)
     */

    @Override
    public java.util.Collection<Gene> findByPhysicalLocation( final int transform, final java.lang.String queryString,
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
     * @see ubic.gemma.model.genome.GeneDao#findByPhysicalLocation(int, ubic.gemma.model.genome.PhysicalLocation)
     */

    @Override
    public java.util.Collection<Gene> findByPhysicalLocation( final int transform,
            final ubic.gemma.model.genome.PhysicalLocation location ) {
        return this.findByPhysicalLocation( transform,
                "from ubic.gemma.model.genome.Gene as gene where gene.location = :location", location );
    }

    /**
     * @see ubic.gemma.model.genome.GeneDao#findByPhysicalLocation(java.lang.String,
     *      ubic.gemma.model.genome.PhysicalLocation)
     */

    @Override
    public java.util.Collection<Gene> findByPhysicalLocation( final java.lang.String queryString,
            final ubic.gemma.model.genome.PhysicalLocation location ) {
        return this.findByPhysicalLocation( TRANSFORM_NONE, queryString, location );
    }

    /**
     * @see ubic.gemma.model.genome.GeneDao#findByPhysicalLocation(ubic.gemma.model.genome.PhysicalLocation)
     */

    @Override
    public java.util.Collection<Gene> findByPhysicalLocation( ubic.gemma.model.genome.PhysicalLocation location ) {
        return this.findByPhysicalLocation( TRANSFORM_NONE, location );
    }

    /**
     * @see ubic.gemma.model.genome.GeneDao#findOrCreate(int, java.lang.String, ubic.gemma.model.genome.Gene)
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
        result = transformEntity( transform, ( ubic.gemma.model.genome.Gene ) result );
        return result;
    }

    /**
     * @see ubic.gemma.model.genome.GeneDao#findOrCreate(int, ubic.gemma.model.genome.Gene)
     */
    public Object findOrCreate( final int transform, final ubic.gemma.model.genome.Gene gene ) {
        return this.findOrCreate( transform, "from ubic.gemma.model.genome.Gene as gene where gene.gene = :gene", gene );
    }

    /**
     * @see ubic.gemma.model.genome.GeneDao#findOrCreate(java.lang.String, ubic.gemma.model.genome.Gene)
     */
    public ubic.gemma.model.genome.Gene findOrCreate( final java.lang.String queryString,
            final ubic.gemma.model.genome.Gene gene ) {
        return ( ubic.gemma.model.genome.Gene ) this.findOrCreate( TRANSFORM_NONE, queryString, gene );
    }

    /**
     * @see ubic.gemma.model.genome.GeneDao#findOrCreate(ubic.gemma.model.genome.Gene)
     */
    public ubic.gemma.model.genome.Gene findOrCreate( ubic.gemma.model.genome.Gene gene ) {
        return ( ubic.gemma.model.genome.Gene ) this.findOrCreate( TRANSFORM_NONE, gene );
    }

    /**
     * @see ubic.gemma.model.genome.GeneDao#geneValueObjectToEntity(ubic.gemma.model.genome.gene.GeneValueObject,
     *      ubic.gemma.model.genome.Gene)
     */
    public void geneValueObjectToEntity( ubic.gemma.model.genome.gene.GeneValueObject source,
            ubic.gemma.model.genome.Gene target, boolean copyIfNull ) {
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

    /**
     * @see ubic.gemma.model.genome.GeneDao#getCoexpressedGenes(ubic.gemma.model.genome.Gene, java.util.Collection,
     *      java.lang.Integer, boolean)
     */
    public Map<Gene, CoexpressionCollectionValueObject> getCoexpressedGenes(
            final Collection<ubic.gemma.model.genome.Gene> genes,
            final java.util.Collection<? extends BioAssaySet> ees, final java.lang.Integer stringency,
            final boolean knownGenesOnly, final boolean interGeneOnly ) {
        try {
            return this.handleGetCoexpressedGenes( genes, ees, stringency, knownGenesOnly, interGeneOnly );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.genome.GeneDao.getCoexpressedGenes(ubic.gemma.model.genome.Gene gene, java.util.Collection ees, java.lang.Integer stringency, boolean knownGenesOnly)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.genome.GeneDao#getCoexpressedGenes(ubic.gemma.model.genome.Gene, java.util.Collection,
     *      java.lang.Integer, boolean)
     */
    public CoexpressionCollectionValueObject getCoexpressedGenes( final ubic.gemma.model.genome.Gene gene,
            final java.util.Collection<? extends BioAssaySet> ees, final java.lang.Integer stringency,
            final boolean knownGenesOnly ) {
        try {
            return this.handleGetCoexpressedGenes( gene, ees, stringency, knownGenesOnly );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.genome.GeneDao.getCoexpressedGenes(ubic.gemma.model.genome.Gene gene, java.util.Collection ees, java.lang.Integer stringency, boolean knownGenesOnly)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.genome.GeneDao#getCompositeSequenceCountById(long)
     */
    public long getCompositeSequenceCountById( final long id ) {
        try {
            return this.handleGetCompositeSequenceCountById( id );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.genome.GeneDao.getCompositeSequenceCountById(long id)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.genome.GeneDao#getCompositeSequences(ubic.gemma.model.genome.Gene,
     *      ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    public java.util.Collection<CompositeSequence> getCompositeSequences( final ubic.gemma.model.genome.Gene gene,
            final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {
        try {
            return this.handleGetCompositeSequences( gene, arrayDesign );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.genome.GeneDao.getCompositeSequences(ubic.gemma.model.genome.Gene gene, ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.genome.GeneDao#getCompositeSequencesById(long)
     */
    public java.util.Collection<CompositeSequence> getCompositeSequencesById( final long id ) {
        try {
            return this.handleGetCompositeSequencesById( id );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.genome.GeneDao.getCompositeSequencesById(long id)' --> " + th,
                    th );
        }
    }

    /**
     * @see ubic.gemma.model.genome.GeneDao#getGenesByTaxon(ubic.gemma.model.genome.Taxon)
     */
    public java.util.Collection<Gene> getGenesByTaxon( final ubic.gemma.model.genome.Taxon taxon ) {
        try {
            return this.handleGetGenesByTaxon( taxon );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.genome.GeneDao.getGenesByTaxon(ubic.gemma.model.genome.Taxon taxon)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.genome.GeneDao#getMicroRnaByTaxon(ubic.gemma.model.genome.Taxon)
     */
    public java.util.Collection<Gene> getMicroRnaByTaxon( final ubic.gemma.model.genome.Taxon taxon ) {
        try {
            return this.handleGetMicroRnaByTaxon( taxon );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.genome.GeneDao.getMicroRnaByTaxon(ubic.gemma.model.genome.Taxon taxon)' --> "
                            + th, th );
        }
    }

    /**
     * @return the probe2ProbeCoexpressionCache
     */
    public Probe2ProbeCoexpressionCache getProbe2ProbeCoexpressionCache() {
        return probe2ProbeCoexpressionCache;
    }

    /**
     * @see ubic.gemma.model.genome.GeneDao#load(int, java.lang.Long)
     */

    public Object load( final int transform, final java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "Gene.load - 'id' can not be null" );
        }
        final Object entity = this.getHibernateTemplate().get( ubic.gemma.model.genome.GeneImpl.class, id );
        return transformEntity( transform, ( ubic.gemma.model.genome.Gene ) entity );
    }

    /**
     * @see ubic.gemma.model.genome.GeneDao#load(java.lang.Long)
     */

    public Gene load( java.lang.Long id ) {
        return ( ubic.gemma.model.genome.Gene ) this.load( TRANSFORM_NONE, id );
    }

    /**
     * @see ubic.gemma.model.genome.GeneDao#load(java.util.Collection)
     */
    public java.util.Collection<Gene> load( final java.util.Collection<Long> ids ) {
        try {
            return this.handleLoadMultiple( ids );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.genome.GeneDao.loadMultiple(java.util.Collection ids)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.genome.GeneDao#loadAll()
     */

    public java.util.Collection<Gene> loadAll() {
        return this.loadAll( TRANSFORM_NONE );
    }

    /**
     * @see ubic.gemma.model.genome.GeneDao#loadAll(int)
     */

    public java.util.Collection<Gene> loadAll( final int transform ) {
        final java.util.Collection results = this.getHibernateTemplate().loadAll(
                ubic.gemma.model.genome.GeneImpl.class );
        this.transformEntities( transform, results );
        return results;
    }

    /**
     * @see ubic.gemma.model.genome.GeneDao#loadKnownGenes(ubic.gemma.model.genome.Taxon)
     */
    public java.util.Collection<Gene> loadKnownGenes( final ubic.gemma.model.genome.Taxon taxon ) {
        try {
            return this.handleLoadKnownGenes( taxon );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.genome.GeneDao.loadKnownGenes(ubic.gemma.model.genome.Taxon taxon)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.genome.GeneDao#loadPredictedGenes(ubic.gemma.model.genome.Taxon)
     */
    public java.util.Collection<PredictedGene> loadPredictedGenes( final ubic.gemma.model.genome.Taxon taxon ) {
        try {
            return this.handleLoadPredictedGenes( taxon );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.genome.GeneDao.loadPredictedGenes(ubic.gemma.model.genome.Taxon taxon)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.genome.GeneDao#loadProbeAlignedRegions(ubic.gemma.model.genome.Taxon)
     */
    public java.util.Collection<ProbeAlignedRegion> loadProbeAlignedRegions( final ubic.gemma.model.genome.Taxon taxon ) {
        try {
            return this.handleLoadProbeAlignedRegions( taxon );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.genome.GeneDao.loadProbeAlignedRegions(ubic.gemma.model.genome.Taxon taxon)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.genome.GeneDao#remove(java.lang.Long)
     */

    public void remove( java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "Gene.remove - 'id' can not be null" );
        }
        ubic.gemma.model.genome.Gene entity = this.load( id );
        if ( entity != null ) {
            this.remove( entity );
        }
    }

    /**
     * @see ubic.gemma.model.common.SecurableDao#remove(java.util.Collection)
     */

    public void remove( java.util.Collection entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "Gene.remove - 'entities' can not be null" );
        }
        this.getHibernateTemplate().deleteAll( entities );
    }

    /**
     * @see ubic.gemma.model.genome.GeneDao#remove(ubic.gemma.model.genome.Gene)
     */
    public void remove( ubic.gemma.model.genome.Gene gene ) {
        if ( gene == null ) {
            throw new IllegalArgumentException( "Gene.remove - 'gene' can not be null" );
        }
        this.getHibernateTemplate().delete( gene );
    }

    /**
     * @param probe2ProbeCoexpressionCache the probe2ProbeCoexpressionCache to set
     */
    public void setProbe2ProbeCoexpressionCache( Probe2ProbeCoexpressionCache probe2ProbeCoexpressionCache ) {
        this.probe2ProbeCoexpressionCache = probe2ProbeCoexpressionCache;
    }

    /**
     * @see ubic.gemma.model.genome.GeneDao#thaw(ubic.gemma.model.genome.Gene)
     */
    public Gene thaw( final ubic.gemma.model.genome.Gene gene ) {
        try {
            return this.handleThaw( gene );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.genome.GeneDao.thaw(ubic.gemma.model.genome.Gene gene)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.genome.GeneDao#thawLite(java.util.Collection)
     */
    public Collection<Gene> thawLite( final java.util.Collection<Gene> genes ) {
        try {
            return this.handleThawLite( genes );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.genome.GeneDao.thawLite(java.util.Collection genes)' --> " + th,
                    th );
        }
    }

    /**
     * @see ubic.gemma.model.genome.GeneDao#toGeneValueObject(ubic.gemma.model.genome.Gene)
     */
    public ubic.gemma.model.genome.gene.GeneValueObject toGeneValueObject( final ubic.gemma.model.genome.Gene entity ) {
        final ubic.gemma.model.genome.gene.GeneValueObject target = new ubic.gemma.model.genome.gene.GeneValueObject();
        toGeneValueObject( entity, target );
        return target;
    }

    /**
     * @see ubic.gemma.model.genome.GeneDao#toGeneValueObject(ubic.gemma.model.genome.Gene,
     *      ubic.gemma.model.genome.gene.GeneValueObject)
     */
    public void toGeneValueObject( ubic.gemma.model.genome.Gene source,
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
            throw new IllegalArgumentException( "Gene.update - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator entityIterator = entities.iterator(); entityIterator.hasNext(); ) {
                            update( ( ubic.gemma.model.genome.Gene ) entityIterator.next() );
                        }
                        return null;
                    }
                } );
    }

    /**
     * @see ubic.gemma.model.genome.GeneDao#update(ubic.gemma.model.genome.Gene)
     */
    public void update( ubic.gemma.model.genome.Gene gene ) {
        if ( gene == null ) {
            throw new IllegalArgumentException( "Gene.update - 'gene' can not be null" );
        }
        this.getHibernateTemplate().update( gene );
    }

    /**
     * Performs the core logic for {@link #countAll()}
     */
    protected abstract java.lang.Integer handleCountAll() throws java.lang.Exception;

    /**
     * Performs the core logic for
     * {@link #findByAccession(java.lang.String, ubic.gemma.model.common.description.ExternalDatabase)}
     */
    protected abstract ubic.gemma.model.genome.Gene handleFindByAccession( java.lang.String accession,
            ubic.gemma.model.common.description.ExternalDatabase source ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findByAlias(java.lang.String)}
     */
    protected abstract java.util.Collection<Gene> handleFindByAlias( java.lang.String search )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findByOfficialSymbol(java.lang.String, ubic.gemma.model.genome.Taxon)}
     */
    protected abstract ubic.gemma.model.genome.Gene handleFindByOfficialSymbol( java.lang.String symbol,
            ubic.gemma.model.genome.Taxon taxon ) throws java.lang.Exception;

    protected abstract Map<Gene, CoexpressionCollectionValueObject> handleGetCoexpressedGenes( Collection<Gene> genes,
            Collection<? extends BioAssaySet> ees, Integer stringency, boolean knownGenesOnly, boolean interGeneOnly )
            throws Exception;

    /**
     * Performs the core logic for
     * {@link #getCoexpressedGenes(ubic.gemma.model.genome.Gene, java.util.Collection, java.lang.Integer, boolean)}
     */
    protected abstract CoexpressionCollectionValueObject handleGetCoexpressedGenes( ubic.gemma.model.genome.Gene gene,
            java.util.Collection<? extends BioAssaySet> ees, java.lang.Integer stringency, boolean knownGenesOnly )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #getCompositeSequenceCountById(long)}
     */
    protected abstract long handleGetCompositeSequenceCountById( long id ) throws java.lang.Exception;

    /**
     * Performs the core logic for
     * {@link #getCompositeSequences(ubic.gemma.model.genome.Gene, ubic.gemma.model.expression.arrayDesign.ArrayDesign)}
     */
    protected abstract java.util.Collection<CompositeSequence> handleGetCompositeSequences(
            ubic.gemma.model.genome.Gene gene, ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #getCompositeSequencesById(long)}
     */
    protected abstract java.util.Collection<CompositeSequence> handleGetCompositeSequencesById( long id )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #getGenesByTaxon(ubic.gemma.model.genome.Taxon)}
     */
    protected abstract java.util.Collection<Gene> handleGetGenesByTaxon( ubic.gemma.model.genome.Taxon taxon )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #getMicroRnaByTaxon(ubic.gemma.model.genome.Taxon)}
     */
    protected abstract java.util.Collection<Gene> handleGetMicroRnaByTaxon( ubic.gemma.model.genome.Taxon taxon )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #loadKnownGenes(ubic.gemma.model.genome.Taxon)}
     */
    protected abstract java.util.Collection<Gene> handleLoadKnownGenes( ubic.gemma.model.genome.Taxon taxon )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #load(java.util.Collection)}
     */
    protected abstract java.util.Collection<Gene> handleLoadMultiple( java.util.Collection<Long> ids )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #loadPredictedGenes(ubic.gemma.model.genome.Taxon)}
     */
    protected abstract java.util.Collection<PredictedGene> handleLoadPredictedGenes( ubic.gemma.model.genome.Taxon taxon )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #loadProbeAlignedRegions(ubic.gemma.model.genome.Taxon)}
     */
    protected abstract java.util.Collection<ProbeAlignedRegion> handleLoadProbeAlignedRegions(
            ubic.gemma.model.genome.Taxon taxon ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #thaw(ubic.gemma.model.genome.Gene)}
     */
    protected abstract Gene handleThaw( ubic.gemma.model.genome.Gene gene ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #thawLite(java.util.Collection)}
     */
    protected abstract Collection<Gene> handleThawLite( java.util.Collection<Gene> genes ) throws java.lang.Exception;

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
                if ( object instanceof ubic.gemma.model.genome.Gene ) {
                    target = toGeneValueObject( ( ubic.gemma.model.genome.Gene ) object );
                    break;
                }
            }
        }
        return target;
    }

    /**
     * Transforms a collection of entities using the {@link #transformEntity(int,ubic.gemma.model.genome.Gene)} method.
     * This method does not instantiate a new collection.
     * <p/>
     * This method is to be used internally only.
     * 
     * @param transform one of the constants declared in <code>ubic.gemma.model.genome.GeneDao</code>
     * @param entities the collection of entities to transform
     * @return the same collection as the argument, but this time containing the transformed entities
     * @see #transformEntity(int,ubic.gemma.model.genome.Gene)
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
     * <code>ubic.gemma.model.genome.GeneDao</code>, please note that the {@link #TRANSFORM_NONE} constant denotes no
     * transformation, so the entity itself will be returned.
     * <p/>
     * This method will return instances of these types:
     * <ul>
     * <li>{@link ubic.gemma.model.genome.Gene} - {@link #TRANSFORM_NONE}</li>
     * <li>{@link ubic.gemma.model.genome.gene.GeneValueObject} - {@link TRANSFORM_GENEVALUEOBJECT}</li>
     * </ul>
     * If the integer argument value is unknown {@link #TRANSFORM_NONE} is assumed.
     * 
     * @param transform one of the constants declared in {@link ubic.gemma.model.genome.GeneDao}
     * @param entity an entity that was found
     * @return the transformed entity (i.e. new value object, etc)
     * @see #transformEntities(int,java.util.Collection)
     */
    protected Object transformEntity( final int transform, final ubic.gemma.model.genome.Gene entity ) {
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