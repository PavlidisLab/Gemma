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
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

import ubic.gemma.model.analysis.expression.coexpression.CoexpressionCollectionValueObject;
import ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionCache;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.BioAssaySet;

/**
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.genome.Gene</code>.
 * 
 * @see ubic.gemma.model.genome.Gene
 */
public abstract class GeneDaoBase extends HibernateDaoSupport implements ubic.gemma.model.genome.GeneDao {

    @Autowired
    private Probe2ProbeCoexpressionCache probe2ProbeCoexpressionCache;

    /**
     * @see ubic.gemma.model.genome.GeneDao#countAll()
     */
    @Override
    public java.lang.Integer countAll() {
        return this.handleCountAll();

    }

    /**
     * @see ubic.gemma.model.genome.GeneDao#create(int, java.util.Collection)
     */
    @Override
    public java.util.Collection<? extends Gene> create( final java.util.Collection<? extends Gene> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "Gene.create - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    @Override
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator<? extends Gene> entityIterator = entities.iterator(); entityIterator
                                .hasNext(); ) {
                            create( entityIterator.next() );
                        }
                        return null;
                    }
                } );
        return entities;
    }

    /**
     * @see ubic.gemma.model.genome.GeneDao#create(int transform, ubic.gemma.model.genome.Gene)
     */
    @Override
    public Gene create( final ubic.gemma.model.genome.Gene gene ) {
        if ( gene == null ) {
            throw new IllegalArgumentException( "Gene.create - 'gene' can not be null" );
        }
        this.getHibernateTemplate().save( gene );
        return gene;
    }

    /**
     * @see ubic.gemma.model.genome.GeneDao#findByAccession(java.lang.String,
     *      ubic.gemma.model.common.description.ExternalDatabase)
     */
    @Override
    public ubic.gemma.model.genome.Gene findByAccession( final java.lang.String accession,
            final ubic.gemma.model.common.description.ExternalDatabase source ) {
        return this.handleFindByAccession( accession, source );

    }

    /**
     * @see ubic.gemma.model.genome.GeneDao#findByAlias(java.lang.String)
     */
    @Override
    public java.util.Collection<Gene> findByAlias( final java.lang.String search ) {
        return this.handleFindByAlias( search );

    }

    /**
     * @see ubic.gemma.model.genome.GeneDao#findByNcbiId(int, java.lang.String, java.lang.String)
     */
    @Override
    public Gene findByNcbiId( Integer ncbiId ) {

        java.util.List<?> results = this.getHibernateTemplate().findByNamedParam(
                "from GeneImpl g where g.ncbiGeneId = :n", "n", ncbiId );
        if ( results.size() > 1 ) {
            throw new RuntimeException( "more than one gene with ncbi id =" + ncbiId );
        }
        if ( results.isEmpty() ) return null;
        return ( Gene ) results.iterator().next();
    }

    /**
     * @see ubic.gemma.model.genome.GeneDao#findByOfficalSymbol(int, java.lang.String)
     */
    @Override
    public java.util.Collection<Gene> findByOfficalSymbol( final java.lang.String officialSymbol ) {
        return this.findByOfficalSymbol(
                "from GeneImpl g where g.officialSymbol=:officialSymbol order by g.officialName", officialSymbol );
    }

    /**
     * @see ubic.gemma.model.genome.GeneDao#findByOfficalSymbol(int, java.lang.String, java.lang.String)
     */
    @SuppressWarnings("unchecked")
    public java.util.Collection<Gene> findByOfficalSymbol( final java.lang.String queryString,
            final java.lang.String officialSymbol ) {
        java.util.List<String> argNames = new java.util.ArrayList<String>();
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add( officialSymbol );
        argNames.add( "officialSymbol" );
        java.util.List<?> results = this.getHibernateTemplate().findByNamedParam( queryString,
                argNames.toArray( new String[argNames.size()] ), args.toArray() );
        return ( Collection<Gene> ) results;
    }

    /**
     * @see ubic.gemma.model.genome.GeneDao#findByOfficialName(int, java.lang.String)
     */
    @Override
    public java.util.Collection<Gene> findByOfficialName( final java.lang.String officialName ) {
        return this.findByOfficialName( "from GeneImpl g where g.officialName=:officialName order by g.officialName",
                officialName );
    }

    /**
     * @see ubic.gemma.model.genome.GeneDao#findByOfficialName(int, java.lang.String, java.lang.String)
     */

    @SuppressWarnings("unchecked")
    public java.util.Collection<Gene> findByOfficialName( final java.lang.String queryString,
            final java.lang.String officialName ) {
        java.util.List<String> argNames = new java.util.ArrayList<String>();
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add( officialName );
        argNames.add( "officialName" );
        java.util.List<?> results = this.getHibernateTemplate().findByNamedParam( queryString,
                argNames.toArray( new String[argNames.size()] ), args.toArray() );

        return ( Collection<Gene> ) results;
    }

    /**
     * @see ubic.gemma.model.genome.GeneDao#findByOfficialSymbol(java.lang.String, ubic.gemma.model.genome.Taxon)
     */
    @Override
    public ubic.gemma.model.genome.Gene findByOfficialSymbol( final java.lang.String symbol,
            final ubic.gemma.model.genome.Taxon taxon ) {
        return this.handleFindByOfficialSymbol( symbol, taxon );

    }

    /**
     * @see ubic.gemma.model.genome.GeneDao#findByOfficialSymbolInexact(int, java.lang.String)
     */
    @Override
    public java.util.Collection<Gene> findByOfficialSymbolInexact( final java.lang.String officialSymbol ) {
        return this
                .findByOfficialSymbolInexact(
                        "from GeneImpl g where g.officialSymbol like :officialSymbol order by g.officialSymbol",
                        officialSymbol );
    }

    /**
     * @see ubic.gemma.model.genome.GeneDao#findByOfficialSymbolInexact(int, java.lang.String, java.lang.String)
     */
    @SuppressWarnings("unchecked")
    public java.util.Collection<Gene> findByOfficialSymbolInexact( final java.lang.String queryString,
            final java.lang.String officialSymbol ) {
        java.util.List<String> argNames = new java.util.ArrayList<String>();
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add( officialSymbol );
        argNames.add( "officialSymbol" );
        java.util.List<?> results = this.getHibernateTemplate().findByNamedParam( queryString,
                argNames.toArray( new String[argNames.size()] ), args.toArray() );
        return ( Collection<Gene> ) results;
    }

    /**
     * @see ubic.gemma.model.genome.GeneDao#findByPhysicalLocation(int, java.lang.String,
     *      ubic.gemma.model.genome.PhysicalLocation)
     */

    @SuppressWarnings("unchecked")
    public java.util.Collection<Gene> findByPhysicalLocation( final java.lang.String queryString,
            final ubic.gemma.model.genome.PhysicalLocation location ) {
        java.util.List<String> argNames = new java.util.ArrayList<String>();
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add( location );
        argNames.add( "location" );
        java.util.List<?> results = this.getHibernateTemplate().findByNamedParam( queryString,
                argNames.toArray( new String[argNames.size()] ), args.toArray() );
        return ( Collection<Gene> ) results;
    }

    /**
     * @see ubic.gemma.model.genome.GeneDao#findByPhysicalLocation(int, ubic.gemma.model.genome.PhysicalLocation)
     */

    @Override
    public java.util.Collection<Gene> findByPhysicalLocation( final ubic.gemma.model.genome.PhysicalLocation location ) {
        return this.findByPhysicalLocation(
                "from ubic.gemma.model.genome.Gene as gene where gene.location = :location", location );
    }

    /**
     * @see ubic.gemma.model.genome.GeneDao#getCoexpressedGenes(ubic.gemma.model.genome.Gene, java.util.Collection,
     *      java.lang.Integer, boolean)
     */
    @Override
    public Map<Gene, CoexpressionCollectionValueObject> getCoexpressedGenes(
            final Collection<ubic.gemma.model.genome.Gene> genes,
            final java.util.Collection<? extends BioAssaySet> ees, final java.lang.Integer stringency,
            final boolean interGeneOnly ) {

        return this.handleGetCoexpressedGenes( genes, ees, stringency, interGeneOnly );

    }

    /**
     * @see ubic.gemma.model.genome.GeneDao#getCoexpressedGenes(ubic.gemma.model.genome.Gene, java.util.Collection,
     *      java.lang.Integer, boolean)
     */
    @Override
    public CoexpressionCollectionValueObject getCoexpressedGenes( final ubic.gemma.model.genome.Gene gene,
            final java.util.Collection<? extends BioAssaySet> ees, final java.lang.Integer stringency ) {
        return this.handleGetCoexpressedGenes( gene, ees, stringency );
    }

    /**
     * @see ubic.gemma.model.genome.GeneDao#getCompositeSequenceCountById(long)
     */
    @Override
    public long getCompositeSequenceCountById( final long id ) {
        return this.handleGetCompositeSequenceCountById( id );

    }

    /**
     * @see ubic.gemma.model.genome.GeneDao#getCompositeSequences(ubic.gemma.model.genome.Gene,
     *      ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    public java.util.Collection<CompositeSequence> getCompositeSequences( final ubic.gemma.model.genome.Gene gene,
            final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {
        return this.handleGetCompositeSequences( gene, arrayDesign );

    }

    /**
     * @see ubic.gemma.model.genome.GeneDao#getCompositeSequencesById(long)
     */
    @Override
    public java.util.Collection<CompositeSequence> getCompositeSequencesById( final long id ) {
        return this.handleGetCompositeSequencesById( id );

    }

    /**
     * @see ubic.gemma.model.genome.GeneDao#getGenesByTaxon(ubic.gemma.model.genome.Taxon)
     */
    @Override
    public java.util.Collection<Gene> getGenesByTaxon( final ubic.gemma.model.genome.Taxon taxon ) {
        return this.handleGetGenesByTaxon( taxon );

    }

    /**
     * @see ubic.gemma.model.genome.GeneDao#getMicroRnaByTaxon(ubic.gemma.model.genome.Taxon)
     */
    @Override
    public java.util.Collection<Gene> getMicroRnaByTaxon( final ubic.gemma.model.genome.Taxon taxon ) {
        return this.handleGetMicroRnaByTaxon( taxon );

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

    @Override
    public Gene load( final java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "Gene.load - 'id' can not be null" );
        }
        final Object entity = this.getHibernateTemplate().get( ubic.gemma.model.genome.GeneImpl.class, id );
        return ( Gene ) entity;
    }

    /**
     * @see ubic.gemma.model.genome.GeneDao#load(java.util.Collection)
     */
    @Override
    public java.util.Collection<Gene> load( final java.util.Collection<Long> ids ) {
        return this.handleLoadMultiple( ids );

    }

    /**
     * @see ubic.gemma.model.genome.GeneDao#loadAll(int)
     */

    @SuppressWarnings("unchecked")
    @Override
    public java.util.Collection<Gene> loadAll() {
        final java.util.Collection<?> results = this.getHibernateTemplate().loadAll(
                ubic.gemma.model.genome.GeneImpl.class );
        return ( Collection<Gene> ) results;
    }

    /**
     * @see ubic.gemma.model.genome.GeneDao#loadKnownGenes(ubic.gemma.model.genome.Taxon)
     */
    @Override
    public java.util.Collection<Gene> loadKnownGenes( final ubic.gemma.model.genome.Taxon taxon ) {
        return this.handleLoadKnownGenes( taxon );

    }

    /**
     * @see ubic.gemma.model.genome.GeneDao#remove(java.lang.Long)
     */
    @Override
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
     * @param probe2ProbeCoexpressionCache the probe2ProbeCoexpressionCache to set
     */
    public void setProbe2ProbeCoexpressionCache( Probe2ProbeCoexpressionCache probe2ProbeCoexpressionCache ) {
        this.probe2ProbeCoexpressionCache = probe2ProbeCoexpressionCache;
    }

    /**
     * @see ubic.gemma.model.genome.GeneDao#thaw(ubic.gemma.model.genome.Gene)
     */
    @Override
    public Gene thaw( final ubic.gemma.model.genome.Gene gene ) {
        return this.handleThaw( gene );

    }

    /**
     * Only thaw the Aliases, very light version
     * 
     * @param gene
     */
    @Override
    public Gene thawAliases( final ubic.gemma.model.genome.Gene gene ) {
        return this.thawAliases( gene );

    }

    /**
     * @see ubic.gemma.model.genome.GeneDao#thawLite(java.util.Collection)
     */
    @Override
    public Collection<Gene> thawLite( final java.util.Collection<Gene> genes ) {
        return this.handleThawLite( genes );

    }

    /**
     * @see ubic.gemma.model.common.SecurableDao#update(java.util.Collection)
     */

    @Override
    public void update( final java.util.Collection<? extends Gene> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "Gene.update - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    @Override
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator<? extends Gene> entityIterator = entities.iterator(); entityIterator
                                .hasNext(); ) {
                            update( entityIterator.next() );
                        }
                        return null;
                    }
                } );
    }

    /**
     * @see ubic.gemma.model.genome.GeneDao#update(ubic.gemma.model.genome.Gene)
     */
    @Override
    public void update( ubic.gemma.model.genome.Gene gene ) {
        if ( gene == null ) {
            throw new IllegalArgumentException( "Gene.update - 'gene' can not be null" );
        }
        this.getHibernateTemplate().update( gene );
    }

    /**
     * Performs the core logic for {@link #countAll()}
     */
    protected abstract java.lang.Integer handleCountAll();

    /**
     * Performs the core logic for
     * {@link #findByAccession(java.lang.String, ubic.gemma.model.common.description.ExternalDatabase)}
     */
    protected abstract ubic.gemma.model.genome.Gene handleFindByAccession( java.lang.String accession,
            ubic.gemma.model.common.description.ExternalDatabase source );

    /**
     * Performs the core logic for {@link #findByAlias(java.lang.String)}
     */
    protected abstract java.util.Collection<Gene> handleFindByAlias( java.lang.String search );

    /**
     * Performs the core logic for {@link #findByOfficialSymbol(java.lang.String, ubic.gemma.model.genome.Taxon)}
     */
    protected abstract ubic.gemma.model.genome.Gene handleFindByOfficialSymbol( java.lang.String symbol,
            ubic.gemma.model.genome.Taxon taxon );

    protected abstract Map<Gene, CoexpressionCollectionValueObject> handleGetCoexpressedGenes( Collection<Gene> genes,
            Collection<? extends BioAssaySet> ees, Integer stringency, boolean interGeneOnly );

    /**
     * Performs the core logic for
     * {@link #getCoexpressedGenes(ubic.gemma.model.genome.Gene, java.util.Collection, java.lang.Integer, boolean)}
     */
    protected abstract CoexpressionCollectionValueObject handleGetCoexpressedGenes( ubic.gemma.model.genome.Gene gene,
            java.util.Collection<? extends BioAssaySet> ees, java.lang.Integer stringency );

    /**
     * Performs the core logic for {@link #getCompositeSequenceCountById(long)}
     */
    protected abstract long handleGetCompositeSequenceCountById( long id );

    /**
     * Performs the core logic for
     * {@link #getCompositeSequences(ubic.gemma.model.genome.Gene, ubic.gemma.model.expression.arrayDesign.ArrayDesign)}
     */
    protected abstract java.util.Collection<CompositeSequence> handleGetCompositeSequences(
            ubic.gemma.model.genome.Gene gene, ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign );

    /**
     * Performs the core logic for {@link #getCompositeSequencesById(long)}
     */
    protected abstract java.util.Collection<CompositeSequence> handleGetCompositeSequencesById( long id );

    /**
     * Performs the core logic for {@link #getGenesByTaxon(ubic.gemma.model.genome.Taxon)}
     */
    protected abstract java.util.Collection<Gene> handleGetGenesByTaxon( ubic.gemma.model.genome.Taxon taxon );

    /**
     * Performs the core logic for {@link #getMicroRnaByTaxon(ubic.gemma.model.genome.Taxon)}
     */
    protected abstract java.util.Collection<Gene> handleGetMicroRnaByTaxon( ubic.gemma.model.genome.Taxon taxon );

    /**
     * Performs the core logic for {@link #loadKnownGenes(ubic.gemma.model.genome.Taxon)}
     */
    protected abstract java.util.Collection<Gene> handleLoadKnownGenes( ubic.gemma.model.genome.Taxon taxon );

    /**
     * Performs the core logic for {@link #load(java.util.Collection)}
     */
    protected abstract java.util.Collection<Gene> handleLoadMultiple( java.util.Collection<Long> ids );

    /**
     * Performs the core logic for {@link #thaw(ubic.gemma.model.genome.Gene)}
     */
    protected abstract Gene handleThaw( ubic.gemma.model.genome.Gene gene );

    /**
     * Performs the core logic for {@link #thawLite(java.util.Collection)}
     */
    protected abstract Collection<Gene> handleThawLite( java.util.Collection<Gene> genes );

}