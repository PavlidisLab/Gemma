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

import ubic.gemma.model.association.phenotype.DifferentialExpressionEvidence;
import ubic.gemma.model.association.phenotype.ExperimentalEvidence;
import ubic.gemma.model.association.phenotype.ExternalDatabaseEvidence;
import ubic.gemma.model.association.phenotype.GenericEvidence;
import ubic.gemma.model.association.phenotype.LiteratureEvidence;
import ubic.gemma.model.association.phenotype.PhenotypeAssociation;
import ubic.gemma.model.association.phenotype.UrlEvidence;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.VocabCharacteristicImpl;

/** Parent class of all evidence value objects */
public abstract class EvidenceValueObject {

    private Long databaseId = null;

    private String description = "";
    private CharacteristicValueObject associationType = null;
    private String evidenceCode = null;
    private Boolean isNegativeEvidence = new Boolean( false );
    private String className = "";
    /** If this evidence has the chosen Phenotypes, used by the service called findCandidateGenes */
    private Double relevance = new Double( 0 );

    private Set<CharacteristicValueObject> phenotypes = null;

    /**
     * Convert an collection of evidence entities to their corresponding value objects
     * 
     * @param phenotypeAssociations The List of entities we need to convert to value object
     * @return Collection<EvidenceValueObject> the converted results
     */
    public static Collection<EvidenceValueObject> convert2ValueObjects(
            Collection<PhenotypeAssociation> phenotypeAssociations ) {

        Collection<EvidenceValueObject> returnEvidence = new HashSet<EvidenceValueObject>();

        if ( phenotypeAssociations != null && phenotypeAssociations.size() > 0 ) {

            for ( PhenotypeAssociation phe : phenotypeAssociations ) {

                EvidenceValueObject evidence = convert2ValueObjects( phe );

                if ( evidence != null ) {
                    returnEvidence.add( evidence );
                }
            }
        }
        return returnEvidence;
    }

    /**
     * Convert an evidence entity to its corresponding value object
     * 
     * @param phe The phenotype Entity
     * @return Collection<EvidenceValueObject> its corresponding value object
     */
    public static EvidenceValueObject convert2ValueObjects( PhenotypeAssociation phe ) {

        EvidenceValueObject evidence = null;

        if ( phe instanceof UrlEvidence ) {
            evidence = new UrlEvidenceValueObject( ( UrlEvidence ) phe );
        } else if ( phe instanceof ExperimentalEvidence ) {
            evidence = new ExperimentalEvidenceValueObject( ( ExperimentalEvidence ) phe );
        } else if ( phe instanceof GenericEvidence ) {
            evidence = new GenericEvidenceValueObject( ( GenericEvidence ) phe );
        } else if ( phe instanceof LiteratureEvidence ) {
            evidence = new LiteratureEvidenceValueObject( ( LiteratureEvidence ) phe );
        } else if ( phe instanceof ExternalDatabaseEvidence ) {
            evidence = new ExternalDatabaseEvidenceValueObject( ( ExternalDatabaseEvidence ) phe );
        } else if ( phe instanceof DifferentialExpressionEvidence ) {
            // TODO
        }

        return evidence;
    }

    public EvidenceValueObject() {
        super();
    }

    /** set fields common to all evidence. Entity to Value Object */
    protected EvidenceValueObject( PhenotypeAssociation phenotypeAssociation ) {

        this.className = this.getClass().getSimpleName();
        this.databaseId = phenotypeAssociation.getId();
        this.description = phenotypeAssociation.getDescription();
        this.evidenceCode = phenotypeAssociation.getEvidenceCode().getValue();
        this.isNegativeEvidence = phenotypeAssociation.getIsNegativeEvidence();
        if ( phenotypeAssociation.getAssociationType() != null ) {

            String category = phenotypeAssociation.getAssociationType().getCategory();
            String value = phenotypeAssociation.getAssociationType().getValue();

            this.associationType = new CharacteristicValueObject( value, category );
        }
        this.phenotypes = new TreeSet<CharacteristicValueObject>();

        for ( Characteristic c : phenotypeAssociation.getPhenotypes() ) {

            CharacteristicValueObject characteristicVO = null;

            VocabCharacteristicImpl voCha = ( VocabCharacteristicImpl ) c;
            characteristicVO = new CharacteristicValueObject( voCha.getValue().toLowerCase(), voCha.getCategory(),
                    voCha.getValueUri(), voCha.getCategoryUri() );

            characteristicVO.setId( voCha.getId() );

            this.phenotypes.add( characteristicVO );
        }
    }

