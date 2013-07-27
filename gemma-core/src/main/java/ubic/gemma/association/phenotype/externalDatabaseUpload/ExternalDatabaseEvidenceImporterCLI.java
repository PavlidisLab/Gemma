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
package ubic.gemma.association.phenotype.externalDatabaseUpload;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.SortedSet;
import java.util.TreeSet;

import ubic.basecode.ontology.model.OntologyTerm;
import ubic.basecode.ontology.ncbo.AnnotatorClient;
import ubic.basecode.ontology.ncbo.AnnotatorResponse;
import ubic.gemma.model.genome.gene.phenotype.valueObject.CharacteristicValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.EvidenceSourceValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.GenericEvidenceValueObject;

public class ExternalDatabaseEvidenceImporterCLI extends ExternalDatabaseEvidenceImporterAbstractCLI {

    HashMap<String, String> phenotypeToOmimMapping = new HashMap<String, String>();

    public static void main( String[] args ) throws Exception {

        // choose the external database to export
        String externalDatabase = "OMIM";

        ExternalDatabaseEvidenceImporterCLI importEvidence = new ExternalDatabaseEvidenceImporterCLI();
        // access the Gemma Ontologies Service
        importEvidence.loadOntologyServices( args );

        // create a special Folder where to place and file Download + Results
        String writeFolder = importEvidence.createWriteFolder( externalDatabase );

        if ( externalDatabase.equalsIgnoreCase( "OMIM" ) ) {

            // download the disease Ontology File
            String diseaseOntologyFile = importEvidence.downloadFileFromWeb( writeFolder, DISEASE_ONT_PATH,
                    DISEASE_ONT_FILE );
            // download the OMIM File called morbid
            String morbidmap = importEvidence.downloadFileFromWeb( writeFolder, OMIM_URL_PATH, OMIM_FILE_MORBID );
            // download the OMIM File called mim2gene
            String mim2gene = importEvidence.downloadFileFromWeb( writeFolder, OMIM_URL_PATH, OMIM_FILE_MIM );

            // map the omim id to the PhenotypeValueUri using the disease Ontology file downloaded
            HashMap<String, HashSet<String>> omimIdToPhenotypeMapping = importEvidence
                    .findOmimPhenotypeMapping( diseaseOntologyFile );

            importEvidence.processOmimFiles( writeFolder, omimIdToPhenotypeMapping, morbidmap, mim2gene );

        }
    }

    private HashMap<String, HashSet<String>> findOmimPhenotypeMapping( String diseaseOntologyPath ) throws IOException {

        HashMap<String, HashSet<String>> omimIdToPhenotypeMapping = new HashMap<String, HashSet<String>>();

        HashSet<String> omimIds = new HashSet<String>();
        String valueUri = null;

        BufferedReader br = new BufferedReader( new FileReader( diseaseOntologyPath ) );

        String line = "";

        boolean foundTerm = false;

        while ( ( line = br.readLine() ) != null ) {

            String[] tokens = null;

            line = line.trim();

            // found a term
            if ( line.equalsIgnoreCase( "[Term]" ) ) {
                foundTerm = true;
                valueUri = null;
                omimIds = new HashSet<String>();
            } else if ( foundTerm ) {

                if ( line.startsWith( "id:" ) ) {

                    tokens = line.split( ":" );

                    String diseaseId = tokens[2].trim();
                    // will throw exception if a number is not found
                    Integer.parseInt( diseaseId );
                    // disease id
                    valueUri = "http://purl.obolibrary.org/obo/DOID_" + diseaseId;

                } else if ( line.indexOf( "xref: OMIM" ) != -1 ) {

                    tokens = line.split( ":" );
                    omimIds.add( tokens[2].trim() );
                }

                // end of a term
                else if ( line.equalsIgnoreCase( "" ) ) {

                    foundTerm = false;

                    for ( String omimId : omimIds ) {

                        HashSet<String> h = new HashSet<String>();

                        if ( omimIdToPhenotypeMapping.get( omimId ) == null ) {
                            h.add( valueUri );
                        } else {
                            h = omimIdToPhenotypeMapping.get( omimId );
                            h.add( valueUri );
                        }
                        omimIdToPhenotypeMapping.put( omimId, h );

                        // only add the mapping if not obsolete
                        if ( !isObsoleteOrNotExist( valueUri ) ) {
                            phenotypeToOmimMapping.put( valueUri, omimId );
                        }
                    }
                }
            }
        }

        return omimIdToPhenotypeMapping;
    }

