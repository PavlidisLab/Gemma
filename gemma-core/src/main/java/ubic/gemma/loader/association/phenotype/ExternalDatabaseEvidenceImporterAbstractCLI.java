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
import java.net.SocketException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;

import ubic.basecode.ontology.model.OntologyTerm;
import ubic.basecode.ontology.ncbo.AnnotatorClient;
import ubic.basecode.ontology.ncbo.AnnotatorResponse;
import ubic.basecode.ontology.providers.DiseaseOntologyService;
import ubic.basecode.ontology.providers.HumanPhenotypeOntologyService;
import ubic.basecode.ontology.providers.MedicOntologyService;
import ubic.gemma.apps.GemmaCLI.CommandGroup;
import ubic.gemma.genome.gene.service.GeneService;
import ubic.gemma.genome.taxon.service.TaxonService;
import ubic.gemma.model.association.phenotype.PhenotypeMappingType;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.ontology.OntologyService;
import ubic.gemma.util.Settings;

/**
 * TODO Parent of all Importers
 * 
 * @author nicolas
 * @version $Id$
 */
public abstract class ExternalDatabaseEvidenceImporterAbstractCLI extends SymbolChangeAndLoggingAbstract {

    @Override
    public String getCommandName() {
        return null;
    }

    @Override
    public CommandGroup getCommandGroup() {
        return CommandGroup.PHENOTYPES;
    }

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

    // keep description in manual file
    private HashMap<String, String> keyToDescription = new HashMap<String, String>();

    // populated using the disease ontology file
    protected HashMap<String, HashSet<String>> diseaseFileMappingFound = new HashMap<String, HashSet<String>>();

    // ********************************************************************************
    // the services that will be needed
    protected DiseaseOntologyService diseaseOntologyService = null;
    protected HumanPhenotypeOntologyService humanPhenotypeOntologyService = null;
    protected OntologyService ontologyService = null;
    protected TaxonService taxonService = null;
    protected MedicOntologyService medicOntologyService = null;

    // *********************************************************************************************
    // the disease ontology file, we are interested in the MESH and OMIM Id in it ( we dont have this information in
    // gemma)
    protected static final String DISEASE_ONT_PATH = "http://rest.bioontology.org/bioportal/virtual/download/";
    protected static final String DISEASE_ONT_FILE = "1009?apikey=68835db8-b142-4c7d-9509-3c843849ad67";

    protected TreeSet<String> outMappingFoundBuffer = new TreeSet<String>();

    protected String writeFolder = null;

    // the 3 possible out files
    protected BufferedWriter outFinalResults = null;
    protected BufferedWriter outMappingFound = null;

    protected BufferedWriter logRequestAnnotator = null;

    // for a search term we always get the answer, no reason to call the annotator again if it gave us the answer for
    // that request before
    private HashMap<String, Collection<AnnotatorResponse>> cacheAnswerFromAnnotator = new HashMap<String, Collection<AnnotatorResponse>>();

    private HashMap<Integer, String> geneToSymbol = new HashMap<Integer, String>();

    // cache from omimIDToLabel
    private HashMap<String, String> omimIDToLabel = new HashMap<String, String>();

    // load all needed services
    protected synchronized void loadServices( String[] args ) throws Exception {

        // this gets the context, so we can access beans
        Exception err = processCommandLine( args );
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

        // 1- the final results
        initFinalOutputFile( false, false );

        // 2- results found with the annotator need to be verify and if ok moved to a manual annotation file
        outMappingFound = new BufferedWriter( new FileWriter( writeFolder + "/mappingFound.tsv" ) );
        outMappingFound
                .write( "Identifier (KEY)\tPhenotype valueUri (THE KEY MAP TO THIS)\tPhenotype value\tSearch Term used in annotator\tChild Term\tHow we found the mapping\tSource\n" );

        // 3- this a log of request sent to annotator
        logRequestAnnotator = new BufferedWriter( new FileWriter( writeFolder + "/logRequestAnnotatorNotFound.tsv" ) );
    }

