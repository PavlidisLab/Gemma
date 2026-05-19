/*
 * The basecode project
 *
 * Copyright (c) 2007-2019 University of British Columbia
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
package ubic.gemma.core.ontology.basecode.jena;

import org.apache.jena.ontology.ObjectProperty;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntResource;
import org.apache.jena.ontology.Restriction;
import org.apache.jena.util.iterator.ExtendedIterator;
import ubic.gemma.core.ontology.basecode.model.OntologyTerm;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * @author pavlidis
 */
class ObjectPropertyImpl extends OntologyPropertyImpl implements ubic.gemma.core.ontology.basecode.model.ObjectProperty {

    private final org.apache.jena.ontology.ObjectProperty resource;
    private final Set<Restriction> additionalRestrictions;

    public ObjectPropertyImpl( ObjectProperty resource, Set<Restriction> additionalRestrictions ) {
        super( resource );
        this.resource = resource;
        this.additionalRestrictions = additionalRestrictions;
    }

    @Override
    public Collection<OntologyTerm> getRange() {
        ExtendedIterator<? extends OntResource> iterator = resource.listRange();
        Collection<OntologyTerm> result = new HashSet<>();
        while ( iterator.hasNext() ) {
            OntResource r = iterator.next();
            if ( r.isClass() ) {
                OntClass class1 = r.asClass();
                result.add( new OntologyTermImpl( class1, additionalRestrictions ) );
            } else {
                log.warn( "Don't know how to deal with {}", r );
            }
        }
        return result;
    }
}
