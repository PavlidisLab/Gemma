package ubic.gemma.model.genome.gene.phenotype.valueObject;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

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
    private Set<String> relevantPublication = new HashSet<String>();
    // TODO find correct name of variable
    private Set<CharacteristicValueObject> experimentCharacteristics = new TreeSet<CharacteristicValueObject>();

    // *********************************************
    // fields that are returned view of the object
    // *********************************************

    private Collection<CitationValueObject> relevantPublicationsValueObjects = new HashSet<CitationValueObject>();
    private CitationValueObject primaryPublicationCitationValueObject = null;

    public ExperimentalEvidenceValueObject( String description, CharacteristicValueObject associationType,
            Boolean isNegativeEvidence, String evidenceCode, Set<CharacteristicValueObject> phenotypes,
            String primaryPublication, Set<String> relevantPublication,
            Set<CharacteristicValueObject> experimentCharacteristics ) {
        super( description, associationType, isNegativeEvidence, evidenceCode, phenotypes );
        this.primaryPublication = primaryPublication;
        this.relevantPublication = relevantPublication;
        this.experimentCharacteristics = experimentCharacteristics;
    }

    public ExperimentalEvidenceValueObject() {
    }

    /** Entity to Value Object */
    public ExperimentalEvidenceValueObject( ExperimentalEvidence experimentalEvidence ) {
        super( experimentalEvidence );

        this.primaryPublicationCitationValueObject = BibliographicReferenceValueObject.constructCitation( experimentalEvidence.getExperiment().getPrimaryPublication() );

        this.relevantPublicationsValueObjects = BibliographicReferenceValueObject
                .constructCitations( experimentalEvidence.getExperiment().getOtherRelevantPublications() );

        for ( CitationValueObject bibli : relevantPublicationsValueObjects ) {
            relevantPublication.add( bibli.getPubmedAccession() );
        }

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

    public Collection<CitationValueObject> getRelevantPublicationsValueObjects() {
        return relevantPublicationsValueObjects;
    }

    public String getPrimaryPublication() {
        return primaryPublication;
    }

    public Set<String> getRelevantPublication() {
        return relevantPublication;
    }

    public Set<CharacteristicValueObject> getExperimentCharacteristics() {
        return experimentCharacteristics;
    }

    public CitationValueObject getPrimaryPublicationCitationValueObject() {
        return primaryPublicationCitationValueObject;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ( ( experimentCharacteristics == null ) ? 0 : experimentCharacteristics.hashCode() );
        result = prime * result + ( ( primaryPublication == null ) ? 0 : primaryPublication.hashCode() );
        result = prime * result + ( ( relevantPublication == null ) ? 0 : relevantPublication.hashCode() );
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
        if ( primaryPublication == null ) {
            if ( other.primaryPublication != null ) return false;
        } else if ( !primaryPublication.equals( other.primaryPublication ) ) return false;
        if ( relevantPublication == null ) {
            if ( other.relevantPublication != null ) return false;
        } else if ( !relevantPublication.equals( other.relevantPublication ) ) return false;
        return true;
    }
}