package ubic.gemma.model.genome.gene.phenotype.valueObject;

import java.util.Collection;
import java.util.HashSet;

import ubic.gemma.model.association.GOEvidenceCode;
import ubic.gemma.model.association.phenotype.DataAnalysisEvidence;
import ubic.gemma.model.association.phenotype.DifferentialExpressionEvidence;
import ubic.gemma.model.association.phenotype.ExperimentalEvidence;
import ubic.gemma.model.association.phenotype.ExternalDatabaseEvidence;
import ubic.gemma.model.association.phenotype.GenericEvidence;
import ubic.gemma.model.association.phenotype.LiteratureEvidence;
import ubic.gemma.model.association.phenotype.PhenotypeAssociation;
import ubic.gemma.model.association.phenotype.UrlEvidence;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.VocabCharacteristicImpl;

/** Parent class of all evidences value object */
public abstract class EvidenceValueObject {

    private Long databaseId = null;

    private String name;
    private String description;
    private GOEvidenceCode evidenceCode = null;
    private Boolean isNegativeEvidence = false;

    private Collection<String> phenotypes = null;

    /**
     * Convert an collection of entities to a collection of value objects, *note: we implement the code here since we
     * cannot put this logic in the object thereself since the code is auto generated
     * 
     * @param phenotypeAssociations The List of entity we need to convert
     * @return A list of Evidence Value objects
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
                } else if ( phe instanceof DataAnalysisEvidence ) {
                    // TODO
                } else if ( phe instanceof DifferentialExpressionEvidence ) {
                    // TODO
                } else if ( phe instanceof ExperimentalEvidence ) {
                    // TODO
                } else if ( phe instanceof ExternalDatabaseEvidence ) {
                    // TODO
                } else if ( phe instanceof GenericEvidence ) {
                    // TODO
                } else if ( phe instanceof LiteratureEvidence ) {
                    // TODO
                }
                // else if ( phe instanceof GenericExperiment ) {
                // TODO
                // }

            }
        }
        return returnEvidences;
    }


    public EvidenceValueObject( String name, String description, GOEvidenceCode evidenceCode,
            Boolean isNegativeEvidence, Collection<Characteristic> characteristics, Long databaseId ) {
        super();
        this.name = name;
        this.description = description;
        this.evidenceCode = evidenceCode;
        this.isNegativeEvidence = isNegativeEvidence;
        this.databaseId = databaseId;

        phenotypes = new HashSet<String>();

        for ( Characteristic c : characteristics ) {
            phenotypes.add( c.getValue() );
        }
    }

    public EvidenceValueObject( String name, String description, Boolean isNegativeEvidence,
            GOEvidenceCode evidenceCode, Collection<String> phenotypes ) {
        super();
        this.name = name;
        this.description = description;
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

    /** Each child must implement this method and determine how to create the entity the represents */
    public abstract PhenotypeAssociation createEntity();

    /**
     * Sets the global field for all Evidences, the fields that are the same for any evidence. This method will be used
     * by all child evidences to create an Entity using createEntity()
     * 
     * @param phe The phenotype association (parent class of an evidence) we are interested in populating
     * @param phenotypes the given phenotypes
     */
    protected void populatePhenotypeAssociation( PhenotypeAssociation phe ) {
        // TODO
        // phe.setAssociationType( evidenceValueObject.get );
        phe.setDescription( description );
        phe.setEvidenceCode( evidenceCode );
        phe.setIsNegativeEvidence( isNegativeEvidence );
        phe.setName( name );

        // here lets add the phenotypes
        Collection<Characteristic> myPhenotypes = new HashSet<Characteristic>();

        for ( String phenotype : phenotypes ) {

            // TODO how to set up correct phenotype
            VocabCharacteristicImpl myPhenotype = new VocabCharacteristicImpl();
            myPhenotype.setValue( phenotype );

            myPhenotypes.add( myPhenotype );
        }

        phe.setPhenotypes( myPhenotypes );
    }

}
