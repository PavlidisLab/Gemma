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

import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.ontology.Restriction;

/**
 * @author pavlidis
 * @version $Id$
 */
public class RestrictionFactory {

    public static OntologyRestriction asRestriction( Restriction restriction, ExternalDatabase source ) {

        OntProperty onProperty = restriction.getOnProperty();

        if ( onProperty.isDatatypeProperty() ) {
            return new OntologyDatatypeRestrictionImpl( restriction, source );
        } else if ( onProperty.isObjectProperty() ) {
            if ( restriction.isCardinalityRestriction() ) {
                return new OntologyCardinalityRestrictionImpl( restriction, source );
            }
            return new OntologyClassRestrictionImpl( restriction, source );

        } else {
            throw new UnsupportedOperationException( "Sorry, can't convert "
                    + restriction.getOnProperty().getClass().getName() );
        }

    }

}
