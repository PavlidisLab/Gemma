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
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

import ubic.basecode.util.FileTools;

public class RgdDatabaseImporter extends ExternalDatabaseEvidenceImporterAbstractCLI {

    // name of the external database
    protected static final String RGD = "RGD";

    // path of files to download
    public static final String RGD_URL_PATH = "ftp://rgd.mcw.edu/pub/data_release/annotated_rgd_objects_by_ontology/";
    public static final String RGD_FILE_HUMAN = "homo_genes_rdo";
    public static final String RGD_FILE_MOUSE = "mus_genes_rdo";
    public static final String RGD_FILE_RAT = "rattus_genes_rdo";

    public static void main( String[] args ) throws Exception {

        RgdDatabaseImporter importEvidence = new RgdDatabaseImporter( args );

        // creates the folder where to place the file web downloaded files and final output files
        String writeFolder = importEvidence.createWriteFolder( RGD );

        String rgdHuman = importEvidence.downloadFileFromWeb( writeFolder, RGD_URL_PATH, RGD_FILE_HUMAN );
        String rgdMouse = importEvidence.downloadFileFromWeb( writeFolder, RGD_URL_PATH, RGD_FILE_MOUSE );
        String rgdRat = importEvidence.downloadFileFromWeb( writeFolder, RGD_URL_PATH, RGD_FILE_RAT );

        // find the OMIM and Mesh terms from the disease ontology file
        importEvidence.findOmimAndMeshMappingUsingOntologyFile( writeFolder );

        // process the rgd files
        importEvidence.processRGDFiles( rgdHuman, rgdMouse, rgdRat, writeFolder );

    }

    private void processRGDFiles( String rgdHuman, String rgdMouse, String rgdRat, String writeFolder )
            throws IOException {

        // this file have the final data to be imported
        BufferedWriter outFinalResults = new BufferedWriter( new FileWriter( writeFolder + "/finalResults.tsv" ) );

        // headers of the final file
        outFinalResults
                .write( "GeneSymbol\tTaxon\tPrimaryPubMed\tEvidenceCode\tComments\tExternalDatabase\tDatabaseLink\tPhenotypes\n" );

        processRGDFile( outFinalResults, "human", rgdHuman );
        processRGDFile( outFinalResults, "mouse", rgdMouse );
        processRGDFile( outFinalResults, "rat", rgdRat );
        outFinalResults.close();

    }

    public void processRGDFile( BufferedWriter outFinalResults, String taxon, String fileName ) throws IOException {

        BufferedReader br = new BufferedReader( new InputStreamReader(
                FileTools.getInputStreamFromPlainOrCompressedFile( fileName ) ) );

        String line = "";

        // reads the manual file and put the data in a stucture
        while ( ( line = br.readLine() ) != null ) {
            if ( line.indexOf( '!' ) != -1 ) {
                continue;
            }

            String[] tokens = line.split( "\t" );

            String geneSymbol = tokens[2];
            String pubmed = tokens[5].substring( tokens[5].indexOf( "PMID:" ) + 5, tokens[5].length() );
            String evidenceCode = tokens[6];
            String comment = tokens[3];
            String databaseLink = "?term=" + tokens[4] + "&id=" + tokens[1];
            String diseaseId = tokens[10];
            String valuesUri = findValueUriWithDiseaseId( diseaseId );

            if ( !valuesUri.isEmpty() && !evidenceCode.equalsIgnoreCase( "ISS" )
                    && !evidenceCode.equalsIgnoreCase( "NAS" ) && !evidenceCode.equalsIgnoreCase( "IEA" ) ) {

                outFinalResults.write( geneSymbol + "\t" + taxon + "\t" + pubmed + "\t" + evidenceCode + "\t" + comment
                        + "\t" + RGD + "\t" + databaseLink + "\t" + valuesUri + "\n" );

            }
        }
    }

    public RgdDatabaseImporter( String[] args ) throws Exception {
        super( args );
    }

}