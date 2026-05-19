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
/*
 * Originated in baseCode (ubic.basecode.ontology.*); pulled in-tree for Gemma 2.0
 * (Phase 3 search/ontology Step 3). Ported from Jena 2.x (com.hp.hpl.jena.*)
 * to Jena 4.x (org.apache.jena.*) namespace. Configuration-related lookups
 * continue to use baseCode's ubic.basecode.util.Configuration via the
 * still-classpath baseCode JAR.
 */
package ubic.gemma.core.ontology.basecode.model;

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
