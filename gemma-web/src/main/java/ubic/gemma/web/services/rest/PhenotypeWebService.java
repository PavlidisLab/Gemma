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

import java.util.Collection;
import java.util.HashSet;
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
import ubic.gemma.model.genome.gene.phenotype.valueObject.EvidenceValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.TreeCharacteristicValueObject;

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
    @Path("/load-all-phenotypes")
    @Produces(MediaType.APPLICATION_JSON)
    public Collection<TreeCharacteristicValueObject> loadAllPhenotypes() {
    	return phenotypeAssociationManagerService.loadAllPhenotypesByTree();
    }

    @GET
    @Path("/find-candidate-genes")
    @Produces(MediaType.APPLICATION_JSON)
    public Collection<GeneValueObject> findCandidateGenes(
            @QueryParam("phenotypeValueUris") List<String> phenotypeValueUris ) {
        return phenotypeAssociationManagerService.findCandidateGenes(new HashSet<String>(phenotypeValueUris), null);
    }

    @GET
    @Path("/find-evidence")
    @Produces(MediaType.APPLICATION_JSON)
    public Collection<EvidenceValueObject> findEvidence( @QueryParam("geneId") Long geneId,
            @QueryParam("phenotypeValueUris") List<String> phenotypeValueUris ) {
        return this.phenotypeAssociationManagerService.findEvidenceByGeneId( geneId, new HashSet<String>(
                phenotypeValueUris ) );
    }
}