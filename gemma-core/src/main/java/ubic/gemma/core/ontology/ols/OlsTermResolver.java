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
package ubic.gemma.core.ontology.ols;

import org.springframework.lang.Nullable;

/**
 * Resolves an ontology term IRI against the EBI Ontology Lookup Service
 * (<a href="https://www.ebi.ac.uk/ols4">ols4</a>).
 * <p>
 * Used as a fallback when validating a term URI that Gemma has not loaded into memory: OLS carries a far
 * larger set of ontologies than any single Gemma instance loads, so a legitimate term from an unloaded
 * ontology still resolves here rather than being mistaken for a fabricated one.
 *
 * @author gemma
 */
public interface OlsTermResolver {

    /**
     * Resolve a term IRI against OLS.
     *
     * @param iri a full term IRI (e.g. {@code http://purl.obolibrary.org/obo/EFO_0000270}).
     * @return the resolved {@link OlsTerm}, or {@code null} if OLS has no term for the IRI.
     * @throws OlsUnavailableException if OLS cannot be reached or returns an unexpected response — a
     *                                 transient failure, NOT a determination that the IRI is invalid.
     */
    @Nullable
    OlsTerm resolve( String iri ) throws OlsUnavailableException;
}
