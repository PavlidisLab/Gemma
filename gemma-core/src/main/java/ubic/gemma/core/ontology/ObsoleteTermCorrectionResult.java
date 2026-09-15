/*
 * The Gemma project
 *
 * Copyright (c) 2026 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package ubic.gemma.core.ontology;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.springframework.lang.Nullable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * What an obsolete-term correction run did, or — in a dry run — would do.
 * <p>
 * A dry run and a live run return the same shape and the same numbers; {@link #dryRun} is the only difference.
 * That is deliberate: the point of the rehearsal is to be able to compare it against the real thing.
 *
 * @author phase 3 ontology maintenance
 */
@Getter
@Setter
@Schema(description = "Outcome of an obsolete-term correction run. A dry run reports the same counts it would have written.")
public class ObsoleteTermCorrectionResult implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "True when nothing was written.")
    private boolean dryRun;

    @Schema(description = "Per-term detail, ordered as applied.")
    private List<TermCorrection> terms = new ArrayList<>();

    @Schema(description = "Distinct experiments touched across all terms.")
    private int experimentsAffected;

    @Schema(description = "Characteristic rows rewritten across all terms.")
    private int characteristicsRewritten;

    @Schema(description = "Terms skipped because they are deferred; name a term explicitly in `uris` to act on it anyway.")
    private List<String> skippedDeferred = new ArrayList<>();

    @Schema(description = "Terms requested that were not auto-correctable, and why.")
    private List<String> skippedNotCorrectable = new ArrayList<>();

    @Schema(description = "Denormalization resync outcome; null on a dry run.")
    @Nullable
    private Resync resync;

    @Getter
    @Setter
    @Schema(description = "One obsolete term's correction.")
    public static class TermCorrection implements Serializable {
        private static final long serialVersionUID = 1L;

        private String fromUri;
        @Nullable
        private String fromLabel;
        private String toUri;
        @Nullable
        private String toLabel;
        @Schema(description = "The mechanical rule the replacement came from, recorded as `assertedBy` in supportingEvidence.")
        @Nullable
        private String resolvedVia;
        @Schema(description = "Characteristic rows rewritten for this term.")
        private int characteristicsRewritten;
        @Schema(description = "How many rewrites landed in each slot. A term can occupy more than one.")
        private int inCategory, inValue, inPredicate, inObject;
        private int experimentsAffected;
    }

    @Getter
    @Setter
    @Schema(description = "The denormalized tables rebuilt after the rewrite, per affected experiment.")
    public static class Resync implements Serializable {
        private static final long serialVersionUID = 1L;

        private int experimentsResynced;
        private int ee2cRowsWritten;
        private int annotationRelationRowsWritten;
        @Schema(description = "Experiments whose resync threw; the rewrite still stands, so re-run the EE2C update for these.")
        private List<String> resyncFailures = new ArrayList<>();
    }
}
