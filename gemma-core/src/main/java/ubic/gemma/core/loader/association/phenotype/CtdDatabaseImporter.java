/*
 * The Gemma project
 *
 * Copyright (c) 2013 University of British Columbia
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

import ubic.basecode.util.FileTools;
import ubic.gemma.core.apps.GemmaCLI.CommandGroup;
import ubic.gemma.model.genome.Gene;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

public class CtdDatabaseImporter extends ExternalDatabaseEvidenceImporterAbstractCLI {

    // name of the external database
    private static final String CTD = "CTD";
    // name of the CTD file
    private static final String CTD_FILE = "CTD_genes_diseases.tsv.gz";
    // path of file to download
    private static final String CTD_URL_PATH = "http://ctdbase.org/reports/";
    // location of the ctd file
    private String ctdFile = "";

    @SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
    public CtdDatabaseImporter( String[] args ) throws Exception {
        super( args );
        Exception e = this.doWork( args );
        if ( e != null ) {
            e.printStackTrace();
        }
    }

    public static void main( String[] args ) throws Exception {
        CtdDatabaseImporter importEvidence = new CtdDatabaseImporter( args );
        Exception e = importEvidence.doWork( args );
        if ( e != null ) {
            e.printStackTrace();
        }
    }

    @Override
    public String getCommandName() {
        return "ctdImport";
    }

    @Override
    public String getShortDesc() {
        return "Creates a .tsv file of lines of evidence from CTD, to be used with EvidenceImporterCLI.java to import into Phenocarta.";
    }

    @Override
    protected void processOptions() {
        super.processOptions();
    }

    @Override
    public CommandGroup getCommandGroup() {
        return null;
    }

    @Override
    protected void buildOptions() {
        super.buildOptions();
    }

    @Override
    protected Exception doWork( String[] args ) {
        // create a folder named CTD
        try {
            writeFolder = this.createWriteFolderIfDoesntExist( CtdDatabaseImporter.CTD );
            // download the CTD file if we dont already have it
            this.downloadCTDFileIfDoesntExist();
            // using the disease ontology file creates the mapping from mesh and omim id to valuesUri
            this.findOmimAndMeshMappingUsingOntologyFile();
            this.processCTDFile();
        } catch ( Exception e ) {
            e.printStackTrace();
            return e;
        }

        return null;
    }

    private void downloadCTDFileIfDoesntExist() {

        // checks for the ctd file
        ctdFile = writeFolder + "/" + CtdDatabaseImporter.CTD_FILE;

        File fileCTD = new File( ctdFile );

        // super big file 700MB, only download if we dont already have
        if ( !fileCTD.exists() ) {
            // download the CTD file
            this.downloadFileFromWeb( CtdDatabaseImporter.CTD_URL_PATH, CtdDatabaseImporter.CTD_FILE );
        }
    }

    private void processCTDFile() throws Exception {

        // read the ctd file
        BufferedReader br = new BufferedReader(
                new InputStreamReader( FileTools.getInputStreamFromPlainOrCompressedFile( ctdFile ) ) );

        String line;

        // for each line of the file treat it
        while ( ( line = br.readLine() ) != null ) {

            if ( line.indexOf( '#' ) != -1 ) {
                continue;
            }

            String[] tokens = line.split( "\t" );

            if ( tokens.length == 9 ) {

                // structure of the file
                String geneSymbol = tokens[0].trim();
                String geneId = tokens[1].trim();
                String diseaseName = tokens[2].trim();
                String diseaseId = tokens[3].trim();
                String directEvidence = tokens[4].trim();
                String pubmedIds;

                pubmedIds = tokens[8].trim();

                // the evidence must be marker/mechanism or therapeutic to be imported into Phenocarta
                if ( ( directEvidence.equalsIgnoreCase( "marker/mechanism" ) || directEvidence
                        .equalsIgnoreCase( "therapeutic" ) ) && !pubmedIds.equalsIgnoreCase( "" ) ) {

                    String[] tokensPubmed = pubmedIds.split( "\\|" );

                    for ( String pubmed : tokensPubmed ) {

                        //noinspection ResultOfMethodCallIgnored // Checking for exceptions
                        Integer.parseInt( pubmed );

                        Gene gene = this.geneService.findByNCBIId( new Integer( geneId ) );

                        if ( gene != null ) {

                            if ( !gene.getOfficialSymbol().equalsIgnoreCase( geneSymbol ) ) {
                                logMessages.add( "!gene.getOfficialSymbol().equalsIgnoreCase( geneSymbol )???? :"
                                        + "Gemma: " + gene.getOfficialSymbol() + " File: " + geneSymbol );
                            }

                            this.findMapping( diseaseId, gene, pubmed, "TAS",
                                    "DiseaseName: " + diseaseName + " (" + diseaseId + "); DirectEvidence: "
                                            + directEvidence, diseaseName, CtdDatabaseImporter.CTD,
                                    "/detail.go?type=relationship&geneAcc=" + geneId + "&diseaseAcc=" + diseaseId
                                            + "&view=reference" );
                        }
                    }
                }
            }
        }

        this.writeBuffersAndCloseFiles();
    }
}