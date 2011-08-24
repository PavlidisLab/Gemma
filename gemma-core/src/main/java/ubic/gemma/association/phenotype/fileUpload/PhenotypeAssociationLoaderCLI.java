package ubic.gemma.association.phenotype.fileUpload;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

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
import ubic.gemma.model.genome.gene.GeneService;
import ubic.gemma.model.genome.gene.phenotype.valueObject.CharacteristicValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.EvidenceValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.ExperimentalEvidenceValueObject;
import ubic.gemma.ontology.OntologyService;
import ubic.gemma.util.AbstractSpringAwareCLI;

/** take a tsv file for the CGMS and creates experimental evidences objects */
public class PhenotypeAssociationLoaderCLI extends AbstractSpringAwareCLI {

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
        args[5] = "./gemma-core/src/main/java/ubic/gemma/association/phenotype/fileUpload/ArtemisInputFile2.tsv";
        args[6] = "-create";

        PhenotypeAssociationLoaderCLI p = new PhenotypeAssociationLoaderCLI();

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

    /** There are 5 Steps in the process of creating the evidences */
    @Override
    protected Exception doWork( String[] args ) {
        Exception err = processCommandLine( "PhenotypeAssociationLoader", args );
        if ( err != null ) return err;

        try {

            System.out.println( "STEP 1 : Load Ontology" );
            loadServices();

            System.out.println( "STEP 2 : Extract the data from the file" );
            Collection<EvidenceLineInfo> linesFromFile = file2Objects( inputFile );

            System.out.println( "STEP 3 : Convert file to Ontology terms" );
            convertOntologiesTerms( linesFromFile );

            // make a intermediate tsv file to check is Ontology correctly mapped (used by students to verify data)
            System.out.println( "STEP 4 : Create intermediate file with uri from ontology" );
            writeFileWithOntology( linesFromFile );

            // check if all Gene ID can be found in Gemma
            System.out.println( "STEP 5 : Verify is all Gene ID exist in Gemma" );
            verifyGeneIdExist( linesFromFile );

            // called as the final step to create the object in the database
            if ( createInDatabase ) {
                System.out.println( "STEP 6 : Create the evidences in the database" );
                createEvidencesInDatabase( linesFromFile );
                System.out.println( "Evidences inserted in the database" );
            }

        } catch ( Exception e ) {
            return e;
        }

        return null;
    }

    /** load services and verify that Ontology are loaded */
    private synchronized void loadServices() throws Exception {

        phenotypeAssociationService = ( PhenotypeAssociationManagerService ) this
                .getBean( "phenotypeAssociationManagerService" );

        geneService = ( GeneService ) this.getBean( "geneService" );

        ontologyService = ( OntologyService ) this.getBean( "ontologyService" );

        diseaseOntologyService = ontologyService.getDiseaseOntologyService();
        mammalianPhenotypeOntologyService = ontologyService.getMammalianPhenotypeOntologyService();
        humanPhenotypeOntologyService = ontologyService.getHumanPhenotypeOntologyService();
        nifstdOntologyService = ontologyService.getNifstfOntologyService();
        obiService = ontologyService.getObiService();
        fmaOntologyService = ontologyService.getFmaOntologyService();

        while ( diseaseOntologyService.isOntologyLoaded() == false ) {
            wait( 1000 );
            System.out.println( "waiting for Disease Ontology to load" );
        }

        while ( mammalianPhenotypeOntologyService.isOntologyLoaded() == false ) {
            wait( 1000 );
            System.out.println( "waiting for MP Ontology to load" );
        }

        while ( humanPhenotypeOntologyService.isOntologyLoaded() == false ) {
            wait( 1000 );
            System.out.println( "waiting for HP Ontology to load" );
        }

        while ( obiService.isOntologyLoaded() == false ) {
            wait( 1000 );
            System.out.println( "waiting for OBI Ontology to load" );
        }

        while ( nifstdOntologyService.isOntologyLoaded() == false ) {
            wait( 1000 );
            System.out.println( "waiting for NIF Ontology to load" );
        }

        while ( ontologyService.getFmaOntologyService().isOntologyLoaded() == false ) {
            wait( 1000 );
            System.out.println( "waiting for FMA Ontology to load" );
        }
    }

