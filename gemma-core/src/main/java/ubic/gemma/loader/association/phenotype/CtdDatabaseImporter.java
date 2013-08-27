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
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.HashSet;
import java.util.TreeSet;

import ubic.basecode.ontology.ncbo.AnnotatorResponse;
import ubic.basecode.util.FileTools;

public class CtdDatabaseImporter extends ExternalDatabaseEvidenceImporterAbstractCLI {

    // name of the external database
    public static final String CTD = "CTD";
    // path of file to download
    public static final String CTD_URL_PATH = "http://ctdbase.org/reports/";
    // name of the CTD file
    public static final String CTD_FILE = "CTD_genes_diseases.tsv.gz";

    // location of the ctd file
    private String ctdFile = "";

    public static void main( String[] args ) throws Exception {

        CtdDatabaseImporter importEvidence = new CtdDatabaseImporter( args );
        importEvidence.processCTDFile();
    }

    public CtdDatabaseImporter( String[] args ) throws Exception {
        super( args );

        // create a folder name CTD
        writeFolder = createWriteFolderIfDoesntExist( CTD );

        // download the CTD file if we dont already have it
        downloadCTDFileIfDoesntExist();

        // using the disease ontology file creates the mapping from mesh and omim id to valuesUri
        findOmimAndMeshMappingUsingOntologyFile();

        // parse the manual description file
        parseFileManualDescriptionFile_RGD_CTD();

        // parse the results to ignore when using the annotator
        parseResultsToIgnore();

        // write headers of the final file
        writeOutputFileHeaders();
    }

    private void processCTDFile() throws IOException {

        // read the ctd file
        BufferedReader br = new BufferedReader( new InputStreamReader(
                FileTools.getInputStreamFromPlainOrCompressedFile( ctdFile ) ) );

        String line = "";

        int i = 0;

        // for each line of the file treat it
        while ( ( line = br.readLine() ) != null ) {
            if ( line.indexOf( '#' ) != -1 ) {
                continue;
            }

            String[] tokens = line.split( "\t" );
            // structure of the file
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

            // the evidence must be marker/mechanism or therapeutic to be imported into Neurocarta
            if ( ( directEvidence.equalsIgnoreCase( "marker/mechanism" ) || directEvidence
                    .equalsIgnoreCase( "therapeutic" ) ) && !pubmedIds.equalsIgnoreCase( "" ) ) {

                i++;

                System.out.println( i + "/22 264" );

                // 1- using the disease ontology first look is a mapping is found
                String valuesUri = findValueUriWithDiseaseId( diseaseId );

                if ( valuesUri.isEmpty() && mesh2ValueUri_RGD_CTD.get( diseaseId ) != null ) {

                    for ( String valueUriFound : mesh2ValueUri_RGD_CTD.get( diseaseId ) ) {
                        // 2 - If we couldnt find it lets use the manual mapping file
                        valuesUri = valuesUri + valueUriFound + ";";
                    }
                }

                // those 2 cases represent final values that can be imported
                if ( !valuesUri.isEmpty() ) {

                    String[] pubmedsIdArray = pubmedIds.split( "\\|" );

                    // for each pubmed id found
                    for ( String pubmed : pubmedsIdArray ) {
                        outFinalResults.write( geneSymbol + "\t" + geneId + "\t" + pubmed + "\t" + "TAS" + "\t"
                                + "DiseaseName: " + diseaseName + " (" + diseaseId + "); DirectEvidence: "
                                + directEvidence + "\t" + CTD + "\t" + "/detail.go?type=relationship&geneAcc=" + geneId
                                + "&diseaseAcc=" + diseaseId + "&view=reference" + "\t" + valuesUri + "\n" );
                    }
                }
                /**
                 * nothing was found in 1- or 2-, in this case lets use the annotator and check later if the returned
                 * value make sense to add them to the manual mapping
                 **/
                else {

                    String searchTerm = diseaseName;

                    // search with the annotator and filter result to take out obsolete terms given
                    Collection<AnnotatorResponse> ontologyTermsNormal = removeNotExistAndObsolete( anoClient
                            .findTerm( searchTerm ) );

                    Collection<AnnotatorResponse> ontologyTermsWithOutKeywords = new HashSet<AnnotatorResponse>();

                    // did we find something ?
                    String condition = findConditionUsed( ontologyTermsNormal, false );

                    if ( condition == null ) {
                        // search again manipulating the search string
                        String searchTermWithOutKeywords = removeSpecificKeywords( searchTerm );

                        ontologyTermsWithOutKeywords = removeNotExistAndObsolete( anoClient
                                .findTerm( searchTermWithOutKeywords ) );
                        // did we find something ?
                        condition = findConditionUsed( ontologyTermsWithOutKeywords, true );
                    }

                    // if a satisfying condition was found write in down in the mapping found
                    if ( condition != null ) {

                        String lineToWrite = diseaseId + "\t" + valueUriForCondition + "\t" + valueForCondition + "\t"
                                + diseaseName + "\t" + condition + "\n";

                        outMappingFoundBuffer.add( lineToWrite );
                    } else if ( !ontologyTermsNormal.isEmpty() || !ontologyTermsWithOutKeywords.isEmpty() ) {

                        Collection<AnnotatorResponse> allAnnotatorResponse = new TreeSet<AnnotatorResponse>();
                        allAnnotatorResponse.addAll( ontologyTermsNormal );
                        allAnnotatorResponse.addAll( ontologyTermsWithOutKeywords );
                        // multiple mapping found without a specific condition
                        writeInPossibleMapping_RGD_CTD( allAnnotatorResponse, diseaseId, diseaseName );
                    }

                    else {
                        // nothing was found with the annotator, write the line in the not found file
                        outNotFoundBuffer.add( line + "\n" );
                    }
                }
            }
        }

        writeBuffersAndCloseFiles();
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

}