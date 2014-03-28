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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.util.Settings;

/**
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type <code>Gene</code>.
 * 
 * @see Gene
 */
public abstract class GeneDaoBase extends HibernateDaoSupport implements GeneDao {

    /**
     * @see GeneDao#countAll()
     */
    @Override
    public Integer countAll() {
        return this.handleCountAll();

    }

    /**
     * @see GeneDao#create(int, Collection)
     */
    @Override
    public Collection<? extends Gene> create( final Collection<? extends Gene> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "Gene.create - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession( new HibernateCallback<Object>() {
            @Override
            public Object doInHibernate( Session session ) throws HibernateException {
                for ( Iterator<? extends Gene> entityIterator = entities.iterator(); entityIterator.hasNext(); ) {
                    create( entityIterator.next() );
                }
                return null;
            }
        } );
        return entities;
    }

    /**
     * @see GeneDao#create(int transform, Gene)
     */
    @Override
    public Gene create( final Gene gene ) {
        if ( gene == null ) {
            throw new IllegalArgumentException( "Gene.create - 'gene' can not be null" );
        }
        this.getHibernateTemplate().save( gene );
        return gene;
    }

    /**
     * @see GeneDao#findByAccession(String, ubic.gemma.model.common.description.ExternalDatabase)
     */
    @Override
    public Gene findByAccession( final String accession, final ExternalDatabase source ) {
        return this.handleFindByAccession( accession, source );

    }

    /**
     * @see GeneDao#findByAlias(String)
     */
    @Override
    public Collection<Gene> findByAlias( final String search ) {
        return this.handleFindByAlias( search );

    }

    /**
     * @see GeneDao#findByNcbiId(int, String, String)
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
     * @see GeneDao#findByOfficalSymbol(int, String)
     */
    @Override
    public Collection<Gene> findByOfficalSymbol( final String officialSymbol ) {
        return this.findByOfficalSymbol(
                "from GeneImpl g where g.officialSymbol=:officialSymbol order by g.officialName", officialSymbol );
    }

    /**
     * @see GeneDao#findByOfficalSymbol(int, String, String)
     */
    @SuppressWarnings("unchecked")
    public Collection<Gene> findByOfficalSymbol( final String queryString, final String officialSymbol ) {
        java.util.List<String> argNames = new ArrayList<String>();
        java.util.List<Object> args = new ArrayList<Object>();
        args.add( officialSymbol );
        argNames.add( "officialSymbol" );
        java.util.List<?> results = this.getHibernateTemplate().findByNamedParam( queryString,
                argNames.toArray( new String[argNames.size()] ), args.toArray() );
        return ( Collection<Gene> ) results;
    }

    /**
     * @see GeneDao#findByOfficialName(int, String)
     */
    @Override
    public Collection<Gene> findByOfficialName( final String officialName ) {
        return this.findByOfficialName( "from GeneImpl g where g.officialName=:officialName order by g.officialName",
                officialName );
    }

    /**
     * @see GeneDao#findByOfficialName(int, String, String)
     */

    @SuppressWarnings("unchecked")
    public Collection<Gene> findByOfficialName( final String queryString, final String officialName ) {
        List<String> argNames = new ArrayList<String>();
        List<Object> args = new ArrayList<Object>();
        args.add( officialName );
        argNames.add( "officialName" );
        java.util.List<?> results = this.getHibernateTemplate().findByNamedParam( queryString,
                argNames.toArray( new String[argNames.size()] ), args.toArray() );

        return ( Collection<Gene> ) results;
    }

    /**
     * @see GeneDao#findByOfficialSymbol(String, Taxon)
     */
    @Override
    public Gene findByOfficialSymbol( final String symbol, final Taxon taxon ) {
        return this.handleFindByOfficialSymbol( symbol, taxon );

    }

    /**
     * @see GeneDao#findByOfficialSymbolInexact(int, String)
     */
    @Override
    public Collection<Gene> findByOfficialSymbolInexact( final String officialSymbol ) {
        return this
                .findByOfficialSymbolInexact(
                        "from GeneImpl g where g.officialSymbol like :officialSymbol order by g.officialSymbol",
                        officialSymbol );
    }

    /**
     * @see GeneDao#findByOfficialSymbolInexact(int, String, String)
     */
    @SuppressWarnings("unchecked")
    public Collection<Gene> findByOfficialSymbolInexact( final String queryString, final String officialSymbol ) {
        java.util.List<String> argNames = new ArrayList<String>();
        java.util.List<Object> args = new ArrayList<Object>();
        args.add( officialSymbol );
        argNames.add( "officialSymbol" );
        java.util.List<?> results = this.getHibernateTemplate().findByNamedParam( queryString,
                argNames.toArray( new String[argNames.size()] ), args.toArray() );
        return ( Collection<Gene> ) results;
    }

    /**
     * @see GeneDao#findByPhysicalLocation(int, String, ubic.gemma.model.genome.PhysicalLocation)
     */

    @SuppressWarnings("unchecked")
    public Collection<Gene> findByPhysicalLocation( final String queryString,
            final ubic.gemma.model.genome.PhysicalLocation location ) {
        java.util.List<String> argNames = new ArrayList<String>();
        java.util.List<Object> args = new ArrayList<Object>();
        args.add( location );
        argNames.add( "location" );
        java.util.List<?> results = this.getHibernateTemplate().findByNamedParam( queryString,
                argNames.toArray( new String[argNames.size()] ), args.toArray() );
        return ( Collection<Gene> ) results;
    }

    /**
     * @see GeneDao#findByPhysicalLocation(int, ubic.gemma.model.genome.PhysicalLocation)
     */

    @Override
    public Collection<Gene> findByPhysicalLocation( final PhysicalLocation location ) {
        return this.findByPhysicalLocation( "from Gene as gene where gene.location = :location", location );
    }

    //
    // /**
    // * @see GeneDao#getCoexpressedGenes(Gene, Collection, Integer, boolean)
    // */
    // @Override
    // public Map<Gene, QueryGeneCoexpression> getCoexpressedGenes( final Collection<Gene> genes,
    // final Collection<? extends BioAssaySet> ees, final Integer stringency, final boolean interGeneOnly ) {
    //
    // return this.handleGetCoexpressedGenes( genes, ees, stringency, interGeneOnly );
    //
    // }
    //
    // /**
    // * @see GeneDao#getCoexpressedGenes(Gene, Collection, Integer, boolean)
    // */
    // @Override
    // public QueryGeneCoexpression getCoexpressedGenes( final Gene gene, final Collection<? extends BioAssaySet> ees,
    // final Integer stringency ) {
    // return this.handleGetCoexpressedGenes( gene, ees, stringency );
    // }

    /**
     * @see GeneDao#getCompositeSequenceCountById(long)
     */
    @Override
    public long getCompositeSequenceCountById( final long id ) {
        return this.handleGetCompositeSequenceCountById( id );

    }

    /**
     * @see GeneDao#getCompositeSequences(Gene, ArrayDesign)
     */
    @Override
    public Collection<CompositeSequence> getCompositeSequences( final Gene gene, final ArrayDesign arrayDesign ) {
        return this.handleGetCompositeSequences( gene, arrayDesign );

    }

    /**
     * @see GeneDao#getCompositeSequencesById(long)
     */
    @Override
    public Collection<CompositeSequence> getCompositeSequencesById( final long id ) {
        return this.handleGetCompositeSequencesById( id );

    }

    /**
     * @see GeneDao#getGenesByTaxon(Taxon)
     */
    @Override
    public Collection<Gene> getGenesByTaxon( final Taxon taxon ) {
        return this.handleGetGenesByTaxon( taxon );

    }

    /**
     * @see GeneDao#getMicroRnaByTaxon(Taxon)
     */
    @Override
    public Collection<Gene> getMicroRnaByTaxon( final Taxon taxon ) {
        return this.handleGetMicroRnaByTaxon( taxon );

    }

    /**
     * @see GeneDao#load(int, java.lang.Long)
     */

    @Override
    public Gene load( final java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "Gene.load - 'id' can not be null" );
        }
        final Object entity = this.getHibernateTemplate().get( GeneImpl.class, id );
        return ( Gene ) entity;
    }

    /**
     * @see GeneDao#load(Collection)
     */
    @Override
    public Collection<Gene> load( final Collection<Long> ids ) {
        return this.handleLoadMultiple( ids );

    }

    /**
     * @see GeneDao#loadAll(int)
     */

    @SuppressWarnings("unchecked")
    @Override
    public Collection<Gene> loadAll() {
        final Collection<?> results = this.getHibernateTemplate().loadAll( GeneImpl.class );
        return ( Collection<Gene> ) results;
    }

    /**
     * @see GeneDao#loadKnownGenes(Taxon)
     */
    @Override
    public Collection<Gene> loadKnownGenes( final Taxon taxon ) {
        return this.handleLoadKnownGenes( taxon );

    }

    /**
     * @see GeneDao#remove(java.lang.Long)
     */
    @Override
    public void remove( java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "Gene.remove - 'id' can not be null" );
        }
        Gene entity = this.load( id );
        if ( entity != null ) {
            this.remove( entity );
        }
    }

    /**
     * @see GeneDao#thaw(Gene)
     */
    @Override
    public Gene thaw( final Gene gene ) {
        return this.handleThaw( gene );

    }

    /**
     * Only thaw the Aliases, very light version
     * 
     * @param gene
     */
    @Override
    public Gene thawAliases( final Gene gene ) {
        return this.thawAliases( gene );

    }

    /**
     * @see GeneDao#thawLite(Collection)
     */
    @Override
    public Collection<Gene> thawLite( final Collection<Gene> genes ) {
        return this.handleThawLite( genes );

    }

    /**
     * @see ubic.gemma.model.common.SecurableDao#update(Collection)
     */

    @Override
    public void update( final Collection<? extends Gene> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "Gene.update - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession( new HibernateCallback<Object>() {
            @Override
            public Object doInHibernate( Session session ) throws HibernateException {
                int i = 0;
                int batchSize = Settings.getInt( "gemma.hibernate.jdbc_batch_size" );

                for ( Iterator<? extends Gene> entityIterator = entities.iterator(); entityIterator.hasNext(); ) {
                    update( entityIterator.next() );
                    if ( i++ % batchSize == 0 ) {
                        session.flush();
                        session.clear();
                    }
                }
                return null;
            }
        } );
    }

    /**
     * @see GeneDao#update(Gene)
     */
    @Override
    public void update( Gene gene ) {
        if ( gene == null ) {
            throw new IllegalArgumentException( "Gene.update - 'gene' can not be null" );
        }
        this.getHibernateTemplate().update( gene );
    }

    /**
     * Performs the core logic for {@link #countAll()}
     */
    protected abstract Integer handleCountAll();

    /**
     * Performs the core logic for
     * {@link #findByAccession(String, ubic.gemma.model.common.description.ExternalDatabase)}
     */
    protected abstract Gene handleFindByAccession( String accession,
            ubic.gemma.model.common.description.ExternalDatabase source );

    /**
     * Performs the core logic for {@link #findByAlias(String)}
     */
    protected abstract Collection<Gene> handleFindByAlias( String search );

    /**
     * Performs the core logic for {@link #findByOfficialSymbol(String, Taxon)}
     */
    protected abstract Gene handleFindByOfficialSymbol( String symbol, Taxon taxon );

    // protected abstract Map<Gene, QueryGeneCoexpression> handleGetCoexpressedGenes( Collection<Gene> genes,
    // Collection<? extends BioAssaySet> ees, Integer stringency, boolean interGeneOnly );
    //
    // /**
    // * Performs the core logic for {@link #getCoexpressedGenes(Gene, Collection, Integer, boolean)}
    // */
    // protected abstract QueryGeneCoexpression handleGetCoexpressedGenes( Gene gene,
    // Collection<? extends BioAssaySet> ees, Integer stringency );

    /**
     * Performs the core logic for {@link #getCompositeSequenceCountById(long)}
     */
    protected abstract long handleGetCompositeSequenceCountById( long id );

    /**
     * Performs the core logic for {@link #getCompositeSequences(Gene, ArrayDesign)}
     */
    protected abstract Collection<CompositeSequence> handleGetCompositeSequences( Gene gene, ArrayDesign arrayDesign );

    /**
     * Performs the core logic for {@link #getCompositeSequencesById(long)}
     */
    protected abstract Collection<CompositeSequence> handleGetCompositeSequencesById( long id );

    /**
     * Performs the core logic for {@link #getGenesByTaxon(Taxon)}
     */
    protected abstract Collection<Gene> handleGetGenesByTaxon( Taxon taxon );

    /**
     * Performs the core logic for {@link #getMicroRnaByTaxon(Taxon)}
     */
    protected abstract Collection<Gene> handleGetMicroRnaByTaxon( Taxon taxon );

    /**
     * Performs the core logic for {@link #loadKnownGenes(Taxon)}
     */
    protected abstract Collection<Gene> handleLoadKnownGenes( Taxon taxon );

    /**
     * Performs the core logic for {@link #load(Collection)}
     */
    protected abstract Collection<Gene> handleLoadMultiple( Collection<Long> ids );

    /**
     * Performs the core logic for {@link #thaw(Gene)}
     */
    protected abstract Gene handleThaw( Gene gene );

    /**
     * Performs the core logic for {@link #thawLite(Collection)}
     */
    protected abstract Collection<Gene> handleThawLite( Collection<Gene> genes );

}