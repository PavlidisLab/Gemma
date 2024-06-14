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
package ubic.gemma.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import ubic.gemma.core.analysis.expression.coexpression.CoexpressionValueObjectExt;
import ubic.gemma.core.analysis.expression.coexpression.GeneCoexpressionSearchService;
import ubic.gemma.model.expression.designElement.CompositeSequenceValueObject;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.GeneOntologyTermValueObject;
import ubic.gemma.model.genome.PhysicalLocationValueObject;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.persistence.service.genome.gene.GeneService;
import ubic.gemma.persistence.service.maintenance.TableMaintenanceUtil;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.rest.util.PaginatedResponseDataObject;
import ubic.gemma.rest.util.ResponseDataObject;
import ubic.gemma.rest.util.args.*;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

import static ubic.gemma.rest.util.Responders.paginate;
import static ubic.gemma.rest.util.Responders.respond;

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

    @Autowired
    private GeneService geneService;
    @Autowired
    private GeneCoexpressionSearchService geneCoexpressionSearchService;
    @Autowired
    private GeneArgService geneArgService;
    @Autowired
    private TableMaintenanceUtil tableMaintenanceUtil;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve all genes")
    public PaginatedResponseDataObject<List<GeneValueObject>> getGenes(
            @QueryParam("offset") @DefaultValue("0") OffsetArg offsetArg,
            @QueryParam("limit") @DefaultValue("20") LimitArg limitArg
    ) {
        return paginate( geneArgService.getGenes( offsetArg.getValue(), limitArg.getValue() ), new String[] { "id" } );
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
    @Operation(summary = "Retrieve genes matching gene identifiers")
    public ResponseDataObject<List<GeneValueObject>> getGenesByIds( // Params:
            @PathParam("genes") GeneArrayArg genes // Required
    ) {
        SortArg<Gene> sort = SortArg.valueOf( "+id" );
        Filters filters = Filters.empty();
        filters.and( geneArgService.getFilters( genes ) );
        return respond( geneService.loadValueObjects( filters, geneArgService.getSort( sort ), 0, -1 ) );
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
        return respond( geneArgService.getGeneLocation( geneArg ) );
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
    public PaginatedResponseDataObject<List<CompositeSequenceValueObject>> getGeneProbes( // Params:
            @PathParam("gene") GeneArg<?> geneArg, // Required
            @QueryParam("offset") @DefaultValue("0") OffsetArg offset, // Optional, default 0
            @QueryParam("limit") @DefaultValue("20") LimitArg limit // Optional, default 20
    ) {
        return paginate( geneArgService.getGeneProbes( geneArg, offset.getValue(), limit.getValue() ), new String[] { "id" } );
    }

    /**
     * Refresh gene-to-probe associations.
     */
    @GET
    @Path("/probes/refresh")
    @Secured("GROUP_ADMIN")
    @Operation(summary = "Refresh gene-to-probe associations.",
            security = {
                    @SecurityRequirement(name = "basicAuth", scopes = { "GROUP_ADMIN" }),
                    @SecurityRequirement(name = "cookieAuth", scopes = { "GROUP_ADMIN" })
            },
            responses = {
                    // FIXME: this is broken, see https://github.com/swagger-api/swagger-core/issues/4693
                    @ApiResponse(responseCode = "204")
            })
    public Response refreshGenesProbes() {
        tableMaintenanceUtil.evictGene2CsQueryCache();
        return Response.noContent().build();
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
        return respond( geneArgService.getGeneGoTerms( geneArg ) );
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
    @Operation(summary = "Retrieve the coexpression of two given genes", hidden = true)
    public ResponseDataObject<List<CoexpressionValueObjectExt>> getGeneGeneCoexpression( // Params:
            @PathParam("gene") final GeneArg<?> geneArg, // Required
            @QueryParam("with") final GeneArg<?> with, // Required
            @QueryParam("limit") @DefaultValue("100") LimitArg limit, // Optional, default 100
            @QueryParam("stringency") @DefaultValue("1") Integer stringency // Optional, default 1
    ) {
        return respond( geneCoexpressionSearchService.coexpressionSearchQuick( null, new ArrayList<Long>( 2 ) {{
            this.add( geneArgService.getEntity( geneArg ).getId() );
            this.add( geneArgService.getEntity( with ).getId() );
        }}, 1, limit.getValueNoMaximum(), false ).getResults() );
    }
}
