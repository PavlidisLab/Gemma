package ubic.gemma.web.services.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ubic.gemma.core.association.phenotype.EntityNotFoundException;
import ubic.gemma.core.association.phenotype.PhenotypeAssociationManagerService;
import ubic.gemma.core.genome.gene.service.GeneService;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonValueObject;
import ubic.gemma.model.genome.gene.phenotype.EvidenceFilter;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.genome.ChromosomeService;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;
import ubic.gemma.web.services.rest.util.*;
import ubic.gemma.web.services.rest.util.args.*;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashSet;

/**
 * RESTful interface for taxa.
 *
 * @author tesarst
 */
@Service
@Path("/taxa")
public class TaxaWebService extends WebServiceWithFiltering<Taxon, TaxonValueObject, TaxonService> {

    private TaxonService taxonService;
    private GeneService geneService;
    private ExpressionExperimentService expressionExperimentService;
    private PhenotypeAssociationManagerService phenotypeAssociationManagerService;
    private ChromosomeService chromosomeService;

    /**
     * Required by spring
     */
    public TaxaWebService() {
    }

    /**
     * Constructor for service autowiring
     */
    @Autowired
    public TaxaWebService( TaxonService taxonService, GeneService geneService,
            ExpressionExperimentService expressionExperimentService,
            PhenotypeAssociationManagerService phenotypeAssociationManagerService,
            ChromosomeService chromosomeService ) {
        super( taxonService );
        this.taxonService = taxonService;
        this.geneService = geneService;
        this.expressionExperimentService = expressionExperimentService;
        this.phenotypeAssociationManagerService = phenotypeAssociationManagerService;
        this.chromosomeService = chromosomeService;
    }

