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

import lombok.EqualsAndHashCode;
import ubic.gemma.model.association.phenotype.ExperimentalEvidence;
import ubic.gemma.model.common.description.Characteristic;

import java.util.Collection;
import java.util.TreeSet;

@EqualsAndHashCode(of = { "experimentCharacteristics" }, callSuper = false)
public class ExperimentalEvidenceValueObject extends EvidenceValueObject<ExperimentalEvidence> {

    private static final long serialVersionUID = 4243531745086284715L;
    private Collection<CharacteristicValueObject> experimentCharacteristics = new TreeSet<>();

    @Deprecated
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

    public Collection<CharacteristicValueObject> getExperimentCharacteristics() {
        return this.experimentCharacteristics;
    }

    public void setExperimentCharacteristics( Collection<CharacteristicValueObject> experimentCharacteristics ) {
        this.experimentCharacteristics = experimentCharacteristics;
    }

}