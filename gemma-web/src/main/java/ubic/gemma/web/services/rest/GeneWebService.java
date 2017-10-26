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
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.persistence.service.expression.designElement.CompositeSequenceService;
import ubic.gemma.web.services.rest.util.Responder;
import ubic.gemma.web.services.rest.util.ResponseDataObject;
import ubic.gemma.web.services.rest.util.WebServiceWithFiltering;
import ubic.gemma.web.services.rest.util.args.*;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

/**
 * RESTful interface for genes.
 * Does not have an 'all' endpoint (no use-cases).
 * Most methods also have a taxon-specific counterpart in the {@link TaxaWebService} (useful when using the 'official
 * symbol' identifier, as this class will just return a random taxon homologue).
 *
 * @author tesarst
 */
@Component
@Path("/genes")
public class GeneWebService extends WebServiceWithFiltering<Gene, GeneValueObject, GeneService> {

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
        super( geneService );
        this.geneService = geneService;
        this.geneOntologyService = geneOntologyService;
        this.compositeSequenceService = compositeSequenceService;
        this.phenotypeAssociationManagerService = phenotypeAssociationManagerService;
    }

    /**
     * Placeholder for root call
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseDataObject all( // Params:
            @Context final HttpServletResponse sr // The servlet response, needed for response code setting.
    ) {
        // Calling valueOf with empty string to get the correct exception
        return Responder.autoCode( ArrayGeneArg.valueOf( null ).getValue(), sr );
    }

    /**
     * Retrieves all genes matching the identifier.
     *
     * @param genes a list of gene identifiers, separated by commas (','). Identifiers can be one of
     *              NCBI ID, Ensembl ID or official symbol. NCBI ID is the most efficient (and
     *              guaranteed to be unique) identifier. Official symbol returns a gene homologue on a random taxon.
     *              <p>
     *              Do not combine different identifiers in one query.
     *              </p>
     * @see WebServiceWithFiltering#some(ArrayEntityArg, FilterArg, IntArg, IntArg, SortArg, HttpServletResponse)
     */
    @GET
    @Path("/{genes: [a-zA-Z0-9\\.,%]+}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public ResponseDataObject genes( // Params:
            @PathParam("genes") ArrayGeneArg genes, // Required
            @Context final HttpServletResponse sr // The servlet response, needed for response code setting.
    ) {
        return super.some( genes, FilterArg.EMPTY_FILTER(), IntArg.valueOf( "0" ), IntArg.valueOf( "-1" ),
                SortArg.valueOf( "+id" ), sr );
    }

    /**
     * Retrieves gene evidence for the given gene.
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
     * Retrieves the probes (composite sequences) with this gene.
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
                .loadValueObjectsForGene( geneArg.getPersistentObject( geneService ), offset.getValue(),
                        limit.getValue() ), sr );
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