    /**
     * Lists all available taxa. Does not offer any advanced filtering or sorting functionality.
     * The reason for this is that Taxa are a relatively small set of objects that rarely change.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public ResponseDataObject all( // Params:
            @Context final HttpServletResponse sr // The servlet response, needed for response code setting.
    ) {
        return Responder.autoCode( taxonService.loadAllValueObjects(), sr );
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
     * @see WebServiceWithFiltering#some(AbstractEntityArrayArg, FilterArg, IntArg, IntArg, SortArg, HttpServletResponse)
     */
    @GET
    @Path("/{taxaArg: [^/]+}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public ResponseDataObject taxa( // Params:
            @PathParam("taxaArg") TaxonArrayArg taxaArg, // Optional
            @Context final HttpServletResponse sr // The servlet response, needed for response code setting.
    ) {
        return super.some( taxaArg, FilterArg.EMPTY_FILTER, IntArg.valueOf( "0" ), IntArg.valueOf( "-1" ),
                SortArg.valueOf( "+id" ), sr );
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
    @Path("/{taxonArg: [a-zA-Z0-9%20\\.]+}/chromosomes/{chromosomeArg: [a-zA-Z0-9\\.]+}/genes")
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseDataObject taxonChromosomeGenes( // Params:
            @PathParam("taxonArg") TaxonArg<Object> taxonArg, // Required
            @PathParam("chromosomeArg") String chromosomeName, // Required
            @QueryParam("strand") @DefaultValue("+") String strand, //Optional, default +
            @QueryParam("start") LongArg start, // Required
            @QueryParam("size") IntArg size, // Required
            @Context final HttpServletResponse sr ) {
        super.checkReqArg( start, "start" );
        super.checkReqArg( size, "size" );
        return Responder.autoCode(
                taxonArg.getGenesOnChromosome( taxonService, chromosomeService, geneService, chromosomeName,
                        start.getValue(), size.getValue() ), sr );
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
    @Path("/{taxonArg: [a-zA-Z0-9%20\\.]+}/genes/{geneArg: [a-zA-Z0-9\\.]+}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public ResponseDataObject genes( // Params:
            @PathParam("taxonArg") TaxonArg<Object> taxonArg, // Required
            @PathParam("geneArg") GeneArg<Object> geneArg, // Required
            @Context final HttpServletResponse sr // The servlet response, needed for response code setting.
    ) {
        return Responder.autoCode( geneArg.getGenesOnTaxon( geneService, taxonService, taxonArg ), sr );
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
    @Path("/{taxonArg: [a-zA-Z0-9%20\\.]+}/genes/{geneArg: [a-zA-Z0-9\\.]+}/evidence")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public ResponseDataObject genesEvidence( // Params:
            @PathParam("taxonArg") TaxonArg<Object> taxonArg, // Required
            @PathParam("geneArg") GeneArg<Object> geneArg, // Required
            @Context final HttpServletResponse sr // The servlet response, needed for response code setting.
    ) {
        Taxon taxon = taxonArg.getEntity( taxonService );
        return Responder
                .autoCode( geneArg.getGeneEvidence( geneService, phenotypeAssociationManagerService, taxon ), sr );
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
    @Path("/{taxonArg: [a-zA-Z0-9%20\\.]+}/genes/{geneArg: [a-zA-Z0-9\\.]+}/locations")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public ResponseDataObject genesLocation( // Params:
            @PathParam("taxonArg") TaxonArg<Object> taxonArg, // Required
            @PathParam("geneArg") GeneArg<Object> geneArg, // Required
            @Context final HttpServletResponse sr // The servlet response, needed for response code setting.
    ) {
        Taxon taxon = taxonArg.getEntity( taxonService );
        return Responder.autoCode( geneArg.getGeneLocation( geneService, taxon ), sr );
    }

    /**
     * Retrieves datasets for the given taxon. Filtering allowed exactly like in {@link DatasetsWebService#all(DatasetFilterArg, IntArg, IntArg, SortArg, HttpServletResponse)}
     *
     * @param taxonArg can either be Taxon ID, Taxon NCBI ID, or one of its string identifiers:
     *                 scientific name, common name. It is recommended to use the ID for efficiency.
     * @see WebServiceWithFiltering#all(FilterArg, IntArg, IntArg, SortArg, HttpServletResponse) for details about the
     * filter, offset, limit and sort arguments.
     */
    @GET
    @Path("/{taxonArg: [a-zA-Z0-9%20\\.]+}/datasets")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public ResponseDataObject taxonDatasets( // Params:
            @PathParam("taxonArg") TaxonArg<Object> taxonArg, // Required
            @QueryParam("filter") @DefaultValue("") DatasetFilterArg filter, // Optional, default null
            @QueryParam("offset") @DefaultValue("0") IntArg offset, // Optional, default 0
            @QueryParam("limit") @DefaultValue("20") IntArg limit, // Optional, default 20
            @QueryParam("sort") @DefaultValue("+id") SortArg sort, // Optional, default +id
            @Context final HttpServletResponse sr // The servlet response, needed for response code setting.
    ) {
        return Responder.autoCode(
                taxonArg.getTaxonDatasets( expressionExperimentService, taxonService, filter.getObjectFilters(),
                        offset.getValue(), limit.getValue(), sort.getField(), sort.isAsc() ), sr );
    }

    /**
     * Loads all phenotypes for the given taxon. Unfortunately, pagination is not possible as the
     * phenotypes are loaded in a tree structure.
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
    @Path("/{taxonArg: [a-zA-Z0-9%20\\.]+}/phenotypes")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public ResponseDataObject taxonPhenotypes( // Params:
            @PathParam("taxonArg") TaxonArg<Object> taxonArg, // Required
            @QueryParam("editableOnly") @DefaultValue("false") BoolArg editableOnly, // Optional, default false
            @QueryParam("tree") @DefaultValue("false") BoolArg tree, // Optional, default false
            @Context final HttpServletResponse sr // The servlet response, needed for response code setting
    ) {
        Taxon taxon = taxonArg.getEntity( taxonService );
        if ( tree.getValue() ) {
            return Responder.autoCode( phenotypeAssociationManagerService
                    .loadAllPhenotypesAsTree( new EvidenceFilter( taxon.getId(), editableOnly.getValue() ) ), sr );
        }
        return Responder.autoCode( phenotypeAssociationManagerService
                .loadAllPhenotypesByTree( new EvidenceFilter( taxon.getId(), editableOnly.getValue() ) ), sr );
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
    @Path("/{taxonArg: [a-zA-Z0-9%20\\.]+}/phenotypes/candidates")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public ResponseDataObject findCandidateGenes( // Params:
            @PathParam("taxonArg") TaxonArg<Object> taxonArg, // Required
            @QueryParam("phenotypes") StringArrayArg phenotypes, // Required
            @QueryParam("editableOnly") @DefaultValue("false") BoolArg editableOnly, // Optional, default false
            @Context final HttpServletResponse sr // The servlet response, needed for response code setting)
    ) {
        super.checkReqArg( phenotypes, "phenotypes" );
        return this.getCandidateGenes( taxonArg, editableOnly, phenotypes, sr );
    }

    /**
     * Tries to retrieve candidate genes for given phenotypes, handles known exceptions that can occur
     *
     * @param taxonArg     the taxon to list the genes for.
     * @param editableOnly whether to only list editable genes.
     * @param phenotypes   phenotype value URIs.
     * @return a list of genes associated with given phenotypes
     * @throws GemmaApiException in case one of the given phenotypes was not found, or one of the given arguments
     *                           is malformed.
     */
    private ResponseDataObject getCandidateGenes( TaxonArg<Object> taxonArg, BoolArg editableOnly,
            StringArrayArg phenotypes, HttpServletResponse sr ) {
        Object response;
        try {
            response = this.phenotypeAssociationManagerService.findCandidateGenes(
                    new EvidenceFilter( taxonArg.getEntity( taxonService ).getId(), editableOnly.getValue() ),
                    new HashSet<>( phenotypes.getValue() ) );
        } catch ( EntityNotFoundException e ) {
            WellComposedErrorBody errorBody = new WellComposedErrorBody( Response.Status.NOT_FOUND, e.getMessage() );
            throw new GemmaApiException( errorBody );
        }
        return Responder.autoCode( response, sr );
    }

}
