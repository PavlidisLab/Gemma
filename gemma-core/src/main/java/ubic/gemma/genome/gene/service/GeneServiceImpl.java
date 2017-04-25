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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.analysis.service.GeneMultifunctionalityPopulationService;
import ubic.gemma.apps.MultifunctionalityCli;
import ubic.gemma.genome.gene.GeneSetValueObjectHelper;
import ubic.gemma.loader.genome.gene.ncbi.homology.HomologeneService;
import ubic.gemma.model.association.Gene2GOAssociation;
import ubic.gemma.model.association.Gene2GOAssociationService;
import ubic.gemma.model.association.coexpression.CoexpressionService;
import ubic.gemma.model.association.coexpression.GeneCoexpressionNodeDegreeValueObject;
import ubic.gemma.model.association.phenotype.PhenotypeAssociation;
import ubic.gemma.model.common.description.AnnotationValueObject;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.common.search.SearchSettingsImpl;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.*;
import ubic.gemma.model.genome.gene.*;
import ubic.gemma.model.genome.gene.phenotype.valueObject.CharacteristicValueObject;
import ubic.gemma.model.genome.sequenceAnalysis.AnnotationAssociationService;
import ubic.gemma.ontology.providers.GeneOntologyService;
import ubic.gemma.search.GeneSetSearch;
import ubic.gemma.search.SearchResult;
import ubic.gemma.search.SearchService;

import java.util.*;
import java.util.Map.Entry;

/**
 * @author pavlidis
 * @author keshav
 * @see GeneService
 */
@Service
public class GeneServiceImpl implements GeneService {

    private static final Log log = LogFactory.getLog( GeneServiceImpl.class.getName() );

    @Autowired
    private AnnotationAssociationService annotationAssociationService;

    @Autowired
    private CoexpressionService coexpressionService;

    @Autowired
    private Gene2GOAssociationService gene2GOAssociationService;

    @Autowired
    private GeneDao geneDao;

    @Autowired
    private GeneOntologyService geneOntologyService;

    @Autowired
    private GeneSetSearch geneSetSearch;

    @Autowired
    private GeneSetValueObjectHelper geneSetValueObjectHelper;

    @Autowired
    private HomologeneService homologeneService;

    @Autowired
    private SearchService searchService;

    /**
     * @see GeneService#countAll()
     */
    @Override
    @Transactional(readOnly = true)
    public Integer countAll() {
        return this.getGeneDao().countAll();
    }

    /**
     * @see GeneService#create(Collection)
     */
    @SuppressWarnings("unchecked")
    @Override
    @Transactional
    public Collection<Gene> create( final Collection<Gene> genes ) {
        return ( Collection<Gene> ) this.getGeneDao().create( genes );
    }

    /**
     * @see GeneService#create(Gene)
     */
    @Override
    @Transactional
    public Gene create( final Gene gene ) {
        return this.getGeneDao().create( gene );
    }

