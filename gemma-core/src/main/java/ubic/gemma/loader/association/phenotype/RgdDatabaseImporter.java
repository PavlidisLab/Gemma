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
import java.util.HashMap;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import ubic.basecode.ontology.model.OntologyTerm;
import ubic.basecode.util.FileTools;

public class RgdDatabaseImporter extends ExternalDatabaseEvidenceImporterAbstractCLI {

    // name of the external database
    protected static final String RGD = "RGD";

    // path of files to download
    public static final String RGD_URL_PATH = "ftp://rgd.mcw.edu/pub/data_release/annotated_rgd_objects_by_ontology/";
    public static final String RGD_FILE_HUMAN = "homo_genes_rdo";
    public static final String RGD_FILE_MOUSE = "mus_genes_rdo";
    public static final String RGD_FILE_RAT = "rattus_genes_rdo";

    // path for resources
    public static final String RGD_FILES_PATH = RESOURCE_PATH + RGD + "_CTD" + File.separator;

    // manual static file mapping
    public static final String MANUAL_MAPPING_RGD = RGD_FILES_PATH + "ManualDescriptionMapping_CTD_RGD.tsv";

    public static void main( String[] args ) throws Exception {

        // JUST FOR NOW
        Logger root = Logger.getRootLogger();
        root.setLevel( Level.INFO );

        RgdDatabaseImporter importEvidence = new RgdDatabaseImporter( args );

        // creates the folder where to place the file web downloaded files and final output files
        importEvidence.createWriteFolderWithDate( RGD );

        String rgdHuman = importEvidence.downloadFileFromWeb( RGD_URL_PATH, RGD_FILE_HUMAN );
        String rgdMouse = importEvidence.downloadFileFromWeb( RGD_URL_PATH, RGD_FILE_MOUSE );
        String rgdRat = importEvidence.downloadFileFromWeb( RGD_URL_PATH, RGD_FILE_RAT );

        // find the OMIM and Mesh terms from the disease ontology file
        importEvidence.findOmimAndMeshMappingUsingOntologyFile();

        // process the rgd files
        importEvidence.processRGDFiles( rgdHuman, rgdMouse, rgdRat );

    }

    private void processRGDFiles( String rgdHuman, String rgdMouse, String rgdRat ) throws IOException {

        // this file have the final data to be imported
        outFinalResults = new BufferedWriter( new FileWriter( writeFolder + "/finalResults.tsv" ) );

        // headers of the final file
        outFinalResults
                .write( "GeneSymbol\tTaxon\tPrimaryPubMed\tEvidenceCode\tComments\tExternalDatabase\tDatabaseLink\tPhenotypes\n" );

        processRGDFile( "human", rgdHuman );
        processRGDFile( "mouse", rgdMouse );
        processRGDFile( "rat", rgdRat );
        outFinalResults.close();

        if ( !errorMessages.isEmpty() ) {

            log.info( "here is the error messages :\n" );

            for ( String err : errorMessages ) {

                log.error( err );
            }
        }

    }

    public void processRGDFile( String taxon, String fileName ) throws IOException {

        BufferedReader br = new BufferedReader( new InputStreamReader(
                FileTools.getInputStreamFromPlainOrCompressedFile( fileName ) ) );

        HashMap<String, String> mesh2ValueUri = parseFileManualDescriptionFile();

        String line = "";

        // reads the manual file and put the data in a structure
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

            String valueUri = findValueUriWithDiseaseId( diseaseId );

            if ( valueUri.isEmpty() ) {
                valueUri = mesh2ValueUri.get( findId( diseaseId ) );
            }

            if ( valueUri != null && !valueUri.isEmpty() && !evidenceCode.equalsIgnoreCase( "ISS" )
                    && !evidenceCode.equalsIgnoreCase( "NAS" ) && !evidenceCode.equalsIgnoreCase( "IEA" ) ) {

                outFinalResults.write( geneSymbol + "\t" + taxon + "\t" + pubmed + "\t" + evidenceCode + "\t" + comment
                        + "\t" + RGD + "\t" + databaseLink + "\t" + valueUri + "\n" );

            }
        }
    }

    public RgdDatabaseImporter( String[] args ) throws Exception {
        super( args );
    }

    // create the mapping from the static file also verify that what is in the file makes sense
    private HashMap<String, String> parseFileManualDescriptionFile() throws IOException {

        HashMap<String, String> mesh2ValueUri = new HashMap<String, String>();

        BufferedReader br = new BufferedReader( new InputStreamReader(
                RgdDatabaseImporter.class.getResourceAsStream( MANUAL_MAPPING_RGD ) ) );

        String line = "";

        // reads the manual file and put the data in a structure
        while ( ( line = br.readLine() ) != null ) {
            String[] tokens = line.split( "\t" );

            String meshId = tokens[0];
            String valueUriStaticFile = tokens[1];
            String valueStaticFile = tokens[2];

            OntologyTerm ontologyTerm = findOntologyTermExistAndNotObsolote( valueUriStaticFile );

            if ( ontologyTerm != null ) {

                if ( ontologyTerm.getLabel().equalsIgnoreCase( valueStaticFile ) ) {

                    mesh2ValueUri.put( meshId, valueUriStaticFile );

                } else {
                    errorMessages.add( "MANUAL VALUEURI AND VALUE DOESNT MATCH: " + line );
                }
            } else {
                errorMessages.add( "MANUAL MAPPING FILE TERM OBSOLETE OR NOT EXISTANT: '" + valueUriStaticFile + "' "
                        + " (" + valueStaticFile + ")" );
            }
        }
        return mesh2ValueUri;
    }

}