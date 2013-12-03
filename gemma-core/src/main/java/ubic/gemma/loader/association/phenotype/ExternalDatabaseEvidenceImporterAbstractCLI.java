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
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeSet;

import ubic.basecode.ontology.model.OntologyTerm;
import ubic.basecode.ontology.ncbo.AnnotatorClient;
import ubic.basecode.ontology.ncbo.AnnotatorResponse;
import ubic.basecode.ontology.providers.DiseaseOntologyService;
import ubic.basecode.ontology.providers.HumanPhenotypeOntologyService;
import ubic.basecode.ontology.providers.MedicOntologyService;
import ubic.gemma.genome.gene.service.GeneService;
import ubic.gemma.genome.taxon.service.TaxonService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.ontology.OntologyService;
import ubic.gemma.util.AbstractCLIContextCLI;
import ubic.gemma.util.Settings;

/**
 * TODO Parent of all Importers
 * 
 * @author nicolas
 * @version $Id$
 */
public abstract class ExternalDatabaseEvidenceImporterAbstractCLI extends AbstractCLIContextCLI {

    protected AnnotatorClient anoClient = null;

    // this is where the results and files downloaded are put
    protected static final String WRITE_FOLDER = Settings.getString( "gemma.appdata.home" );
    // this are where the static resources are kept, like manual mapping files and others
    protected static final String RESOURCE_PATH = File.separator + "neurocarta" + File.separator
            + "externalDatabaseImporter" + File.separator;

    // terms to exclude when we search for a phenotype using the annotator (stop words)
    protected static final String EXCLUDE_KEYWORDS = RESOURCE_PATH + "KeyWordsToExclude.tsv";
    protected ArrayList<String> wordsToExclude = new ArrayList<String>();

    // results we exclude, we know those results are not good
    protected static final String RESULTS_TO_IGNORE = RESOURCE_PATH + "ResultsToIgnore.tsv";
    protected HashSet<String> resultsToIgnore = new HashSet<String>();

    // description we want to ignore, when we find those description, we know we are not interested in them
    public static final String DESCRIPTION_TO_IGNORE = RESOURCE_PATH + "DescriptionToIgnore.tsv";
    protected HashSet<String> descriptionToIgnore = new HashSet<String>();

    // manual description file, MeshID, OmimID or txt description to a valueUri
    public static final String MANUAL_MAPPING = RESOURCE_PATH + "ManualDescriptionMapping.tsv";
    private HashMap<String, HashSet<String>> manualDescriptionToValuesUriMapping = new HashMap<String, HashSet<String>>();

    // populated using the disease ontology file
    protected HashMap<String, HashSet<String>> diseaseFileMappingFound = new HashMap<String, HashSet<String>>();

    // ********************************************************************************
    // the services that will be needed
    protected DiseaseOntologyService diseaseOntologyService = null;
    protected HumanPhenotypeOntologyService humanPhenotypeOntologyService = null;
    protected OntologyService ontologyService = null;
    protected GeneService geneService = null;
    protected TaxonService taxonService = null;
    protected MedicOntologyService medicOntologyService = null;

    // *********************************************************************************************
    // the disease ontology file, we are interested in the MESH and OMIM Id in it ( we dont have this information in
    // gemma)
    protected static final String DISEASE_ONT_PATH = "http://rest.bioontology.org/bioportal/virtual/download/";
    protected static final String DISEASE_ONT_FILE = "1009?apikey=68835db8-b142-4c7d-9509-3c843849ad67";

    protected TreeSet<String> outMappingFoundBuffer = new TreeSet<String>();
    protected TreeSet<String> outNotFoundBuffer = new TreeSet<String>();
    protected String valueUriForCondition = "";
    protected String valueForCondition = "";

    protected String writeFolder = null;

    // keep all errors and show unique ones at the end of the program
    protected TreeSet<String> errorMessages = new TreeSet<String>();
    // the 3 possible out files
    protected BufferedWriter outFinalResults = null;
    protected BufferedWriter outMappingFound = null;
    protected BufferedWriter outNotFound = null;

