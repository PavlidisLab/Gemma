package ubic.gemma.rest.util.args;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ubic.basecode.ontology.model.OntologyTerm;
import ubic.gemma.core.analysis.preprocess.OutlierDetails;
import ubic.gemma.core.analysis.preprocess.OutlierDetectionService;
import ubic.gemma.core.search.*;
import ubic.gemma.model.common.description.AnnotationValueObject;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.BibliographicReferenceValueObject;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.QuantitationTypeValueObject;
import ubic.gemma.model.common.search.SearchSettings;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssay.BioAssayUtils;
import ubic.gemma.model.expression.bioAssay.BioAssayValueObject;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.bioAssay.BioAssayService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.rest.util.MalformedArgException;

import javax.annotation.Nullable;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.ServiceUnavailableException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

@Service
@CommonsLog
public class DatasetArgService extends AbstractEntityArgService<ExpressionExperiment, ExpressionExperimentService> {

    private final SearchService searchService;
    private final ArrayDesignService adService;
    private final BioAssayService baService;
    private final OutlierDetectionService outlierDetectionService;

    @Autowired
    public DatasetArgService( ExpressionExperimentService service, SearchService searchService, ArrayDesignService adService, BioAssayService baService, OutlierDetectionService outlierDetectionService ) {
        super( service );
        this.searchService = searchService;
        this.adService = adService;
        this.baService = baService;
        this.outlierDetectionService = outlierDetectionService;
    }

    /**
     * Retrieve an ID for a given dataset argument.
     */
    @Nullable
    public Long getEntityId( DatasetArg<?> datasetArg ) {
        return datasetArg.getEntityId( service );
    }

    /**
     * Obtain a list of exclude URIs from an argument containing excluded URIs.
     * @param excludedUrisArg    argument containing excluded URIs or null if unspecified
     * @param excludeFreeText if true, null will be included in the returned list which will result in the exclusion of
     *                        free-text categories or terms
     * @return null if excludedUrisArg is null and excludeFreeText is false, otherwise a list of excluded URIs
     */
    @Nullable
    public List<String> getExcludedUris( @Nullable StringArrayArg excludedUrisArg, boolean excludeFreeText, boolean excludeUncategorizedTerms ) {
        List<String> result = null;
        if ( excludedUrisArg != null ) {
            result = excludedUrisArg.getValue();
        }
        if ( excludeFreeText || excludeUncategorizedTerms ) {
            if ( result == null ) {
                result = new ArrayList<>();
            } else {
                result = new ArrayList<>( result );
            }
        }
        if ( excludeFreeText ) {
            result.add( ExpressionExperimentService.FREE_TEXT );
        }
        if ( excludeUncategorizedTerms ) {
            result.add( ExpressionExperimentService.UNCATEGORIZED );
        }
        return result;
    }

    @Override
    public Filters getFilters( FilterArg<ExpressionExperiment> filterArg ) throws BadRequestException, ServiceUnavailableException {
        return getFilters( filterArg, null, null );
    }

    public Filters getFilters( FilterArg<ExpressionExperiment> filterArg, @Nullable Collection<OntologyTerm> mentionedTerms, @Nullable Collection<OntologyTerm> inferredTerms ) throws ServiceUnavailableException {
        try {
            return service.getFiltersWithInferredAnnotations( super.getFilters( filterArg ), mentionedTerms, inferredTerms, 30, TimeUnit.SECONDS );
        } catch ( TimeoutException e ) {
            throw new ServiceUnavailableException( "Inferring terms for the filter timed out.", DateUtils.addSeconds( new Date(), 30 ), e );
        }
    }

    public Filters getFilters( FilterArg<ExpressionExperiment> filterArg, @Nullable Collection<OntologyTerm> mentionedTerms, @Nullable Collection<OntologyTerm> inferredTerms, long timeout, TimeUnit timeUnit ) throws TimeoutException {
        return service.getFiltersWithInferredAnnotations( super.getFilters( filterArg ), mentionedTerms, inferredTerms, timeout, timeUnit );
    }

