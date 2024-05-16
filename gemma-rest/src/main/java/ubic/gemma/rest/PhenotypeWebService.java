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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ubic.gemma.core.association.phenotype.PhenotypeAssociationManagerService;
import ubic.gemma.model.association.phenotype.PhenotypeAssociation;
import ubic.gemma.model.genome.gene.phenotype.valueObject.DumpsValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.EvidenceValueObject;
import ubic.gemma.persistence.service.association.phenotype.PhenotypeAssociationDaoImpl;
import ubic.gemma.rest.util.ResponseDataObject;
import ubic.gemma.rest.util.args.LimitArg;
import ubic.gemma.rest.util.args.OffsetArg;
import ubic.gemma.rest.util.args.TaxonArg;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.Set;

import static ubic.gemma.rest.util.Responders.respond;

/**
 * RESTful interface for phenotypes.
 * Does not have an 'all' endpoint (no use-cases). To list all phenotypes on a specific taxon,
 * see {@link TaxaWebService#getTaxonPhenotypes(TaxonArg, Boolean, Boolean)}}.
 *
 * @author tesarst
 */
@Deprecated
@Service
@Path("/phenotypes")
public class PhenotypeWebService {

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
    @Operation(summary = "Retrieve all the evidence from a given external database name", hidden = true)
    public ResponseDataObject<Set<EvidenceValueObject<? extends PhenotypeAssociation>>> getPhenotypeEvidence( // Params:
            @QueryParam("database") String database, // required
            @QueryParam("offset") @DefaultValue("0") OffsetArg offset, // Optional, default 0
            @QueryParam("limit") @DefaultValue(PhenotypeAssociationDaoImpl.DEFAULT_PA_LIMIT + "") LimitArg limit // Opt.
    ) {
        if ( database == null ) {
            throw new BadRequestException( "The 'database' query parameter must be supplied." );
        }
        return respond( this.phenotypeAssociationManagerService
                .loadEvidenceWithExternalDatabaseName( database, limit.getValueNoMaximum(), offset.getValue() ) );
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
    @Operation(summary = "Retrieve all phenotype data dumps", hidden = true)
    public ResponseDataObject<Set<DumpsValueObject>> getPhenotypeDumps() {
        return respond( this.phenotypeAssociationManagerService.helpFindAllDumps() );
    }

}
