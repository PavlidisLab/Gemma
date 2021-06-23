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

package ubic.gemma.web.services.rest;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ubic.gemma.core.expression.experiment.service.ExpressionExperimentSearchService;
import ubic.gemma.core.ontology.OntologyService;
import ubic.gemma.core.search.SearchResult;
import ubic.gemma.core.search.SearchService;
import ubic.gemma.model.common.search.SearchSettings;
import ubic.gemma.model.common.search.SearchSettingsImpl;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.phenotype.valueObject.CharacteristicValueObject;
import ubic.gemma.persistence.service.common.description.CharacteristicService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;
import ubic.gemma.web.services.rest.util.Responder;
import ubic.gemma.web.services.rest.util.ResponseDataObject;
import ubic.gemma.web.services.rest.util.WebService;
import ubic.gemma.web.services.rest.util.WebServiceWithFiltering;
import ubic.gemma.web.services.rest.util.args.*;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.*;

/**
 * RESTful interface for annotations.
 *
 * @author tesarst
 */
@Component
@Path("/annotations")
public class AnnotationsWebService extends
        WebServiceWithFiltering<ExpressionExperiment, ExpressionExperimentValueObject, ExpressionExperimentService> {
    private static final String URL_PREFIX = "http://";

    private OntologyService ontologyService;
    private SearchService searchService;
    private CharacteristicService characteristicService;
    private ExpressionExperimentService expressionExperimentService;
    private TaxonService taxonService;

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
            TaxonService taxonService ) {
        super( expressionExperimentService );
        this.ontologyService = ontologyService;
        this.searchService = searchService;
        this.characteristicService = characteristicService;
        this.expressionExperimentService = expressionExperimentService;
        this.taxonService = taxonService;
    }

    /**
     * Placeholder for root call
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseDataObject all( // Params:
            @Context final HttpServletResponse sr // The servlet response, needed for response code setting.
    ) {
        return Responder.code404( WebService.ERROR_MSG_UNMAPPED_PATH, sr );
    }

    /**
     * Placeholder for search call without a query parameter
     */
    @GET
    @Path("/search/")
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseDataObject emptySearch( // Params:
            @Context final HttpServletResponse sr // The servlet response, needed for response code setting.
    ) {
        return Responder.code400( "Search query empty.", sr );
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
    @Path("/search/{query}")
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseDataObject search( // Params:
            @PathParam("query") ArrayStringArg query, // Required
            @Context final HttpServletResponse sr // The servlet response, needed for response code setting.
    ) {
        return Responder.autoCode( this.getTerms( query ), sr );
    }

    /**
     * Does a search for datasets containing characteristics matching the given string.
     * If filter, offset, limit or sort parameters are provided, acts same as
     * {@link WebServiceWithFiltering#some(ArrayEntityArg, FilterArg, IntArg, IntArg, SortArg, HttpServletResponse) }.
     *
     * @param query the search query. Either plain text, or an ontology term URI
     * @return response data object with a collection of dataset that match the search query.
     * @see ExpressionExperimentSearchService#searchExpressionExperiments(String) for better description of the search process.
     */
    @GET
    @Path("/search/{query}/datasets")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public ResponseDataObject datasets( // Params:
            @PathParam("query") ArrayStringArg query, // Required
            @QueryParam("filter") @DefaultValue("") DatasetFilterArg filter, // Optional, default null
            @QueryParam("offset") @DefaultValue("0") IntArg offset, // Optional, default 0
            @QueryParam("limit") @DefaultValue("0") IntArg limit, // Optional, default 0
            @QueryParam("sort") @DefaultValue("+id") SortArg sort, // Optional, default +id
            @Context final HttpServletResponse sr // The servlet response, needed for response code setting.
    ) {
        Collection<Long> foundIds = this.searchEEs( query.getValue() );

        if ( foundIds.isEmpty() ) {
            return Responder.autoCode( foundIds, sr );
        }

        // If there are filters other than the search query, intersect the results.
        if ( filter.getObjectFilters() != null || offset.getValue() != 0 || limit.getValue() != 0 || !sort.getField()
                .equals( "id" ) || !sort.isAsc() ) {
            // Converting list to string that will be parsed out again - not ideal, but is currently the best way to do
            // this without cluttering the code.
            return super
                    .some( ArrayDatasetArg.valueOf( StringUtils.join( foundIds, ',' ) ), filter, offset, limit, sort,
                            sr );
        }

        // Otherwise there is no need to go the pre-filter path since we already know exactly what IDs we want.
        return Responder.autoCode( expressionExperimentService.loadValueObjects( foundIds, false ), sr );
    }

    /**
     * Same as this#datasets(ArrayStringArg, DatasetFilterArg, IntArg, IntArg, SortArg, HttpServletResponse) but
     * also filters by taxon.
     * see this#datasets(ArrayStringArg, DatasetFilterArg, IntArg, IntArg, SortArg, HttpServletResponse).
     */
    @GET
    @Path("/{taxonArg: [a-zA-Z0-9%20\\.]+}/search/{query}/datasets")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public ResponseDataObject taxonDatasets( // Params:
            @PathParam("taxonArg") TaxonArg<Object> taxonArg, // Required
            @PathParam("query") ArrayStringArg query, // Required
            @QueryParam("filter") @DefaultValue("") DatasetFilterArg filter, // Optional, default null
            @QueryParam("offset") @DefaultValue("0") IntArg offset, // Optional, default 0
            @QueryParam("limit") @DefaultValue("0") IntArg limit, // Optional, default 0
            @QueryParam("sort") @DefaultValue("+id") SortArg sort, // Optional, default +id
            @Context final HttpServletResponse sr // The servlet response, needed for response code setting.
    ) {
        Collection<Long> foundIds = this.searchEEs( query.getValue() );

        if ( foundIds.isEmpty() ) {
            return Responder.autoCode( foundIds, sr );
        }

        // We always have to do filtering, because we always have at least the taxon argument (otherwise this#datasets method is used)
        return Responder.autoCode( taxonArg.getTaxonDatasets( expressionExperimentService, taxonService,
                ArrayDatasetArg.valueOf( StringUtils.join( foundIds, ',' ) )
                        .combineFilters( filter.getObjectFilters(), expressionExperimentService ), offset.getValue(),
                limit.getValue(), sort.getField(), sort.isAsc() ), sr );
    }

    /**
     * Performs a dataset search for each given value, then intersects the results to create a final set of dataset IDs.
     *
     * @param values the values that the datasets should match.
     * @return set of IDs that satisfy all given search values.
     */
    private Collection<Long> searchEEs( List<String> values ) {
        Set<Long> ids = new HashSet<>();
        boolean firstRun = true;
        for ( String value : values ) {
            Set<Long> valueIds = new HashSet<>();

            SearchSettings settings = SearchSettingsImpl.expressionExperimentSearch( value );

            Map<Class<?>, List<SearchResult>> results = searchService.search( settings, false, false );
            List<SearchResult> eeResults = results.get( ExpressionExperiment.class );

            if ( eeResults == null ) {
                return new HashSet<>(); // No terms found for the current term means the intersection will be empty.
            }

            // Working only with IDs
            for ( SearchResult result : eeResults ) {
                valueIds.add( result.getResultId() );
            }

            // Intersecting with previous results
            if ( firstRun ) {
                // In the first run we keep the whole list od IDs
                ids = valueIds;
            } else {
                // Intersecting with the IDs found in the current run
                ids.retainAll( valueIds );
            }
            firstRun = false;
        }
        return ids;
    }

    /**
     * Finds characteristics by either a plain text or URI.
     *
     * @param arg the array arg containing all the strings to search for.
     * @return a collection of characteristics matching the input query.
     */
    private Collection<AnnotationSearchResultValueObject> getTerms( ArrayStringArg arg ) {
        Collection<AnnotationSearchResultValueObject> vos = new LinkedList<>();
        for ( String query : arg.getValue() ) {
            query = query.trim();
            if ( query.startsWith( AnnotationsWebService.URL_PREFIX ) ) {
                this.addAsSearchResults( vos, characteristicService.loadValueObjects( characteristicService
                        .findByUri( StringEscapeUtils.escapeJava( StringUtils.strip( query ) ) ) ) );
            } else {
                this.addAsSearchResults( vos, ontologyService.findExperimentsCharacteristicTags( query, true ) );
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

    private static class AnnotationSearchResultValueObject {
        public String value;
        public String valueUri;
        public String category;
        public String categoryUri;

        AnnotationSearchResultValueObject( String value, String valueUri, String category, String categoryUri ) {
            this.value = value;
            this.valueUri = valueUri;
            this.category = category;
            this.categoryUri = categoryUri;
        }
    }
}
