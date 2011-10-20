/*
 * The Gemma project
 * 
 * Copyright (c) 2011 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.web.services.rest;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ubic.gemma.association.phenotype.PhenotypeAssociationManagerService;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.CharacteristicValueObject;
import ubic.gemma.web.remote.JsonReaderResponse;

/**
 * RESTful web services for phenotypes 
 * 
 * @author frances
 * @version $Id$
*/

@Component
@Path("/phenotype")
public class PhenotypeWebService {
	
    @Autowired
    private PhenotypeAssociationManagerService phenotypeAssociationManagerService;

    @GET
    @Path("/find-all-phenotypes")
	@Produces(MediaType.APPLICATION_JSON)
    public JsonReaderResponse<CharacteristicValueObject> findAllPhenotypes() {	
    	return new JsonReaderResponse<CharacteristicValueObject>(
    			new ArrayList<CharacteristicValueObject>(phenotypeAssociationManagerService.loadAllPhenotypes()));
    }

    @GET
    @Path("/find-candidate-genes")
	@Produces(MediaType.APPLICATION_JSON)
    public JsonReaderResponse<GeneValueObject> findCandidateGenes(@QueryParam("phenotypeValue") List<String> phenotypeValues) {
    	return new JsonReaderResponse<GeneValueObject>(
    			new ArrayList<GeneValueObject>(phenotypeAssociationManagerService.findCandidateGenes(phenotypeValues.toArray(new String[0]))));
    }
}