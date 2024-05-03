/*
 * The gemma-web project
 *
 * Copyright (c) 2015 University of British Columbia
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

package ubic.gemma.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.Explode;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.Value;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ubic.basecode.ontology.model.OntologyTerm;
import ubic.gemma.core.expression.experiment.service.ExpressionExperimentSearchService;
import ubic.gemma.core.ontology.OntologyService;
import ubic.gemma.core.search.*;
import ubic.gemma.core.search.lucene.LuceneQueryUtils;
import ubic.gemma.model.common.description.CharacteristicValueObject;
import ubic.gemma.model.common.search.SearchSettings;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.common.description.CharacteristicService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Slice;
import ubic.gemma.persistence.util.Sort;
import ubic.gemma.rest.util.QueriedAndFilteredAndPaginatedResponseDataObject;
import ubic.gemma.rest.util.Responder;
import ubic.gemma.rest.util.ResponseDataObject;
import ubic.gemma.rest.util.ResponseErrorObject;
import ubic.gemma.rest.util.args.*;

import javax.annotation.Nullable;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.net.URI;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * RESTful interface for annotations.
 *
 * @author tesarst
 */
@Service
@Path("/annotations")
public class AnnotationsWebService {


    private static final String SEARCH_QUERY_DESCRIPTION = "A comma-delimited list of keywords to find annotations.";

    private static final String FIND_CHARACTERISTICS_TIMEOUT_DESCRIPTION = "The search for annotations has timed out. It can generally be resolved by reattempting the search 30 seconds later. Lookup the `Retry-After` header for the recommended delay.";

    /**
     * Amout of time allowed to spend on finding characteristics.
     */
    private static final long FIND_CHARACTERISTICS_TIMEOUT_MS = 30000;

    private OntologyService ontologyService;
    private SearchService searchService;
    private CharacteristicService characteristicService;
    private ExpressionExperimentService expressionExperimentService;
    private DatasetArgService datasetArgService;
    private TaxonArgService taxonArgService;

    /**
     * Required by spring
     */
    public AnnotationsWebService() {
    }

    /**
     * Constructor for service autowiring
     */
    @Autowired
    public AnnotationsWebService( OntologyService ontologyService, SearchService searchService,
            CharacteristicService characteristicService, ExpressionExperimentService expressionExperimentService,
            DatasetArgService datasetArgService, TaxonArgService taxonArgService ) {
        this.ontologyService = ontologyService;
        this.searchService = searchService;
        this.characteristicService = characteristicService;
        this.expressionExperimentService = expressionExperimentService;
        this.datasetArgService = datasetArgService;
        this.taxonArgService = taxonArgService;
    }

