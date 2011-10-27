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
package ubic.gemma.model.genome.gene;

import java.util.Collection;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.model.analysis.expression.coexpression.CoexpressionCollectionValueObject;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.GeneDao;
import ubic.gemma.model.genome.PredictedGene;
import ubic.gemma.model.genome.ProbeAlignedRegion;
import ubic.gemma.model.genome.Taxon;

/**
 * Spring Service base class for <code>GeneService</code>, provides access to all services and entities referenced by
 * this service.
 * 
 * @version $Id$
 * @see GeneService
 */
public abstract class GeneServiceBase implements GeneService {

    @Autowired
    private GeneDao geneDao;

    /**
     * @see GeneService#countAll()
     */
    @Override
    public Integer countAll() {
        try {
            return this.handleCountAll();
        } catch ( Throwable th ) {
            throw new GeneServiceException( "Error performing 'GeneService.countAll()' --> " + th, th );
        }
    }

    /**
     * @see GeneService#create(Collection)
     */
    @Override
    public Collection<Gene> create( final Collection<Gene> genes ) {
        try {
            return this.handleCreate( genes );
        } catch ( Throwable th ) {
            throw new GeneServiceException( "Error performing 'GeneService.create(Collection genes)' --> " + th, th );
        }
    }

    /**
     * @see GeneService#create(Gene)
     */
    @Override
    public Gene create( final Gene gene ) {
        try {
            return this.handleCreate( gene );
        } catch ( Throwable th ) {
            throw new GeneServiceException( "Error performing 'GeneService.create(Gene gene)' --> " + th, th );
        }
    }

    /**
     * @see GeneService#find(Gene)
     */
    @Override
    public Gene find( final Gene gene ) {
        try {
            return this.handleFind( gene );
        } catch ( Throwable th ) {
            throw new GeneServiceException( "Error performing 'GeneService.find(Gene gene)' --> " + th, th );
        }
    }

    /**
     * @see GeneService#findByAccession(String, ubic.gemma.model.common.description.ExternalDatabase)
     */
    @Override
    public Gene findByAccession( final String accession,
            final ubic.gemma.model.common.description.ExternalDatabase source ) {
        try {
            return this.handleFindByAccession( accession, source );
        } catch ( Throwable th ) {
            throw new GeneServiceException(
                    "Error performing 'GeneService.findByAccession(String accession, ubic.gemma.model.common.description.ExternalDatabase source)' --> "
                            + th, th );
        }
    }

    /**
     * @see GeneService#findByAlias(String)
     */
    @Override
    public Collection<Gene> findByAlias( final String search ) {
        try {
            return this.handleFindByAlias( search );
        } catch ( Throwable th ) {
            throw new GeneServiceException( "Error performing 'GeneService.findByAlias(String search)' --> " + th, th );
        }
    }

    /**
     * @see GeneService#findByNCBIId(String)
     */
    @Override
    public Gene findByNCBIId( Integer accession ) {
        try {
            return this.handleFindByNCBIId( accession );
        } catch ( Throwable th ) {
            throw new GeneServiceException( "Error performing 'GeneService.findByNCBIId(String accession)' --> " + th,
                    th );
        }
    }

    /**
     * @see GeneService#findByOfficialName(String)
     */
    @Override
    public Collection<Gene> findByOfficialName( final String officialName ) {
        try {
            return this.handleFindByOfficialName( officialName );
        } catch ( Throwable th ) {
            throw new GeneServiceException(
                    "Error performing 'GeneService.findByOfficialName(String officialName)' --> " + th, th );
        }
    }

    /**
     * @see GeneService#findByOfficialSymbol(String)
     */
    @Override
    public Collection<Gene> findByOfficialSymbol( final String officialSymbol ) {
        try {
            return this.handleFindByOfficialSymbol( officialSymbol );
        } catch ( Throwable th ) {
            throw new GeneServiceException(
                    "Error performing 'GeneService.findByOfficialSymbol(String officialSymbol)' --> " + th, th );
        }
    }

    /**
     * @see GeneService#findByOfficialSymbol(String, Taxon)
     */
    @Override
    public Gene findByOfficialSymbol( final String symbol, final Taxon taxon ) {
        try {
            return this.handleFindByOfficialSymbol( symbol, taxon );
        } catch ( Throwable th ) {
            throw new GeneServiceException(
                    "Error performing 'GeneService.findByOfficialSymbol(String symbol, Taxon taxon)' --> " + th, th );
        }
    }

    /**
     * @see GeneService#findByOfficialSymbolInexact(String)
     */
    @Override
    public Collection<Gene> findByOfficialSymbolInexact( final String officialSymbol ) {
        try {
            return this.handleFindByOfficialSymbolInexact( officialSymbol );
        } catch ( Throwable th ) {
            throw new GeneServiceException(
                    "Error performing 'GeneService.findByOfficialSymbolInexact(String officialSymbol)' --> " + th, th );
        }
    }

