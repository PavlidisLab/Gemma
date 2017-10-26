package ubic.gemma.web.services.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.web.services.rest.util.Responder;
import ubic.gemma.web.services.rest.util.ResponseDataObject;
import ubic.gemma.web.services.rest.util.WebService;
import ubic.gemma.web.services.rest.util.args.ArrayGeneArg;
import ubic.gemma.web.services.rest.util.args.DatasetArg;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

/**
 * RESTful interface for gene expression levels.
 *
 * @author tesarst
 */
@Service
@Path("/genes/expressionsXX")
public class ExpressionsWebService extends WebService {

    private ExpressionExperimentService expressionExperimentService;

    /**
     * Required by spring
     */
    public ExpressionsWebService() {
    }

    /**
     * Constructor for service autowiring
     */
    @Autowired
    public ExpressionsWebService( ExpressionExperimentService expressionExperimentService ) {
        this.expressionExperimentService = expressionExperimentService;
    }

    /**
     * Retrieves the gene expressions for the given dataset.
     *
     * @param datasetArg can either be the ExpressionExperiment ID or its short name (e.g. GSE1234). Retrieval by ID
     *                   is more efficient. Only datasets that user has access to will be available.
     */
    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public ResponseDataObject datasetExpressions( // Params:
            @QueryParam("datasets") DatasetArg<Object> datasetArg, // Required
            @QueryParam("genes") ArrayGeneArg genes, // Optional, default null
            @Context final HttpServletResponse sr // The servlet response, needed for response code setting.
    ) {
        return Responder.autoCode( null, sr );
    }

}
