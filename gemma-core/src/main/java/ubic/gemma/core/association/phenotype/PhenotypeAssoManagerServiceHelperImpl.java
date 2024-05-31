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
package ubic.gemma.core.association.phenotype;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ubic.gemma.persistence.service.common.description.BibliographicReferenceService;
import ubic.gemma.persistence.service.genome.gene.GeneService;
import ubic.gemma.core.loader.entrez.pubmed.PubMedXMLFetcher;
import ubic.gemma.model.analysis.Investigation;
import ubic.gemma.model.association.GOEvidenceCode;
import ubic.gemma.model.association.phenotype.*;
import ubic.gemma.model.common.description.*;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.genome.gene.phenotype.valueObject.*;
import ubic.gemma.persistence.persister.Persister;
import ubic.gemma.persistence.service.analysis.expression.diff.GeneDiffExMetaAnalysisService;
import ubic.gemma.persistence.service.association.phenotype.service.PhenotypeAssociationService;
import ubic.gemma.persistence.service.common.description.CharacteristicService;
import ubic.gemma.persistence.service.common.description.DatabaseEntryDao;
import ubic.gemma.persistence.service.common.description.ExternalDatabaseService;
import ubic.gemma.persistence.service.common.quantitationtype.QuantitationTypeService;

import java.io.IOException;
import java.util.*;

/**
 * @author nicolas
 * @see    PhenotypeAssoManagerServiceHelper
 */
@Component

@Deprecated
public class PhenotypeAssoManagerServiceHelperImpl implements PhenotypeAssoManagerServiceHelper {

    private final PubMedXMLFetcher pubMedXmlFetcher = new PubMedXMLFetcher();
    private final BibliographicReferenceService bibliographicReferenceService;
    private final CharacteristicService characteristicService;
    private final DatabaseEntryDao databaseEntryDao;
    private final ExternalDatabaseService externalDatabaseService;
    private final GeneDiffExMetaAnalysisService geneDiffExMetaAnalysisService;
    private final GeneService geneService;
    private final PhenotypeAssoOntologyHelper ontologyHelper;
    private final Persister persisterHelper;
    private final PhenotypeAssociationService phenotypeAssociationService;
    private final QuantitationTypeService quantitationTypeService;

    @Autowired
    public PhenotypeAssoManagerServiceHelperImpl( BibliographicReferenceService bibliographicReferenceService,
            CharacteristicService characteristicService, DatabaseEntryDao databaseEntryDao,
            ExternalDatabaseService externalDatabaseService,
            GeneDiffExMetaAnalysisService geneDiffExMetaAnalysisService, GeneService geneService,
            PhenotypeAssoOntologyHelper ontologyHelper, Persister persisterHelper,
            PhenotypeAssociationService phenotypeAssociationService, QuantitationTypeService quantitationTypeService ) {
        this.bibliographicReferenceService = bibliographicReferenceService;
        this.characteristicService = characteristicService;
        this.databaseEntryDao = databaseEntryDao;
        this.externalDatabaseService = externalDatabaseService;
        this.geneDiffExMetaAnalysisService = geneDiffExMetaAnalysisService;
        this.geneService = geneService;
        this.ontologyHelper = ontologyHelper;
        this.persisterHelper = persisterHelper;
        this.phenotypeAssociationService = phenotypeAssociationService;
        this.quantitationTypeService = quantitationTypeService;
    }

