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

import java.io.Serializable;
import java.util.Objects;

/**
 * A term resolved from the EBI Ontology Lookup Service by its IRI.
 * <p>
 * Carries a {@link #found} flag so a negative lookup (OLS knows no term for the IRI) can be cached
 * alongside positive ones without a null-value sentinel.
 *
 * @author gemma
 * @see OlsTermResolver
 */
public class OlsTerm implements Serializable {

    /**
     * A term OLS could not resolve. {@link #getLabel()} is {@code null}.
     */
    public static OlsTerm notFound( String iri ) {
        return new OlsTerm( iri, null, false );
    }

    private final String iri;
    @Nullable
    private final String label;
    private final boolean found;

    public OlsTerm( String iri, @Nullable String label ) {
        this( iri, label, true );
    }

    private OlsTerm( String iri, @Nullable String label, boolean found ) {
        this.iri = iri;
        this.label = label;
        this.found = found;
    }

    public String getIri() {
        return iri;
    }

    /**
     * The term's canonical label, or {@code null} when {@link #isFound()} is {@code false}.
     */
    @Nullable
    public String getLabel() {
        return label;
    }

    /**
     * Whether OLS resolved a term for the IRI.
     */
    public boolean isFound() {
        return found;
    }

    @Override
    public boolean equals( Object o ) {
        if ( this == o )
            return true;
        if ( !( o instanceof OlsTerm ) )
            return false;
        OlsTerm olsTerm = ( OlsTerm ) o;
        return found == olsTerm.found && Objects.equals( iri, olsTerm.iri ) && Objects.equals( label, olsTerm.label );
    }

    @Override
    public int hashCode() {
        return Objects.hash( iri, label, found );
    }

    @Override
    public String toString() {
        return found ? iri + " (" + label + ")" : iri + " (not found in OLS)";
    }
}
