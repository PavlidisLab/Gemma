package ubic.gemma.loader.association.phenotype;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;

import ubic.gemma.annotation.reference.BibliographicReferenceService;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.MedicalSubjectHeading;
import ubic.gemma.util.AbstractCLIContextCLI;
import ubic.gemma.util.Settings;

public class LoadEvidenceForClassifier extends AbstractCLIContextCLI {

    // the services that will be needed
    BibliographicReferenceService bibliographicReferenceService = null;

    // a monthly dump of all evidence, takes too long to all, use files auto-generated
    private String evidenceDumpPath = File.separator + "neurocarta" + File.separator + "classifier" + File.separator
            + "AllPhenocartaAnnotations.tsv";
    // these are all evidence that were used in the training set, exclude those ones to not give false results
    private String trainingSetPath = File.separator + "neurocarta" + File.separator + "classifier" + File.separator
            + "trainingSet.tsv";

    // comes from the excluded set
    private HashSet<String> excludedPubmed = new HashSet<String>();

    private BufferedWriter writer = null;

    @SuppressWarnings("unused")
    public static void main( String[] args ) throws Exception {

        new LoadEvidenceForClassifier( args );

    }

    public LoadEvidenceForClassifier( String[] args ) throws Exception {

        loadServices( args );

        // place to put the files results
        String writeFolder = createWriteFolderIfDoesntExist( "CLASSIFIER" );

        writer = new BufferedWriter( new FileWriter( writeFolder + "/mappingFound.tsv" ) );

        loadTrainingSetUsed();

        findEvidence();
    }

    private void findEvidence() throws FileNotFoundException, IOException {

        BufferedReader br = new BufferedReader( new InputStreamReader(
                LoadEvidenceForClassifier.class.getResourceAsStream( evidenceDumpPath ) ) );

        // skip first line
        String firstLine = br.readLine();
        writer.write( firstLine + "\t" + "Mesh Terms" + "\n" );

        String line = "";

        while ( ( line = br.readLine() ) != null ) {

            if ( line.split( "\t" ).length > 7 ) {

                String pubmed = line.split( "\t" )[6];
                String taxon = line.split( "\t" )[3];

                if ( !excludedPubmed.contains( pubmed ) ) {

                    // must have 1 pubmed
                    if ( pubmed.indexOf( ";" ) == -1 ) {

                        // cannot be human taxon, this can be changed
                        if ( !taxon.equalsIgnoreCase( "human" ) ) {

                            // find the MeshTerms
                            String meshTerms = findMeshTerms( pubmed );

                            // must have mesh terms associtaed with it
                            if ( !meshTerms.isEmpty() ) {
                                writer.write( line + "\t" + meshTerms + "\n" );
                            }

                            // dont treat 2 times the same pubmed
                            excludedPubmed.add( pubmed );
                        }
                    }
                }
            }
        }
    }

    // return mesh term of a pubmed format : meshTerm1;meshTerm2;meshTerms3.... etc
    private String findMeshTerms( String pubmed ) {

        String result = "";

        DatabaseEntry de = DatabaseEntry.Factory.newInstance();
        de.setAccession( pubmed );

        BibliographicReference bi = BibliographicReference.Factory.newInstance();
        bi.setPubAccession( de );

        System.out.println( pubmed );
        BibliographicReference b = this.bibliographicReferenceService.find( bi );

        for ( MedicalSubjectHeading m : b.getMeshTerms() ) {

            result = result + m.getTerm() + ";";
        }

        return result;

    }

    private void loadTrainingSetUsed() throws FileNotFoundException, IOException {

        BufferedReader br = new BufferedReader( new InputStreamReader(
                LoadEvidenceForClassifier.class.getResourceAsStream( trainingSetPath ) ) );

        String line = "";

        while ( ( line = br.readLine() ) != null ) {
            excludedPubmed.add( line.split( "\t" )[0] );
        }

    }

    // load all needed services
    protected synchronized void loadServices( String[] args ) throws Exception {

        // this gets the context, so we can access beans
        processCommandLine( "LoadEvidenceForClassifier", args );

        // add services if needed later
        this.bibliographicReferenceService = this.getBean( BibliographicReferenceService.class );

    }

    @Override
    protected void buildOptions() {
        // TODO Auto-generated method stub

    }

    @Override
    protected Exception doWork( String[] args ) {
        // TODO Auto-generated method stub
        return null;
    }

    // creates the folder where the output files will be put, use this one if file is too big
    protected String createWriteFolderIfDoesntExist( String name ) throws Exception {

        // where to put the final results
        String writeFolder = Settings.getString( "gemma.appdata.home" ) + File.separator + name;

        File folder = new File( writeFolder );
        folder.mkdir();

        if ( !folder.exists() ) {
            throw new Exception( "having trouble to create a folder" );
        }

        return writeFolder;
    }

}