    @Override
    public void populateModifiedValues( EvidenceValueObject<? extends PhenotypeAssociation> evidenceValueObject,
            PhenotypeAssociation phenotypeAssociation ) {

        // 1- modify common values to all evidences
        phenotypeAssociation.setDescription( evidenceValueObject.getDescription() );
        phenotypeAssociation.setEvidenceCode( GOEvidenceCode.valueOf( evidenceValueObject.getEvidenceCode() ) );
        phenotypeAssociation.setIsNegativeEvidence( evidenceValueObject.getIsNegativeEvidence() );
        phenotypeAssociation.setGene( this.geneService.findByNCBIId( evidenceValueObject.getGeneNCBI() ) );

        this.setScoreInformation( evidenceValueObject, phenotypeAssociation );
        this.updatePhenotypeAssociationPublication( phenotypeAssociation, evidenceValueObject );

        // 2- modify specific values depending on evidence type
        if ( phenotypeAssociation instanceof ExperimentalEvidence ) {

            ExperimentalEvidenceValueObject experimentalVO = ( ExperimentalEvidenceValueObject ) evidenceValueObject;
            ExperimentalEvidence experimentalEvidence = ( ExperimentalEvidence ) phenotypeAssociation;
            Investigation experiment = experimentalEvidence.getExperiment();

            // ***************************************************************
            // 1- take care of the characteristic an investigation can have
            // ***************************************************************

            Collection<Characteristic> finalCharacteristics = new HashSet<>();

            HashMap<Long, CharacteristicValueObject> updatedCharacteristicsMap = new HashMap<>();

            for ( CharacteristicValueObject updatedCharacteristic : experimentalVO.getExperimentCharacteristics() ) {

                // updated
                if ( updatedCharacteristic.getId() != null ) { // this was != 0.
                    updatedCharacteristicsMap.put( updatedCharacteristic.getId(), updatedCharacteristic );
                }
                // new one
                else {
                    Characteristic characteristic = this.ontologyHelper
                            .characteristicValueObject2Characteristic( updatedCharacteristic );
                    finalCharacteristics.add( characteristic );
                }
            }

            for ( Characteristic experimentCharacteristic : experiment.getCharacteristics() ) {

                CharacteristicValueObject experimentCharacteristicVO = new CharacteristicValueObject(
                        experimentCharacteristic );

                CharacteristicValueObject updatedCharacteristic = updatedCharacteristicsMap
                        .get( experimentCharacteristic.getId() );

                // found an update, same database id
                if ( updatedCharacteristic != null ) {

                    // same values as before
                    if ( updatedCharacteristic.equals( experimentCharacteristicVO ) ) {
                        finalCharacteristics.add( experimentCharacteristic );
                    } else {

                        // different values found
                        Characteristic characteristic = this.ontologyHelper
                                .characteristicValueObject2Characteristic( updatedCharacteristic );

                        experimentCharacteristic.setValueUri( characteristic.getValueUri() );
                        experimentCharacteristic.setValue( characteristic.getValue() );
                        experimentCharacteristic.setCategory( characteristic.getCategory() );
                        experimentCharacteristic.setCategoryUri( characteristic.getCategoryUri() );
                        finalCharacteristics.add( experimentCharacteristic );
                    }
                }
                // this experimentCharacteristic was deleted
                else {
                    this.characteristicService.remove( experimentCharacteristic );
                }
            }
            experiment.getCharacteristics().clear();
            experiment.getCharacteristics().addAll( finalCharacteristics );
        }

    }

    @Override
    public PhenotypeAssociation valueObject2Entity( EvidenceValueObject<? extends PhenotypeAssociation> evidence ) {

        if ( evidence instanceof LiteratureEvidenceValueObject ) {
            return this.conversion2LiteratureEvidence( ( LiteratureEvidenceValueObject ) evidence );
        } else if ( evidence instanceof ExperimentalEvidenceValueObject ) {
            return this.conversion2ExperimentalEvidence( ( ExperimentalEvidenceValueObject ) evidence );
        } else if ( evidence instanceof GenericEvidenceValueObject ) {
            return this.conversion2GenericEvidence( ( GenericEvidenceValueObject ) evidence );
        } else if ( evidence instanceof DiffExpressionEvidenceValueObject ) {
            return this.conversion2DifferentialExpressionEvidence( ( DiffExpressionEvidenceValueObject ) evidence );
        } else {
            throw new UnsupportedOperationException(
                    "Don't know how to convert " + evidence.getClass().getSimpleName() );
        }

    }