    /**
     * Goes on the specified urlPath and download the file to place it into the writeFolder
     */
    private String downloadFileFromWeb( String writeFolder, String pathName, String fileUrlName ) {

        String fileName = fileUrlName;

        String fullPathToDownload = pathName + fileUrlName;

        // here we change the name of this specific file so itcan be human readable
        if ( fileName.equalsIgnoreCase( DISEASE_ONT_FILE ) ) {
            fileName = "diseaseOntology.txt";
        }

        String pathFileName = writeFolder + "/" + fileName;

        try {

            System.out.println( "Trying to download : " + fullPathToDownload );

            URL url = new URL( fullPathToDownload );
            url.openConnection();
            InputStream reader = url.openStream();

            FileOutputStream writer = new FileOutputStream( pathFileName );
            byte[] buffer = new byte[153600];
            int bytesRead = 0;

            while ( ( bytesRead = reader.read( buffer ) ) > 0 ) {
                writer.write( buffer, 0, bytesRead );
                buffer = new byte[153600];
            }

            writer.close();
            reader.close();
            System.out.println( "Download Completed" );

        } catch ( MalformedURLException e ) {
            e.printStackTrace();
        } catch ( IOException e ) {
            e.printStackTrace();
        }

        return pathFileName;
    }

    private void processOmimFiles( String writeFolder, HashMap<String, HashSet<String>> omimIdToPhenotypeMapping,
            String morbidmap, String mim2gene ) throws IOException {

        // special keywords to exclude on search with annotator (manual mapping)
        ArrayList<String> wordsToExclude = parseFileFindExcludeTerms();
        // special resultsToIgnore
        HashSet<String> resultsToIgnore = parseResultsToIgnore();
        // omim description we want to ignore
        HashSet<String> descriptionToIgnore = parseDescriptionToIgnore();

        // mapping from OMIM description to phenotype (manual mapping)
        HashMap<String, Collection<String>> omimIdToPhenotype = parseFileOmimDescriptionToPhenotype();
        // mapping find using mim2gene file, Omim id ---> Gene NCBI
        HashMap<String, String> omimIdToGeneNCBI = parseFileOmimIdToGeneNCBI( mim2gene );

        // create the 4 possible outputFiles
        BufferedWriter outFinalResults = new BufferedWriter( new FileWriter( writeFolder + "/finalResults.tsv" ) );
        BufferedWriter outMappinpFound = new BufferedWriter( new FileWriter( writeFolder + "/mappingFound.tsv" ) );
        BufferedWriter outNotFound = new BufferedWriter( new FileWriter( writeFolder + "/notFound.tsv" ) );
        TreeSet<String> outMappingFoundBuffer = new TreeSet<String>();
        TreeSet<String> outNotFoundBuffer = new TreeSet<String>();

        String line = null;

        BufferedReader br = new BufferedReader( new FileReader( morbidmap ) );

        int v = 0;

        Collection<Long> ontologiesToUse = new HashSet<Long>();
        ontologiesToUse.add( 1009l );
        ontologiesToUse.add( 1125l );
        AnnotatorClient anoClient = new AnnotatorClient( ontologiesToUse );

        // parse the last Omim file and decide what outfile file each result should be put
        while ( ( line = br.readLine() ) != null ) {

            String[] tokens = line.split( "\\|" );

            int pos = tokens[0].lastIndexOf( "," );

            // if there is a database link
            if ( pos != -1 ) {

                Collection<String> phenotypesUri = new HashSet<String>();
                // this is how we find the result
                String conditionUsed = "";

                // OMIM description find in file
                String description = tokens[0].substring( 0, pos ).trim();

                // evidence code we will use
                String evidenceCode = "TAS";
                // database Link
                String omimId = tokens[0].substring( pos + 1, tokens[0].length() ).trim().split( " " )[0];

                String omimGeneId = tokens[2];

                // search terms that will be used by the annotator
                String searchTerm = removeSymbol( description );
                String searchTermWithOutKeywords = removeSpecificKeywords( searchTerm, wordsToExclude );
                String ncbiGeneId = omimIdToGeneNCBI.get( omimGeneId );

                // is the omimGeneId found in the other file
                if ( ncbiGeneId != null ) {

                    String geneSymbol = this.geneService.findByNCBIId( new Integer( ncbiGeneId ) ).getOfficialSymbol();

                    // if there is no databaselink we cannot do anything with this line (happens often)
                    if ( !isInteger( omimId ) || Integer.parseInt( omimId ) < 100 ) {
                        continue;
                    }

                    v++;
                    System.out.println( "teating line: " + v );

                    // Case 0: we want to exclude this omim descriptipn
                    if ( descriptionToIgnore.contains( description ) ) {
                        conditionUsed = "Case 0: Ignored Descripton";
                    }

                    // Case 1: lets use the Omim id to find the mapping phenotype, it cannot be obsolete
                    if ( omimIdToPhenotypeMapping.get( omimId ) != null ) {
                        phenotypesUri = omimIdToPhenotypeMapping.get( omimId );
                        conditionUsed = "Case 1: Found with OMIM ID";
                    }

                    // Case 2: use the static manual annotation file
                    else if ( omimIdToPhenotype.get( omimId ) != null ) {
                        phenotypesUri = omimIdToPhenotype.get( omimId );
                        conditionUsed = "Case 2: Found with Description, Manual Mapping";
                    }

                    // lets use the annotator to see if we find it there
                    else {

                        Collection<AnnotatorResponse> ontologyTermsNormal = removeNotExistAndObsolete( anoClient
                                .findTerm( searchTerm ) );
                        Collection<AnnotatorResponse> ontologyTermsWithOutKeywords = new HashSet<AnnotatorResponse>();

                        AnnotatorResponse annotatorResponseFirstNormal = null;

                        if ( !ontologyTermsNormal.isEmpty() ) {
                            annotatorResponseFirstNormal = ontologyTermsNormal.iterator().next();
                        }

                        if ( annotatorResponseFirstNormal != null
                                && this.ontologyService.getTerm( annotatorResponseFirstNormal.getValueUri() ) != null
                                && !this.ontologyService.getTerm( annotatorResponseFirstNormal.getValueUri() )
                                        .isTermObsolete() ) {

                            if ( annotatorResponseFirstNormal.getOntologyUsed().equalsIgnoreCase( "DOID" ) ) {

                                if ( annotatorResponseFirstNormal.isExactMatch() ) {
                                    phenotypesUri.add( annotatorResponseFirstNormal.getValueUri() );
                                    conditionUsed = "Case 4a: Found Exact With Disease Annotator";

                                } else if ( annotatorResponseFirstNormal.isSynonym() ) {
                                    phenotypesUri.add( annotatorResponseFirstNormal.getValueUri() );
                                    conditionUsed = "Case 5a: Found Synonym With Disease Annotator Synonym";
                                }
                            } else if ( annotatorResponseFirstNormal.getOntologyUsed().equalsIgnoreCase( "HP" ) ) {

                                if ( annotatorResponseFirstNormal.isExactMatch() ) {
                                    phenotypesUri.add( annotatorResponseFirstNormal.getValueUri() );
                                    conditionUsed = "Case 6a: Found Exact With HP Annotator";

                                } else if ( annotatorResponseFirstNormal.isSynonym() ) {
                                    phenotypesUri.add( annotatorResponseFirstNormal.getValueUri() );
                                    conditionUsed = "Case 7a: Found Synonym With HP Annotator Synonym";
                                }
                            }
                        }

                        if ( conditionUsed.isEmpty() ) {

                            ontologyTermsWithOutKeywords = removeNotExistAndObsolete( anoClient
                                    .findTerm( searchTermWithOutKeywords ) );

                            AnnotatorResponse annotatorResponsFirst = null;

                            if ( !ontologyTermsWithOutKeywords.isEmpty() ) {
                                annotatorResponsFirst = ontologyTermsWithOutKeywords.iterator().next();
                            }

                            if ( annotatorResponsFirst != null
                                    && this.ontologyService.getTerm( annotatorResponsFirst.getValueUri() ) != null
                                    && !this.ontologyService.getTerm( annotatorResponsFirst.getValueUri() )
                                            .isTermObsolete() ) {

                                if ( annotatorResponsFirst.getOntologyUsed().equalsIgnoreCase( "DOID" ) ) {

                                    if ( annotatorResponsFirst.isExactMatch() ) {
                                        phenotypesUri.add( annotatorResponsFirst.getValueUri() );
                                        conditionUsed = "Case 4b: Found Exact With Disease Annotator (keywords taken out)";

                                    } else if ( annotatorResponsFirst.isSynonym() ) {
                                        phenotypesUri.add( annotatorResponsFirst.getValueUri() );
                                        conditionUsed = "Case 5b: Found Synonym With Disease Annotator Synonym (keywords taken out)";
                                    }
                                } else if ( annotatorResponsFirst.getOntologyUsed().equalsIgnoreCase( "HP" ) ) {

                                    if ( annotatorResponsFirst.isExactMatch() ) {
                                        phenotypesUri.add( annotatorResponsFirst.getValueUri() );
                                        conditionUsed = "Case 6b: Found Exact With HP Annotator (without keyword)";

                                    } else if ( annotatorResponsFirst.isSynonym() ) {
                                        phenotypesUri.add( annotatorResponsFirst.getValueUri() );
                                        conditionUsed = "Case 7b: Found Synonym With HP Annotator Synonym (keywords taken out)";
                                    }
                                }
                            }
                        }

                        if ( conditionUsed.isEmpty() ) {

                            if ( ontologyTermsNormal.size() > 0 || ontologyTermsWithOutKeywords.size() > 0 ) {

                                HashSet<String> mappingFoundNotExactValueUri = new HashSet<String>();
                                HashSet<String> mappingFoundNotExactValue = new HashSet<String>();

                                for ( AnnotatorResponse annotatorResponse : ontologyTermsNormal ) {
                                    mappingFoundNotExactValue.add( annotatorResponse.getValue() );
                                    mappingFoundNotExactValueUri.add( annotatorResponse.getValueUri() );
                                }
                                for ( AnnotatorResponse annotatorResponse : ontologyTermsWithOutKeywords ) {
                                    mappingFoundNotExactValue.add( annotatorResponse.getValue() );
                                    mappingFoundNotExactValueUri.add( annotatorResponse.getValueUri() );
                                }

                                HashSet<String> mappingFoundNotExactValue2 = new HashSet<String>();
                                String resultsIgnored = "";

                                // take out the excluded results
                                for ( String phenoValueUri : mappingFoundNotExactValueUri ) {

                                    if ( resultsToIgnore.contains( phenoValueUri ) ) {

                                        resultsIgnored = resultsIgnored + phenoValueUri + "; ";
                                    } else {
                                        mappingFoundNotExactValue2.add( phenoValueUri );

                                    }
                                }

                                if ( mappingFoundNotExactValue2.isEmpty() ) {
                                    conditionUsed = "Case 10: Exclude Results\t";
                                } else {

                                    conditionUsed = "Case 8: Found Mappings, No Match Detected";

                                    for ( String mappingF : mappingFoundNotExactValueUri ) {
                                        phenotypesUri.add( mappingF );
                                    }
                                }

                            } else {
                                conditionUsed = "Case 9: No Mapping Found";
                            }
                        }

                    }

                    // ingored
                    if ( conditionUsed.indexOf( "Case 0:" ) != -1 ) {
                        outNotFoundBuffer.add( description + "\t" + conditionUsed + "\n" );
                    }

                    // found
                    else if ( conditionUsed.indexOf( "Case 1:" ) != -1 || conditionUsed.indexOf( "Case 2:" ) != -1 ) {

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

                    else if ( conditionUsed.indexOf( "Case 4" ) != -1 || conditionUsed.indexOf( "Case 5" ) != -1
                            || conditionUsed.indexOf( "Case 6" ) != -1 || conditionUsed.indexOf( "Case 7" ) != -1
                            || conditionUsed.indexOf( "Case 8:" ) != -1 || conditionUsed.indexOf( "Case 3:" ) != -1 ) {

                        String phenotypeValueUri = "";
                        String phenotypeValue = "";

                        for ( String valueUri : phenotypesUri ) {
                            OntologyTerm o = this.diseaseOntologyService.getTerm( valueUri );
                            if ( o == null ) {
                                o = this.humanPhenotypeOntologyService.getTerm( valueUri );
                            }

                            // ontologyTerm can never be null or obsolete we checked before

                            phenotypeValueUri = phenotypeValueUri + o.getUri() + ";";
                            phenotypeValue = phenotypeValue + o.getLabel() + ";";

                        }

                        if ( !phenotypeValueUri.isEmpty() ) {
                            String lineToWrite = omimId + "\t" + phenotypeValueUri + "\t" + phenotypeValue + "\t"
                                    + description + "\t" + conditionUsed + "\n";

                            outMappingFoundBuffer.add( lineToWrite );
                        }

                    }

                    else if ( conditionUsed.indexOf( "Case 10:" ) != -1 ) {

                        String lineToWrite = description + "\t" + conditionUsed + "\n";

                        outNotFoundBuffer.add( lineToWrite );
                    } else {
                        String lineToWrite = description + "\t" + conditionUsed + "\n";

                        outNotFoundBuffer.add( lineToWrite );
                    }
                }
            }
        }

        if ( !errorMessages.isEmpty() ) {

            System.out.println( "here is the error messages :\n" );

            for ( String err : errorMessages ) {

                System.err.println( err );
            }
        }

        for ( String lineMappinpFound : outMappingFoundBuffer ) {
            outMappinpFound.write( lineMappinpFound );
        }
        for ( String lineNotFound : outNotFoundBuffer ) {
            outNotFound.write( lineNotFound );
        }

        // case 0 and 1 at the end
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
                    + databaseLink + "\t" + allValueUri + "\t" + phenotypeValue + "\t" + conditionUsed + "\n";

            outFinalResults.write( evidenceLine );
        }

        // close the used files
        outFinalResults.close();
        outMappinpFound.close();
        outNotFound.close();
    }

