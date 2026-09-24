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
         * An NCBI gene id that NCBI itself still carries, but as a WITHDRAWN or secondary record: the
         * annotation names a gene NCBI has retired. Distinct from {@link #URI_UNRESOLVED} because the id
         * was real — the fix is the successor record, not a different gene.
         */
        GENE_WITHDRAWN,
        /**
         * A gene id absent from Gemma's gene table that NCBI could not be reached to check — unverified,
         * not proven bad, and retryable. The NCBI sibling of {@link #UNVERIFIED_OLS_UNAVAILABLE}: a client
         * retry loop keyed on the 400 status alone will spin on the two reasons above, which are not.
         */
        UNVERIFIED_NCBI_UNAVAILABLE,
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
        UNGROUNDED_NOT_DECLARED,
        /**
         * A free-text experiment tag that hangs off nothing: no statement on it carries both a predicate
         * and a grounded object, so the annotation is attached to the ontology at no point.
         * <p>
         * Paul's ruling, 2026-09-06: a free-text tag must give the reader some grounded context for the
         * text — {@code cell line: WTC-11} means nothing to a query, while the same value plus
         * {@code derives from cell line cell -> induced pluripotent stem cell line cell} is reachable
         * from the ontology even though the identifier itself has no term.
         * <p>
         * Separate from {@link #UNGROUNDED_NOT_DECLARED} on purpose: that one asks whether the missing
         * URI was a decision, this one asks whether the annotation is connected to anything. A caller can
         * get either right and the other wrong, so both are checked and neither substitutes for the other.
         * <p>
         * 🛑 Experiment tags only. A factor value or a sample characteristic may be bare free text — a GEO
         * characteristic is a string the submitter wrote, and requiring a hook there would refuse the corpus.
         */
        FREE_TEXT_NOT_HOOKED
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
            case UNGROUNDED_NOT_DECLARED:
                // These two carry no URI by definition, so the default's "<slot> URI null" said nothing.
                return slot + " \"" + submittedLabel + "\" carries no URI, and the item does not set"
                        + " freeTextIntended to say the free text was meant";
            case FREE_TEXT_NOT_HOOKED:
                return "free-text tag \"" + submittedLabel + "\" has no statement carrying both a predicate"
                        + " and a grounded object, so it is attached to the ontology at no point; give it a"
                        + " hook (e.g. derives from cell line cell -> <parent line URI>) or ground the value";
            default:
                return slot + " URI " + submittedUri;
        }
    }
}
