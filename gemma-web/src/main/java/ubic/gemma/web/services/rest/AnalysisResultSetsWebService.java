/*
 * The Gemma project
 *
 * Copyright (c) 2006 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package ubic.gemma.web.services.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ubic.gemma.model.analysis.AnalysisResultSet;
import ubic.gemma.model.analysis.AnalysisResultSetValueObject;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.persistence.service.analysis.expression.diff.ExpressionAnalysisResultSetService;
import ubic.gemma.persistence.service.common.description.DatabaseEntryService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.web.services.rest.util.Responder;
import ubic.gemma.web.services.rest.util.ResponseDataObject;
import ubic.gemma.web.services.rest.util.WebService;
import ubic.gemma.web.services.rest.util.args.DatabaseEntryArg;
import ubic.gemma.web.services.rest.util.args.DatasetArg;
import ubic.gemma.web.services.rest.util.args.ExpressionAnalysisResultSetArg;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Endpoint for {@link ubic.gemma.model.analysis.AnalysisResultSet}
 */
@Component
@Path("/resultSets")
public class AnalysisResultSetsWebService extends WebService {

    @Autowired
    private ExpressionAnalysisResultSetService expressionAnalysisResultSetService;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private DatabaseEntryService databaseEntryService;

    /**
     * Retrieve all {@link AnalysisResultSet} matching a set of criteria.
     *
     * @param datasetIds filter result sets that belong to any of the provided dataset identifiers, or null to ignore
     * @param externalIds filter by associated datasets with given external identifiers, or null to ignore
     * @param servlet
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseDataObject<List<AnalysisResultSetValueObject>> findAll(
            @QueryParam("datasetIds") Set<DatasetArg<?>> datasetIds,
            @QueryParam("externalIds") Set<DatabaseEntryArg<?>> externalIds,
            @Context final HttpServletResponse servlet ) {
        Collection<BioAssaySet> bioAssaySets = datasetIds.stream()
                .map( datasetId -> datasetId.getPersistentObject( expressionExperimentService ) )
                .filter( Objects::nonNull )
                .collect( Collectors.toSet() );
        Collection<DatabaseEntry> databaseEntries = externalIds.stream()
                .map( externalId -> externalId.getPersistentObject( databaseEntryService ) )
                .filter( Objects::nonNull )
                .collect( Collectors.toSet() );
        return Responder.code200( expressionAnalysisResultSetService.findByBioAssaySetInAndDatabaseEntryInLimit( bioAssaySets, databaseEntries, 10 ), servlet );
    }

    /**
     * Retrieve a {@link AnalysisResultSet} given its identifier.
     *
     * @param analysisResultSetId
     * @return
     */
    @GET
    @Path("/{analysisResultSetId}")
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseDataObject<AnalysisResultSetValueObject> findById(
            @PathParam("analysisResultSetId") ExpressionAnalysisResultSetArg analysisResultSetId,
            @Context final HttpServletResponse servlet ) {
        return Responder.code200( analysisResultSetId.getValueObject( expressionAnalysisResultSetService ), servlet );
    }
}
