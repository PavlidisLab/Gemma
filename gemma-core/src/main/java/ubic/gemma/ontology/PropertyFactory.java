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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;

import com.hp.hpl.jena.ontology.DatatypeProperty;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.ontology.OntResource;

/**
 * @author pavlidis
 * @version $Id$
 */
public class PropertyFactory {

    private static Log log = LogFactory.getLog( PropertyFactory.class.getName() );

    /**
     * Convert a Jena property.
     * 
     * @param property
     * @param source
     * @return
     */
    public static ubic.gemma.ontology.OntologyProperty asProperty( OntProperty property, ExternalDatabase source ) {

        if ( property.isObjectProperty() ) {
            return new ObjectPropertyImpl( property.asObjectProperty(), source );
        } else if ( property.isDatatypeProperty() ) {
            return new DatatypePropertyImpl( property.asDatatypeProperty(), source );
        } else {
            log.warn( "Sorry, can't convert " + property.getClass().getName() + ": " + property );
            return null;
        }

    }

    public static PrimitiveType convertType( DatatypeProperty resource ) {
        PrimitiveType type = PrimitiveType.STRING;

        OntResource range = resource.getRange();
        if ( range != null ) {
            String uri = range.getURI();

            if ( uri == null ) {
                log.warn( "Can't get type for " + resource + " with range " + range );
                type = PrimitiveType.STRING;
            } else if ( uri.equals( "http://www.w3.org/2001/XMLSchema#&xsd;string" ) ) {
                type = PrimitiveType.STRING;
            } else if ( uri.equals( "http://www.w3.org/2001/XMLSchema#&xsd;boolean" ) ) {
                type = PrimitiveType.BOOLEAN;
            } else if ( uri.equals( "http://www.w3.org/2001/XMLSchema#&xsd;float" ) ) {
                type = PrimitiveType.DOUBLE;
            } else if ( uri.equals( "http://www.w3.org/2001/XMLSchema#&xsd;double" ) ) {
                type = PrimitiveType.DOUBLE;
            } else if ( uri.equals( "http://www.w3.org/2001/XMLSchema#&xsd;int" ) ) {
                type = PrimitiveType.INT;
            } else if ( uri.equals( "http://www.w3.org/2001/XMLSchema#&xsd;date" ) ) {
                type = PrimitiveType.STRING;
            }
        }
        return type;
    }
}
