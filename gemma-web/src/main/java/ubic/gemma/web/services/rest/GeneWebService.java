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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ubic.gemma.core.association.phenotype.PhenotypeAssociationManagerService;
import ubic.gemma.core.genome.gene.service.GeneService;
import ubic.gemma.core.ontology.providers.GeneOntologyService;
import ubic.gemma.persistence.service.expression.designElement.CompositeSequenceService;
import ubic.gemma.web.services.rest.util.Responder;
import ubic.gemma.web.services.rest.util.ResponseDataObject;
import ubic.gemma.web.services.rest.util.WebService;
import ubic.gemma.web.services.rest.util.args.GeneArg;
import ubic.gemma.web.services.rest.util.args.IntArg;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

/**
 * RESTful web services for genes. Does not have 'all' endpoint (no use-cases).
 *
 * @author tesarst
 */
@Component
@Path("/genes")
public class GeneWebService extends WebService {

    private GeneService geneService;
    private GeneOntologyService geneOntologyService;
    private CompositeSequenceService compositeSequenceService;
    private PhenotypeAssociationManagerService phenotypeAssociationManagerService;

    /**
     * Required by spring
     */
    public GeneWebService() {
    }

    /**
     * Constructor for service autowiring
     */
    @Autowired
    public GeneWebService( GeneService geneService, GeneOntologyService geneOntologyService,
            CompositeSequenceService compositeSequenceService,
            PhenotypeAssociationManagerService phenotypeAssociationManagerService ) {
        this.geneService = geneService;
        this.geneOntologyService = geneOntologyService;
        this.compositeSequenceService = compositeSequenceService;
        this.phenotypeAssociationManagerService = phenotypeAssociationManagerService;
    }

    /**
     * Retrieves all genes matching the identifier.
     *
     * @param geneArg can either be the NCBI ID, Ensembl ID or official symbol. NCBI ID is most efficient (and
     *                guaranteed to be unique). Official symbol returns a gene homologue on a random taxon.
     */
    @GET
    @Path("/{geneArg: [a-zA-Z0-9\\.]+}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public ResponseDataObject genes( // Params:
            @PathParam("geneArg") GeneArg<Object> geneArg, // Required
            @Context final HttpServletResponse sr // The servlet response, needed for response code setting.
    ) {
        return Responder.autoCode( geneArg.getValueObjects( geneService ), sr );
    }

    /**
     * Retrieves gene evidence matching the gene identifier
     *
     * @param geneArg can either be the NCBI ID, Ensembl ID or official symbol. NCBI ID is most efficient (and
     *                guaranteed to be unique). Official symbol returns a gene homologue on a random taxon.
     */
    @GET
    @Path("/{geneArg: [a-zA-Z0-9\\.]+}/evidence")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public ResponseDataObject geneEvidence( // Params:
            @PathParam("geneArg") GeneArg<Object> geneArg, // Required
            @Context final HttpServletResponse sr // The servlet response, needed for response code setting.
    ) {
        return Responder
                .autoCode( geneArg.getGeneEvidence( geneService, phenotypeAssociationManagerService, null ), sr );
    }

    /**
     * Retrieves the physical location of the given gene.
     *
     * @param geneArg can either be the NCBI ID, Ensembl ID or official symbol. NCBI ID is most efficient (and
     *                guaranteed to be unique). Official symbol returns a gene homologue on a random taxon.
     */
    @GET
    @Path("/{geneArg: [a-zA-Z0-9\\.]+}/locations")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public ResponseDataObject geneLocations( // Params:
            @PathParam("geneArg") GeneArg<Object> geneArg, // Required
            @Context final HttpServletResponse sr // The servlet response, needed for response code setting.
    ) {
        return Responder.autoCode( geneArg.getGeneLocation( geneService ), sr );
    }

    /**
     * Retrieves the composite sequences (probes) with this gene.
     *
     * @param geneArg can either be the NCBI ID, Ensembl ID or official symbol. NCBI ID is most efficient (and
     *                guaranteed to be unique). Official symbol returns a gene homologue on a random taxon.
     */
    @GET
    @Path("/{geneArg: [a-zA-Z0-9\\.]+}/probes")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public ResponseDataObject geneProbes( // Params:
            @PathParam("geneArg") GeneArg<Object> geneArg, // Required
            @QueryParam("offset") @DefaultValue("0") IntArg offset, // Optional, default 0
            @QueryParam("limit") @DefaultValue("20") IntArg limit, // Optional, default 20
            @Context final HttpServletResponse sr // The servlet response, needed for response code setting.
    ) {
        return Responder.autoCode( compositeSequenceService
                .loadValueObjectsForGene( geneArg.getPersistentObject( geneService ), offset.getValue(), limit.getValue() ), sr );
    }

    /**
     * Retrieves the GO terms of the given gene.
     *
     * @param geneArg can either be the NCBI ID, Ensembl ID or official symbol. NCBI ID is most efficient (and
     *                guaranteed to be unique). Official symbol returns a gene homologue on a random taxon.
     */
    @GET
    @Path("/{geneArg: [a-zA-Z0-9\\.]+}/goTerms")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public ResponseDataObject genesGoTerms( // Params:
            @PathParam("geneArg") GeneArg<Object> geneArg, // Required
            @Context final HttpServletResponse sr // The servlet response, needed for response code setting.
    ) {
        return Responder.autoCode( geneArg.getGoTerms( geneService, geneOntologyService ), sr );
    }

}