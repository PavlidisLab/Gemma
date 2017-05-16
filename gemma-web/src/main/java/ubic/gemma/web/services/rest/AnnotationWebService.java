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
import ubic.gemma.core.genome.taxon.service.TaxonService;
import ubic.gemma.core.ontology.OntologyService;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.phenotype.valueObject.CharacteristicValueObject;
import ubic.gemma.web.services.rest.util.TaxonArg;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.Collection;

/**
 * RESTful interface to AnnotationController methods.
 *
 * @author paul
 */
@Component
@Path("/annotations")
public class AnnotationWebService {

    private OntologyService ontologyService;
    private TaxonService taxonService;

    public AnnotationWebService() {
    }

    @Autowired
    public AnnotationWebService( OntologyService ontologyService, TaxonService taxonService ) {
        this.ontologyService = ontologyService;
        this.taxonService = taxonService;
    }

    /* ********************************
     * API GET Methods
     * ********************************/

    /**
     * Does a search for ontology terms based on the given string.
     *
     * @param givenQueryString the search input query.
     * @param taxonArg         whether to limit the search to a specific taxon, can be either null (to search all taxons), or Taxon ID or one of its string identifiers:
     *                         scientific name, common name, abbreviation. Using the ID is most efficient.
     * @return a collection of found terms wrapped in a CharacteristicValueObject
     * @see OntologyService#findTermsInexact(String, Taxon) for better description of the search process.
     * @see CharacteristicValueObject for the output object structure.
     * FIXME are taxons attached to ANY terms at all? - possible redundant optional argument TaxonArg
     */
    @GET
    @Path("/terms/search/{query}")
    @Produces(MediaType.APPLICATION_JSON)
    public Collection<CharacteristicValueObject> searchTerms( @PathParam("query") String givenQueryString,
            @QueryParam("taxon") TaxonArg taxonArg ) {
        return ontologyService.findTermsInexact( givenQueryString, TaxonArg.getTaxon( taxonArg, this.taxonService ) );
    }

}
