package ubic.gemma.association.phenotype;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ubic.basecode.ontology.model.OntologyTerm;
import ubic.gemma.association.phenotype.PhenotypeExceptions.EntityNotFoundException;
import ubic.gemma.loader.entrez.pubmed.PubMedXMLFetcher;
import ubic.gemma.model.DatabaseEntryValueObject;
import ubic.gemma.model.association.GOEvidenceCode;
import ubic.gemma.model.association.phenotype.ExperimentalEvidence;
import ubic.gemma.model.association.phenotype.ExternalDatabaseEvidence;
import ubic.gemma.model.association.phenotype.GenericEvidence;
import ubic.gemma.model.association.phenotype.GenericExperiment;
import ubic.gemma.model.association.phenotype.LiteratureEvidence;
import ubic.gemma.model.association.phenotype.PhenotypeAssociation;
import ubic.gemma.model.association.phenotype.UrlEvidence;
import ubic.gemma.model.association.phenotype.service.PhenotypeAssociationService;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.BibliographicReferenceService;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.CharacteristicService;
import ubic.gemma.model.common.description.CitationValueObject;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.DatabaseEntryDao;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.common.description.ExternalDatabaseService;
import ubic.gemma.model.common.description.VocabCharacteristic;
import ubic.gemma.model.genome.gene.phenotype.valueObject.CharacteristicValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.DiffExpressionEvidenceValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.EvidenceValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.ExperimentalEvidenceValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.ExternalDatabaseEvidenceValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.GenericEvidenceValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.LiteratureEvidenceValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.UrlEvidenceValueObject;
import ubic.gemma.persistence.PersisterHelper;

/** This helper class is responsible to convert all types of EvidenceValueObjects to their corresponding entity */
@Component
public class PhenotypeAssoManagerServiceHelper {

    @Autowired
    private BibliographicReferenceService bibliographicReferenceService;

    @Autowired
    CharacteristicService characteristicService;

    @Autowired
    private PhenotypeAssociationService phenotypeAssociationService;

    @Autowired
    private PersisterHelper persisterHelper;

    @Autowired
    private ExternalDatabaseService externalDatabaseService;

    @Autowired
    private DatabaseEntryDao databaseEntryDao;

    private PubMedXMLFetcher pubMedXmlFetcher = new PubMedXMLFetcher();

    /**
     * Changes all type of evidenceValueObject to their corresponding entities
     * 
     * @param evidence the value object to change in an entity
     * @return PhenotypeAssociation the entity created from the ValueObject
     */
    public PhenotypeAssociation valueObject2Entity( EvidenceValueObject evidence ) {

        if ( evidence instanceof LiteratureEvidenceValueObject ) {
            return conversion2LiteratureEvidence( ( LiteratureEvidenceValueObject ) evidence );
        } else if ( evidence instanceof ExperimentalEvidenceValueObject ) {
            return conversion2ExperimentalEvidence( ( ExperimentalEvidenceValueObject ) evidence );
        } else if ( evidence instanceof GenericEvidenceValueObject ) {
            return conversion2GenericEvidence( ( GenericEvidenceValueObject ) evidence );
        } else if ( evidence instanceof UrlEvidenceValueObject ) {
            return conversion2UrlEvidence( ( UrlEvidenceValueObject ) evidence );
        } else if ( evidence instanceof DiffExpressionEvidenceValueObject ) {
            // TODO
            // return conversion2DifferentialExpressionEvidence (( DiffExpressionEvidenceValueObject ) evidence );
        } else if ( evidence instanceof ExternalDatabaseEvidenceValueObject ) {
            return conversion2ExternalDatabaseEvidence( ( ExternalDatabaseEvidenceValueObject ) evidence );
        }

        return null;

    }

    /**
     * @param evidenceValueObject the evidence we want to convert
     * @return PhenotypeAssociation the entity created from the ValueObject
     */
    private PhenotypeAssociation conversion2ExternalDatabaseEvidence(
            ExternalDatabaseEvidenceValueObject evidenceValueObject ) {

        // create the entity to populate
        ExternalDatabaseEvidence externalDatabaseEvidence = ExternalDatabaseEvidence.Factory.newInstance();

        // populate common field to all evidences
        populatePhenotypeAssociation( externalDatabaseEvidence, evidenceValueObject );

        // populate specific fields for this evidence
        DatabaseEntryValueObject databaseEntryValueObject = evidenceValueObject.getDatabaseEntryValueObject();

        // find the correct database
        ExternalDatabase externalDatabase = externalDatabaseService.find( databaseEntryValueObject
                .getExternalDatabase().getName() );

        if ( externalDatabase == null ) {
            throw new EntityNotFoundException( "Could not locate External Database: "
                    + databaseEntryValueObject.getExternalDatabase().getName() );
        }

        DatabaseEntry databaseEntry = DatabaseEntry.Factory.newInstance( externalDatabase );
        databaseEntry.setAccession( databaseEntryValueObject.getAccession() );

        databaseEntryDao.create( databaseEntry );

        externalDatabaseEvidence.setEvidenceSource( databaseEntry );

        return externalDatabaseEvidence;
    }

