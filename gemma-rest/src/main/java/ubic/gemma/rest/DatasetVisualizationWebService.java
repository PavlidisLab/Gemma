/*
 * The Gemma project
 *
 * Copyright (c) 2026 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 */
package ubic.gemma.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.rest.annotations.GZIP;
import ubic.gemma.rest.util.MalformedArgException;
import ubic.gemma.rest.util.ResponseDataObject;
import ubic.gemma.rest.util.ResponseErrorObject;
import ubic.gemma.rest.util.Responders;
import ubic.gemma.rest.util.args.DatasetArg;
import ubic.gemma.rest.util.args.DatasetArgService;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Dataset visualization endpoints. Carved out of {@link DatasetsWebService} (already 4000+ LOC)
 * for the client-side heatmap rewrite. Sits alongside {@code /datasets/{id}/svd} and
 * {@code /datasets/{id}/data/processed}; ships raw matrix + meta with NO server-side ordering
 * decisions, NO colour assignment, NO legend assembly.
 * <p>
 * Locked decisions for the session-2 scaffold:
 * <ul>
 *   <li>Single-EE endpoint (client batches if needed; mirrors /svd).</li>
 *   <li>Continuous factors carry their per-sample measurements on the {@code factors[]} entry,
 *       keyed by bioAssayId.</li>
 *   <li>JSON {@code number[][]} encoding by default; {@code ?encoding=base64f32} opt-in for
 *       large matrices.</li>
 *   <li>Diffex requests set {@code rows[].pvalue} and {@code rows[].validated}; non-diffex
 *       requests leave both {@code null}.</li>
 * </ul>
 *
 * @author claude
 */
@Service
@Path("/datasets")
@Slf4j
public class DatasetVisualizationWebService {

    /** Hard upper bound on {@code ?sampleSize=}; mirrors the legacy DEDVController MAX_RESULTS_TO_RETURN. */
    static final int MAX_SAMPLE_SIZE = 150;
    static final int DEFAULT_SAMPLE_SIZE = 20;
    static final int DEFAULT_PCA_COUNT = 20;
    static final double DEFAULT_DIFFEX_THRESHOLD = 0.01;

    @Autowired
    private DatasetArgService datasetArgService;

    @Autowired
    private HeatmapDataService heatmapDataService;

    public DatasetVisualizationWebService() {
    }