    protected void initFinalOutputFile( boolean useScore, boolean useNegative ) throws IOException {

        String score = "";
        String negative = "";

        if ( useScore ) {
            score = "\tScoreType\tScore\tStrength";
        }
        if ( useNegative ) {
            negative = "\tIsNegative";
        }

        // 1- sure results to be imported into Neurocarta
        outFinalResults = new BufferedWriter( new FileWriter( writeFolder + "/finalResults.tsv" ) );
        // headers of the final file
        outFinalResults
                .write( "GeneSymbol\tGeneId\tPrimaryPubMeds\tEvidenceCode\tComments\tExternalDatabase\tDatabaseLink\tPhenotypeMapping\tOrginalPhenotype\tPhenotypes"
                        + negative + score + "\n" );
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

            if ( !isObsoleteOrNotExist( annotatorResponse.getValueUri() )
                    && !resultsToIgnore.contains( annotatorResponse.getValueUri() ) ) {
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

                OntologyTerm on = findOntologyTermExistAndNotObsolote( valueUri );

                if ( on != null ) {
                    terms.add( on );
                }
            }
        }

        return terms;
    }

    // special rule used to format the search term
    public static String removeSpecificKeywords( String txt ) {

        String txtWithExcludeDigitWords = removeEndDigitWords( txt );

        if ( txtWithExcludeDigitWords.isEmpty() ) {
            txtWithExcludeDigitWords = txt;
        }

        String newTxt = txtWithExcludeDigitWords;

        String word = "type";

        String newTxtLowerCase = newTxt.toLowerCase();
        int indexOfWordToExclude = newTxtLowerCase.indexOf( word );
        int wordLength = word.length();

        if ( indexOfWordToExclude != -1 ) {
            newTxt = newTxt.substring( 0, indexOfWordToExclude )
                    + newTxt.substring( indexOfWordToExclude + wordLength, newTxt.length() );
        }

        /*
         * for ( String word : wordsToExclude ) { String newTxtLowerCase = newTxt.toLowerCase(); int
         * indexOfWordToExclude = newTxtLowerCase.indexOf( word ); int wordLength = word.length();
         * 
         * if ( indexOfWordToExclude != -1 ) { newTxt = newTxt.substring( 0, indexOfWordToExclude ) + newTxt.substring(
         * indexOfWordToExclude + wordLength, newTxt.length() ); } }
         */
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
    protected static String removeEndDigitWords( String txt ) {
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

    // write all found in set to files and close files
    // the reason this is done that way is to not have duplicates for 2 files
    protected void writeBuffersAndCloseFiles() throws IOException {

        // write all possible mapping found
        for ( String lineMappinpFound : outMappingFoundBuffer ) {
            outMappingFound.write( lineMappinpFound );
        }

        outMappingFound.close();
        outFinalResults.close();
        logRequestAnnotator.close();

        if ( !logMessages.isEmpty() ) {

            log.info( "here are the error messages :\n" );

            for ( String err : logMessages ) {

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

                    keyToDescription.put( termId, valueStaticFile );

                } else {
                    writeError( "MANUAL VALUEURI AND VALUE DOESNT MATCH IN FILE: line" + line
                            + "\t What the value supposed to be:" + ontologyTerm.getLabel() );
                }
            } else {
                writeError( "MANUAL MAPPING FILE TERM OBSOLETE OR NOT EXISTANT: '" + valueUriStaticFile + "' " + " ("
                        + valueStaticFile + ")" );
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

    protected void findMapping( String meshOrOmimId, Gene gene, String pubmed, String evidenceCode, String description,
            String annotatorKeyword, String externalDatabase, String databaseLink ) throws Exception {

        if ( gene == null ) {
            throw new IllegalArgumentException( "Called with a gene being null   on line pubmed; " + pubmed );
        }

        boolean mappingFound = false;

        // do without parents
        mappingFound = findMapping( meshOrOmimId, gene, pubmed, evidenceCode, description, annotatorKeyword,
                externalDatabase, databaseLink, null );

        if ( mappingFound == false && meshOrOmimId != null ) {

            OntologyTerm on = this.medicOntologyService.getTerm( changeMedicToUri( meshOrOmimId ) );

            if ( on != null ) {
                Collection<OntologyTerm> onParents = on.getParents( true );

                // use omim/mesh parents
                findMapping( meshOrOmimId, gene, pubmed, evidenceCode, description, annotatorKeyword, externalDatabase,
                        databaseLink, onParents );
            }
        }
    }

    private boolean findMapping( String meshOrOmimId, Gene gene, String pubmed, String evidenceCode,
            String description, String annotatorKeyword, String externalDatabase, String databaseLink,
            Collection<OntologyTerm> onParents ) throws Exception {

        boolean mappingFound = false;

        String keywordSearchMeshOrOmimIdLabel = "";

        if ( meshOrOmimId != null && !meshOrOmimId.isEmpty() ) {
            // step 1 using omim or mesh id look in the disease file for annotation
            mappingFound = findOmimMeshInDiseaseOntology( meshOrOmimId, gene, pubmed, evidenceCode, description,
                    externalDatabase, databaseLink, onParents );
        }

        // step 2, the manual mapping file, by OMIM id, MESH or by description if no mesh was given
        if ( mappingFound == false ) {
            mappingFound = findUsingManualMappingFile( meshOrOmimId, annotatorKeyword, gene, pubmed, evidenceCode,
                    description, externalDatabase, databaseLink, onParents );
        }

        // search with the given keyword, usually the description
        if ( annotatorKeyword != null ) {

            // step 3a, use the annotator
            if ( mappingFound == false ) {
                mappingFound = findWithAnnotator( meshOrOmimId, annotatorKeyword, pubmed, evidenceCode,
                        externalDatabase, databaseLink, false, onParents, null );
            }

            // step 3b, use the annotator modify the search
            if ( mappingFound == false ) {

                // same thing but lets modify the search
                findWithAnnotator( meshOrOmimId, annotatorKeyword, pubmed, evidenceCode, externalDatabase,
                        databaseLink, true, onParents, null );
            }
        }

        // lets find the label of the identifier used
        if ( meshOrOmimId != null && !meshOrOmimId.isEmpty() ) {
            keywordSearchMeshOrOmimIdLabel = findDescriptionUsingTerm( meshOrOmimId );
        }

        // search with the label for extra chance of success, found using an OMIM or MESH id
        // if the description given is different than the label found in the ontology
        if ( keywordSearchMeshOrOmimIdLabel != null
                && !keywordSearchMeshOrOmimIdLabel.equalsIgnoreCase( annotatorKeyword ) ) {

            // step 3a, use the annotator
            if ( mappingFound == false ) {
                mappingFound = findWithAnnotator( meshOrOmimId, keywordSearchMeshOrOmimIdLabel, pubmed, evidenceCode,
                        externalDatabase, databaseLink, false, onParents, null );
            }

            // step 3b, use the annotator modify the search
            if ( mappingFound == false ) {

                // same thing but lets modify the search
                findWithAnnotator( meshOrOmimId, keywordSearchMeshOrOmimIdLabel, pubmed, evidenceCode,
                        externalDatabase, databaseLink, true, onParents, null );
            }
        }

        return mappingFound;

    }

    // step 1 using an OMIM or MESH to link to a disease id
    private boolean findOmimMeshInDiseaseOntology( String meshOrOmimId, Gene gene, String pubmed, String evidenceCode,
            String description, String externalDatabase, String databaseLink, Collection<OntologyTerm> onParents )
            throws Exception {

        String mappingType = "";
        String valuesUri = "";
        String originalPhenotype = meshOrOmimId;

        String meshOrOmimIdValue = findDescriptionUsingTerm( meshOrOmimId );
        // use the ontology to find description
        if ( meshOrOmimIdValue != null ) {
            originalPhenotype = originalPhenotype + " (" + meshOrOmimIdValue.toLowerCase() + ")";
        }

        // using parents
        if ( onParents != null ) {

            mappingType = PhenotypeMappingType.INFERRED_XREF.toString();

            HashMap<String, Collection<OntologyTerm>> dieaseOn = meshToDiseaseTerms( onParents );

            originalPhenotype = originalPhenotype + " PARENT: (";

            for ( String key : dieaseOn.keySet() ) {
                originalPhenotype = originalPhenotype + key + ",";
            }

            originalPhenotype = StringUtils.removeEnd( originalPhenotype, "," ) + ")";

            for ( Collection<OntologyTerm> colOn : dieaseOn.values() ) {

                for ( OntologyTerm o : colOn ) {
                    valuesUri = valuesUri + o.getUri() + ";";
                }
            }

        } else {

            mappingType = PhenotypeMappingType.XREF.toString();

            Collection<OntologyTerm> ontologyTerms = findOntologyTermsUriWithDiseaseId( meshOrOmimId );

            for ( OntologyTerm ontologyTerm : ontologyTerms ) {
                valuesUri = valuesUri + ontologyTerm.getUri() + ";";
            }
        }

        if ( !valuesUri.isEmpty() ) {

            outFinalResults.write( gene.getOfficialSymbol() + "\t" + gene.getNcbiGeneId() + "\t" + pubmed + "\t"
                    + evidenceCode + "\t" + description + "\t" + externalDatabase + "\t" + databaseLink + "\t"
                    + mappingType + "\t" + originalPhenotype + "\t" + valuesUri + "\n" );
            return true;

        }
        return false;
    }

    // step 2 manual mapping file
    private boolean findUsingManualMappingFile( String meshOrOmimId, String annotatorKeyword, Gene gene, String pubmed,
            String evidenceCode, String description, String externalDatabase, String databaseLink,
            Collection<OntologyTerm> onParents ) throws Exception {

        String mappingType = "";
        String originalPhenotype = null;
        Collection<String> phenotypesUri = new HashSet<String>();

        if ( onParents != null ) {
            mappingType = PhenotypeMappingType.INFERRED_CURATED.toString();

            originalPhenotype = meshOrOmimId + findExtraInfoMeshDescription( meshOrOmimId ) + " PARENT: (";

            for ( OntologyTerm o : onParents ) {

                String meshId = changeToId( o.getUri() );
                Collection<String> uri = findManualMappingTermValueUri( meshId );
                if ( uri != null && !uri.isEmpty() ) {
                    phenotypesUri.addAll( uri );
                    originalPhenotype = originalPhenotype + meshId + ",";
                }
            }
            originalPhenotype = StringUtils.removeEnd( originalPhenotype, "," ) + ")";

        } else {

            mappingType = PhenotypeMappingType.CURATED.toString();

            if ( meshOrOmimId != null ) {
                originalPhenotype = meshOrOmimId;

            } else {
                originalPhenotype = annotatorKeyword;
            }

            phenotypesUri = findManualMappingTermValueUri( originalPhenotype );

            originalPhenotype = originalPhenotype + findExtraInfoMeshDescription( originalPhenotype );

        }

        if ( phenotypesUri != null && !phenotypesUri.isEmpty() ) {

            outFinalResults.write( gene.getOfficialSymbol() + "\t" + gene.getNcbiGeneId() + "\t" + pubmed + "\t"
                    + evidenceCode + "\t" + description + "\t" + externalDatabase + "\t" + databaseLink + "\t"
                    + mappingType + "\t" + originalPhenotype + "\t" + StringUtils.join( phenotypesUri, ";" ) + "\n" );
            return true;

        }
        return false;
    }

    // step 3
    private boolean findWithAnnotator( String meshOrOmimId, String keywordSearchAnnotator, String pubmed,
            String evidenceCode, String externalDatabase, String databaseLink, boolean modifySearch,
            Collection<OntologyTerm> onParents, String child ) throws Exception {

        String usedChild = "";

        if ( child == null ) {
            usedChild = child;
        }

        if ( onParents != null ) {

            for ( OntologyTerm o : onParents ) {
                boolean found = findWithAnnotator( o.getLabel(), keywordSearchAnnotator, pubmed, evidenceCode,
                        externalDatabase, databaseLink, modifySearch, null, meshOrOmimId );
                if ( found ) {
                    return true;
                }
            }
            return false;

        }

        String searchTerm = keywordSearchAnnotator.toLowerCase();
        Collection<AnnotatorResponse> annotatorResponses = null;

        String key = meshOrOmimId;

        // we are not dealing with an omim identifier, gwas using this case for example
        if ( key == null ) {
            key = keywordSearchAnnotator;
        }

        if ( modifySearch ) {
            searchTerm = removeSpecificKeywords( keywordSearchAnnotator.toLowerCase() );
        }

        if ( !descriptionToIgnore.contains( searchTerm ) ) {

            // we already know the answer for this term
            if ( cacheAnswerFromAnnotator.containsKey( searchTerm ) ) {
                return false;
            }

            // search with the annotator and filter result to take out obsolete terms given
            try {
                annotatorResponses = removeNotExistAndObsolete( AnnotatorClient.findTerm( searchTerm ) );
            } catch ( SocketException e1 ) {
                Thread.sleep( 10000 );
                try {
                    annotatorResponses = removeNotExistAndObsolete( AnnotatorClient.findTerm( searchTerm ) );
                } catch ( SocketException e2 ) {
                    Thread.sleep( 10000 );
                    try {
                        annotatorResponses = removeNotExistAndObsolete( AnnotatorClient.findTerm( searchTerm ) );
                    } catch ( SocketException e3 ) {
                        log.error( "connection problem" );
                        return false;
                    }
                }
            }
            cacheAnswerFromAnnotator.put( searchTerm, annotatorResponses );

            if ( annotatorResponses != null && !annotatorResponses.isEmpty() ) {

                AnnotatorResponse annotatorResponse = annotatorResponses.iterator().next();
                String condition = annotatorResponse.findCondition( modifySearch );

                if ( condition != null ) {

                    OntologyTerm on = findOntologyTermExistAndNotObsolote( annotatorResponse.getValueUri() );

                    if ( on != null ) {

                        searchTerm = AnnotatorClient.removeSpecialCharacters( searchTerm ).replaceAll( "\\+", " " );

                        if ( modifySearch ) {
                            searchTerm = searchTerm + "   (" + keywordSearchAnnotator + ")";
                        }

                        String lineToWrite = key + "\t" + on.getUri() + "\t" + on.getLabel() + "\t" + searchTerm + "\t"
                                + usedChild + "\t" + condition + "\t" + externalDatabase + "\n";

                        outMappingFoundBuffer.add( lineToWrite );

                        return true;
                    }
                }
            }
        }

        logRequestAnnotator.write( AnnotatorClient.removeSpecialCharacters( searchTerm ).replaceAll( "\\+", " " )
                + "   (" + keywordSearchAnnotator + ")\t" );

        if ( annotatorResponses != null && !annotatorResponses.isEmpty() ) {

            for ( AnnotatorResponse ar : annotatorResponses ) {
                logRequestAnnotator.write( ar.getTxtMatched() + ";   " );
            }
        }

        logRequestAnnotator.write( "\n" );
        logRequestAnnotator.flush();

        return false;
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

    private String findDescriptionUsingTerm( String meshOrOmimId ) throws Exception {

        OntologyTerm ontologyTerm = this.medicOntologyService.getTerm( changeMedicToUri( meshOrOmimId ) );

        if ( ontologyTerm != null ) {
            return ontologyTerm.getLabel();
        }

        String conceptId = meshOrOmimId.substring( meshOrOmimId.indexOf( ":" ) + 1, meshOrOmimId.length() );

        // root term in medic
        if ( conceptId.equalsIgnoreCase( "C" ) ) {
            return null;
        }

        // look in cache
        if ( omimIDToLabel.containsKey( conceptId ) ) {
            return omimIDToLabel.get( conceptId );
        }

        String label = null;

        if ( meshOrOmimId.indexOf( "OMIM:" ) != -1 ) {
            label = AnnotatorClient.findLabelUsingIdentifier( 1348l, conceptId );
        } else if ( meshOrOmimId.indexOf( "MESH:" ) != -1 ) {
            label = AnnotatorClient.findLabelUsingIdentifier( 3019l, conceptId );
        } else {
            throw new Exception( "diseaseId not OMIM or MESH: " + meshOrOmimId );
        }

        omimIDToLabel.put( conceptId, label );

        return label;
    }

    private HashMap<String, Collection<OntologyTerm>> meshToDiseaseTerms( Collection<OntologyTerm> meshTerms ) {

        HashMap<String, Collection<OntologyTerm>> diseaseTerms = new HashMap<String, Collection<OntologyTerm>>();

        for ( OntologyTerm m : meshTerms ) {

            String meshId = changeToId( m.getUri() );

            Collection<OntologyTerm> onDisease = findOntologyTermsUriWithDiseaseId( meshId );

            if ( !onDisease.isEmpty() ) {
                diseaseTerms.put( meshId, onDisease );

            }
        }

        return diseaseTerms;
    }

    private String findExtraInfoMeshDescription( String keyword ) throws Exception {

        // use the given description
        if ( findDescriptionUsingTerm( keyword ) != null ) {
            return " (" + findDescriptionUsingTerm( keyword ) + ")";
        }
        // look in the manual mapping file for the description
        else if ( keyToDescription.get( keyword.toLowerCase() ) != null ) {
            return " (" + keyToDescription.get( keyword.toLowerCase() ).toLowerCase() + ")";
        }
        return "";

    }

}
