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

package ubic.gemma.core.genome.gene.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ubic.gemma.core.genome.gene.GeneSetValueObjectHelper;
import ubic.gemma.core.loader.genome.gene.ncbi.homology.HomologeneService;
import ubic.gemma.core.ontology.providers.GeneOntologyService;
import ubic.gemma.core.search.GeneSetSearch;
import ubic.gemma.core.search.SearchResult;
import ubic.gemma.core.search.SearchService;
import ubic.gemma.model.association.Gene2GOAssociation;
import ubic.gemma.model.association.coexpression.GeneCoexpressionNodeDegreeValueObject;
import ubic.gemma.model.association.phenotype.PhenotypeAssociation;
import ubic.gemma.model.common.description.AnnotationValueObject;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.common.search.SearchSettingsImpl;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Chromosome;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.PhysicalLocation;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneAlias;
import ubic.gemma.model.genome.gene.GeneProduct;
import ubic.gemma.model.genome.gene.GeneProductValueObject;
import ubic.gemma.model.genome.gene.GeneSet;
import ubic.gemma.model.genome.gene.GeneSetValueObject;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.CharacteristicValueObject;
import ubic.gemma.persistence.service.VoEnabledService;
import ubic.gemma.persistence.service.association.Gene2GOAssociationService;
import ubic.gemma.persistence.service.association.coexpression.CoexpressionService;
import ubic.gemma.persistence.service.genome.GeneDao;
import ubic.gemma.persistence.service.genome.RelativeLocationData;
import ubic.gemma.persistence.service.genome.sequenceAnalysis.AnnotationAssociationService;

/**
 * @author pavlidis
 * @author keshav
 * @see GeneService
 */
@Service
public class GeneServiceImpl extends VoEnabledService<Gene, GeneValueObject> implements GeneService {

    private final GeneDao geneDao;
    private AnnotationAssociationService annotationAssociationService;
    private CoexpressionService coexpressionService;
    private Gene2GOAssociationService gene2GOAssociationService;
    private GeneOntologyService geneOntologyService;
    private GeneSetSearch geneSetSearch;
    private GeneSetValueObjectHelper geneSetValueObjectHelper;
    private HomologeneService homologeneService;
    private SearchService searchService;

