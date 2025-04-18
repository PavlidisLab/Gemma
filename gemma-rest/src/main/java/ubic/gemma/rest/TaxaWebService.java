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
import ubic.gemma.persistence.service.genome.taxon.TaxonService;
import ubic.gemma.persistence.util.Filter;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Sort;
import ubic.gemma.rest.util.FilteredAndPaginatedResponseDataObject;
import ubic.gemma.rest.util.PaginatedResponseDataObject;
import ubic.gemma.rest.util.ResponseDataObject;
import ubic.gemma.rest.util.args.*;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

import static ubic.gemma.rest.util.Responders.paginate;
import static ubic.gemma.rest.util.Responders.respond;

/**
 * RESTful interface for taxa.
 *
 * @author tesarst
 */
@Service
@Path("/taxa")
public class TaxaWebService {

    protected static final Log log = LogFactory.getLog( TaxaWebService.class.getName() );

    private final TaxonService taxonService;
    private final ExpressionExperimentService expressionExperimentService;
    private final TaxonArgService taxonArgService;
    private final DatasetArgService datasetArgService;
    private final GeneArgService geneArgService;

    /**
     * Constructor for service autowiring
     */
    @Autowired
    public TaxaWebService( TaxonService taxonService, ExpressionExperimentService expressionExperimentService, TaxonArgService taxonArgService, DatasetArgService datasetArgService, GeneArgService geneArgService ) {
        this.taxonService = taxonService;
        this.expressionExperimentService = expressionExperimentService;
        this.taxonArgService = taxonArgService;
        this.datasetArgService = datasetArgService;
        this.geneArgService = geneArgService;
    }

    /**
     * Lists all available taxa. Does not offer any advanced filtering or sorting functionality.
     * The reason for this is that Taxa are a relatively small set of objects that rarely change.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve all available taxa")
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
    @Operation(summary = "Retrieve taxa by their identifiers")
    public ResponseDataObject<List<TaxonValueObject>> getTaxaByIds( @PathParam("taxa") TaxonArrayArg taxaArg ) {
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
    @Operation(summary = "Retrieve genes overlapping a given region in a taxon")
    public ResponseDataObject<List<GeneValueObject>> getTaxonGenesOverlappingChromosome( // Params:
            @PathParam("taxon") TaxonArg<?> taxonArg, // Required
            @PathParam("chromosome") String chromosomeName, // Required
            @QueryParam("strand") String strand, //Optional, default +
            @Parameter(required = true) @QueryParam("start") Long start, // Required
            @Parameter(required = true) @QueryParam("size") Integer size // Required
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
        return respond( taxonArgService.getGenesOnChromosome( taxonArg, chromosomeName, strand, start, size ) );
    }

    @GET
    @Path("/{taxon}/genes")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve all genes in a given taxon")
    public PaginatedResponseDataObject<GeneValueObject> getTaxonGenes(
            @PathParam("taxon") TaxonArg<?> taxonArg,
            @QueryParam("offset") @DefaultValue("0") OffsetArg offsetArg,
            @QueryParam("limit") @DefaultValue("20") LimitArg limitArg
    ) {
        return paginate( geneArgService.getGenesInTaxon( taxonArgService.getEntity( taxonArg ), offsetArg.getValue(), limitArg.getValue() ), new String[] { "id" } );
    }

    /**
     * Retrieves genes matching the identifier on the given taxon.
     *
     * @param taxonArg can either be Taxon ID or one of its string identifiers:
     *                 scientific name, common name. It is recommended to use the ID for efficiency.
     * @param geneArg  can either be the NCBI ID, Ensembl ID or official symbol. NCBI ID is most efficient (and
     *                 guaranteed to be unique). Official symbol returns a gene homologue on a random taxon.
     * @see GeneWebService#getGenes(GeneArrayArg)
     */
    @GET
    @Path("/{taxon}/genes/{gene}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve genes matching gene identifiers in a given taxon")
    public ResponseDataObject<List<GeneValueObject>> getTaxonGenesByIds( // Params:
            @PathParam("taxon") TaxonArg<?> taxonArg, // Required
            @PathParam("gene") GeneArrayArg geneArg // Required
    ) {
        return respond( geneArgService.getGenesInTaxon( geneArg, taxonArgService.getEntity( taxonArg ) ) );
    }

    /**
     * @see GeneWebService#getGeneProbes(GeneArg, OffsetArg, LimitArg)
     */
    @GET
    @Path("/{taxon}/genes/{gene}/probes")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve the probes associated to a genes across all platforms in a given taxon")
    public PaginatedResponseDataObject<CompositeSequenceValueObject> getTaxonGeneProbes( @PathParam("taxon") TaxonArg<?> taxonArg, @PathParam("gene") GeneArg<?> geneArg, @QueryParam("offset") @DefaultValue("0") OffsetArg offsetArg, @QueryParam("limit") @DefaultValue("20") LimitArg limitArg ) {
        return paginate( geneArgService.getGeneProbesInTaxon( geneArg, taxonArgService.getEntity( taxonArg ), offsetArg.getValue(), limitArg.getValue() ), new String[] { "id" } );
    }

    /**
     * @see GeneWebService#getGeneGoTerms(GeneArg)
     */
    @GET
    @Path("/{taxon}/genes/{gene}/goTerms")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve the GO terms associated to a gene in a given taxon")
    public ResponseDataObject<List<GeneOntologyTermValueObject>> getTaxonGeneGoTerms( @PathParam("taxon") TaxonArg<?> taxonArg, @PathParam("gene") GeneArg<?> geneArg ) {
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
    @Operation(summary = "Retrieve physical locations for a given gene and taxon")
    public ResponseDataObject<List<PhysicalLocationValueObject>> getTaxonGeneLocations( // Params:
            @PathParam("taxon") TaxonArg<?> taxonArg, // Required
            @PathParam("gene") GeneArg<?> geneArg // Required
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
    @Operation(summary = "Retrieve the datasets for a given taxon")
    public FilteredAndPaginatedResponseDataObject<ExpressionExperimentValueObject> getTaxonDatasets( // Params:
            @PathParam("taxon") TaxonArg<?> taxonArg, // Required
            @QueryParam("filter") @DefaultValue("") FilterArg<ExpressionExperiment> filter, // Optional, default null
            @QueryParam("offset") @DefaultValue("0") OffsetArg offset, // Optional, default 0
            @QueryParam("limit") @DefaultValue("20") LimitArg limit, // Optional, default 20
            @QueryParam("sort") @DefaultValue("+id") SortArg<ExpressionExperiment> sort // Optional, default +id
    ) {
        // will raise a NotFoundException if the taxon is not found
        Taxon taxon = taxonArgService.getEntity( taxonArg );
        Filters filters = datasetArgService.getFilters( filter )
                .and( expressionExperimentService.getFilter( "taxon.id", Long.class, Filter.Operator.eq, taxon.getId() ) );
        return paginate( expressionExperimentService::loadValueObjects, filters, new String[] { "id" }, datasetArgService.getSort( sort ), offset.getValue(), limit.getValue() );
    }
}
