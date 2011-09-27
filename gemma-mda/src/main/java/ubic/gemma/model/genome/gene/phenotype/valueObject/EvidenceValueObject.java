package ubic.gemma.model.genome.gene.phenotype.valueObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import ubic.gemma.model.association.phenotype.DifferentialExpressionEvidence;
import ubic.gemma.model.association.phenotype.ExperimentalEvidence;
import ubic.gemma.model.association.phenotype.ExternalDatabaseEvidence;
import ubic.gemma.model.association.phenotype.GenericEvidence;
import ubic.gemma.model.association.phenotype.LiteratureEvidence;
import ubic.gemma.model.association.phenotype.PhenotypeAssociation;
import ubic.gemma.model.association.phenotype.UrlEvidence;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.VocabCharacteristicImpl;

/** Parent class of all evidences value objects */
public abstract class EvidenceValueObject {

    private Long databaseId = null;

    private String description = "";
    private CharacteristicValueObject associationType = null;
    private String evidenceCode = null;
    private Boolean isNegativeEvidence = false;
    private String className = "";
    /** If this evidence has the chosen Phenotypes, used by the service called findCandidateGenes */
    private Double relevance = 0D;

    private Collection<CharacteristicValueObject> phenotypes = null;

    /**
     * Convert an collection of evidence entities to their corresponding value objects
     * 
     * @param phenotypeAssociations The List of entities we need to convert to value object
     * @return Collection<EvidenceValueObject> the converted results
     */
    public static Collection<EvidenceValueObject> convert2ValueObjects(
            Collection<PhenotypeAssociation> phenotypeAssociations ) {

        Collection<EvidenceValueObject> returnEvidences = new HashSet<EvidenceValueObject>();

        if ( phenotypeAssociations != null && phenotypeAssociations.size() > 0 ) {

            for ( PhenotypeAssociation phe : phenotypeAssociations ) {

                EvidenceValueObject evidence = null;

                if ( phe instanceof UrlEvidence ) {
                    evidence = new UrlEvidenceValueObject( ( UrlEvidence ) phe );
                    returnEvidences.add( evidence );
                } else if ( phe instanceof ExperimentalEvidence ) {
                    evidence = new ExperimentalEvidenceValueObject( ( ExperimentalEvidence ) phe );
                    returnEvidences.add( evidence );
                } else if ( phe instanceof GenericEvidence ) {
                    evidence = new GenericEvidenceValueObject( ( GenericEvidence ) phe );
                    returnEvidences.add( evidence );
                } else if ( phe instanceof LiteratureEvidence ) {
                    evidence = new LiteratureEvidenceValueObject( ( LiteratureEvidence ) phe );
                    returnEvidences.add( evidence );
                } else if ( phe instanceof ExternalDatabaseEvidence ) {
                    evidence = new ExternalDatabaseEvidenceValueObject( ( ExternalDatabaseEvidence ) phe );
                    returnEvidences.add( evidence );
                    // TODO
                } else if ( phe instanceof DifferentialExpressionEvidence ) {
                    // TODO
                }

            }
        }
        return returnEvidences;
    }

    public EvidenceValueObject() {

    }

    /** set fields common to all evidences. Entity to Value Object */
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
        phenotypes = new ArrayList<CharacteristicValueObject>();

        for ( Characteristic c : phenotypeAssociation.getPhenotypes() ) {

            CharacteristicValueObject characteristicVO = null;

            VocabCharacteristicImpl voCha = ( VocabCharacteristicImpl ) c;
            characteristicVO = new CharacteristicValueObject( voCha.getValue().toLowerCase(), voCha.getCategory(),
                    voCha.getValueUri(), voCha.getCategoryUri() );

            phenotypes.add( characteristicVO );
        }
    }

    protected EvidenceValueObject( String description, CharacteristicValueObject associationType,
            Boolean isNegativeEvidence, String evidenceCode, Collection<CharacteristicValueObject> phenotypes ) {
        super();
        this.description = description;
        this.associationType = associationType;
        this.evidenceCode = evidenceCode;
        this.isNegativeEvidence = isNegativeEvidence;
        this.phenotypes = phenotypes;
    }

    public Long getDatabaseId() {
        return databaseId;
    }

    public String getDescription() {
        return description;
    }

    public CharacteristicValueObject getAssociationType() {
        return associationType;
    }

    public String getEvidenceCode() {
        return evidenceCode;
    }

    public Boolean getIsNegativeEvidence() {
        return isNegativeEvidence;
    }

    public Collection<CharacteristicValueObject> getPhenotypes() {
        return phenotypes;
    }

    public String getClassName() {
        return className;
    }

    public Double getRelevance() {
        return relevance;
    }

    public void setRelevance( Double relevance ) {
        this.relevance = relevance;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( associationType == null ) ? 0 : associationType.hashCode() );
        result = prime * result + ( ( description == null ) ? 0 : description.hashCode() );
        result = prime * result + ( ( evidenceCode == null ) ? 0 : evidenceCode.hashCode() );
        result = prime * result + ( ( isNegativeEvidence == null ) ? 0 : isNegativeEvidence.hashCode() );
        result = prime * result + ( ( phenotypes == null ) ? 0 : phenotypes.hashCode() );
        return result;
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj ) return true;
        if ( obj == null ) return false;
        if ( getClass() != obj.getClass() ) return false;
        EvidenceValueObject other = ( EvidenceValueObject ) obj;
        if ( associationType == null ) {
            if ( other.associationType != null ) return false;
        } else if ( !associationType.equals( other.associationType ) ) return false;
        if ( description == null ) {
            if ( other.description != null ) return false;
        } else if ( !description.equals( other.description ) ) return false;
        if ( evidenceCode == null ) {
            if ( other.evidenceCode != null ) return false;
        } else if ( !evidenceCode.equals( other.evidenceCode ) ) return false;
        if ( isNegativeEvidence == null ) {
            if ( other.isNegativeEvidence != null ) return false;
        } else if ( !isNegativeEvidence.equals( other.isNegativeEvidence ) ) return false;

        if ( phenotypes == null ) {
            if ( other.phenotypes != null ) return false;
        } else {
            HashSet<CharacteristicValueObject> set1 = new HashSet<CharacteristicValueObject>();
            HashSet<CharacteristicValueObject> set2 = new HashSet<CharacteristicValueObject>();
            set1.addAll( phenotypes );
            set2.addAll( other.phenotypes );

            if ( !set1.equals( set2 ) ) return false;
        }
        return true;
    }

}