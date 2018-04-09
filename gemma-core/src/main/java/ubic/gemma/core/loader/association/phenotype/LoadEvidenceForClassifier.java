/*
 * The gemma project
 *
 * Copyright (c) 2014 University of British Columbia
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
package ubic.gemma.core.loader.association.phenotype;

import ubic.gemma.core.annotation.reference.BibliographicReferenceService;
import ubic.gemma.core.apps.GemmaCLI.CommandGroup;
import ubic.gemma.core.util.AbstractCLI;
import ubic.gemma.core.util.AbstractCLIContextCLI;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.MedicalSubjectHeading;
import ubic.gemma.persistence.util.Settings;

import java.io.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Retrieve information used for classifying Phenocarta evidence quality.
 *
 * @author nicolas
 */
public class LoadEvidenceForClassifier extends AbstractCLIContextCLI {

    // a monthly dump of all evidence, takes too long to all, use files auto-generated
    private final String evidenceDumpPath =
            File.separator + "neurocarta" + File.separator + "classifier" + File.separator
                    + "AllPhenocartaAnnotations.tsv";
    // comes from the excluded set
    private final Set<String> excludedPubmed = new HashSet<>();
    // these are all evidence that were used in the training set, exclude those ones to not give false results
    private final String trainingSetPath =
            File.separator + "neurocarta" + File.separator + "classifier" + File.separator + "trainingSet.tsv";
    // the services that will be needed
    private BibliographicReferenceService bibliographicReferenceService = null;
    private BufferedWriter writer;

    @SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
    public LoadEvidenceForClassifier( String[] args ) throws Exception {

        this.loadServices( args );

        // place to put the files results
        String writeFolder = this.createWriteFolderIfNotExists();

        writer = new BufferedWriter( new FileWriter( writeFolder + "/mappingFound.tsv" ) );

        this.loadTrainingSetUsed();

        this.findEvidence();
    }

    public static void main( String[] args ) throws Exception {

        new LoadEvidenceForClassifier( args );

    }

    @Override
    public CommandGroup getCommandGroup() {
        return CommandGroup.ANALYSIS;
    }

    @Override
    public String getCommandName() {
        return "loadEvidenceForClassifier";
    }

    @Override
    protected void buildOptions() {
    }

    @Override
    protected Exception doWork( String[] args ) {
        return null;
    }

    // creates the folder where the output files will be put, use this one if file is too big
    private String createWriteFolderIfNotExists() throws Exception {

        // where to put the final results
        String writeFolder = Settings.getString( "gemma.appdata.home" ) + File.separator + "CLASSIFIER";

        File folder = new File( writeFolder );

        if ( !folder.mkdir() && !folder.exists() ) {
            throw new Exception( "having trouble to create a folder" );
        }

        return writeFolder;
    }

    // load all needed services
    private synchronized void loadServices( String[] args ) {

        // this gets the context, so we can access beans
        Exception exception = this.processCommandLine( args );
        if ( exception != null ) {
            AbstractCLI.log.error( exception );
            exception.printStackTrace();
        }

        // add services if needed later
        this.bibliographicReferenceService = this.getBean( BibliographicReferenceService.class );
    }

    private void findEvidence() throws IOException {

        BufferedReader br = new BufferedReader(
                new InputStreamReader( LoadEvidenceForClassifier.class.getResourceAsStream( evidenceDumpPath ) ) );

        // skip first line
        String firstLine = br.readLine();
        writer.write( firstLine + "\t" + "Mesh Terms" + "\n" );

        String line;

        while ( ( line = br.readLine() ) != null ) {

            if ( line.split( "\t" ).length > 7 ) {

                String pubmed = line.split( "\t" )[6];
                String taxon = line.split( "\t" )[3];

                if ( !excludedPubmed.contains( pubmed ) ) {

                    // must have 1 pubmed
                    if ( !pubmed.contains( ";" ) ) {

                        // cannot be human taxon, this can be changed
                        if ( !taxon.equalsIgnoreCase( "human" ) ) {

                            // find the MeshTerms
                            String meshTerms = this.findMeshTerms( pubmed );

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

        StringBuilder result = new StringBuilder();

        DatabaseEntry de = DatabaseEntry.Factory.newInstance();
        de.setAccession( pubmed );

        BibliographicReference bi = BibliographicReference.Factory.newInstance();
        bi.setPubAccession( de );

        System.out.println( pubmed );
        BibliographicReference b = this.bibliographicReferenceService.find( bi );

        for ( MedicalSubjectHeading m : b.getMeshTerms() ) {
            result.append( m.getTerm() ).append( ";" );
        }

        return result.toString();

    }

    private void loadTrainingSetUsed() throws IOException {

        BufferedReader br = new BufferedReader(
                new InputStreamReader( LoadEvidenceForClassifier.class.getResourceAsStream( trainingSetPath ) ) );

        String line;

        while ( ( line = br.readLine() ) != null ) {
            excludedPubmed.add( line.split( "\t" )[0] );
        }

    }

}
