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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import ubic.gemma.model.association.phenotype.ExperimentalEvidence;
import ubic.gemma.model.common.description.BibliographicReferenceValueObject;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.CitationValueObject;
import ubic.gemma.model.common.description.VocabCharacteristicImpl;

public class ExperimentalEvidenceValueObject extends EvidenceValueObject {

    private Set<CharacteristicValueObject> experimentCharacteristics = new TreeSet<CharacteristicValueObject>();
    private Set<CitationValueObject> relevantPublicationsCitationValueObjects = new HashSet<CitationValueObject>();
    private CitationValueObject primaryPublicationCitationValueObject = null;

    public ExperimentalEvidenceValueObject( String description, CharacteristicValueObject associationType,
            Boolean isNegativeEvidence, String evidenceCode, Set<CharacteristicValueObject> phenotypes,
            String primaryPublication, Set<String> relevantPublication,
            Set<CharacteristicValueObject> experimentCharacteristics ) {
        super( description, associationType, isNegativeEvidence, evidenceCode, phenotypes );
        this.primaryPublicationCitationValueObject = new CitationValueObject();
        this.primaryPublicationCitationValueObject.setPubmedAccession( primaryPublication );

        for ( String relevantPubMedID : relevantPublication ) {
            CitationValueObject relevantPublicationValueObject = new CitationValueObject();
            relevantPublicationValueObject.setPubmedAccession( relevantPubMedID );
            this.relevantPublicationsCitationValueObjects.add( relevantPublicationValueObject );
        }

        this.experimentCharacteristics = experimentCharacteristics;
    }

    public ExperimentalEvidenceValueObject() {
    }

    /** Entity to Value Object */
    public ExperimentalEvidenceValueObject( ExperimentalEvidence experimentalEvidence ) {
        super( experimentalEvidence );

        this.primaryPublicationCitationValueObject = BibliographicReferenceValueObject
                .constructCitation( experimentalEvidence.getExperiment().getPrimaryPublication() );

        this.relevantPublicationsCitationValueObjects.addAll( BibliographicReferenceValueObject
                .constructCitations( experimentalEvidence.getExperiment().getOtherRelevantPublications() ) );

        Collection<Characteristic> collectionCharacteristics = experimentalEvidence.getExperiment()
                .getCharacteristics();

        if ( collectionCharacteristics != null ) {
            for ( Characteristic c : collectionCharacteristics ) {
                if ( c instanceof VocabCharacteristicImpl ) {
                    VocabCharacteristicImpl voCha = ( VocabCharacteristicImpl ) c;
                    this.experimentCharacteristics.add( new CharacteristicValueObject( voCha.getValue(), voCha
                            .getCategory(), voCha.getValueUri(), voCha.getCategoryUri() ) );
                } else {
                    this.experimentCharacteristics.add( new CharacteristicValueObject( c.getValue(), c.getCategory() ) );
                }
            }
        }
    }

    public Set<CharacteristicValueObject> getExperimentCharacteristics() {
        return experimentCharacteristics;
    }

    public Collection<CitationValueObject> getRelevantPublicationsValueObjects() {
        return relevantPublicationsCitationValueObjects;
    }

    public CitationValueObject getPrimaryPublicationCitationValueObject() {
        return primaryPublicationCitationValueObject;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ( ( experimentCharacteristics == null ) ? 0 : experimentCharacteristics.hashCode() );
        result = prime
                * result
                + ( ( primaryPublicationCitationValueObject == null ) ? 0 : primaryPublicationCitationValueObject
                        .hashCode() );
        result = prime
                * result
                + ( ( relevantPublicationsCitationValueObjects == null ) ? 0 : relevantPublicationsCitationValueObjects
                        .hashCode() );
        return result;
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj ) return true;
        if ( !super.equals( obj ) ) return false;
        if ( getClass() != obj.getClass() ) return false;
        ExperimentalEvidenceValueObject other = ( ExperimentalEvidenceValueObject ) obj;
        if ( experimentCharacteristics == null ) {
            if ( other.experimentCharacteristics != null ) return false;
        } else if ( !experimentCharacteristics.equals( other.experimentCharacteristics ) ) return false;
        if ( primaryPublicationCitationValueObject == null ) {
            if ( other.primaryPublicationCitationValueObject != null ) return false;
        } else if ( !primaryPublicationCitationValueObject.equals( other.primaryPublicationCitationValueObject ) )
            return false;
        if ( relevantPublicationsCitationValueObjects == null ) {
            if ( other.relevantPublicationsCitationValueObjects != null ) return false;
        } else if ( !relevantPublicationsCitationValueObjects.equals( other.relevantPublicationsCitationValueObjects ) )
            return false;
        return true;
    }

}