    /**
     * Obtain the search results for a given query and highlighter.
     *
     * @param highlighter   a highlighter to use for the query or null to ignore
     * @param queryWarnings a collection that will receive warnings regarding the full-text query
     * @throws BadRequestException          if the query is empty
     * @throws ServiceUnavailableException  if the search times out
     * @throws InternalServerErrorException for any other search-related exceptions
     */
    public List<SearchResult<ExpressionExperiment>> getResultsForSearchQuery( QueryArg query, @Nullable Highlighter highlighter, @Nullable Collection<Throwable> queryWarnings ) throws BadRequestException, ServiceUnavailableException, InternalServerErrorException {
        try {
            SearchSettings settings = SearchSettings.builder()
                    .query( query.getValue() )
                    .resultType( ExpressionExperiment.class )
                    .fillResults( false )
                    .build();
            return searchService.search( settings, new SearchContext( highlighter, queryWarnings != null ? queryWarnings::add : null ) )
                    .getByResultObjectType( ExpressionExperiment.class );
        } catch ( ParseSearchException e ) {
            throw new MalformedArgException( "Invalid search query: " + e.getQuery(), e );
        } catch ( SearchTimeoutException e ) {
            throw new ServiceUnavailableException( e.getMessage(), DateUtils.addSeconds( new Date(), 30 ), e.getCause() );
        } catch ( SearchException e ) {
            throw new InternalServerErrorException( e );
        }
    }

    /**
     * Shortcut for extracting the result IDs and scores from {@link #getResultsForSearchQuery(QueryArg, Highlighter, Collection)}.
     * @see #getResultsForSearchQuery(QueryArg, Highlighter, Collection)
     */
    public Set<Long> getIdsForSearchQuery( QueryArg query, Map<Long, Double> scoreById, @Nullable Collection<Throwable> queryWarnings ) {
        List<SearchResult<ExpressionExperiment>> _results = getResultsForSearchQuery( query, null, queryWarnings );
        for ( SearchResult<ExpressionExperiment> result : _results ) {
            scoreById.put( result.getResultId(), result.getScore() );
        }
        return _results.stream().map( SearchResult::getResultId ).collect( Collectors.toSet() );
    }

    /**
     * Shortcut for extracting the result IDs from {@link #getResultsForSearchQuery(QueryArg, Highlighter, Collection)}.
     * @see #getResultsForSearchQuery(QueryArg, Highlighter, Collection)
     */
    public Set<Long> getIdsForSearchQuery( QueryArg query, @Nullable Collection<Throwable> queryWarnings ) {
        return getResultsForSearchQuery( query, null, queryWarnings ).stream()
                .map( SearchResult::getResultId )
                .collect( Collectors.toSet() );
    }

    /**
     * Retrieve a dataset with quantitation type initialized.
     */
    public Set<QuantitationTypeValueObject> getQuantitationTypes( DatasetArg<?> arg ) {
        return new HashSet<>( service.getQuantitationTypeValueObjects( getEntity( arg ) ) );
    }

    /**
     * Retrieves the Platforms of the Dataset that this argument represents.
     *
     * @return a collection of Platforms that the dataset represented by this argument is in.
     */
    public List<ArrayDesignValueObject> getPlatforms( DatasetArg<?> arg ) {
        ExpressionExperiment ee = this.getEntity( arg );
        return adService.loadValueObjectsForEE( ee.getId() );
    }

    /**
     * @return a collection of BioAssays that represent the experiments samples.
     */
    public List<BioAssayValueObject> getSamples( DatasetArg<?> arg ) {
        ExpressionExperiment ee = service.thawLite( this.getEntity( arg ) );
        List<BioAssayValueObject> bioAssayValueObjects = baService.loadValueObjects( ee.getBioAssays(), null, true, true );
        populateOutliers( ee, bioAssayValueObjects );
        return bioAssayValueObjects;
    }

    /**
     * Obtain a collection of BioAssays that represent the experiments samples for a particular quantitation type.
     */
    public List<BioAssayValueObject> getSamples( DatasetArg<?> datasetArg, QuantitationType qt ) {
        ExpressionExperiment ee = service.thawLite( getEntity( datasetArg ) );
        BioAssayDimension bad = service.getBioAssayDimensionWithAssays( ee, qt );
        if ( bad == null ) {
            throw new NotFoundException( "There are no assays associated to " + qt + "." );
        }
        Map<BioAssay, BioAssay> assay2sourceAssayMap = BioAssayUtils.createBioAssayToSourceBioAssayMap( ee, bad.getBioAssays() );
        List<BioAssayValueObject> bioAssayValueObjects = baService.loadValueObjects( bad.getBioAssays(), assay2sourceAssayMap, true, true );
        populateOutliers( ee, bioAssayValueObjects );
        return bioAssayValueObjects;
    }

    /**
     * @return a collection of Annotations value objects that represent the experiments annotations.
     */
    public Set<AnnotationValueObject> getAnnotations( DatasetArg<?> arg ) {
        ExpressionExperiment ee = this.getEntity( arg );
        return service.getAnnotations( ee );
    }

    public List<ExpressionExperimentSubSet> getSubSets( DatasetArg<?> datasetArg ) {
        return service.getSubSetsWithCharacteristics( getEntity( datasetArg ) ).stream()
                .sorted( Comparator.comparing( ExpressionExperimentSubSet::getName ) )
                .collect( Collectors.toList() );
    }