    /**
     * @param evidenceValueObject the evidence we want to convert
     * @return PhenotypeAssociation the entity created from the ValueObject
     */
    private PhenotypeAssociation conversion2UrlEvidence( UrlEvidenceValueObject evidenceValueObject ) {

        // create the entity to populate
        UrlEvidence urlEvidence = UrlEvidence.Factory.newInstance();

        // populate common field to all evidences
        populatePhenotypeAssociation( urlEvidence, evidenceValueObject );

        // populate specific fields for this evidence
        urlEvidence.setUrl( evidenceValueObject.getUrl() );

        return urlEvidence;
    }

    /**
     * @param evidenceValueObject the evidence we want to convert
     * @return PhenotypeAssociation the entity created from the ValueObject
     */
    private PhenotypeAssociation conversion2GenericEvidence( GenericEvidenceValueObject evidenceValueObject ) {

        // create the entity to populate
        GenericEvidence genericEvidence = GenericEvidence.Factory.newInstance();

        // populate common field to all evidences
        populatePhenotypeAssociation( genericEvidence, evidenceValueObject );

        return genericEvidence;
    }

    /**
     * @param evidenceValueObject the evidence we want to convert
     * @return PhenotypeAssociation the entity created from the ValueObject
     */
    private PhenotypeAssociation conversion2LiteratureEvidence( LiteratureEvidenceValueObject evidenceValueObject ) {

        // create the entity to populate
        LiteratureEvidence literatureEvidence = LiteratureEvidence.Factory.newInstance();

        // populate common field to all evidences
        populatePhenotypeAssociation( literatureEvidence, evidenceValueObject );

        // populate specific fields for this evidence
        String pubmedId = evidenceValueObject.getCitationValueObject().getPubmedAccession();
        literatureEvidence.setCitation( findOrCreateBibliographicReference( pubmedId ) );
        return literatureEvidence;

    }

    /**
     * @param evidenceValueObject the evidence we want to convert
     * @return PhenotypeAssociation the entity created from the ValueObject
     */
    private PhenotypeAssociation conversion2ExperimentalEvidence( ExperimentalEvidenceValueObject evidenceValueObject ) {

        // create the entity to populate
        ExperimentalEvidence experimentalEvidence = ExperimentalEvidence.Factory.newInstance();

        // populate common field to all evidences
        populatePhenotypeAssociation( experimentalEvidence, evidenceValueObject );

        // we only need to create the experiment if its not already in the database
        Collection<GenericExperiment> genericExperimentWithPubmed = phenotypeAssociationService
                .findByPubmedID( evidenceValueObject.getPrimaryPublicationCitationValueObject().getPubmedAccession() );

        GenericExperiment genericExperiment = null;

        // for the list received we need to check each one to see if they are the same
        for ( GenericExperiment genericExp : genericExperimentWithPubmed ) {

            boolean sameFound = true;

            HashSet<String> relevantPublication = new HashSet<String>();

            for ( CitationValueObject relevantPubli : evidenceValueObject.getRelevantPublicationsValueObjects() ) {

                relevantPublication.add( relevantPubli.getPubmedAccession() );
            }

            for ( BibliographicReference bilbi : genericExp.getOtherRelevantPublications() ) {

                // same relevant pubmed
                if ( !relevantPublication.contains( bilbi.getPubAccession().getAccession() ) ) {
                    sameFound = false;
                }
            }

            // list of all values for a characteristic
            Collection<String> values = new HashSet<String>();

            for ( CharacteristicValueObject characteristic : evidenceValueObject.getExperimentCharacteristics() ) {
                values.add( characteristic.getValue() );
            }

            for ( Characteristic cha : genericExp.getCharacteristics() ) {
                if ( !values.contains( cha.getValue() ) ) {
                    sameFound = false;
                }
            }

            if ( sameFound ) {
                genericExperiment = genericExp;
            }
        }

        // we didn't find the experiment in the database
        if ( genericExperiment == null ) {

            // create the GenericExperiment
            genericExperiment = GenericExperiment.Factory.newInstance();

            // find all pubmed id from the value object
            String primaryPubmedId = evidenceValueObject.getPrimaryPublicationCitationValueObject()
                    .getPubmedAccession();

            Collection<String> relevantPubmedId = new HashSet<String>();

            for ( CitationValueObject citationValueObject : evidenceValueObject.getRelevantPublicationsValueObjects() ) {

                relevantPubmedId.add( citationValueObject.getPubmedAccession() );
            }

            // creates or find those Bibliographic Reference and add them to the GenericExperiment
            genericExperiment.setPrimaryPublication( findOrCreateBibliographicReference( primaryPubmedId ) );
            genericExperiment.setOtherRelevantPublications( findOrCreateBibliographicReference( relevantPubmedId ) );

            // characteristic
            Collection<Characteristic> characteristics = null;

            if ( evidenceValueObject.getExperimentCharacteristics() != null ) {

                characteristics = new HashSet<Characteristic>();

                for ( CharacteristicValueObject chaValueObject : evidenceValueObject.getExperimentCharacteristics() ) {

                    VocabCharacteristic experimentCha = VocabCharacteristic.Factory.newInstance();

                    experimentCha.setValue( chaValueObject.getValue() );
                    experimentCha.setCategory( chaValueObject.getCategory() );
                    experimentCha.setValueUri( chaValueObject.getValueUri() );
                    experimentCha.setCategoryUri( chaValueObject.getCategoryUri() );

                    characteristics.add( experimentCha );
                }
            }

            genericExperiment.getCharacteristics().addAll( characteristics );
            phenotypeAssociationService.createGenericExperiment( genericExperiment );
        }

        experimentalEvidence.setExperiment( genericExperiment );
        return experimentalEvidence;
    }

