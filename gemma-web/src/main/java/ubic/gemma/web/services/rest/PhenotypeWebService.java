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
import ubic.gemma.model.genome.gene.phenotype.EvidenceFilter;
import ubic.gemma.model.genome.gene.phenotype.valueObject.EvidenceValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.PhenotypeValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.SimpleTreeValueObject;

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

    /**
     * Given a set of phenotypes, return all genes asociated with them. Not sure how useful this is since there is no
     * information about which phenotypes go with with genes, and apparently the child terms are not included?
     * 
     * @param taxonId
     * @param showOnlyEditable
     * @param phenotypeValueUris
     * @return
     * @deprecated unless this can be shown to be useful.
     */
    @GET
    @Path("/find-candidate-genes")
    @Produces(MediaType.APPLICATION_JSON)
    @Deprecated
    public Collection<GeneValueObject> findCandidateGenes( @QueryParam("taxonId") Long taxonId,
            @QueryParam("showOnlyEditable") boolean showOnlyEditable,
            @QueryParam("phenotypeValueUris") List<String> phenotypeValueUris ) {
        return this.phenotypeAssociationManagerService.findCandidateGenes( new EvidenceFilter( taxonId,
                showOnlyEditable ), new HashSet<String>( phenotypeValueUris ) );
    }

    /**
     * Given a phenotype, return all genes associated with it, including child terms.
     * 
     * @param taxonId
     * @param phenotypeValueUri
     * @return
     */
    @GET
    @Path("/find-phenotype-genes")
    @Produces(MediaType.APPLICATION_JSON)
    public Collection<GeneValueObject> findPhenotypeGenes( @QueryParam("taxonId") Long taxonId,
            @QueryParam("phenotypeValueUri") String phenotypeValueUri ) {
        return this.phenotypeAssociationManagerService.findCandidateGenes( phenotypeValueUri, taxonId );
    }

    @GET
    @Path("/find-evidence")
    @Produces(MediaType.APPLICATION_JSON)
    public Collection<EvidenceValueObject> findEvidence( @QueryParam("taxonId") Long taxonId,
            @QueryParam("showOnlyEditable") boolean showOnlyEditable, @QueryParam("geneId") Long geneId,
            @QueryParam("phenotypeValueUris") List<String> phenotypeValueUris ) {
        return this.phenotypeAssociationManagerService.findEvidenceByGeneId( geneId, new HashSet<String>(
                phenotypeValueUris ), new EvidenceFilter( taxonId, showOnlyEditable ) );
    }

    @GET
    @Path("/load-all-neurocarta-phenotypes")
    @Produces(MediaType.APPLICATION_JSON)
    public Collection<PhenotypeValueObject> loadAllNeurocartaPhenotypes() {
        return this.phenotypeAssociationManagerService.loadAllNeurocartaPhenotypes();
    }

    @GET
    @Path("/load-all-phenotypes")
    @Produces(MediaType.APPLICATION_JSON)
    public Collection<SimpleTreeValueObject> loadAllPhenotypesByTree( @QueryParam("taxonId") Long taxonId,
            @QueryParam("showOnlyEditable") boolean showOnlyEditable ) {
        return this.phenotypeAssociationManagerService.loadAllPhenotypesByTree( new EvidenceFilter( taxonId,
                showOnlyEditable ) );
    }
}