    public ExpressionExperimentSubSet getSubSet( DatasetArg<?> datasetArg, Long subSetId ) {
        ExpressionExperiment ee = getEntity( datasetArg );
        ExpressionExperimentSubSet subset = service.getSubSetByIdWithCharacteristics( ee, subSetId );
        if ( subset == null ) {
            throw new NotFoundException( "No subset found with ID " + subSetId );
        }
        return subset;
    }

    public List<Long> getSubSetGroupIds( DatasetArg<?> datasetArg, ExpressionExperimentSubSet subset ) {
        // TODO: only retrieve the subset groups for the given subset
        return getSubSetsGroupIds( datasetArg ).getOrDefault( subset, Collections.emptyList() );
    }

    public Map<ExpressionExperimentSubSet, List<Long>> getSubSetsGroupIds( DatasetArg<?> datasetArg ) {
        Map<BioAssayDimension, Set<ExpressionExperimentSubSet>> ss2bad = service.getSubSetsByDimension( getEntity( datasetArg ) );
        Map<ExpressionExperimentSubSet, List<Long>> subSetGroups = new HashMap<>();
        for ( Map.Entry<BioAssayDimension, Set<ExpressionExperimentSubSet>> entry : ss2bad.entrySet() ) {
            for ( ExpressionExperimentSubSet s : entry.getValue() ) {
                subSetGroups.computeIfAbsent( s, k -> new ArrayList<>() )
                        .add( entry.getKey().getId() );
            }
        }
        subSetGroups.values().forEach( list -> list.sort( Comparator.naturalOrder() ) );
        return subSetGroups;
    }

    public List<BioAssayValueObject> getSubSetSamples( DatasetArg<?> datasetArg, Long subSetId ) {
        ExpressionExperiment ee = getEntity( datasetArg );
        ExpressionExperimentSubSet subset = service.getSubSetByIdWithCharacteristicsAndBioAssays( ee, subSetId );
        if ( subset == null ) {
            throw new NotFoundException( "No subset found with ID " + subSetId );
        }
        Map<BioAssay, BioAssay> assay2sourceAssayMap = BioAssayUtils.createBioAssayToSourceBioAssayMap( subset.getSourceExperiment(), subset.getBioAssays() );
        List<BioAssayValueObject> bioAssayValueObjects = baService.loadValueObjects( subset.getBioAssays(), assay2sourceAssayMap, true, true );
        populateOutliers( subset.getSourceExperiment(), bioAssayValueObjects );
        return bioAssayValueObjects;
    }

    public QuantitationType getPreferredQuantitationType( DatasetArg<?> datasetArg ) {
        return service.getPreferredQuantitationType( getEntity( datasetArg ) )
                .orElseThrow( () -> new NotFoundException( "No preferred quantitation type found for dataset with ID " + datasetArg + "." ) );
    }

    public List<BibliographicReferenceValueObject> getPublications( DatasetArg<?> datasetArg ) {
        Long eeId = getEntityId( datasetArg );
        if (eeId == null) {
            throw new NotFoundException( "Dataset " + datasetArg + " does not exist." );
        }
        ExpressionExperiment ee = service.loadWithPrimaryPublicationAndOtherRelevantPublications( eeId );
        if (ee == null){
            throw new NotFoundException( "Dataset " + datasetArg + " does not exist." );
        }
        BibliographicReference prim_ref = ee.getPrimaryPublication();
        Set<BibliographicReference> other_refs = ee.getOtherRelevantPublications();
        List<BibliographicReferenceValueObject> out = new ArrayList<>();
        if (prim_ref != null){
            out.add(  new BibliographicReferenceValueObject(prim_ref) );
        }
        for (BibliographicReference ref : other_refs) {
            if (prim_ref != null && Objects.equals( ref.getId(), prim_ref.getId() )){
                    continue;
            }
            out.add( new BibliographicReferenceValueObject(ref));
        }

        out.sort( Comparator.comparing( BibliographicReferenceValueObject::getId ) );

        return out;
    }

    public void populateOutliers( ExpressionExperiment ee, Collection<BioAssayValueObject> bioAssayValueObjects ) {
        Collection<OutlierDetails> outliers = outlierDetectionService.getOutlierDetails( ee );
        if ( outliers != null ) {
            Set<Long> predictedOutlierBioAssayIds = outliers.stream()
                    .map( OutlierDetails::getBioAssay )
                    .map( BioAssay::getId )
                    .collect( Collectors.toSet() );
            for ( BioAssayValueObject vo : bioAssayValueObjects ) {
                vo.setPredictedOutlier( predictedOutlierBioAssayIds.contains( vo.getId() ) );
            }
        }
    }
}
