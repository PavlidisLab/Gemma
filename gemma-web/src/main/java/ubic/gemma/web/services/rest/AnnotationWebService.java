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

import java.util.Collection;
import java.util.HashSet;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ubic.gemma.genome.taxon.service.TaxonService;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.phenotype.valueObject.CharacteristicValueObject;
import ubic.gemma.ontology.OntologyService;

/**
 * RESTful interface to AnnotationController methods.
 * 
 * @author paul
 * @version $Id$
 */
@Component
@Path("/annot")
public class AnnotationWebService {

    @Autowired
    private OntologyService ontologyService;
    @Autowired
    private TaxonService taxonService;

    /**
     * This is the same as the controller method, but can't delegate to it. I could have annotated the Controller
     * version, but currently all web services have to be under the services/rest package. This can be changed (one
     * cost: slightly slower system startup).
     * 
     * @param givenQueryString
     * @param taxonId can be null
     * @return
     */
    @GET
    @Path("/findTerm")
    @Produces(MediaType.APPLICATION_JSON)
    public Collection<CharacteristicValueObject> findTerm( @QueryParam("query") String givenQueryString,
            @QueryParam("taxonId") Long taxonId ) {
        if ( StringUtils.isBlank( givenQueryString ) ) {
            return new HashSet<CharacteristicValueObject>();
        }
        Taxon taxon = null;
        if ( taxonId != null ) {
            taxon = taxonService.load( taxonId );
        }
        return ontologyService.findTermsInexact( givenQueryString, taxon );
    }
}
