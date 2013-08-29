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
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.SortedSet;
import java.util.TreeSet;

import ubic.basecode.ontology.model.OntologyTerm;
import ubic.gemma.model.genome.gene.phenotype.valueObject.CharacteristicValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.EvidenceSourceValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.GenericEvidenceValueObject;

public class OmimDatabaseImporter extends ExternalDatabaseEvidenceImporterAbstractCLI {

    // name of the external database
    protected static final String OMIM = "OMIM";

    // ***********************************************************************************
    // reading OMIM static files
    public static final String OMIM_FILES_PATH = RESOURCE_PATH + OMIM + File.separator;

    // when we find this description ingore it
    public static final String DESCRIPTION_TO_IGNORE = OMIM_FILES_PATH + "DescriptionToIgnore.tsv";

    // ********************************************************************************
    // the OMIM files to download
    public static final String OMIM_URL_PATH = "ftp://grcf.jhmi.edu/OMIM/";
    public static final String OMIM_FILE_MORBID = "morbidmap";
    public static final String OMIM_FILE_MIM = "mim2gene.txt";
    // ********************************************************************************

    // data structure that we will be using with OMIM data
    private HashMap<String, ArrayList<GenericEvidenceValueObject>> omimIDGeneToEvidence = new HashMap<String, ArrayList<GenericEvidenceValueObject>>();

    public static void main( String[] args ) throws Exception {

        OmimDatabaseImporter importEvidence = new OmimDatabaseImporter( args );

        // creates the folder where to place the file web downloaded files and final output files
        importEvidence.createWriteFolderWithDate( OMIM );

        // download the OMIM File called morbid
        String morbidmap = importEvidence.downloadFileFromWeb( OMIM_URL_PATH, OMIM_FILE_MORBID );
        // download the OMIM File called mim2gene
        String mim2gene = importEvidence.downloadFileFromWeb( OMIM_URL_PATH, OMIM_FILE_MIM );

        // find the OMIM and Mesh terms
        importEvidence.findOmimAndMeshMappingUsingOntologyFile();

        importEvidence.processOmimFiles( morbidmap, mim2gene );
    }

    public OmimDatabaseImporter( String[] args ) throws Exception {
        super( args );
    }

    // process all OMIm files to get the data out and manipulates it
    private void processOmimFiles( String morbidmap, String mim2gene ) throws IOException {

        // omim description we want to ignore, when we find those description, we know we are not interested in them
        HashSet<String> descriptionToIgnore = parseDescriptionToIgnore();

        // mapping find using mim2gene file, Omim id ---> Gene NCBI
        HashMap<String, String> omimIdToGeneNCBI = parseFileOmimIdToGeneNCBI( mim2gene );

        String line = null;

        BufferedReader br = new BufferedReader( new FileReader( morbidmap ) );

        int lineNumber = 1;

        // parse the morbid OMIM file
        while ( ( line = br.readLine() ) != null ) {

            String[] tokens = line.split( "\\|" );

            int pos = tokens[0].lastIndexOf( "," );

            // if there is a database link
            if ( pos != -1 ) {

                // phenotypes found
                Collection<String> phenotypesUri = new HashSet<String>();

                String conditionUsed = "";

                // OMIM description find in file, the annotator use description
                String description = tokens[0].substring( 0, pos ).trim();

                // evidence code we will use
                String evidenceCode = "TAS";
                // the OMIM id, (also is the database link)
                String omimId = tokens[0].substring( pos + 1, tokens[0].length() ).trim().split( " " )[0];
                // OMOM gene id
                String omimGeneId = tokens[2];

                // omimGeneid ---> ncbi id
                String ncbiGeneId = omimIdToGeneNCBI.get( omimGeneId );

                // is the omimGeneId found in the other file
                if ( ncbiGeneId != null ) {

                    String geneSymbol = this.geneService.findByNCBIId( new Integer( ncbiGeneId ) ).getOfficialSymbol();

                    // if there is no omim id given we cannot do anything with this line (happens often)
                    if ( !isInteger( omimId ) || Integer.parseInt( omimId ) < 100 ) {
                        continue;
                    }

                    omimId = "OMIM:" + omimId;

                    log.info( "teating line: " + lineNumber++ );

                    // Case 0: we want to exclude this omim descriptipn
                    if ( descriptionToIgnore.contains( description ) ) {
                        outNotFoundBuffer.add( description + "\t" + "Description to Ignore" + "\n" );
                        continue;
                    }

                    // Case 1: lets use the Omim id to find the mapping phenotype, if it exists and not obsolote
                    else if ( diseaseFileMappingFound.get( omimId ) != null ) {
                        phenotypesUri = diseaseFileMappingFound.get( omimId );
                        conditionUsed = "Case 1: Found with OMIM ID";
                    }

                    // Case 2: use the static manual annotation file
                    else if ( manualDescriptionToValuesUriMapping.get( omimId ) != null ) {
                        phenotypesUri = manualDescriptionToValuesUriMapping.get( omimId );
                        conditionUsed = "Case 2: Found with Description, Manual Mapping";
                    }

                    // Case 1 and Case 2 are the only cases that we want to import
                    if ( conditionUsed.indexOf( "Case 1:" ) != -1 || conditionUsed.indexOf( "Case 2:" ) != -1 ) {

                        SortedSet<CharacteristicValueObject> phenotypes = new TreeSet<CharacteristicValueObject>();

                        for ( String valueUri : phenotypesUri ) {

                            OntologyTerm ontologyTerm = this.diseaseOntologyService.getTerm( valueUri );
                            if ( ontologyTerm == null ) {
                                ontologyTerm = this.humanPhenotypeOntologyService.getTerm( valueUri );
                            }

                            // ontologyTerm can never be null or obsolete we checked before
                            CharacteristicValueObject c = new CharacteristicValueObject( ontologyTerm.getLabel(),
                                    valueUri );
                            phenotypes.add( c );
                        }

                        if ( !phenotypes.isEmpty() ) {

                            EvidenceSourceValueObject evidenceSource = new EvidenceSourceValueObject( omimId, null );
                            evidenceSource.setExternalUrl( conditionUsed );

                            GenericEvidenceValueObject e = new GenericEvidenceValueObject( new Integer( ncbiGeneId ),
                                    phenotypes, description, evidenceCode, false, evidenceSource );

                            e.setGeneOfficialSymbol( geneSymbol );

                            String key = omimId + ncbiGeneId;

                            ArrayList<GenericEvidenceValueObject> evidences = new ArrayList<GenericEvidenceValueObject>();

                            if ( omimIDGeneToEvidence.get( key ) == null ) {
                                evidences.add( e );
                            } else {
                                evidences = omimIDGeneToEvidence.get( key );
                                evidences.add( e );
                            }

                            omimIDGeneToEvidence.put( key, evidences );
                        }
                    }

                    // if Case 1 and 2 fail, then lets use the annotator to try finding an answer
                    else {
                        writeInPossibleMappingAndNotFound( omimId, description, line, OMIM );
                    }
                }
            }
        }

        // all lines have been treated here
        writeOutputFileHeaders3();
        combineAndWriteFinalEvidence();
        writeBuffersAndCloseFiles();
        br.close();
    }

