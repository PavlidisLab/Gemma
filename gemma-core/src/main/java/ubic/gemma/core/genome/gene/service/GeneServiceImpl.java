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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.core.genome.gene.GeneSetValueObjectHelper;
import ubic.gemma.core.loader.genome.gene.ncbi.homology.HomologeneService;
import ubic.gemma.core.loader.genome.gene.ncbi.homology.HomologeneServiceFactory;
import ubic.gemma.core.ontology.providers.GeneOntologyService;
import ubic.gemma.core.search.GeneSetSearch;
import ubic.gemma.core.search.SearchException;
import ubic.gemma.core.search.SearchResult;
import ubic.gemma.core.search.SearchService;
import ubic.gemma.model.association.Gene2GOAssociation;
import ubic.gemma.model.association.coexpression.GeneCoexpressionNodeDegreeValueObject;
import ubic.gemma.model.common.description.AnnotationValueObject;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.common.search.SearchSettings;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.PhysicalLocation;
import ubic.gemma.model.genome.PhysicalLocationValueObject;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.*;
import ubic.gemma.model.common.description.CharacteristicValueObject;
import ubic.gemma.persistence.service.AbstractFilteringVoEnabledService;
import ubic.gemma.persistence.service.AbstractService;
import ubic.gemma.persistence.service.association.Gene2GOAssociationService;
import ubic.gemma.persistence.service.association.coexpression.CoexpressionService;
import ubic.gemma.persistence.service.genome.GeneDao;
import ubic.gemma.persistence.service.genome.sequenceAnalysis.AnnotationAssociationService;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;
import ubic.gemma.persistence.util.AsyncFactoryBeanUtils;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.Future;

/**
 * @author pavlidis
 * @author keshav
 * @see    GeneService
 */
@Service
@ParametersAreNonnullByDefault
public class GeneServiceImpl extends AbstractFilteringVoEnabledService<Gene, GeneValueObject> implements GeneService {

    private final GeneDao geneDao;

    @Autowired
    private AnnotationAssociationService annotationAssociationService;
    @Autowired
    private CoexpressionService coexpressionService;
    @Autowired
    private Gene2GOAssociationService gene2GOAssociationService;
    @Autowired
    private GeneOntologyService geneOntologyService;
    @Autowired
    private GeneSetSearch geneSetSearch;
    @Autowired
    private GeneSetValueObjectHelper geneSetValueObjectHelper;
    @Autowired
    private SearchService searchService;
    @Autowired
    private TaxonService taxonService;
    @Autowired
    private Future<HomologeneService> homologeneService;

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