    private String removeSmallNumberAndTxt( String txt ) {

        String txtWithoutSimpLetters = "";

        String[] tokens = txt.split( " " );

        for ( int i = 0; i < tokens.length; i++ ) {

            String token = tokens[i];

            // last token
            if ( tokens.length - 1 == i ) {

                if ( token.length() < 4 ) {
                    // contains a number
                    if ( token.matches( ".*\\d.*" ) ) {
                        token = "";
                    }
                }
                txtWithoutSimpLetters = txtWithoutSimpLetters + token;
            } else {
                txtWithoutSimpLetters = txtWithoutSimpLetters + token + " ";
            }
        }
        return txtWithoutSimpLetters.trim();

    }

    private String removeSpecificKeywords( String txt, ArrayList<String> wordsToExclude ) {

        String txtWithExcludeDigitWords = removeEndDigitWords( txt );

        String newTxt = txtWithExcludeDigitWords;

        for ( String word : wordsToExclude ) {
            int indexOfWordToExclude = newTxt.indexOf( word );
            int wordLength = word.length();

            if ( indexOfWordToExclude != -1 ) {
                newTxt = newTxt.substring( 0, indexOfWordToExclude )
                        + newTxt.substring( indexOfWordToExclude + wordLength, newTxt.length() );
            }
        }

        String[] tokens = newTxt.split( "," );
        newTxt = "";
        for ( String token : tokens ) {
            token = token.trim();
            if ( token.length() > 0 ) {
                newTxt = newTxt + token + ",";
            }
        }
        newTxt = newTxt.substring( 0, newTxt.length() - 1 );

        tokens = newTxt.split( "," );

        // reverse them if 2 tokens,
        if ( tokens.length == 2 ) {
            newTxt = tokens[1] + "," + tokens[0];
        }

        // replace , with " "
        return newTxt.replaceAll( ",", " " );
    }