    /**
     * @see GeneService#find(Gene)
     */
    @Override
    @Transactional(readOnly = true)
    public Gene find( final Gene gene ) {
        return this.getGeneDao().find( gene );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<Gene> find( PhysicalLocation physicalLocation ) {
        return this.getGeneDao().find( physicalLocation );
    }

    /**
     * @see GeneService#findByAccession(String, ubic.gemma.model.common.description.ExternalDatabase)
     */
    @Override
    @Transactional(readOnly = true)
    public Gene findByAccession( final String accession, final ExternalDatabase source ) {
        return this.getGeneDao().findByAccession( accession, source );
    }

    /**
     * @see GeneService#findByAlias(String)
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<Gene> findByAlias( final String search ) {
        return this.getGeneDao().findByAlias( search );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<? extends Gene> findByEnsemblId( String exactString ) {
        return this.getGeneDao().findByEnsemblId( exactString );
    }

    /**
     * @see GeneService#findByNCBIId(Integer)
     */
    @Override
    public Gene findByNCBIId( Integer accession ) {
        return this.getGeneDao().findByNcbiId( accession );
    }

    @Override
    public GeneValueObject findByNCBIIdValueObject( Integer accession ) {
        Gene gene = findByNCBIId( accession );
        return new GeneValueObject( gene );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Integer, GeneValueObject> findByNcbiIds( Collection<Integer> ncbiIds ) {
        Map<Integer, GeneValueObject> result = new HashMap<>();
        Map<Integer, Gene> genes = this.getGeneDao().findByNcbiIds( ncbiIds );
        for ( Entry<Integer, Gene> entry : genes.entrySet() ) {
            result.put( entry.getKey(), new GeneValueObject( entry.getValue() ) );
        }
        return result;
    }

    /**
     * @see GeneService#findByOfficialName(String)
     */
    @Override
    public Collection<Gene> findByOfficialName( final String officialName ) {
        return this.getGeneDao().findByOfficialName( officialName );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<Gene> findByOfficialNameInexact( String officialName ) {
        return this.getGeneDao().findByOfficialNameInexact( officialName );
    }

    /**
     * @see GeneService#findByOfficialSymbol(String)
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<Gene> findByOfficialSymbol( final String officialSymbol ) {
        return this.getGeneDao().findByOfficalSymbol( officialSymbol );
    }

    /**
     * @see GeneService#findByOfficialSymbol(String, Taxon)
     */
    @Override
    @Transactional(readOnly = true)
    public Gene findByOfficialSymbol( final String symbol, final Taxon taxon ) {
        return this.getGeneDao().findByOfficialSymbol( symbol, taxon );
    }

    /**
     * @see GeneService#findByOfficialSymbolInexact(String)
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<Gene> findByOfficialSymbolInexact( final String officialSymbol ) {
        return this.getGeneDao().findByOfficialSymbolInexact( officialSymbol );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, GeneValueObject> findByOfficialSymbols( Collection<String> query, Long taxonId ) {
        Map<String, GeneValueObject> result = new HashMap<>();
        Map<String, Gene> genes = this.getGeneDao().findByOfficialSymbols( query, taxonId );
        for ( String q : genes.keySet() ) {
            result.put( q, new GeneValueObject( genes.get( q ) ) );
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<AnnotationValueObject> findGOTerms( Long geneId ) {
        if ( geneId == null )
            throw new IllegalArgumentException( "Null id for gene" );
        Collection<AnnotationValueObject> ontos = new HashSet<>();
        Gene g = load( geneId );

        if ( g == null ) {
            throw new IllegalArgumentException( "No such gene could be loaded with id=" + geneId );
        }

        Collection<Gene2GOAssociation> associations = gene2GOAssociationService.findAssociationByGene( g );

        for ( Gene2GOAssociation assoc : associations ) {

            if ( assoc.getOntologyEntry() == null )
                continue;

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
    @Transactional(readOnly = true)
    public RelativeLocationData findNearest( PhysicalLocation physicalLocation, boolean useStrand ) {
        return this.getGeneDao().findNearest( physicalLocation, useStrand );
    }

    /**
     * @see GeneService#getCompositeSequenceCountById(Long)
     */
    @Override
    @Transactional(readOnly = true)
    public long getCompositeSequenceCountById( final Long id ) {
        return this.getGeneDao().getCompositeSequenceCountById( id );
    }

    /**
     * @see GeneService#getCompositeSequences(Gene, ArrayDesign)
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<CompositeSequence> getCompositeSequences( final Gene gene, final ArrayDesign arrayDesign ) {
        return this.getGeneDao().getCompositeSequences( gene, arrayDesign );
    }

    /**
     * @see GeneService#getCompositeSequencesById(Long)
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<CompositeSequence> getCompositeSequencesById( final Long id ) {
        return this.getGeneDao().getCompositeSequencesById( id );
    }

    /**
     * @see GeneService#getGenesByTaxon(Taxon)
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<Gene> getGenesByTaxon( final Taxon taxon ) {
        return this.getGeneDao().getGenesByTaxon( taxon );
    }

    @Override
    @Transactional(readOnly = true)
    public PhysicalLocation getMaxPhysicalLength( Gene gene ) {
        if ( gene == null )
            return null;

        Collection<GeneProduct> gpCollection = gene.getProducts();

        if ( gpCollection == null )
            return null;

        Long minStartNt = Long.MAX_VALUE;
        Long maxEndNt = Long.MIN_VALUE;
        String strand = null;
        Chromosome chromosome = null;

        for ( GeneProduct gp : gpCollection ) {

            PhysicalLocation pLoc = gp.getPhysicalLocation();
            if ( pLoc == null ) {
                log.warn(
                        "No physical location for Gene: " + gene.getOfficialSymbol() + "'s Gene Product: " + gp.getId()
                                + ". Skipping." );
                continue;
            }

            String currentStrand = pLoc.getStrand();
            Chromosome currentChromosome = pLoc.getChromosome();
            Long currentStartNt = pLoc.getNucleotide();
            Long currentEndNt = currentStartNt + pLoc.getNucleotideLength();

            // 1st time through loop
            if ( minStartNt == Long.MAX_VALUE ) {
                minStartNt = currentStartNt;
                maxEndNt = currentEndNt;
                strand = currentStrand;
                chromosome = currentChromosome;
                continue;
            }

            // FIXME: This is defensive coding. Not sure if this will ever happen. If it does, will need to sort the
            // gene products in advance to remove the outliers. Currently this method is assuming the 1st gene product
            // is not the outlier.
            if ( !currentStrand.equalsIgnoreCase( strand ) ) {
                log.warn( "Gene products for " + gene.getOfficialSymbol() + " , Id=" + gene.getId()
                        + " are on different strands. Unable to compute distance when products are on different strands. Skipping Gene product: "
                        + gp.getId() );
                continue;
            }

            if ( !currentChromosome.equals( chromosome ) ) {
                log.warn( "Gene products for " + gene.getOfficialSymbol() + " , Id=" + gene.getId()
                        + " are on different chromosomes. Unable to compute distance when gene products are on different chromosomes. Skipping Gene product: "
                        + gp.getId() );

                continue;
            }

            if ( currentStartNt < minStartNt )
                minStartNt = currentStartNt;

            if ( currentEndNt > maxEndNt )
                maxEndNt = currentEndNt;

        } // for each gene product

        Long length = maxEndNt - minStartNt;
        PhysicalLocation result = PhysicalLocation.Factory.newInstance();
        result.setChromosome( chromosome );
        result.setNucleotide( minStartNt );
        result.setNucleotideLength( length.intValue() );
        result.setStrand( strand ); // bin is null.
        return result;

    }

    @Override
    @Transactional(readOnly = true)
    public Collection<GeneProductValueObject> getProducts( Long geneId ) {
        if ( geneId == null )
            throw new IllegalArgumentException( "Null id for gene" );
        Gene gene = load( geneId );

        if ( gene == null )
            throw new IllegalArgumentException( "No gene with id " + geneId );

        Collection<GeneProductValueObject> result = new ArrayList<>();
        for ( GeneProduct gp : gene.getProducts() ) {
            result.add( new GeneProductValueObject( gp ) );
        }

        return result;
    }

    /**
     * @see GeneService#load(Long)
     */
    @Override
    @Transactional(readOnly = true)
    public Gene load( final Long id ) {
        return this.getGeneDao().load( id );
    }

    /**
     * @see GeneService#loadAll()
     */
    @SuppressWarnings("unchecked")
    @Override
    @Transactional(readOnly = true)
    public Collection<Gene> loadAll() {
        return ( Collection<Gene> ) this.getGeneDao().loadAll();
    }

    /**
     * @see GeneService#loadAll(Taxon)
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<Gene> loadAll( final Taxon taxon ) {
        return this.getGeneDao().loadKnownGenes( taxon );
    }

    @Override
    @Transactional(readOnly = true)
    public GeneValueObject loadFullyPopulatedValueObject( Long id ) {
        Gene gene = this.getGeneDao().load( id );
        if ( gene == null ) {
            return null;
        }
        gene = this.getGeneDao().thaw( gene );

        GeneValueObject gvo = GeneValueObject.convert2ValueObject( gene );

        Collection<GeneAlias> aliasObjs = gene.getAliases();
        Collection<String> aliasStrs = new ArrayList<>();
        for ( GeneAlias ga : aliasObjs ) {
            aliasStrs.add( ga.getAlias() );
        }
        gvo.setAliases( aliasStrs );

        if ( gene.getMultifunctionality() != null ) {
            gvo.setMultifunctionalityRank( gene.getMultifunctionality().getRank() );
        }

        Long compositeSequenceCount = getCompositeSequenceCountById( id );
        gvo.setCompositeSequenceCount( compositeSequenceCount.intValue() );

        Integer platformCount = this.geneDao.getPlatformCountById( id );
        gvo.setPlatformCount( platformCount );

        Collection<GeneSet> genesets = this.geneSetSearch.findByGene( gene );
        Collection<GeneSetValueObject> gsvos = new ArrayList<>();
        gsvos.addAll( geneSetValueObjectHelper.convertToLightValueObjects( genesets, false ) );

        gvo.setGeneSets( gsvos );

        Collection<Gene> geneHomologues = this.homologeneService.getHomologues( gene );
        geneHomologues = this.thawLite( geneHomologues );
        Collection<GeneValueObject> homologues = GeneValueObject.convert2ValueObjects( geneHomologues );

        gvo.setHomologues( homologues );

        Collection<PhenotypeAssociation> phenoAssocs = gene.getPhenotypeAssociations();
        Collection<CharacteristicValueObject> cvos = new HashSet<>();
        for ( PhenotypeAssociation pa : phenoAssocs ) {
            cvos.addAll( CharacteristicValueObject.characteristic2CharacteristicVO( pa.getPhenotypes() ) );
        }

        gvo.setPhenotypes( cvos );

        if ( gvo.getNcbiId() != null ) {
            SearchSettingsImpl s = new SearchSettingsImpl();
            s.setTermUri( "http://purl.org/commons/record/ncbi_gene/" + gvo.getNcbiId() );
            s.noSearches();
            s.setSearchExperiments( true );
            Map<Class<?>, List<SearchResult>> r = searchService.search( s );
            if ( r.containsKey( ExpressionExperiment.class ) ) {
                List<SearchResult> hits = r.get( ExpressionExperiment.class );
                gvo.setAssociatedExperimentCount( hits.size() );
            }
        }

        GeneCoexpressionNodeDegreeValueObject nodeDegree = coexpressionService.getNodeDegree( gene );

        if ( nodeDegree != null ) {
            gvo.setNodeDegreesPos( nodeDegree.asIntArrayPos() );

            gvo.setNodeDegreesNeg( nodeDegree.asIntArrayNeg() );

            gvo.setNodeDegreePosRanks( nodeDegree.asDoubleArrayPosRanks() );

            gvo.setNodeDegreeNegRanks( nodeDegree.asDoubleArrayNegRanks() );
        }

        return gvo;
    }

    @Override
    @Transactional(readOnly = true)
    public GeneValueObject loadGenePhenotypes( Long geneId ) {
        Gene gene = load( geneId );
        gene = thaw( gene );
        GeneValueObject initialResult = GeneValueObject.convert2ValueObject( gene );
        GeneValueObject details = new GeneValueObject( initialResult );

        Collection<GeneAlias> aliasObjs = gene.getAliases();
        Collection<String> aliasStrs = new ArrayList<>();
        for ( GeneAlias ga : aliasObjs ) {
            aliasStrs.add( ga.getAlias() );
        }
        details.setAliases( aliasStrs );

        Long compositeSequenceCount = getCompositeSequenceCountById( geneId );
        details.setCompositeSequenceCount( compositeSequenceCount.intValue() );

        Collection<GeneSet> genesets = geneSetSearch.findByGene( gene );
        Collection<GeneSetValueObject> gsvos = new ArrayList<>();
        gsvos.addAll( geneSetValueObjectHelper.convertToValueObjects( genesets, false ) );
        details.setGeneSets( gsvos );

        Collection<Gene> geneHomologues = homologeneService.getHomologues( gene );
        geneHomologues = this.thawLite( geneHomologues );
        Collection<GeneValueObject> homologues = GeneValueObject.convert2ValueObjects( geneHomologues );
        details.setHomologues( homologues );

        return details;
    }

    /**
     * @see GeneService#loadMicroRNAs(Taxon)
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<Gene> loadMicroRNAs( final Taxon taxon ) {
        return this.getGeneDao().getMicroRnaByTaxon( taxon );
    }

    /**
     * @see GeneService#loadMultiple(Collection)
     */
    @SuppressWarnings("unchecked")
    @Override
    @Transactional(readOnly = true)
    public Collection<Gene> loadMultiple( final Collection<Long> ids ) {
        return ( Collection<Gene> ) this.getGeneDao().load( ids );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<Gene> loadThawed( Collection<Long> ids ) {
        return this.getGeneDao().loadThawed( ids );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<Gene> loadThawedLiter( Collection<Long> ids ) {// maybe this isn't needed, just use value objects
        // method
        return this.getGeneDao().loadThawedLiter( ids );
    }

    @Override
    @Transactional(readOnly = true)
    public GeneValueObject loadValueObject( Long id ) {
        Gene g = this.getGeneDao().load( id );
        if ( g == null)
            return null;
        g = this.getGeneDao().thaw( g );
        return GeneValueObject.convert2ValueObject( g );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<GeneValueObject> loadValueObjects( Collection<Long> ids ) {
        Collection<Gene> g = this.getGeneDao().loadThawed( ids );
        return GeneValueObject.convert2ValueObjects( g );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<GeneValueObject> loadValueObjectsLiter( Collection<Long> ids ) {
        Collection<Gene> g = this.getGeneDao().loadThawedLiter( ids );
        return GeneValueObject.convert2ValueObjects( g );
    }

    /**
     * @see GeneService#remove(Collection)
     */
    @Override
    @Transactional
    public void remove( final Collection<Gene> genes ) {
        this.getGeneDao().remove( genes );
    }

    /**
     * @see GeneService#remove(Gene)
     */
    @Override
    @Transactional
    public void remove( Gene gene ) {
        this.getGeneDao().remove( gene );
    }

    /**
     * @see GeneService#thaw(Gene)
     */
    @Override
    @Transactional(readOnly = true)
    public Gene thaw( final Gene gene ) {
        return this.getGeneDao().thaw( gene );

    }

    /**
     * Only thaw the Aliases, very light version
     */
    @Override
    @Transactional(readOnly = true)
    public Gene thawAliases( Gene gene ) {
        return this.getGeneDao().thawAliases( gene );
    }

    /**
     * @see GeneService#thawLite(Collection)
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<Gene> thawLite( final Collection<Gene> genes ) {
        return this.getGeneDao().thawLite( genes );
    }

    @Override
    @Transactional(readOnly = true)
    public Gene thawLite( Gene gene ) {
        return this.getGeneDao().thawLite( gene );
    }

    @Override
    @Transactional(readOnly = true)
    public Gene thawLiter( Gene gene ) {
        return this.getGeneDao().thawLiter( gene );
    }

    @Override
    @Transactional
    public void update( Collection<Gene> genes ) {
        this.getGeneDao().update( genes );
    }

    /**
     * @see GeneService#update(Gene)
     */
    @Override
    @Transactional
    public void update( final Gene gene ) {
        this.getGeneDao().update( gene );

    }

    /**
     * Gets the reference to <code>gene</code>'s DAO.
     */
    protected GeneDao getGeneDao() {
        return this.geneDao;
    }

    /**
     * Sets the reference to <code>gene</code>'s DAO.
     */
    public void setGeneDao( GeneDao geneDao ) {
        this.geneDao = geneDao;
    }

}