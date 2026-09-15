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

/**
 * Raised when the EBI Ontology Lookup Service cannot be reached or returns an unexpected response — a
 * transient condition, distinct from OLS successfully reporting that it has no term for an IRI (which is a
 * {@code null} return from {@link OlsTermResolver#resolve(String)}).
 * <p>
 * Callers should treat this as "could not verify", not "the term is bad": a URI that Gemma can't resolve
 * locally and can't check against OLS is unverified, not proven wrong.
 *
 * @author gemma
 */
public class OlsUnavailableException extends Exception {

    public OlsUnavailableException( String message ) {
        super( message );
    }

    public OlsUnavailableException( String message, Throwable cause ) {
        super( message, cause );
    }
}
