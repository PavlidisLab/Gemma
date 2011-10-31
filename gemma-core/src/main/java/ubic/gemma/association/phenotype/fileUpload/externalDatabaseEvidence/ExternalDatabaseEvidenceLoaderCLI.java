package ubic.gemma.association.phenotype.fileUpload.externalDatabaseEvidence;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;

import ubic.basecode.ontology.model.OntologyTerm;
import ubic.basecode.ontology.providers.DiseaseOntologyService;
import ubic.basecode.ontology.providers.HumanPhenotypeOntologyService;
import ubic.basecode.ontology.providers.MammalianPhenotypeOntologyService;
import ubic.gemma.association.phenotype.PhenotypeAssociationManagerService;
import ubic.gemma.model.DatabaseEntryValueObject;
import ubic.gemma.model.ExternalDatabaseValueObject;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.gene.GeneService;
import ubic.gemma.model.genome.gene.phenotype.valueObject.CharacteristicValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.EvidenceValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.ExternalDatabaseEvidenceValueObject;
import ubic.gemma.ontology.OntologyService;
import ubic.gemma.util.AbstractSpringAwareCLI;

public class ExternalDatabaseEvidenceLoaderCLI extends AbstractSpringAwareCLI {

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

    private String allPhenotypesNotFound = "";

