/*
 * The Gemma project
 * 
 * Copyright (c) 2007 University of British Columbia
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
package ubic.gemma.ontology;

import java.util.Collection;
import java.util.HashSet;

import ubic.gemma.model.common.description.ExternalDatabase;

import com.hp.hpl.jena.ontology.ObjectProperty;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;

/**
 * @author pavlidis
 * @version $Id$
 */
public class ObjectPropertyImpl extends OntologyPropertyImpl implements ubic.gemma.ontology.ObjectProperty {

    private com.hp.hpl.jena.ontology.ObjectProperty resource;

    public ObjectPropertyImpl( ObjectProperty resource, ExternalDatabase source ) {
        this.isFunctional = resource.isFunctionalProperty();
        this.resource = resource;
        this.sourceOntology = source;
    }

    public String getLabel() {
        return this.toString();
    }

    public Collection<OntologyTerm> getRange() {
        ExtendedIterator iterator = resource.listRange();
        Collection<OntologyTerm> result = new HashSet<OntologyTerm>();
        while ( iterator.hasNext() ) {
            OntResource r = ( OntResource ) iterator.next();
            if ( r.isClass() ) {
                OntClass class1 = r.asClass();
                result.add( new OntologyTermImpl( class1, this.getSourceOntology() ) );
            } else {
                log.warn( "Don't know how to deal with " + r );
            }
        }
        return result;
    }

    @Override
    public String getUri() {
        return resource.getURI();
    }

    @Override
    public String toString() {
        String label = resource.getLabel( null );
        if ( label == null ) label = resource.getLocalName();
        if ( label == null ) label = resource.getURI();
        if ( label == null ) label = resource.toString();
        if ( label == null ) label = "[no string version available!]";
        return label;
    }
}