    // for a search term we always get the answer, no reason to call the annotator again if it gave us the answer for
    // that request before
    private HashMap<String, Collection<AnnotatorResponse>> cacheAnswerFromAnnotator = new HashMap<String, Collection<AnnotatorResponse>>();

    private HashMap<Integer, String> geneToSymbol = new HashMap<Integer, String>();

    // load all needed services
    protected synchronized void loadServices( String[] args ) throws Exception {

        // this gets the context, so we can access beans
        Exception err = processCommandLine( "ExternalDatabaseEvidenceImporterAbstractCLI", args );
        if ( err != null ) throw err;

        this.ontologyService = this.getBean( OntologyService.class );
        this.diseaseOntologyService = this.ontologyService.getDiseaseOntologyService();
        this.humanPhenotypeOntologyService = this.ontologyService.getHumanPhenotypeOntologyService();
        this.geneService = this.getBean( GeneService.class );
        this.taxonService = this.getBean( TaxonService.class );

        while ( this.diseaseOntologyService.isOntologyLoaded() == false ) {
            wait( 3000 );
            log.info( "waiting for the Disease Ontology to load" );
        }

        while ( this.humanPhenotypeOntologyService.isOntologyLoaded() == false ) {
            wait( 3000 );
            log.info( "waiting for the HP Ontology to load" );
        }

        this.medicOntologyService = new MedicOntologyService();
    }

    // the loadServices is in the constructor, we always need those
    public ExternalDatabaseEvidenceImporterAbstractCLI( String[] args ) throws Exception {
        super();

        // load all needed services
        loadServices( args );
        // results to exclude using the annoator
        parseFileFindExcludeTerms();
        // results returned by the annotator to ignore
        parseResultsToIgnore();
        // parse the manual mapping file
        parseManualMappingFile();
        // description we know we are not intereted in the result
        parseDescriptionToIgnore();
        // set up the annotator
        initNcboAnnotatorClient();
    }

    private void initNcboAnnotatorClient() {
        Collection<Long> ontologiesToUse = new HashSet<Long>();
        ontologiesToUse.add( AnnotatorClient.DOID_ONTOLOGY );
        ontologiesToUse.add( AnnotatorClient.HP_ONTOLOGY );
        anoClient = new AnnotatorClient( ontologiesToUse );
    }