    @Autowired
    public GeneServiceImpl( GeneDao geneDao ) {
        super( geneDao );
        this.geneDao = geneDao;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<Gene> find( PhysicalLocation physicalLocation ) {
        return this.geneDao.find( physicalLocation );
    }

    /**
     * @see GeneService#findByAccession(String, ubic.gemma.model.common.description.ExternalDatabase)
     */
    @Override
    @Transactional(readOnly = true)
    public Gene findByAccession( final String accession, final ExternalDatabase source ) {
        return this.geneDao.findByAccession( accession, source );
    }

    /**
     * @see GeneService#findByAlias(String)
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<Gene> findByAlias( final String search ) {
        return this.geneDao.findByAlias( search );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<? extends Gene> findByEnsemblId( String exactString ) {
        return this.geneDao.findByEnsemblId( exactString );
    }

    /**
     * @see GeneService#findByNCBIId(Integer)
     */
    @Override
    @Transactional(readOnly = true)
    public Gene findByNCBIId( Integer accession ) {
        return this.geneDao.findByNcbiId( accession );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Integer, GeneValueObject> findByNcbiIds( Collection<Integer> ncbiIds ) {
        Map<Integer, GeneValueObject> result = new HashMap<>();
        Map<Integer, Gene> genes = this.geneDao.findByNcbiIds( ncbiIds );
        for ( Entry<Integer, Gene> entry : genes.entrySet() ) {
            result.put( entry.getKey(), new GeneValueObject( entry.getValue() ) );
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public GeneValueObject findByNCBIIdValueObject( Integer accession ) {
        Gene gene = findByNCBIId( accession );
        return new GeneValueObject( gene );
    }

    /**
     * @see GeneService#findByOfficialName(String)
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<Gene> findByOfficialName( final String officialName ) {
        return this.geneDao.findByOfficialName( officialName );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<Gene> findByOfficialNameInexact( String officialName ) {
        return this.geneDao.findByOfficialNameInexact( officialName );
    }

    /**
     * @see GeneService#findByOfficialSymbol(String)
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<Gene> findByOfficialSymbol( final String officialSymbol ) {
        return this.geneDao.findByOfficialSymbol( officialSymbol );
    }

    /**
     * @see GeneService#findByOfficialSymbol(String, Taxon)
     */
    @Override
    @Transactional(readOnly = true)
    public Gene findByOfficialSymbol( final String symbol, final Taxon taxon ) {
        return this.geneDao.findByOfficialSymbol( symbol, taxon );
    }

    /**
     * @see GeneService#findByOfficialSymbolInexact(String)
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<Gene> findByOfficialSymbolInexact( final String officialSymbol ) {
        return this.geneDao.findByOfficialSymbolInexact( officialSymbol );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, GeneValueObject> findByOfficialSymbols( Collection<String> query, Long taxonId ) {
        Map<String, GeneValueObject> result = new HashMap<>();
        Map<String, Gene> genes = this.geneDao.findByOfficialSymbols( query, taxonId );
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
        Collection<AnnotationValueObject> ontologies = new HashSet<>();
        Gene g = load( geneId );

        if ( g == null ) {
            throw new IllegalArgumentException( "No such gene could be loaded with id=" + geneId );
        }

        Collection<Gene2GOAssociation> associations = gene2GOAssociationService.findAssociationByGene( g );

        for ( Gene2GOAssociation assoc : associations ) {

            if ( assoc.getOntologyEntry() == null )
                continue;

            AnnotationValueObject annotationValueObject = new AnnotationValueObject();

            annotationValueObject.setId( assoc.getOntologyEntry().getId() );
            annotationValueObject.setTermName( geneOntologyService.getTermName( assoc.getOntologyEntry().getValue() ) );
            annotationValueObject.setTermUri( assoc.getOntologyEntry().getValue() );
            annotationValueObject.setEvidenceCode( assoc.getEvidenceCode().getValue() );
            annotationValueObject.setDescription( assoc.getOntologyEntry().getDescription() );
            annotationValueObject.setClassUri( assoc.getOntologyEntry().getCategoryUri() );
            annotationValueObject.setClassName( assoc.getOntologyEntry().getCategory() );

            ontologies.add( annotationValueObject );
        }
        return annotationAssociationService.removeRootTerms( ontologies );
    }

    @Override
    @Transactional(readOnly = true)
    public RelativeLocationData findNearest( PhysicalLocation physicalLocation, boolean useStrand ) {
        return this.geneDao.findNearest( physicalLocation, useStrand );
    }

    /**
     * @see GeneService#getCompositeSequenceCountById(Long)
     */
    @Override
    @Transactional(readOnly = true)
    public long getCompositeSequenceCountById( final Long id ) {
        return this.geneDao.getCompositeSequenceCountById( id );
    }

    /**
     * @see GeneService#getCompositeSequences(Gene, ArrayDesign)
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<CompositeSequence> getCompositeSequences( final Gene gene, final ArrayDesign arrayDesign ) {
        return this.geneDao.getCompositeSequences( gene, arrayDesign );
    }

    /**
     * @see GeneService#getCompositeSequencesById(Long)
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<CompositeSequence> getCompositeSequencesById( final Long id ) {
        return this.geneDao.getCompositeSequencesById( id );
    }

    /**
     * @see GeneService#getGenesByTaxon(Taxon)
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<Gene> getGenesByTaxon( final Taxon taxon ) {
        return this.geneDao.getGenesByTaxon( taxon );
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
     * @see GeneService#loadAll(Taxon)
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<Gene> loadAll( final Taxon taxon ) {
        return this.geneDao.loadKnownGenes( taxon );
    }

    @Override
    @Transactional(readOnly = true)
    public GeneValueObject loadFullyPopulatedValueObject( Long id ) {
        Gene gene = this.geneDao.load( id );
        if ( gene == null ) {
            return null;
        }
        gene = this.geneDao.thaw( gene );

        GeneValueObject gvo = GeneValueObject.convert2ValueObject( gene );

        Collection<GeneAlias> aliasObjects = gene.getAliases();
        Collection<String> aliasStrings = new ArrayList<>();
        for ( GeneAlias ga : aliasObjects ) {
            aliasStrings.add( ga.getAlias() );
        }
        gvo.setAliases( aliasStrings );

        if ( gene.getMultifunctionality() != null ) {
            gvo.setMultifunctionalityRank( gene.getMultifunctionality().getRank() );
        }

        Long compositeSequenceCount = getCompositeSequenceCountById( id );
        gvo.setCompositeSequenceCount( compositeSequenceCount.intValue() );

        Integer platformCount = this.geneDao.getPlatformCountById( id );
        gvo.setPlatformCount( platformCount );

        Collection<GeneSet> geneSets = this.geneSetSearch.findByGene( gene );
        Collection<GeneSetValueObject> gsVos = new ArrayList<>();
        gsVos.addAll( geneSetValueObjectHelper.convertToLightValueObjects( geneSets, false ) );

        gvo.setGeneSets( gsVos );

        Collection<Gene> geneHomologues = this.homologeneService.getHomologues( gene );
        geneHomologues = this.thawLite( geneHomologues );
        Collection<GeneValueObject> homologues = this.loadValueObjects( geneHomologues );

        gvo.setHomologues( homologues );

        Collection<PhenotypeAssociation> pas = gene.getPhenotypeAssociations();
        Collection<CharacteristicValueObject> cVos = new HashSet<>();
        for ( PhenotypeAssociation pa : pas ) {
            cVos.addAll( CharacteristicValueObject.characteristic2CharacteristicVO( pa.getPhenotypes() ) );
        }

        gvo.setPhenotypes( cVos );

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

        Collection<GeneAlias> aliasObjects = gene.getAliases();
        Collection<String> aliasStrings = new ArrayList<>();
        for ( GeneAlias ga : aliasObjects ) {
            aliasStrings.add( ga.getAlias() );
        }
        details.setAliases( aliasStrings );

        Long compositeSequenceCount = getCompositeSequenceCountById( geneId );
        details.setCompositeSequenceCount( compositeSequenceCount.intValue() );

        Collection<GeneSet> geneSets = geneSetSearch.findByGene( gene );
        Collection<GeneSetValueObject> gsVos = new ArrayList<>();

        gsVos.addAll( geneSetValueObjectHelper.convertToValueObjects( geneSets, false ) );
        details.setGeneSets( gsVos );

        Collection<Gene> geneHomologues = homologeneService.getHomologues( gene );
        geneHomologues = this.thawLite( geneHomologues );
        Collection<GeneValueObject> homologues = this.loadValueObjects( geneHomologues );
        details.setHomologues( homologues );

        return details;
    }

    /**
     * @see GeneService#loadMicroRNAs(Taxon)
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<Gene> loadMicroRNAs( final Taxon taxon ) {
        return this.geneDao.getMicroRnaByTaxon( taxon );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<Gene> loadThawed( Collection<Long> ids ) {
        return this.geneDao.loadThawed( ids );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<Gene> loadThawedLiter( Collection<Long> ids ) {
        return this.geneDao.loadThawedLiter( ids );
    }

    @Override
    @Transactional(readOnly = true)
    public GeneValueObject loadValueObjectById( Long id ) {
        Gene g = this.geneDao.load( id );
        if ( g == null )
            return null;
        g = this.geneDao.thaw( g );
        return GeneValueObject.convert2ValueObject( g );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<GeneValueObject> loadValueObjectsByIds( Collection<Long> ids ) {
        Collection<Gene> g = this.geneDao.loadThawed( ids );
        return this.loadValueObjects( g );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<GeneValueObject> loadValueObjectsByIdsLiter( Collection<Long> ids ) {
        Collection<Gene> g = this.geneDao.loadThawedLiter( ids );
        return this.loadValueObjects( g );
    }

    @Autowired
    public void setAnnotationAssociationService( AnnotationAssociationService annotationAssociationService ) {
        this.annotationAssociationService = annotationAssociationService;
    }

    @Autowired
    public void setCoexpressionService( CoexpressionService coexpressionService ) {
        this.coexpressionService = coexpressionService;
    }

    @Autowired
    public void setGene2GOAssociationService( Gene2GOAssociationService gene2GOAssociationService ) {
        this.gene2GOAssociationService = gene2GOAssociationService;
    }

    @Autowired
    public void setGeneOntologyService( GeneOntologyService geneOntologyService ) {
        this.geneOntologyService = geneOntologyService;
    }

    @Autowired
    public void setGeneSetSearch( GeneSetSearch geneSetSearch ) {
        this.geneSetSearch = geneSetSearch;
    }

    @Autowired
    public void setGeneSetValueObjectHelper( GeneSetValueObjectHelper geneSetValueObjectHelper ) {
        this.geneSetValueObjectHelper = geneSetValueObjectHelper;
    }

    @Autowired
    public void setHomologeneService( HomologeneService homologeneService ) {
        this.homologeneService = homologeneService;
    }

    @Autowired
    public void setSearchService( SearchService searchService ) {
        this.searchService = searchService;
    }

    @Override
    public Gene thaw( Gene gene ) {
        return this.geneDao.thaw( gene );
    }

    /**
     * Only thaw the Aliases, very light version
     */
    @Override
    @Transactional(readOnly = true)
    public Gene thawAliases( Gene gene ) {
        return this.geneDao.thawAliases( gene );
    }

    /**
     * @see GeneService#thawLite(Collection)
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<Gene> thawLite( final Collection<Gene> genes ) {
        return this.geneDao.thawLite( genes );
    }

    @Override
    @Transactional(readOnly = true)
    public Gene thawLite( Gene gene ) {
        return this.geneDao.thawLite( gene );
    }

    @Override
    @Transactional(readOnly = true)
    public Gene thawLiter( Gene gene ) {
        return this.geneDao.thawLiter( gene );
    }

}