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
package ubic.gemma.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.Explode;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ubic.gemma.core.analysis.service.ExpressionAnalysisResultSetFileService;
import ubic.gemma.model.analysis.AnalysisResultSet;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResultSetValueObject;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.persistence.service.analysis.expression.diff.ExpressionAnalysisResultSetService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.rest.annotations.GZIP;
import ubic.gemma.rest.util.*;
import ubic.gemma.rest.util.args.*;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.StreamingOutput;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Endpoint for {@link ubic.gemma.model.analysis.AnalysisResultSet}
 */
@Service
@Path("/resultSets")
public class AnalysisResultSetsWebService {

    private static final String TEXT_TAB_SEPARATED_VALUE_Q9_MEDIA_TYPE = MediaTypeUtils.TEXT_TAB_SEPARATED_VALUES_UTF8 + "; q=0.9";

    @Autowired
    private ExpressionAnalysisResultSetService expressionAnalysisResultSetService;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private ExpressionAnalysisResultSetFileService expressionAnalysisResultSetFileService;

    @Autowired
    private ExpressionAnalysisResultSetArgService expressionAnalysisResultSetArgService;

    @Autowired
    private DatasetArgService datasetArgService;
    @Autowired
    private DatabaseEntryArgService databaseEntryArgService;

    /**
     * Retrieve all {@link AnalysisResultSet} matching a set of criteria.
     *
     * @param datasets        filter result sets that belong to any of the provided dataset identifiers, or null to ignore
     * @param databaseEntries filter by associated datasets with given external identifiers, or null to ignore
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve all result sets matching the provided criteria")
    public FilteredAndPaginatedResponseDataObject<DifferentialExpressionAnalysisResultSetValueObject> getResultSets(
            @Parameter(schema = @Schema(implementation = DatasetArrayArg.class), explode = Explode.FALSE) @QueryParam("datasets") DatasetArrayArg datasets,
            @Parameter(schema = @Schema(implementation = DatabaseEntryArrayArg.class), explode = Explode.FALSE) @QueryParam("databaseEntries") DatabaseEntryArrayArg databaseEntries,
            @QueryParam("filter") @DefaultValue("") FilterArg<ExpressionAnalysisResultSet> filters,
            @QueryParam("offset") @DefaultValue("0") OffsetArg offset,
            @QueryParam("limit") @DefaultValue("20") LimitArg limit,
            @QueryParam("sort") @DefaultValue("+id") SortArg<ExpressionAnalysisResultSet> sort ) {
        Collection<BioAssaySet> bas = null;
        if ( datasets != null ) {
            Collection<ExpressionExperiment> ees = new ArrayList<>( datasetArgService.getEntities( datasets ) );
            bas = new ArrayList<>( ees );
            // expand with all subsets
            for ( ExpressionExperiment ee : ees ) {
                bas.addAll( expressionExperimentService.getSubSets( ee ) );
            }
        }
        Collection<DatabaseEntry> des = null;
        if ( databaseEntries != null ) {
            des = databaseEntryArgService.getEntities( databaseEntries );
        }
        Filters filters2 = expressionAnalysisResultSetArgService.getFilters( filters );
        return Responder.paginate( expressionAnalysisResultSetService.findByBioAssaySetInAndDatabaseEntryInLimit(
                        bas, des, filters2, offset.getValue(), limit.getValue(), expressionAnalysisResultSetArgService.getSort( sort ) ),
                filters2, new String[] { "id" } );
    }

    @GET
    @Path("/count")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Count result sets matching the provided filter")
    public ResponseDataObject<Long> getNumberOfResultSets(
            @QueryParam("filter") @DefaultValue("") FilterArg<ExpressionAnalysisResultSet> filter ) {
        return Responder.respond( expressionAnalysisResultSetService.count( expressionAnalysisResultSetArgService.getFilters( filter ) ) );
    }

    private static final String TSV_EXAMPLE = "# If you use this file for your research, please cite:\n" +
            "# Lim et al. (2021) Curation of over 10 000 transcriptomic studies to enable data reuse.\n" +
            "# Database, baab006 (doi:10.1093/database/baab006).\n" +
            "# Experimental factors: [name: genotype, values: [id: 7, characteristics: [wild type genotype], id: 8, characteristics: [Mfi2 [mouse] antigen p97 (melanoma associated) identified by monoclonal antibodies 133.2 and 96.5, Homozygous negative]]]\n" +
            "id\tprobe_id\tprobe_name\tgene_id\tgene_name\tgene_ncbi_id\tgene_official_symbol\tgene_official_name\tpvalue\tcorrected_pvalue\trank\tcontrast_8_log2fc\tcontrast_8_tstat\tcontrast_8_pvalue\n" +
            "2492126315\t86895\t1427283_at\t793757\tKmt2a\t214162\tKmt2a\tlysine (K)-specific methyltransferase 2A\t1.652E-1\t9.116E-1\t1.784E-1\t1.772E-1\t1.696E0\t1.652E-1\n" +
            "2492126316\t87072\t1427106_at\t721517\tZbtb3\t75291\tZbtb3\tzinc finger and BTB domain containing 3\t8.961E-1\t9.863E-1\t9.086E-1\t4.574E-2\t1.391E-1\t8.961E-1\n" +
            "2492126313\t89962\t1424216_a_at\t555147\tPapola\t18789\tPapola\tpoly (A) polymerase alpha\t4.174E-1\t9.183E-1\t4.544E-1\t-1.627E-1\t-9.033E-1\t4.174E-1\n" +
            "2492126314\t67610\t1446568_at\t\t\t\t\t\t9.137E-1\t9.888E-1\t9.241E-1\t1.099E-1\t1.154E-1\t9.137E-1\n" +
            "2492126319\t66807\t1447371_at\t863526\t9430037G07Rik\t320692\t9430037G07Rik\tRIKEN cDNA 9430037G07 gene\t8.41E-1\t9.763E-1\t8.614E-1\t6.666E-2\t2.14E-1\t8.41E-1\n" +
            "2492126320\t81508\t1432670_at\t\t\t\t\t\t9.086E-1\t9.878E-1\t9.198E-1\t1.773E-1\t1.222E-1\t9.086E-1\n" +
            "2492126317\t94081\t1420084_at\t\t\t\t\t\t8.125E-1\t9.726E-1\t8.354E-1\t-1.836E-1\t-2.533E-1\t8.125E-1\n" +
            "2492126318\t59156\t1455033_at\t868095\tFam102b\t329739\tFam102b\tfamily with sequence similarity 102, member B\t3.26E-1\t9.153E-1\t3.56E-1\t2.782E-1\t1.118E0\t3.26E-1\n" +
            "2492126307\t86209\t1427969_s_at\t699382\tZfp654\t72020\tZfp654\tzinc finger protein 654\t4.24E-1\t9.198E-1\t4.608E-1\t-1.774E-1\t-8.894E-1\t4.24E-1";

    /**
     * Retrieve a {@link AnalysisResultSet} given its identifier.
     */
    @GZIP
    @GET
    @Path("/{resultSet}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve a single analysis result set by its identifier", responses = {
            @ApiResponse(useReturnTypeSchema = true, content = @Content()),
            @ApiResponse(responseCode = "404", description = "The analysis result set could not be found.",
                    content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))) })
    public ResponseDataObject<DifferentialExpressionAnalysisResultSetValueObject> getResultSet(
            @PathParam("resultSet") ExpressionAnalysisResultSetArg analysisResultSet,
            @Parameter(hidden = true) @QueryParam("excludeResults") @DefaultValue("false") Boolean excludeResults ) {
        if ( excludeResults ) {
            ExpressionAnalysisResultSet ears = expressionAnalysisResultSetArgService.getEntity( analysisResultSet );
            return Responder.respond( expressionAnalysisResultSetService.loadValueObject( ears ) );
        } else {
            ExpressionAnalysisResultSet ears = analysisResultSet.getEntityWithContrastsAndResults( expressionAnalysisResultSetService );
            if ( ears == null ) {
                throw new NotFoundException( "Could not find ExpressionAnalysisResultSet for " + analysisResultSet + "." );
            }
            return Responder.respond( expressionAnalysisResultSetService.loadValueObjectWithResults( ears ) );
        }
    }

    /**
     * Retrieve an {@link AnalysisResultSet} in a tabular format.
     * <p>
     * This is intentionally using a slightly different parameter name for the {@link Path} to create a distinct entry
     * in the OpenAPI specification as a workaround to Swagger's codegen incapability to treat multiple media types per
     * endpoint.
     */
    @GZIP
    @GET
    @Path("/{resultSet_}")
    @Produces(TEXT_TAB_SEPARATED_VALUE_Q9_MEDIA_TYPE)
    @Operation(summary = "Retrieve a single analysis result set by its identifier as a tab-separated values", responses = {
            @ApiResponse(content = @Content(mediaType = MediaTypeUtils.TEXT_TAB_SEPARATED_VALUES_UTF8, /* no need to show the q-value in the endpoint */
                    schema = @Schema(type = "string", format = "binary"),
                    examples = { @ExampleObject(value = TSV_EXAMPLE) })),
            @ApiResponse(responseCode = "404", description = "The analysis result set could not be found.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ResponseErrorObject.class))) })
    public StreamingOutput getResultSetAsTsv(
            @PathParam("resultSet_") ExpressionAnalysisResultSetArg analysisResultSet ) {
        final ExpressionAnalysisResultSet ears = analysisResultSet.getEntityWithContrastsAndResults( expressionAnalysisResultSetService );
        if ( ears == null ) {
            throw new NotFoundException( "Could not find ExpressionAnalysisResultSet for " + analysisResultSet + "." );
        }
        final Map<Long, List<Gene>> resultId2Genes = expressionAnalysisResultSetService.loadResultIdToGenesMap( ears );
        return outputStream -> {
            try ( OutputStreamWriter writer = new OutputStreamWriter( outputStream ) ) {
                expressionAnalysisResultSetFileService.writeTsvToAppendable( ears, resultId2Genes, writer );
            }
        };
    }
}
