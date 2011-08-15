package ubic.gemma.model.genome.gene.phenotype.valueObject;

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

/** Parent class of all evidences value objects */
public abstract class EvidenceValueObject {

    private Long databaseId = null;

    private String name = "";
    private String description = "";
    private CharacteristicValueObject associationType = null;
    private String evidenceCode = null;
    private Boolean isNegativeEvidence = false;

    private Collection<CharacteristicValueObject> phenotypes = null;

    /**
     * Convert an collection of evidence entities to their corresponding value objects
     * 
     * @param phenotypeAssociations The List of entities we need to convert to value object
     * @return Collection<EvidenceValueObject> the converted results
     */
    public static Collection<EvidenceValueObject> convert2ValueObjects(
            Collection<PhenotypeAssociation> phenotypeAssociations ) {

        Collection<EvidenceValueObject> returnEvidences = null;

        if ( phenotypeAssociations != null && phenotypeAssociations.size() > 0 ) {

            returnEvidences = new HashSet<EvidenceValueObject>();

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

    /** set fields common to all evidences. Entity to Value Object */
    protected EvidenceValueObject( PhenotypeAssociation phenotypeAssociation ) {

        this.databaseId = phenotypeAssociation.getId();
        this.name = phenotypeAssociation.getName();
        this.description = phenotypeAssociation.getDescription();
        this.evidenceCode = phenotypeAssociation.getEvidenceCode().getValue();
        this.isNegativeEvidence = phenotypeAssociation.getIsNegativeEvidence();
        if ( phenotypeAssociation.getAssociationType() != null ) {

            String category = phenotypeAssociation.getAssociationType().getCategory();
            String value = phenotypeAssociation.getAssociationType().getValue();

            this.associationType = new CharacteristicValueObject( category, value );
        }
        phenotypes = new HashSet<CharacteristicValueObject>();

        for ( Characteristic c : phenotypeAssociation.getPhenotypes() ) {

            CharacteristicValueObject characteristicVO = new CharacteristicValueObject( c.getCategory(), c.getValue() );

            phenotypes.add( characteristicVO );
        }
    }

    protected EvidenceValueObject( String name, String description, CharacteristicValueObject associationType,
            Boolean isNegativeEvidence, String evidenceCode, Collection<CharacteristicValueObject> phenotypes ) {
        super();
        this.name = name;
        this.description = description;
        this.associationType = associationType;
        this.evidenceCode = evidenceCode;
        this.isNegativeEvidence = isNegativeEvidence;
        this.phenotypes = phenotypes;
    }

    public Long getDatabaseId() {
        return databaseId;
    }

    public String getName() {
        return name;
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

}
