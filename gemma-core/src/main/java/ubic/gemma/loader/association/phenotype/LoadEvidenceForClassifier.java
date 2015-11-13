/*
 * The gemma-core project
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
package ubic.gemma.loader.association.phenotype;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

import ubic.gemma.annotation.reference.BibliographicReferenceService;
import ubic.gemma.apps.GemmaCLI.CommandGroup;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.MedicalSubjectHeading;
import ubic.gemma.util.AbstractCLIContextCLI;
import ubic.gemma.util.Settings;

/**
 * Retrieve information used for classifying Phenocarta evidence quality. FIXME this doesn't do anything right now.
 * 
 * @author nicolas
 * @version $Id$
 */
public class LoadEvidenceForClassifier extends AbstractCLIContextCLI {

    @SuppressWarnings("unused")
    public static void main( String[] args ) throws Exception {

        new LoadEvidenceForClassifier( args );

    }

    // the services that will be needed
    BibliographicReferenceService bibliographicReferenceService = null;
    // a monthly dump of all evidence, takes too long to all, use files auto-generated
    private String evidenceDumpPath = File.separator + "neurocarta" + File.separator + "classifier" + File.separator
            + "AllPhenocartaAnnotations.tsv";

    // comes from the excluded set
    private Set<String> excludedPubmed = new HashSet<>();

    // these are all evidence that were used in the training set, exclude those ones to not give false results
    private String trainingSetPath = File.separator + "neurocarta" + File.separator + "classifier" + File.separator
            + "trainingSet.tsv";

    private BufferedWriter writer = null;

    public LoadEvidenceForClassifier( String[] args ) throws Exception {

        loadServices( args );

        // place to put the files results
        String writeFolder = createWriteFolderIfDoesntExist( "CLASSIFIER" );

        writer = new BufferedWriter( new FileWriter( writeFolder + "/mappingFound.tsv" ) );

        loadTrainingSetUsed();

        findEvidence();
    }

    @Override
    public CommandGroup getCommandGroup() {
        return CommandGroup.ANALYSIS;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.AbstractCLI#getCommandName()
     */
    @Override
    public String getCommandName() {
        return "loadEvidenceForClassifier";
    }

    @Override
    protected void buildOptions() {
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

    @Override
    protected Exception doWork( String[] args ) {
        // TODO Auto-generated method stub
        return null;
    }

    // load all needed services
    protected synchronized void loadServices( String[] args ) throws Exception {

        // this gets the context, so we can access beans
        processCommandLine( args );

        // add services if needed later
        this.bibliographicReferenceService = this.getBean( BibliographicReferenceService.class );

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

            // boolean isMajor = m.getIsMajorTopic();
            // for ( MedicalSubjectHeading q : m.getQualifiers() ) {
            // // ...
            // }

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

}
