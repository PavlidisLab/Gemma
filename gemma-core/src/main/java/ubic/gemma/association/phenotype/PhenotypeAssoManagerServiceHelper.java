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
package ubic.gemma.association.phenotype;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ubic.basecode.ontology.model.OntologyTerm;
import ubic.gemma.association.phenotype.PhenotypeExceptions.EntityNotFoundException;
import ubic.gemma.genome.gene.service.GeneService;
import ubic.gemma.loader.entrez.pubmed.PubMedXMLFetcher;
import ubic.gemma.model.DatabaseEntryValueObject;
import ubic.gemma.model.analysis.Investigation;
import ubic.gemma.model.association.GOEvidenceCode;
import ubic.gemma.model.association.phenotype.ExperimentalEvidence;
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
import ubic.gemma.model.genome.gene.phenotype.valueObject.GenericEvidenceValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.LiteratureEvidenceValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.UrlEvidenceValueObject;
import ubic.gemma.ontology.OntologyService;
import ubic.gemma.persistence.Persister;

/** This helper class is responsible to convert all types of EvidenceValueObjects to their corresponding entity */
@Service
public class PhenotypeAssoManagerServiceHelper {

    @Autowired
    private BibliographicReferenceService bibliographicReferenceService;

    @Autowired
    private CharacteristicService characteristicService;

    @Autowired
    private PhenotypeAssociationService phenotypeAssociationService;

    @Autowired
    private Persister persisterHelper;

    @Autowired
    private ExternalDatabaseService externalDatabaseService;

    @Autowired
    private OntologyService ontologyService;

    @Autowired
    private DatabaseEntryDao databaseEntryDao;