    protected EvidenceValueObject( String description, CharacteristicValueObject associationType,
            Boolean isNegativeEvidence, String evidenceCode, Set<CharacteristicValueObject> phenotypes ) {
        super();
        this.description = description;
        this.associationType = associationType;
        this.evidenceCode = evidenceCode;
        this.isNegativeEvidence = isNegativeEvidence;
        this.phenotypes = phenotypes;
    }

    public Long getDatabaseId() {
        return this.databaseId;
    }

    public String getDescription() {
        return this.description;
    }

    public CharacteristicValueObject getAssociationType() {
        return this.associationType;
    }

    public String getEvidenceCode() {
        return this.evidenceCode;
    }

    public Boolean getIsNegativeEvidence() {
        return this.isNegativeEvidence;
    }

    public Collection<CharacteristicValueObject> getPhenotypes() {
        return this.phenotypes;
    }

    public String getClassName() {
        return this.className;
    }

    public Double getRelevance() {
        return this.relevance;
    }

    public void setRelevance( Double relevance ) {
        this.relevance = relevance;
    }

    public void setDescription( String description ) {
        this.description = description;
    }

    public void setAssociationType( CharacteristicValueObject associationType ) {
        this.associationType = associationType;
    }

    public void setEvidenceCode( String evidenceCode ) {
        this.evidenceCode = evidenceCode;
    }

    public void setIsNegativeEvidence( Boolean isNegativeEvidence ) {
        this.isNegativeEvidence = isNegativeEvidence;
    }

    public void setPhenotypes( Set<CharacteristicValueObject> phenotypes ) {
        this.phenotypes = phenotypes;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( this.associationType == null ) ? 0 : this.associationType.hashCode() );
        result = prime * result + ( ( this.description == null ) ? 0 : this.description.hashCode() );
        result = prime * result + ( ( this.evidenceCode == null ) ? 0 : this.evidenceCode.hashCode() );
        result = prime * result + ( ( this.isNegativeEvidence == null ) ? 0 : this.isNegativeEvidence.hashCode() );
        result = prime * result + ( ( this.phenotypes == null ) ? 0 : this.phenotypes.hashCode() );
        return result;
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj ) return true;
        if ( obj == null ) return false;
        if ( getClass() != obj.getClass() ) return false;
        EvidenceValueObject other = ( EvidenceValueObject ) obj;
        if ( this.associationType == null ) {
            if ( other.associationType != null ) return false;
        } else if ( !this.associationType.equals( other.associationType ) ) return false;
        if ( this.description == null ) {
            if ( other.description != null ) return false;
        } else if ( !this.description.equals( other.description ) ) return false;
        if ( this.evidenceCode == null ) {
            if ( other.evidenceCode != null ) return false;
        } else if ( !this.evidenceCode.equals( other.evidenceCode ) ) return false;
        if ( this.isNegativeEvidence == null ) {
            if ( other.isNegativeEvidence != null ) return false;
        } else if ( !this.isNegativeEvidence.equals( other.isNegativeEvidence ) ) return false;
        if ( this.phenotypes == null ) {
            if ( other.phenotypes != null ) return false;
        } else if ( !this.phenotypes.equals( other.phenotypes ) ) return false;
        return true;
    }

}