    /**
     * Resolve raw matrix + metadata for a client-rendered heatmap. See class-level Javadoc.
     *
     * @param datasetArg   dataset id or short name; ACL-gated by the standard mechanism.
     * @param genesCsv     CSV of gene IDs (mutually exclusive with the other selection modes; takes precedence over them).
     * @param probesCsv    CSV of probe (composite sequence) IDs.
     * @param resultSetId  differential analysis result-set id; the endpoint returns top hits below {@code threshold}.
     * @param threshold    p-value threshold for diffex mode. Default {@value #DEFAULT_DIFFEX_THRESHOLD}.
     * @param pcaComponent PCA component (1-based); the endpoint returns the top-loaded probes for that component.
     * @param pcaCount     how many top-loaded probes per PCA component. Default {@value #DEFAULT_PCA_COUNT}.
     * @param sampleSize   fallback random sample size when no other selection mode is given. Default {@value #DEFAULT_SAMPLE_SIZE}, max {@value #MAX_SAMPLE_SIZE}.
     * @param encoding     {@code "json"} (default) or {@code "base64f32"}.
     */
    @GET
    @GZIP
    @Path("/{dataset}/heatmap-data")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "Retrieve raw matrix + metadata for a client-rendered heatmap",
            description = "Returns a probe×sample expression matrix plus per-row, per-column and per-factor metadata. "
                    + "Selection modes (mutually exclusive, listed in precedence order): (1) ?genes=csv, (2) ?probes=csv, "
                    + "(3) ?resultSet=N&threshold=p (diffex top hits), (4) ?pcaComponent=k&pcaCount=n (PCA-loaded probes), "
                    + "(5) default fallback: a random sample of ?sampleSize=n probes (default 20, max 150). "
                    + "NO ordering decisions are made server-side; the client sorts, groups, palettes, and renders.",
            responses = {
                    @ApiResponse(responseCode = "200", useReturnTypeSchema = true,
                            content = @Content(examples = @ExampleObject(value = "{...see restapidocs/examples/dataset-heatmap-data.json...}"))),
                    @ApiResponse(responseCode = "400", description = "Malformed query parameters.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))),
                    @ApiResponse(responseCode = "404", description = "The dataset does not exist.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))) })
    public ResponseDataObject<HeatmapDataValueObject> getDatasetHeatmapData(
            @PathParam("dataset") DatasetArg<?> datasetArg,
            @QueryParam("genes") @Nullable String genesCsv,
            @QueryParam("probes") @Nullable String probesCsv,
            @QueryParam("resultSet") @Nullable Long resultSetId,
            @QueryParam("threshold") @Nullable Double threshold,
            @QueryParam("pcaComponent") @Nullable Integer pcaComponent,
            @QueryParam("pcaCount") @Nullable Integer pcaCount,
            @QueryParam("sampleSize") @Nullable Integer sampleSize,
            @QueryParam("encoding") @DefaultValue("json") String encoding ) {
        if ( !"json".equalsIgnoreCase( encoding ) && !"base64f32".equalsIgnoreCase( encoding ) ) {
            throw new MalformedArgException( "encoding must be one of: json, base64f32" );
        }

        Collection<Long> geneIds = parseLongCsv( "genes", genesCsv );
        Collection<Long> probeIds = parseLongCsv( "probes", probesCsv );

        int effectiveSampleSize = sampleSize == null ? DEFAULT_SAMPLE_SIZE : Math.min( sampleSize, MAX_SAMPLE_SIZE );
        if ( effectiveSampleSize < 1 ) {
            throw new MalformedArgException( "sampleSize must be >= 1" );
        }
        int effectivePcaCount = pcaCount == null ? DEFAULT_PCA_COUNT : pcaCount;
        if ( effectivePcaCount < 1 ) {
            throw new MalformedArgException( "pcaCount must be >= 1" );
        }
        double effectiveThreshold = threshold == null ? DEFAULT_DIFFEX_THRESHOLD : threshold;

        ExpressionExperiment ee = datasetArgService.getEntity( datasetArg );

        HeatmapDataValueObject payload = heatmapDataService.buildHeatmapData(
                ee,
                geneIds,
                probeIds,
                resultSetId,
                effectiveThreshold,
                pcaComponent,
                effectivePcaCount,
                effectiveSampleSize,
                encoding.toLowerCase() );

        // For diffex-driven requests, flag rows whose pvalue meets the threshold as validated.
        // The legacy DEDVController computed this via a separate getProbeDiffExValidation pass;
        // here the diffex vector path returns vectors with pvalue already attached, so the
        // threshold check is sufficient for the client-side highlight.
        if ( resultSetId != null && payload.getRows() != null ) {
            for ( HeatmapDataValueObject.RowMeta row : payload.getRows() ) {
                Double p = row.getPvalue();
                row.setValidated( p != null && p <= effectiveThreshold );
            }
        }

        return Responders.respond( payload );
    }

    @Nullable
    private Collection<Long> parseLongCsv( String paramName, @Nullable String csv ) {
        if ( StringUtils.isBlank( csv ) ) {
            return null;
        }
        String[] parts = csv.split( "," );
        List<Long> out = new ArrayList<>( parts.length );
        for ( String p : parts ) {
            String trimmed = p.trim();
            if ( trimmed.isEmpty() ) continue;
            try {
                out.add( Long.parseLong( trimmed ) );
            } catch ( NumberFormatException e ) {
                throw new MalformedArgException( paramName + " must be a comma-separated list of integers; offending value: " + trimmed );
            }
        }
        return out.isEmpty() ? Collections.emptyList() : out;
    }
}
