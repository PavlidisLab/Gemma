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

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;

import ubic.basecode.ontology.model.OntologyTerm;
import ubic.basecode.ontology.providers.DiseaseOntologyService;
import ubic.basecode.ontology.providers.FMAOntologyService;
import ubic.basecode.ontology.providers.HumanPhenotypeOntologyService;
import ubic.basecode.ontology.providers.MammalianPhenotypeOntologyService;
import ubic.basecode.ontology.providers.NIFSTDOntologyService;
import ubic.basecode.ontology.providers.ObiService;
import ubic.gemma.association.phenotype.PhenotypeAssociationManagerService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.gene.GeneService;
import ubic.gemma.model.genome.gene.phenotype.valueObject.CharacteristicValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.EvidenceValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.ExperimentalEvidenceValueObject;
import ubic.gemma.ontology.OntologyService;
import ubic.gemma.util.AbstractSpringAwareCLI;

/**
 * take a tsv file for the CGMS and creates experimental evidence objects
 * 
 * @version $Id$
 * @author nicolas
 */
public class ExperimentalEvidenceLoaderCLI extends AbstractSpringAwareCLI {

    // input file path
    private String inputFile = "";
    // flag to decided if the data is ready to be imported
    private Boolean createInDatabase = false;

    private OntologyService ontologyService = null;
    private PhenotypeAssociationManagerService phenotypeAssociationService = null;
    private GeneService geneService = null;

    // Ontology services used
    private DiseaseOntologyService diseaseOntologyService = null;
    private MammalianPhenotypeOntologyService mammalianPhenotypeOntologyService = null;
    private HumanPhenotypeOntologyService humanPhenotypeOntologyService = null;
    private NIFSTDOntologyService nifstdOntologyService = null;
    private ObiService obiService = null;
    private FMAOntologyService fmaOntologyService = null;

