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
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ubic.gemma.core.ontology;

import org.springframework.lang.Nullable;

/**
 * A single in-place rewrite that {@link OntologyTermValidator} applied to a slot of a
 * {@link ubic.gemma.model.common.description.Characteristic} while grounding it — the slot still passes (it
 * is not a {@link TermViolation}), but what was stored differs from what was submitted, so a client can echo
 * the correction back to its display. Covers three cases (independently or together):
 * <ul>
 *     <li>a case/whitespace-only near-match label rewritten to the term's canonical label;</li>
 *     <li>a blank label filled in from its URI;</li>
 *     <li>a known Gemma-ontology term (e.g. {@code TGEMO_*}) whose URI arrived on a foreign base and was
 *     normalized to the canonical Gemma base.</li>
 * </ul>
 * {@code submittedLabel}/{@code submittedUri} record what arrived; {@code canonicalLabel}/{@code canonicalUri}
 * record what was stored. A field pair being equal means that dimension was unchanged.
 *
 * @author gemma
 */
public class TermCanonicalization {

    private final String slot;
    @Nullable
    private final String submittedLabel;
    private final String canonicalLabel;
    private final String submittedUri;
    private final String canonicalUri;

    public TermCanonicalization( String slot, @Nullable String submittedLabel, String canonicalLabel, String submittedUri, String canonicalUri ) {
        this.slot = slot;
        this.submittedLabel = submittedLabel;
        this.canonicalLabel = canonicalLabel;
        this.submittedUri = submittedUri;
        this.canonicalUri = canonicalUri;
    }

    /**
     * The slot that was rewritten: one of {@code category}, {@code value}, {@code predicate}, {@code object},
     * {@code secondPredicate}, {@code secondObject}.
     */
    public String getSlot() {
        return slot;
    }

    /** The label as submitted; {@code null}/blank when a URI arrived with no label and one was filled in. */
    @Nullable
    public String getSubmittedLabel() {
        return submittedLabel;
    }

    /** The term's canonical label, now stored (equals {@link #getSubmittedLabel()} when only the URI changed). */
    public String getCanonicalLabel() {
        return canonicalLabel;
    }

    /** The URI as submitted. */
    public String getSubmittedUri() {
        return submittedUri;
    }

    /** The URI now stored (equals {@link #getSubmittedUri()} when only the label changed). */
    public String getCanonicalUri() {
        return canonicalUri;
    }
}
