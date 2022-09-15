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

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ubic.gemma.core.analysis.expression.coexpression.CoexpressionValueObjectExt;
import ubic.gemma.core.analysis.expression.coexpression.GeneCoexpressionSearchService;
import ubic.gemma.core.association.phenotype.PhenotypeAssociationManagerService;
import ubic.gemma.core.genome.gene.service.GeneService;
import ubic.gemma.core.ontology.providers.GeneOntologyService;
import ubic.gemma.core.search.SearchException;
import ubic.gemma.model.expression.designElement.CompositeSequenceValueObject;
import ubic.gemma.model.genome.GeneOntologyTermValueObject;
import ubic.gemma.model.genome.PhysicalLocationValueObject;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.GeneEvidenceValueObject;
import ubic.gemma.persistence.service.expression.designElement.CompositeSequenceService;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.web.services.rest.util.ArgUtils;
import ubic.gemma.web.services.rest.util.PaginatedResponseDataObject;
import ubic.gemma.web.services.rest.util.Responder;
import ubic.gemma.web.services.rest.util.ResponseDataObject;
import ubic.gemma.web.services.rest.util.args.*;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.List;

/**
 * RESTful interface for genes.
 * Does not have an 'all' endpoint (no use-cases).
 * Most methods also have a taxon-specific counterpart in the {@link TaxaWebService} (useful when using the 'official
 * symbol' identifier, as this class will just return a random taxon homologue).
 *
 * @author tesarst
 */
@Service
@Path("/genes")
public class GeneWebService {

    private GeneService geneService;
    private GeneOntologyService geneOntologyService;
    private CompositeSequenceService compositeSequenceService;
    private PhenotypeAssociationManagerService phenotypeAssociationManagerService;
    private GeneCoexpressionSearchService geneCoexpressionSearchService;

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
            PhenotypeAssociationManagerService phenotypeAssociationManagerService,
            GeneCoexpressionSearchService geneCoexpressionSearchService ) {
        this.geneService = geneService;
        this.geneOntologyService = geneOntologyService;
        this.compositeSequenceService = compositeSequenceService;
        this.phenotypeAssociationManagerService = phenotypeAssociationManagerService;
        this.geneCoexpressionSearchService = geneCoexpressionSearchService;
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
     */
    @GET
    @Path("/{genes}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve genes matching a gene identifier")
    public ResponseDataObject<List<GeneValueObject>> getGenes( // Params:
            @PathParam("genes") GeneArrayArg genes // Required
    ) {
        SortArg sort = SortArg.valueOf( "+id" );
        Filters filters = new Filters();
        filters.add( genes.getObjectFilters( geneService ) );
        return Responder.respond( geneService.loadValueObjectsPreFilter( filters, sort.getSort( geneService ), IntArg.valueOf( "0" ).getValue(), IntArg.valueOf( "-1" ).getValue() ) );
    }

    /**
     * Retrieves gene evidence for the given gene.
     *
     * @param geneArg can either be the NCBI ID, Ensembl ID or official symbol. NCBI ID is most efficient (and
     *                guaranteed to be unique). Official symbol returns a gene homologue on a random taxon.
     */
    @GET
    @Path("/{gene}/evidence")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve the evidence for a given gene", hidden = true)
    public ResponseDataObject<List<GeneEvidenceValueObject>> getGeneEvidence( // Params:
            @PathParam("gene") GeneArg<?> geneArg // Required
    ) {
        try {
            return Responder
                    .respond( geneArg.getGeneEvidence( geneService, phenotypeAssociationManagerService, null ) );
        } catch ( SearchException e ) {
            throw new BadRequestException( "Invalid search settings.", e );
        }
    }

    /**
     * Retrieves the physical location of the given gene.
     *
     * @param geneArg can either be the NCBI ID, Ensembl ID or official symbol. NCBI ID is most efficient (and
     *                guaranteed to be unique). Official symbol returns a gene homologue on a random taxon.
     */
    @GET
    @Path("/{gene}/locations")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve the physical locations of a given gene")
    public ResponseDataObject<List<PhysicalLocationValueObject>> getGeneLocations( // Params:
            @PathParam("gene") GeneArg<?> geneArg // Required
    ) {
        return Responder.respond( geneArg.getGeneLocation( geneService ) );
    }

    /**
     * Retrieves the probes (composite sequences) with this gene.
     *
     * @param geneArg can either be the NCBI ID, Ensembl ID or official symbol. NCBI ID is most efficient (and
     *                guaranteed to be unique). Official symbol returns a gene homologue on a random taxon.
     */
    @GET
    @Path("/{gene}/probes")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve the probes associated to a genes across all platforms")
    public PaginatedResponseDataObject<CompositeSequenceValueObject> getGeneProbes( // Params:
            @PathParam("gene") GeneArg<?> geneArg, // Required
            @QueryParam("offset") @DefaultValue("0") OffsetArg offset, // Optional, default 0
            @QueryParam("limit") @DefaultValue("20") LimitArg limit // Optional, default 20
    ) {
        return Responder.paginate( compositeSequenceService
                .loadValueObjectsForGene( geneArg.getEntity( geneService ), offset.getValue(),
                        limit.getValue() ) );
    }

    /**
     * Retrieves the GO terms of the given gene.
     *
     * @param geneArg can either be the NCBI ID, Ensembl ID or official symbol. NCBI ID is most efficient (and
     *                guaranteed to be unique). Official symbol returns a gene homologue on a random taxon.
     */
    @GET
    @Path("/{gene}/goTerms")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve the GO terms associated to a gene")
    public ResponseDataObject<List<GeneOntologyTermValueObject>> getGeneGoTerms( // Params:
            @PathParam("gene") GeneArg<?> geneArg // Required
    ) {
        return Responder.respond( geneArg.getGoTerms( geneService, geneOntologyService ) );
    }

    /**
     * Retrieves the coexpression of two given genes.
     *
     * @param geneArg    can either be the NCBI ID, Ensembl ID or official symbol. NCBI ID is most efficient (and
     *                   guaranteed to be unique). Official symbol returns a gene homologue on a random taxon.
     * @param with       the gene to calculate the coexpression with. Same formatting rules as with the 'geneArg' apply.
     * @param stringency optional parameter controlling the stringency of coexpression search. Defaults to 1.
     */
    @GET
    @Path("/{gene}/coexpression")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve the coexpression of two given genes")
    public ResponseDataObject<List<CoexpressionValueObjectExt>> getGeneGeneCoexpression( // Params:
            @PathParam("gene") final GeneArg<?> geneArg, // Required
            @QueryParam("with") final GeneArg<?> with, // Required
            @QueryParam("limit") @DefaultValue("100") LimitArg limit, // Optional, default 100
            @QueryParam("stringency") @DefaultValue("1") IntArg stringency // Optional, default 1
    ) {
        ArgUtils.requiredArg( with, "with" );
        return Responder
                .respond( geneCoexpressionSearchService.coexpressionSearchQuick( null, new ArrayList<Long>( 2 ) {{
                    this.add( geneArg.getEntity( geneService ).getId() );
                    this.add( with.getEntity( geneService ).getId() );
                }}, 1, limit.getValueNoMaximum(), false ).getResults() );
    }

}
