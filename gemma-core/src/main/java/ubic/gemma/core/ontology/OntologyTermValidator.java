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

import ubic.gemma.model.common.description.Characteristic;

import java.util.List;

/**
 * Validates the ontology terms carried by a {@link Characteristic} (or {@link ubic.gemma.model.expression.experiment.Statement})
 * against the terms Gemma actually knows about — the last checkpoint before a curator's or an agent's
 * annotation is persisted, so a hallucinated URI or a label that doesn't match its URI is caught here
 * rather than becoming stored data.
 * <p>
 * For each URI-bearing slot (category, value/subject, predicate, object, secondPredicate, secondObject) the
 * URI is resolved against Gemma's loaded ontologies first, then against
 * {@link ubic.gemma.core.ontology.ols.OlsTermResolver OLS} as a fallback. The label is then compared to the
 * resolved term's label. A slot passes when the URI is blank (free text), the URI is an allow-listed
 * non-ontology URI (e.g. an NCBI gene), or the label matches (exactly, or after case/whitespace
 * normalization — in which case the stored label is rewritten to the canonical form). Everything else is a
 * {@link TermViolation}.
 *
 * @author gemma
 */
public interface OntologyTermValidator {

    /**
     * Validate every URI-bearing slot of a characteristic, rewriting accepted case/whitespace near-matches to
     * their canonical labels in place.
     *
     * @param c the characteristic (possibly a {@link ubic.gemma.model.expression.experiment.Statement}) to
     *          validate; may be mutated to canonicalize near-match labels.
     * @return the violations found, in slot order; empty when every slot is grounded.
     */
    List<TermViolation> validateAndCanonicalize( Characteristic c );
}
