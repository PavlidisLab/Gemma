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
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.phenotype.valueObject.CharacteristicValueObject;
import ubic.gemma.persistence.service.common.description.CharacteristicService;
import ubic.gemma.web.services.rest.util.Responder;
import ubic.gemma.web.services.rest.util.ResponseDataObject;
import ubic.gemma.web.services.rest.util.WebService;
import ubic.gemma.web.services.rest.util.args.ArrayStringArg;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.Collection;
import java.util.LinkedList;

/**
 * RESTful interface for annotations.
 *
 * @author tesarst
 */
@Component
@Path("/annotations")
public class AnnotationsWebService extends WebService {
    private static final String URL_PREFIX = "http://";

    private OntologyService ontologyService;
    private ExpressionExperimentSearchService expressionExperimentSearchService;
    private CharacteristicService characteristicService;

    /**
     * Required by spring
     */
    public AnnotationsWebService() {
    }

    /**
     * Constructor for service autowiring
     */
    @Autowired
    public AnnotationsWebService( OntologyService ontologyService,
            ExpressionExperimentSearchService expressionExperimentSearchService,
            CharacteristicService characteristicService ) {
        this.ontologyService = ontologyService;
        this.expressionExperimentSearchService = expressionExperimentSearchService;
        this.characteristicService = characteristicService;
    }

    /**
     * Placeholder for root call
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseDataObject all( // Params:
            @Context final HttpServletResponse sr // The servlet response, needed for response code setting.
    ) {
        return Responder.code404( ERROR_MSG_UNMAPPED_PATH, sr );
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
        return Responder.autoCode( getTerms( query ), sr );
    }

    /**
     * Does a search for datasets containing characteristics matching the given string.
     *
     * @param query the search query. Either plain text, or an ontology term URI
     * @return response data object with a collection of found terms, each wrapped in a CharacteristicValueObject.
     * @see ExpressionExperimentSearchService#searchExpressionExperiments(String) for better description of the search process.
     */
    @GET
    @Path("/search/{query}/datasets")
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseDataObject datasets( // Params:
            @PathParam("query") ArrayStringArg query, // Required
            @Context final HttpServletResponse sr // The servlet response, needed for response code setting.
    ) {
        return Responder
                .autoCode( expressionExperimentSearchService.searchExpressionExperiments( query.getValue() ), sr );
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
            if ( query.startsWith( URL_PREFIX ) ) {
                addAsSearchResults( vos, characteristicService.loadValueObjects( characteristicService
                        .findByUri( StringEscapeUtils.escapeJava( StringUtils.strip( query ) ) ) ) );
            } else {
                addAsSearchResults( vos, ontologyService.findExperimentsCharacteristicTags( query, true ) );
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

    private class AnnotationSearchResultValueObject {
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