    public static void main( String[] args ) {

        // example of parameters
        args = new String[7];
        args[0] = "-u";
        args[1] = "administrator";
        args[2] = "-p";
        args[3] = "administrator";
        args[4] = "-f";
        args[5] = "./gemma-core/src/main/java/ubic/gemma/association/phenotype/fileUpload/externalDatabaseEvidence/Willie.tsv";
        args[6] = "-create";

        ExternalDatabaseEvidenceLoaderCLI p = new ExternalDatabaseEvidenceLoaderCLI();

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

    /** There are 6 Steps in the process of creating the evidence */
    @Override
    protected Exception doWork( String[] args ) {
        Exception err = processCommandLine( "PhenotypeAssociationLoader", args );
        if ( err != null ) return err;

        try {

            System.out.println( "STEP 1 : Load Ontology" );
            loadServices();

            System.out.println( "STEP 2 : Extract the data from the file" );
            Collection<DatabaseEvidenceLineInfo> linesFromFile = file2Objects( inputFile );

            System.out.println( "STEP 3 : Convert file to Ontology terms" );
            convertOntologiesTerms( linesFromFile );

            System.out.println( "All phenotypes not found: \n" + allPhenotypesNotFound );

            // check if all Gene ID can be found in Gemma
            System.out.println( "STEP 4 : Verify is all Gene ID exist in Gemma" );
            verifyGeneIdExist( linesFromFile );

            // called as the final step to create the object in the database
            if ( this.createInDatabase ) {
                System.out.println( "STEP 5 : Create evidence in the database" );
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
    }

    /** Take the file and transform it into an object structure for each line */
    private Collection<DatabaseEvidenceLineInfo> file2Objects( String inputFile ) throws IOException {

        Collection<DatabaseEvidenceLineInfo> DatabaseEvidenceLineInfos = new ArrayList<DatabaseEvidenceLineInfo>();

        BufferedReader br = new BufferedReader( new FileReader( inputFile ) );

        String line;
        int lineNumber = 0;

        // for each line of the file
        while ( ( line = br.readLine() ) != null ) {
            lineNumber++;

            if ( lineNumber != 1 ) {

                System.out.println( "Creating object for line: " + lineNumber );
                DatabaseEvidenceLineInfos.add( new DatabaseEvidenceLineInfo( line ) );
            }
        }

        br.close();

        return DatabaseEvidenceLineInfos;
    }

    /**
     * find the exact term of a search term in a Collection of Ontology terms
     * 
     * @param ontologyTerms Collection of ontologyTerms
     * @param search The value we are interested in finding
     * @return OntologyTerm the exact match value found
     */
    private OntologyTerm findExactTerm( Collection<OntologyTerm> ontologyTerms, String search ) {

        // list of OntelogyTerms found
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
    private String phenotype2Ontology( DatabaseEvidenceLineInfo lineInfo, int index ) throws Exception {

        String search = lineInfo.getPhenotype()[index];

        // search disease
        Collection<OntologyTerm> ontologyTerms = this.diseaseOntologyService.findTerm( search );

        OntologyTerm ot = findExactTerm( ontologyTerms, search );

        if ( ot != null ) {
            lineInfo.getPhenotype()[index] = ot.getLabel();
            return ot.getUri();
        }

        // search hp
        ontologyTerms = this.humanPhenotypeOntologyService.findTerm( search );

        ot = findExactTerm( ontologyTerms, search );

        if ( ot != null ) {
            lineInfo.getPhenotype()[index] = ot.getLabel();
            return ot.getUri();
        }

        // search mamalian
        ontologyTerms = this.mammalianPhenotypeOntologyService.findTerm( search );

        ot = findExactTerm( ontologyTerms, search );

        if ( ot != null ) {
            lineInfo.getPhenotype()[index] = ot.getLabel();
            return ot.getUri();
        }

        // all phenotypes must be find
        System.err.println( "phenotype not found in disease, hp and mp Ontology : " + search );
        allPhenotypesNotFound = allPhenotypesNotFound + search + "\n";
        return null;
    }

    /** change each line of the file by Ontology terms */
    private void convertOntologiesTerms( Collection<DatabaseEvidenceLineInfo> DatabaseEvidenceLineInfos )
            throws Exception {

        int line = 1;

        for ( DatabaseEvidenceLineInfo lineInfo : DatabaseEvidenceLineInfos ) {

            line++;

            System.out.println( "Treating Ontology terms for line: " + line );

            // The phenotype column
            for ( int i = 0; i < lineInfo.getPhenotype().length; i++ ) {
                if ( !lineInfo.getPhenotype()[i].equalsIgnoreCase( "" ) ) {

                    String valueURI = phenotype2Ontology( lineInfo, i );

                    CharacteristicValueObject phenotype = new CharacteristicValueObject( lineInfo.getPhenotype()[i],
                            DatabaseEvidenceLineInfo.PHENOTYPE, valueURI, DatabaseEvidenceLineInfo.PHENOTYPE_ONTOLOGY );
                    lineInfo.addPhenotype( phenotype );
                }
            }
        }
    }

    /**
     * Step 5 check that all gene exists in Gemma
     * 
     * @throws Exception
     */
    private void verifyGeneIdExist( Collection<DatabaseEvidenceLineInfo> linesFromFile ) throws Exception {

        int i = 0;

        for ( DatabaseEvidenceLineInfo lineInfo : linesFromFile ) {

            i++;

            Gene gene = this.geneService.findByNCBIId( Integer.parseInt( lineInfo.getGeneID() ) );

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
    private void createEvidenceInDatabase( Collection<DatabaseEvidenceLineInfo> linesFromFile ) throws Exception {

        int evidenceNumber = 1;

        // for each evidence found, we need to populate its evidenceObject and to call the service to save it
        for ( DatabaseEvidenceLineInfo phenoAss : linesFromFile ) {

            String description = phenoAss.getComment();
            CharacteristicValueObject associationType = null;

            String evidenceCode = phenoAss.getEvidenceCode();

            Set<CharacteristicValueObject> phenotypes = phenoAss.getPhenotypes();

            ExternalDatabaseValueObject externalDatabase = new ExternalDatabaseValueObject();
            externalDatabase.setName( phenoAss.getExternalDatabaseName() );

            DatabaseEntryValueObject databaseEntryValueObject = new DatabaseEntryValueObject();

            databaseEntryValueObject.setAccession( phenoAss.getDatabaseID() );
            databaseEntryValueObject.setExternalDatabase( externalDatabase );

            EvidenceValueObject evidence = new ExternalDatabaseEvidenceValueObject( description, associationType,
                    phenoAss.getIsEdivenceNegative(), evidenceCode, phenotypes, databaseEntryValueObject );

            String geneId = phenoAss.getGeneID();

            try {

                this.phenotypeAssociationService.create( geneId, evidence );
                System.out.println( "Evidence " + evidenceNumber + " created" );

            } catch ( Exception e ) {
                System.err.println( "Evidence " + evidenceNumber + " was NOT Created: " + e.getStackTrace().toString() );
                throw e;
            }
            evidenceNumber++;
        }

    }

}