    public static void main( String[] args ) {

        // example of parameters
        args = new String[7];
        args[0] = "-u";
        args[1] = "administrator";
        args[2] = "-p";
        args[3] = "administrator";
        args[4] = "-f";
        args[5] = "./gemma-core/src/main/java/ubic/gemma/association/phenotype/fileUpload/experimentalEvidence/Willie.tsv";
        args[6] = "-create";

        ExperimentalEvidenceLoaderCLI p = new ExperimentalEvidenceLoaderCLI();

        try {
            Exception ex = p.doWork( args );
            if ( ex != null ) {
                ex.printStackTrace();
            }
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

    @Override
    protected void buildOptions() {

        @SuppressWarnings("static-access")
        Option fileOption = OptionBuilder.withDescription( "The file" ).hasArg().withArgName( "file path" )
                .isRequired().create( "f" );
        addOption( fileOption );

        @SuppressWarnings("static-access")
        Option createInDatabase = OptionBuilder.withDescription( "Create in database" ).create( "create" );
        addOption( createInDatabase );

    }

    @Override
    protected void processOptions() {
        super.processOptions();
        inputFile = getOptionValue( 'f' );

        if ( hasOption( "create" ) ) {
            createInDatabase = true;
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
            Collection<ExpEvidenceLineInfo> linesFromFile = file2Objects( inputFile );

            System.out.println( "STEP 3 : Convert file to Ontology terms" );
            convertOntologiesTerms( linesFromFile );

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

    /** load services and verify that Ontology are loaded */
    private synchronized void loadServices() throws Exception {

        this.phenotypeAssociationService = ( PhenotypeAssociationManagerService ) this
                .getBean( "phenotypeAssociationManagerService" );

        this.geneService = ( GeneService ) this.getBean( "geneService" );

        this.ontologyService = ( OntologyService ) this.getBean( "ontologyService" );

        this.diseaseOntologyService = ontologyService.getDiseaseOntologyService();
        this.mammalianPhenotypeOntologyService = ontologyService.getMammalianPhenotypeOntologyService();
        this.humanPhenotypeOntologyService = ontologyService.getHumanPhenotypeOntologyService();
        this.nifstdOntologyService = ontologyService.getNifstfOntologyService();
        this.obiService = ontologyService.getObiService();
        this.fmaOntologyService = ontologyService.getFmaOntologyService();

        while ( this.diseaseOntologyService.isOntologyLoaded() == false ) {
            wait( 1000 );
            System.out.println( "waiting for Disease Ontology to load" );
        }

        while ( this.mammalianPhenotypeOntologyService.isOntologyLoaded() == false ) {
            wait( 1000 );
            System.out.println( "waiting for MP Ontology to load" );
        }

        while ( this.humanPhenotypeOntologyService.isOntologyLoaded() == false ) {
            wait( 1000 );
            System.out.println( "waiting for HP Ontology to load" );
        }

        while ( this.obiService.isOntologyLoaded() == false ) {
            wait( 1000 );
            System.out.println( "waiting for OBI Ontology to load" );
        }

        while ( this.nifstdOntologyService.isOntologyLoaded() == false ) {
            wait( 1000 );
            System.out.println( "waiting for NIF Ontology to load" );
        }

        while ( this.ontologyService.getFmaOntologyService().isOntologyLoaded() == false ) {
            wait( 1000 );
            System.out.println( "waiting for FMA Ontology to load" );
        }
    }

    /** Take the file and transform it into an object structure for each line */
    private Collection<ExpEvidenceLineInfo> file2Objects( String inputFile ) throws IOException {

        Collection<ExpEvidenceLineInfo> ExpEvidenceLineInfos = new ArrayList<ExpEvidenceLineInfo>();

        BufferedReader br = new BufferedReader( new FileReader( inputFile ) );

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

    /**
     * find the exact term of a search term in a Collection of Ontology terms
     * 
     * @param ontologyTerms Collection of ontologyTerms
     * @param search The value we are interested in finding
     * @return OntologyTerm the exact match value found
     */
    // TODO we dont need this part just use "" to find exact terms...
    private OntologyTerm findExactTerm( Collection<OntologyTerm> ontologyTerms, String search ) {

        // list of OntologyTerms found
        Collection<OntologyTerm> ontologyKept = new HashSet<OntologyTerm>();
        OntologyTerm termFound = null;

        for ( OntologyTerm ot : ontologyTerms ) {
            if ( ot.getLabel() != null ) {
                if ( ot.getLabel().equalsIgnoreCase( search ) ) {
                    ontologyKept.add( ot );
                    termFound = ot;
                }
            }
        }

        // if we have more than 1 result, hardcode the one to choose
        if ( ontologyKept.size() > 1 ) {

            if ( search.equalsIgnoreCase( "juvenile" ) ) {

                for ( OntologyTerm ontologyTerm : ontologyKept ) {
                    if ( ontologyTerm.getUri().equalsIgnoreCase( "http://purl.org/obo/owl/PATO#PATO_0001190" ) ) {
                        return ontologyTerm;
                    }
                }
            } else if ( search.equalsIgnoreCase( "adult" ) ) {

                for ( OntologyTerm ontologyTerm : ontologyKept ) {

                    if ( ontologyTerm.getUri().equalsIgnoreCase(
                            "http://ontology.neuinfo.org/NIF/BiomaterialEntities/NIF-Organism.owl#birnlex_681" ) ) {
                        return ontologyTerm;
                    }
                }
            } else if ( search.equalsIgnoreCase( "newborn" ) ) {

                for ( OntologyTerm ontologyTerm : ontologyKept ) {

                    if ( ontologyTerm.getUri().equalsIgnoreCase(
                            "http://ontology.neuinfo.org/NIF/BiomaterialEntities/NIF-Organism.owl#birnlex_699" ) ) {
                        return ontologyTerm;
                    }
                }
            } else if ( search.equalsIgnoreCase( "prenatal" ) ) {

                for ( OntologyTerm ontologyTerm : ontologyKept ) {

                    if ( ontologyTerm.getUri().equalsIgnoreCase(
                            "http://ontology.neuinfo.org/NIF/BiomaterialEntities/NIF-Organism.owl#birnlex_7014" ) ) {
                        return ontologyTerm;
                    }
                }
            } else if ( search.equalsIgnoreCase( "infant" ) ) {

                for ( OntologyTerm ontologyTerm : ontologyKept ) {

                    if ( ontologyTerm.getUri().equalsIgnoreCase(
                            "http://ontology.neuinfo.org/NIF/BiomaterialEntities/NIF-Organism.owl#birnlex_695" ) ) {
                        return ontologyTerm;
                    }
                }
            }
        }

        if ( ontologyKept.size() > 1 ) {

            System.err.println( "More than 1 term found for : " + search + "   " + ontologyKept.size() );
        }

        return termFound;
    }

    /** search phenotype term the diseaseOntology then hp, then mp */
    private String phenotype2Ontology( ExpEvidenceLineInfo lineInfo, int index ) throws Exception {

        String search = lineInfo.getPhenotype()[index];

        // search disease
        Collection<OntologyTerm> ontologyTerms = this.diseaseOntologyService.findTerm( "\""+search+  "\"");

        OntologyTerm ot = findExactTerm( ontologyTerms, search );

        if ( ot != null ) {
            lineInfo.getPhenotype()[index] = ot.getLabel();
            return ot.getUri();
        }

        // search hp
        ontologyTerms = this.humanPhenotypeOntologyService.findTerm( "\""+search+  "\"");

        ot = findExactTerm( ontologyTerms, search );

        if ( ot != null ) {
            lineInfo.getPhenotype()[index] = ot.getLabel();
            return ot.getUri();
        }

        // search mamalian
        ontologyTerms = this.mammalianPhenotypeOntologyService.findTerm( "\""+search+  "\"");

        ot = findExactTerm( ontologyTerms, search );

        if ( ot != null ) {
            lineInfo.getPhenotype()[index] = ot.getLabel();
            return ot.getUri();
        }

        // all phenotypes must be find
        System.err.println( "phenotype not found in disease, hp and mp Ontology : " + search );
        return null;
    }

    /** search term in the obi Ontology */
    private String obi2OntologyExperimentDesign( ExpEvidenceLineInfo lineInfo, int index ) {

        String search = lineInfo.getExperimentDesign()[index];

        // search disease
        Collection<OntologyTerm> ontologyTerms = this.obiService.findTerm( "\""+search+  "\"");

        OntologyTerm ot = findExactTerm( ontologyTerms, search );

        if ( ot != null ) {
            lineInfo.getExperimentDesign()[index] = ot.getLabel();
            return ot.getUri();
        }
        System.out.println( "term not found in obi Ontology : " + search );
        return null;

    }

    /** search term in the obi Ontology */
    private String obi2OntologyExperimentOBI( ExpEvidenceLineInfo lineInfo, int index ) {

        String search = lineInfo.getExperimentOBI()[index];

        // search disease
        Collection<OntologyTerm> ontologyTerms = this.obiService.findTerm( "\""+search+  "\"");

        OntologyTerm ot = findExactTerm( ontologyTerms, search );

        if ( ot != null ) {
            lineInfo.getExperimentOBI()[index] = ot.getLabel();
            return ot.getUri();
        }
        System.out.println( "term not found in obi Ontology : " + search );
        return null;

    }

    /** search term in the nifstd Ontology */
    private String nifstd2Ontology( ExpEvidenceLineInfo lineInfo, int index ) {

        String search = lineInfo.getDevelopmentStage()[index];

        Collection<OntologyTerm> ontologyTerms = this.nifstdOntologyService.findTerm( "\""+search+  "\"");

        OntologyTerm ot = findExactTerm( ontologyTerms, search );

        if ( ot != null ) {
            lineInfo.getDevelopmentStage()[index] = ot.getLabel();
            return ot.getUri();
        }
        System.out.println( "term not found in nif Ontology : " + search );
        return null;

    }

    /** search term in the fma Ontology */
    private String fma2Ontology( ExpEvidenceLineInfo lineInfo, int index ) {

        String search = lineInfo.getOrganismPart()[index];

        Collection<OntologyTerm> ontologyTerms = this.fmaOntologyService.findTerm( "\""+search+  "\"");

        OntologyTerm ot = findExactTerm( ontologyTerms, search );

        if ( ot != null ) {
            lineInfo.getOrganismPart()[index] = ot.getLabel();
            return ot.getUri();
        }
        System.out.println( "term not found in nif Ontology : " + search );
        return null;

    }

    /** change each line of the file by Ontology terms */
    private void convertOntologiesTerms( Collection<ExpEvidenceLineInfo> ExpEvidenceLineInfos ) throws Exception {

        int line = 1;

        for ( ExpEvidenceLineInfo lineInfo : ExpEvidenceLineInfos ) {

            line++;

            System.out.println( "Treating Ontology terms for line: " + line );

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

            // The phenotype column
            for ( int i = 0; i < lineInfo.getPhenotype().length; i++ ) {
                if ( !lineInfo.getPhenotype()[i].equalsIgnoreCase( "" ) ) {

                    String valueURI = phenotype2Ontology( lineInfo, i );

                    CharacteristicValueObject phenotype = new CharacteristicValueObject( lineInfo.getPhenotype()[i],
                            ExpEvidenceLineInfo.PHENOTYPE, valueURI, ExpEvidenceLineInfo.PHENOTYPE_ONTOLOGY );
                    lineInfo.addPhenotype( phenotype );
                }
            }
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

    /**
     * Step 5 check that all gene exists in Gemma
     * 
     * @throws Exception
     */
    private void verifyGeneIdExist( Collection<ExpEvidenceLineInfo> linesFromFile ) throws Exception {

        int i = 0;

        for ( ExpEvidenceLineInfo lineInfo : linesFromFile ) {

            i++;

            Gene gene = this.geneService.findByNCBIId(Integer.parseInt( lineInfo.getGeneID()) );

            if ( gene == null ) {
                System.err.println( "Gene not found in Gemma: " + lineInfo.getGeneID() + " Description: "
                        + lineInfo.getComment() );
            } else if ( !gene.getName().equalsIgnoreCase( lineInfo.getGeneName() ) ) {
                System.err.println( "************Different Gene name found************" );
                System.err.println( "LINE: " + i );
                System.err.println( "Gene name in File: " + lineInfo.getGeneName() );
                System.err.println( "Gene name in Gemma: " + gene.getName() );
                System.err.println( "*************************************************" );
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

            EvidenceValueObject evidence = new ExperimentalEvidenceValueObject( description, associationType,
                    phenoAss.getIsEdivenceNegative(), evidenceCode, phenotypes, primaryPublicationPubmed,
                    relevantPublicationsPubmed, characteristics );

            String geneId = phenoAss.getGeneID();

            try {

                this.phenotypeAssociationService.create( geneId, evidence );
                System.out.println( "Evidence " + evidenceNumber + " created" );

            } catch ( Exception e ) {
                System.out.println( "Evidence " + evidenceNumber + " was NOT Created: " + e.getMessage() );
                // throw e;
            }
            evidenceNumber++;
        }

    }
}