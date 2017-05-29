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
package ubic.gemma.persistence.service.genome;

import org.hibernate.SessionFactory;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.PhysicalLocation;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.persistence.service.VoEnabledDao;

import java.util.Collection;

/**
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type <code>Gene</code>.
 *
 * @see Gene
 */
public abstract class GeneDaoBase extends VoEnabledDao<Gene, GeneValueObject> implements GeneDao {

    public GeneDaoBase( SessionFactory sessionFactory ) {
        super( Gene.class, sessionFactory );
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

    @Override
    public Gene findByNcbiId( Integer ncbiId ) {
        return ( Gene ) this.getSession().createQuery( "from GeneImpl g where g.ncbiGeneId = :n" )
                .setParameter( "n", ncbiId ).uniqueResult();
    }

    @Override
    public Collection<Gene> findByOfficialSymbol( final String officialSymbol ) {
        return this
                .findByOfficialSymbol( "from GeneImpl g where g.officialSymbol=:officialSymbol order by g.officialName",
                        officialSymbol );
    }

    @Override
    public Collection<Gene> load( Collection<Long> ids ) {
        return this.handleLoadMultiple( ids );
    }

    @Override
    public Collection<Gene> findByOfficialName( final String officialName ) {
        //noinspection unchecked
        return this.getSession()
                .createQuery( "from GeneImpl g where g.officialName=:officialName order by g.officialName" )
                .setParameter( "officialName", officialName ).list();
    }

    /**
     * @see GeneDao#findByOfficialSymbol(String, Taxon)
     */
    @Override
    public Gene findByOfficialSymbol( final String symbol, final Taxon taxon ) {
        return this.handleFindByOfficialSymbol( symbol, taxon );

    }

    @Override
    public Collection<Gene> findByOfficialSymbolInexact( final String officialSymbol ) {
        return this.findByOfficialSymbol(
                "from GeneImpl g where g.officialSymbol like :officialSymbol order by g.officialSymbol",
                officialSymbol );
    }

    @Override
    public Collection<Gene> findByPhysicalLocation( final PhysicalLocation location ) {
        //noinspection unchecked
        return this.getSession().createQuery( "from GeneImpl as gene where gene.physicalLocation = :location" )
                .setParameter( "location", location ).list();
    }

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
     * @see GeneDao#loadKnownGenes(Taxon)
     */
    @Override
    public Collection<Gene> loadKnownGenes( final Taxon taxon ) {
        return this.handleLoadKnownGenes( taxon );

    }

    @Override
    public void thaw( final Gene gene ) {
        this.handleThaw( gene );
    }

    /**
     * @see GeneDao#thawLite(Collection)
     */
    @Override
    public void thawLite( final Collection<Gene> genes ) {
        this.handleThawLite( genes );
    }

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
    protected abstract void handleThaw( Gene gene );

    /**
     * Performs the core logic for {@link #thawLite(Collection)}
     */
    protected abstract void handleThawLite( Collection<Gene> genes );

    private Collection<Gene> findByOfficialSymbol( final String queryString, final String officialSymbol ) {
        //noinspection unchecked
        return this.getSession().createQuery( queryString ).setParameter( "officialSymbol", officialSymbol ).list();
    }

}