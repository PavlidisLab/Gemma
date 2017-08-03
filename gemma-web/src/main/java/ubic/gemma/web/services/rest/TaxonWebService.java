package ubic.gemma.web.services.rest;

import org.hibernate.QueryException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;
import ubic.gemma.persistence.util.ObjectFilter;
import ubic.gemma.web.services.rest.util.Responder;
import ubic.gemma.web.services.rest.util.ResponseDataObject;
import ubic.gemma.web.services.rest.util.WebServiceWithFiltering;
import ubic.gemma.web.services.rest.util.WellComposedErrorBody;
import ubic.gemma.web.services.rest.util.args.*;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.text.ParseException;
import java.util.ArrayList;

/**
 * RESTful interface for taxa.
 *
 * @author tesarst
 */
@Service
@Path("/taxa")
public class TaxonWebService extends WebServiceWithFiltering {

    private TaxonService taxonService;
    private ExpressionExperimentService expressionExperimentService;

    /**
     * Required by spring
     */
    public TaxonWebService() {
    }

    /**
     * Constructor for service autowiring
     */
    @Autowired
    public TaxonWebService( TaxonService taxonService, ExpressionExperimentService expressionExperimentService ) {
        this.taxonService = taxonService;
        this.expressionExperimentService = expressionExperimentService;
    }

    /**
     * Unlike most other web services, Taxa do not offer any advanced filtering or sorting functionality.
     * The reason for this is that Taxa are a fairly small set of objects that rarely change.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public ResponseDataObject all( @Context final HttpServletResponse sr
            // The servlet response, needed for response code setting.
    ) {
        // Uses this.loadVOsPreFilter(...)
        return super.all( null, null, null, null, sr );
    }

    /**
     * Retrieves single taxon based on the given identifier.
     *
     * @param taxonArg can either be Taxon ID or one of its string identifiers:
     *                 scientific name, common name, abbreviation. Using the ID is most efficient.
     */
    @GET
    @Path("/{taxonArg: [a-zA-Z0-9\\.]+}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public ResponseDataObject taxon( // Params:
            @PathParam("taxonArg") TaxonArg<Object> taxonArg, // Required
            @Context final HttpServletResponse sr // The servlet response, needed for response code setting.
    ) {
        Object response = taxonArg.getValueObject( taxonService );
        return this.autoCodeResponse( taxonArg, response, sr );
    }

    /**
     * Retrieves datasets in the given taxon. Filtering allowed exactly like in {@link DatasetsWebService#all(DatasetFilterArg, IntArg, IntArg, SortArg, HttpServletResponse)}
     *
     * @param taxonArg can either be Taxon ID or one of its string identifiers:
     *                 scientific name, common name, abbreviation. Using the ID is most efficient.
     */
    @GET
    @Path("/{taxonArg: [a-zA-Z0-9\\.]+}/datasets")
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
        try {
            Taxon taxon = taxonArg.getPersistentObject( taxonService );
            ArrayList<ObjectFilter[]> filters = filter.getObjectFilters();

            if ( filters == null ) {
                filters = new ArrayList<>( 1 );
            }

            filters.add( new ObjectFilter[] {
                    new ObjectFilter( "id", taxon.getId(), ObjectFilter.is, ObjectFilter.DAO_TAXON_ALIAS ) } );

            return Responder.autoCode( expressionExperimentService
                    .loadValueObjectsPreFilter( offset.getValue(), limit.getValue(), sort.getField(), sort.isAsc(),
                            filters ), sr );
        } catch ( QueryException | ParseException e ) {
            if ( log.isDebugEnabled() ) {
                e.printStackTrace();
            }
            WellComposedErrorBody error = new WellComposedErrorBody( Response.Status.BAD_REQUEST,
                    ERROR_MSG_MALFORMED_REQUEST );
            WellComposedErrorBody.addExceptionFields( error, e );
            return Responder.code( error.getStatus(), error, sr );
        }
    }

    /**
     * This filtering is not for Taxa but for Datasets.
     */
    @Override
    protected ResponseDataObject loadVOsPreFilter( FilterArg filter, IntArg offset, IntArg limit, SortArg sort,
            HttpServletResponse sr ) throws ParseException {
        return Responder.autoCode( taxonService.loadAllValueObjects(), sr );
    }
}
