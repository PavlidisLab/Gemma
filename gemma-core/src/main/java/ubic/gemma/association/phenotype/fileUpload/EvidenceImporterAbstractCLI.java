package ubic.gemma.association.phenotype.fileUpload;

import java.io.BufferedReader;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;

import ubic.basecode.ontology.providers.DiseaseOntologyService;
import ubic.basecode.ontology.providers.FMAOntologyService;
import ubic.basecode.ontology.providers.HumanPhenotypeOntologyService;
import ubic.basecode.ontology.providers.MammalianPhenotypeOntologyService;
import ubic.basecode.ontology.providers.NIFSTDOntologyService;
import ubic.basecode.ontology.providers.ObiService;
import ubic.gemma.association.phenotype.PhenotypeAssociationManagerService;
import ubic.gemma.genome.gene.service.GeneService;
import ubic.gemma.model.ExternalDatabaseValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.EvidenceSourceValueObject;
import ubic.gemma.ontology.OntologyService;
import ubic.gemma.util.AbstractSpringAwareCLI;

public abstract class EvidenceImporterAbstractCLI extends AbstractSpringAwareCLI {

    protected PhenotypeAssociationManagerService phenotypeAssociationService = null;
    protected GeneService geneService = null;

    protected DiseaseOntologyService diseaseOntologyService = null;
    protected MammalianPhenotypeOntologyService mammalianPhenotypeOntologyService = null;
    protected HumanPhenotypeOntologyService humanPhenotypeOntologyService = null;
    protected OntologyService ontologyService = null;
    protected NIFSTDOntologyService nifstdOntologyService = null;
    protected ObiService obiService = null;
    protected FMAOntologyService fmaOntologyService = null;

    // input file path
    protected String inputFile = "";
    protected BufferedReader br = null;
    protected boolean createInDatabase = false;
    protected boolean prodDatabase = false;

    protected final String EXPERIMENTAL_EVIDENCE = "EXPERIMENTAL";
    protected final String LITERATURE_EVIDENCE = "LITERATURE";
    protected final String DEVELOPMENTAL_STAGE = "DevelopmentalStage";
    protected final String BIOSOURCE = "BioSource";
    protected final String ORGANISM_PART = "OrganismPart";
    protected final String EXPERIMENT_DESIGN = "ExperimentDesign";
    protected final String TREATMENT = "Treatment";
    protected final String EXPERIMENT = "Experiment";

    protected final String DEVELOPMENTAL_STAGE_ONTOLOGY = "http://mged.sourceforge.net/ontologies/MGEDOntology.owl#DevelopmentalStage";
    protected final String BIOSOURCE_ONTOLOGY = "http://mged.sourceforge.net/ontologies/MGEDOntology.owl#BioSource";
    protected final String ORGANISM_PART_ONTOLOGY = "http://mged.sourceforge.net/ontologies/MGEDOntology.owl#OrganismPart";
    protected final String EXPERIMENT_DESIGN_ONTOLOGY = "http://mged.sourceforge.net/ontologies/MGEDOntology.owl#ExperimentDesign";
    protected final String TREATMENT_ONTOLOGY = "http://mged.sourceforge.net/ontologies/MGEDOntology.owl#Treatment";
    protected final String EXPERIMENT_ONTOLOGY = "http://mged.sourceforge.net/ontologies/MGEDOntology.owl#Experiment";

    @Override
    protected void buildOptions() {
        @SuppressWarnings("static-access")
        Option fileOption = OptionBuilder.withDescription( "The file" ).hasArg().withArgName( "file path" )
                .isRequired().create( "f" );
        addOption( fileOption );
        @SuppressWarnings("static-access")
        Option createOption = OptionBuilder.withDescription( "Create in Database (false or true)" ).hasArg()
                .withArgName( "create in Database" ).isRequired().create( "c" );
        addOption( createOption );
        @SuppressWarnings("static-access")
        Option prodOption = OptionBuilder.withDescription( "Using production database (false or true)" ).hasArg()
                .withArgName( "indicate if Prod database" ).isRequired().create( "e" );
        addOption( prodOption );
    }

