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
package ubic.gemma.core.loader.association.phenotype;

import org.apache.commons.lang3.StringUtils;
import ubic.basecode.ontology.model.OntologyTerm;
import ubic.basecode.ontology.ncbo.AnnotatorClient;
import ubic.basecode.ontology.ncbo.AnnotatorResponse;
import ubic.basecode.ontology.providers.DiseaseOntologyService;
import ubic.basecode.ontology.providers.HumanPhenotypeOntologyService;
import ubic.basecode.ontology.providers.MedicOntologyService;
import ubic.gemma.core.apps.GemmaCLI.CommandGroup;
import ubic.gemma.core.genome.gene.service.GeneService;
import ubic.gemma.core.ontology.OntologyService;
import ubic.gemma.core.util.AbstractCLI;
import ubic.gemma.model.association.phenotype.PhenotypeMappingType;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.persistence.util.Settings;

import java.io.*;
import java.net.SocketException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author nicolas
 */
public abstract class ExternalDatabaseEvidenceImporterAbstractCLI extends SymbolChangeAndLoggingAbstract {

    // this is where the results and files downloaded are put
    static final String WRITE_FOLDER = Settings.getString( "gemma.appdata.home" );
    // this are where the static resources are kept, like manual mapping files and others
    private static final String RESOURCE_PATH =
            File.separator + "neurocarta" + File.separator + "externalDatabaseImporter" + File.separator;
    // description we want to ignore, when we find those description, we know we are not interested in them
    private static final String DESCRIPTION_TO_IGNORE =
            ExternalDatabaseEvidenceImporterAbstractCLI.RESOURCE_PATH + "DescriptionToIgnore.tsv";
    // manual description file, MeshID, OmimID or txt description to a valueUri
    private static final String MANUAL_MAPPING =
            ExternalDatabaseEvidenceImporterAbstractCLI.RESOURCE_PATH + "ManualDescriptionMapping.tsv";
    private static final String DISEASE_ONT_FILE = "1009?apikey=68835db8-b142-4c7d-9509-3c843849ad67";
    // *********************************************************************************************
    // the disease ontology file, we are interested in the MESH and OMIM Id in it ( we dont have this information in
    // gemma)
    private static final String DISEASE_ONT_PATH = "http://rest.bioontology.org/bioportal/virtual/download/";
    // results we exclude, we know those results are not good
    private static final String RESULTS_TO_IGNORE =
            ExternalDatabaseEvidenceImporterAbstractCLI.RESOURCE_PATH + "ResultsToIgnore.tsv";
    private final HashSet<String> descriptionToIgnore = new HashSet<>();
    // populated using the disease ontology file
    private final HashMap<String, HashSet<String>> diseaseFileMappingFound = new HashMap<>();
    private final TreeSet<String> outMappingFoundBuffer = new TreeSet<>();
    private final HashSet<String> resultsToIgnore = new HashSet<>();
    // for a search term we always get the answer, no reason to call the annotator again if it gave us the answer for
    // that request before
    private final HashMap<String, Collection<AnnotatorResponse>> cacheAnswerFromAnnotator = new HashMap<>();
    private final HashMap<Integer, String> geneToSymbol = new HashMap<>();
    // keep description in manual file
    private final HashMap<String, String> keyToDescription = new HashMap<>();
    private final HashMap<String, HashSet<String>> manualDescriptionToValuesUriMapping = new HashMap<>();
    // cache from omimIDToLabel
    private final HashMap<String, String> omimIDToLabel = new HashMap<>();
    // the 3 possible out files
    BufferedWriter outFinalResults = null;
    String writeFolder = null;
    // ********************************************************************************
    // the services that will be needed
    private DiseaseOntologyService diseaseOntologyService = null;
    private HumanPhenotypeOntologyService humanPhenotypeOntologyService = null;
    private BufferedWriter logRequestAnnotator = null;
    private MedicOntologyService medicOntologyService = null;
    private BufferedWriter outMappingFound = null;

    // the loadServices is in the constructor, we always need those
    ExternalDatabaseEvidenceImporterAbstractCLI( String[] args ) throws Exception {
        super();

        // load all needed services
        this.loadServices( args );
        // results returned by the annotator to ignore
        this.parseResultsToIgnore();
        // parse the manual mapping file
        this.parseManualMappingFile();
        // description we know we are not interested in the result
        this.parseDescriptionToIgnore();
    }

