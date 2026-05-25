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
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import ubic.gemma.core.ontology.OntologyUtils;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.common.protocol.Protocol;
import ubic.gemma.model.expression.arrayDesign.AlternateName;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.common.description.CharacteristicReadService;
import ubic.gemma.persistence.service.common.protocol.ProtocolReadService;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentSetService;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;
import ubic.gemma.rest.util.ResponseDataObject;
import ubic.gemma.rest.util.ResponseErrorObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import static ubic.gemma.rest.util.Responders.respond;

/**
 * Prefix-aware completion suggestions for CLI / curation-UI / REST clients.
 * <p>
 * Endpoints accept an optional case-insensitive {@code prefix} and a {@code limit}
 * (default {@value #DEFAULT_LIMIT}, hard-capped at {@value #MAX_LIMIT}) and return
 * a flat list of {@code { value, description }} tuples ordered by the underlying
 * service's natural order. Empty prefix returns the first {@code limit} entries.
 * <p>
 * Mirrors the lookups performed by {@code gemma-cli complete} (see
 * {@code ubic.gemma.apps.CompleteCli}) so the REST API can replace the slow
 * cold-start CLI path for completions consumed by curation-UI typeaheads and
 * by the shell-completion scripts via the loopback REST.
 *
 * @author claude (PavlidisLab/Gemma#1611)
 */
@Service
@Path("/completions")
@Slf4j
public class CompletionsWebService {

    /** Default {@code limit} when the client does not supply one. */
    public static final int DEFAULT_LIMIT = 50;
    /** Hard cap on {@code limit}; requests above this are clamped down. */
    public static final int MAX_LIMIT = 500;

    @Autowired
    private TaxonService taxonService;
    @Autowired
    private ArrayDesignService arrayDesignService;
    @Autowired
    private ExpressionExperimentService expressionExperimentService;
    @Autowired
    private ExpressionExperimentSetService expressionExperimentSetService;
    @Autowired
    private ProtocolReadService protocolReadService;
    @Autowired
    private CharacteristicReadService characteristicService;