    /**
     * @see GeneService#findOrCreate(Gene)
     */
    @Override
    public Gene findOrCreate( final Gene gene ) {
        try {
            return this.handleFindOrCreate( gene );
        } catch ( Throwable th ) {
            throw new GeneServiceException( "Error performing 'GeneService.findOrCreate(Gene gene)' --> " + th, th );
        }
    }

    /**
     * @see GeneService#getCoexpressedGenes(Gene, Collection, Integer, boolean)
     */
    @Override
    public Map<Gene, CoexpressionCollectionValueObject> getCoexpressedGenes( final Collection<Gene> genes,
            final Collection<? extends BioAssaySet> ees, final Integer stringency, final boolean interGenesOnly ) {
        try {
            return this.handleGetCoexpressedGenes( genes, ees, stringency, interGenesOnly );
        } catch ( Throwable th ) {
            throw new GeneServiceException(
                    "Error performing 'GeneService.getCoexpressedGenes(Collection<Gene> genes, Collection ees, Integer stringency, boolean knownGenesOnly)' --> "
                            + th, th );
        }
    }

    /**
     * @see GeneService#getCoexpressedGenes(Gene, Collection, Integer, boolean)
     */
    @Override
    public CoexpressionCollectionValueObject getCoexpressedGenes( final Gene gene,
            final Collection<? extends BioAssaySet> ees, final Integer stringency ) {
        try {
            return this.handleGetCoexpressedGenes( gene, ees, stringency );
        } catch ( Throwable th ) {
            throw new GeneServiceException(
                    "Error performing 'GeneService.getCoexpressedGenes(Gene gene, Collection ees, Integer stringency, boolean knownGenesOnly)' --> "
                            + th, th );
        }
    }

    /**
     * @see GeneService#getCompositeSequenceCountById(Long)
     */
    @Override
    public long getCompositeSequenceCountById( final Long id ) {
        try {
            return this.handleGetCompositeSequenceCountById( id );
        } catch ( Throwable th ) {
            throw new GeneServiceException(
                    "Error performing 'GeneService.getCompositeSequenceCountById(Long id)' --> " + th, th );
        }
    }

    /**
     * @see GeneService#getCompositeSequences(Gene, ArrayDesign)
     */
    @Override
    public Collection<CompositeSequence> getCompositeSequences( final Gene gene, final ArrayDesign arrayDesign ) {
        try {
            return this.handleGetCompositeSequences( gene, arrayDesign );
        } catch ( Throwable th ) {
            throw new GeneServiceException(
                    "Error performing 'GeneService.getCompositeSequences(Gene gene, ArrayDesign arrayDesign)' --> "
                            + th, th );
        }
    }

    /**
     * @see GeneService#getCompositeSequencesById(Long)
     */
    @Override
    public Collection<CompositeSequence> getCompositeSequencesById( final Long id ) {
        try {
            return this.handleGetCompositeSequencesById( id );
        } catch ( Throwable th ) {
            throw new GeneServiceException( "Error performing 'GeneService.getCompositeSequencesById(Long id)' --> "
                    + th, th );
        }
    }

    /**
     * @see GeneService#getGenesByTaxon(Taxon)
     */
    @Override
    public Collection<Gene> getGenesByTaxon( final Taxon taxon ) {
        try {
            return this.handleGetGenesByTaxon( taxon );
        } catch ( Throwable th ) {
            throw new GeneServiceException( "Error performing 'GeneService.getGenesByTaxon(Taxon taxon)' --> " + th, th );
        }
    }

    /**
     * @see GeneService#loadMicroRNAs(Taxon)
     */
    @Override
    public Collection<Gene> loadMicroRNAs( final Taxon taxon ) {
        try {
            return this.handleGetMicroRnaByTaxon( taxon );
        } catch ( Throwable th ) {
            throw new GeneServiceException( "Error performing 'GeneService.getMicroRnaByTaxon(Taxon taxon)' --> " + th,
                    th );
        }
    }

    /**
     * @see GeneService#load(long)
     */
    @Override
    public Gene load( final long id ) {
        try {
            return this.handleLoad( id );
        } catch ( Throwable th ) {
            throw new GeneServiceException( "Error performing 'GeneService.load(long id)' --> " + th, th );
        }
    }

    /**
     * @see GeneService#loadAll()
     */
    @Override
    public Collection<Gene> loadAll() {
        try {
            return this.handleLoadAll();
        } catch ( Throwable th ) {
            throw new GeneServiceException( "Error performing 'GeneService.loadAll()' --> " + th, th );
        }
    }

