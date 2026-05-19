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

package ubic.gemma.persistence.service.genome.gene;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.core.loader.genome.gene.ncbi.homology.HomologeneService;
import ubic.gemma.core.search.GeneSetSearch;
import ubic.gemma.core.search.SearchException;
import ubic.gemma.core.search.SearchService;
import ubic.gemma.core.util.concurrent.FutureUtils;
import ubic.gemma.model.association.Gene2GOAssociation;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.common.description.AnnotationValueObject;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.common.search.SearchResult;
import ubic.gemma.model.common.search.SearchSettings;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.PhysicalLocation;
import ubic.gemma.model.genome.PhysicalLocationValueObject;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.*;
import ubic.gemma.persistence.service.AbstractFilteringVoEnabledService;
import ubic.gemma.persistence.service.association.Gene2GOAssociationService;
import ubic.gemma.persistence.service.common.description.CharacteristicReadService;
import ubic.gemma.persistence.service.genome.GeneDao;
import ubic.gemma.persistence.service.genome.sequenceAnalysis.AnnotationAssociationService;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;

import org.springframework.lang.Nullable;
import java.util.*;
import java.util.concurrent.Future;

/**
 * @author pavlidis
 * @author keshav
 * @see GeneService
 */
@Service
public class GeneServiceImpl extends AbstractFilteringVoEnabledService<Gene, GeneValueObject> implements GeneService {

    private final GeneDao geneDao;
    private final GeneReadService geneReadService;

    @Autowired
    private AnnotationAssociationService annotationAssociationService;
    @Autowired
    private CharacteristicReadService characteristicService;
    @Autowired
    private Gene2GOAssociationService gene2GOAssociationService;
    @Autowired
    private GeneSetSearch geneSetSearch;
    @Autowired
    private GeneSetValueObjectHelper geneSetValueObjectHelper;
    @Autowired
    private SearchService searchService;
    @Autowired
    private TaxonService taxonService;
    @Autowired
    @Qualifier("homologeneService")
    private Future<HomologeneService> homologeneService;

    @Autowired
    public GeneServiceImpl( GeneDao geneDao, GeneReadService geneReadService ) {
        super( geneDao );
        this.geneDao = geneDao;
        this.geneReadService = geneReadService;
    }

    @Override
    public Collection<Gene> find( PhysicalLocation physicalLocation ) {
        return geneReadService.find( physicalLocation );
    }

    @Override
    public Gene findByAccession( final String accession, @Nullable final ExternalDatabase source ) {
        return geneReadService.findByAccession( accession, source );
    }

    @Override
    public Collection<Gene> findByAlias( final String search ) {
        return geneReadService.findByAlias( search );
    }

    @Override
    public Gene findByEnsemblId( String exactString ) {
        return geneReadService.findByEnsemblId( exactString );
    }

    @Override
    public Gene findByNCBIId( Integer accession ) {
        return geneReadService.findByNCBIId( accession );
    }

    @Override
    public GeneValueObject findByNCBIIdValueObject( Integer accession ) {
        return geneReadService.findByNCBIIdValueObject( accession );
    }

    @Override
    public Map<Integer, GeneValueObject> findByNcbiIds( Collection<Integer> ncbiIds ) {
        return geneReadService.findByNcbiIds( ncbiIds );
    }

    @Override
    public Collection<Gene> findByOfficialName( final String officialName ) {
        return geneReadService.findByOfficialName( officialName );
    }

    @Override
    public Collection<Gene> findByOfficialNameInexact( String officialName ) {
        return geneReadService.findByOfficialNameInexact( officialName );
    }

    @Override
    public Collection<Gene> findByOfficialSymbol( final String officialSymbol ) {
        return geneReadService.findByOfficialSymbol( officialSymbol );
    }

    @Override
    public Gene findByOfficialSymbol( final String symbol, final Taxon taxon ) {
        return geneReadService.findByOfficialSymbol( symbol, taxon );
    }

