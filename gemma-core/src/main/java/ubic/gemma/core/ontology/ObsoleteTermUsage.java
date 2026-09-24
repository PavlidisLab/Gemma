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
 * One obsolete ontology term that Gemma's annotations still use, together with whatever the owning ontology says
 * should replace it.
 * <p>
 * The distinction between {@link #replacedByUri} and {@link #considerUris} is the whole point of this object, and
 * it is the difference between a correction a machine may apply and one it may not:
 * <ul>
 *     <li>{@code IAO:0100001 term replaced by} is the ontology asserting an exact substitute. EFO says
 *     {@code EFO_0000408} (obsolete_disease) is replaced by {@code MONDO_0000001}. Following that assertion is
 *     not a guess — it is reading the answer the ontology already published.</li>
 *     <li>{@code oboInOwl:consider} is a suggestion to a human being. It carries no claim of equivalence and may
 *     list several candidates. Applying one automatically would be inventing curation.</li>
 * </ul>
 *
 * @author phase 3 ontology maintenance
 */
@Getter
@Setter
@Schema(description = "An obsolete ontology term still used by Gemma annotations, with the replacement its ontology asserts (if any).")
public class ObsoleteTermUsage implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "URI of the obsolete term, as it appears in Gemma's annotations.")
    private String uri;

    @Schema(description = "Label of the obsolete term according to the ontology, e.g. \"obsolete_disease\".")
    @Nullable
    private String label;

    @Schema(description = "The label Gemma has stored against this URI, which is typically the pre-obsolescence one.")
    @Nullable
    private String storedValue;

    @Schema(description = "URI the ontology asserts as an exact replacement via IAO:0100001 (`term replaced by`), or null if it asserts none.")
    @Nullable
    private String replacedByUri;

    @Schema(description = "Label of the replacement term.")
    @Nullable
    private String replacedByLabel;

    @Schema(description = "URIs offered by oboInOwl:consider. These are suggestions for a curator, NOT assertions of equivalence, and are never applied automatically.")
    private List<String> considerUris = new ArrayList<>();

    @Schema(description = "Number of experiments carrying an annotation with this URI, in any of the category, subject, predicate or object slots.")
    private long experimentCount;

    /**
     * Whether the term is used as a CATEGORY. This is a different repair from a stale value and worth knowing
     * before acting: {@code EFO_0000408} is the "disease" category on a large share of the corpus, so correcting it
     * rewrites the shape of the annotation rather than one of its terms.
     */
    @Schema(description = "True when the term is used in the CATEGORY slot of at least one characteristic.")
    private boolean usedAsCategory;

    @Schema(description = "True when the term is used as a value, predicate or object.")
    private boolean usedAsTerm;

    @Schema(description = "True when the ontology asserts a replacement that itself resolves and is not obsolete — i.e. the correction can be derived rather than decided. False means a curator has to choose.")
    private boolean autoCorrectable;

    /**
     * Which mechanical rule produced {@link #replacedByUri}. This is what gets written into the corrected
     * characteristic's {@code supportingEvidence} as {@code assertedBy}, so a later reader can tell a derived
     * correction from a curator's decision, and tell which derivation was used.
     */
    @Schema(description = "The mechanical rule that produced the replacement: `IAO:0100001` (directly asserted), "
            + "`IAO:0100001-chain` (asserted, reached by following obsolete intermediates), or `hasAlternativeId` "
            + "(the obsolete term was merged into the replacement, which records it as an alternative ID). "
            + "Null when nothing resolved it.")
    @Nullable
    private String resolvedVia;

    @Schema(description = "Number of replaced-by hops followed to reach the replacement. 1 is a direct assertion.")
    private int replacementHops;

    @Schema(description = "Why this term is not auto-correctable; null when it is.")
    @Nullable
    private String blockedReason;
}
