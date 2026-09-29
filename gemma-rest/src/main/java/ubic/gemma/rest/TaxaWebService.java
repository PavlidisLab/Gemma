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
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ubic.gemma.model.expression.designElement.CompositeSequenceValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.genome.GeneOntologyTermValueObject;
import ubic.gemma.model.genome.PhysicalLocationValueObject;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonValueObject;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.genome.gene.GeneService;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;
import ubic.gemma.persistence.util.CursorPage;
import ubic.gemma.persistence.util.Filter;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Slice;
import ubic.gemma.persistence.util.Sort;
import ubic.gemma.rest.util.CursorPaginatedResponseDataObject;
import ubic.gemma.rest.util.FilteredAndCursorPaginatedResponseDataObject;
import ubic.gemma.rest.util.FilteredAndPaginatedResponseDataObject;
import ubic.gemma.rest.util.OpenApiResponseTypes.*;
import ubic.gemma.rest.util.PaginatedResponseDataObject;
import ubic.gemma.rest.util.ResponseDataObject;
import ubic.gemma.rest.util.args.*;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

import static ubic.gemma.rest.util.Responders.paginate;
import static ubic.gemma.rest.util.Responders.paginateByCursor;
import static ubic.gemma.rest.util.Responders.respond;

/**
 * RESTful interface for taxa.
 *
 * @author tesarst
 */
@Service
@Path("/taxa")
@Tag(name = "Taxa", description = "Taxa, and the genes and datasets scoped to one")
public class TaxaWebService {

    protected static final Log log = LogFactory.getLog( TaxaWebService.class.getName() );

    private final TaxonService taxonService;
    private final ExpressionExperimentService expressionExperimentService;
    private final TaxonArgService taxonArgService;
    private final DatasetArgService datasetArgService;
    private final GeneArgService geneArgService;
    private final GeneService geneService;

    /**
     * Constructor for service autowiring
     */
    @Autowired
    public TaxaWebService( TaxonService taxonService, ExpressionExperimentService expressionExperimentService, TaxonArgService taxonArgService, DatasetArgService datasetArgService, GeneArgService geneArgService, GeneService geneService ) {
        this.taxonService = taxonService;
        this.expressionExperimentService = expressionExperimentService;
        this.taxonArgService = taxonArgService;
        this.datasetArgService = datasetArgService;
        this.geneArgService = geneArgService;
        this.geneService = geneService;
    }

    /**
     * Lists all available taxa. Does not offer any advanced filtering or sorting functionality.
     * The reason for this is that Taxa are a relatively small set of objects that rarely change.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve all available taxa",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Every taxon Gemma knows about.", useReturnTypeSchema = true, content = @Content())
            })
    public ResponseDataObject<List<TaxonValueObject>> getTaxa() {
        return respond( taxonService.loadAllValueObjects() );
    }

    /**
     * Retrieves single taxon based on the given identifier.
     *
     * @param taxaArg a list of identifiers, separated by commas (','). Identifiers can be the any of
     *                taxon ID, scientific name, common name. It is recommended to use ID for efficiency.
     *                <p>
     *                Only datasets that user has access to will be available.
     *                </p>
     *                <p>
     *                Do not combine different identifiers in one query.
     *                </p>
     */
    @GET
    @Path("/{taxa}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve taxa by their identifiers",
            responses = {
                    @ApiResponse(responseCode = "200", description = "The taxa the identifiers resolved to.", useReturnTypeSchema = true, content = @Content())
            })
    public ResponseDataObject<List<TaxonValueObject>> getTaxaByIds( @Parameter(description = "Taxon identifiers, comma-separated.") @PathParam("taxa") TaxonArrayArg taxaArg ) {
        Filters filters = taxonArgService.getFilters( taxaArg );
        Sort sort = taxonService.getSort( "id", null, Sort.NullMode.LAST );
        return respond( taxonService.loadValueObjects( filters, sort ) );
    }