    /**
     * Sets the fields that are the same for any evidences.
     * 
     * @param phe The phenotype association (parent class of an evidence) we are interested in populating
     * @param evidenceValueObject the value object representing a phenotype
     */
    public void populatePhenotypeAssociation( PhenotypeAssociation phe, EvidenceValueObject evidenceValueObject ) {

        // TODO
        phe.setDescription( evidenceValueObject.getDescription() );

        phe.setEvidenceCode( GOEvidenceCode.fromString( evidenceValueObject.getEvidenceCode() ) );
        phe.setIsNegativeEvidence( evidenceValueObject.getIsNegativeEvidence() );

        if ( evidenceValueObject.getAssociationType() != null ) {
            VocabCharacteristic associationType = VocabCharacteristic.Factory.newInstance();

            associationType.setValue( evidenceValueObject.getAssociationType().getValue() );
            associationType.setCategory( evidenceValueObject.getAssociationType().getCategory() );
            associationType.setValueUri( evidenceValueObject.getAssociationType().getValueUri() );
            associationType.setCategoryUri( evidenceValueObject.getAssociationType().getCategoryUri() );

            phe.setAssociationType( associationType );
        }
        // here lets add the phenotypes
        Collection<Characteristic> myPhenotypes = new HashSet<Characteristic>();

        for ( CharacteristicValueObject phenotype : evidenceValueObject.getPhenotypes() ) {

            // TODO how to set up correct phenotype
            VocabCharacteristic myPhenotype = VocabCharacteristic.Factory.newInstance();

            myPhenotype.setValue( phenotype.getValue() );
            myPhenotype.setCategory( phenotype.getCategory() );
            myPhenotype.setValueUri( phenotype.getValueUri() );
            myPhenotype.setCategoryUri( phenotype.getCategoryUri() );

            myPhenotypes.add( myPhenotype );
        }

        phe.getPhenotypes().addAll( myPhenotypes );
    }

    /** calls findOrCreateBibliographicReference for a Collection */
    private Collection<BibliographicReference> findOrCreateBibliographicReference( Collection<String> pubMedIds ) {

        Collection<BibliographicReference> bibliographicReferences = null;

        if ( pubMedIds != null && pubMedIds.size() != 0 ) {

            bibliographicReferences = new HashSet<BibliographicReference>();

            for ( String pubmedId : pubMedIds ) {

                bibliographicReferences.add( findOrCreateBibliographicReference( pubmedId ) );
            }
        }
        return bibliographicReferences;
    }

    /**
     * Creates a BibliographicReference if it doesn't exist in the database
     * 
     * @param pubMedId the pubmedID of the reference
     * @param BibliographicReference the BibliogrphicReference
     */
    private BibliographicReference findOrCreateBibliographicReference( String pubMedId ) {

        // check if already in the database
        BibliographicReference bibRef = bibliographicReferenceService.findByExternalId( pubMedId );

        if ( bibRef == null ) {

            // creates a new BibliographicReference
            bibRef = this.pubMedXmlFetcher.retrieveByHTTP( Integer.parseInt( pubMedId ) );

            // the pudmedId doesn't exists
            if ( bibRef == null ) {
                throw new EntityNotFoundException( "Could not locate reference with pubmed id=" + pubMedId );
            }

            // this will create or find the BibliographicReference
            persisterHelper.persist( bibRef );

        }

        return bibRef;
    }

    /** Ontology term to CharacteristicValueObject */
    public Set<CharacteristicValueObject> ontology2PhenotypeVO( Collection<OntologyTerm> ontologyTerms,
            String ontologyUsed ) {

        Set<CharacteristicValueObject> characteristicsVO = new HashSet<CharacteristicValueObject>();

        for ( OntologyTerm ontologyTerm : ontologyTerms ) {

            CharacteristicValueObject phenotype = new CharacteristicValueObject( ontologyTerm.getLabel(),
                    PhenotypeAssociationConstants.PHENOTYPE, ontologyTerm.getUri(),
                    PhenotypeAssociationConstants.PHENOTYPE_ONTOLOGY );
            phenotype.setOntologyUsed( ontologyUsed );
            characteristicsVO.add( phenotype );

        }
        return characteristicsVO;
    }

}
