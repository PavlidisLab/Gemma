package ubic.gemma.association.phenotype.fileUpload.literatureEvidence;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.SortedSet;

import ubic.gemma.association.phenotype.fileUpload.EvidenceLoaderCLI;
import ubic.gemma.model.DatabaseEntryValueObject;
import ubic.gemma.model.ExternalDatabaseValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.CharacteristicValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.EvidenceSourceValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.EvidenceValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.LiteratureEvidenceValueObject;

public class LiteratureEvidenceLoaderCLI extends EvidenceLoaderCLI {

    public static void main( String[] args ) {

        LiteratureEvidenceLoaderCLI p = new LiteratureEvidenceLoaderCLI();

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
            Collection<LitEvidenceLineInfo> linesFromFile = file2Objects();

            System.out.println( "STEP 3 : Convert file to Ontology terms" );
            convertOntologiesTerms( linesFromFile );

            if ( this.testEnvironment ) {
                System.err.println( this.testMessage );
            }

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
    private Collection<LitEvidenceLineInfo> file2Objects() throws IOException {

        Collection<LitEvidenceLineInfo> LitEvidenceLineInfos = new ArrayList<LitEvidenceLineInfo>();

        BufferedReader br = new BufferedReader( new FileReader( this.inputFile ) );

        String line;
        int lineNumber = 0;

        // for each line of the file
        while ( ( line = br.readLine() ) != null ) {
            lineNumber++;

            if ( lineNumber != 1 ) {

                System.out.println( "Creating object for line: " + lineNumber );
                LitEvidenceLineInfos.add( new LitEvidenceLineInfo( line ) );
            }
        }

        br.close();

        return LitEvidenceLineInfos;
    }

    /**
     * populate the evidence and save it to the database calling the service
     * 
     * @throws Exception
     */
    private void createEvidenceInDatabase( Collection<LitEvidenceLineInfo> linesFromFile ) throws Exception {

        int evidenceNumber = 1;

        // for each evidence found, we need to populate its evidenceObject and to call the service to save it
        for ( LitEvidenceLineInfo phenoAss : linesFromFile ) {

            String description = phenoAss.getComment();

            if ( !phenoAss.getAssociationType().equalsIgnoreCase( "" ) ) {
                // associationType = new CharacteristicValueObject( "Association Type", phenoAss.getAssociationType() );
            }
            String evidenceCode = phenoAss.getEvidenceCode();
            String primaryPublicationPubmed = phenoAss.getPrimaryReferencePubmed();

            SortedSet<CharacteristicValueObject> phenotypes = phenoAss.getPhenotypes();

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

            EvidenceValueObject evidence = new LiteratureEvidenceValueObject( new Integer( phenoAss.getGeneID() ),
                    phenotypes, description, evidenceCode, phenoAss.isEdivenceNegative(), evidenceSource,
                    primaryPublicationPubmed );

            try {

                this.phenotypeAssociationService.create( evidence );
                System.out.println( "Evidence " + evidenceNumber + " created" );

            } catch ( Exception e ) {
                System.err.println( "Evidence " + evidenceNumber + " was NOT Created: " + e.getStackTrace() );
                throw e;
            }
            evidenceNumber++;
        }
    }
}
