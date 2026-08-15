/*
 * The basecode project
 *
 * Copyright (c) 2007-2019 Columbia University
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
 *
 */
package ubic.gemma.core.ontology.model;

import javax.annotation.Nullable;
import java.util.Collection;

/**
 * @author Paul
 */
public interface OntologyTerm extends OntologyResource {

    /**
     * Obtain alternative IDs for this term.
     */
    Collection<String> getAlternativeIds();

    /**
     * Obtain all annotations for this term.
     */
    Collection<AnnotationProperty> getAnnotations();

    /**
     * Obtain all the annotations for a given property URI.
     */
    Collection<AnnotationProperty> getAnnotations( String propertyUri );

    /**
     * Obtain an annotation by property URI.
     */
    @Nullable
    AnnotationProperty getAnnotation( String propertyUri );

    /**
     * Obtain the children of this term via subclasses and additional properties.
     *
     * @see #getChildren(boolean, boolean)
     */
    default Collection<OntologyTerm> getChildren( boolean direct ) {
        return getChildren( direct, true, false );
    }

    default Collection<OntologyTerm> getChildren( boolean direct, boolean includeAdditionalProperties ) {
        return getChildren( direct, includeAdditionalProperties, false );
    }

    /**
     * Obtain the children of this term via subclass relationships and possibly some additional properties.
     *
     * @param direct                      return only the immediate children; if false, return all of them down to the leaves.
     * @param includeAdditionalProperties include terms matched via additional properties
     */
    Collection<OntologyTerm> getChildren( boolean direct, boolean includeAdditionalProperties, boolean keepObsoletes );

    default Collection<OntologyIndividual> getIndividuals() {
        return getIndividuals( true );
    }

    Collection<OntologyIndividual> getIndividuals( boolean direct );

    /**
     * Note that any restriction superclasses are not returned, unless they are has_proper_part
     *
     * @param direct
     * @return
     */
    default Collection<OntologyTerm> getParents( boolean direct ) {
        return getParents( direct, true, false );
    }

    default Collection<OntologyTerm> getParents( boolean direct, boolean includeAdditionalProperties ) {
        return getParents( direct, includeAdditionalProperties, false );
    }

    Collection<OntologyTerm> getParents( boolean direct, boolean includeAdditionalProperties, boolean keepObsoletes );

    Collection<OntologyRestriction> getRestrictions();

    /**
     * The taxon this term is restricted to, when the ontology declares one, else {@code null}.
     * <p>
     * OBO writes this as {@code relationship: in_taxon NCBITaxon:9940}, which becomes an OWL
     * {@code SubClassOf(in_taxon some NCBITaxon_9940)} restriction. MONDO uses it almost exclusively
     * to mark terms that are NOT human — 3,201 terms carry it and only 30 of those are human — so it
     * is the discriminator a curation client needs to reject a species-mismatched grounding.
     * <p>
     * Deliberately narrower than {@link #getRestrictions()}, and not implemented in terms of it.
     * That method walks the superclass graph twice and uses a thrown exception as a type test for
     * every non-restriction superclass; running it per search hit is exactly the kind of cost that
     * makes an endpoint slower over time. This answers one question with one pass over the direct
     * superclasses and no exceptions.
     *
     * @return the taxon restriction, or null when the term declares none (the common case)
     */
    @Nullable
    default TaxonConstraint getTaxonConstraint() {
        return null;
    }

    /**
     * A term's declared {@code in_taxon} value: the NCBITaxon URI, its numeric id, and its label
     * when the loaded model carries one.
     * <p>
     * {@code label} is null whenever NCBITaxon itself is not loaded — Gemma does not load it — and
     * the referencing ontology declared no {@code rdfs:label} for the class. The id is always
     * available, and is the field to key on: a label round-trips through a display string, an id
     * does not.
     */
    class TaxonConstraint {
        private final String uri;
        @Nullable
        private final Integer ncbiTaxonId;
        @Nullable
        private final String label;

        public TaxonConstraint( String uri, @Nullable Integer ncbiTaxonId, @Nullable String label ) {
            this.uri = uri;
            this.ncbiTaxonId = ncbiTaxonId;
            this.label = label;
        }

        public String getUri() {
            return uri;
        }

        @Nullable
        public Integer getNcbiTaxonId() {
            return ncbiTaxonId;
        }

        @Nullable
        public String getLabel() {
            return label;
        }

        @Override
        public String toString() {
            return "TaxonConstraint{" + uri + ( label != null ? " (" + label + ")" : "" ) + "}";
        }
    }

    /**
     * @deprecated use {@link #getLabel()} instead.
     */
    @Nullable
    @Deprecated
    String getTerm();

    boolean isRoot();

    /**
     * check to see if the term is obsolete, if it is it should not be used
     *
     * @deprecated use {@link #isObsolete()} instead
     */
    @Deprecated
    boolean isTermObsolete();
}