    /*https://www.w3.org/TR/owl-ref/#subClassOf-def*
     * Obtain the parent of a given annotation.
     * <p>
     * This is plural as we might add support for querying multiple annotations at once in the future.
     */
    @GET
    @Path("/parents")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve the parents of the given annotations",
            description = "Terms that are returned satisfies the [rdfs:subClassOf](https://www.w3.org/TR/2012/REC-owl2-syntax-20121211/#Subclass_Axioms) or [part_of](http://purl.obolibrary.org/obo/BFO_0000050) relations. When `direct` is set to false, this rule is applied recursively.",
            responses = {
                    @ApiResponse(useReturnTypeSchema = true, content = @Content()),
                    @ApiResponse(responseCode = "404", description = "No term matched the given URI."),
                    @ApiResponse(responseCode = "503", description = "Ontology inference timed out.") })
    public List<AnnotationSearchResultValueObject> getAnnotationsParents(
            @Parameter(description = "Term URI") @QueryParam("uri") String termUri,
            @Parameter(description = "Only include direct children.") @QueryParam("direct") @DefaultValue("false") boolean direct ) {
        return getAnnotationsParentsOrChildren( termUri, direct, true );
    }

    /**
     * Obtain the children of a given annotation.
     * <p>
     * This is plural as we might add support for querying multiple annotations at once in the future.
     */
    @GET
    @Path("/children")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve the children of the given annotations",
            description = "Terms that are returned satisfies the [inverse of rdfs:subClassOf](https://www.w3.org/TR/2012/REC-owl2-syntax-20121211/#Subclass_Axioms) or [has_part](http://purl.obolibrary.org/obo/BFO_0000051) relations. When `direct` is set to false, this rule is applied recursively.",
            responses = {
                    @ApiResponse(useReturnTypeSchema = true, content = @Content()),
                    @ApiResponse(responseCode = "404", description = "No term matched the given URI."),
                    @ApiResponse(responseCode = "503", description = "Ontology inference timed out.") })
    public List<AnnotationSearchResultValueObject> getAnnotationsChildren(
            @Parameter(description = "Term URI") @QueryParam("uri") String termUri,
            @Parameter(description = "Only include direct parents.") @QueryParam("direct") @DefaultValue("false") boolean direct ) {
        return getAnnotationsParentsOrChildren( termUri, direct, false );
    }

    private List<AnnotationSearchResultValueObject> getAnnotationsParentsOrChildren( String termUri, boolean direct, boolean parents ) {
        if ( StringUtils.isBlank( termUri ) ) {
            throw new BadRequestException( "The 'uri' parameter must not be blank." );
        }
        OntologyTerm term = ontologyService.getTerm( termUri );
        if ( term == null ) {
            throw new NotFoundException( "No ontology term with URI " + termUri );
        }
        try {
            return ( parents ? ontologyService.getParents( Collections.singleton( term ), direct, true, 30, TimeUnit.SECONDS ) :
                    ontologyService.getChildren( Collections.singleton( term ), direct, true, 30, TimeUnit.SECONDS ) ).stream()
                    .map( t -> new AnnotationSearchResultValueObject( t.getLabel(), t.getUri(), null, null ) )
                    .collect( Collectors.toList() );
        } catch ( TimeoutException e ) {
            throw new ServiceUnavailableException( DateUtils.addSeconds( new Date(), 30 ), e );
        }
    }

    /**
     * Does a search for annotation tags based on the given string.
     *
     * @param query the search query. Either plain text, or an ontology term URI
     * @return response data object with a collection of found terms, each wrapped in a CharacteristicValueObject.
     * @see OntologyService#findTermsInexact(String, Taxon) for better description of the search process.
     * @see CharacteristicValueObject for the output object structure.
     */
    @GET
    @Path("/search")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Search for annotation tags", responses = {
            @ApiResponse(useReturnTypeSchema = true, content = @Content()),
            @ApiResponse(responseCode = "400", description = "The search query is empty or invalid.", content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))),
            @ApiResponse(responseCode = "503", description = FIND_CHARACTERISTICS_TIMEOUT_DESCRIPTION, content = @Content(schema = @Schema(implementation = ResponseErrorObject.class)))
    })
    public ResponseDataObject<List<AnnotationSearchResultValueObject>> searchAnnotations(
            @Parameter(schema = @Schema(implementation = StringArrayArg.class), explode = Explode.FALSE, description = SEARCH_QUERY_DESCRIPTION) @QueryParam("query") @DefaultValue("") StringArrayArg query
    ) {
        if ( query == null || query.getValue().isEmpty() ) {
            throw new BadRequestException( "Search query cannot be empty." );
        }
        try {
            return Responder.respond( new ArrayList<>( this.getTerms( query, FIND_CHARACTERISTICS_TIMEOUT_MS ) ) );
        } catch ( SearchTimeoutException e ) {
            throw new ServiceUnavailableException( e.getMessage(), DateUtils.addSeconds( new Date(), 30 ), e.getCause() );
        } catch ( ParseSearchException e ) {
            throw new BadRequestException( "Invalid search query: " + e.getQuery(), e );
        } catch ( SearchException e ) {
            throw new InternalServerErrorException( e );
        }
    }

    /**
     * @see #searchAnnotations(StringArrayArg)
     */
    @GET
    @Path("/search/{query}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Search for annotation tags",
            description = "This is deprecated in favour of passing `query` as a query parameter.",
            deprecated = true,
            responses = {
                    @ApiResponse(useReturnTypeSchema = true, content = @Content()),
                    @ApiResponse(responseCode = "400", description = "The search query is empty or invalid.", content = @Content(schema = @Schema(implementation = ResponseErrorObject.class)))
            })
    public ResponseDataObject<List<AnnotationSearchResultValueObject>> searchAnnotationsByPathQuery( // Params:
            @Parameter(schema = @Schema(implementation = StringArrayArg.class), explode = Explode.FALSE, description = SEARCH_QUERY_DESCRIPTION) @PathParam("query") @DefaultValue("") StringArrayArg query // Required
    ) {
        return searchAnnotations( query );
    }

    /**
     * Does a search for datasets containing characteristics matching the given string.
     * If filterArg, offset, limit or sortArg parameters are provided.
     *
     * @param query the search query. Either plain text, or an ontology term URI
     * @return response data object with a collection of dataset that match the search query.
     * @see ExpressionExperimentSearchService#searchExpressionExperiments(String) for better description of the search process.
     */
    @GET
    @Path("/search/datasets")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve datasets associated to an annotation tags search",
            description = "This is deprecated in favour of the [/datasets](#/default/getDatasets) endpoint. Use the `AND` operator to intersect the results of multiple queries.",
            deprecated = true,
            responses = {
                    @ApiResponse(useReturnTypeSchema = true, content = @Content()),
                    @ApiResponse(responseCode = "400", description = "The search query is empty or invalid.", content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))),
                    @ApiResponse(responseCode = "503", description = FIND_CHARACTERISTICS_TIMEOUT_DESCRIPTION, content = @Content(schema = @Schema(implementation = ResponseErrorObject.class)))
            })
    public QueriedAndFilteredAndPaginatedResponseDataObject<ExpressionExperimentValueObject> searchDatasets( // Params:
            @Parameter(schema = @Schema(implementation = StringArrayArg.class), explode = Explode.FALSE, description = SEARCH_QUERY_DESCRIPTION + " Matching datasets for each query are intersected.") @QueryParam("query") @DefaultValue("") StringArrayArg query,
            @QueryParam("filter") @DefaultValue("") FilterArg<ExpressionExperiment> filterArg, // Optional, default null
            @QueryParam("offset") @DefaultValue("0") OffsetArg offset, // Optional, default 0
            @QueryParam("limit") @DefaultValue("20") LimitArg limit, // Optional, default 20
            @QueryParam("sort") @DefaultValue("+id") SortArg<ExpressionExperiment> sortArg // Optional, default +id
    ) {
        if ( query == null || query.getValue().isEmpty() ) {
            throw new BadRequestException( "Search query cannot be empty." );
        }
        Collection<Long> foundIds;
        try {
            foundIds = this.searchEEs( query.getValue(), null );
        } catch ( ParseSearchException e ) {
            throw new BadRequestException( "Invalid search query: " + e.getQuery(), e );
        } catch ( SearchTimeoutException e ) {
            throw new ServiceUnavailableException( e.getMessage(), DateUtils.addSeconds( new Date(), 30 ), e.getCause() );
        } catch ( SearchException e ) {
            throw new InternalServerErrorException( e );
        }

        Filters filters = datasetArgService.getFilters( filterArg );
        Sort sort = datasetArgService.getSort( sortArg );

        Slice<ExpressionExperimentValueObject> slice;
        if ( foundIds.isEmpty() ) {
            slice = new Slice<>( Collections.emptyList(), sort, offset.getValue(), limit.getValue(), 0L );
        } else if ( filters.isEmpty()
                && offset.getValue() == 0
                && foundIds.size() <= limit.getValue()
                && sort.getPropertyName().

                equals( "id" )
                && sort.getDirection() == Sort.Direction.ASC ) {
            slice = new Slice<>( expressionExperimentService.loadValueObjectsByIds( foundIds ), sort, 0, limit.getValue(), ( long ) foundIds.size() );

        } else {
            // Otherwise there is no need to go the pre-filter path since we already know exactly what IDs we want.
            // If there are filters other than the search query, intersect the results.
            Filters filtersWithQuery = Filters.by( filters ).and( datasetArgService.getFilters( DatasetArrayArg.valueOf( StringUtils.join( foundIds, ',' ) ) ) );
            slice = expressionExperimentService.loadValueObjects( filtersWithQuery, sort, offset.getValue(), limit.getValue() );
        }

        return Responder.queryAndPaginate( slice, String.join( " AND ", query.getValue() ), filters, new String[]

                {
                        "id"
                } );
    }

    @GET
    @Path("/search/{query}/datasets")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve datasets associated to an annotation tags search",
            description = "This is deprecated in favour of passing `query` as a query parameter.",
            deprecated = true,
            responses = {
                    @ApiResponse(useReturnTypeSchema = true, content = @Content()),
                    @ApiResponse(responseCode = "400", description = "The search query is empty or invalid.", content = @Content(schema = @Schema(implementation = ResponseErrorObject.class)))
            })
    public QueriedAndFilteredAndPaginatedResponseDataObject<ExpressionExperimentValueObject> searchDatasetsByQueryInPath( // Params:
            @Parameter(schema = @Schema(implementation = StringArrayArg.class), explode = Explode.FALSE, description = SEARCH_QUERY_DESCRIPTION + " Matching datasets for each query are intersected.")
            @PathParam("query") @DefaultValue("") StringArrayArg query, // Required
            @QueryParam("filter") @DefaultValue("") FilterArg<ExpressionExperiment> filterArg, // Optional, default null
            @QueryParam("offset") @DefaultValue("0") OffsetArg offset, // Optional, default 0
            @QueryParam("limit") @DefaultValue("20") LimitArg limit, // Optional, default 20
            @QueryParam("sort") @DefaultValue("+id") SortArg<ExpressionExperiment> sortArg // Optional, default +id
    ) {
        return searchDatasets( query, filterArg, offset, limit, sortArg );
    }

    /**
     * Same as {@link #searchDatasets(StringArrayArg, FilterArg, OffsetArg, LimitArg, SortArg)} but also filters by
     * taxon.
     */
    @GET
    @Path("/{taxon}/search/datasets")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve datasets within a given taxa associated to an annotation tags search",
            description = "This is deprecated in favour of the [/datasets](#/default/getDatasets) endpoint with a `query` parameter and a `filter` parameter with `taxon.id = {taxon} or taxon.commonName = {taxon} or taxon.scientificName = {taxon}` to restrict the taxon instead.  Use the `AND` operator to intersect the results of multiple queries.",
            deprecated = true,
            responses = {
                    @ApiResponse(useReturnTypeSchema = true, content = @Content()),
                    @ApiResponse(responseCode = "400", description = "The search query is empty or invalid.", content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))),
                    @ApiResponse(responseCode = "503", description = FIND_CHARACTERISTICS_TIMEOUT_DESCRIPTION, content = @Content(schema = @Schema(implementation = ResponseErrorObject.class)))
            })
    public QueriedAndFilteredAndPaginatedResponseDataObject<ExpressionExperimentValueObject> searchTaxonDatasets( // Params:
            @PathParam("taxon") TaxonArg<?> taxonArg, // Required
            @Parameter(schema = @Schema(implementation = StringArrayArg.class), explode = Explode.FALSE, description = SEARCH_QUERY_DESCRIPTION + " Matching datasets for each query are intersected.")
            @QueryParam("query") @DefaultValue("") StringArrayArg query,
            @QueryParam("filter") @DefaultValue("") FilterArg<ExpressionExperiment> filter, // Optional, default null
            @QueryParam("offset") @DefaultValue("0") OffsetArg offset, // Optional, default 0
            @QueryParam("limit") @DefaultValue("20") LimitArg limit, // Optional, default 20
            @QueryParam("sort") @DefaultValue("+id") SortArg<ExpressionExperiment> sort // Optional, default +id
    ) {
        if ( query == null || query.getValue().isEmpty() ) {
            throw new BadRequestException( "Search query cannot be empty." );
        }

        // will raise a NotFoundException early if not found
        Taxon taxon = taxonArgService.getEntity( taxonArg );

        Collection<Long> foundIds;
        try {
            foundIds = this.searchEEs( query.getValue(), taxon );
        } catch ( ParseSearchException e ) {
            throw new BadRequestException( "Invalid search query: " + e.getQuery(), e );
        } catch ( SearchTimeoutException e ) {
            throw new ServiceUnavailableException( e.getMessage(), DateUtils.addSeconds( new Date(), 30 ), e.getCause() );
        } catch ( SearchException e ) {
            throw new InternalServerErrorException( e );
        }

        // We always have to do filtering, because we always have at least the taxon argument (otherwise this#datasets method is used)
        Filters filters = datasetArgService.getFilters( filter ).and( taxonArgService.getFilters( taxonArg ) );

        Slice<ExpressionExperimentValueObject> slice;
        if ( foundIds.isEmpty() ) {
            slice = new Slice<>( Collections.emptyList(), datasetArgService.getSort( sort ), offset.getValue(), limit.getValue(), 0L );
        } else {
            // We always have to do filtering, because we always have at least the taxon argument (otherwise this#datasets method is used)
            Filters filtersWithQuery = Filters.by( filters ).and( datasetArgService.getFilters( DatasetArrayArg.valueOf( StringUtils.join( foundIds, ',' ) ) ) );
            slice = expressionExperimentService.loadValueObjects( filtersWithQuery, datasetArgService.getSort( sort ), offset.getValue(), limit.getValue() );
        }

        return Responder.queryAndPaginate( slice, String.join( " AND ", query.getValue() ), filters, new String[] { "id" } );
    }

    /**
     * @see #searchDatasets(StringArrayArg, FilterArg, OffsetArg, LimitArg, SortArg)
     */
    @GET
    @Path("/{taxon}/search/{query}/datasets")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve datasets within a given taxa associated to an annotation tags search",
            description = "This is deprecated in favour of passing `query` as a query parameter.",
            deprecated = true,
            responses = {
                    @ApiResponse(useReturnTypeSchema = true, content = @Content()),
                    @ApiResponse(responseCode = "400", description = "The search query is empty or invalid.", content = @Content(schema = @Schema(implementation = ResponseErrorObject.class)))
            })
    public QueriedAndFilteredAndPaginatedResponseDataObject<ExpressionExperimentValueObject> searchTaxonDatasetsByQueryInPath( // Params:
            @PathParam("taxon") TaxonArg<?> taxonArg, // Required
            @Parameter(schema = @Schema(implementation = StringArrayArg.class), explode = Explode.FALSE, description = SEARCH_QUERY_DESCRIPTION + " Matching datasets for each query are intersected.")
            @PathParam("query") @DefaultValue("") StringArrayArg query, // Required
            @QueryParam("filter") @DefaultValue("") FilterArg<ExpressionExperiment> filter, // Optional, default null
            @QueryParam("offset") @DefaultValue("0") OffsetArg offset, // Optional, default 0
            @QueryParam("limit") @DefaultValue("20") LimitArg limit, // Optional, default 20
            @QueryParam("sort") @DefaultValue("+id") SortArg<ExpressionExperiment> sort // Optional, default +id
    ) {
        return searchTaxonDatasets( taxonArg, query, filter, offset, limit, sort );
    }

    /**
     * Performs a dataset search for each given value, then intersects the results to create a final set of dataset IDs.
     *
     * @param values the values that the datasets should match.
     * @return set of IDs that satisfy all given search values.
     */
    private Collection<Long> searchEEs( List<String> values, @Nullable Taxon taxon ) throws SearchException {
        SearchSettings settings = SearchSettings.builder()
                .resultType( ExpressionExperiment.class )
                .fillResults( false )
                .taxon( taxon )
                .build();
        Set<Long> ids = new HashSet<>();
        for ( String value : values ) {
            List<SearchResult<ExpressionExperiment>> eeResults = searchService.search( settings.withQuery( value ) )
                    .getByResultObjectType( ExpressionExperiment.class );
            // Working only with IDs
            Set<Long> valueIds = new HashSet<>();
            for ( SearchResult<ExpressionExperiment> result : eeResults ) {
                valueIds.add( result.getResultId() );
            }
            // Intersecting with previous results
            if ( ids.isEmpty() ) {
                // In the first run we keep the whole list od IDs
                ids.addAll( valueIds );
            } else {
                // Intersecting with the IDs found in the current run
                ids.retainAll( valueIds );
            }
            // if one query is empty, then the intersection will be empty as well
            if ( ids.isEmpty() ) {
                break;
            }
        }
        return ids;
    }

    /**
     * Finds characteristics by either a plain text or URI.
     *
     * @param arg the array arg containing all the strings to search for.
     * @return a collection of characteristics matching the input query.
     */
    private LinkedHashSet<AnnotationSearchResultValueObject> getTerms( StringArrayArg arg, long timeoutMs ) throws SearchException {
        StopWatch timer = StopWatch.createStarted();
        LinkedHashSet<AnnotationSearchResultValueObject> vos = new LinkedHashSet<>();
        for ( String query : arg.getValue() ) {
            query = query.trim();
            URI uri = LuceneQueryUtils.prepareTermUriQuery( query );
            if ( uri != null ) {
                this.addAsSearchResults( vos, characteristicService.loadValueObjects( characteristicService
                        .findByUri( StringUtils.strip( query ) ) ) );
            } else {
                this.addAsSearchResults( vos, ontologyService.findExperimentsCharacteristicTags( query, 100, false, Math.max( timeoutMs - timer.getTime(), 0 ), TimeUnit.MILLISECONDS ) );
            }
        }
        return vos;
    }

    private void addAsSearchResults( Collection<AnnotationSearchResultValueObject> to,
            Collection<CharacteristicValueObject> vos ) {
        for ( CharacteristicValueObject vo : vos ) {
            to.add( new AnnotationSearchResultValueObject( vo.getValue(), vo.getValueUri(), vo.getCategory(),
                    vo.getCategoryUri() ) );
        }
    }

    @Value
    public static class AnnotationSearchResultValueObject {
        String value;
        String valueUri;
        String category;
        String categoryUri;
    }
}
