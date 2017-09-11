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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ubic.gemma.core.ontology.OntologyService;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.phenotype.valueObject.CharacteristicValueObject;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;
import ubic.gemma.web.services.rest.util.Responder;
import ubic.gemma.web.services.rest.util.ResponseDataObject;
import ubic.gemma.web.services.rest.util.WebService;
import ubic.gemma.web.services.rest.util.args.TaxonArg;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

/**
 * RESTful interface for annotations.
 *
 * @author tesarst
 */
@Component
@Path("/annotations")
public class AnnotationsWebService extends WebService {

    private OntologyService ontologyService;
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
    public AnnotationsWebService( OntologyService ontologyService, TaxonService taxonService ) {
        this.ontologyService = ontologyService;
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
     * Does a search for ontology terms based on the given string.
     *
     * @param query    the search query.
     * @param taxonArg only limits the genes in the result set. Can be either null (to search all taxons), or Taxon ID or one of its string identifiers:
     *                 scientific name, common name, abbreviation. It is recommended to use the ID for efficiency.
     * @return response data object with a collection of found terms, each wrapped in a CharacteristicValueObject.
     * @see OntologyService#findTermsInexact(String, Taxon) for better description of the search process.
     * @see CharacteristicValueObject for the output object structure.
     */
    @GET
    @Path("/search/{query}")
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseDataObject search( // Params:
            @PathParam("query") String query, // Required
            @QueryParam("taxon") @DefaultValue("") TaxonArg<Object> taxonArg, // Optional, default null
            @Context final HttpServletResponse sr // The servlet response, needed for response code setting.
    ) {
        return Responder.autoCode( ontologyService.findTermsInexact( query,
                taxonArg.isNull() ? null : taxonArg.getPersistentObject( this.taxonService ) ), sr );
    }
}