    /** Take the file and transform it into an object structure for each line */
    private Collection<EvidenceLineInfo> file2Objects( String inputFile ) throws IOException {

        Collection<EvidenceLineInfo> evidenceLineInfos = new ArrayList<EvidenceLineInfo>();

        BufferedReader br = new BufferedReader( new InputStreamReader( new DataInputStream( new FileInputStream(
                inputFile ) ) ) );

        String line;
        int lineNumber = 0;

        // for each line of the file
        while ( ( line = br.readLine() ) != null ) {
            lineNumber++;

            if ( lineNumber != 1 ) {

                System.out.println( "Creating object for line: " + lineNumber );
                evidenceLineInfos.add( new EvidenceLineInfo( line ) );
            }
        }

        br.close();

        return evidenceLineInfos;
    }

    /**
     * find the exact term of a search term in a Collection of Ontology terms
     * 
     * @param ontologyTerms Collection of ontologyTerms
     * @param search The value we are interested in finding
     * @return OntologyTerm the exact match value found
     */
    private OntologyTerm findExactTerm( Collection<OntologyTerm> ontologyTerms, String search ) {

        for ( OntologyTerm ot : ontologyTerms ) {
            if ( ot.getLabel() != null ) {
                if ( ot.getLabel().equalsIgnoreCase( search ) ) {
                    return ot;
                }
            } else if ( ot.toString().equalsIgnoreCase( search ) ) {
                return ot;
            }
        }
        return null;
    }

    /** search phenotype term the diseaseOntology then hp, then mp */
    private String phenotype2Ontology( String search ) throws Exception {

        // search disease
        Collection<OntologyTerm> ontologyTerms = diseaseOntologyService.findTerm( search );

        OntologyTerm ot = findExactTerm( ontologyTerms, search );

        if ( ot != null ) {
            return ot.getUri();
        }

        // search hp
        ontologyTerms = humanPhenotypeOntologyService.findTerm( search );

        ot = findExactTerm( ontologyTerms, search );

        if ( ot != null ) {
            return ot.getUri();
        }

        // search mamalian
        ontologyTerms = mammalianPhenotypeOntologyService.findTerm( search );

        ot = findExactTerm( ontologyTerms, search );

        if ( ot != null ) {
            return ot.getUri();
        }

        // all phenotypes must be find
        System.err.println( "phenotype not found in disease, hp and mp Ontology : " + search );
        return null;
        // throw new Exception( "phenotype not found in disease, hp and mp Ontology : " + search );
    }

    /** search term in the obi Ontology */
    private String obi2Ontology( String search ) {

        // search disease
        Collection<OntologyTerm> ontologyTerms = obiService.findTerm( search );

        OntologyTerm ot = findExactTerm( ontologyTerms, search );

        if ( ot != null ) {
            return ot.getUri();
        } else {
            System.out.println( "term not found in obi Ontology : " + search );
            return null;
        }
    }

    /** search term in the nifstd Ontology */
    private String nifstd2Ontology( String search ) {

        Collection<OntologyTerm> ontologyTerms = nifstdOntologyService.findTerm( search );

        OntologyTerm ot = findExactTerm( ontologyTerms, search );

        if ( ot != null ) {
            return ot.getUri();
        } else {
            System.out.println( "term not found in nif Ontology : " + search );
            return null;
        }
    }

    /** search term in the fma Ontology */
    private String fma2Ontology( String search ) {

        Collection<OntologyTerm> ontologyTerms = fmaOntologyService.findTerm( search );

        OntologyTerm ot = findExactTerm( ontologyTerms, search );

        if ( ot != null ) {
            return ot.getUri();
        } else {
            System.out.println( "term not found in nif Ontology : " + search );
            return null;
        }
    }

