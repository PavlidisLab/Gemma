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
import java.io.InputStreamReader;

import ubic.basecode.ontology.ncbo.AnnotatorClient;
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
        importEvidence.createWriteFolderWithDate( RGD );

        String rgdHuman = importEvidence.downloadFileFromWeb( RGD_URL_PATH, RGD_FILE_HUMAN );
        String rgdMouse = importEvidence.downloadFileFromWeb( RGD_URL_PATH, RGD_FILE_MOUSE );
        String rgdRat = importEvidence.downloadFileFromWeb( RGD_URL_PATH, RGD_FILE_RAT );

        // find the OMIM and Mesh terms from the disease ontology file
        importEvidence.findOmimAndMeshMappingUsingOntologyFile();

        // process the rgd files
        importEvidence.processRGDFiles( rgdHuman, rgdMouse, rgdRat );

    }

    private void processRGDFiles( String rgdHuman, String rgdMouse, String rgdRat ) throws Exception {

        writeOutputFileHeaders2();
        processRGDFile( "human", rgdHuman );
        processRGDFile( "mouse", rgdMouse );
        processRGDFile( "rat", rgdRat );
        writeBuffersAndCloseFiles();
    }

    public void processRGDFile( String taxon, String fileName ) throws Exception {

        BufferedReader br = new BufferedReader( new InputStreamReader(
                FileTools.getInputStreamFromPlainOrCompressedFile( fileName ) ) );

        String line = "";

        // reads the manual file and put the data in a structure
        while ( ( line = br.readLine() ) != null ) {
            if ( line.indexOf( '!' ) != -1 ) {
                continue;
            }

            String[] tokens = line.split( "\t" );

            String geneSymbol = removeSpecialSymbol( tokens[2] );

            String pubmed = tokens[5].substring( tokens[5].indexOf( "PMID:" ) + 5, tokens[5].length() );
            String evidenceCode = tokens[6];
            String comment = tokens[3];
            String databaseLink = "?term=" + tokens[4] + "&id=" + tokens[1];
            String diseaseId = tokens[10];

            if ( !evidenceCode.equalsIgnoreCase( "ISS" ) && !evidenceCode.equalsIgnoreCase( "NAS" )
                    && !evidenceCode.equalsIgnoreCase( "IEA" ) && !diseaseId.equals( "" ) && !pubmed.equals( "" ) ) {

                // 1- using the disease ontology first look is a mapping is found
                String valuesUri = findValueUriWithDiseaseId( diseaseId );

                if ( valuesUri.isEmpty() && manualDescriptionToValuesUriMapping.get( diseaseId ) != null ) {

                    for ( String valueUriFound : manualDescriptionToValuesUriMapping.get( diseaseId ) ) {
                        // 2 - If we couldnt find it lets use the manual mapping file
                        valuesUri = valuesUri + valueUriFound + ";";
                    }
                }

                if ( !valuesUri.isEmpty() ) {

                    outFinalResults.write( geneSymbol + "\t" + taxon + "\t" + pubmed + "\t" + evidenceCode + "\t"
                            + comment + "\t" + RGD + "\t" + databaseLink + "\t" + valuesUri + "\n" );
                }
                /**
                 * nothing was found in 1- or 2-, in this case lets use the annotator and check later if the returned
                 * value make sense to add them to the manual mapping
                 **/
                else {
                    /**
                     * we dont have any description of those mesh and omim term we need to use a web service to find
                     * them
                     **/

                    String description = findDescriptionUsingTerm( diseaseId );

                    if ( description != null && !description.isEmpty() ) {
                        writeInPossibleMappingAndNotFound( diseaseId, description, line, RGD );
                    }
                }
            }
        }
    }

    // we are not given the phenotype description, use a web service to find it
    private String findDescriptionUsingTerm( String diseaseId ) throws Exception {

        String conceptId = diseaseId.substring( diseaseId.indexOf( ":" ) + 1, diseaseId.length() );

        if ( diseaseId.indexOf( "OMIM:" ) != -1 ) {
            return AnnotatorClient.findLabelUsingIdentifier( AnnotatorClient.OMIM_ONTOLOGY, conceptId );
        } else if ( diseaseId.indexOf( "MESH:" ) != -1 ) {
            return AnnotatorClient.findLabelUsingIdentifier( AnnotatorClient.MESH_ONTOLOGY, conceptId );
        } else {
            throw new Exception( "diseaseId not OMIM or MESH: " + diseaseId );
        }

    }

    public RgdDatabaseImporter( String[] args ) throws Exception {
        super( args );
    }

    private String removeSpecialSymbol( String geneId ) {
        int index1 = geneId.indexOf( "<sup>" );

        if ( index1 != -1 ) {
            return geneId.substring( 0, index1 );
        }
        return geneId;
    }

}