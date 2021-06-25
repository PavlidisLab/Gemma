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
import org.springframework.stereotype.Service;
import ubic.gemma.core.analysis.service.ExpressionAnalysisResultSetFileService;
import ubic.gemma.model.analysis.AnalysisResultSet;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSetValueObject;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.persistence.service.analysis.expression.diff.ExpressionAnalysisResultSetService;
import ubic.gemma.persistence.service.common.description.DatabaseEntryService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.web.services.rest.util.GemmaApiException;
import ubic.gemma.web.services.rest.util.Responder;
import ubic.gemma.web.services.rest.util.ResponseDataObject;
import ubic.gemma.web.services.rest.util.WebService;
import ubic.gemma.web.services.rest.util.args.*;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Endpoint for {@link ubic.gemma.model.analysis.AnalysisResultSet}
 */
@Service("analysisResultSetWebService")
@Path("/resultSets")
public class AnalysisResultSetsWebService extends WebService {

    @Autowired
    private ExpressionAnalysisResultSetService expressionAnalysisResultSetService;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private DatabaseEntryService databaseEntryService;

    @Autowired
    private ExpressionAnalysisResultSetFileService expressionAnalysisResultSetFileService;

    /**
     * Retrieve all {@link AnalysisResultSet} matching a set of criteria.
     *
     * @param datasetIds filter result sets that belong to any of the provided dataset identifiers, or null to ignore
     * @param externalIds filter by associated datasets with given external identifiers, or null to ignore
     * @param servlet
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseDataObject<?> findAll(
            @QueryParam("datasets") ArrayDatasetArg datasets,
            @QueryParam("databaseEntries") ArrayDatabaseEntryArg databaseEntries,
            @QueryParam("offset") @DefaultValue("0") IntArg offset,
            @QueryParam("limit") @DefaultValue("20") IntArg limit,
            @QueryParam("sort") @DefaultValue("+id") SortArg sort,
            @Context final HttpServletResponse servlet ) {
        Collection<ExpressionAnalysisResultSet> resultSets = expressionAnalysisResultSetService.findByBioAssaySetInAndDatabaseEntryInLimit(
                Optional.ofNullable( datasets ).map( d -> d.getPersistentObjects( expressionExperimentService ).stream().map( BioAssaySet.class::cast ).collect( Collectors.toSet() ) ).orElse( null ),
                Optional.ofNullable( databaseEntries ).map( de -> de.getPersistentObjects( databaseEntryService ) ).orElse( null ),
                null,
                offset.getValue(),
                limit.getValue(),
                sort.getField(),
                sort.isAsc() );
        return Responder.code200( expressionAnalysisResultSetService.loadValueObjects( resultSets ), servlet );
    }

    /**
     * Retrieve a {@link AnalysisResultSet} given its identifier.
     *
     * @param analysisResultSet
     * @return
     */
    @GET
    @Path("/{analysisResultSet:[^/]+}")
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseDataObject<?> findById(
            @PathParam("analysisResultSet") ExpressionAnalysisResultSetArg analysisResultSet,
            @Context final HttpServletResponse servlet ) {
        ExpressionAnalysisResultSet ears = analysisResultSet.getPersistentObject( expressionAnalysisResultSetService );
        if ( ears == null ) {
            throw new GemmaApiException( Response.Status.NOT_FOUND, "Could not find ExpressionAnalysisResultSet for " + analysisResultSet + "." );
        }
        ears = expressionAnalysisResultSetService.thawWithoutContrasts( ears );
        return Responder.code200( new ExpressionAnalysisResultSetValueObject( ears ), servlet );
    }

    /**
     * Retrieve an {@link AnalysisResultSet} in a tabular format.
     *
     * @param analysisResultSet
     * @param servlet
     * @return
     */
    @GET
    @Path("/{analysisResultSet:[^/]+}")
    @Produces("text/tab-separated-values; qs=0.9")
    public StreamingOutput findByIdToTsv(
            @PathParam("analysisResultSet") ExpressionAnalysisResultSetArg analysisResultSet,
            @Context final HttpServletResponse servlet ) {
        ExpressionAnalysisResultSet ears = analysisResultSet.getPersistentObject( expressionAnalysisResultSetService );
        // only thaw the related analysis and experimental factors without contrasts
        ears = expressionAnalysisResultSetService.thawWithoutContrasts( ears );
        return new ExpressionAnalysisResultSetTsvStreamingOutput( ears );
    }

    private class ExpressionAnalysisResultSetTsvStreamingOutput implements StreamingOutput {

        private final ExpressionAnalysisResultSet resultSet;

        public ExpressionAnalysisResultSetTsvStreamingOutput( ExpressionAnalysisResultSet resultSet ) {
            this.resultSet = resultSet;
        }

        @Override
        public void write( OutputStream outputStream ) throws IOException, WebApplicationException {
            try ( OutputStreamWriter writer = new OutputStreamWriter( outputStream ) ) {
                expressionAnalysisResultSetFileService.writeTsvToAppendable( resultSet, writer );
            }
        }
    }

}