    @Override
    protected void processOptions() {
        super.processOptions();
        this.inputFile = getOptionValue( 'f' );
        this.createInDatabase = new Boolean( getOptionValue( 'c' ) );
        this.prodDatabase = new Boolean( getOptionValue( 'e' ) );
    }

    protected EvidenceSourceValueObject makeEvidenceSource( String databaseID, String externalDatabaseName ) {

        EvidenceSourceValueObject evidenceSourceValueObject = null;
        if ( !databaseID.isEmpty() && !externalDatabaseName.isEmpty() ) {
            ExternalDatabaseValueObject externalDatabase = new ExternalDatabaseValueObject();
            externalDatabase.setName( externalDatabaseName );
            evidenceSourceValueObject = new EvidenceSourceValueObject( databaseID, externalDatabase );
        }

        return evidenceSourceValueObject;
    }

    protected synchronized void loadServices( boolean experimentalEvidenceServicesNeeded ) throws Exception {

        this.phenotypeAssociationService = this.getBean( PhenotypeAssociationManagerService.class );

        this.geneService = this.getBean( GeneService.class );

        this.ontologyService = this.getBean( OntologyService.class );

        this.diseaseOntologyService = this.ontologyService.getDiseaseOntologyService();
        this.mammalianPhenotypeOntologyService = this.ontologyService.getMammalianPhenotypeOntologyService();
        this.humanPhenotypeOntologyService = this.ontologyService.getHumanPhenotypeOntologyService();

        while ( this.diseaseOntologyService.isOntologyLoaded() == false ) {
            wait( 3000 );
            System.out.println( "waiting for the Disease Ontology to load" );
        }

        while ( this.mammalianPhenotypeOntologyService.isOntologyLoaded() == false ) {
            wait( 3000 );
            System.out.println( "waiting for the MP Ontology to load" );
        }

        while ( this.humanPhenotypeOntologyService.isOntologyLoaded() == false ) {
            wait( 3000 );
            System.out.println( "waiting for the HP Ontology to load" );
        }

        // only need those services for experimental evidences
        if ( experimentalEvidenceServicesNeeded ) {

            this.nifstdOntologyService = this.ontologyService.getNifstfOntologyService();
            this.obiService = this.ontologyService.getObiService();
            this.fmaOntologyService = this.ontologyService.getFmaOntologyService();

            while ( this.obiService.isOntologyLoaded() == false ) {
                wait( 3000 );
                System.out.println( "waiting for the OBI Ontology to load" );
            }

            while ( this.nifstdOntologyService.isOntologyLoaded() == false ) {
                wait( 3000 );
                System.out.println( "waiting for the NIF Ontology to load" );
            }

            while ( this.fmaOntologyService.isOntologyLoaded() == false ) {
                wait( 3000 );
                System.out.println( "waiting for the FMA Ontology to load" );
            }
        }
    }

    protected Set<String> trimArray( String[] array ) {

        Set<String> mySet = new HashSet<String>();

        String[] trimmedArray = new String[array.length];

        for ( int i = 0; i < trimmedArray.length; i++ ) {
            String value = array[i].trim();

            if ( !value.equals( "" ) ) {
                mySet.add( value );
            }
        }

        return mySet;
    }

    protected String findTypeOfEvidence() throws Exception {

        // lets check what type of evidence when have on the sheet
        // 16 columns == EXPERIMENTAL
        // 9 columns == LITERATURE
        // other values == ERROR
        int numOfColums = this.br.readLine().split( "\t" ).length;

        if ( numOfColums == 9 ) {
            System.out.println( "The type of Evidence found is: " + this.LITERATURE_EVIDENCE );
            loadServices( false );
            return this.LITERATURE_EVIDENCE;

        } else if ( numOfColums >= 16 ) {
            System.out.println( "The type of Evidence found is: " + this.EXPERIMENTAL_EVIDENCE );
            loadServices( true );
            return this.EXPERIMENTAL_EVIDENCE;

        } else {
            throw new Exception( "Cannot determine the type of evidence found: " + numOfColums + " columns" );
        }

    }

}