    /** change each line of the file by Ontology terms */
    private void convertOntologiesTerms( Collection<EvidenceLineInfo> evidenceLineInfos ) throws Exception {

        int line = 1;

        for ( EvidenceLineInfo lineInfo : evidenceLineInfos ) {

            line++;

            System.out.println( "Treating Ontology terms for line: " + line );

            // The DevelopmentStage column get converted
            for ( int i = 0; i < lineInfo.getDevelopmentStage().length; i++ ) {
                if ( !lineInfo.getDevelopmentStage()[i].equalsIgnoreCase( "" ) ) {
                    CharacteristicValueObject characteristic = new CharacteristicValueObject(
                            lineInfo.getDevelopmentStage()[i], EvidenceLineInfo.DEVELOPMENTAL_STAGE,
                            nifstd2Ontology( lineInfo.getDevelopmentStage()[i] ),
                            EvidenceLineInfo.DEVELOPMENTAL_STAGE_ONTOLOGY );
                    lineInfo.addExperimentCharacteristic( characteristic );
                }
            }

            // The BioSource column get converted ( no Ontology to convert )
            for ( int i = 0; i < lineInfo.getBioSource().length; i++ ) {
                if ( !lineInfo.getBioSource()[i].equalsIgnoreCase( "" ) ) {
                    CharacteristicValueObject characteristic = new CharacteristicValueObject(
                            lineInfo.getBioSource()[i], EvidenceLineInfo.BIOSOURCE, null,
                            EvidenceLineInfo.BIOSOURCE_ONTOLOGY );
                    lineInfo.addExperimentCharacteristic( characteristic );
                }
            }

            // The OrganismPart column get converted
            for ( int i = 0; i < lineInfo.getOrganismPart().length; i++ ) {
                if ( !lineInfo.getOrganismPart()[i].equalsIgnoreCase( "" ) ) {
                    CharacteristicValueObject characteristic = new CharacteristicValueObject(
                            lineInfo.getOrganismPart()[i], EvidenceLineInfo.ORGANISM_PART,
                            fma2Ontology( lineInfo.getOrganismPart()[i] ), EvidenceLineInfo.ORGANISM_PART_ONTOLOGY );
                    lineInfo.addExperimentCharacteristic( characteristic );
                }
            }

            // The ExperimentDesign column
            for ( int i = 0; i < lineInfo.getExperimentDesign().length; i++ ) {
                if ( !lineInfo.getExperimentDesign()[i].equalsIgnoreCase( "" ) ) {
                    CharacteristicValueObject characteristic = new CharacteristicValueObject(
                            lineInfo.getExperimentDesign()[i], EvidenceLineInfo.EXPERIMENT_DESIGN,
                            obi2Ontology( lineInfo.getExperimentDesign()[i] ),
                            EvidenceLineInfo.EXPERIMENT_DESIGN_ONTOLOGY );
                    lineInfo.addExperimentCharacteristic( characteristic );
                }
            }

            // The Treatment column get converted ( no Ontology to convert )
            for ( int i = 0; i < lineInfo.getTreatment().length; i++ ) {
                if ( !lineInfo.getTreatment()[i].equalsIgnoreCase( "" ) ) {
                    CharacteristicValueObject characteristic = new CharacteristicValueObject(
                            lineInfo.getTreatment()[i], EvidenceLineInfo.TREATMENT, null,
                            EvidenceLineInfo.TREATMENT_ONTOLOGY );
                    lineInfo.addPhenotype( characteristic );
                }
            }

            // The ExperimentOBI column get converted
            for ( int i = 0; i < lineInfo.getExperimentOBI().length; i++ ) {
                if ( !lineInfo.getExperimentOBI()[i].equalsIgnoreCase( "" ) ) {
                    CharacteristicValueObject characteristic = new CharacteristicValueObject(
                            lineInfo.getExperimentOBI()[i], EvidenceLineInfo.EXPERIMENT,
                            obi2Ontology( lineInfo.getExperimentOBI()[i] ), EvidenceLineInfo.EXPERIMENT_ONTOLOGY );
                    lineInfo.addExperimentCharacteristic( characteristic );
                }
            }

            // The phenotype column
            for ( int i = 0; i < lineInfo.getPhenotype().length; i++ ) {
                if ( !lineInfo.getPhenotype()[i].equalsIgnoreCase( "" ) ) {
                    CharacteristicValueObject phenotype = new CharacteristicValueObject( lineInfo.getPhenotype()[i],
                            EvidenceLineInfo.PHENOTYPE, phenotype2Ontology( lineInfo.getPhenotype()[i] ),
                            EvidenceLineInfo.PHENOTYPE_ONTOLOGY );
                    lineInfo.addPhenotype( phenotype );
                }
            }

        }
    }

