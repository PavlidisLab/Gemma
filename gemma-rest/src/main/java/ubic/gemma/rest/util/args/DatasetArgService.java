package ubic.gemma.rest.util.args;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ubic.basecode.ontology.model.OntologyTerm;
import ubic.gemma.core.analysis.preprocess.OutlierDetails;
import ubic.gemma.core.analysis.preprocess.OutlierDetectionService;
import ubic.gemma.core.search.Highlighter;
import ubic.gemma.core.search.SearchException;
import ubic.gemma.core.search.SearchResult;
import ubic.gemma.core.search.SearchService;
import ubic.gemma.model.common.description.AnnotationValueObject;
import ubic.gemma.model.common.quantitationtype.QuantitationTypeValueObject;
import ubic.gemma.model.common.search.SearchSettings;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssay.BioAssayValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.bioAssay.BioAssayService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.util.Filter;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Sort;
import ubic.gemma.rest.util.MalformedArgException;

import javax.annotation.Nullable;
import javax.ws.rs.BadRequestException;
import java.util.*;
import java.util.stream.Collectors;

@Service
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
    public Filters getFilters( FilterArg<ExpressionExperiment> filterArg ) throws BadRequestException {
        return getFilters( filterArg, null );
    }

    @Override
    public Sort getSort( SortArg<ExpressionExperiment> sortArg ) throws BadRequestException {
        Sort sort = super.getSort( sortArg );
        // always show non-null GEEQ scores first
        // there are two special cases in ExpressionExperimentDaoImpl to handle manual overrides of public quality and
        // suitability scores that need special treatment
        if ( "geeq".equals( sort.getObjectAlias() ) || "geeq.publicQualityScore".equals( sort.getOriginalProperty() ) || "geeq.publicSuitabilityScore".equals( sort.getOriginalProperty() ) ) {
            String alias = ( sort.getObjectAlias() != null ? sort.getObjectAlias() + "." : "" ) + sort.getPropertyName();
            return Sort.by( null, "(case when " + alias + " is not null then 0 else 1 end)", Sort.Direction.ASC )
                    .andThen( sort );
        } else {
            return sort;
        }
    }

    public Filters getFilters( FilterArg<ExpressionExperiment> filterArg, @Nullable Collection<OntologyTerm> mentionedTerm ) {
        return service.getFiltersWithInferredAnnotations( super.getFilters( filterArg ), mentionedTerm );
    }

    /**
     * Obtain the search results for a given query and highlighter.
     * @param highlighter a highlighter to use for the query or null to ignore
     * @throws BadRequestException if the query is empty
     */
    public List<SearchResult<ExpressionExperiment>> getResultsForSearchQuery( String query, @Nullable Highlighter highlighter ) throws BadRequestException {
        if ( StringUtils.isBlank( query ) ) {
            throw new BadRequestException( "A non-empty query must be supplied." );
        }
        try {
            SearchSettings settings = SearchSettings.builder()
                    .query( query )
                    .resultType( ExpressionExperiment.class )
                    .highlighter( highlighter )
                    .fillResults( false )
                    .build();
            return searchService.search( settings ).getByResultObjectType( ExpressionExperiment.class );
        } catch ( SearchException e ) {
            throw new MalformedArgException( "Invalid search query.", e );
        }
    }

    /**
     * Obtain a {@link Filter} for the result of a {@link ExpressionExperiment} search.
     * <p>
     * The filter is a restriction over the EE IDs.
     *
     * @param query     query
     * @param scoreById if non-null, a destination for storing the scores by result ID
     * @throws BadRequestException if the query is empty
     */
    public Set<Long> getIdsForSearchQuery( String query, @Nullable Map<Long, Double> scoreById ) throws BadRequestException {
        List<SearchResult<ExpressionExperiment>> _results = getResultsForSearchQuery( query, null );
        if ( scoreById != null ) {
            for ( SearchResult<ExpressionExperiment> result : _results ) {
                scoreById.put( result.getResultId(), result.getScore() );
            }
        }
        return _results.stream().map( SearchResult::getResultId ).collect( Collectors.toSet() );
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
        ExpressionExperiment ee = service.thawBioAssays( this.getEntity( arg ) );
        List<BioAssayValueObject> bioAssayValueObjects = baService.loadValueObjects( ee.getBioAssays(), true );
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
        return bioAssayValueObjects;
    }

    /**
     * @return a collection of Annotations value objects that represent the experiments annotations.
     */
    public Set<AnnotationValueObject> getAnnotations( DatasetArg<?> arg ) {
        ExpressionExperiment ee = this.getEntity( arg );
        return service.getAnnotationsById( ee.getId() );
    }
}
