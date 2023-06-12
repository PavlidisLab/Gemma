package ubic.gemma.rest.util.args;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ubic.gemma.core.analysis.preprocess.OutlierDetails;
import ubic.gemma.core.analysis.preprocess.OutlierDetectionService;
import ubic.gemma.core.search.SearchException;
import ubic.gemma.core.search.SearchResult;
import ubic.gemma.core.search.SearchService;
import ubic.gemma.model.common.description.AnnotationValueObject;
import ubic.gemma.model.common.quantitationtype.QuantitationTypeValueObject;
import ubic.gemma.model.common.search.Highlighter;
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

import javax.annotation.Nullable;
import javax.ws.rs.BadRequestException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

    @Override
    public Filters getFilters( FilterArg<ExpressionExperiment> filterArg ) throws BadRequestException {
        return getFilters( filterArg, null );
    }

    public Filters getFilters( FilterArg<ExpressionExperiment> filterArg, @Nullable Collection<String> impliedTermUris ) {
        return service.getFiltersWithInferredAnnotations( super.getFilters( filterArg ), impliedTermUris );
    }

    /**
     * Obtain a {@link Filter} for the result of a {@link ExpressionExperiment} search.
     * <p>
     * The filter is a restriction over the EE IDs.
     *
     * @param query    query
     * @param minScore minimum score to retain a result
     * @param _results destination for the search results
     */
    public Filter getFilterForSearchQuery( String query, double minScore, @Nullable Highlighter highlighter, @Nullable List<SearchResult<ExpressionExperiment>> _results ) {
        try {
            SearchSettings settings = SearchSettings.builder()
                    .query( query )
                    .resultType( ExpressionExperiment.class )
                    .highlighter( highlighter )
                    .fillResults( false )
                    .build();
            List<SearchResult<ExpressionExperiment>> results = searchService.search( settings )
                    .getByResultObjectType( ExpressionExperiment.class )
                    .stream()
                    .filter( r -> r.getScore() >= minScore )
                    .collect( Collectors.toList() );
            if ( _results != null ) {
                _results.addAll( results );
            }
            Set<Long> ids = results.stream()
                    .map( SearchResult::getResultId )
                    .collect( Collectors.toSet() );
            if ( ids.isEmpty() ) {
                return service.getFilter( "id", Long.class, Filter.Operator.eq, -1L );
            } else {
                return service.getFilter( "id", Long.class, Filter.Operator.in, ids );
            }
        } catch ( SearchException e ) {
            throw new BadRequestException( e );
        }
    }

    /**
     * @see #getFilterForSearchQuery(String, double, Highlighter, List)
     */
    public Filter getFilterForSearchQuery( String query ) {
        return getFilterForSearchQuery( query, 0.0, null, null );
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
