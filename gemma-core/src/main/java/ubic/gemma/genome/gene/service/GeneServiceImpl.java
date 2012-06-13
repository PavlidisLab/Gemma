/*
 * The Gemma project.
 * 
 * Copyright (c) 2006-2008 University of British Columbia
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

package ubic.gemma.genome.gene.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ubic.gemma.genome.gene.GeneSetValueObjectHelper;
import ubic.gemma.loader.genome.gene.ncbi.homology.HomologeneService;
import ubic.gemma.model.analysis.expression.coexpression.CoexpressionCollectionValueObject;
import ubic.gemma.model.association.Gene2GOAssociation;
import ubic.gemma.model.association.Gene2GOAssociationService;
import ubic.gemma.model.association.coexpression.GeneCoexpressionNodeDegree;
import ubic.gemma.model.common.description.AnnotationValueObject;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.genome.Chromosome;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.GeneDao;
import ubic.gemma.model.genome.PhysicalLocation;
import ubic.gemma.model.genome.RelativeLocationData;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneAlias;
import ubic.gemma.model.genome.gene.GeneProduct;
import ubic.gemma.model.genome.gene.GeneProductValueObject;
import ubic.gemma.model.genome.gene.GeneSet;
import ubic.gemma.model.genome.gene.GeneSetValueObject;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.model.genome.sequenceAnalysis.AnnotationAssociationService;
import ubic.gemma.ontology.providers.GeneOntologyService;
import ubic.gemma.search.GeneSetSearch;

/**
 * @author pavlidis
 * @author keshav
 * @version $Id$
 * @see gene.GeneService
 */
@Service
public class GeneServiceImpl implements GeneService {

    private static Log log = LogFactory.getLog( GeneServiceImpl.class.getName() );

    @Autowired
    private GeneSetSearch geneSetSearch;

    @Autowired
    private HomologeneService homologeneService;

    @Autowired
    private Gene2GOAssociationService gene2GOAssociationService;

    @Autowired
    private GeneOntologyService geneOntologyService;

    @Autowired
    private AnnotationAssociationService annotationAssociationService;

    @Autowired
    private GeneSetValueObjectHelper geneSetValueObjectHelper;

    @Autowired
    private GeneDao geneDao;