    @Override
    @Transactional(readOnly = true)
    public Gene findByAccession( final String accession, @Nullable final ExternalDatabase source ) {
        return this.geneDao.findByAccession( accession, source );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<Gene> findByAlias( final String search ) {
        return this.geneDao.findByAlias( search );
    }

    @Override
    @Transactional(readOnly = true)
    public Gene findByEnsemblId( String exactString ) {
        return this.geneDao.findByEnsemblId( exactString );
    }

    @Override
    @Transactional(readOnly = true)
    public Gene findByNCBIId( Integer accession ) {
        return this.geneDao.findByNcbiId( accession );
    }

    @Override
    @Transactional(readOnly = true)
    public GeneValueObject findByNCBIIdValueObject( Integer accession ) {
        Gene gene = this.findByNCBIId( accession );
        return gene != null ? new GeneValueObject( gene ) : null;
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
    public Collection<Gene> findByOfficialName( final String officialName ) {
        return this.geneDao.findByOfficialName( officialName );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<Gene> findByOfficialNameInexact( String officialName ) {
        return this.geneDao.findByOfficialNameInexact( officialName );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<Gene> findByOfficialSymbol( final String officialSymbol ) {
        return this.geneDao.findByOfficialSymbol( officialSymbol );
    }

    @Override
    @Transactional(readOnly = true)
    public Gene findByOfficialSymbol( final String symbol, final Taxon taxon ) {
        return this.geneDao.findByOfficialSymbol( symbol, taxon );
    }

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
        Gene g = this.load( geneId );

        if ( g == null ) {
            throw new IllegalArgumentException( "No such gene could be loaded with id=" + geneId );
        }

        Collection<Gene2GOAssociation> associations = gene2GOAssociationService.findAssociationByGene( g );

        for ( Gene2GOAssociation assoc : associations ) {

            if ( assoc.getOntologyEntry() == null )
                continue;

            AnnotationValueObject annotationValueObject = new AnnotationValueObject( assoc.getOntologyEntry() );
            annotationValueObject.setTermName( geneOntologyService.getTermName( assoc.getOntologyEntry().getValue() ) );

            ontologies.add( annotationValueObject );
        }
        return annotationAssociationService.removeRootTerms( ontologies );
    }

    @Override
    @Transactional(readOnly = true)
    public long getCompositeSequenceCountById( final Long id ) {
        return this.geneDao.getCompositeSequenceCountById( id );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<CompositeSequence> getCompositeSequences( final Gene gene, final ArrayDesign arrayDesign ) {
        return this.geneDao.getCompositeSequences( gene, arrayDesign );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<CompositeSequence> getCompositeSequencesById( final Long id ) {
        return this.geneDao.getCompositeSequencesById( id );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<Gene> getGenesByTaxon( final Taxon taxon ) {
        return this.geneDao.getGenesByTaxon( taxon );
    }

    @Override
    @Transactional(readOnly = true)
    public List<PhysicalLocationValueObject> getPhysicalLocationsValueObjects( Gene gene ) {
        if ( gene == null ) {
            return Collections.emptyList();
        }

        gene = this.thaw( gene );

        Collection<GeneProduct> gpCollection = gene.getProducts();
        List<PhysicalLocationValueObject> locations = new LinkedList<>();

        if ( gpCollection == null )
            return null;

        for ( GeneProduct gp : gpCollection ) {

            PhysicalLocation physicalLocation = gp.getPhysicalLocation();

            if ( physicalLocation == null ) {
                if ( AbstractService.log.isDebugEnabled() )
                    AbstractService.log
                            .debug( gene.getOfficialSymbol() + " product " + gp.getName() + " (id:" + gp.getId()
                                    + ") has no location." );
                continue;
            }
            // Only add if the physical location of the product is different from any we already know.
            PhysicalLocationValueObject vo = new PhysicalLocationValueObject( physicalLocation );
            if ( !locations.contains( vo ) ) {
                locations.add( vo );
            }
        }

        return locations;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<GeneProductValueObject> getProducts( Long geneId ) {
        if ( geneId == null )
            throw new IllegalArgumentException( "Null id for gene" );
        Gene gene = this.load( geneId );

        if ( gene == null )
            throw new IllegalArgumentException( "No gene with id " + geneId );

        Collection<GeneProductValueObject> result = new ArrayList<>();
        for ( GeneProduct gp : gene.getProducts() ) {
            result.add( new GeneProductValueObject( gp ) );
        }

        return result;
    }

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

        // FIXME: this is redundant as aliases are setup by the converter
        Collection<GeneAlias> aliasObjects = gene.getAliases();
        SortedSet<String> aliasStrings = new TreeSet<>();
        for ( GeneAlias ga : aliasObjects ) {
            aliasStrings.add( ga.getAlias() );
        }
        gvo.setAliases( aliasStrings );

        if ( gene.getMultifunctionality() != null ) {
            gvo.setMultifunctionalityRank( gene.getMultifunctionality().getRank() );
        }

        Long compositeSequenceCount = this.getCompositeSequenceCountById( id );
        gvo.setCompositeSequenceCount( compositeSequenceCount.intValue() );

        Integer platformCount = this.geneDao.getPlatformCountById( id );
        gvo.setPlatformCount( platformCount );

        Collection<GeneSet> geneSets = this.geneSetSearch.findByGene( gene );
        Collection<GeneSetValueObject> gsVos = new ArrayList<>();
        //noinspection CollectionAddAllCanBeReplacedWithConstructor // Constructor can't handle subclasses
        gsVos.addAll( geneSetValueObjectHelper.convertToLightValueObjects( geneSets, false ) );

        gvo.setGeneSets( gsVos );

        Collection<Gene> geneHomologues = AsyncFactoryBeanUtils.getSilently( this.homologeneService, HomologeneServiceFactory.class ).getHomologues( gene );
        geneHomologues = this.thawLite( geneHomologues );
        Collection<GeneValueObject> homologues = this.loadValueObjects( geneHomologues );

        gvo.setHomologues( homologues );

        if ( gvo.getNcbiId() != null ) {
            SearchSettings s = SearchSettings.builder()
                    .query( "http://purl.org/commons/record/ncbi_gene/" + gvo.getNcbiId() )
                    .resultType( ExpressionExperiment.class )
                    .build();
            SearchService.SearchResultMap r;
            try {
                r = searchService.search( s );
                List<SearchResult<ExpressionExperiment>> hits = r.getByResultObjectType( ExpressionExperiment.class );
                gvo.setAssociatedExperimentCount( hits.size() );
            } catch ( SearchException e ) {
                log.error( "Failed to retrieve the associated EE count for " + s + ".", e );
                gvo.setAssociatedExperimentCount( null );
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
    public List<GeneValueObject> loadValueObjectsByIds( Collection<Long> ids ) {
        List<Gene> g = this.geneDao.loadThawed( ids );
        return this.loadValueObjects( g );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<GeneValueObject> loadValueObjectsByIdsLiter( Collection<Long> ids ) {
        Collection<Gene> g = this.geneDao.loadThawedLiter( ids );
        return this.loadValueObjects( g );
    }

    @Override
    @Transactional(readOnly = true)
    public Gene thaw( Gene gene ) {
        return this.geneDao.thaw( gene );
    }

    @Override
    @Transactional(readOnly = true)
    public Gene thawAliases( Gene gene ) {
        return this.geneDao.thawAliases( gene );
    }

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

    /**
     * Search for genes (by name or symbol)
     *
     * @param  taxonId, can be null to not constrain by taxon
     * @return Collection of Gene entity objects
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<GeneValueObject> searchGenes( String query, Long taxonId ) throws SearchException {

        Taxon taxon = null;
        if ( taxonId != null ) {
            taxon = this.taxonService.load( taxonId );
        }
        SearchSettings settings = SearchSettings.geneSearch( query, taxon );
        List<SearchResult<Gene>> geneSearchResults = this.searchService.search( settings ).getByResultObjectType( Gene.class );

        Collection<Gene> genes = new HashSet<>();
        if ( geneSearchResults == null || geneSearchResults.isEmpty() ) {
            log.info( "No Genes for search: " + query + " taxon=" + taxonId );
            return new HashSet<>();
        }
        log.info( "Gene search: " + query + " taxon=" + taxonId + ", " + geneSearchResults.size() + " found" );

        for ( SearchResult<Gene> sr : geneSearchResults ) {
            Gene g = sr.getResultObject();
            if ( g != null ) {
                g = this.thaw( g );
                genes.add( g );
                log.debug( "Gene search result: " + g.getOfficialSymbol() );
            }
        }
        Collection<GeneValueObject> geneValueObjects = this.loadValueObjects( genes );
        log.debug( "Gene search: " + geneValueObjects.size() + " value objects returned." );
        return geneValueObjects;
    }

    @Override
    @Transactional
    public int removeAll() {
        return geneDao.removeAll();
    }

}