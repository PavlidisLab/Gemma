/*
 * The Gemma project
 * 
 * Copyright (c) 2011 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.association.phenotype.fileUpload.experimentalEvidence;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import ubic.basecode.ontology.model.OntologyTerm;
import ubic.basecode.ontology.providers.FMAOntologyService;
import ubic.basecode.ontology.providers.NIFSTDOntologyService;
import ubic.basecode.ontology.providers.ObiService;
import ubic.gemma.association.phenotype.fileUpload.EvidenceLoaderCLI;
import ubic.gemma.model.DatabaseEntryValueObject;
import ubic.gemma.model.ExternalDatabaseValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.CharacteristicValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.EvidenceSourceValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.EvidenceValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.ExperimentalEvidenceValueObject;

/**
 * take a tsv file for the CGMS and creates experimental evidence objects
 * 
 * @version $Id$
 * @author nicolas
 */
public class ExperimentalEvidenceLoaderCLI extends EvidenceLoaderCLI {

    // specific service used by ExperimentalEvidence
    private NIFSTDOntologyService nifstdOntologyService = null;
    private ObiService obiService = null;
    private FMAOntologyService fmaOntologyService = null;

    public static void main( String[] args ) {

        ExperimentalEvidenceLoaderCLI p = new ExperimentalEvidenceLoaderCLI();

        try {
            // to pass args by the command line dont use the initArguments method
            // Exception ex = p.doWork( args );

            // arguments were hardcoded to make it more easy
            Exception ex = p.doWork( initArguments() );

            if ( ex != null ) {
                ex.printStackTrace();
            }
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

    /** There are 6 Steps in the process of creating evidence */
    @Override
    protected Exception doWork( String[] args ) {
        Exception err = processCommandLine( "PhenotypeAssociationLoader", args );
        if ( err != null ) return err;

        try {

            System.out.println( "STEP 1 : Load Ontology" );
            loadServices();

            System.out.println( "STEP 2 : Extract the data from the file" );
            Collection<ExpEvidenceLineInfo> linesFromFile = file2Objects();

            System.out.println( "STEP 3 : Convert file to Ontology terms" );
            convertExperimentalOntologiesTerms( linesFromFile );

            if ( this.testEnvironment ) {
                System.err.println( this.testMessage );
            }

            // make a intermediate tsv file to check is Ontology correctly mapped (used by students to verify data)
            System.out.println( "STEP 4 : Create intermediate file with uri from ontology" );
            writeFileWithOntology( linesFromFile );

            // check if all Gene ID can be found in Gemma
            System.out.println( "STEP 5 : Verify is all Gene ID exist in Gemma" );
            verifyGeneIdExist( linesFromFile );

            // called as the final step to create the object in the database
            if ( this.createInDatabase ) {
                System.out.println( "STEP 6 : Create evidence in the database" );
                createEvidenceInDatabase( linesFromFile );
                System.out.println( "Evidence inserted in the database" );
            }

        } catch ( Exception e ) {
            return e;
        }

        return null;
    }

    @Override
    protected synchronized void loadServices() throws Exception {
        super.loadServices();

        // those services are specific to this type of evidence
        this.nifstdOntologyService = this.ontologyService.getNifstfOntologyService();
        this.obiService = this.ontologyService.getObiService();
        this.fmaOntologyService = this.ontologyService.getFmaOntologyService();

        while ( this.obiService.isOntologyLoaded() == false ) {
            wait( 1000 );
            System.out.println( "waiting for OBI Ontology to load" );
        }

        while ( this.nifstdOntologyService.isOntologyLoaded() == false ) {
            wait( 1000 );
            System.out.println( "waiting for NIF Ontology to load" );
        }

        while ( this.fmaOntologyService.isOntologyLoaded() == false ) {
            wait( 1000 );
            System.out.println( "waiting for FMA Ontology to load" );
        }
    }

    /**
     * search term in the obi Ontology
     * 
     * @throws Exception
     */
    private String obi2OntologyExperimentOBI( ExpEvidenceLineInfo lineInfo, int index ) throws Exception {

        String search = lineInfo.getExperimentOBI()[index];

        Collection<OntologyTerm> ontologyTerms = this.obiService.findTerm( search );

        OntologyTerm ot = findExactTerm( ontologyTerms, search );

        if ( ot != null ) {
            lineInfo.getExperimentOBI()[index] = ot.getLabel();
            return ot.getUri();
        }
        System.out.println( "term not found in obi Ontology : " + search );
        return null;
    }

    /**
     * search term in the nifstd Ontology
     * 
     * @throws Exception
     */
    private String nifstd2Ontology( ExpEvidenceLineInfo lineInfo, int index ) throws Exception {

        String search = lineInfo.getDevelopmentStage()[index];

        Collection<OntologyTerm> ontologyTerms = this.nifstdOntologyService.findTerm( search );

        OntologyTerm ot = findExactTerm( ontologyTerms, search );

        if ( ot != null ) {
            lineInfo.getDevelopmentStage()[index] = ot.getLabel();
            return ot.getUri();
        }
        System.out.println( "term not found in nif Ontology : " + search );
        return null;

    }

    /**
     * search term in the fma Ontology
     * 
     * @throws Exception
     */
    private String fma2Ontology( ExpEvidenceLineInfo lineInfo, int index ) throws Exception {

        String search = lineInfo.getOrganismPart()[index];

        Collection<OntologyTerm> ontologyTerms = this.fmaOntologyService.findTerm( search );

        OntologyTerm ot = findExactTerm( ontologyTerms, search );

        if ( ot != null ) {
            lineInfo.getOrganismPart()[index] = ot.getLabel();
            return ot.getUri();
        }
        System.out.println( "term not found in nif Ontology : " + search );
        return null;

    }

    /**
     * search term in the obi Ontology
     * 
     * @throws Exception
     */
    private String obi2OntologyExperimentDesign( ExpEvidenceLineInfo lineInfo, int index ) throws Exception {

        String search = lineInfo.getExperimentDesign()[index];

        // search disease
        Collection<OntologyTerm> ontologyTerms = this.obiService.findTerm( search );

        OntologyTerm ot = findExactTerm( ontologyTerms, search );

        if ( ot != null ) {
            lineInfo.getExperimentDesign()[index] = ot.getLabel();
            return ot.getUri();
        }
        System.out.println( "term not found in obi Ontology : " + search );
        return null;

    }

    /** change each line of the file by Ontology terms */
    private void convertExperimentalOntologiesTerms( Collection<ExpEvidenceLineInfo> expEvidenceLineInfos )
            throws Exception {

        // take care of the phenotype
        super.convertOntologiesTerms( expEvidenceLineInfos );

        // othr value are specific to this type of evidence

        int line = 1;

        for ( ExpEvidenceLineInfo lineInfo : expEvidenceLineInfos ) {

            line++;

            System.out.println( "Treating Ontology terms for line (Not Phenotype): " + line );

            // The DevelopmentStage column get converted
            for ( int i = 0; i < lineInfo.getDevelopmentStage().length; i++ ) {
                if ( !lineInfo.getDevelopmentStage()[i].equalsIgnoreCase( "" ) ) {

                    String valueURI = nifstd2Ontology( lineInfo, i );

                    CharacteristicValueObject characteristic = new CharacteristicValueObject(
                            lineInfo.getDevelopmentStage()[i], ExpEvidenceLineInfo.DEVELOPMENTAL_STAGE, valueURI,
                            ExpEvidenceLineInfo.DEVELOPMENTAL_STAGE_ONTOLOGY );
                    lineInfo.addExperimentCharacteristic( characteristic );
                }
            }

            // The BioSource column get converted ( no Ontology to convert )
            for ( int i = 0; i < lineInfo.getBioSource().length; i++ ) {
                if ( !lineInfo.getBioSource()[i].equalsIgnoreCase( "" ) ) {
                    CharacteristicValueObject characteristic = new CharacteristicValueObject(
                            lineInfo.getBioSource()[i], ExpEvidenceLineInfo.BIOSOURCE, null,
                            ExpEvidenceLineInfo.BIOSOURCE_ONTOLOGY );
                    lineInfo.addExperimentCharacteristic( characteristic );
                }
            }

            // The OrganismPart column get converted
            for ( int i = 0; i < lineInfo.getOrganismPart().length; i++ ) {
                if ( !lineInfo.getOrganismPart()[i].equalsIgnoreCase( "" ) ) {

                    String valueURI = fma2Ontology( lineInfo, i );

                    CharacteristicValueObject characteristic = new CharacteristicValueObject(
                            lineInfo.getOrganismPart()[i], ExpEvidenceLineInfo.ORGANISM_PART, valueURI,
                            ExpEvidenceLineInfo.ORGANISM_PART_ONTOLOGY );
                    lineInfo.addExperimentCharacteristic( characteristic );
                }
            }

            // The ExperimentDesign column
            for ( int i = 0; i < lineInfo.getExperimentDesign().length; i++ ) {
                if ( !lineInfo.getExperimentDesign()[i].equalsIgnoreCase( "" ) ) {

                    String valueURI = obi2OntologyExperimentDesign( lineInfo, i );

                    CharacteristicValueObject characteristic = new CharacteristicValueObject(
                            lineInfo.getExperimentDesign()[i], ExpEvidenceLineInfo.EXPERIMENT_DESIGN, valueURI,
                            ExpEvidenceLineInfo.EXPERIMENT_DESIGN_ONTOLOGY );
                    lineInfo.addExperimentCharacteristic( characteristic );
                }
            }

            // The Treatment column get converted ( no Ontology to convert )
            for ( int i = 0; i < lineInfo.getTreatment().length; i++ ) {
                if ( !lineInfo.getTreatment()[i].equalsIgnoreCase( "" ) ) {
                    CharacteristicValueObject characteristic = new CharacteristicValueObject(
                            lineInfo.getTreatment()[i], ExpEvidenceLineInfo.TREATMENT, null,
                            ExpEvidenceLineInfo.TREATMENT_ONTOLOGY );
                    lineInfo.addExperimentCharacteristic( characteristic );
                }
            }

            // The ExperimentOBI column get converted
            for ( int i = 0; i < lineInfo.getExperimentOBI().length; i++ ) {
                if ( !lineInfo.getExperimentOBI()[i].equalsIgnoreCase( "" ) ) {

                    String valueURI = obi2OntologyExperimentOBI( lineInfo, i );

                    CharacteristicValueObject characteristic = new CharacteristicValueObject(
                            lineInfo.getExperimentOBI()[i], ExpEvidenceLineInfo.EXPERIMENT, valueURI,
                            ExpEvidenceLineInfo.EXPERIMENT_ONTOLOGY );
                    lineInfo.addExperimentCharacteristic( characteristic );
                }
            }
        }
    }

    /**
     * Step 6 populate the evidence and save it to the database calling the service
     * 
     * @throws Exception
     */
    private void createEvidenceInDatabase( Collection<ExpEvidenceLineInfo> linesFromFile ) throws Exception {

        int evidenceNumber = 1;

        // for each evidence found, we need to populate its evidenceObject and to call the service to save it
        for ( ExpEvidenceLineInfo phenoAss : linesFromFile ) {

            String description = phenoAss.getComment();
            CharacteristicValueObject associationType = null;

            if ( !phenoAss.getAssociationType().equalsIgnoreCase( "" ) ) {
                // associationType = new CharacteristicValueObject( "Association Type", phenoAss.getAssociationType() );
            }
            String evidenceCode = phenoAss.getEvidenceCode();
            String primaryPublicationPubmed = phenoAss.getPrimaryReferencePubmed();
            String relevantPublicationPubmed = phenoAss.getReviewReferencePubmed();
            Set<String> relevantPublicationsPubmed = new HashSet<String>();

            if ( !relevantPublicationPubmed.equalsIgnoreCase( "" ) ) {

                relevantPublicationsPubmed.add( relevantPublicationPubmed );
            }

            Set<CharacteristicValueObject> phenotypes = phenoAss.getPhenotypes();

            Set<CharacteristicValueObject> characteristics = phenoAss.getExperimentCharacteristics();

            EvidenceSourceValueObject evidenceSource = null;

            if ( phenoAss.getExternalDatabaseName() != null
                    && !phenoAss.getExternalDatabaseName().trim().equalsIgnoreCase( "" ) ) {

                ExternalDatabaseValueObject externalDatabase = new ExternalDatabaseValueObject();
                externalDatabase.setName( phenoAss.getExternalDatabaseName() );

                DatabaseEntryValueObject databaseEntryValueObject = new DatabaseEntryValueObject();

                databaseEntryValueObject.setAccession( phenoAss.getDatabaseID() );
                databaseEntryValueObject.setExternalDatabase( externalDatabase );

                evidenceSource = new EvidenceSourceValueObject( phenoAss.getDatabaseID(), externalDatabase );
            }

            EvidenceValueObject evidence = new ExperimentalEvidenceValueObject( new Integer( phenoAss.getGeneID() ),
                    phenotypes, description, evidenceCode, phenoAss.isEdivenceNegative(), evidenceSource,
                    associationType, primaryPublicationPubmed, relevantPublicationsPubmed, characteristics );

            try {

                this.phenotypeAssociationService.create( evidence );
                System.out.println( "Evidence " + evidenceNumber + " created" );

            } catch ( Exception e ) {
                System.out.println( "Evidence " + evidenceNumber + " was NOT Created: " + e.getMessage() );
                throw e;
            }
            evidenceNumber++;
        }

    }

    /**
     * used by Step 4 to make an intermediate file to check before the insert in the database, with terms replaced by
     * Ontology
     */
    private void writeFileWithOntology( Collection<ExpEvidenceLineInfo> linesFromFile ) throws IOException {

        BufferedWriter out = new BufferedWriter( new FileWriter(
                "./gemma-core/src/main/java/ubic/gemma/association/phenotype/fileUpload/intermediateOutputFile.tsv" ) );

        out.write( "Gene ID" + "\t" + "Experimental Source (PMID)" + "\t" + "Review Source (PMID)" + "\t"
                + "EvidenceCode" + "\t" + "Comments" + "\t" + "Association Type" + "\t" + "Value" + "\t" + "ValueUri"
                + "\t" + "Category" + "\t" + "CategoryUri" + "\n" );

        for ( ExpEvidenceLineInfo p : linesFromFile ) {

            for ( CharacteristicValueObject c : p.getExperimentCharacteristics() ) {

                out.write( p.getGeneID() + "\t" + p.getPrimaryReferencePubmed() + "\t" + p.getReviewReferencePubmed()
                        + "\t" + p.getEvidenceCode() + "\t" + p.getComment() + "\t" + p.getAssociationType() + "\t"
                        + c.getValue() + "\t" + c.getValueUri() + "\t" + c.getCategory() + "\t" + c.getCategoryUri()
                        + "\n" );
            }

            for ( CharacteristicValueObject phe : p.getPhenotypes() ) {

                out.write( p.getGeneID() + "\t" + p.getPrimaryReferencePubmed() + "\t" + p.getReviewReferencePubmed()
                        + "\t" + p.getEvidenceCode() + "\t" + p.getComment() + "\t" + p.getAssociationType() + "\t"
                        + phe.getValue() + "\t" + phe.getValueUri() + "\t" + phe.getCategory() + "\t"
                        + phe.getCategoryUri() + "\n" );
            }
        }

        out.close();
    }

    /** Take the file and transform it into an object structure for each line */
    private Collection<ExpEvidenceLineInfo> file2Objects() throws IOException {

        Collection<ExpEvidenceLineInfo> ExpEvidenceLineInfos = new ArrayList<ExpEvidenceLineInfo>();

        BufferedReader br = new BufferedReader( new FileReader( this.inputFile ) );

        String line;
        int lineNumber = 0;

        // for each line of the file
        while ( ( line = br.readLine() ) != null ) {
            lineNumber++;

            if ( lineNumber != 1 ) {

                System.out.println( "Creating object for line: " + lineNumber );
                ExpEvidenceLineInfos.add( new ExpEvidenceLineInfo( line ) );
            }
        }

        br.close();

        return ExpEvidenceLineInfos;
    }

}