    private String removeEndDigitWords( String txt ) {
        String finalTxt = "";
        String[] termFoundInFile = txt.split( "," );
        int j = 0;

        for ( String term : termFoundInFile ) {

            String[] txtFoundInTerms = term.split( " " );

            for ( int i = 0; i < txtFoundInTerms.length; i++ ) {

                String txtFoundInTerm = txtFoundInTerms[i];

                // last token
                if ( txtFoundInTerms.length - 1 == i ) {

                    if ( txtFoundInTerm.length() < 4 ) {
                        // contains a number
                        if ( txtFoundInTerm.matches( ".*\\d.*" ) ) {
                            txtFoundInTerm = "";
                        }
                    }
                }
                finalTxt = finalTxt + txtFoundInTerm.trim();

                if ( i != txtFoundInTerms.length - 1 ) {
                    finalTxt = finalTxt + " ";
                }
            }
            if ( j != termFoundInFile.length - 1 ) {
                finalTxt = finalTxt.trim() + ",";
            }
            j++;
        }
        return finalTxt;
    }

    private boolean isInteger( String input ) {
        try {
            Integer.parseInt( input );
            return true;
        } catch ( Exception e ) {
            return false;
        }
    }

    private String removeCha( String txt ) {

        String newTxt = txt.replaceAll( "\"", "" );
        return newTxt.trim();
    }