    @Autowired
    private GeneService geneService;

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
        }

        return null;
    }

    /**
     * Sets the fields that are the same for any evidence.
     * 
     * @param phe The phenotype association (parent class of an evidence) we are interested in populating
     * @param evidenceValueObject the value object representing a phenotype
     */
    public void populatePhenotypeAssociation( PhenotypeAssociation phe, EvidenceValueObject evidenceValueObject ) {
        populatePheAssoWithoutPhenotypes( phe, evidenceValueObject );
        populatePheAssoPhenotypes( phe, evidenceValueObject );
    }

    /**
     * Sets the fields that are the same for any evidence. Doesn't populate phenotypes
     * 
     * @param phe The phenotype association (parent class of an evidence) we are interested in populating
     * @param evidenceValueObject the value object representing a phenotype
     */
    public void populatePheAssoWithoutPhenotypes( PhenotypeAssociation phe, EvidenceValueObject evidenceValueObject ) {
        phe.setDescription( evidenceValueObject.getDescription() );
        phe.setEvidenceCode( GOEvidenceCode.fromString( evidenceValueObject.getEvidenceCode() ) );
        phe.setIsNegativeEvidence( evidenceValueObject.getIsNegativeEvidence() );
        phe.setGene( this.geneService.findByNCBIId( evidenceValueObject.getGeneNCBI() ) );

        if ( evidenceValueObject.getAssociationType() != null ) {
            VocabCharacteristic associationType = VocabCharacteristic.Factory.newInstance();

            associationType.setValue( evidenceValueObject.getAssociationType().getValue() );
            associationType.setCategory( evidenceValueObject.getAssociationType().getCategory() );
            associationType.setValueUri( evidenceValueObject.getAssociationType().getValueUri() );
            associationType.setCategoryUri( evidenceValueObject.getAssociationType().getCategoryUri() );

            phe.setAssociationType( associationType );
        }

        if ( evidenceValueObject.getEvidenceSource() != null ) {
            populateEvidenceSource( phe, evidenceValueObject );
        }
    }

    /** Ontology term to CharacteristicValueObject */
    public Set<CharacteristicValueObject> ontology2CharacteristicValueObject( Collection<OntologyTerm> ontologyTerms,
            String ontologyUsed ) {

        Set<CharacteristicValueObject> characteristicsVO = new HashSet<CharacteristicValueObject>();

        for ( OntologyTerm ontologyTerm : ontologyTerms ) {

            CharacteristicValueObject phenotype = new CharacteristicValueObject( ontologyTerm.getLabel(),
                    ontologyTerm.getUri() );
            phenotype.setOntologyUsed( ontologyUsed );
            characteristicsVO.add( phenotype );

        }
        return characteristicsVO;
    }

    // load evidence from the database and populate it with the updated information
    public void populateModifiedValues( EvidenceValueObject evidenceValueObject,
            PhenotypeAssociation phenotypeAssociation ) {

        // 1- modify common values to all evidences
        phenotypeAssociation.setDescription( evidenceValueObject.getDescription() );
        phenotypeAssociation.setEvidenceCode( GOEvidenceCode.fromString( evidenceValueObject.getEvidenceCode() ) );
        phenotypeAssociation.setIsNegativeEvidence( evidenceValueObject.getIsNegativeEvidence() );
        phenotypeAssociation.setGene( this.geneService.findByNCBIId( evidenceValueObject.getGeneNCBI() ) );

        if ( evidenceValueObject.getAssociationType() != null ) {
            VocabCharacteristic associationType = VocabCharacteristic.Factory.newInstance();

            associationType.setValue( evidenceValueObject.getAssociationType().getValue() );
            associationType.setCategory( evidenceValueObject.getAssociationType().getCategory() );
            associationType.setValueUri( evidenceValueObject.getAssociationType().getValueUri() );
            associationType.setCategoryUri( evidenceValueObject.getAssociationType().getCategoryUri() );

            phenotypeAssociation.setAssociationType( associationType );
        }

        // 2- modify specific values depending on evidence type
        if ( phenotypeAssociation instanceof LiteratureEvidence ) {

            LiteratureEvidence literatureEvidence = ( LiteratureEvidence ) phenotypeAssociation;

            LiteratureEvidenceValueObject literatureVO = ( LiteratureEvidenceValueObject ) evidenceValueObject;

            String primaryPubMed = literatureVO.getCitationValueObject().getPubmedAccession();

            // primary bibliographic reference
            literatureEvidence.setCitation( findOrCreateBibliographicReference( primaryPubMed ) );

        } else if ( phenotypeAssociation instanceof ExperimentalEvidence ) {

            ExperimentalEvidenceValueObject experimentalVO = ( ExperimentalEvidenceValueObject ) evidenceValueObject;
            ExperimentalEvidence experimentalEvidence = ( ExperimentalEvidence ) phenotypeAssociation;
            Investigation experiment = experimentalEvidence.getExperiment();

            // ***************************************************************
            // 1- take care of the characteristic an investigation can have
            // ***************************************************************

            // the final characteristics to update the evidence with
            Collection<Characteristic> characteristicsUpdated = new HashSet<Characteristic>();

            // To determine the change in the Characteristic for an Investigation, the id will be used
            Map<Long, Characteristic> databaseIds = new HashMap<Long, Characteristic>();

            for ( Characteristic cha : experiment.getCharacteristics() ) {
                databaseIds.put( cha.getId(), cha );
            }

            Set<Long> newDatabaseIds = new HashSet<Long>();

            for ( CharacteristicValueObject chaVO : experimentalVO.getExperimentCharacteristics() ) {

                // new characteristic, since no database id received
                if ( chaVO.getId() == null ) {

                    VocabCharacteristic characteristic = VocabCharacteristic.Factory.newInstance();

                    characteristic.setValue( chaVO.getValue() );
                    characteristic.setCategory( chaVO.getCategory() );
                    characteristic.setValueUri( chaVO.getValueUri() );
                    characteristic.setCategoryUri( chaVO.getCategoryUri() );
                    characteristicsUpdated.add( characteristic );

                }
                // not new but could be modified, take the values inside VO
                else {
                    newDatabaseIds.add( chaVO.getId() );

                    VocabCharacteristic cha = ( VocabCharacteristic ) databaseIds.get( chaVO.getId() );

                    cha.setValue( chaVO.getValue() );
                    cha.setValueUri( chaVO.getValueUri() );
                    cha.setCategory( chaVO.getCategory() );
                    cha.setCategoryUri( chaVO.getCategoryUri() );

                    characteristicsUpdated.add( cha );
                }
            }

            // verify if something was deleted
            for ( Characteristic cha : experiment.getCharacteristics() ) {

                if ( !newDatabaseIds.contains( cha.getId() ) ) {
                    // delete characteristic from the database
                    this.characteristicService.delete( cha.getId() );
                }
            }

            experiment.getCharacteristics().clear();
            experiment.getCharacteristics().addAll( characteristicsUpdated );

            // ***************************************************************
            // 2- The bibliographic references
            // ***************************************************************

            String primaryPubMed = experimentalVO.getPrimaryPublicationCitationValueObject().getPubmedAccession();

            // primary bibliographic reference
            experiment.setPrimaryPublication( findOrCreateBibliographicReference( primaryPubMed ) );

            Set<String> otherRelevantPubMed = new HashSet<String>();

            for ( CitationValueObject citation : experimentalVO.getRelevantPublicationsValueObjects() ) {
                otherRelevantPubMed.add( citation.getPubmedAccession() );
            }

            // relevant bibliographic references
            experiment.setOtherRelevantPublications( findOrCreateBibliographicReference( otherRelevantPubMed ) );

        } else if ( phenotypeAssociation instanceof GenericEvidence ) {
            // nothing special to do
        } else if ( phenotypeAssociation instanceof UrlEvidence ) {
            // nothing special to do
        } else if ( evidenceValueObject instanceof DiffExpressionEvidenceValueObject ) {
            // TODO
        }
    }

    /**
     * @param evidenceValueObject the evidence we want to convert
     * @return PhenotypeAssociation the entity created from the ValueObject
     */
    private PhenotypeAssociation conversion2UrlEvidence( UrlEvidenceValueObject evidenceValueObject ) {

        // create the entity to populate
        UrlEvidence urlEvidence = UrlEvidence.Factory.newInstance();

        // populate common field to all evidence
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

        // populate common field to evidence
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

        // populate common field to evidence
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

        // populate common field to evidence
        populatePhenotypeAssociation( experimentalEvidence, evidenceValueObject );

        // we only need to create the experiment if its not already in the database
        Collection<GenericExperiment> genericExperimentWithPubmed = this.phenotypeAssociationService
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

            if ( values.size() != genericExp.getCharacteristics().size() ) {
                sameFound = false;
            } else {

                for ( Characteristic cha : genericExp.getCharacteristics() ) {
                    if ( !values.contains( cha.getValue() ) ) {
                        sameFound = false;
                    }
                }
            }

            // the Investigation is already present in the database so we can reuse it
            if ( sameFound ) {
                System.out
                        .println( "Investigation For the ExperimentalEvidence found in the database and will be reuse" );
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

            // characteristics for an experiment
            Collection<Characteristic> characteristics = new HashSet<Characteristic>();

            for ( CharacteristicValueObject chaValueObject : evidenceValueObject.getExperimentCharacteristics() ) {

                VocabCharacteristic experimentCha = VocabCharacteristic.Factory.newInstance();

                experimentCha.setValue( chaValueObject.getValue() );
                experimentCha.setCategory( chaValueObject.getCategory() );
                experimentCha.setValueUri( chaValueObject.getValueUri() );
                experimentCha.setCategoryUri( chaValueObject.getCategoryUri() );

                characteristics.add( experimentCha );
            }

            genericExperiment.getCharacteristics().addAll( characteristics );
            this.phenotypeAssociationService.create( genericExperiment );
        }

        experimentalEvidence.setExperiment( genericExperiment );
        return experimentalEvidence;
    }

    /**
     * Populate the phenotypes for an PhenotypeAssociation using an EvidenceValueObject
     * 
     * @param phe The phenotype association (parent class of an evidence) we are interested in populating
     * @param evidenceValueObject the value object representing a phenotype
     */
    private void populatePheAssoPhenotypes( PhenotypeAssociation phe, EvidenceValueObject evidenceValueObject ) {
        // here lets add the phenotypes
        Collection<Characteristic> myPhenotypes = new HashSet<Characteristic>();

        for ( CharacteristicValueObject phenotype : evidenceValueObject.getPhenotypes() ) {

            VocabCharacteristic myPhenotype = VocabCharacteristic.Factory.newInstance();

            OntologyTerm ontologyTerm = this.ontologyService.getTerm( phenotype.getValueUri() );

            if ( ontologyTerm == null ) {
                throw new EntityNotFoundException( "Could not locate ontology term with uri: "
                        + phenotype.getValueUri() );
            }

            myPhenotype.setValueUri( ontologyTerm.getUri() );
            myPhenotype.setValue( ontologyTerm.getLabel() );
            myPhenotype.setCategory( PhenotypeAssociationConstants.PHENOTYPE );
            myPhenotype.setCategoryUri( PhenotypeAssociationConstants.PHENOTYPE_CATEGORY_URI );

            myPhenotypes.add( myPhenotype );
        }

        phe.getPhenotypes().addAll( myPhenotypes );
    }

    /**
     * @param phe
     * @param evidenceValueObject
     */
    private void populateEvidenceSource( PhenotypeAssociation phe, EvidenceValueObject evidenceValueObject ) {
        DatabaseEntryValueObject databaseEntryValueObject = evidenceValueObject.getEvidenceSource();

        // find the correct database
        ExternalDatabase externalDatabase = this.externalDatabaseService.find( databaseEntryValueObject
                .getExternalDatabase().getName() );

        if ( externalDatabase == null ) {
            throw new EntityNotFoundException( "Could not locate External Database: "
                    + databaseEntryValueObject.getExternalDatabase().getName() );
        }

        DatabaseEntry databaseEntry = DatabaseEntry.Factory.newInstance( externalDatabase );
        databaseEntry.setAccession( databaseEntryValueObject.getAccession() );

        databaseEntry = this.databaseEntryDao.create( databaseEntry );

        phe.setEvidenceSource( databaseEntry );
    }

    /** calls findOrCreateBibliographicReference for a Collection */
    private Collection<BibliographicReference> findOrCreateBibliographicReference( Collection<String> pubMedIds ) {

        Collection<BibliographicReference> bibliographicReferences = null;

        if ( pubMedIds != null && !pubMedIds.isEmpty() ) {

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
        BibliographicReference bibRef = this.bibliographicReferenceService.findByExternalId( pubMedId );

        if ( bibRef == null ) {

            // creates a new BibliographicReference
            bibRef = this.pubMedXmlFetcher.retrieveByHTTP( Integer.parseInt( pubMedId ) );

            // the pudmedId doesn't exists
            if ( bibRef == null ) {
                throw new EntityNotFoundException( "Could not locate reference with pubmed id=" + pubMedId );
            }

            // this will create or find the BibliographicReference
            this.persisterHelper.persist( bibRef );
        }

        return bibRef;
    }

}