    @Override
    public Collection<Gene> find( PhysicalLocation physicalLocation ) {
        return this.getGeneDao().find( physicalLocation );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.gene.GeneService#findByOfficialNameInexact(java.lang.String)
     */
    @Override
    public Collection<Gene> findByOfficialNameInexact( String officialName ) {
        return this.getGeneDao().findByOfficialNameInexact( officialName );
    }

    @Override
    public RelativeLocationData findNearest( PhysicalLocation physicalLocation, boolean useStrand ) {
        return this.getGeneDao().findNearest( physicalLocation, useStrand );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.gene.GeneService#getGeneCoexpressionNodeDegree(java.util.Collection)
     */
    @Override
    public Map<Gene, GeneCoexpressionNodeDegree> getGeneCoexpressionNodeDegree( Collection<Gene> genes ) {
        return this.getGeneDao().getGeneCoexpressionNodeDegree( genes );
    }

    @Override
    public GeneCoexpressionNodeDegree getGeneCoexpressionNodeDegree( Gene gene ) {
        return this.getGeneDao().getGeneCoexpressionNodeDegree( gene );
    }

    @Override
    public Map<BioAssaySet, Double> getGeneCoexpressionNodeDegree( Gene gene, Collection<? extends BioAssaySet> ees ) {
        return this.getGeneDao().getGeneCoexpressionNodeDegree( gene, ees );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.gene.GeneService#getMaxPhysicalLength(ubic.gemma.model.genome.Gene)
     */
    @Override
    public PhysicalLocation getMaxPhysicalLength( Gene gene ) {
        if ( gene == null ) return null;

        Collection<GeneProduct> gpCollection = gene.getProducts();

        if ( gpCollection == null ) return null;

        Long minStartNt = Long.MAX_VALUE;
        Long maxEndNt = Long.MIN_VALUE;
        String strand = null;
        Chromosome chromosome = null;

        for ( GeneProduct gp : gpCollection ) {

            PhysicalLocation pLoc = gp.getPhysicalLocation();
            if ( pLoc == null ) {
                log.warn( "No physical location for Gene: " + gene.getOfficialSymbol() + "'s Gene Product: "
                        + gp.getId() + ". Skipping." );
                continue;
            }

            String currentStrand = pLoc.getStrand();
            Chromosome currentChromosone = pLoc.getChromosome();
            Long currentStartNt = pLoc.getNucleotide();
            Long currentEndNt = currentStartNt + pLoc.getNucleotideLength();

            // 1st time through loop
            if ( minStartNt == Long.MAX_VALUE ) {
                minStartNt = currentStartNt;
                maxEndNt = currentEndNt;
                strand = currentStrand;
                chromosome = currentChromosone;
                continue;
            }

            // FIXME: This is defensive coding. Not sure if this will ever happen. If it does, will need to sort the
            // gene products in advance to remove the outliers. Currently this method is assuming the 1st gene product
            // is not the outlier.
            if ( !currentStrand.equalsIgnoreCase( strand ) ) {
                log.warn( "Gene products for "
                        + gene.getOfficialSymbol()
                        + " , Id="
                        + gene.getId()
                        + " are on different strands. Unable to compute distance when products are on different strands. Skipping Gene product: "
                        + gp.getId() );
                continue;
            }

            if ( !currentChromosone.equals( chromosome ) ) {
                log.warn( "Gene products for "
                        + gene.getOfficialSymbol()
                        + " , Id="
                        + gene.getId()
                        + " are on different chromosones. Unable to compute distance when gene products are on different chromosomes. Skipping Gene product: "
                        + gp.getId() );

                continue;
            }

            if ( currentStartNt < minStartNt ) minStartNt = currentStartNt;

            if ( currentEndNt > maxEndNt ) maxEndNt = currentEndNt;

        } // for each gene product

        Long length = maxEndNt - minStartNt;
        PhysicalLocation result = PhysicalLocation.Factory.newInstance();
        result.setChromosome( chromosome );
        result.setNucleotide( minStartNt );
        result.setNucleotideLength( length.intValue() );
        result.setStrand( strand ); // bin is null.
        return result;

    }

    public Collection<Gene> handleThawLite( Collection<Gene> genes ) {
        return this.getGeneDao().thawLite( genes );
    }

    @Override
    public Collection<Gene> loadThawed( Collection<Long> ids ) {
        return this.getGeneDao().loadThawed( ids );
    }

    @Override
    public Collection<GeneValueObject> loadValueObjects( Collection<Long> ids ) {
        Collection<Gene> g = this.getGeneDao().loadThawed( ids );
        return GeneValueObject.convert2ValueObjects( g );
    }

    @Override
    public GeneValueObject loadValueObject( Long id ) {
        Collection<Long> ids = new ArrayList<Long>( 1 );
        ids.add( id );
        Collection<Gene> g = this.getGeneDao().loadThawed( ids );
        if ( g == null || g.isEmpty() ) return null;
        return GeneValueObject.convert2ValueObjects( g ).iterator().next();
    }

    @Override
    public Gene thawLite( Gene gene ) {
        return this.getGeneDao().thawLite( gene );
    }

    /**
     * @see gene.GeneService#handleFindByID(java.lang.long)
     */
    protected Gene handleFindByID( Long id ) {
        return this.getGeneDao().load( id );
    }

    // @Override
    // protected Collection<PredictedGene> handleLoadPredictedGenes( Taxon taxon ) throws Exception {
    // return this.getGeneDao().loadPredictedGenes( taxon );
    // }
    //
    // @Override
    // protected Collection<ProbeAlignedRegion> handleLoadProbeAlignedRegions( Taxon taxon ) throws Exception {
    // return this.getGeneDao().loadProbeAlignedRegions( taxon );
    // }

    /**
     * This was created because calling saveGene from Spring causes caching errors. I left saveGene in place on the
     * assumption that Kiran's loaders use it with success.
     * 
     * @see gene.GeneService#createGene(Gene)
     */
    protected Gene handleSaveGene( Gene gene ) {
        return this.getGeneDao().create( gene );
    }

    @Override
    public Collection<? extends Gene> findByEnsemblId( String exactString ) {
        return this.getGeneDao().findByEnsemblId( exactString );
    }

    @Override
    public Collection<Gene> loadKnownGenesWithProducts( Taxon taxon ) {
        return this.getGeneDao().loadKnownGenesWithProducts( taxon );
    }

    @Override
    public Collection<GeneProductValueObject> getProducts( Long geneId ) {
        if ( geneId == null ) throw new IllegalArgumentException( "Null id for gene" );
        Gene gene = load( geneId );

        if ( gene == null ) throw new IllegalArgumentException( "No gene with id " + geneId );

        Collection<GeneProductValueObject> result = new ArrayList<GeneProductValueObject>();
        for ( GeneProduct gp : gene.getProducts() ) {
            result.add( new GeneProductValueObject( gp ) );
        }

        return result;
    }

    @Override
    public GeneValueObject loadGenePhenotypes( Long geneId ) {
        Gene gene = load( geneId );

        Collection<Long> ids = new HashSet<Long>();
        ids.add( gene.getId() );
        Collection<GeneValueObject> initialResults = loadValueObjects( ids );

        if ( initialResults.size() == 0 ) {
            return null;
        }

        GeneValueObject initialResult = initialResults.iterator().next();
        GeneValueObject details = new GeneValueObject( initialResult );

        Collection<GeneAlias> aliasObjs = gene.getAliases();
        Collection<String> aliasStrs = new ArrayList<String>();
        for ( GeneAlias ga : aliasObjs ) {
            aliasStrs.add( ga.getAlias() );
        }
        details.setAliases( aliasStrs );

        Long compositeSequenceCount = getCompositeSequenceCountById( geneId );
        details.setCompositeSequenceCount( compositeSequenceCount.intValue() );

        Collection<GeneSet> genesets = geneSetSearch.findByGene( gene );
        Collection<GeneSetValueObject> gsvos = new ArrayList<GeneSetValueObject>();
        gsvos.addAll( geneSetValueObjectHelper.convertToValueObjects( genesets, false ) );
        details.setGeneSets( gsvos );

        Collection<Gene> geneHomologues = homologeneService.getHomologues( gene );
        Collection<GeneValueObject> homologues = GeneValueObject.convert2ValueObjects( geneHomologues );
        details.setHomologues( homologues );

        return details;
    }

    @Override
    public Collection<AnnotationValueObject> findGOTerms( Long geneId ) {
        if ( geneId == null ) throw new IllegalArgumentException( "Null id for gene" );
        Collection<AnnotationValueObject> ontos = new HashSet<AnnotationValueObject>();
        Gene g = load( geneId );

        if ( g == null ) {
            throw new IllegalArgumentException( "No such gene could be loaded with id=" + geneId );
        }

        Collection<Gene2GOAssociation> associations = gene2GOAssociationService.findAssociationByGene( g );

        for ( Gene2GOAssociation assoc : associations ) {

            if ( assoc.getOntologyEntry() == null ) continue;

            AnnotationValueObject annot = new AnnotationValueObject();

            annot.setId( assoc.getOntologyEntry().getId() );
            annot.setTermName( geneOntologyService.getTermName( assoc.getOntologyEntry().getValue() ) );
            annot.setTermUri( assoc.getOntologyEntry().getValue() );
            annot.setEvidenceCode( assoc.getEvidenceCode().getValue() );
            annot.setDescription( assoc.getOntologyEntry().getDescription() );
            annot.setClassUri( assoc.getOntologyEntry().getCategoryUri() );
            annot.setClassName( assoc.getOntologyEntry().getCategory() );

            ontos.add( annot );
        }
        return annotationAssociationService.removeRootTerms( ontos );
    }

    @Override
    public GeneValueObject findByNCBIIdValueObject( Integer accession ) {
        Gene gene = findByNCBIId( accession );
        return new GeneValueObject( gene );
    }

    /**
     * @see GeneService#countAll()
     */
    @Override
    public Integer countAll() {
        return this.getGeneDao().countAll();
    }

    /**
     * @see GeneService#create(Collection)
     */
    @Override
    public Collection<Gene> create( final Collection<Gene> genes ) {
        return ( Collection<Gene> ) this.getGeneDao().create( genes );
    }

    /**
     * @see GeneService#create(Gene)
     */
    @Override
    public Gene create( final Gene gene ) {
        return this.getGeneDao().create( gene );
    }

    /**
     * @see GeneService#find(Gene)
     */
    @Override
    public Gene find( final Gene gene ) {
        return this.getGeneDao().find( gene );
    }

    /**
     * @see GeneService#findByAccession(String, ubic.gemma.model.common.description.ExternalDatabase)
     */
    @Override
    public Gene findByAccession( final String accession, final ExternalDatabase source ) {
        return this.getGeneDao().findByAccession( accession, source );
    }

    /**
     * @see GeneService#findByAlias(String)
     */
    @Override
    public Collection<Gene> findByAlias( final String search ) {
        return this.getGeneDao().findByAlias( search );
    }

    /**
     * @see GeneService#findByNCBIId(String)
     */
    @Override
    public Gene findByNCBIId( Integer accession ) {
        return this.getGeneDao().findByNcbiId( accession );
    }

    /**
     * @see GeneService#findByOfficialName(String)
     */
    @Override
    public Collection<Gene> findByOfficialName( final String officialName ) {
        return this.getGeneDao().findByOfficialName( officialName );
    }

    /**
     * @see GeneService#findByOfficialSymbol(String)
     */
    @Override
    public Collection<Gene> findByOfficialSymbol( final String officialSymbol ) {
        return this.getGeneDao().findByOfficalSymbol( officialSymbol );
    }

    /**
     * @see GeneService#findByOfficialSymbol(String, Taxon)
     */
    @Override
    public Gene findByOfficialSymbol( final String symbol, final Taxon taxon ) {
        return this.getGeneDao().findByOfficialSymbol( symbol, taxon );
    }

    /**
     * @see GeneService#findByOfficialSymbolInexact(String)
     */
    @Override
    public Collection<Gene> findByOfficialSymbolInexact( final String officialSymbol ) {
        return this.getGeneDao().findByOfficialSymbolInexact( officialSymbol );
    }

    /**
     * @see GeneService#findOrCreate(Gene)
     */
    @Override
    public Gene findOrCreate( final Gene gene ) {
        return this.getGeneDao().findOrCreate( gene );
    }

    /**
     * @see GeneService#getCoexpressedGenes(Gene, Collection, Integer, boolean)
     */
    @Override
    public Map<Gene, CoexpressionCollectionValueObject> getCoexpressedGenes( final Collection<Gene> genes,
            final Collection<? extends BioAssaySet> ees, final Integer stringency, final boolean interGenesOnly ) {
        return this.getGeneDao().getCoexpressedGenes( genes, ees, stringency, interGenesOnly );
    }

    /**
     * @see GeneService#getCoexpressedGenes(Gene, Collection, Integer, boolean)
     */
    @Override
    public CoexpressionCollectionValueObject getCoexpressedGenes( final Gene gene,
            final Collection<? extends BioAssaySet> ees, final Integer stringency ) {
        return this.getGeneDao().getCoexpressedGenes( gene, ees, stringency );
    }

    /**
     * @see GeneService#getCompositeSequenceCountById(Long)
     */
    @Override
    public long getCompositeSequenceCountById( final Long id ) {
        return this.getGeneDao().getCompositeSequenceCountById( id );
    }

    /**
     * @see GeneService#getCompositeSequences(Gene, ArrayDesign)
     */
    @Override
    public Collection<CompositeSequence> getCompositeSequences( final Gene gene, final ArrayDesign arrayDesign ) {
        return this.getGeneDao().getCompositeSequences( gene, arrayDesign );
    }

    /**
     * @see GeneService#getCompositeSequencesById(Long)
     */
    @Override
    public Collection<CompositeSequence> getCompositeSequencesById( final Long id ) {
        return this.getGeneDao().getCompositeSequencesById( id );
    }

    /**
     * @see GeneService#getGenesByTaxon(Taxon)
     */
    @Override
    public Collection<Gene> getGenesByTaxon( final Taxon taxon ) {
        return this.getGeneDao().getGenesByTaxon( taxon );
    }

    /**
     * @see GeneService#loadMicroRNAs(Taxon)
     */
    @Override
    public Collection<Gene> loadMicroRNAs( final Taxon taxon ) {
        return this.getGeneDao().getMicroRnaByTaxon( taxon );
    }

    /**
     * @see GeneService#load(long)
     */
    @Override
    public Gene load( final long id ) {
        return this.getGeneDao().load( id );
    }

    /**
     * @see GeneService#loadAll()
     */
    @Override
    public Collection<Gene> loadAll() {
        return ( Collection<Gene> ) this.getGeneDao().loadAll();
    }

    /**
     * @see GeneService#loadKnownGenes(Taxon)
     */
    @Override
    public Collection<Gene> loadKnownGenes( final Taxon taxon ) {
        return this.getGeneDao().loadKnownGenes( taxon );
    }

    /**
     * @see GeneService#loadMultiple(Collection)
     */
    @Override
    public Collection<Gene> loadMultiple( final Collection<Long> ids ) {
        return ( Collection<Gene> ) this.getGeneDao().load( ids );
    }

    /**
     * @see GeneService#remove(String)
     */
    @Override
    public void remove( Gene gene ) {
        this.getGeneDao().remove( gene );
    }

    /**
     * @see GeneService#remove(Collection)
     */
    @Override
    public void remove( final Collection<Gene> genes ) {
        this.getGeneDao().remove( genes );
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
        return this.getGeneDao().thaw( gene );

    }

    /**
     * @see GeneService#thawLite(Collection)
     */
    @Override
    public Collection<Gene> thawLite( final Collection<Gene> genes ) {
        return this.handleThawLite( genes );
    }

    /**
     * Only thaw the Aliases, very light version
     * 
     * @param gene
     */
    @Override
    public Gene thawAliases( Gene gene ) {
        return this.getGeneDao().thawAliases( gene );
    }

    /**
     * @see GeneService#update(Gene)
     */
    @Override
    public void update( final Gene gene ) {
        this.getGeneDao().update( gene );

    }

    /**
     * Gets the reference to <code>gene</code>'s DAO.
     */
    protected GeneDao getGeneDao() {
        return this.geneDao;
    }

}