    /**
     * used by Step 4 to make an intermediate file to check before the insert in the database, with terms replaced by
     * Ontology
     */
    private void writeFileWithOntology( Collection<EvidenceLineInfo> linesFromFile ) throws IOException {

        BufferedWriter out = new BufferedWriter( new FileWriter(
                "./gemma-core/src/main/java/ubic/gemma/association/phenotype/fileUpload/intermediateOutputFile.tsv" ) );

        out.write( "Gene ID" + "\t" + "Experimental Source (PMID)" + "\t" + "Review Source (PMID)" + "\t"
                + "EvidenceCode" + "\t" + "Comments" + "\t" + "Association Type" + "\t" + "Value" + "\t" + "ValueUri"
                + "\t" + "Category" + "\t" + "CategoryUri" + "\n" );

        for ( EvidenceLineInfo p : linesFromFile ) {

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
    private void verifyGeneIdExist( Collection<EvidenceLineInfo> linesFromFile ) throws Exception {

        for ( EvidenceLineInfo lineInfo : linesFromFile ) {
            if ( geneService.findByNCBIId( lineInfo.getGeneID() ) == null ) {
                System.err.println( "Gene not found in Gemma: " + lineInfo.getGeneID() + " Description: "
                        + lineInfo.getComment() );
            }
        }
    }

    /**
     * Step 6 populate the evidence and save it to the database calling the service
     * 
     * @throws Exception
     */
    private void createEvidencesInDatabase( Collection<EvidenceLineInfo> linesFromFile ) throws Exception {

        int evidenceNumber = 1;

        // for each evidence found, we need to populate its evidenceObject and to call the service to save it
        for ( EvidenceLineInfo phenoAss : linesFromFile ) {

            String description = phenoAss.getComment();
            CharacteristicValueObject associationType = null;

            if ( !phenoAss.getAssociationType().equalsIgnoreCase( "" ) ) {
                associationType = new CharacteristicValueObject( "Association Type", phenoAss.getAssociationType() );
            }
            String evidenceCode = phenoAss.getEvidenceCode();
            String primaryPublicationPubmed = phenoAss.getPrimaryReferencePubmed();
            String relevantPublicationPubmed = phenoAss.getReviewReferencePubmed();
            Collection<String> relevantPublicationsPubmed = null;

            if ( !relevantPublicationPubmed.equalsIgnoreCase( "" ) ) {
                relevantPublicationsPubmed = new HashSet<String>();
                relevantPublicationsPubmed.add( relevantPublicationPubmed );
            }

            Collection<CharacteristicValueObject> phenotypes = phenoAss.getPhenotypes();

            Collection<CharacteristicValueObject> characteristics = phenoAss.getExperimentCharacteristics();

            EvidenceValueObject evidence = new ExperimentalEvidenceValueObject( description, associationType, false,
                    evidenceCode, phenotypes, primaryPublicationPubmed, relevantPublicationsPubmed, characteristics );

            String geneId = phenoAss.getGeneID();

            try {
                // here should be gene id using 2 for test since I have a test database
                phenotypeAssociationService.linkGeneToPhenotype( geneId, evidence );
                System.out.println( "Evidence " + evidenceNumber + " created" );

            } catch ( Exception e ) {
                System.out.println( "Evidence " + evidenceNumber + " was NOT Created" );
                // throw new Exception( "Evidence " + evidenceNumber + " was NOT Created" );
            }
            evidenceNumber++;
        }

    }
}