    private void populatePheAssoWithoutPhenotypes( PhenotypeAssociation phe,
            EvidenceValueObject<? extends PhenotypeAssociation> evidenceValueObject ) {
        phe.setDescription( evidenceValueObject.getDescription() );
        phe.setEvidenceCode( GOEvidenceCode.valueOf( evidenceValueObject.getEvidenceCode() ) );
        phe.setIsNegativeEvidence( evidenceValueObject.getIsNegativeEvidence() );
        phe.setGene( this.geneService.findByNCBIId( evidenceValueObject.getGeneNCBI() ) );
        phe.setRelationship( evidenceValueObject.getRelationship() );

        if ( evidenceValueObject.getPhenotypeAssPubVO() != null
                && evidenceValueObject.getPhenotypeAssPubVO().size() != 0 ) {
            this.populatePhenotypeAssociationPublication( phe, evidenceValueObject );
        }

        if ( evidenceValueObject.getEvidenceSource() != null ) {
            this.populateEvidenceSource( phe, evidenceValueObject );
        }

        phe.setOriginalPhenotype( evidenceValueObject.getOriginalPhenotype() );
        phe.setMappingType( evidenceValueObject.findPhenotypeMappingAsEnum() );

        this.setScoreInformation( evidenceValueObject, phe );
    }

    private void populatePhenotypeAssociation( PhenotypeAssociation phe,
            EvidenceValueObject<? extends PhenotypeAssociation> evidenceValueObject ) {
        this.populatePheAssoWithoutPhenotypes( phe, evidenceValueObject );
        this.populatePheAssoPhenotypes( phe, evidenceValueObject );
    }

    /**
     * @param  evidenceValueObject the evidence we want to convert
     * @return                     PhenotypeAssociation the entity created from the ValueObject
     */
    private PhenotypeAssociation conversion2DifferentialExpressionEvidence(
            DiffExpressionEvidenceValueObject evidenceValueObject ) {

        DifferentialExpressionEvidence differentialExpressionEvidence = DifferentialExpressionEvidence.Factory
                .newInstance();

        // populate common field to evidence
        this.populatePhenotypeAssociation( differentialExpressionEvidence, evidenceValueObject );

        differentialExpressionEvidence.setGeneDifferentialExpressionMetaAnalysisResult(
                this.geneDiffExMetaAnalysisService
                        .loadResult( evidenceValueObject.getGeneDifferentialExpressionMetaAnalysisResultId() ) );

        differentialExpressionEvidence.setSelectionThreshold( evidenceValueObject.getSelectionThreshold() );

        return differentialExpressionEvidence;
    }

