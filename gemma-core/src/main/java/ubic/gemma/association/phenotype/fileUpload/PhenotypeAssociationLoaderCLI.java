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
import ubic.gemma.model.genome.gene.phenotype.valueObject.CharacteristicValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.EvidenceValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.ExperimentalEvidenceValueObject;
import ubic.gemma.ontology.OntologyService;
import ubic.gemma.util.AbstractSpringAwareCLI;

/** take a tsv file for the CGMS and creates experimental evidences */
public class PhenotypeAssociationLoaderCLI extends AbstractSpringAwareCLI {

    private String inputFile = "";
    private Boolean createInDatabase = false;

    private OntologyService ontologyService = null;

    // Ontology services used
    private DiseaseOntologyService diseaseOntologyService = null;
    private MammalianPhenotypeOntologyService mammalianPhenotypeOntologyService = null;
    private HumanPhenotypeOntologyService humanPhenotypeOntologyService = null;
    private NIFSTDOntologyService nifstdOntologyService = null;
    private PhenotypeAssociationManagerService phenotypeAssociationService = null;
    private ObiService obiService = null;
    private FMAOntologyService fmaOntologyService = null;

    public static void main( String[] args ) {

        args = new String[7];
        args[0] = "-u";
        args[1] = "administrator";
        args[2] = "-p";
        args[3] = "administrator";
        args[4] = "-f";
        args[5] = "./gemma-core/src/main/java/ubic/gemma/association/phenotype/fileUpload/ArtemisInputFile.tsv";
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

    /** There is 5 Steps in the process of creating the evidences */
    @Override
    protected Exception doWork( String[] args ) {
        Exception err = processCommandLine( "PhenotypeAssociationLoader", args );
        if ( err != null ) return err;

        try {

            System.out.println( "STEP 1 : Load Ontology" );
            loadOntologyServices();

            System.out.println( "STEP 2 : Extract the data from the file" );
            Collection<PhenoAssoLineInfo> linesFromFile = file2Objects( inputFile );

            System.out.println( "STEP 3 : Convert file to Ontology terms" );
            convertOntologiesTerms( linesFromFile );

            // make a tsv file with terms replaced by Ontology values, for checking purpose
            System.out.println( "STEP 4 : Create intermediate file with uri from ontology" );
            writeFileWithOntology( linesFromFile );

            // called when we are sure about step 4, option is put on the command line
            if ( createInDatabase ) {
                System.out.println( "STEP 5 : Create the evidences in the database" );
                createEvidencesInDatabase( linesFromFile );
                System.out.println( "Evidences inserted in the database" );
            }

        } catch ( Exception e ) {
            return e;
        }

        return null;
    }

    /** check that all Ontology are loaded */
    public synchronized void loadOntologyServices() throws Exception {

        int time = 0;

        ontologyService = ( OntologyService ) this.getBean( "ontologyService" );

        diseaseOntologyService = ontologyService.getDiseaseOntologyService();
        mammalianPhenotypeOntologyService = ontologyService.getMammalianPhenotypeOntologyService();
        humanPhenotypeOntologyService = ontologyService.getHumanPhenotypeOntologyService();
        obiService = ontologyService.getObiService();
        nifstdOntologyService = ontologyService.getNifstfOntologyService();
        fmaOntologyService = ontologyService.getFmaOntologyService();
        /*
         * while ( diseaseOntologyService.isOntologyLoaded() == false ) { wait( 1000 ); time++; if ( time > 600 ) {
         * throw new Exception( "Taking more than 10 minutes" ); } System.out.println(
         * "waiting for Disease Ontology to load" ); }
         * 
         * while ( mammalianPhenotypeOntologyService.isOntologyLoaded() == false ) { wait( 1000 ); time++; if ( time >
         * 600 ) { throw new Exception( "Taking more than 10 minutes" ); } System.out.println(
         * "waiting for MP Ontology to load" ); }
         * 
         * while ( humanPhenotypeOntologyService.isOntologyLoaded() == false ) { wait( 1000 ); time++; if ( time > 600 )
         * { throw new Exception( "Taking more than 10 minutes" ); } System.out.println(
         * "waiting for HP Ontology to load" ); }
         * 
         * while ( obiService.isOntologyLoaded() == false ) { wait( 1000 ); time++; if ( time > 600 ) { throw new
         * Exception( "Taking more than 10 minutes" ); } System.out.println( "waiting for OBI Ontology to load" ); }
         */

        while ( nifstdOntologyService.isOntologyLoaded() == false ) {
            wait( 1000 );
            time++;
            if ( time > 600 ) {
                throw new Exception( "Taking more than 10 minutes" );
            }
            System.out.println( "waiting for NIF Ontology to load" );
        }

        // WONT LOAD TOO BIG...
        /*
         * while ( ontologyService.getFmaOntologyService().isOntologyLoaded() == false ) { wait( 1000 ); time++; if (
         * time > 600 ) { throw new Exception( "Taking more than 10 minutes" ); } System.out.println(
         * "waiting for FMA Ontology to load" ); }
         */
    }

    /** Take the file and transform it into an object structure for each line */
    private Collection<PhenoAssoLineInfo> file2Objects( String inputFile ) throws IOException {

        Collection<PhenoAssoLineInfo> phenoAssoFileInfo = new ArrayList<PhenoAssoLineInfo>();

        BufferedReader br = new BufferedReader( new InputStreamReader( new DataInputStream( new FileInputStream(
                inputFile ) ) ) );

        String line;
        int lineNumber = 0;

        // for each line of the file
        while ( ( line = br.readLine() ) != null ) {
            lineNumber++;

            if ( lineNumber != 1 ) {

                System.out.println( "Creating object for line: " + lineNumber );

                phenoAssoFileInfo.add( new PhenoAssoLineInfo( line ) );
            }
        }

        br.close();

        return phenoAssoFileInfo;
    }

    /**
     * find the exact term of a search term in a Collection of Ontology terms
     * 
     * @param ontologyTerms Collection of ontologyTerms
     * @param search The value we are intereted in finding
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

    /** search term first in the diseaseOntology then hp, then mp */
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

        System.out.println( "term not found in disease, hp and mp Ontology : " + search );
        return search;
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
            return search;
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
            return search;
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
            return search;
        }
    }

    /** changing each line of the file by Ontology terms */
    private void convertOntologiesTerms( Collection<PhenoAssoLineInfo> linesInfo ) throws Exception {

        int line = 1;

        for ( PhenoAssoLineInfo lineInfo : linesInfo ) {

            line++;

            System.out.println( "Treating Ontology terms for line: " + line );

            // The phenotype column get converted
            for ( int i = 0; i < lineInfo.getPhenotype().length; i++ ) {

                if ( !lineInfo.getPhenotype()[i].equalsIgnoreCase( "" ) ) {

                    lineInfo.getPhenotype()[i] = phenotype2Ontology( lineInfo.getPhenotype()[i] );
                }
            }

            // The ExperimentDesign column get converted
            for ( int i = 0; i < lineInfo.getExperimentDesign().length; i++ ) {

                if ( !lineInfo.getExperimentDesign()[i].equalsIgnoreCase( "" ) ) {

                    lineInfo.getExperimentDesign()[i] = obi2Ontology( lineInfo.getExperimentDesign()[i] );
                }
            }

            // The ExperimentOBI column get converted
            for ( int i = 0; i < lineInfo.getExperimentOBI().length; i++ ) {
                if ( !lineInfo.getExperimentOBI()[i].equalsIgnoreCase( "" ) ) {

                    lineInfo.getExperimentOBI()[i] = obi2Ontology( lineInfo.getExperimentOBI()[i] );
                }
            }

            // The DevelopmentStage column get converted
            for ( int i = 0; i < lineInfo.getDevelopmentStage().length; i++ ) {
                if ( !lineInfo.getDevelopmentStage()[i].equalsIgnoreCase( "" ) ) {

                    lineInfo.getDevelopmentStage()[i] = nifstd2Ontology( lineInfo.getDevelopmentStage()[i] );
                }
            }

            // The OrganismPart column get converted
            for ( int i = 0; i < lineInfo.getOrganismPart().length; i++ ) {
                if ( !lineInfo.getOrganismPart()[i].equalsIgnoreCase( "" ) ) {

                    lineInfo.getOrganismPart()[i] = fma2Ontology( lineInfo.getOrganismPart()[i] );
                }
            }

        }
    }

    /**
     * used by Step 4 to make an intermediate file to check before the insert in the database, with terms replaced by
     * Ontology
     */
    private void writeFileWithOntology( Collection<PhenoAssoLineInfo> linesFromFile ) throws IOException {

        BufferedWriter out = new BufferedWriter( new FileWriter(
                "./gemma-core/src/main/java/ubic/gemma/association/phenotype/fileUpload/ArtemisOutputFile.tsv" ) );

        out.write( "Gene ID" + "\t" + "Experimental Source (PMID)" + "\t" + "Review Source (PMID)" + "\t"
                + "EvidenceCode" + "\t" + "Comments" + "\t" + "Association Type" + "\t"
                + PhenoAssoLineInfo.DEVELOPMENTAL_STAGE_ONTOLOGY + "\t" + PhenoAssoLineInfo.BIOSOURCE_ONTOLOGY + "\t"
                + PhenoAssoLineInfo.ORGANISM_PART_ONTOLOGY + "\t" + PhenoAssoLineInfo.EXPERIMENT_DESIGN_ONTOLOGY + "\t"
                + PhenoAssoLineInfo.TREATMENT_ONTOLOGY + "\t" + PhenoAssoLineInfo.EXPERIMENT_ONTOLOGY + "\t"
                + PhenoAssoLineInfo.PHENOTYPE_ONTOLOGY + "\n" );

        for ( PhenoAssoLineInfo p : linesFromFile ) {
            out.write( p.getGeneID() + "\t" + p.getPrimaryReferencePubmed() + "\t" + p.getReviewReferencePubmed()
                    + "\t" + p.getEvidenceCode() + "\t" + p.getComment() + "\t" + p.getAssociationType() + "\t"
                    + array2String( p.getDevelopmentStage() ) + "\t" + array2String( p.getBioSource() ) + "\t"
                    + array2String( p.getOrganismPart() ) + "\t" + array2String( p.getExperimentDesign() ) + "\t"
                    + array2String( p.getTreatment() ) + "\t" + array2String( p.getExperimentOBI() ) + "\t"
                    + array2String( p.getPhenotype() ) + "\n" );
        }

        out.close();
    }

    /** Step 5 populate the evidence and save it to the database calling the service */
    private void createEvidencesInDatabase( Collection<PhenoAssoLineInfo> linesFromFile ) throws IOException {

        int evidenceNumber = 1;

        // for each evidence found, we need to populate its evidenceObject and to call the service to save it
        for ( PhenoAssoLineInfo phenoAss : linesFromFile ) {

            String description = phenoAss.getComment() + phenoAss.getReviewReferencePubmed();
            // dont know what category to use for now lets not use it
            CharacteristicValueObject associationType = null;
            String evidenceCode = phenoAss.getEvidenceCode();

            Collection<CharacteristicValueObject> phenotypes = new HashSet<CharacteristicValueObject>();

            for ( String phenotype : phenoAss.getPhenotype() ) {
                CharacteristicValueObject characteristicValueObject = new CharacteristicValueObject( phenotype,
                        PhenoAssoLineInfo.PHENOTYPE_ONTOLOGY );
                phenotypes.add( characteristicValueObject );
            }
            String primaryPublicationPubmed = phenoAss.getPrimaryReferencePubmed();

            Collection<CharacteristicValueObject> characteristics = new HashSet<CharacteristicValueObject>();

            for ( String developmentStage : phenoAss.getDevelopmentStage() ) {
                CharacteristicValueObject c = new CharacteristicValueObject( developmentStage,
                        PhenoAssoLineInfo.DEVELOPMENTAL_STAGE_ONTOLOGY );
                characteristics.add( c );
            }
            for ( String bioSource : phenoAss.getBioSource() ) {
                CharacteristicValueObject c = new CharacteristicValueObject( bioSource,
                        PhenoAssoLineInfo.BIOSOURCE_ONTOLOGY );
                characteristics.add( c );
            }
            for ( String organismPart : phenoAss.getOrganismPart() ) {
                CharacteristicValueObject c = new CharacteristicValueObject( organismPart,
                        PhenoAssoLineInfo.ORGANISM_PART_ONTOLOGY );
                characteristics.add( c );
            }
            for ( String experimentDesign : phenoAss.getExperimentDesign() ) {
                CharacteristicValueObject c = new CharacteristicValueObject( experimentDesign,
                        PhenoAssoLineInfo.EXPERIMENT_DESIGN_ONTOLOGY );
                characteristics.add( c );
            }
            for ( String treatment : phenoAss.getTreatment() ) {
                CharacteristicValueObject c = new CharacteristicValueObject( treatment,
                        PhenoAssoLineInfo.TREATMENT_ONTOLOGY );
                characteristics.add( c );
            }
            for ( String experimentOBI : phenoAss.getExperimentOBI() ) {
                CharacteristicValueObject c = new CharacteristicValueObject( experimentOBI,
                        PhenoAssoLineInfo.EXPERIMENT_ONTOLOGY );
                characteristics.add( c );
            }

            EvidenceValueObject evidence = new ExperimentalEvidenceValueObject( "", description, associationType,
                    false, evidenceCode, phenotypes, primaryPublicationPubmed, null, characteristics );

            phenotypeAssociationService = ( PhenotypeAssociationManagerService ) this
                    .getBean( "phenotypeAssociationManagerService" );

            phenotypeAssociationService.linkGeneToPhenotype( "2", evidence );

            System.out.println( "Evidence " + evidenceNumber + " created" );
            evidenceNumber++;
        }

    }

    /** take an array of String and makes one String, values separated by ; */
    private String array2String( String[] array ) {

        String result = "";

        for ( int i = 0; i < array.length; i++ ) {
            if ( i == array.length - 1 ) {
                result = result + array[i];
            } else {
                result = result + array[i] + ";";
            }
        }
        return result;
    }

}
