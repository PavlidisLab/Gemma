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
import ubic.gemma.persistence.service.association.phenotype.PhenotypeAssociationDaoImpl;
import ubic.gemma.web.services.rest.util.*;
import ubic.gemma.web.services.rest.util.args.BoolArg;
import ubic.gemma.web.services.rest.util.args.IntArg;
import ubic.gemma.web.services.rest.util.args.TaxonArg;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * RESTful interface for phenotypes.
 * Does not have an 'all' endpoint (no use-cases). To list all phenotypes on a specific taxon,
 * see {@link TaxaWebService#taxonPhenotypes(TaxonArg, BoolArg, HttpServletResponse)}.
 *
 * @author tesarst
 */
@Component
@Path("/phenotypes")
public class PhenotypeWebService extends WebService {

    private static final String ERROR_MSG_DB_NAME_EMPTY = "Required argument 'database' not found.";
    private PhenotypeAssociationManagerService phenotypeAssociationManagerService;

    /**
     * Required by spring
     */
    public PhenotypeWebService() {
    }

    /**
     * Constructor for service autowiring
     */
    @Autowired
    public PhenotypeWebService( PhenotypeAssociationManagerService phenotypeAssociationManagerService ) {
        this.phenotypeAssociationManagerService = phenotypeAssociationManagerService;
    }

    /**
     * Finds all evidence with the given external database name.
     *
     * @param database The name of external database to match.
     * @param offset   Only return the result collection from this index.
     * @param limit    Limit the number of results to this amount.
     * @return A collection of Evidence Value Objects.
     */
    @GET
    @Path("/evidence")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public ResponseDataObject evidence( // Params:
            @QueryParam("database") String database, // required
            @QueryParam("offset") @DefaultValue("0") IntArg offset, // Optional, default 0
            @QueryParam("limit") @DefaultValue(PhenotypeAssociationDaoImpl.DEFAULT_PA_LIMIT + "") IntArg limit, // Opt.
            @Context final HttpServletResponse sr // The servlet response, needed for response code setting
    ) {
        super.checkReqArg( database, "database" );
        return Responder.autoCode( this.phenotypeAssociationManagerService
                .loadEvidenceWithExternalDatabaseName( database, limit.getValue(), offset.getValue() ), sr );
    }

    /**
     * Finds all dumps.
     *
     * @return A collection of Dumps Value Objects.
     * @see PhenotypeAssociationManagerService#helpFindAllDumps()
     */
    @GET
    @Path("/dumps")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public ResponseDataObject dumps( // Params:
            @Context final HttpServletResponse sr // The servlet response, needed for response code setting
    ) {
        return Responder.autoCode( this.phenotypeAssociationManagerService.helpFindAllDumps(), sr );
    }

}
