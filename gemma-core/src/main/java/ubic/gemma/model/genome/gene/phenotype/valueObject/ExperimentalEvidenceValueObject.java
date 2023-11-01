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
package ubic.gemma.model.genome.gene.phenotype.valueObject;

import ubic.gemma.model.association.phenotype.ExperimentalEvidence;
import ubic.gemma.model.common.description.Characteristic;

import java.util.Collection;
import java.util.TreeSet;

import ubic.gemma.model.common.description.CharacteristicValueObject;

public class ExperimentalEvidenceValueObject extends EvidenceValueObject<ExperimentalEvidence> {

    private static final long serialVersionUID = 4243531745086284715L;
    private Collection<CharacteristicValueObject> experimentCharacteristics = new TreeSet<>();

    /**
     * Required when using the class as a spring bean.
     */
    public ExperimentalEvidenceValueObject() {
        super();
    }

    public ExperimentalEvidenceValueObject( Long id ) {
        super( id );
    }

    public ExperimentalEvidenceValueObject( ExperimentalEvidence experimentalEvidence ) {
        super( experimentalEvidence );

        Collection<Characteristic> collectionCharacteristics = experimentalEvidence.getExperiment()
                .getCharacteristics();

        if ( collectionCharacteristics != null ) {
            for ( Characteristic c : collectionCharacteristics ) {

                CharacteristicValueObject chaValueObject = new CharacteristicValueObject( c.getId(),
                        c.getValue(), c.getCategory(), c.getValueUri(), c.getCategoryUri() );

                this.experimentCharacteristics.add( chaValueObject );

            }
        }
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj )
            return true;
        if ( !super.equals( obj ) )
            return false;
        if ( getClass() != obj.getClass() )
            return false;
        ExperimentalEvidenceValueObject other = ( ExperimentalEvidenceValueObject ) obj;

        if ( this.experimentCharacteristics.size() != other.experimentCharacteristics.size() ) {
            return false;
        }
        for ( CharacteristicValueObject characteristicValueObject : this.experimentCharacteristics ) {
            if ( !other.experimentCharacteristics.contains( characteristicValueObject ) ) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        for ( CharacteristicValueObject phenotype : this.experimentCharacteristics ) {
            result = prime * result + phenotype.hashCode();
        }

        return result;
    }

    public Collection<CharacteristicValueObject> getExperimentCharacteristics() {
        return this.experimentCharacteristics;
    }

    public void setExperimentCharacteristics( Collection<CharacteristicValueObject> experimentCharacteristics ) {
        this.experimentCharacteristics = experimentCharacteristics;
    }

}