    /**
     * @see GeneService#loadKnownGenes(Taxon)
     */
    @Override
    public Collection<Gene> loadKnownGenes( final Taxon taxon ) {
        try {
            return this.handleLoadKnownGenes( taxon );
        } catch ( Throwable th ) {
            throw new GeneServiceException( "Error performing 'GeneService.loadKnownGenes(Taxon taxon)' --> " + th, th );
        }
    }

    /**
     * @see GeneService#loadMultiple(Collection)
     */
    @Override
    public Collection<Gene> loadMultiple( final Collection<Long> ids ) {
        try {
            return this.handleLoadMultiple( ids );
        } catch ( Throwable th ) {
            throw new GeneServiceException( "Error performing 'GeneService.loadMultiple(Collection ids)' --> " + th, th );
        }
    }

    /**
     * @see GeneService#loadPredictedGenes(Taxon)
     */
    @Override
    public Collection<PredictedGene> loadPredictedGenes( final Taxon taxon ) {
        try {
            return this.handleLoadPredictedGenes( taxon );
        } catch ( Throwable th ) {
            throw new GeneServiceException( "Error performing 'GeneService.loadPredictedGenes(Taxon taxon)' --> " + th,
                    th );
        }
    }

    /**
     * @see GeneService#loadProbeAlignedRegions(Taxon)
     */
    @Override
    public Collection<ProbeAlignedRegion> loadProbeAlignedRegions( final Taxon taxon ) {
        try {
            return this.handleLoadProbeAlignedRegions( taxon );
        } catch ( Throwable th ) {
            throw new GeneServiceException( "Error performing 'GeneService.loadProbeAlignedRegions(Taxon taxon)' --> "
                    + th, th );
        }
    }

    /**
     * @see GeneService#remove(String)
     */
    @Override
    public void remove( Gene gene ) {
        try {
            this.handleRemove( gene );
        } catch ( Throwable th ) {
            throw new GeneServiceException( "Error performing 'GeneService.remove(String officialName)' --> " + th, th );
        }
    }

    /**
     * @see GeneService#remove(Collection)
     */
    @Override
    public void remove( final Collection<Gene> genes ) {
        try {
            this.handleRemove( genes );
        } catch ( Throwable th ) {
            throw new GeneServiceException( "Error performing 'GeneService.remove(Collection genes)' --> " + th, th );
        }
    }

    /**
     * Sets the reference to <code>gene</code>'s DAO.
     */
    public void setGeneDao( GeneDao geneDao ) {
        this.geneDao = geneDao;
    }

    /**
     * @see GeneService#thaw(Gene)
     */
    @Override
    public Gene thaw( final Gene gene ) {
        try {
            return this.handleThaw( gene );
        } catch ( Throwable th ) {
            throw new GeneServiceException( "Error performing 'GeneService.thaw(Gene gene)' --> " + th, th );
        }
    }

    /**
     * @see GeneService#thawLite(Collection)
     */
    @Override
    public Collection<Gene> thawLite( final Collection<Gene> genes ) {
        try {
            return this.handleThawLite( genes );
        } catch ( Throwable th ) {
            throw new GeneServiceException( "Error performing 'GeneService.thawLite(Collection genes)' --> " + th, th );
        }
    }

    /**
     * @see GeneService#update(Gene)
     */
    @Override
    public void update( final Gene gene ) {
        try {
            this.handleUpdate( gene );
        } catch ( Throwable th ) {
            throw new GeneServiceException( "Error performing 'GeneService.update(Gene gene)' --> " + th, th );
        }
    }

    /**
     * Gets the reference to <code>gene</code>'s DAO.
     */
    protected GeneDao getGeneDao() {
        return this.geneDao;
    }

    /**
     * Performs the core logic for {@link #countAll()}
     */
    protected abstract Integer handleCountAll() throws Exception;

    /**
     * Performs the core logic for {@link #create(Collection)}
     */
    protected abstract Collection<Gene> handleCreate( Collection<Gene> genes ) throws Exception;

    /**
     * Performs the core logic for {@link #create(Gene)}
     */
    protected abstract Gene handleCreate( Gene gene ) throws Exception;

    /**
     * Performs the core logic for {@link #find(Gene)}
     */
    protected abstract Gene handleFind( Gene gene ) throws Exception;

    /**
     * Performs the core logic for
     * {@link #findByAccession(String, ubic.gemma.model.common.description.ExternalDatabase)}
     */
    protected abstract Gene handleFindByAccession( String accession,
            ubic.gemma.model.common.description.ExternalDatabase source ) throws Exception;

    /**
     * Performs the core logic for {@link #findByAlias(String)}
     */
    protected abstract Collection<Gene> handleFindByAlias( String search ) throws Exception;

