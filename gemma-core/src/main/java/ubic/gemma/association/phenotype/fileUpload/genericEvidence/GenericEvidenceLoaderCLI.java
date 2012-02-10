package ubic.gemma.association.phenotype.fileUpload.genericEvidence;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import ubic.gemma.association.phenotype.fileUpload.EvidenceLoaderCLI;
import ubic.gemma.genome.taxon.service.TaxonService;
import ubic.gemma.model.DatabaseEntryValueObject;
import ubic.gemma.model.ExternalDatabaseValueObject;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.gene.phenotype.valueObject.CharacteristicValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.EvidenceSourceValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.EvidenceValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.GenericEvidenceValueObject;

public class GenericEvidenceLoaderCLI extends EvidenceLoaderCLI {

    private TaxonService taxonService = null;

    boolean findGeneId = false;

    public static void main( String[] args ) {

        GenericEvidenceLoaderCLI p = new GenericEvidenceLoaderCLI();

        try {
            // to pass args by the command line dont use the initArguments method
            // Exception ex = p.doWork( args );

            // args was hardcoded to make it more easy
            Exception ex = p.doWork( initArguments() );

            if ( ex != null ) {
                ex.printStackTrace();
            }
        } catch ( Exception e ) {
            throw new RuntimeException( e );
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
            Collection<GenericEvidenceLineInfo> linesFromFile = file2Objects();

            // Optional
            if ( this.findGeneId ) {
                findGenesId( linesFromFile );
            }

            System.out.println( "STEP 3 : Convert file to Ontology terms" );
            convertOntologiesTerms( linesFromFile );

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

    /** Take the file and transform it into an object structure for each line */
    private Collection<GenericEvidenceLineInfo> file2Objects() throws IOException {

        Collection<GenericEvidenceLineInfo> genericLineInfos = new ArrayList<GenericEvidenceLineInfo>();

        BufferedReader br = new BufferedReader( new FileReader( this.inputFile ) );

        String line;
        int lineNumber = 0;

        // for each line of the file
        while ( ( line = br.readLine() ) != null ) {
            lineNumber++;

            if ( lineNumber != 1 ) {

                System.out.println( "Creating object for line: " + lineNumber );
                genericLineInfos.add( new GenericEvidenceLineInfo( line ) );
            }
        }

        br.close();

        return genericLineInfos;
    }

    /**
     * Step 6 populate the evidence and save it to the database calling the service
     * 
     * @throws Exception
     */
    private void createEvidenceInDatabase( Collection<GenericEvidenceLineInfo> linesFromFile ) throws Exception {

        int evidenceNumber = 1;

        // for each evidence found, we need to populate its evidenceObject and to call the service to save it
        for ( GenericEvidenceLineInfo phenoAss : linesFromFile ) {

            String description = phenoAss.getComment();
            CharacteristicValueObject associationType = null;

            String evidenceCode = phenoAss.getEvidenceCode();

            Set<CharacteristicValueObject> phenotypes = phenoAss.getPhenotypes();

            ExternalDatabaseValueObject externalDatabase = new ExternalDatabaseValueObject();
            externalDatabase.setName( phenoAss.getExternalDatabaseName() );

            DatabaseEntryValueObject databaseEntryValueObject = new DatabaseEntryValueObject();

            databaseEntryValueObject.setAccession( phenoAss.getDatabaseID() );
            databaseEntryValueObject.setExternalDatabase( externalDatabase );

            EvidenceSourceValueObject evidenceSource = new EvidenceSourceValueObject( phenoAss.getDatabaseID(),
                    externalDatabase );

            EvidenceValueObject evidence = new GenericEvidenceValueObject( description, associationType, new Boolean(
                    phenoAss.getIsEdivenceNegative() ), evidenceCode, phenotypes, evidenceSource, new Integer(
                    phenoAss.getGeneID() ) );

            try {

                this.phenotypeAssociationService.create( evidence );
                System.out.println( "Evidence " + evidenceNumber + " created" );

            } catch ( Exception e ) {
                System.err.println( "Evidence " + evidenceNumber + " was NOT Created: " + e.getStackTrace().toString() );
                throw e;
            }
            evidenceNumber++;
        }

    }

    // Optional can be useful when we dont have the NCBI id
    private void findGenesId( Collection<GenericEvidenceLineInfo> linesFromFile ) {
        int y = 1;

        for ( GenericEvidenceLineInfo g : linesFromFile ) {

            y++;
            Gene gene = null;

            if ( g.getGeneName().equals( "HLA-DRB1" ) ) {

                g.setGeneID( "3123" );

            } else if ( g.getGeneName().equals( "CCR2" ) ) {

                g.setGeneID( "729230" );

            } else if ( g.getGeneName().equals( "NPC1" ) ) {

                g.setGeneID( "4864" );
            }

            else if ( g.getGeneName().equals( "PRG4" ) ) {

                g.setGeneID( "10216" );

            } else {

                gene = this.geneService.findByOfficialSymbol( g.getGeneName(),
                        this.taxonService.findByCommonName( "human" ) );

                if ( gene == null ) {

                    System.err.println( y );
                    System.err.println( g.getGeneName() );
                    g.setGeneID( "1" );
                    // System.exit( -1 );

                } else if ( gene.getNcbiGeneId() == null ) {
                    System.err.println( "not Supposed to go there" );
                    System.err.println( y );
                    System.err.println( g.getGeneName() );
                    g.setGeneID( "1" );
                } else {
                    g.setGeneID( gene.getNcbiGeneId().toString() );
                }
            }
        }
    }
}
