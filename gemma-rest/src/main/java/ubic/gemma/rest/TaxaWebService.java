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
import io.swagger.v3.oas.annotations.enums.Explode;
import io.swagger.v3.oas.annotations.media.Schema;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ubic.gemma.core.association.phenotype.PhenotypeAssociationManagerService;
import ubic.gemma.core.search.ParseSearchException;
import ubic.gemma.core.search.SearchException;
import ubic.gemma.core.search.SearchTimeoutException;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.genome.PhysicalLocationValueObject;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonValueObject;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.model.genome.gene.phenotype.EvidenceFilter;
import ubic.gemma.model.genome.gene.phenotype.valueObject.GeneEvidenceValueObject;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Sort;
import ubic.gemma.rest.util.FilteredAndPaginatedResponseDataObject;
import ubic.gemma.rest.util.Responder;
import ubic.gemma.rest.util.ResponseDataObject;
import ubic.gemma.rest.util.args.*;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.*;

/**
 * RESTful interface for taxa.
 *
 * @author tesarst
 */
@Service
@Path("/taxa")
public class TaxaWebService {

    protected static final Log log = LogFactory.getLog( TaxaWebService.class.getName() );
    private TaxonService taxonService;
    private ExpressionExperimentService expressionExperimentService;
    private PhenotypeAssociationManagerService phenotypeAssociationManagerService;
    private TaxonArgService taxonArgService;
    private DatasetArgService datasetArgService;
    private GeneArgService geneArgService;

    /**
     * Required by spring
     */
    public TaxaWebService() {
    }

    /**
     * Constructor for service autowiring
     */
    @Autowired
    public TaxaWebService( TaxonService taxonService, ExpressionExperimentService expressionExperimentService,
            PhenotypeAssociationManagerService phenotypeAssociationManagerService, TaxonArgService taxonArgService,
            DatasetArgService datasetArgService, GeneArgService geneArgService ) {
        this.taxonService = taxonService;
        this.expressionExperimentService = expressionExperimentService;
        this.phenotypeAssociationManagerService = phenotypeAssociationManagerService;
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
        return Responder.respond( taxonService.loadAllValueObjects() );
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
        Sort sort = taxonService.getSort( "id", null );
        return Responder.respond( taxonService.loadValueObjects( filters, sort ) );
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
        return Responder.respond( taxonArgService.getGenesOnChromosome( taxonArg, chromosomeName, strand, start, size ) );
    }

    /**
     * Retrieves genes matching the identifier on the given taxon.
     *
     * @param taxonArg can either be Taxon ID or one of its string identifiers:
     *                 scientific name, common name. It is recommended to use the ID for efficiency.
     * @param geneArg  can either be the NCBI ID, Ensembl ID or official symbol. NCBI ID is most efficient (and
     *                 guaranteed to be unique). Official symbol returns a gene homologue on a random taxon.
     */
    @GET
    @Path("/{taxon}/genes/{gene}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve all genes in a given taxon")
    public ResponseDataObject<List<GeneValueObject>> getTaxonGenes( // Params:
            @PathParam("taxon") TaxonArg<?> taxonArg, // Required
            @PathParam("gene") GeneArg<?> geneArg // Required
    ) {
        return Responder.respond( geneArgService.getGenesOnTaxon( geneArg, taxonArgService.getEntity( taxonArg ) ) );
    }