    @Override
    public Collection<Gene> findByOfficialSymbolInexact( final String officialSymbol ) {
        return geneReadService.findByOfficialSymbolInexact( officialSymbol );
    }

    @Override
    public Map<String, GeneValueObject> findByOfficialSymbols( Collection<String> query, Long taxonId ) {
        return geneReadService.findByOfficialSymbols( query, taxonId );
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
            AnnotationValueObject annotationValueObject = new AnnotationValueObject( assoc.getOntologyEntry() );
            annotationValueObject.setTermName( assoc.getOntologyEntry().getValue() );
            ontologies.add( annotationValueObject );
        }
        return annotationAssociationService.removeRootTerms( ontologies );
    }

    @Override
    public long getCompositeSequenceCount( Gene gene, boolean includeDummyProducts ) {
        return geneReadService.getCompositeSequenceCount( gene, includeDummyProducts );
    }

    @Override
    public long getCompositeSequenceCountById( final Long id, boolean includeDummyProducts ) {
        return geneReadService.getCompositeSequenceCountById( id, includeDummyProducts );
    }

    @Override
    public Collection<CompositeSequence> getCompositeSequences( final Gene gene, final ArrayDesign arrayDesign, boolean includeDummyProducts ) {
        return geneReadService.getCompositeSequences( gene, arrayDesign, includeDummyProducts );
    }

    @Override
    public Collection<CompositeSequence> getCompositeSequences( final Gene gene, boolean includeDummyProducts ) {
        return geneReadService.getCompositeSequences( gene, includeDummyProducts );
    }

    @Override
    public Collection<CompositeSequence> getCompositeSequencesById( Long geneId, boolean includeDummyProducts ) {
        return geneReadService.getCompositeSequencesById( geneId, includeDummyProducts );
    }

    @Override
    public List<PhysicalLocationValueObject> getPhysicalLocationsValueObjects( Gene gene ) {
        return geneReadService.getPhysicalLocationsValueObjects( gene );
    }

    @Override
    public Collection<GeneProductValueObject> getProducts( Long geneId ) {
        return geneReadService.getProducts( geneId );
    }

    @Override
    public Collection<Gene> loadAll( final Taxon taxon ) {
        return geneReadService.loadAll( taxon );
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

        long compositeSequenceCount = this.getCompositeSequenceCountById( id, true );
        gvo.setCompositeSequenceCount( ( int ) compositeSequenceCount );

        long platformCount = this.geneDao.getPlatformCountById( id, true );
        gvo.setPlatformCount( ( int ) platformCount );

        Collection<GeneSet> geneSets = this.geneSetSearch.findByGene( gene );
        Collection<GeneSetValueObject> gsVos = new ArrayList<>();
        //noinspection CollectionAddAllCanBeReplacedWithConstructor // Constructor can't handle subclasses
        gsVos.addAll( geneSetValueObjectHelper.convertToLightValueObjects( geneSets, false ) );

        gvo.setGeneSets( gsVos );

        Collection<Gene> geneHomologues = FutureUtils.get( this.homologeneService ).getHomologues( gene );
        geneHomologues = this.thawLite( geneHomologues );
        Collection<GeneValueObject> homologues = this.loadValueObjects( geneHomologues );

        gvo.setHomologues( homologues );

        populateAssociatedExperimentCount( Collections.singletonList( gvo ) );

        return gvo;
    }

    @Override
    public Collection<Gene> loadMicroRNAs( final Taxon taxon ) {
        return geneReadService.loadMicroRNAs( taxon );
    }

    @Override
    public Collection<Gene> loadThawed( Collection<Long> ids ) {
        return geneReadService.loadThawed( ids );
    }

    @Override
    public Collection<Gene> loadThawedLiter( Collection<Long> ids ) {
        return geneReadService.loadThawedLiter( ids );
    }

    @Override
    public GeneValueObject loadValueObjectById( Long id ) {
        return geneReadService.loadValueObjectById( id );
    }

    @Override
    public List<GeneValueObject> loadValueObjectsByIds( Collection<Long> ids ) {
        return geneReadService.loadValueObjectsByIds( ids );
    }

    @Override
    public Collection<GeneValueObject> loadValueObjectsByIdsLiter( Collection<Long> ids ) {
        return geneReadService.loadValueObjectsByIdsLiter( ids );
    }

    @Override
    public Gene thaw( Gene gene ) {
        return geneReadService.thaw( gene );
    }

    @Override
    public Gene thawAliases( Gene gene ) {
        return geneReadService.thawAliases( gene );
    }

    @Override
    public Collection<Gene> thawLite( final Collection<Gene> genes ) {
        return geneReadService.thawLite( genes );
    }

    @Override
    public Gene thawLite( Gene gene ) {
        return geneReadService.thawLite( gene );
    }

    @Override
    public Gene thawLiter( Gene gene ) {
        return geneReadService.thawLiter( gene );
    }

    /**
     * Search for genes (by name or symbol)
     *
     * @param taxonId, can be null to not constrain by taxon
     * @return Collection of Gene entity objects
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<GeneValueObject> searchGenes( String query, @Nullable Long taxonId ) throws SearchException {

        Taxon taxon = null;
        if ( taxonId != null ) {
            taxon = this.taxonService.load( taxonId );
        }
        SearchSettings settings = SearchSettings.geneSearch( query, taxon );
        List<SearchResult<Gene>> geneSearchResults = this.searchService
                .search( settings )
                .getByResultObjectType( Gene.class );

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

    /**
     * Looks up all gene URIs in a single {@link CharacteristicReadService#findExperimentsByUris}
     * call, then assigns the distinct-EE counts back to each VO. VOs without an NCBI ID are left at their
     * current value (the default initializer of 0).
     */
    @Override
    @Transactional(readOnly = true)
    public void populateAssociatedExperimentCount( @Nullable Collection<GeneValueObject> vos ) {
        if ( vos == null || vos.isEmpty() ) {
            return;
        }
        Map<String, List<GeneValueObject>> byUri = new HashMap<>();
        for ( GeneValueObject vo : vos ) {
            if ( vo != null && vo.getNcbiId() != null ) {
                byUri.computeIfAbsent( Gene.NCBI_URI_PREFIX + vo.getNcbiId(), k -> new ArrayList<>() ).add( vo );
            }
        }
        if ( byUri.isEmpty() ) {
            return;
        }
        // we are duplicating code from AnnotationsWebService.getDistinctEeCountsByUri here. consider refactoring
        Map<Class<? extends Identifiable>, Map<String, Set<ExpressionExperiment>>> hits =
                characteristicService.findExperimentsByUris( byUri.keySet(), true, true, true, null, -1, false, false );
        Map<String, Set<Long>> distinctEeIdsByUri = new HashMap<>();
        for ( Map<String, Set<ExpressionExperiment>> perClass : hits.values() ) {
            for ( Map.Entry<String, Set<ExpressionExperiment>> entry : perClass.entrySet() ) {
                Set<Long> bucket = distinctEeIdsByUri.computeIfAbsent( entry.getKey(), k -> new HashSet<>() );
                for ( ExpressionExperiment ee : entry.getValue() ) {
                    bucket.add( ee.getId() );
                }
            }
        }
        for ( Map.Entry<String, List<GeneValueObject>> entry : byUri.entrySet() ) {
            Set<Long> bucket = distinctEeIdsByUri.get( entry.getKey() );
            int count = bucket != null ? bucket.size() : 0;
            for ( GeneValueObject vo : entry.getValue() ) {
                vo.setAssociatedExperimentCount( count );
            }
        }
    }

    @Override
    @Transactional
    public int removeAll() {
        return geneDao.removeAll();
    }

}
