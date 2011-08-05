package ubic.gemma.model.genome.gene.phenotype.valueObject;

import java.util.Collection;
import java.util.HashSet;

import ubic.gemma.model.association.GOEvidenceCode;
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
    private String characteristic = "";
    private GOEvidenceCode evidenceCode = null;
    private Boolean isNegativeEvidence = false;

    private Collection<String> phenotypes = null;

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
        this.evidenceCode = phenotypeAssociation.getEvidenceCode();
        this.isNegativeEvidence = phenotypeAssociation.getIsNegativeEvidence();
        if ( phenotypeAssociation.getAssociationType() != null ) {
            this.characteristic = phenotypeAssociation.getAssociationType().getValue();
        }
        phenotypes = new HashSet<String>();

        for ( Characteristic c : phenotypeAssociation.getPhenotypes() ) {
            phenotypes.add( c.getValue() );
        }
    }

    protected EvidenceValueObject( String name, String description, String characteristic, Boolean isNegativeEvidence,
            GOEvidenceCode evidenceCode, Collection<String> phenotypes ) {
        super();
        this.name = name;
        this.description = description;
        this.characteristic = characteristic;
        this.evidenceCode = evidenceCode;
        this.isNegativeEvidence = isNegativeEvidence;
        this.phenotypes = phenotypes;
    }

    public String getName() {
        return name;
    }

    public void setName( String name ) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription( String description ) {
        this.description = description;
    }

    public GOEvidenceCode getEvidenceCode() {
        return evidenceCode;
    }

    public void setEvidenceCode( GOEvidenceCode evidenceCode ) {
        this.evidenceCode = evidenceCode;
    }

    public Boolean isNegativeEvidence() {
        return isNegativeEvidence;
    }

    public void setIsNegativeEvidence( Boolean isNegativeEvidence ) {
        this.isNegativeEvidence = isNegativeEvidence;
    }

    public Collection<String> getPhenotypes() {
        return phenotypes;
    }

    public void setPhenotypes( Collection<String> phenotypes ) {
        this.phenotypes = phenotypes;
    }

    public Long getDatabaseId() {
        return databaseId;
    }

    public void setDatabaseId( Long databaseId ) {
        this.databaseId = databaseId;
    }

    public Boolean getIsNegativeEvidence() {
        return isNegativeEvidence;
    }

    public String getCharacteristic() {
        return characteristic;
    }

    public void setCharacteristic( String characteristic ) {
        this.characteristic = characteristic;
    }

}
