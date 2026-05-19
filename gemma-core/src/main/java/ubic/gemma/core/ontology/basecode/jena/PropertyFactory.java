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

import org.apache.jena.ontology.DatatypeProperty;
import org.apache.jena.ontology.OntProperty;
import org.apache.jena.ontology.OntResource;
import org.apache.jena.ontology.Restriction;

import java.util.Set;

/**
 * @author pavlidis
 */
class PropertyFactory {

    /**
     * Convert a Jena property.
     *
     * @return new property or null if it could not be converted.
     */
    public static ubic.gemma.core.ontology.basecode.model.OntologyProperty asProperty( OntProperty property, Set<Restriction> additionalRestrictions ) {

        if ( property.isObjectProperty() ) {
            return new ObjectPropertyImpl( property.asObjectProperty(), additionalRestrictions );
        } else if ( property.isDatatypeProperty() ) {
            return new DatatypePropertyImpl( property.asDatatypeProperty() );
        } else {
            return null;
        }

    }

    public static Class<?> convertType( DatatypeProperty resource ) {
        Class<?> type = java.lang.String.class;

        OntResource range = resource.getRange();
        if ( range != null ) {
            String uri = range.getURI();

            if ( uri == null ) {
                type = java.lang.String.class;
            } else if ( uri.equals( "http://www.w3.org/2001/XMLSchema#&xsd;string" ) ) {
                type = java.lang.String.class;
            } else if ( uri.equals( "http://www.w3.org/2001/XMLSchema#&xsd;boolean" ) ) {
                type = java.lang.Boolean.class;
            } else if ( uri.equals( "http://www.w3.org/2001/XMLSchema#&xsd;float" ) ) {
                type = java.lang.Double.class;
            } else if ( uri.equals( "http://www.w3.org/2001/XMLSchema#&xsd;double" ) ) {
                type = java.lang.Double.class;
            } else if ( uri.equals( "http://www.w3.org/2001/XMLSchema#&xsd;int" ) ) {
                type = java.lang.Integer.class;
            } else if ( uri.equals( "http://www.w3.org/2001/XMLSchema#&xsd;date" ) ) {
                type = java.util.Date.class;
            }
        }
        return type;
    }
}
