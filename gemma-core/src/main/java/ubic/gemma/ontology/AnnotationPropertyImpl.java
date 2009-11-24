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

import ubic.gemma.model.common.description.ExternalDatabase;

import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.impl.PropertyImpl;

/**
 * Note that this is a concrete instance of the annotation.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class AnnotationPropertyImpl implements AnnotationProperty {

    private com.hp.hpl.jena.ontology.AnnotationProperty property;

    private String contents;

    private ExternalDatabase sourceOntology;

    /**
     * @param prop property for the statement
     * @param source ontology this relates to.
     * @param object of the statement
     */
    public AnnotationPropertyImpl( com.hp.hpl.jena.ontology.AnnotationProperty prop, ExternalDatabase source,
            RDFNode object ) {
        this.property = prop;
        this.sourceOntology = source;

        if ( object.isResource() ) {
            // need a level of indirection...
            Resource r = ( Resource ) object;
            Statement s = r.getProperty( new PropertyImpl( "http://www.w3.org/2000/01/rdf-schema#label" ) );
            if ( s != null ) {
                this.contents = s.getObject().toString();
            }
        } else {
            this.contents = OntologyLoader.asString( object );
        }

    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj ) return true;
        if ( obj == null ) return false;
        if ( getClass() != obj.getClass() ) return false;
        final AnnotationPropertyImpl other = ( AnnotationPropertyImpl ) obj;
        if ( contents == null ) {
            if ( other.contents != null ) return false;
        } else if ( !contents.equals( other.contents ) ) return false;
        if ( property == null ) {
            if ( other.property != null ) return false;
        } else if ( !property.equals( other.property ) ) return false;
        return true;
    }

    public String getContents() {
        return contents;
    }

    public String getProperty() {
        if ( property.getLabel( null ) != null ) {
            return property.getLabel( null );
        } else if ( property.getLocalName() != null ) {
            return property.getLocalName();
        } else {
            return property.toString();
        }
    }

    public ExternalDatabase getSourceOntology() {
        return sourceOntology;
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + ( ( contents == null ) ? 0 : contents.hashCode() );
        result = PRIME * result + ( ( property == null ) ? 0 : property.hashCode() );
        return result;
    }

    @Override
    public String toString() {
        return property.getLocalName() + " " + contents;
    }

}
