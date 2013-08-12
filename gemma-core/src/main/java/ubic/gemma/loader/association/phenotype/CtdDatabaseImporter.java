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
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

import ubic.basecode.util.FileTools;

public class CtdDatabaseImporter extends ExternalDatabaseEvidenceImporterAbstractCLI {

    // name of the external database
    protected static final String CTD = "CTD";

    // path of file to download
    public static final String CTD_URL_PATH = "http://ctdbase.org/reports/";
    public static final String CTD_FILE = "CTD_genes_diseases.tsv.gz";

    public static void main( String[] args ) throws Exception {

        CtdDatabaseImporter importEvidence = new CtdDatabaseImporter( args );

        // creates the folder where to place the file web downloaded files and final output files
        String writeFolder = importEvidence.createWriteFolderIfDoesntExist( CTD );

        String ctdFile = writeFolder + "/" + CTD_FILE;

        File fileCTD = new File( ctdFile );

        // super big file 700MB, only download if we dont already have
        if ( !fileCTD.exists() ) {
            // download the GWAS file
            ctdFile = importEvidence.downloadFileFromWeb( writeFolder, CTD_URL_PATH, CTD_FILE );
        }

        // find the OMIM and Mesh terms from the disease ontology file
        importEvidence.findOmimAndMeshMappingUsingOntologyFile( writeFolder );

        // process the gwas file
        importEvidence.processCTDFile( ctdFile, writeFolder );

    }

    private void processCTDFile( String fileName, String writeFolder ) throws IOException {

        // this file have the final data to be imported
        BufferedWriter outFinalResults = new BufferedWriter( new FileWriter( writeFolder + "/finalResults.tsv" ) );

        // headers of the final file
        outFinalResults
                .write( "GeneSymbol\tGeneId\tPrimaryPubMed\tEvidenceCode\tComments\tExternalDatabase\tDatabaseLink\tPhenotypes\n" );

        BufferedReader br = new BufferedReader( new InputStreamReader(
                FileTools.getInputStreamFromPlainOrCompressedFile( fileName ) ) );

        String line = "";

        // reads the manual file and put the data in a stucture
        while ( ( line = br.readLine() ) != null ) {
            if ( line.indexOf( '#' ) != -1 ) {
                continue;
            }

            String[] tokens = line.split( "\t" );

            String geneSymbol = tokens[0];
            String geneId = tokens[1];
            String diseaseName = tokens[2];
            String diseaseId = tokens[3];
            String directEvidence = tokens[4];
            String pubmedIds = "";

            if ( tokens.length < 9 ) {
                pubmedIds = tokens[7].trim();
            } else {
                pubmedIds = tokens[8].trim();
            }

            String evidenceCode = "TAS";

            if ( ( directEvidence.equalsIgnoreCase( "marker/mechanism" ) || directEvidence
                    .equalsIgnoreCase( "therapeutic" ) ) && !pubmedIds.equalsIgnoreCase( "" ) ) {

                String allValueUri = findValueUriWithDiseaseId( diseaseId );

                if ( !allValueUri.isEmpty() ) {

                    String[] pubmedsIdArray = pubmedIds.split( "\\|" );

                    // for each pubmed id found
                    for ( String pubmed : pubmedsIdArray ) {
                        outFinalResults.write( geneSymbol + "\t" + geneId + "\t" + pubmed + "\t" + evidenceCode + "\t"
                                + "DiseaseName: " + diseaseName + " (" + diseaseId + "); DirectEvidence: "
                                + directEvidence + "\t" + CTD + "\t" + "/detail.go?type=relationship&geneAcc=" + geneId
                                + "&diseaseAcc=" + diseaseId + "&view=reference" + "\t" + allValueUri + "\n" );

                    }
                }
            }
        }
        outFinalResults.close();
    }

    public CtdDatabaseImporter( String[] args ) throws Exception {
        super( args );
    }

}