    /**
     * Retrieves gene evidence for the gene on the given taxon.
     *
     * @param taxonArg can either be Taxon ID or one of its string identifiers:
     *                 scientific name, common name. It is recommended to use the ID for efficiency.
     * @param geneArg  can either be the NCBI ID, Ensembl ID or official symbol. NCBI ID is most efficient (and
     *                 guaranteed to be unique). Official symbol returns a gene homologue on a random taxon.
     */
    @GET
    @Path("/{taxon}/genes/{gene}/evidence")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve evidences for a given gene and taxon", hidden = true)
    @Deprecated
    public ResponseDataObject<List<GeneEvidenceValueObject>> getGenesEvidenceInTaxon( // Params:
            @PathParam("taxon") TaxonArg<?> taxonArg, // Required
            @PathParam("gene") GeneArg<?> geneArg // Required
    ) {
        Taxon taxon = taxonArgService.getEntity( taxonArg );
        try {
            return Responder.respond( geneArgService.getGeneEvidence( geneArg, taxon ) );
        } catch ( ParseSearchException e ) {
            throw new BadRequestException( "Invalid search query: " + e.getQuery(), e );
        } catch ( SearchTimeoutException e ) {
            throw new ServiceUnavailableException( e.getMessage(), DateUtils.addSeconds( new Date(), 30 ), e.getCause() );
        } catch ( SearchException e ) {
            throw new InternalServerErrorException( e );
        }
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
    public ResponseDataObject<List<PhysicalLocationValueObject>> getGeneLocationsInTaxon( // Params:
            @PathParam("taxon") TaxonArg<?> taxonArg, // Required
            @PathParam("gene") GeneArg<?> geneArg // Required
    ) {
        Taxon taxon = taxonArgService.getEntity( taxonArg );
        return Responder.respond( geneArgService.getGeneLocation( geneArg, taxon ) );
    }

    /**
     * Retrieves datasets for the given taxon. Filtering allowed exactly like in {@link DatasetsWebService#getDatasets(String, FilterArg, OffsetArg, LimitArg, SortArg)}.
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
            @QueryParam("sort") @DefaultValue("+id") SortArg<Taxon> sort // Optional, default +id
    ) {
        // will raise a NotFoundException if the taxon is not found
        taxonArgService.getEntity( taxonArg );
        Filters filters = datasetArgService.getFilters( filter )
                .and( taxonArgService.getFilters( taxonArg ) );
        return Responder.paginate( expressionExperimentService::loadValueObjects, filters, new String[] { "id" },
                taxonArgService.getSort( sort ), offset.getValue(), limit.getValue() );
    }

    /**
     * Loads all phenotypes for the given taxon. Unfortunately, pagination is not possible as the
     * phenotypes are loaded in a tree structure.
     * <p>
     * TODO: We need to split this in two methods because otherwise we cannot infer the type this endpoint is producing
     *       and provide a backward compatible switch.
     *
     * @param taxonArg     the taxon to list the phenotypes for.
     * @param editableOnly whether to only list editable phenotypes.
     * @param tree         whether the returned structure should be an actual tree (nested JSON objects). Default is
     *                     false - the tree is flattened and the edges of the tree are stored in
     *                     the values of the value object.
     * @return a list of Simple Tree value objects allowing a reconstruction of a tree, or an actual tree structure of
     * TreeCharacteristicValueObjects, if the
     */
    @GET
    @Path("/{taxon}/phenotypes")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve the phenotypes for a given taxon", hidden = true)
    public ResponseDataObject<Collection<?>> getTaxonPhenotypes( // Params:
            @PathParam("taxon") TaxonArg<?> taxonArg, // Required
            @QueryParam("editableOnly") @DefaultValue("false") Boolean editableOnly, // Optional, default false
            @QueryParam("tree") @DefaultValue("false") Boolean tree // Optional, default false
    ) {
        Taxon taxon = taxonArgService.getEntity( taxonArg );
        if ( tree ) {
            return Responder.respond( phenotypeAssociationManagerService
                    .loadAllPhenotypesAsTree( new EvidenceFilter( taxon.getId(), editableOnly ) ) );
        }
        return Responder.respond( phenotypeAssociationManagerService
                .loadAllPhenotypesByTree( new EvidenceFilter( taxon.getId(), editableOnly ) ) );
    }

    /**
     * Given a set of phenotypes, return all genes associated with them.
     *
     * @param taxonArg     the taxon to list the genes for.
     * @param editableOnly whether to only list editable genes.
     * @param phenotypes   phenotype value URIs separated by commas.
     * @return a list of genes associated with given phenotypes.
     */
    @GET
    @Path("/{taxon}/phenotypes/candidates")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve the candidate gens for a given set of phenotypes and taxon", hidden = true)
    @Deprecated
    public ResponseDataObject<Set<GeneEvidenceValueObject>> findCandidateGenesInTaxon( // Params:
            @PathParam("taxon") TaxonArg<?> taxonArg, // Required
            @Parameter(schema = @Schema(implementation = StringArrayArg.class), explode = Explode.FALSE) @QueryParam("phenotypes") StringArrayArg phenotypes, // Required
            @QueryParam("editableOnly") @DefaultValue("false") Boolean editableOnly // Optional, default false
    ) {
        Set<GeneEvidenceValueObject> response;
        response = this.phenotypeAssociationManagerService.findCandidateGenes(
                new EvidenceFilter( taxonArgService.getEntity( taxonArg ).getId(), editableOnly ),
                new HashSet<>( phenotypes.getValue() ) );
        return Responder.respond( response );
    }

}