    @Override
    protected Exception doWork( String[] args ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected void buildOptions() {
        // TODO Auto-generated method stub
    }

    // creates the folder where the output files will be put, with today date
    protected void createWriteFolderWithDate( String externalDatabaseUse ) throws Exception {

        // where to put the final results
        writeFolder = WRITE_FOLDER + File.separator + externalDatabaseUse + "_" + getTodayDate();

        File folder = new File( writeFolder );

        if ( !folder.mkdir() ) {
            throw new Exception( "having trouble to create a folder" );
        }

        initOutputfiles();
    }

    // creates the folder where the output files will be put, use this one if file is too big
    protected String createWriteFolderIfDoesntExist( String externalDatabaseUse ) throws Exception {

        // where to put the final results
        writeFolder = WRITE_FOLDER + File.separator + externalDatabaseUse;

        File folder = new File( writeFolder );
        folder.mkdir();

        if ( !folder.exists() ) {
            throw new Exception( "having trouble to create a folder" );
        }

        initOutputfiles();

        return writeFolder;
    }

    // all imported can have up to 3 outputFiles, that are the results
    private void initOutputfiles() throws IOException {
        // 1- sure results to be imported into Neurocarta
        outFinalResults = new BufferedWriter( new FileWriter( writeFolder + "/finalResults.tsv" ) );
        // 2- results found with the annotator need to be verify and if ok moved to a manual annotation file
        outMappingFound = new BufferedWriter( new FileWriter( writeFolder + "/mappingFound.tsv" ) );
        // 3- no results found with id, manual mapping and annotator
        outNotFound = new BufferedWriter( new FileWriter( writeFolder + "/notFound.tsv" ) );
    }

    private String getTodayDate() {
        DateFormat dateFormat = new SimpleDateFormat( "yyyy-MM-dd_HH:mm" );
        Calendar cal = Calendar.getInstance();
        return dateFormat.format( cal.getTime() );
    }

    /**
     * Goes on the specified urlPath and download the file to place it into the writeFolder
     */
    protected String downloadFileFromWeb( String pathName, String fileUrlName ) {

        String fileName = fileUrlName;

        String fullPathToDownload = pathName + fileUrlName;

        // here we change the name of this specific file so itcan be human readable
        if ( fileName.equalsIgnoreCase( DISEASE_ONT_FILE ) ) {
            fileName = "diseaseOntology.txt";
        }

        String pathFileName = writeFolder + File.separator + fileName;
        log.info( "Trying to download : " + fullPathToDownload );

        URL url;
        try {
            url = new URL( fullPathToDownload );
            url.openConnection();
        } catch ( IOException e1 ) {
            throw new RuntimeException( e1 );
        }

        try (InputStream reader = url.openStream(); FileOutputStream writer = new FileOutputStream( pathFileName );) {

            byte[] buffer = new byte[153600];
            int bytesRead = 0;

            while ( ( bytesRead = reader.read( buffer ) ) > 0 ) {
                writer.write( buffer, 0, bytesRead );
                buffer = new byte[153600];
            }

            writer.close();
            reader.close();
            log.info( "Download Completed" );

        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }

        return pathFileName;
    }

    // is the valueUri existing or obsolete ?
    protected boolean isObsoleteOrNotExist( String valueUri ) {

        OntologyTerm o = this.diseaseOntologyService.getTerm( valueUri );
        if ( o == null ) {
            o = this.humanPhenotypeOntologyService.getTerm( valueUri );
        }

        if ( o == null || o.isTermObsolete() ) {
            return true;
        }

        return false;
    }

    // is the valueUri existing or obsolete ?
    protected OntologyTerm findOntologyTermExistAndNotObsolote( String valueUri ) {

        OntologyTerm o = this.diseaseOntologyService.getTerm( valueUri );
        if ( o == null ) {
            o = this.humanPhenotypeOntologyService.getTerm( valueUri );
        }

        if ( o == null || o.isTermObsolete() ) {
            return null;
        }

        return o;
    }

    // checks if a value uri usualy found with the annotator exists or obsolete, filters the results
    protected Collection<AnnotatorResponse> removeNotExistAndObsolete( Collection<AnnotatorResponse> annotatorResponses ) {

        Collection<AnnotatorResponse> annotatorResponseWithNoObsolete = new TreeSet<AnnotatorResponse>();

        for ( AnnotatorResponse annotatorResponse : annotatorResponses ) {

            if ( !isObsoleteOrNotExist( annotatorResponse.getValueUri() ) ) {
                annotatorResponseWithNoObsolete.add( annotatorResponse );
            }
        }
        return annotatorResponseWithNoObsolete;
    }

    /**
     * parse the disease ontology file to create the structure needed (omimIdToPhenotypeMapping and
     * meshIdToPhenotypeMapping)
     **/
    protected void findOmimAndMeshMappingUsingOntologyFile() throws IOException {

        // download the disease Ontology File
        String diseaseOntologyFile = downloadFileFromWeb( DISEASE_ONT_PATH, DISEASE_ONT_FILE );

        HashSet<String> omimIds = new HashSet<String>();
        HashSet<String> meshIds = new HashSet<String>();
        String valueUri = null;

        try (BufferedReader br = new BufferedReader( new FileReader( diseaseOntologyFile ) );) {

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
                    meshIds = new HashSet<String>();
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
                    } else if ( line.indexOf( "xref: MSH" ) != -1 ) {
                        tokens = line.split( ":" );
                        meshIds.add( tokens[2].trim() );
                    }

                    // end of a term
                    else if ( line.equalsIgnoreCase( "" ) ) {

                        foundTerm = false;

                        for ( String omimId : omimIds ) {

                            HashSet<String> h = new HashSet<String>();
                            String key = "OMIM:" + omimId;

                            if ( diseaseFileMappingFound.get( key ) == null ) {
                                if ( !isObsoleteOrNotExist( valueUri ) ) {
                                    h.add( valueUri );
                                }
                            } else {
                                h = diseaseFileMappingFound.get( key );
                                if ( !isObsoleteOrNotExist( valueUri ) ) {
                                    h.add( valueUri );
                                }
                            }
                            diseaseFileMappingFound.put( key, h );
                        }

                        for ( String meshId : meshIds ) {

                            String key = "MESH:" + meshId;

                            HashSet<String> h = new HashSet<String>();

                            if ( diseaseFileMappingFound.get( key ) == null ) {
                                if ( !isObsoleteOrNotExist( valueUri ) ) {
                                    h.add( valueUri );
                                }
                            } else {
                                h = diseaseFileMappingFound.get( key );
                                if ( !isObsoleteOrNotExist( valueUri ) ) {
                                    h.add( valueUri );
                                }
                            }
                            diseaseFileMappingFound.put( key, h );
                        }
                    }
                }
            }
        }
    }

    // parse file and returns a collection of stop words to exclude
    protected ArrayList<String> parseFileFindExcludeTerms() throws IOException {

        BufferedReader br = new BufferedReader( new InputStreamReader(
                ExternalDatabaseEvidenceImporterAbstractCLI.class.getResourceAsStream( EXCLUDE_KEYWORDS ) ) );

        String line = "";

        while ( ( line = br.readLine() ) != null ) {

            line = removeSmallNumberAndTxt( line );

            wordsToExclude.add( removeCha( line ).toLowerCase() );
        }
        MyComparator myComparator = new MyComparator();

        java.util.Collections.sort( wordsToExclude, myComparator );

        return wordsToExclude;
    }

    // special rules used to format the search term, when using a modified search in the annotator
    protected String removeSmallNumberAndTxt( String txt ) {

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

    // if a excel file was used values sometime save as "value", always take out the "
    protected String removeCha( String txt ) {

        String newTxt = txt.replaceAll( "\"", "" );
        return newTxt.trim();
    }

    protected String findValueUriWithDiseaseId( String diseaseId ) {

        Collection<String> valuesUri = diseaseFileMappingFound.get( diseaseId );
        String allValueUri = "";

        if ( valuesUri != null && !valuesUri.isEmpty() ) {

            for ( String valueUri : valuesUri ) {
                allValueUri = allValueUri + valueUri + ";";
            }
        }

        return allValueUri;
    }

    private Collection<OntologyTerm> findOntologyTermsUriWithDiseaseId( String diseaseId ) {

        Collection<OntologyTerm> terms = new HashSet<OntologyTerm>();
        Collection<String> valuesUri = diseaseFileMappingFound.get( diseaseId );

        if ( valuesUri != null && !valuesUri.isEmpty() ) {

            for ( String valueUri : valuesUri ) {

                OntologyTerm on = this.ontologyService.getTerm( valueUri );

                if ( on != null ) {

                    terms.add( on );
                }
            }
        }

        return terms;
    }

    // special rule used to format the search term
    protected String removeSpecificKeywords( String txt ) {

        String txtWithExcludeDigitWords = removeEndDigitWords( txt );

        if ( txtWithExcludeDigitWords.isEmpty() ) {
            txtWithExcludeDigitWords = txt;
        }

        String newTxt = txtWithExcludeDigitWords;

        for ( String word : wordsToExclude ) {
            String newTxtLowerCase = newTxt.toLowerCase();
            int indexOfWordToExclude = newTxtLowerCase.indexOf( word );
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
        if ( !newTxt.isEmpty() ) {
            newTxt = newTxt.substring( 0, newTxt.length() - 1 );
        }

        tokens = newTxt.split( "," );

        // reverse them if 2 tokens,
        if ( tokens.length == 2 ) {
            newTxt = tokens[1] + "," + tokens[0];
        }

        // replace , with " "
        return newTxt.replaceAll( ",", " " );
    }

    // special rule used to format the search term
    protected String removeEndDigitWords( String txt ) {
        String finalTxt = "";
        String[] termFoundInFile = txt.split( "," );
        int j = 0;

        for ( String term : termFoundInFile ) {

            String[] txtFoundInTerms = term.split( " " );

            for ( int i = 0; i < txtFoundInTerms.length; i++ ) {

                String txtFoundInTerm = txtFoundInTerms[i];

                // last token
                if ( txtFoundInTerms.length - 1 == i ) {

                    if ( txtFoundInTerm.length() < 5 ) {
                        txtFoundInTerm = "";
                        // contains a number
                        // if ( txtFoundInTerm.matches( ".*\\d.*" ) ) {
                        // txtFoundInTerm = "";
                        // }
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

    protected boolean isInteger( String input ) {
        try {
            Integer.parseInt( input );
            return true;
        } catch ( Exception e ) {
            return false;
        }
    }

    // order strings by length, when we remove stop words, we need to remove longest words first
    public class MyComparator implements java.util.Comparator<String> {
        @Override
        public int compare( String s1, String s2 ) {
            if ( s1.length() > s2.length() ) {

                return -1;
            }
            return 1;
        }
    }

    protected String findConditionUsed( Collection<AnnotatorResponse> ontologyTerms, boolean modifiedKeywords ) {

        String conditionUsed = null;

        AnnotatorResponse annotatorResponseFirst = null;

        if ( !ontologyTerms.isEmpty() ) {
            annotatorResponseFirst = ontologyTerms.iterator().next();
        }

        if ( annotatorResponseFirst != null
                && this.ontologyService.getTerm( annotatorResponseFirst.getValueUri() ) != null
                && !this.ontologyService.getTerm( annotatorResponseFirst.getValueUri() ).isTermObsolete() ) {

            valueUriForCondition = annotatorResponseFirst.getValueUri();
            valueForCondition = annotatorResponseFirst.getValue();

            if ( annotatorResponseFirst.getOntologyUsed().equalsIgnoreCase( "Human Disease Ontology" ) ) {

                if ( annotatorResponseFirst.isExactMatch() ) {

                    conditionUsed = "Case 4: Found Exact With Disease Annotator";

                } else if ( annotatorResponseFirst.isSynonym() ) {
                    conditionUsed = "Case 5: Found Synonym With Disease Annotator Synonym";
                }
            } else if ( annotatorResponseFirst.getOntologyUsed().equalsIgnoreCase( "Human Phenotype Ontology" ) ) {

                if ( annotatorResponseFirst.isExactMatch() ) {

                    conditionUsed = "Case 6: Found Exact With HP Annotator";

                } else if ( annotatorResponseFirst.isSynonym() ) {

                    conditionUsed = "Case 7: Found Synonym With HP Annotator Synonym";
                }
            }
        }

        if ( conditionUsed != null && modifiedKeywords ) {
            conditionUsed = conditionUsed + " (keywords taken out)";
        }

        return conditionUsed;

    }

    // many importers can have different headers, this is 1 possible case
    protected void writeOutputFileHeaders1() throws IOException {

        // headers of the final out file to write
        outFinalResults
                .write( "GeneSymbol\tGeneId\tPrimaryPubMeds\tEvidenceCode\tComments\tExternalDatabase\tDatabaseLink\tPhenotypes\n" );
    }

    // many importers can have different headers, this is 1 possible case
    protected void writeOutputFileHeaders2() throws IOException {

        // headers of the final file
        outFinalResults
                .write( "GeneSymbol\tTaxon\tPrimaryPubMeds\tEvidenceCode\tComments\tExternalDatabase\tDatabaseLink\tPhenotypes\n" );
    }

    // many importers can have different headers, this is 1 possible case
    protected void writeOutputFileHeaders3() throws IOException {
        outFinalResults
                .write( "GeneSymbol\tPrimaryPubMeds\tEvidenceCode\tComments\tScore\tStrength\tScoreType\tExternalDatabase\tDatabaseLink\tPhenotypes\tTaxon\n" );
    }

    // many importers can have different headers, this is 1 possible case
    protected void writeOutputFileHeaders4() throws IOException {
        // headers of the final file
        outFinalResults
                .write( "GeneSymbol\tGeneId\tEvidenceCode\tComments\tDatabaseLink\tPhenotypes\tExtraInfo\tExtraInfo\tExternalDatabase\tPrimaryPubMeds\tExtraInfo\n" );
    }

    protected void writeOutputFileHeaders5() throws IOException {
        // headers of the final file
        outFinalResults
                .write( "Phenotypes\tExtraInfo\tGeneSymbol\tGeneId\tPrimaryPubMeds\tComments\tEvidenceCode\tDatabaseLink\tExternalDatabase\n" );
    }

    // write all found in set to files and close files
    // the reason this is done that way is to not have duplicates for 2 files
    protected void writeBuffersAndCloseFiles() throws IOException {

        outMappingFound
                .write( "Identifier (KEY)\tPhenotype valueUri (THE KEY MAP TO THIS)\tPhenotype value\tSearch Term used in annotator\tParent Mesh\tHow we found the mapping\tSource" );

        // write all possible mapping found
        for ( String lineMappinpFound : outMappingFoundBuffer ) {
            outMappingFound.write( lineMappinpFound );
        }

        // write all not found mapping
        for ( String lineNotFound : outNotFoundBuffer ) {
            outNotFound.write( lineNotFound );
        }

        outMappingFound.close();
        outNotFound.close();
        outFinalResults.close();

        if ( !errorMessages.isEmpty() ) {

            log.info( "here are the error messages :\n" );

            for ( String err : errorMessages ) {

                log.error( err );
            }
        }
    }

    protected void parseManualMappingFile() throws IOException {

        BufferedReader br = new BufferedReader( new InputStreamReader(
                RgdDatabaseImporter.class.getResourceAsStream( MANUAL_MAPPING ) ) );

        String line = "";

        // skip first line, the headers
        line = br.readLine();

        // reads the manual file and put the data in a structure
        while ( ( line = br.readLine() ) != null ) {

            HashSet<String> col = new HashSet<String>();

            String[] tokens = line.split( "\t" );

            String termId = tokens[0].trim().toLowerCase();
            String valueUriStaticFile = tokens[1].trim();
            String valueStaticFile = tokens[2].trim();

            OntologyTerm ontologyTerm = findOntologyTermExistAndNotObsolote( valueUriStaticFile );

            if ( ontologyTerm != null ) {

                if ( ontologyTerm.getLabel().equalsIgnoreCase( valueStaticFile ) ) {

                    if ( manualDescriptionToValuesUriMapping.get( termId ) != null ) {
                        col = manualDescriptionToValuesUriMapping.get( termId );
                    }

                    col.add( valueUriStaticFile );

                    manualDescriptionToValuesUriMapping.put( termId, col );

                } else {
                    errorMessages.add( "MANUAL VALUEURI AND VALUE DOESNT MATCH IN FILE: line" + line
                            + "\t What the value supposed to be:" + ontologyTerm.getLabel() );
                }
            } else {
                errorMessages.add( "MANUAL MAPPING FILE TERM OBSOLETE OR NOT EXISTANT: '" + valueUriStaticFile + "' "
                        + " (" + valueStaticFile + ")" );
            }
        }
    }

    // parse file and returns a collection of terms found
    private HashSet<String> parseResultsToIgnore() throws IOException {

        BufferedReader br = new BufferedReader( new InputStreamReader(
                ExternalDatabaseEvidenceImporterAbstractCLI.class.getResourceAsStream( RESULTS_TO_IGNORE ) ) );

        String line = "";

        while ( ( line = br.readLine() ) != null ) {

            String[] tokens = line.split( "\t" );

            resultsToIgnore.add( removeCha( tokens[0] ) );
        }

        return resultsToIgnore;
    }

    protected boolean writeInPossibleMapping( Collection<AnnotatorResponse> annotatorResponses, String identifier,
            String searchTerm, String externalDatabaseName ) {

        // keep all that was found by the annotator
        String valuesUri = "";
        String values = "";

        for ( AnnotatorResponse annotatorResponse : annotatorResponses ) {

            if ( !resultsToIgnore.contains( annotatorResponse.getValueUri() ) ) {

                valuesUri = valuesUri + annotatorResponse.getValueUri() + "; ";
                values = values + annotatorResponse.getValue() + "; ";
            }
        }

        if ( !values.isEmpty() ) {
            String lineToWrite = identifier + "\t" + valuesUri + "\t" + values + "\t" + searchTerm + "\t\t"
                    + "Case 8: Found Mappings, No Match Detected" + "\t" + externalDatabaseName + "\n";
            // multiple mapping detected
            outMappingFoundBuffer.add( lineToWrite );
            return true;
        }
        return false;
    }

    protected void writeInPossibleMappingAndNotFound( String identifier, String searchTerm, String line,
            String externalDatabaseName, boolean useMedicOntology ) {

        String searchTermWithOutKeywords = "";

        if ( descriptionToIgnore.contains( searchTerm ) ) {
            outNotFoundBuffer.add( line + "\n" );
        } else {

            Collection<AnnotatorResponse> ontologyTermsNormal = cacheAnswerFromAnnotator.get( searchTerm );

            if ( ontologyTermsNormal == null ) {
                // search with the annotator and filter result to take out obsolete terms given
                ontologyTermsNormal = removeNotExistAndObsolete( anoClient.findTerm( searchTerm ) );
                // cache results
                cacheAnswerFromAnnotator.put( searchTerm, ontologyTermsNormal );
            }

            Collection<AnnotatorResponse> ontologyTermsWithOutKeywords = new HashSet<AnnotatorResponse>();

            // did we find something ?
            String condition = findConditionUsed( ontologyTermsNormal, false );

            if ( condition == null ) {
                // search again manipulating the search string
                searchTermWithOutKeywords = removeSpecificKeywords( searchTerm );

                ontologyTermsWithOutKeywords = cacheAnswerFromAnnotator.get( searchTermWithOutKeywords );

                if ( ontologyTermsWithOutKeywords == null ) {
                    // search with the modifed keyword
                    ontologyTermsWithOutKeywords = removeNotExistAndObsolete( anoClient
                            .findTerm( searchTermWithOutKeywords ) );
                    // cache results
                    cacheAnswerFromAnnotator.put( searchTermWithOutKeywords, ontologyTermsWithOutKeywords );
                }

                // did we find something ?
                condition = findConditionUsed( ontologyTermsWithOutKeywords, true );
            }

            // if a satisfying condition was found write in down in the mapping found
            if ( condition != null ) {

                String searchTerms = searchTerm;

                if ( !searchTermWithOutKeywords.isEmpty() ) {
                    searchTerms = searchTerms + " (" + searchTermWithOutKeywords + ")";
                }

                String lineToWrite = identifier + "\t" + valueUriForCondition + "\t" + valueForCondition + "\t"
                        + searchTerms + "\t\t" + condition + "\t" + externalDatabaseName + "\n";

                outMappingFoundBuffer.add( lineToWrite );

            } else {

                boolean foundUsingParent = false;
                String usingParent = "Using Parent";

                if ( useMedicOntology ) {

                    OntologyTerm on = this.medicOntologyService.getTerm( changeMedicToUri( identifier ) );

                    if ( on == null ) {
                        on = this.medicOntologyService.findUsingAlternativeId( identifier );
                    }

                    if ( on != null ) {
                        Collection<OntologyTerm> onParents = on.getParents( true );

                        // here we have all parents try to do something with them
                        for ( OntologyTerm onParent : onParents ) {
                            String parentId = changeToId( onParent.getUri() );
                            String parentUsed = "Parent used: " + onParent.getLabel() + "(" + parentId + ")";

                            Collection<OntologyTerm> ontologyTermsAssociated = findOntologyTermsUriWithDiseaseId( parentId );

                            if ( !ontologyTermsAssociated.isEmpty() ) {

                                for ( OntologyTerm term : ontologyTermsAssociated ) {

                                    String lineToWrite = identifier + "\t" + term.getUri() + "\t" + term.getLabel()
                                            + "\t" + searchTerm + "\t" + parentUsed + "\t"
                                            + "Case 1 : Mesh/Omim term mapped " + usingParent + "\t"
                                            + externalDatabaseName + "\n";

                                    outMappingFoundBuffer.add( lineToWrite );
                                    foundUsingParent = true;
                                }
                            } else if ( findManualMappingTermValueUri( parentId ) != null ) {

                                for ( String valueUriFound : findManualMappingTermValueUri( parentId ) ) {
                                    // 2 - If we couldnt find it lets use the manual mapping file

                                    OntologyTerm ontologyTerm = findOntologyTermExistAndNotObsolote( valueUriFound );

                                    String lineToWrite = identifier + "\t" + ontologyTerm.getUri() + "\t"
                                            + ontologyTerm.getLabel() + "\t" + searchTerm + "\t" + parentUsed + "\t"
                                            + "Case 2 : Found in manual Mapping " + usingParent + "\t"
                                            + externalDatabaseName + "\n";

                                    outMappingFoundBuffer.add( lineToWrite );
                                    foundUsingParent = true;

                                }

                            }

                            // finally lets use the annotator
                            else {

                                Collection<AnnotatorResponse> ontologyTermsNormal1 = cacheAnswerFromAnnotator
                                        .get( onParent.getLabel() );

                                if ( ontologyTermsNormal1 == null ) {
                                    ontologyTermsNormal1 = removeNotExistAndObsolete( anoClient.findTerm( onParent
                                            .getLabel() ) );

                                    // cache results
                                    cacheAnswerFromAnnotator.put( onParent.getLabel(), ontologyTermsNormal1 );
                                }

                                if ( ontologyTermsNormal1 != null ) {

                                    // did we find something ?
                                    condition = findConditionUsed( ontologyTermsNormal1, false );

                                    if ( condition != null ) {

                                        String lineToWrite = identifier + "\t" + valueUriForCondition + "\t"
                                                + valueForCondition + "\t" + searchTerm + "\t" + parentUsed + condition
                                                + usingParent + "\t" + externalDatabaseName + "\n";

                                        outMappingFoundBuffer.add( lineToWrite );
                                        foundUsingParent = true;
                                    }
                                }
                            }
                        }
                    }
                }

                if ( ( !ontologyTermsNormal.isEmpty() || !ontologyTermsWithOutKeywords.isEmpty() ) && !foundUsingParent ) {

                    Collection<AnnotatorResponse> allAnnotatorResponse = new TreeSet<AnnotatorResponse>();
                    allAnnotatorResponse.addAll( ontologyTermsNormal );
                    allAnnotatorResponse.addAll( ontologyTermsWithOutKeywords );
                    // multiple mapping found without a specific condition
                    boolean found = writeInPossibleMapping( allAnnotatorResponse, identifier, searchTerm,
                            externalDatabaseName );

                    if ( !found ) {
                        outNotFoundBuffer.add( line + "\n" );
                    }
                }

                else {
                    // nothing was found with the annotator, write the line in the not found file
                    outNotFoundBuffer.add( line + "\n" );
                }
            }
        }
    }

    private void parseDescriptionToIgnore() throws IOException {

        BufferedReader br = new BufferedReader( new InputStreamReader(
                ExternalDatabaseEvidenceImporterAbstractCLI.class.getResourceAsStream( DESCRIPTION_TO_IGNORE ) ) );

        String line = "";

        while ( ( line = br.readLine() ) != null ) {

            line = removeSmallNumberAndTxt( line );

            descriptionToIgnore.add( removeCha( line ).trim() );
        }

    }

    // the search key will only work if lower case, the key is always lower case, this method take care of it
    protected Collection<String> findManualMappingTermValueUri( String termId ) {
        return manualDescriptionToValuesUriMapping.get( termId.toLowerCase() );
    }

    protected String geneToSymbol( Integer geneId ) {
        // little cache from previous results just to speed up things
        if ( geneToSymbol.get( geneId ) != null ) {
            return geneToSymbol.get( geneId );
        }

        Gene g = this.geneService.findByNCBIId( geneId );

        if ( g != null ) {
            geneToSymbol.put( geneId, g.getOfficialSymbol() );
            return g.getOfficialSymbol();
        }

        return null;
    }

    private String changeToId( String valueUri ) {
        return valueUri.substring( valueUri.lastIndexOf( "/" ) + 1, valueUri.length() ).replace( '_', ':' );
    }

    private String changeMedicToUri( String medicTerm ) {

        String randomUri = this.medicOntologyService.getAllURIs().iterator().next();

        return randomUri.substring( 0, randomUri.lastIndexOf( "/" ) + 1 ) + medicTerm.replace( ':', '_' );
    }

}
