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
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ubic.basecode.ontology.model.OntologyTerm;
import ubic.gemma.association.phenotype.PhenotypeAssociationManagerService;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.model.genome.gene.phenotype.EvidenceFilter;
import ubic.gemma.model.genome.gene.phenotype.valueObject.EvidenceValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.GeneEvidenceValueObject;
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
     * Given a set of phenotypes, return all genes asociated with them.
     * 
     * @param taxonId
     * @param showOnlyEditable
     * @param phenotypeValueUris
     * @return
     */
    @GET
    @Path("/find-candidate-genes")
    @Produces(MediaType.APPLICATION_JSON)
    public Collection<GeneEvidenceValueObject> findCandidateGenes( @QueryParam("taxonId") Long taxonId,
            @QueryParam("showOnlyEditable") boolean showOnlyEditable,
            @QueryParam("phenotypeValueUris") List<String> phenotypeValueUris ) {
        return this.phenotypeAssociationManagerService.findCandidateGenes( new EvidenceFilter( taxonId,
                showOnlyEditable ), new HashSet<String>( phenotypeValueUris ) );
    }

    /**
     * @param taxonId
     * @param showOnlyEditable
     * @param geneId
     * @param phenotypeValueUris
     * @return
     */
    @GET
    @Path("/find-evidence")
    @Produces(MediaType.APPLICATION_JSON)
    public Collection<EvidenceValueObject> findEvidence( @QueryParam("taxonId") Long taxonId,
            @QueryParam("showOnlyEditable") boolean showOnlyEditable, @QueryParam("geneId") Long geneId,
            @QueryParam("phenotypeValueUris") List<String> phenotypeValueUris ) {
        return this.phenotypeAssociationManagerService.findEvidenceByGeneId( geneId, new HashSet<String>(
                phenotypeValueUris ), new EvidenceFilter( taxonId, showOnlyEditable ) );
    }

    /**
     * Given a phenotype, return all genes associated with it, denoting the exact phenotype that was annotated, which
     * might be a child of the requested term.
     * 
     * @param taxonId
     * @param phenotypeValueUri
     * @return genes, where the associated CharacteristicValueObjects is a collection with one member representing the
     *         specific phenotype (the input query, or a child term).
     */
    @GET
    @Path("/find-phenotype-genes")
    @Produces(MediaType.APPLICATION_JSON)
    public JSONArray findPhenotypeGenes( @QueryParam("taxonId") Long taxonId,
            @QueryParam("phenotypeValueUri") String phenotypeValueUri ) {

        Map<GeneValueObject, OntologyTerm> results = this.phenotypeAssociationManagerService.findGenesForPhenotype(
                phenotypeValueUri, taxonId, false );

        JSONArray answer = new JSONArray();
        for ( GeneValueObject r : results.keySet() ) {
            String symbol = r.getOfficialSymbol();
            OntologyTerm term = results.get( r );

            answer.add( ( new JSONObject() ).put( symbol, new JSONObject().put( term.getUri(), term ) ) );
        }

        return answer;
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