    // special rule used to format the search term
    private static String removeSpecificKeywords( String txt ) {

        String txtWithExcludeDigitWords = ExternalDatabaseEvidenceImporterAbstractCLI.removeEndDigitWords( txt );

        if ( txtWithExcludeDigitWords.isEmpty() ) {
            txtWithExcludeDigitWords = txt;
        }

        StringBuilder newTxt = new StringBuilder( txtWithExcludeDigitWords );

        String word = "type";

        String newTxtLowerCase = newTxt.toString().toLowerCase();
        int indexOfWordToExclude = newTxtLowerCase.indexOf( word );
        int wordLength = word.length();

        if ( indexOfWordToExclude != -1 ) {
            newTxt = new StringBuilder( newTxt.substring( 0, indexOfWordToExclude ) + newTxt
                    .substring( indexOfWordToExclude + wordLength, newTxt.length() ) );
        }

        String[] tokens = newTxt.toString().split( "," );
        newTxt = new StringBuilder();
        for ( String token : tokens ) {
            token = token.trim();
            if ( token.length() > 0 ) {
                newTxt.append( token ).append( "," );
            }
        }
        if ( newTxt.length() > 0 ) {
            newTxt = new StringBuilder( newTxt.substring( 0, newTxt.length() - 1 ) );
        }

        tokens = newTxt.toString().split( "," );

        // reverse them if 2 tokens,
        if ( tokens.length == 2 ) {
            newTxt = new StringBuilder( tokens[1] + "," + tokens[0] );
        }

        // replace , with " "
        return newTxt.toString().replaceAll( ",", " " );
    }

