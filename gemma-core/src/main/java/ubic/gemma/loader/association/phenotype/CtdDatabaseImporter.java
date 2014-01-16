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
package ubic.gemma.loader.association.phenotype;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

import ubic.basecode.util.FileTools;
import ubic.gemma.model.genome.Gene;

public class CtdDatabaseImporter extends ExternalDatabaseEvidenceImporterAbstractCLI {

    // name of the external database
    public static final String CTD = "CTD";
    // path of file to download
    public static final String CTD_URL_PATH = "http://ctdbase.org/reports/";
    // name of the CTD file
    public static final String CTD_FILE = "CTD_genes_diseases.tsv.gz";

    public static void main( String[] args ) throws Exception {

        CtdDatabaseImporter importEvidence = new CtdDatabaseImporter( args );
        importEvidence.processCTDFile();
    }

    // location of the ctd file
    private String ctdFile = "";

    public CtdDatabaseImporter( String[] args ) throws Exception {
        super( args );

        // create a folder named CTD
        writeFolder = createWriteFolderIfDoesntExist( CTD );

        // download the CTD file if we dont already have it
        downloadCTDFileIfDoesntExist();

        // using the disease ontology file creates the mapping from mesh and omim id to valuesUri
        findOmimAndMeshMappingUsingOntologyFile();

    }

    private void downloadCTDFileIfDoesntExist() {

        // checks for the ctd file
        ctdFile = writeFolder + "/" + CTD_FILE;

        File fileCTD = new File( ctdFile );

        // super big file 700MB, only download if we dont already have
        if ( !fileCTD.exists() ) {
            // download the CTD file
            downloadFileFromWeb( CTD_URL_PATH, CTD_FILE );
        }
    }

    private void processCTDFile() throws Exception {

        // read the ctd file
        BufferedReader br = new BufferedReader( new InputStreamReader(
                FileTools.getInputStreamFromPlainOrCompressedFile( ctdFile ) ) );

        String line = "";

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
                String pubmedIds = "";

                pubmedIds = tokens[8].trim();

                // the evidence must be marker/mechanism or therapeutic to be imported into Phenocarta
                if ( ( directEvidence.equalsIgnoreCase( "marker/mechanism" ) || directEvidence
                        .equalsIgnoreCase( "therapeutic" ) ) && !pubmedIds.equalsIgnoreCase( "" ) ) {

                    String[] tokensPubmed = pubmedIds.split( "\\|" );

                    for ( String pubmed : tokensPubmed ) {

                        Integer.parseInt( pubmed );

                        Gene gene = this.geneService.findByNCBIId( new Integer( geneId ) );

                        if ( gene != null ) {

                            if ( !gene.getOfficialSymbol().equalsIgnoreCase( geneSymbol ) ) {
                                logMessages.add( "!gene.getOfficialSymbol().equalsIgnoreCase( geneSymbol )???? :"
                                        + "Gemma: " + gene.getOfficialSymbol() + " File: " + geneSymbol );
                            }

                            findMapping( diseaseId, gene, pubmed, "TAS", "DiseaseName: " + diseaseName + " ("
                                    + diseaseId + "); DirectEvidence: " + directEvidence, diseaseName, CTD,
                                    "/detail.go?type=relationship&geneAcc=" + geneId + "&diseaseAcc=" + diseaseId
                                            + "&view=reference" );
                        }
                    }
                }
            }
        }

        writeBuffersAndCloseFiles();
    }

}