    /**
     * @param  evidenceValueObject the evidence we want to convert
     * @return                     PhenotypeAssociation the entity created from the ValueObject
     */
    private PhenotypeAssociation conversion2ExperimentalEvidence(
            ExperimentalEvidenceValueObject evidenceValueObject ) {

        CitationValueObject primaryCitationValueObject = null;
        CitationValueObject relevantCitationValueObject = null;

        // when we create the experiment we had the pubmed to it ( kind of optional since the are already on the
        // evidence)
        if ( evidenceValueObject.getPhenotypeAssPubVO().size() > 0 ) {
            primaryCitationValueObject = evidenceValueObject.getPhenotypeAssPubVO().iterator().next()
                    .getCitationValueObject();
        }
        if ( evidenceValueObject.getPhenotypeAssPubVO().size() > 1 ) {
            relevantCitationValueObject = evidenceValueObject.getPhenotypeAssPubVO().iterator().next()
                    .getCitationValueObject();
        }

        // create the entity to populate
        ExperimentalEvidence experimentalEvidence = ExperimentalEvidence.Factory.newInstance();

        // populate common field to evidence
        this.populatePhenotypeAssociation( experimentalEvidence, evidenceValueObject );

        GenericExperiment genericExperiment = null;

        if ( primaryCitationValueObject != null ) {

            // we only need to create the experiment if its not already in the database
            Collection<GenericExperiment> genericExperimentWithPubmed = this.phenotypeAssociationService
                    .findByPubmedID( primaryCitationValueObject.getPubmedAccession() );

            // for the list received we need to check each one to see if they are the same
            for ( GenericExperiment genericExp : genericExperimentWithPubmed ) {

                boolean sameFound = true;

                HashSet<String> relevantPublication = new HashSet<>();

                if ( relevantCitationValueObject != null ) {
                    relevantPublication.add( relevantCitationValueObject.getPubmedAccession() );
                }

                for ( BibliographicReference bilbi : genericExp.getOtherRelevantPublications() ) {

                    // same relevant pubmed
                    if ( !relevantPublication.contains( bilbi.getPubAccession().getAccession() ) ) {
                        sameFound = false;
                    }
                }

                // list of all values for a characteristic
                Collection<String> values = new HashSet<>();

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
                    genericExperiment = genericExp;
                }
            }
        }
        // we didn't find the experiment in the database
        if ( genericExperiment == null ) {

            // create the GenericExperiment
            genericExperiment = GenericExperiment.Factory.newInstance();

            // find all pubmed id from the value object
            String primaryPubmedId;

            if ( primaryCitationValueObject != null ) {

                primaryPubmedId = primaryCitationValueObject.getPubmedAccession();
                genericExperiment.setPrimaryPublication( this.findOrCreateBibliographicReference( primaryPubmedId ) );
            }

            Collection<String> relevantPubmedId = new HashSet<>();

            if ( relevantCitationValueObject != null ) {
                relevantPubmedId.add( relevantCitationValueObject.getPubmedAccession() );
            }

            genericExperiment
                    .setOtherRelevantPublications( this.findOrCreateBibliographicReference( relevantPubmedId ) );

            // characteristics for an experiment
            Collection<Characteristic> characteristics = new HashSet<>();

            for ( CharacteristicValueObject chaValueObject : evidenceValueObject.getExperimentCharacteristics() ) {

                Characteristic experimentCha = Characteristic.Factory.newInstance();

                experimentCha.setValue( chaValueObject.getValue() );
                experimentCha.setCategory( chaValueObject.getCategory() );
                experimentCha.setValueUri( StringUtils.stripToNull( chaValueObject.getValueUri() ) );
                experimentCha.setCategoryUri( StringUtils.stripToNull( chaValueObject.getCategoryUri() ) );

                characteristics.add( experimentCha );
            }

            genericExperiment.getCharacteristics().addAll( characteristics );
            this.phenotypeAssociationService.create( genericExperiment );
        }