    /**
     * Performs the core logic for {@link #findByNCBIId(Integer)}
     */
    protected abstract Gene handleFindByNCBIId( Integer accession ) throws Exception;

    /**
     * Performs the core logic for {@link #findByOfficialName(String)}
     */
    protected abstract Collection<Gene> handleFindByOfficialName( String officialName ) throws Exception;

    /**
     * Performs the core logic for {@link #findByOfficialSymbol(String)}
     */
    protected abstract Collection<Gene> handleFindByOfficialSymbol( String officialSymbol ) throws Exception;

    /**
     * Performs the core logic for {@link #findByOfficialSymbol(String, Taxon)}
     */
    protected abstract Gene handleFindByOfficialSymbol( String symbol, Taxon taxon ) throws Exception;

    /**
     * Performs the core logic for {@link #findByOfficialSymbolInexact(String)}
     */
    protected abstract Collection<Gene> handleFindByOfficialSymbolInexact( String officialSymbol ) throws Exception;

    /**
     * Performs the core logic for {@link #findOrCreate(Gene)}
     */
    protected abstract Gene handleFindOrCreate( Gene gene ) throws Exception;

    /**
     * Performs the core logic for {@link #getCoexpressedGenes(Gene, Collection, Integer, boolean)}
     */
    protected abstract Map<Gene, CoexpressionCollectionValueObject> handleGetCoexpressedGenes( Collection<Gene> genes,
            Collection<? extends BioAssaySet> ees, Integer stringency, boolean interGenesOnly ) throws Exception;

    /**
     * Performs the core logic for {@link #getCoexpressedGenes(Gene, Collection, Integer, boolean)}
     */
    protected abstract CoexpressionCollectionValueObject handleGetCoexpressedGenes( Gene gene,
            Collection<? extends BioAssaySet> ees, Integer stringency ) throws Exception;

    /**
     * Performs the core logic for {@link #getCompositeSequenceCountById(Long)}
     */
    protected abstract long handleGetCompositeSequenceCountById( Long id ) throws Exception;

    /**
     * Performs the core logic for {@link #getCompositeSequences(Gene, ArrayDesign)}
     */
    protected abstract Collection<CompositeSequence> handleGetCompositeSequences( Gene gene, ArrayDesign arrayDesign )
            throws Exception;

    /**
     * Performs the core logic for {@link #getCompositeSequencesById(Long)}
     */
    protected abstract Collection<CompositeSequence> handleGetCompositeSequencesById( Long id ) throws Exception;

    /**
     * Performs the core logic for {@link #getGenesByTaxon(Taxon)}
     */
    protected abstract Collection<Gene> handleGetGenesByTaxon( Taxon taxon ) throws Exception;

    /**
     * Performs the core logic for {@link #loadMicroRNAs(Taxon)}
     */
    protected abstract Collection<Gene> handleGetMicroRnaByTaxon( Taxon taxon ) throws Exception;

    /**
     * Performs the core logic for {@link #load(long)}
     */
    protected abstract Gene handleLoad( long id ) throws Exception;

    /**
     * Performs the core logic for {@link #loadAll()}
     */
    protected abstract Collection<Gene> handleLoadAll() throws Exception;

    /**
     * Performs the core logic for {@link #loadKnownGenes(Taxon)}
     */
    protected abstract Collection<Gene> handleLoadKnownGenes( Taxon taxon ) throws Exception;

    /**
     * Performs the core logic for {@link #loadMultiple(Collection)}
     */
    protected abstract Collection<Gene> handleLoadMultiple( Collection<Long> ids ) throws Exception;

    /**
     * Performs the core logic for {@link #loadPredictedGenes(Taxon)}
     */
    protected abstract Collection<PredictedGene> handleLoadPredictedGenes( Taxon taxon ) throws Exception;

    /**
     * Performs the core logic for {@link #loadProbeAlignedRegions(Taxon)}
     */
    protected abstract Collection<ProbeAlignedRegion> handleLoadProbeAlignedRegions( Taxon taxon ) throws Exception;

    /**
     */
    protected abstract void handleRemove( Gene gene ) throws Exception;

    /**
     * Performs the core logic for {@link #remove(Collection)}
     */
    protected abstract void handleRemove( Collection<Gene> genes ) throws Exception;

    /**
     * Performs the core logic for {@link #thaw(Gene)}
     */
    protected abstract Gene handleThaw( Gene gene ) throws Exception;

    /**
     * Performs the core logic for {@link #thawLite(Collection)}
     */
    protected abstract Collection<Gene> handleThawLite( Collection<Gene> genes ) throws Exception;

    /**
     * Performs the core logic for {@link #update(Gene)}
     */
    protected abstract void handleUpdate( Gene gene ) throws Exception;

}