    @GET
    @Path("/taxa")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Prefix-aware completions for taxa",
            description = "Returns matching taxa as { value, description } pairs. Each taxon "
                    + "contributes up to four entries (id, ncbiId, commonName, scientificName); "
                    + "duplicates are dropped.",
            responses = @ApiResponse(responseCode = "200", useReturnTypeSchema = true, content = @Content()))
    public ResponseDataObject<List<CompletionValueObject>> getTaxonCompletions(
            @Parameter(description = "Case-insensitive prefix to match against the candidate value.")
            @QueryParam("prefix") @DefaultValue("") String prefix,
            @Parameter(description = "Maximum number of suggestions to return; capped at " + MAX_LIMIT + ".")
            @QueryParam("limit") @DefaultValue("" + DEFAULT_LIMIT) int limit ) {
        Builder b = new Builder( prefix, limit );
        for ( Taxon taxon : taxonService.loadAll() ) {
            String description = taxon.getScientificName();
            b.add( String.valueOf( taxon.getId() ), description );
            if ( taxon.getNcbiId() != null ) {
                b.add( String.valueOf( taxon.getNcbiId() ), description );
            }
            if ( taxon.getCommonName() != null ) {
                b.add( taxon.getCommonName(), description );
            }
            if ( taxon.getScientificName() != null ) {
                b.add( taxon.getScientificName(), description );
            }
            if ( b.full() ) break;
        }
        return respond( b.build() );
    }

    @GET
    @Path("/platforms")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Prefix-aware completions for array-design platforms",
            description = "Returns matching platforms. Each platform contributes id, shortName, name, "
                    + "and any alternate names. Pass {@code generic=true} to restrict to platforms that "
                    + "are valid as 'generic gene' targets.",
            responses = @ApiResponse(responseCode = "200", useReturnTypeSchema = true, content = @Content()))
    public ResponseDataObject<List<CompletionValueObject>> getPlatformCompletions(
            @QueryParam("prefix") @DefaultValue("") String prefix,
            @QueryParam("limit") @DefaultValue("" + DEFAULT_LIMIT) int limit,
            @Parameter(description = "If true, restrict the candidate set to generic-gene-capable platforms.")
            @QueryParam("generic") @DefaultValue("false") boolean generic ) {
        Builder b = new Builder( prefix, limit );
        Collection<ArrayDesign> ads = generic ? arrayDesignService.loadAllGenericGenePlatforms() : arrayDesignService.loadAll();
        for ( ArrayDesign ad : ads ) {
            String description = ad.getName();
            b.add( String.valueOf( ad.getId() ), description );
            b.add( ad.getShortName(), description );
            b.add( description, description );
            for ( AlternateName alternateName : ad.getAlternateNames() ) {
                b.add( alternateName.getName(), description );
            }
            if ( b.full() ) break;
        }
        return respond( b.build() );
    }

    @GET
    @Path("/protocols")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Prefix-aware completions for protocols",
            responses = @ApiResponse(responseCode = "200", useReturnTypeSchema = true, content = @Content()))
    public ResponseDataObject<List<CompletionValueObject>> getProtocolCompletions(
            @QueryParam("prefix") @DefaultValue("") String prefix,
            @QueryParam("limit") @DefaultValue("" + DEFAULT_LIMIT) int limit ) {
        Builder b = new Builder( prefix, limit );
        for ( Protocol protocol : protocolReadService.loadAllUniqueByName() ) {
            b.add( String.valueOf( protocol.getId() ), protocol.getName() );
            b.add( protocol.getName(), protocol.getName() );
            if ( b.full() ) break;
        }
        return respond( b.build() );
    }

    @GET
    @Path("/dataset-groups")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Prefix-aware completions for dataset (expression-experiment) groups",
            responses = @ApiResponse(responseCode = "200", useReturnTypeSchema = true, content = @Content()))
    public ResponseDataObject<List<CompletionValueObject>> getDatasetGroupCompletions(
            @QueryParam("prefix") @DefaultValue("") String prefix,
            @QueryParam("limit") @DefaultValue("" + DEFAULT_LIMIT) int limit ) {
        Builder b = new Builder( prefix, limit );
        for ( ExpressionExperimentSet eeSet : expressionExperimentSetService.loadAll() ) {
            b.add( String.valueOf( eeSet.getId() ), eeSet.getName() );
            b.add( eeSet.getName(), eeSet.getName() );
            if ( b.full() ) break;
        }
        return respond( b.build() );
    }

    @GET
    @Path("/datasets")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Prefix-aware completions for datasets (expression experiments)",
            description = "Returns matching dataset identifiers (id, short name) with the dataset's "
                    + "short name as description. Backed by ExpressionExperimentService.loadAllIdentifiersAndName "
                    + "so the lookup avoids fetching full VOs.",
            responses = @ApiResponse(responseCode = "200", useReturnTypeSchema = true, content = @Content()))
    public ResponseDataObject<List<CompletionValueObject>> getDatasetCompletions(
            @QueryParam("prefix") @DefaultValue("") String prefix,
            @QueryParam("limit") @DefaultValue("" + DEFAULT_LIMIT) int limit ) {
        Builder b = new Builder( prefix, limit );
        for ( var entry : expressionExperimentService.loadAllIdentifiersAndName( false ).entrySet() ) {
            b.add( entry.getKey(), entry.getValue() );
            if ( b.full() ) break;
        }
        return respond( b.build() );
    }

    @GET
    @Path("/ontology-terms")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Prefix-aware completions for ontology terms used as Characteristic values",
            description = "Returns matching ontology terms by URI and, where applicable, by OBO short ID. "
                    + "Backed by CharacteristicReadService.findValueGroupedByValueUri.",
            responses = @ApiResponse(responseCode = "200", useReturnTypeSchema = true, content = @Content()))
    public ResponseDataObject<List<CompletionValueObject>> getOntologyTermCompletions(
            @QueryParam("prefix") @DefaultValue("") String prefix,
            @QueryParam("limit") @DefaultValue("" + DEFAULT_LIMIT) int limit ) {
        Builder b = new Builder( prefix, limit );
        characteristicService.findValueGroupedByValueUri( null, true, false, true, -1 )
                .forEach( ( uri, label ) -> {
                    if ( b.full() ) return;
                    b.add( uri, label );
                    if ( OntologyUtils.isTermUri( uri ) ) {
                        try {
                            b.add( OntologyUtils.uriToTermId( uri ), label );
                        } catch ( IllegalArgumentException e ) {
                            // Skip malformed URIs silently; the CLI logs them.
                        }
                    }
                } );
        return respond( b.build() );
    }

    /**
     * Wire shape for a single completion suggestion.
     * <p>
     * Mirrors the TSV {@code value\tdescription} lines emitted by {@code gemma-cli complete} so
     * REST clients and CLI consumers share a common surface.
     */
    @Value
    public static class CompletionValueObject {
        /**
         * The candidate completion value (id, short name, URI, etc. — varies by completion type).
         */
        String value;
        /**
         * Human-readable label suitable for UI display alongside the value.
         */
        @Nullable
        String description;
    }

    /**
     * Local accumulator: prefix-matches, dedup-by-value, hard-caps at {@code limit}.
     * Case-insensitive matching uses the default JVM locale folding via {@link Locale#ROOT}.
     */
    private static final class Builder {
        private final String lowerPrefix;
        private final int limit;
        private final List<CompletionValueObject> out;
        private final java.util.Set<String> seen = new java.util.HashSet<>();

        Builder( String prefix, int requestedLimit ) {
            this.lowerPrefix = prefix == null ? "" : prefix.toLowerCase( Locale.ROOT );
            this.limit = Math.max( 0, Math.min( requestedLimit, MAX_LIMIT ) );
            this.out = new ArrayList<>( Math.min( 64, this.limit ) );
        }

        void add( @Nullable String value, @Nullable String description ) {
            if ( value == null || full() ) return;
            if ( !lowerPrefix.isEmpty() && !value.toLowerCase( Locale.ROOT ).startsWith( lowerPrefix ) ) return;
            if ( !seen.add( value ) ) return;
            out.add( new CompletionValueObject( value, description ) );
        }

        boolean full() {
            return out.size() >= limit;
        }

        List<CompletionValueObject> build() {
            return out;
        }
    }
}
