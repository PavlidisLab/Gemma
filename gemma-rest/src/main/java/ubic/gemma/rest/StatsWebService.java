/*
 * The Gemma project
 *
 * Copyright (c) 2026 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package ubic.gemma.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.ServiceUnavailableException;
import jakarta.ws.rs.core.MediaType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import ubic.gemma.core.analysis.report.HomeStats;
import ubic.gemma.core.analysis.report.HomeStatsService;
import ubic.gemma.rest.annotations.CacheControl;
import ubic.gemma.rest.util.ResponseDataObject;
import ubic.gemma.rest.util.ResponseErrorObject;

import java.util.Date;

import static ubic.gemma.rest.util.Responders.respond;

/**
 * Public statistics endpoints used by the Gemma home page and similar landing widgets.
 * The snapshot is precomputed daily by
 * {@code ubic.gemma.core.analysis.report.HomeStatsRefresher} so individual requests do
 * not pay the aggregation cost.
 */
@Service
@Path("/stats")
@Slf4j
public class StatsWebService {

    @Autowired
    private HomeStatsService homeStatsService;

    @GET
    @Path("/home")
    @CacheControl(maxAge = 3600)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve cached statistics for the public home page",
            description = "Returns datasets / platforms / samples counts, per-taxon and per-platform-technology breakdowns, "
                    + "and the most-recently-curated experiments. The snapshot is recomputed daily; for a forced refresh, see POST /stats/home/refresh.",
            responses = {
                    @ApiResponse(responseCode = "200", useReturnTypeSchema = true, content = @Content()),
                    @ApiResponse(responseCode = "503", description = "Snapshot not yet generated; retry shortly.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class)))
            })
    public ResponseDataObject<HomeStats> getHomeStats() {
        HomeStats cached = homeStatsService.getCached();
        if ( cached == null ) {
            throw new ServiceUnavailableException( DateUtils.addMinutes( new Date(), 5 ) );
        }
        return respond( cached );
    }

    @POST
    @Path("/home/refresh")
    @PreAuthorize("hasAuthority('GROUP_ADMIN')")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Force a refresh of the home-page statistics snapshot",
            description = "Recomputes the daily snapshot synchronously and returns the new value. Admin-only.",
            security = {
                    @SecurityRequirement(name = "basicAuth", scopes = { "GROUP_ADMIN" }),
                    @SecurityRequirement(name = "cookieAuth", scopes = { "GROUP_ADMIN" })
            })
    public ResponseDataObject<HomeStats> refreshHomeStats() {
        log.info( "Admin-triggered HomeStats refresh" );
        return respond( homeStatsService.refresh() );
    }
}
