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
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import ubic.basecode.ontology.model.OntologyTerm;
import ubic.basecode.ontology.ncbo.OmimAnnotatorClient;
import ubic.gemma.model.genome.gene.phenotype.valueObject.CharacteristicValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.EvidenceSourceValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.GenericEvidenceValueObject;

public class OmimDatabaseImporter extends ExternalDatabaseEvidenceImporterAbstractCLI {

    // name of the external database
    protected static final String OMIM = "OMIM";

    // ********************************************************************************
    // the OMIM files to download
    public static final String OMIM_URL_PATH = "ftp://grcf.jhmi.edu/OMIM/";
    public static final String OMIM_FILE_MORBID = "morbidmap";
    public static final String OMIM_FILE_MIM = "mim2gene.txt";
    // ********************************************************************************

    // all omimID (gene or phenotype)
    private HashSet<Long> allOmimId = new HashSet<Long>();
    // omimID --> all list of publications
    private HashMap<Long, Collection<Long>> omimIdToPubmeds = new HashMap<Long, Collection<Long>>();

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

    // process all OMIM files to get the data out and manipulates it
    private void processOmimFiles( String morbidmap, String mim2gene ) throws IOException, NumberFormatException,
            InterruptedException {

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
                String omimPhenotypeId = tokens[0].substring( pos + 1, tokens[0].length() ).trim().split( " " )[0];
                // OMOM gene id
                String omimGeneId = tokens[2];
                // omimGeneid ---> ncbi id
                String ncbiGeneId = omimIdToGeneNCBI.get( omimGeneId );

                // is the omimGeneId found in the other file
                if ( ncbiGeneId != null ) {

                    String geneSymbol = this.geneService.findByNCBIId( new Integer( ncbiGeneId ) ).getOfficialSymbol();

                    // if there is no omim id given we cannot do anything with this line (happens often)
                    if ( !isInteger( omimPhenotypeId ) || Integer.parseInt( omimPhenotypeId ) < 100 ) {
                        continue;
                    }

                    String omimId = "OMIM:" + omimPhenotypeId;

                    log.info( "teating line: " + lineNumber++ );

                    // Case 1: lets use the Omim id to find the mapping phenotype, if it exists and not obsolote
                    if ( diseaseFileMappingFound.get( omimId ) != null ) {
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

                            EvidenceSourceValueObject evidenceSource = new EvidenceSourceValueObject( omimPhenotypeId,
                                    null );

                            // cheating a bit, using this structure to keep informations
                            GenericEvidenceValueObject e = new GenericEvidenceValueObject( new Integer( ncbiGeneId ),
                                    phenotypes, description, evidenceCode, false, evidenceSource );

                            // those 2 are wrong but doesnt matter much
                            evidenceSource.setExternalUrl( conditionUsed );
                            e.setGeneOfficialName( omimGeneId );
                            e.setGeneOfficialSymbol( geneSymbol );

                            String key = omimPhenotypeId + ncbiGeneId;

                            ArrayList<GenericEvidenceValueObject> evidences = new ArrayList<GenericEvidenceValueObject>();

                            // group them if they have the same key
                            if ( omimIDGeneToEvidence.get( key ) == null ) {
                                evidences.add( e );
                            } else {
                                evidences = omimIDGeneToEvidence.get( key );
                                evidences.add( e );
                            }
                            omimIDGeneToEvidence.put( key, evidences );

                            // keep all OmimId found (gene and phenotype)
                            allOmimId.add( new Long( omimGeneId ) );
                            allOmimId.add( new Long( omimPhenotypeId ) );
                        }
                    }

                    // if Case 1 and 2 fail, then lets use the annotator to try finding an answer
                    else {
                        writeInPossibleMappingAndNotFound( omimId, description, line, OMIM );
                    }
                }
            }
        }

        /** we have a set of OMIM id (phenotype and gene), we wanna know for each id the list of pubmeds related to them **/
        populateOmimIdsToPubmeds();

        // all lines have been treated here
        writeOutputFileHeaders4();
        combineAndWriteFinalEvidence();
        writeBuffersAndCloseFiles();
        br.close();
    }

    private void populateOmimIdsToPubmeds() throws InterruptedException {

        // HashSet to ArrayList, so no duplicate but can use list methods
        ArrayList<Long> allOmimIdList = new ArrayList<Long>();
        // allOmimId contains all OMIM id (phenotype and gene)
        allOmimIdList.addAll( allOmimId );

        int i = 0;

        while ( i < allOmimIdList.size() ) {

            int j = i + 10;

            if ( j > allOmimIdList.size() ) {
                j = allOmimIdList.size();
            }

            // each List can have a max size of 10, divide allOmimIdList into many lists
            List<Long> listWithLimitSize10 = allOmimIdList.subList( i, j );

            // call the api limiting the request to 10 Omim id and populates omimIdToPubmeds
            OmimAnnotatorClient.findLinkedPublications( listWithLimitSize10, omimIdToPubmeds );

            i = j;
        }
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

    private void combineAndWriteFinalEvidence() throws IOException, NumberFormatException {

        // case 0 and 1 at the end, combine same
        for ( ArrayList<GenericEvidenceValueObject> evidences : omimIDGeneToEvidence.values() ) {

            String geneSymbol = "";
            String ncbiGeneId = "";
            String evidenceCode = "";
            String description = "";
            HashSet<String> descriptions = new HashSet<String>();
            String omimPhenotypeId = "";
            String omimGeneId = "";
            String allValueUri = "";
            String phenotypeValue = "";
            String conditionUsed = "";

            HashSet<CharacteristicValueObject> phenoSet = new HashSet<CharacteristicValueObject>();

            for ( GenericEvidenceValueObject g : evidences ) {

                geneSymbol = g.getGeneOfficialSymbol();
                ncbiGeneId = g.getGeneNCBI().toString();
                evidenceCode = g.getEvidenceCode();
                descriptions.add( g.getDescription() );
                omimPhenotypeId = g.getEvidenceSource().getAccession();
                omimGeneId = g.getGeneOfficialName();
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

            // using the OMIM gene id and OMIM phenotype id keep what is common
            Collection<Long> commonsPubmeds = findCommonPubmed( new Long( omimGeneId ), new Long( omimPhenotypeId ) );

            for ( Long pubmed : commonsPubmeds ) {
                String evidenceLine = geneSymbol + "\t" + ncbiGeneId + "\t" + evidenceCode + "\t" + description + "\t"
                        + omimPhenotypeId + "\t" + allValueUri + "\t" + phenotypeValue + "\t" + conditionUsed + "\t"
                        + "" + "\t" + OMIM + "\t" + pubmed + "\t" + omimGeneId + "\n";

                outFinalResults.write( evidenceLine );
            }

            if ( commonsPubmeds.isEmpty() ) {
                String evidenceLine = geneSymbol + "\t" + ncbiGeneId + "\t" + evidenceCode + "\t" + description + "\t"
                        + omimPhenotypeId + "\t" + allValueUri + "\t" + phenotypeValue + "\t" + conditionUsed + "\t"
                        + "" + "\t" + OMIM + "\t" + "" + "\t" + omimGeneId + "\n";

                outFinalResults.write( evidenceLine );
            }
        }
    }

    // return all common pubmed between an omimGeneId and a omimPhenotypeId
    private Collection<Long> findCommonPubmed( Long omimGeneId, Long omimPhenotypeId ) {

        Collection<Long> pudmedFromGeneId = omimIdToPubmeds.get( omimGeneId );
        Collection<Long> pudmedFromPhenotypeId = omimIdToPubmeds.get( omimPhenotypeId );

        if ( pudmedFromGeneId != null && pudmedFromPhenotypeId != null ) {
            Collection<Long> commonElements = new HashSet<Long>( pudmedFromGeneId );
            commonElements.retainAll( pudmedFromPhenotypeId );

            return commonElements;
        }

        return new HashSet<Long>();
    }

}