    /**
     * Finds genes overlapping a given region.
     *
     * @param taxonArg       can either be Taxon ID or one of its string identifiers:
     *                       scientific name, common name. It is recommended to use the ID for efficiency.
     * @param chromosomeName - eg: 3, 21, X
     * @param strand         - '+' or '-'. Defaults to '+'. (WIP, currently does not do anything).
     * @param start          - start of the region (nucleotide position).
     * @param size           - size of the region (in nucleotides).
     * @return GeneValue objects of the genes in the region.
     */
    @GET
    @Path("/{taxon}/chromosomes/{chromosome}/genes")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve genes overlapping a given region in a taxon",
            responses = {
                    @ApiResponse(responseCode = "200", description = "The genes overlapping the requested region.", useReturnTypeSchema = true, content = @Content())
            })
    public ResponseDataObject<List<GeneValueObject>> getTaxonGenesOverlappingChromosome( // Params:
            @Parameter(description = "Taxon identifier: its id, or its scientific or common name. The id is unambiguous.") @PathParam("taxon") TaxonArg<?> taxonArg, // Required
            @Parameter(description = "Chromosome name, as the assembly spells it (for example `X` or `11`).") @PathParam("chromosome") String chromosomeName, // Required
            @Parameter(description = "Restrict to genes on this strand, `+` or `-`. Omit for both.") @QueryParam("strand") String strand, //Optional, default +
            @Parameter(description = "Start of the region, in base pairs.", required = true) @QueryParam("start") Long start, // Required
            @Parameter(description = "Length of the region, in base pairs.", required = true) @QueryParam("size") Integer size // Required
    ) {
        if ( start == null ) {
            throw new BadRequestException( "The 'start' query parameter must be supplied." );
        }
        if ( size == null ) {
            throw new BadRequestException( "The 'size' query parameter must be supplied." );
        }
        if ( strand != null && !( strand.equals( "+" ) || strand.equals( "-" ) ) ) {
            throw new BadRequestException( "The 'strand' query parameter must be either '+', '-' or left unspecified." );
        }
        List<GeneValueObject> vos = taxonArgService.getGenesOnChromosome( taxonArg, chromosomeName, strand, start, size );
        geneService.populateAssociatedExperimentCount( vos );
        return respond( vos );
    }

    @GET
    @Path("/{taxon}/genes")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve all genes in a given taxon",
            description = "Supports two pagination modes. Legacy mode: pass `offset` (and `limit`); response includes `offset` and `totalElements`. "
                    + "Cursor mode (recommended for deep pagination and consistency under writes): pass an opaque `cursor` token from a previous response's `nextCursor` / `prevCursor` field. "
                    + "`offset` and `cursor` are mutually exclusive — passing a non-null `cursor` selects cursor mode. "
                    + "In cursor mode the result is always sorted by ascending `id` (cursor mode forces a single-component id sort pending the indexed-column audit in phase B); the `taxon.id = ?` constraint is preserved; `totalElements` is `null` by default (no count query per request).",
            responses = {
                    @ApiResponse(responseCode = "200", description = "The taxon's genes, in whichever pagination envelope the request selected.",
                            content = @Content(schema = @Schema(oneOf = {
                                    PaginatedResponseDataObjectGeneValueObject.class,
                                    CursorPaginatedResponseDataObjectGeneValueObject.class
                            }))),
            })
    public Object getTaxonGenes(
            @Parameter(description = "Taxon identifier: its id, or its scientific or common name. The id is unambiguous.") @PathParam("taxon") TaxonArg<?> taxonArg,
            @Parameter(description = "How many results to skip before the page begins. Mutually exclusive with `cursor`.") @QueryParam("offset") @DefaultValue("0") OffsetArg offsetArg,
            @Parameter(description = "Maximum number of results to return.") @QueryParam("limit") @DefaultValue("20") LimitArg limitArg,
            @Parameter(description = "Opaque keyset-pagination cursor token; mutually exclusive with `offset`.") @QueryParam("cursor") CursorArg cursorArg
    ) {
        Taxon taxon = taxonArgService.getEntity( taxonArg );
        if ( cursorArg != null ) {
            // Mutual-exclusion: a non-null cursor selects cursor mode. The default offset=0 is
            // not considered user-supplied (parallels GET /platforms/{platform}/datasets step 1f).
            // In cursor mode we currently force a +id sort (GeneArgService.getGenesInTaxonByCursor)
            // — the DAO restricts cursors to single-component id sorts until the index audit lands.
            // The path-derived taxon.id filter is composed into the Filters inside
            // getGenesInTaxonByCursor so the taxon scope is enforced identically in both modes.
            CursorPage<GeneValueObject> page = geneArgService.getGenesInTaxonByCursor(
                    taxon, cursorArg.getValue(), limitArg.getValue() );
            geneService.populateAssociatedExperimentCount( page );
            return paginateByCursor( page, new String[] { "id" } );
        }
        Slice<GeneValueObject> slice = geneArgService.getGenesInTaxon( taxon, offsetArg.getValue(), limitArg.getValue() );
        geneService.populateAssociatedExperimentCount( slice );
        return paginate( slice, new String[] { "id" } );
    }

    /**
     * Retrieves genes matching the identifier on the given taxon.
     *
     * @param taxonArg can either be Taxon ID or one of its string identifiers:
     *                 scientific name, common name. It is recommended to use the ID for efficiency.
     * @param geneArg  can either be the NCBI ID, Ensembl ID or official symbol. NCBI ID is most efficient (and
     *                 guaranteed to be unique). Official symbol returns a gene homologue on a random taxon.
     * @see GeneWebService#getGenesByIds(GeneArrayArg)
     */
    @GET
    @Path("/{taxon}/genes/{gene}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve genes matching gene identifiers in a given taxon",
            responses = {
                    @ApiResponse(responseCode = "200", description = "The genes the identifiers resolved to, restricted to the taxon.", useReturnTypeSchema = true, content = @Content())
            })
    public ResponseDataObject<List<GeneValueObject>> getTaxonGenesByIds( // Params:
            @Parameter(description = "Taxon identifier: its id, or its scientific or common name. The id is unambiguous.") @PathParam("taxon") TaxonArg<?> taxonArg, // Required
            @Parameter(description = "Gene identifier: an NCBI id, an Ensembl id, or an official symbol. The NCBI id is unambiguous; an official symbol can resolve to a homologue in another taxon.") @PathParam("gene") GeneArrayArg geneArg // Required
    ) {
        List<GeneValueObject> vos = geneArgService.getGenesInTaxon( geneArg, taxonArgService.getEntity( taxonArg ) );
        geneService.populateAssociatedExperimentCount( vos );
        return respond( vos );
    }

    /**
     * @see GeneWebService#getGeneProbes
     */
    @GET
    @Path("/{taxon}/genes/{gene}/probes")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve the probes associated to a genes across all platforms in a given taxon",
            description = "Supports two pagination modes. Legacy mode: pass `offset` (and `limit`); response includes `offset` and `totalElements`. "
                    + "Cursor mode (recommended for deep pagination and consistency under writes — a single gene can map to many probes across multi-platform inventories): "
                    + "pass an opaque `cursor` token from a previous response's `nextCursor` / `prevCursor` field. "
                    + "`offset` and `cursor` are mutually exclusive — passing a non-null `cursor` selects cursor mode. "
                    + "In cursor mode the result is always sorted by ascending `cs.id` (cursor mode forces a single-component id sort pending the indexed-column audit in phase B); "
                    + "the path-derived `{taxon}` + `{gene}` constraints are preserved (taxon enforced at gene-resolution time, identical scope to the offset variant); "
                    + "`totalElements` is `null` by default (no count query per request).",
            responses = {
                    @ApiResponse(responseCode = "200", description = "The probes for the gene across the taxon's platforms, in whichever pagination envelope the request selected.",
                            content = @Content(schema = @Schema(oneOf = {
                                    PaginatedResponseDataObjectCompositeSequenceValueObject.class,
                                    CursorPaginatedResponseDataObjectCompositeSequenceValueObject.class
                            }))),
            })
    public Object getTaxonGeneProbes( @Parameter(description = "Taxon identifier: its id, or its scientific or common name. The id is unambiguous.") @PathParam("taxon") TaxonArg<?> taxonArg, @Parameter(description = "Gene identifier: an NCBI id, an Ensembl id, or an official symbol. The NCBI id is unambiguous; an official symbol can resolve to a homologue in another taxon.") @PathParam("gene") GeneArg<?> geneArg, @Parameter(description = "How many results to skip before the page begins. Mutually exclusive with `cursor`.") @QueryParam("offset") @DefaultValue("0") OffsetArg offsetArg, @Parameter(description = "Maximum number of results to return.") @QueryParam("limit") @DefaultValue("20") LimitArg limitArg,
            @Parameter(description = "Opaque keyset-pagination cursor token; mutually exclusive with `offset`.") @QueryParam("cursor") CursorArg cursorArg ) {
        Taxon taxon = taxonArgService.getEntity( taxonArg );
        if ( cursorArg != null ) {
            // Mutual-exclusion: a non-null cursor selects cursor mode. The default offset=0 is
            // not considered user-supplied (parallels GET /genes/{gene}/probes step 1m).
            // In cursor mode we currently force a +id sort (GeneArgService.getGeneProbesInTaxonByCursor)
            // — the DAO restricts cursors to single-component id sorts until the index audit lands.
            // The path-derived taxon scope is enforced inside getGeneProbesInTaxonByCursor at
            // gene-resolution time (getEntityWithTaxon), identical to the offset variant.
            CursorPage<CompositeSequenceValueObject> page = geneArgService.getGeneProbesInTaxonByCursor( geneArg, taxon, cursorArg.getValue(), limitArg.getValue() );
            return paginateByCursor( page, new String[] { "id" } );
        }
        return paginate( geneArgService.getGeneProbesInTaxon( geneArg, taxon, offsetArg.getValue(), limitArg.getValue() ), new String[] { "id" } );
    }

    /**
     * @see GeneWebService#getGeneGoTerms(GeneArg)
     */
    @GET
    @Path("/{taxon}/genes/{gene}/goTerms")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve the GO terms associated to a gene in a given taxon",
            responses = {
                    @ApiResponse(responseCode = "200", description = "The GO terms annotated to the gene.", useReturnTypeSchema = true, content = @Content())
            })
    public ResponseDataObject<List<GeneOntologyTermValueObject>> getTaxonGeneGoTerms( @Parameter(description = "Taxon identifier: its id, or its scientific or common name. The id is unambiguous.") @PathParam("taxon") TaxonArg<?> taxonArg, @Parameter(description = "Gene identifier: an NCBI id, an Ensembl id, or an official symbol. The NCBI id is unambiguous; an official symbol can resolve to a homologue in another taxon.") @PathParam("gene") GeneArg<?> geneArg ) {
        return respond( geneArgService.getGeneGoTermsInTaxon( geneArg, taxonArgService.getEntity( taxonArg ) ) );
    }

    /**
     * Retrieves gene location for the gene on the given taxon.
     *
     * @param taxonArg can either be Taxon ID or one of its string identifiers:
     *                 scientific name, common name. It is recommended to use the ID for efficiency.
     * @param geneArg  can either be the NCBI ID, Ensembl ID or official symbol. NCBI ID is most efficient (and
     *                 guaranteed to be unique). Official symbol returns a gene homologue on a random taxon.
     */
    @GET
    @Path("/{taxon}/genes/{gene}/locations")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve physical locations for a given gene and taxon",
            responses = {
                    @ApiResponse(responseCode = "200", description = "The gene's physical locations.", useReturnTypeSchema = true, content = @Content())
            })
    public ResponseDataObject<List<PhysicalLocationValueObject>> getTaxonGeneLocations( // Params:
            @Parameter(description = "Taxon identifier: its id, or its scientific or common name. The id is unambiguous.") @PathParam("taxon") TaxonArg<?> taxonArg, // Required
            @Parameter(description = "Gene identifier: an NCBI id, an Ensembl id, or an official symbol. The NCBI id is unambiguous; an official symbol can resolve to a homologue in another taxon.") @PathParam("gene") GeneArg<?> geneArg // Required
    ) {
        return respond( geneArgService.getGeneLocationInTaxon( geneArg, taxonArgService.getEntity( taxonArg ) ) );
    }

    /**
     * Retrieves datasets for the given taxon.
     * <p>
     * Filtering allowed exactly like in {@link DatasetsWebService#getDatasets(QueryArg, FilterArg, OffsetArg, LimitArg, SortArg)}.
     *
     * @param taxonArg can either be Taxon ID, Taxon NCBI ID, or one of its string identifiers:
     *                 scientific name, common name. It is recommended to use the ID for efficiency.
     */
    @GET
    @Path("/{taxon}/datasets")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve the datasets for a given taxon",
            description = "Supports two pagination modes. Legacy mode: pass `offset` (and `limit`); response includes `offset` and `totalElements`. "
                    + "Cursor mode (recommended for deep pagination and consistency under writes): pass an opaque `cursor` token from a previous response's `nextCursor` / `prevCursor` field. "
                    + "`offset` and `cursor` are mutually exclusive — passing a non-null `cursor` selects cursor mode. "
                    + "In cursor mode the result is always sorted by ascending `id` (the user `sort` arg is currently ignored, pending the indexed-column audit in phase B); the `taxon.id = ?` constraint is preserved on top of the user-supplied `?filter=`; `totalElements` is `null` by default (no count query per request).",
            responses = {
                    @ApiResponse(responseCode = "200", description = "The taxon's datasets, in whichever pagination envelope the request selected.",
                            content = @Content(schema = @Schema(oneOf = {
                                    FilteredAndPaginatedResponseDataObjectExpressionExperimentValueObject.class,
                                    FilteredAndCursorPaginatedResponseDataObjectExpressionExperimentValueObject.class
                            }))),
            })
    public Object getTaxonDatasets( // Params:
            @Parameter(description = "Taxon identifier: its id, or its scientific or common name. The id is unambiguous.") @PathParam("taxon") TaxonArg<?> taxonArg, // Required
            @Parameter(description = "Restrict the results with a filter expression. The schema documents the syntax and lists the properties available.") @QueryParam("filter") @DefaultValue("") FilterArg<ExpressionExperiment> filter, // Optional, default null
            @Parameter(description = "How many results to skip before the page begins. Mutually exclusive with `cursor`.") @QueryParam("offset") @DefaultValue("0") OffsetArg offset, // Optional, default 0
            @Parameter(description = "Maximum number of results to return.") @QueryParam("limit") @DefaultValue("20") LimitArg limit, // Optional, default 20
            @Parameter(description = "Order the results by a property: `+` for ascending, `-` for descending.") @QueryParam("sort") @DefaultValue("+id") SortArg<ExpressionExperiment> sort, // Optional, default +id
            @Parameter(description = "Opaque keyset-pagination cursor token; mutually exclusive with `offset`.") @QueryParam("cursor") CursorArg cursorArg
    ) {
        // will raise a NotFoundException if the taxon is not found
        Taxon taxon = taxonArgService.getEntity( taxonArg );
        Filters filters = datasetArgService.getFilters( filter )
                .and( expressionExperimentService.getFilter( "taxon.id", Long.class, Filter.Operator.eq, taxon.getId() ) );
        if ( cursorArg != null ) {
            // Mutual-exclusion: a non-null cursor selects cursor mode. The default offset=0 is
            // not considered user-supplied (parallels GET /platforms step 1c). In cursor mode we
            // currently force a +id sort (DatasetArgService.getDatasetsByCursor) — the DAO
            // restricts cursors to single-component id sorts until the index audit lands.
            // The taxon.id filter still applies; it has been composed into `filters` above.
            CursorPage<ExpressionExperimentValueObject> page = datasetArgService.getDatasetsByCursor(
                    filters, cursorArg.getValue(), limit.getValue() );
            return new FilteredAndCursorPaginatedResponseDataObject<>( page, filters, new String[] { "id" } );
        }
        return paginate( expressionExperimentService::loadValueObjects, filters, new String[] { "id" }, datasetArgService.getSort( sort ), offset.getValue(), limit.getValue() );
    }
}
