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
package ubic.gemma.core.ontology.jena;

import org.apache.jena.ontology.Restriction;
import ubic.gemma.core.ontology.model.OntologyProperty;
import ubic.gemma.core.ontology.model.OntologyRestriction;

import java.util.Set;

/**
 * Represents a restriction on instances that are subclasses of this.
 *
 * @author Paul
 */
abstract class OntologyRestrictionImpl extends OntologyTermImpl implements OntologyRestriction {

    protected OntologyProperty restrictionOn;

    /**
     * Kept because a restriction's identity is this node and nothing else — see {@link #equals}.
     */
    private final Restriction restriction;

    public OntologyRestrictionImpl( Restriction resource, Set<Restriction> additionalRestrictions ) {
        super( resource, additionalRestrictions );
        this.restriction = resource;
        this.restrictionOn = PropertyFactory.asProperty( resource.getOnProperty(), additionalRestrictions );
    }

    @Override
    public OntologyProperty getRestrictionOn() {
        return restrictionOn;
    }

    /**
     * 🛑 <b>A restriction is a blank node, so it must not inherit equality-by-URI-or-label.</b>
     *
     * <p>{@code AbstractOntologyResource} hashes on {@code getUri()} and, when both URIs are null,
     * compares {@code getLabel()}. A restriction has neither: the hash is one constant for every
     * restriction in every ontology, and null-label equals null-label, so <b>any two restrictions
     * compared equal</b>. {@code getRestrictions()} and {@code getDirectRestrictions()} both collect
     * into a {@code HashSet}, so a class carrying ten roles kept ONE — whichever
     * {@code listSuperClasses} yielded first.</p>
     *
     * <p>That is why the relation producer's counts moved between runs of an unchanged artifact:
     * CHEBI at 11,413 / 11,393 / 11,478, and CLO's {@code CLO_0000179} at 441 / 1,000 / 1,899 before
     * that. Iteration order decided which single restriction survived. The earlier switch from the
     * closure walk to the direct walk fixed a real problem and not this one; both walks collect into
     * the same broken set.</p>
     *
     * <p>Identity is the node. Two wrappers around one restriction are one restriction; two structurally
     * identical restrictions on distinct nodes stay distinct, which is the conservative direction —
     * a caller that wants them merged is deduplicating on the property and filler anyway.</p>
     */
    @Override
    public boolean equals( Object obj ) {
        if ( this == obj ) {
            return true;
        }
        if ( !( obj instanceof OntologyRestrictionImpl ) ) {
            return false;
        }
        return restriction.equals( ( ( OntologyRestrictionImpl ) obj ).restriction );
    }

    @Override
    public int hashCode() {
        return restriction.hashCode();
    }

}
