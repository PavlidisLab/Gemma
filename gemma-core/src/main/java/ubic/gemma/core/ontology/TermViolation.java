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
 * A single ontology-term grounding failure found by {@link OntologyTermValidator}: one term slot
 * (category / value / predicate / object / …) whose URI does not resolve, or resolves to a term whose label
 * disagrees with the submitted one.
 *
 * @author gemma
 */
public class TermViolation {

    public enum Reason {
        /**
         * The URI resolves (locally or via OLS) but the submitted label is not the term's label — the
         * signature of a hallucinated or mis-copied term.
         */
        LABEL_MISMATCH,
        /**
         * The URI resolves in neither Gemma's loaded ontologies nor OLS, and is not an allow-listed
         * non-ontology URI — a fabricated / ungrounded term.
         */
        URI_UNRESOLVED,
        /**
         * The URI is unknown locally and OLS could not be reached to check it — unverified, not proven bad.
         */
        UNVERIFIED_OLS_UNAVAILABLE,
        /**
         * The term carries no URI at all and the caller did not declare that it meant to leave it as free
         * text.
         * <p>
         * Distinct from {@link #URI_UNRESOLVED}, which is a URI that does not check out: this is the
         * absence of one. It is refused by default because an experiment tag with no URI is usually an
         * oversight rather than a decision, and it is indistinguishable after the fact from a grounding
         * the client meant to do and forgot. A caller that means it says so per item, and the tag is then
         * accepted.
         */
        UNGROUNDED_NOT_DECLARED
    }

    private final String slot;
    private final String submittedLabel;
    @Nullable
    private final String submittedUri;
    @Nullable
    private final String resolvedLabel;
    private final Reason reason;

    public TermViolation( String slot, @Nullable String submittedLabel, @Nullable String submittedUri, @Nullable String resolvedLabel, Reason reason ) {
        this.slot = slot;
        this.submittedLabel = submittedLabel;
        this.submittedUri = submittedUri;
        this.resolvedLabel = resolvedLabel;
        this.reason = reason;
    }

    /**
     * The slot that failed: one of {@code category}, {@code value}, {@code predicate}, {@code object},
     * {@code secondPredicate}, {@code secondObject}.
     */
    public String getSlot() {
        return slot;
    }

    @Nullable
    public String getSubmittedLabel() {
        return submittedLabel;
    }

    @Nullable
    public String getSubmittedUri() {
        return submittedUri;
    }

    /**
     * The canonical label the URI actually resolves to; set for {@link Reason#LABEL_MISMATCH}, {@code null}
     * otherwise (there is nothing to resolve to).
     */
    @Nullable
    public String getResolvedLabel() {
        return resolvedLabel;
    }

    public Reason getReason() {
        return reason;
    }

    @Override
    public String toString() {
        switch ( reason ) {
            case LABEL_MISMATCH:
                return slot + " URI " + submittedUri + " resolves to \"" + resolvedLabel
                        + "\", not the submitted label \"" + submittedLabel + "\"";
            case URI_UNRESOLVED:
                return slot + " URI " + submittedUri + " (label \"" + submittedLabel
                        + "\") resolves in neither Gemma nor OLS; the term is not grounded";
            case UNVERIFIED_OLS_UNAVAILABLE:
                return slot + " URI " + submittedUri + " is unknown to Gemma and OLS could not be reached to verify it";
            default:
                return slot + " URI " + submittedUri;
        }
    }
}