    // special rule used to format the search term
    private static String removeEndDigitWords( String txt ) {
        StringBuilder finalTxt = new StringBuilder();
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
                    }
                }
                finalTxt.append( txtFoundInTerm.trim() );

                if ( i != txtFoundInTerms.length - 1 ) {
                    finalTxt.append( " " );
                }
            }
            if ( j != termFoundInFile.length - 1 ) {
                finalTxt = new StringBuilder( finalTxt.toString().trim() + "," );
            }
            j++;
        }
        return finalTxt.toString();
    }

    @Override
    public CommandGroup getCommandGroup() {
        return CommandGroup.PHENOTYPES;
    }

    @SuppressWarnings("EmptyMethod") // This CLI has no options
    @Override
    protected void buildOptions() {
    }

    @Override
    protected Exception doWork( String[] args ) {
        return null;
    }

    // creates the folder where the output files will be put, with today date
    void createWriteFolderWithDate( String externalDatabaseUse ) throws Exception {

        // where to put the final results
        writeFolder =
                ExternalDatabaseEvidenceImporterAbstractCLI.WRITE_FOLDER + File.separator + externalDatabaseUse + "_"
                        + this.getTodayDate();

        File folder = new File( writeFolder );

        if ( !folder.mkdir() && !folder.exists() ) {
            throw new Exception( "having trouble to create a folder" );
        }

        this.initOutputfiles();
    }

    /**
     * Goes on the specified urlPath and download the file to place it into the writeFolder
     */
    String downloadFileFromWeb( String pathName, String fileUrlName ) {

        String fileName = fileUrlName;

        String fullPathToDownload = pathName + fileUrlName;

        // here we change the name of this specific file so itcan be human readable
        if ( fileName.equalsIgnoreCase( ExternalDatabaseEvidenceImporterAbstractCLI.DISEASE_ONT_FILE ) ) {
            fileName = "diseaseOntology.txt";
        }

        String pathFileName = writeFolder + File.separator + fileName;
        AbstractCLI.log.info( "Trying to download : " + fullPathToDownload );

        URL url;
        try {
            url = new URL( fullPathToDownload );
            url.openConnection();
        } catch ( IOException e1 ) {
            throw new RuntimeException( e1 );
        }

        try (InputStream reader = url.openStream(); FileOutputStream writer = new FileOutputStream( pathFileName )) {

            byte[] buffer = new byte[153600];
            int bytesRead;

            while ( ( bytesRead = reader.read( buffer ) ) > 0 ) {
                writer.write( buffer, 0, bytesRead );
                buffer = new byte[153600];
            }

            writer.close();
            reader.close();
            AbstractCLI.log.info( "Download Completed" );

        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }

        return pathFileName;
    }

    void findMapping( String meshOrOmimId, Gene gene, String pubmed, String evidenceCode, String description,
            String annotatorKeyword, String externalDatabase, String databaseLink ) throws Exception {

        if ( gene == null ) {
            throw new IllegalArgumentException( "Called with a gene being null   on line pubmed; " + pubmed );
        }

        boolean mappingFound;

        // do without parents
        mappingFound = this
                .findMapping( meshOrOmimId, gene, pubmed, evidenceCode, description, annotatorKeyword, externalDatabase,
                        databaseLink, null );

        if ( !mappingFound && meshOrOmimId != null ) {

            OntologyTerm on = this.medicOntologyService.getTerm( this.changeMedicToUri( meshOrOmimId ) );

            if ( on != null ) {
                Collection<OntologyTerm> onParents = on.getParents( true );

                // use omim/mesh parents
                this.findMapping( meshOrOmimId, gene, pubmed, evidenceCode, description, annotatorKeyword,
                        externalDatabase, databaseLink, onParents );
            }
        }
    }

    /**
     * parse the disease ontology file to create the structure needed (omimIdToPhenotypeMapping and
     * meshIdToPhenotypeMapping)
     **/
    void findOmimAndMeshMappingUsingOntologyFile() throws IOException {

        // download the disease Ontology File
        String diseaseOntologyFile = this
                .downloadFileFromWeb( ExternalDatabaseEvidenceImporterAbstractCLI.DISEASE_ONT_PATH,
                        ExternalDatabaseEvidenceImporterAbstractCLI.DISEASE_ONT_FILE );

        HashSet<String> omimIds = new HashSet<>();
        HashSet<String> meshIds = new HashSet<>();
        String valueUri = null;

        try (BufferedReader br = new BufferedReader( new FileReader( diseaseOntologyFile ) )) {

            String line;

            boolean foundTerm = false;

            while ( ( line = br.readLine() ) != null ) {

                String[] tokens;

                line = line.trim();

                // found a term
                if ( line.equalsIgnoreCase( "[Term]" ) ) {
                    foundTerm = true;
                    valueUri = null;
                    omimIds = new HashSet<>();
                    meshIds = new HashSet<>();
                } else if ( foundTerm ) {

                    if ( line.startsWith( "id:" ) ) {

                        tokens = line.split( ":" );

                        String diseaseId = tokens[2].trim();
                        // will throw exception if a number is not found
                        //noinspection ResultOfMethodCallIgnored // Called for the possible exception
                        Integer.parseInt( diseaseId );
                        // disease id
                        valueUri = "http://purl.obolibrary.org/obo/DOID_" + diseaseId;

                    } else if ( line.contains( "xref: OMIM" ) ) {

                        tokens = line.split( ":" );
                        omimIds.add( tokens[2].trim() );
                    } else if ( line.contains( "xref: MSH" ) ) {
                        tokens = line.split( ":" );
                        meshIds.add( tokens[2].trim() );
                    }

                    // end of a term
                    else if ( line.equalsIgnoreCase( "" ) ) {

                        foundTerm = false;

                        for ( String omimId : omimIds ) {

                            HashSet<String> h = new HashSet<>();
                            String key = "OMIM:" + omimId;

                            h = this.checkAndAddValue( valueUri, h, key );
                            diseaseFileMappingFound.put( key, h );
                        }

                        for ( String meshId : meshIds ) {

                            String key = "MESH:" + meshId;

                            HashSet<String> h = new HashSet<>();

                            h = this.checkAndAddValue( valueUri, h, key );
                            diseaseFileMappingFound.put( key, h );
                        }
                    }
                }
            }
        }
    }

    // is the valueUri existing or obsolete ?
    OntologyTerm findOntologyTermExistAndNotObsolote( String valueUri ) {

        OntologyTerm o = this.diseaseOntologyService.getTerm( valueUri );
        if ( o == null ) {
            o = this.humanPhenotypeOntologyService.getTerm( valueUri );
        }

        if ( o == null || o.isTermObsolete() ) {
            return null;
        }

        return o;
    }

    String geneToSymbol( Integer geneId ) {
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

    void initFinalOutputFile( boolean useScore, boolean useNegative ) throws IOException {

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

    // write all found in set to files and close files
    // the reason this is done that way is to not have duplicates for 2 files
    void writeBuffersAndCloseFiles() throws IOException {

        // write all possible mapping found
        for ( String lineMappinpFound : outMappingFoundBuffer ) {
            outMappingFound.write( lineMappinpFound );
        }

        outMappingFound.close();
        outFinalResults.close();
        logRequestAnnotator.close();

        if ( !logMessages.isEmpty() ) {

            AbstractCLI.log.info( "here are the error messages :\n" );

            for ( String err : logMessages ) {

                AbstractCLI.log.error( err );
            }
        }
    }

    // creates the folder where the output files will be put, use this one if file is too big
    String createWriteFolderIfDoesntExist( String externalDatabaseUse ) throws Exception {

        // where to put the final results
        writeFolder = ExternalDatabaseEvidenceImporterAbstractCLI.WRITE_FOLDER + File.separator + externalDatabaseUse;

        File folder = new File( writeFolder );

        if ( !folder.mkdir() && !folder.exists() ) {
            throw new Exception( "having trouble to create a folder" );
        }

        this.initOutputfiles();

        return writeFolder;
    }

    boolean notInteger( String input ) {
        try {
            //noinspection ResultOfMethodCallIgnored // Invoked to check if it would throw an exception
            Integer.parseInt( input );
            return false;
        } catch ( Exception e ) {
            return true;
        }
    }

    private HashSet<String> checkAndAddValue( String valueUri, HashSet<String> h, String key ) {
        if ( diseaseFileMappingFound.get( key ) == null ) {
            if ( this.existsAndNotObsolete( valueUri ) ) {
                h.add( valueUri );
            }
        } else {
            h = diseaseFileMappingFound.get( key );
            if ( this.existsAndNotObsolete( valueUri ) ) {
                h.add( valueUri );
            }
        }
        return h;
    }

    // the search key will only work if lower case, the key is always lower case, this method take care of it
    private Collection<String> findManualMappingTermValueUri( String termId ) {
        return manualDescriptionToValuesUriMapping.get( termId.toLowerCase() );
    }

    // load all needed services
    private synchronized void loadServices( String[] args ) throws Exception {

        // this gets the context, so we can access beans
        Exception err = this.processCommandLine( args );
        if ( err != null )
            throw err;

        OntologyService ontologyService = this.getBean( OntologyService.class );
        this.diseaseOntologyService = ontologyService.getDiseaseOntologyService();
        this.humanPhenotypeOntologyService = ontologyService.getHumanPhenotypeOntologyService();
        this.geneService = this.getBean( GeneService.class );

        while ( !this.diseaseOntologyService.isOntologyLoaded() ) {
            this.wait( 3000 );
            AbstractCLI.log.info( "waiting for the Disease Ontology to load" );
        }

        while ( !this.humanPhenotypeOntologyService.isOntologyLoaded() ) {
            this.wait( 3000 );
            AbstractCLI.log.info( "waiting for the HP Ontology to load" );
        }

        this.medicOntologyService = new MedicOntologyService();
    }

    private void parseManualMappingFile() throws IOException {

        BufferedReader br = new BufferedReader( new InputStreamReader( RgdDatabaseImporter.class
                .getResourceAsStream( ExternalDatabaseEvidenceImporterAbstractCLI.MANUAL_MAPPING ) ) );

        String line;
        // skip first line, the headers
        br.readLine();

        // reads the manual file and put the data in a structure
        while ( ( line = br.readLine() ) != null ) {

            HashSet<String> col = new HashSet<>();

            String[] tokens = line.split( "\t" );

            String termId = tokens[0].trim().toLowerCase();
            String valueUriStaticFile = tokens[1].trim();
            String valueStaticFile = tokens[2].trim();

            OntologyTerm ontologyTerm = this.findOntologyTermExistAndNotObsolote( valueUriStaticFile );

            if ( ontologyTerm != null ) {

                if ( ontologyTerm.getLabel().equalsIgnoreCase( valueStaticFile ) ) {

                    if ( manualDescriptionToValuesUriMapping.get( termId ) != null ) {
                        col = manualDescriptionToValuesUriMapping.get( termId );
                    }

                    col.add( valueUriStaticFile );

                    manualDescriptionToValuesUriMapping.put( termId, col );

                    keyToDescription.put( termId, valueStaticFile );

                } else {
                    this.writeError( "MANUAL VALUEURI AND VALUE DOESNT MATCH IN FILE: line" + line
                            + "\t What the value supposed to be:" + ontologyTerm.getLabel() );
                }
            } else {
                this.writeError(
                        "MANUAL MAPPING FILE TERM OBSOLETE OR NOT EXISTANT: '" + valueUriStaticFile + "' " + " ("
                                + valueStaticFile + ")" );
            }
        }
    }

    // if a excel file was used values sometime save as "value", always take out the "
    private String removeCha( String txt ) {

        String newTxt = txt.replaceAll( "\"", "" );
        return newTxt.trim();
    }

    // checks if a value uri usually found with the annotator exists or obsolete, filters the results
    private Collection<AnnotatorResponse> removeNotExistAndObsolete(
            Collection<AnnotatorResponse> annotatorResponses ) {

        Collection<AnnotatorResponse> annotatorResponseWithNoObsolete = new TreeSet<>();

        for ( AnnotatorResponse annotatorResponse : annotatorResponses ) {

            if ( this.existsAndNotObsolete( annotatorResponse.getValueUri() ) && !resultsToIgnore
                    .contains( annotatorResponse.getValueUri() ) ) {
                annotatorResponseWithNoObsolete.add( annotatorResponse );
            }
        }
        return annotatorResponseWithNoObsolete;
    }

    // special rules used to format the search term, when using a modified search in the annotator
    private String removeSmallNumberAndTxt( String txt ) {

        StringBuilder txtWithoutSimpLetters = new StringBuilder();

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
                txtWithoutSimpLetters.append( token );
            } else {
                txtWithoutSimpLetters.append( token ).append( " " );
            }
        }
        return txtWithoutSimpLetters.toString().trim();
    }

    // is the valueUri existing or obsolete ?
    private boolean existsAndNotObsolete( String valueUri ) {

        OntologyTerm o = this.diseaseOntologyService.getTerm( valueUri );
        if ( o == null ) {
            o = this.humanPhenotypeOntologyService.getTerm( valueUri );
        }

        return o != null && !o.isTermObsolete();
    }

    private String changeMedicToUri( String medicTerm ) {

        String randomUri = this.medicOntologyService.getAllURIs().iterator().next();

        return randomUri.substring( 0, randomUri.lastIndexOf( "/" ) + 1 ) + medicTerm.replace( ':', '_' );
    }

    private String changeToId( String valueUri ) {
        return valueUri.substring( valueUri.lastIndexOf( "/" ) + 1, valueUri.length() ).replace( '_', ':' );
    }

    private String findDescriptionUsingTerm( String meshOrOmimId ) throws Exception {

        OntologyTerm ontologyTerm = this.medicOntologyService.getTerm( this.changeMedicToUri( meshOrOmimId ) );

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

        String label;

        if ( meshOrOmimId.contains( "OMIM:" ) ) {
            label = AnnotatorClient.findLabelUsingIdentifier( 1348L, conceptId );
        } else if ( meshOrOmimId.contains( "MESH:" ) ) {
            label = AnnotatorClient.findLabelUsingIdentifier( 3019L, conceptId );
        } else {
            throw new Exception( "diseaseId not OMIM or MESH: " + meshOrOmimId );
        }

        omimIDToLabel.put( conceptId, label );

        return label;
    }

    private String findExtraInfoMeshDescription( String keyword ) throws Exception {

        // use the given description
        if ( this.findDescriptionUsingTerm( keyword ) != null ) {
            return " (" + this.findDescriptionUsingTerm( keyword ) + ")";
        }
        // look in the manual mapping file for the description
        else if ( keyToDescription.get( keyword.toLowerCase() ) != null ) {
            return " (" + keyToDescription.get( keyword.toLowerCase() ).toLowerCase() + ")";
        }
        return "";

    }

    private boolean findMapping( String meshOrOmimId, Gene gene, String pubmed, String evidenceCode, String description,
            String annotatorKeyword, String externalDatabase, String databaseLink, Collection<OntologyTerm> onParents )
            throws Exception {

        boolean mappingFound = false;

        String keywordSearchMeshOrOmimIdLabel = "";

        if ( meshOrOmimId != null && !meshOrOmimId.isEmpty() ) {
            // step 1 using omim or mesh id look in the disease file for annotation
            mappingFound = this.findOmimMeshInDiseaseOntology( meshOrOmimId, gene, pubmed, evidenceCode, description,
                    externalDatabase, databaseLink, onParents );
        }

        // step 2, the manual mapping file, by OMIM id, MESH or by description if no mesh was given
        if ( !mappingFound ) {
            mappingFound = this.findUsingManualMappingFile( meshOrOmimId, annotatorKeyword, gene, pubmed, evidenceCode,
                    description, externalDatabase, databaseLink, onParents );
        }

        // search with the given keyword, usually the description
        if ( annotatorKeyword != null ) {

            // step 3a, use the annotator
            if ( !mappingFound ) {
                mappingFound = this
                        .findWithAnnotator( meshOrOmimId, annotatorKeyword, externalDatabase, false, onParents, null );
            }

            // step 3b, use the annotator modify the search
            if ( !mappingFound ) {

                // same thing but lets modify the search
                this.findWithAnnotator( meshOrOmimId, annotatorKeyword, externalDatabase, true, onParents, null );
            }
        }

        // lets find the label of the identifier used
        if ( meshOrOmimId != null && !meshOrOmimId.isEmpty() ) {
            keywordSearchMeshOrOmimIdLabel = this.findDescriptionUsingTerm( meshOrOmimId );
        }

        // search with the label for extra chance of success, found using an OMIM or MESH id
        // if the description given is different than the label found in the ontology
        if ( keywordSearchMeshOrOmimIdLabel != null && !keywordSearchMeshOrOmimIdLabel
                .equalsIgnoreCase( annotatorKeyword ) ) {

            // step 3a, use the annotator
            if ( !mappingFound ) {
                mappingFound = this
                        .findWithAnnotator( meshOrOmimId, keywordSearchMeshOrOmimIdLabel, externalDatabase, false,
                                onParents, null );
            }

            // step 3b, use the annotator modify the search
            if ( !mappingFound ) {

                // same thing but lets modify the search
                this.findWithAnnotator( meshOrOmimId, keywordSearchMeshOrOmimIdLabel, externalDatabase, true, onParents,
                        null );
            }
        }

        return mappingFound;

    }

    // step 1 using an OMIM or MESH to link to a disease id
    private boolean findOmimMeshInDiseaseOntology( String meshOrOmimId, Gene gene, String pubmed, String evidenceCode,
            String description, String externalDatabase, String databaseLink, Collection<OntologyTerm> onParents )
            throws Exception {

        String mappingType;
        StringBuilder valuesUri = new StringBuilder();
        StringBuilder originalPhenotype = new StringBuilder( meshOrOmimId );

        String meshOrOmimIdValue = this.findDescriptionUsingTerm( meshOrOmimId );
        // use the ontology to find description
        if ( meshOrOmimIdValue != null ) {
            originalPhenotype.append( " (" ).append( meshOrOmimIdValue.toLowerCase() ).append( ")" );
        }

        // using parents
        if ( onParents != null ) {

            mappingType = PhenotypeMappingType.INFERRED_XREF.toString();

            HashMap<String, Collection<OntologyTerm>> dieaseOn = this.meshToDiseaseTerms( onParents );

            originalPhenotype.append( " PARENT: (" );

            for ( String key : dieaseOn.keySet() ) {
                originalPhenotype.append( key ).append( "," );
            }

            originalPhenotype = new StringBuilder( StringUtils.removeEnd( originalPhenotype.toString(), "," ) + ")" );

            for ( Collection<OntologyTerm> colOn : dieaseOn.values() ) {

                for ( OntologyTerm o : colOn ) {
                    valuesUri.append( o.getUri() ).append( ";" );
                }
            }

        } else {

            mappingType = PhenotypeMappingType.XREF.toString();

            Collection<OntologyTerm> ontologyTerms = this.findOntologyTermsUriWithDiseaseId( meshOrOmimId );

            for ( OntologyTerm ontologyTerm : ontologyTerms ) {
                valuesUri.append( ontologyTerm.getUri() ).append( ";" );
            }
        }

        if ( valuesUri.length() > 0 ) {

            outFinalResults
                    .write( gene.getOfficialSymbol() + "\t" + gene.getNcbiGeneId() + "\t" + pubmed + "\t" + evidenceCode
                            + "\t" + description + "\t" + externalDatabase + "\t" + databaseLink + "\t" + mappingType
                            + "\t" + originalPhenotype + "\t" + valuesUri + "\n" );
            return true;

        }
        return false;
    }

    private Collection<OntologyTerm> findOntologyTermsUriWithDiseaseId( String diseaseId ) {

        Collection<OntologyTerm> terms = new HashSet<>();
        Collection<String> valuesUri = diseaseFileMappingFound.get( diseaseId );

        if ( valuesUri != null && !valuesUri.isEmpty() ) {

            for ( String valueUri : valuesUri ) {

                OntologyTerm on = this.findOntologyTermExistAndNotObsolote( valueUri );

                if ( on != null ) {
                    terms.add( on );
                }
            }
        }

        return terms;
    }

    // step 2 manual mapping file
    private boolean findUsingManualMappingFile( String meshOrOmimId, String annotatorKeyword, Gene gene, String pubmed,
            String evidenceCode, String description, String externalDatabase, String databaseLink,
            Collection<OntologyTerm> onParents ) throws Exception {

        String mappingType;
        StringBuilder originalPhenotype;
        Collection<String> phenotypesUri = new HashSet<>();

        if ( onParents != null ) {
            mappingType = PhenotypeMappingType.INFERRED_CURATED.toString();

            originalPhenotype = new StringBuilder(
                    meshOrOmimId + this.findExtraInfoMeshDescription( meshOrOmimId ) + " PARENT: (" );

            for ( OntologyTerm o : onParents ) {

                String meshId = this.changeToId( o.getUri() );
                Collection<String> uri = this.findManualMappingTermValueUri( meshId );
                if ( uri != null && !uri.isEmpty() ) {
                    phenotypesUri.addAll( uri );
                    originalPhenotype.append( meshId ).append( "," );
                }
            }
            originalPhenotype = new StringBuilder( StringUtils.removeEnd( originalPhenotype.toString(), "," ) + ")" );

        } else {

            mappingType = PhenotypeMappingType.CURATED.toString();

            if ( meshOrOmimId != null ) {
                originalPhenotype = new StringBuilder( meshOrOmimId );

            } else {
                originalPhenotype = new StringBuilder( annotatorKeyword );
            }

            phenotypesUri = this.findManualMappingTermValueUri( originalPhenotype.toString() );

            originalPhenotype.append( this.findExtraInfoMeshDescription( originalPhenotype.toString() ) );

        }

        if ( phenotypesUri != null && !phenotypesUri.isEmpty() ) {

            outFinalResults
                    .write( gene.getOfficialSymbol() + "\t" + gene.getNcbiGeneId() + "\t" + pubmed + "\t" + evidenceCode
                            + "\t" + description + "\t" + externalDatabase + "\t" + databaseLink + "\t" + mappingType
                            + "\t" + originalPhenotype + "\t" + StringUtils.join( phenotypesUri, ";" ) + "\n" );
            return true;

        }
        return false;
    }

    // step 3
    private boolean findWithAnnotator( String meshOrOmimId, String keywordSearchAnnotator, String externalDatabase,
            boolean modifySearch, Collection<OntologyTerm> onParents, String child ) throws Exception {

        String usedChild = "";

        if ( child == null ) {
            usedChild = null;
        }

        if ( onParents != null ) {

            for ( OntologyTerm o : onParents ) {
                boolean found = this
                        .findWithAnnotator( o.getLabel(), keywordSearchAnnotator, externalDatabase, modifySearch, null,
                                meshOrOmimId );
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
            searchTerm = ExternalDatabaseEvidenceImporterAbstractCLI
                    .removeSpecificKeywords( keywordSearchAnnotator.toLowerCase() );
        }

        if ( !descriptionToIgnore.contains( searchTerm ) ) {

            // we already know the answer for this term
            if ( cacheAnswerFromAnnotator.containsKey( searchTerm ) ) {
                return false;
            }

            // search with the annotator and filter result to take out obsolete terms given
            try {
                annotatorResponses = this.removeNotExistAndObsolete( AnnotatorClient.findTerm( searchTerm ) );
            } catch ( SocketException e1 ) {
                Thread.sleep( 10000 );
                try {
                    annotatorResponses = this.removeNotExistAndObsolete( AnnotatorClient.findTerm( searchTerm ) );
                } catch ( SocketException e2 ) {
                    Thread.sleep( 10000 );
                    try {
                        annotatorResponses = this.removeNotExistAndObsolete( AnnotatorClient.findTerm( searchTerm ) );
                    } catch ( SocketException e3 ) {
                        AbstractCLI.log.error( "connection problem" );
                        return false;
                    }
                }
            }
            cacheAnswerFromAnnotator.put( searchTerm, annotatorResponses );

            if ( annotatorResponses != null && !annotatorResponses.isEmpty() ) {

                AnnotatorResponse annotatorResponse = annotatorResponses.iterator().next();
                String condition = annotatorResponse.findCondition( modifySearch );

                if ( condition != null ) {

                    OntologyTerm on = this.findOntologyTermExistAndNotObsolote( annotatorResponse.getValueUri() );

                    if ( on != null ) {

                        searchTerm = AnnotatorClient.removeSpecialCharacters( searchTerm ).replaceAll( "\\+", " " );

                        if ( modifySearch ) {
                            searchTerm = searchTerm + "   (" + keywordSearchAnnotator + ")";
                        }

                        String lineToWrite =
                                key + "\t" + on.getUri() + "\t" + on.getLabel() + "\t" + searchTerm + "\t" + usedChild
                                        + "\t" + condition + "\t" + externalDatabase + "\n";

                        outMappingFoundBuffer.add( lineToWrite );

                        return true;
                    }
                }
            }
        }

        logRequestAnnotator
                .write( AnnotatorClient.removeSpecialCharacters( searchTerm ).replaceAll( "\\+", " " ) + "   ("
                        + keywordSearchAnnotator + ")\t" );

        if ( annotatorResponses != null && !annotatorResponses.isEmpty() ) {

            for ( AnnotatorResponse ar : annotatorResponses ) {
                logRequestAnnotator.write( ar.getTxtMatched() + ";   " );
            }
        }

        logRequestAnnotator.write( "\n" );
        logRequestAnnotator.flush();

        return false;
    }

    private String getTodayDate() {
        DateFormat dateFormat = new SimpleDateFormat( "yyyy-MM-dd_HH:mm" );
        Calendar cal = Calendar.getInstance();
        return dateFormat.format( cal.getTime() );
    }

    // all imported can have up to 3 outputFiles, that are the results
    private void initOutputfiles() throws IOException {

        // 1- the final results
        this.initFinalOutputFile( false, false );

        // 2- results found with the annotator need to be verify and if ok moved to a manual annotation file
        outMappingFound = new BufferedWriter( new FileWriter( writeFolder + "/mappingFound.tsv" ) );
        outMappingFound
                .write( "Identifier (KEY)\tPhenotype valueUri (THE KEY MAP TO THIS)\tPhenotype value\tSearch Term used in annotator\tChild Term\tHow we found the mapping\tSource\n" );

        // 3- this a log of request sent to annotator
        logRequestAnnotator = new BufferedWriter( new FileWriter( writeFolder + "/logRequestAnnotatorNotFound.tsv" ) );
    }

    private HashMap<String, Collection<OntologyTerm>> meshToDiseaseTerms( Collection<OntologyTerm> meshTerms ) {

        HashMap<String, Collection<OntologyTerm>> diseaseTerms = new HashMap<>();

        for ( OntologyTerm m : meshTerms ) {

            String meshId = this.changeToId( m.getUri() );

            Collection<OntologyTerm> onDisease = this.findOntologyTermsUriWithDiseaseId( meshId );

            if ( !onDisease.isEmpty() ) {
                diseaseTerms.put( meshId, onDisease );

            }
        }

        return diseaseTerms;
    }

    private void parseDescriptionToIgnore() throws IOException {

        BufferedReader br = new BufferedReader( new InputStreamReader( ExternalDatabaseEvidenceImporterAbstractCLI.class
                .getResourceAsStream( ExternalDatabaseEvidenceImporterAbstractCLI.DESCRIPTION_TO_IGNORE ) ) );

        String line;

        while ( ( line = br.readLine() ) != null ) {

            line = this.removeSmallNumberAndTxt( line );

            descriptionToIgnore.add( this.removeCha( line ).trim() );
        }
    }

    // parse file and returns a collection of terms found
    private void parseResultsToIgnore() throws IOException {

        BufferedReader br = new BufferedReader( new InputStreamReader( ExternalDatabaseEvidenceImporterAbstractCLI.class
                .getResourceAsStream( ExternalDatabaseEvidenceImporterAbstractCLI.RESULTS_TO_IGNORE ) ) );
        String line;

        while ( ( line = br.readLine() ) != null ) {
            String[] tokens = line.split( "\t" );
            resultsToIgnore.add( this.removeCha( tokens[0] ) );
        }

    }

}
