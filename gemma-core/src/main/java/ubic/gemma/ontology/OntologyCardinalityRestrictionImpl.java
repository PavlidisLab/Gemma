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

import com.hp.hpl.jena.ontology.Restriction;

/**
 * @author pavlidis
 * @version $Id$
 */
public class OntologyCardinalityRestrictionImpl extends OntologyRestrictionImpl implements
        OntologyCardinalityRestriction {

    int cardinality = 0;
    CardinalityType cardType;

    public OntologyCardinalityRestrictionImpl( Restriction resource, ExternalDatabase source ) {
        super( resource, source );

        if ( resource.isMaxCardinalityRestriction() ) {
            this.cardType = CardinalityType.MAX_CARDINALITY;
            this.cardinality = resource.asMaxCardinalityRestriction().getMaxCardinality();
        } else if ( resource.isMinCardinalityRestriction() ) {
            this.cardType = CardinalityType.MIN_CARDINALITY;
            this.cardinality = resource.asMinCardinalityRestriction().getMinCardinality();
        } else if ( resource.isCardinalityRestriction() ) {
            this.cardType = CardinalityType.CARDINALITY;
            this.cardinality = resource.asCardinalityRestriction().getCardinality();
        } else {
            throw new IllegalArgumentException( "Must pass in a cardinality restriction" );
        }

    }

    public int getCardinality() {
        return cardinality;
    }

    public CardinalityType getCardinalityType() {
        return cardType;
    }

    @Override
    public String toString() {
        return " cardinality restriction: " + cardType + "  " + this.getCardinality();
    }

}