    private ArrayList<String> parseFileFindExcludeTerms() throws IOException {

        ArrayList<String> wordsToExclude = new ArrayList<String>();

        BufferedReader br = new BufferedReader( new FileReader( EXCLUDE_KEYWORDS_OMIM ) );

        String line = "";

        while ( ( line = br.readLine() ) != null ) {

            line = removeSmallNumberAndTxt( line );

            wordsToExclude.add( removeCha( line ) );
        }
        MyComparator myComparator = new MyComparator();

        java.util.Collections.sort( wordsToExclude, myComparator );

        return wordsToExclude;
    }

    private HashSet<String> parseResultsToIgnore() throws IOException {

        HashSet<String> resultsToIgnore = new HashSet<String>();

        BufferedReader br = new BufferedReader( new FileReader( RESULTS_TO_IGNORE ) );

        String line = "";

        while ( ( line = br.readLine() ) != null ) {

            String[] tokens = line.split( "\t" );

            resultsToIgnore.add( removeCha( tokens[0] ) );
        }

        return resultsToIgnore;
    }

    private HashSet<String> parseDescriptionToIgnore() throws IOException {

        HashSet<String> descriptionToIgnore = new HashSet<String>();

        BufferedReader br = new BufferedReader( new FileReader( DESCRIPTION_TO_IGNORE ) );

        String line = "";

        while ( ( line = br.readLine() ) != null ) {

            line = removeSmallNumberAndTxt( line );

            descriptionToIgnore.add( removeCha( line ).trim() );
        }

        return descriptionToIgnore;
    }

