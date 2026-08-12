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
package ubic.gemma.core.geoscrape;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.lang.Nullable;

import java.util.List;

/**
 * Candidate returned from a dry-run GEO scrape. Wire shape mirrors the
 * {@code PreboardedExperiment} JSON used by {@code GET /preboarded/{id}}
 * so the gemma-curation-agents service can mock-persist these as-if they
 * had been produced by a production scrape — flip the dryRun flag off
 * later and no downstream change is needed.
 *
 * <p>Lifecycle fields ({@code preboardedId}, {@code enteredCurrentStateAt},
 * {@code auditTrailUrl}) are always {@code null} for dry-run candidates
 * since nothing is persisted.</p>
 */
public class GeoScrapeDryRunCandidate {

    @JsonProperty("preboardedId")
    @Nullable
    public Long preboardedId;

    public String accession;

    public String source;

    @JsonProperty("identifyingMetadata")
    @Nullable
    public String identifyingMetadata;

    public String state;

    @JsonProperty("enteredCurrentStateAt")
    @Nullable
    public java.util.Date enteredCurrentStateAt;

    @JsonProperty("proposalCount")
    public long proposalCount;

    @JsonProperty("latestProposal")
    @Nullable
    public Object latestProposal;

    @JsonProperty("auditTrailUrl")
    @Nullable
    public String auditTrailUrl;

    @JsonProperty("matchedCriteria")
    public List<String> matchedCriteria;
}