        experimentalEvidence.setExperiment( genericExperiment );
        return experimentalEvidence;
    }

    /**
     * @param  evidenceValueObject the evidence we want to convert
     * @return                     PhenotypeAssociation the entity created from the ValueObject
     */
    private PhenotypeAssociation conversion2GenericEvidence( GenericEvidenceValueObject evidenceValueObject ) {

        // create the entity to populate
        GenericEvidence genericEvidence = GenericEvidence.Factory.newInstance();

        // populate common field to evidence
        this.populatePhenotypeAssociation( genericEvidence, evidenceValueObject );

        return genericEvidence;
    }

    /**
     * @param  evidenceValueObject the evidence we want to convert
     * @return                     PhenotypeAssociation the entity created from the ValueObject
     */
    private PhenotypeAssociation conversion2LiteratureEvidence( LiteratureEvidenceValueObject evidenceValueObject ) {

        // create the entity to populate
        LiteratureEvidence literatureEvidence = new LiteratureEvidence();

        // populate common field to evidence
        this.populatePhenotypeAssociation( literatureEvidence, evidenceValueObject );

        return literatureEvidence;
    }

    /**
     * calls findOrCreateBibliographicReference for a Collection
     */
    private Set<BibliographicReference> findOrCreateBibliographicReference( Collection<String> pubMedIds ) {

        Set<BibliographicReference> bibliographicReferences = new HashSet<>();

        if ( pubMedIds != null && !pubMedIds.isEmpty() ) {
            for ( String pubmedId : pubMedIds ) {
                bibliographicReferences.add( this.findOrCreateBibliographicReference( pubmedId ) );
            }
        }
        return bibliographicReferences;
    }

    /**
     * Creates a BibliographicReference if it doesn't exist in the database
     *
     * @param pubMedId the pubmedID of the reference
     */
    private BibliographicReference findOrCreateBibliographicReference( String pubMedId ) {

        // check if already in the database
        BibliographicReference bibRef = this.bibliographicReferenceService.findByExternalId( pubMedId );

        if ( bibRef == null ) {
            try {
                bibRef = this.pubMedXmlFetcher.retrieveByHTTP( Integer.parseInt( pubMedId ) );
            } catch ( IOException e ) {
                throw new RuntimeException( e );
            }

            if ( bibRef == null ) {
                throw new EntityNotFoundException( "Could not locate reference with pubmed id=" + pubMedId );
            }
            bibRef = ( BibliographicReference ) this.persisterHelper.persist( bibRef );
        }

        return bibRef;
    }

    private void populateEvidenceSource( PhenotypeAssociation phe, EvidenceValueObject<?> evidenceValueObject ) {
        DatabaseEntryValueObject databaseEntryValueObject = evidenceValueObject.getEvidenceSource();

        // find the correct database
        ExternalDatabase externalDatabase = this.externalDatabaseService
                .findByName( databaseEntryValueObject.getExternalDatabase().getName() );

        if ( externalDatabase == null ) {
            throw new EntityNotFoundException(
                    "Could not locate External Database: " + databaseEntryValueObject.getExternalDatabase().getName() );
        }

        DatabaseEntry databaseEntry = DatabaseEntry.Factory.newInstance( externalDatabase );
        databaseEntry.setAccession( databaseEntryValueObject.getAccession() );

        databaseEntry = this.databaseEntryDao.create( databaseEntry );

        phe.setEvidenceSource( databaseEntry );
    }

    private void updatePhenotypeAssociationPublication( PhenotypeAssociation phe,
            EvidenceValueObject<? extends PhenotypeAssociation> evidenceValueObject ) {

        boolean toUpdate = false;

        if ( evidenceValueObject.getPhenotypeAssPubVO().size() != phe.getPhenotypeAssociationPublications().size() ) {
            toUpdate = true;
        } else {

            Collection<String> pubmeds = new HashSet<>();

            if ( !phe.getPhenotypeAssociationPublications().isEmpty() ) {

                for ( PhenotypeAssociationPublication associationPublication : phe
                        .getPhenotypeAssociationPublications() ) {
                    pubmeds.add( associationPublication.getCitation().getPubAccession().getAccession() );
                }
            }

            for ( PhenotypeAssPubValueObject p : evidenceValueObject.getPhenotypeAssPubVO() ) {

                if ( !pubmeds.contains( p.getCitationValueObject().getPubmedAccession() ) ) {
                    toUpdate = true;
                    break;
                }
            }
        }

        if ( toUpdate ) {

            for ( PhenotypeAssociationPublication associationPublication : phe.getPhenotypeAssociationPublications() ) {
                this.phenotypeAssociationService.removePhenotypePublication( associationPublication );
            }

            Collection<PhenotypeAssociationPublication> phenotypeAssociationPublications = new HashSet<>();

            this.processPASPVOs( evidenceValueObject, phenotypeAssociationPublications );

            phe.getPhenotypeAssociationPublications().clear();
            phe.getPhenotypeAssociationPublications().addAll( phenotypeAssociationPublications );
        }
    }

    private void populatePhenotypeAssociationPublication( PhenotypeAssociation phe,
            EvidenceValueObject<? extends PhenotypeAssociation> evidenceValueObject ) {

        Collection<PhenotypeAssociationPublication> phenotypeAssociationPublications = new HashSet<>();

        this.processPASPVOs( evidenceValueObject, phenotypeAssociationPublications );

        phe.getPhenotypeAssociationPublications().addAll( phenotypeAssociationPublications );
    }

    private void processPASPVOs( EvidenceValueObject<? extends PhenotypeAssociation> evidenceValueObject,
            Collection<PhenotypeAssociationPublication> phenotypeAssociationPublications ) {
        for ( PhenotypeAssPubValueObject phenotypeAssPubValueObject : evidenceValueObject.getPhenotypeAssPubVO() ) {

            PhenotypeAssociationPublication phenotypeAssociationPublication;

            if ( phenotypeAssPubValueObject != null && phenotypeAssPubValueObject.getCitationValueObject() != null ) {

                phenotypeAssociationPublication = PhenotypeAssociationPublication.Factory.newInstance();
                phenotypeAssociationPublication.setType( phenotypeAssPubValueObject.getType() );
                String pubmedId = phenotypeAssPubValueObject.getCitationValueObject().getPubmedAccession();
                phenotypeAssociationPublication.setCitation( this.findOrCreateBibliographicReference( pubmedId ) );
                phenotypeAssociationPublications.add( phenotypeAssociationPublication );
            }

        }
    }

    /**
     * Populate the phenotypes for an PhenotypeAssociation using an EvidenceValueObject
     *
     * @param phe                 The phenotype association (parent class of an evidence) we are interested in
     *                            populating
     * @param evidenceValueObject the value object representing a phenotype
     */
    private void populatePheAssoPhenotypes( PhenotypeAssociation phe,
            EvidenceValueObject<? extends PhenotypeAssociation> evidenceValueObject ) {
        // here lets add the phenotypes
        Collection<Characteristic> myPhenotypes = new HashSet<>();

        for ( CharacteristicValueObject phenotype : evidenceValueObject.getPhenotypes() ) {
            Characteristic c = this.ontologyHelper.valueUri2Characteristic( phenotype.getValueUri() );
            if ( c == null ) {
                throw new IllegalStateException(
                        phenotype.getValueUri() + " could not be converted to a characteristic" );
            }
            myPhenotypes.add( c );
        }

        phe.getPhenotypes().addAll( myPhenotypes );
    }

    private void setScoreInformation( EvidenceValueObject<?> evidenceValueObject,
            PhenotypeAssociation phenotypeAssociation ) {
        if ( evidenceValueObject.getScoreValueObject() != null ) {

            if ( evidenceValueObject.getScoreValueObject().getScoreName() != null && !evidenceValueObject
                    .getScoreValueObject().getScoreName().equals( "" ) ) {

                // find the score, we use the description which is : NeuroCarta + ScoreName
                List<QuantitationType> quantitationTypes = this.quantitationTypeService
                        .loadByDescription( "NeuroCarta " + evidenceValueObject.getScoreValueObject().getScoreName() );

                if ( quantitationTypes.size() != 1 ) {
                    throw new EntityNotFoundException(
                            "Could not locate Score used in database using description: " + "NeuroCarta "
                                    + evidenceValueObject.getScoreValueObject().getScoreName() );
                }

                phenotypeAssociation.setScoreType( quantitationTypes.iterator().next() );
                phenotypeAssociation.setScore( evidenceValueObject.getScoreValueObject().getScoreValue() );
                phenotypeAssociation.setStrength( evidenceValueObject.getScoreValueObject().getStrength() );
            } else if ( evidenceValueObject.getScoreValueObject().getStrength() != null && !evidenceValueObject
                    .getScoreValueObject().getStrength().equals( Double.NaN ) ) {
                phenotypeAssociation.setStrength( evidenceValueObject.getScoreValueObject().getStrength() );
            }
        } else if ( evidenceValueObject.getIsNegativeEvidence() ) {
            phenotypeAssociation.setStrength( 0.0 );
        }

    }

}