    private HashMap<String, Collection<String>> parseFileOmimDescriptionToPhenotype() throws IOException {

        HashMap<String, Collection<String>> omimIdToPhenotype = new HashMap<String, Collection<String>>();

        BufferedReader br = new BufferedReader( new FileReader( MANUAL_MAPPING_OMIM ) );

        String line = br.readLine();

        String[] tokens = null;

        while ( ( line = br.readLine() ) != null ) {

            Collection<String> col = new HashSet<String>();

            tokens = line.split( "\t" );

            String omimIdStaticFile = removeCha( tokens[0] );
            String valueUriStaticFile = removeCha( tokens[1] );

            if ( !isObsoleteOrNotExist( valueUriStaticFile ) ) {

                if ( omimIdToPhenotype.get( omimIdStaticFile ) == null ) {

                    col.add( valueUriStaticFile );
                } else {
                    col = omimIdToPhenotype.get( omimIdStaticFile );
                    col.add( valueUriStaticFile );
                }
                omimIdToPhenotype.put( omimIdStaticFile, col );
            } else {

                errorMessages.add( "MANUAL MAPPING FILE TERM OBSOLETE OR NOT EXISTANT: " + line );

            }
        }

        return omimIdToPhenotype;
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
        return omimIdToGeneNCBI;

    }

    // order strings by length
    public class MyComparator implements java.util.Comparator<String> {
        @Override
        public int compare( String s1, String s2 ) {
            if ( s1.length() > s2.length() ) {

                return -1;
            }
            return 1;
        }
    }

    private String removeSymbol( String txt ) {

        String newTxt = txt.replaceAll( "\\{", "" );
        newTxt = newTxt.replaceAll( "\\}", "" );
        newTxt = newTxt.replaceAll( "\\[", "" );
        newTxt = newTxt.replaceAll( "\\]", "" );
        newTxt = newTxt.replaceAll( "\\?", "" );

        return newTxt;
    }

    private boolean isObsoleteOrNotExist( String valueUri ) {

        OntologyTerm o = this.diseaseOntologyService.getTerm( valueUri );
        if ( o == null ) {
            o = this.humanPhenotypeOntologyService.getTerm( valueUri );
        }

        if ( o == null || o.isTermObsolete() ) {

            return true;
        }

        return false;
    }

    private Collection<AnnotatorResponse> removeNotExistAndObsolete( Collection<AnnotatorResponse> annotatorResponses ) {

        Collection<AnnotatorResponse> annotatorResponseWithNoObsolete = new TreeSet<AnnotatorResponse>();

        for ( AnnotatorResponse annotatorResponse : annotatorResponses ) {

            if ( !isObsoleteOrNotExist( annotatorResponse.getValueUri() ) ) {
                annotatorResponseWithNoObsolete.add( annotatorResponse );
            }
        }
        return annotatorResponseWithNoObsolete;
    }

}