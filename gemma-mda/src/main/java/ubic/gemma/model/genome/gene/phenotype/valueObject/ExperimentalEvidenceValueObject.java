package ubic.gemma.model.genome.gene.phenotype.valueObject;

import java.util.ArrayList;
import java.util.Collection;

import ubic.gemma.model.association.phenotype.ExperimentalEvidence;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.VocabCharacteristicImpl;

public class ExperimentalEvidenceValueObject extends EvidenceValueObject {

    // *********************************************
    // field used to create the Bibliographic object
    // *********************************************
    // The primary pubmed id
    private String primaryPublication = "";
    // other relevant pubmed id
    private Collection<String> relevantPublication = null;
    // TODO find correct name of variable
    private Collection<CharacteristicValueObject> experimentCharacteristics = null;

    // *********************************************
    // fields that are returned view of the object
    // *********************************************
    private BibliographicReferenceValueObject primaryPublicationValueObject = null;
    private Collection<BibliographicReferenceValueObject> relevantPublicationsValueObjects = null;

    public ExperimentalEvidenceValueObject( String description, CharacteristicValueObject associationType,
            Boolean isNegativeEvidence, String evidenceCode, Collection<CharacteristicValueObject> phenotypes,
            String primaryPublication, Collection<String> relevantPublication,
            Collection<CharacteristicValueObject> experimentCharacteristics ) {
        super( description, associationType, isNegativeEvidence, evidenceCode, phenotypes );
        this.primaryPublication = primaryPublication;
        this.relevantPublication = relevantPublication;
        this.experimentCharacteristics = experimentCharacteristics;
    }

    
    public ExperimentalEvidenceValueObject(){
    }
    
    /** Entity to Value Object */
    public ExperimentalEvidenceValueObject( ExperimentalEvidence experimentalEvidence ) {
        super( experimentalEvidence );

        this.primaryPublicationValueObject = new BibliographicReferenceValueObject( experimentalEvidence
                .getExperiment().getPrimaryPublication() );
        this.relevantPublicationsValueObjects = BibliographicReferenceValueObject
                .convert2ValueObjects( experimentalEvidence.getExperiment().getOtherRelevantPublications() );

        Collection<Characteristic> collectionCharacteristics = experimentalEvidence.getExperiment()
                .getCharacteristics();

        if ( collectionCharacteristics != null ) {
            this.experimentCharacteristics = new ArrayList<CharacteristicValueObject>();
            for ( Characteristic c : collectionCharacteristics ) {
                if ( c instanceof VocabCharacteristicImpl ) {
                    VocabCharacteristicImpl voCha = ( VocabCharacteristicImpl ) c;
                    experimentCharacteristics.add( new CharacteristicValueObject( voCha.getValue(),
                            voCha.getCategory(), voCha.getValueUri(), voCha.getCategoryUri() ) );
                } else {
                    experimentCharacteristics.add( new CharacteristicValueObject( c.getValue(), c.getCategory() ) );
                }
            }
        }
    }

    public BibliographicReferenceValueObject getPrimaryPublicationValueObject() {
        return primaryPublicationValueObject;
    }

    public Collection<BibliographicReferenceValueObject> getRelevantPublicationsValueObjects() {
        return relevantPublicationsValueObjects;
    }

    public String getPrimaryPublication() {
        return primaryPublication;
    }

    public Collection<String> getRelevantPublication() {
        return relevantPublication;
    }

    public Collection<CharacteristicValueObject> getExperimentCharacteristics() {
        return experimentCharacteristics;
    }

}