    private HashSet<String> parseDescriptionToIgnore() throws IOException {

        HashSet<String> descriptionToIgnore = new HashSet<String>();

        BufferedReader br = new BufferedReader( new InputStreamReader(
                OmimDatabaseImporter.class.getResourceAsStream( DESCRIPTION_TO_IGNORE ) ) );

        String line = "";

        while ( ( line = br.readLine() ) != null ) {

            line = removeSmallNumberAndTxt( line );

            descriptionToIgnore.add( removeCha( line ).trim() );
        }

        return descriptionToIgnore;
    }

    private HashMap<String, String> parseFileOmimIdToGeneNCBI( String mim2gene ) throws IOException {

        String line = null;
        HashMap<String, String> omimIdToGeneNCBI = new HashMap<String, String>();

        BufferedReader br = new BufferedReader( new FileReader( mim2gene ) );

        while ( ( line = br.readLine() ) != null ) {

            String[] tokens = line.split( "\t" );

            if ( !tokens[2].trim().equals( "-" ) && !tokens[3].trim().equals( "-" ) && !tokens[2].trim().equals( "" )
                    && !tokens[3].trim().equals( "" ) ) {
                omimIdToGeneNCBI.put( tokens[0], tokens[2] );
            }
        }
        br.close();
        return omimIdToGeneNCBI;
    }

    private void combineAndWriteFinalEvidence() throws IOException {

        // case 0 and 1 at the end, combine same
        for ( ArrayList<GenericEvidenceValueObject> evidences : omimIDGeneToEvidence.values() ) {

            String geneSymbol = "";
            String ncbiGeneId = "";
            String evidenceCode = "";
            String description = "";
            HashSet<String> descriptions = new HashSet<String>();
            String databaseLink = "";
            String allValueUri = "";
            String phenotypeValue = "";
            String conditionUsed = "";
            HashSet<CharacteristicValueObject> phenoSet = new HashSet<CharacteristicValueObject>();

            for ( GenericEvidenceValueObject g : evidences ) {

                geneSymbol = g.getGeneOfficialSymbol();
                ncbiGeneId = g.getGeneNCBI().toString();
                evidenceCode = g.getEvidenceCode();
                descriptions.add( g.getDescription() );
                databaseLink = g.getEvidenceSource().getAccession();
                // just used this field to hide some information...
                conditionUsed = g.getEvidenceSource().getExternalUrl();

                for ( CharacteristicValueObject c : g.getPhenotypes() ) {

                    phenoSet.add( c );
                }
            }

            for ( CharacteristicValueObject phe : phenoSet ) {
                allValueUri = allValueUri + phe.getValueUri() + ";";
                phenotypeValue = phenotypeValue + phe.getValue() + ";";
            }
            for ( String desc : descriptions ) {
                description = description + desc + "; ";
            }

            if ( !description.isEmpty() ) {
                description = description.substring( 0, description.length() - 2 );
            }

            String evidenceLine = geneSymbol + "\t" + ncbiGeneId + "\t" + evidenceCode + "\t" + description + "\t"
                    + databaseLink + "\t" + allValueUri + "\t" + phenotypeValue + "\t" + conditionUsed + "\t" + ""
                    + "\t" + OMIM + "\n";

            outFinalResults.write( evidenceLine );
        }

    }

}