/*
 * The gemma-core project
 *
 * Copyright (c) 2018 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package ubic.gemma.core.loader.association.phenotype;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.ClassPathResource;
import org.xml.sax.SAXException;
import ubic.basecode.ontology.model.OntologyTerm;
import ubic.basecode.ontology.ncbo.AnnotatorClient;
import ubic.basecode.ontology.ncbo.AnnotatorResponse;
import ubic.basecode.ontology.providers.HumanPhenotypeOntologyService;
import ubic.basecode.ontology.providers.MedicOntologyService;
import ubic.basecode.util.Configuration;
import ubic.gemma.persistence.service.genome.gene.GeneService;
import ubic.gemma.core.ontology.providers.MondoOntologyService;
import ubic.gemma.model.association.phenotype.PhenotypeMappingType;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.core.config.Settings;

import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Moved code here so we can test it
 *
 * @author paul (code originally from Nicolas)
 */
@CommonsLog
@Deprecated
class PhenotypeProcessingUtil {
    // this is where the results and files downloaded are put
    static final String WRITE_FOLDER = Settings.getString( "gemma.appdata.home" ) + File.separator + "Phenocarta";
    // this are where the static resources are kept, like manual mapping files and others
    private static final String RESOURCE_CLASSPATH = "/phenocarta/externalDatabaseImporter/";

    // populated using the disease ontology OBO
    private static final Map<String, Set<String>> omimAndmesh2DO = new HashMap<>();
    // description we want to ignore, when we find those description, we know we are not interested in them
    private static final String DESCRIPTION_TO_IGNORE = RESOURCE_CLASSPATH + "DescriptionToIgnore.tsv";
    /*
     * the disease ontology OBO file, we are interested in the MESH and OMIM Ids in it.
     * 598 refers to the file version as of 12/2018, which will change...
     */
    private static final String DISEASE_ONT_OBO_FILE =
            "598/download?apikey=" + Configuration.getString( "ncbo.api.key" );
    private static final String DISEASE_ONT_PATH = "http://data.bioontology.org/ontologies/DOID/submissions";
    // manual description file, MeshID, OmimID or txt description to a valueUri
    private static final String MANUAL_MAPPING = RESOURCE_CLASSPATH + "ManualDescriptionMapping.tsv";
    // results we exclude, we know those results are not good
    private static final String RESULTS_TO_IGNORE = RESOURCE_CLASSPATH + "ResultsToIgnore.tsv";
    // keep a log file of the process and error
    final TreeSet<String> logMessages = new TreeSet<>();
    private final Map<Integer, String> geneToSymbol = new HashMap<>();
    // for a search term we always get the answer, no reason to call the annotator again if it gave us the answer for
    // that request before
    private final Map<String, Collection<AnnotatorResponse>> cacheAnswerFromAnnotator = new HashMap<>();
    private final Set<String> descriptionToIgnore = new HashSet<>();
    // keep description in manual file
    private final Map<String, String> keyToDescription = new HashMap<>();
    private final Map<String, Set<String>> manualDescriptionToValuesUriMapping = new HashMap<>();
    // cache from omimIDToLabel
    private final Map<String, String> omimIDToLabel = new HashMap<>();
    private final Set<String> outMappingFoundBuffer = new TreeSet<>();
    private final Set<String> resultsToIgnore = new HashSet<>();
    GeneService geneService;
    // the 3 possible out files
    BufferedWriter outFinalResults = null;
    private final MedicOntologyService medicOntologyService;
    private final MondoOntologyService diseaseOntologyService;
    private final HumanPhenotypeOntologyService humanPhenotypeOntologyService;
    // to avoid repeatedly checking....
    private Set<String> unmappableIds = new HashSet<>();
    private BufferedWriter logRequestAnnotator = null;
    private BufferedWriter outMappingFound = null;

    PhenotypeProcessingUtil( GeneService g, MedicOntologyService medicOntologyService, MondoOntologyService diseaseOntologyService, HumanPhenotypeOntologyService humanPhenotypeOntologyService ) throws Exception {
        this.geneService = g;
        this.medicOntologyService = medicOntologyService;
        this.diseaseOntologyService = diseaseOntologyService;
        this.humanPhenotypeOntologyService = humanPhenotypeOntologyService;
        assert g != null;
        init();
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

    // special rule used to format the search term
    private static String removeSpecificKeywords( String txt ) {

        String txtWithExcludeDigitWords = removeEndDigitWords( txt );

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

    /**
     * creates the folder where the output files will be put, use this one if file is too big
     * and initializes files
     *
     * @param externalDatabaseUse external database use
     * @return the folder
     * @throws Exception when the folder can't be created
     */
    String createWriteFolderIfDoesntExist( String externalDatabaseUse ) throws Exception {

        // where to put the final results
        String writeFolder = WRITE_FOLDER + File.separator + externalDatabaseUse;

        File folder = new File( writeFolder );

        if ( !folder.mkdir() && !folder.exists() ) {
            throw new Exception( "having trouble to create a folder" );
        }

        this.initOutputfiles( externalDatabaseUse, writeFolder );

        return writeFolder;
    }

    /**
     * creates the folder where the output files will be put, with today's date
     *
     * @param externalDatabaseUse external database use
     * @throws Exception when the folder can't be created
     */
    String createWriteFolderWithDate( String externalDatabaseUse ) throws Exception {

        // where to put the final results
        String writeFolder =
                PhenotypeProcessingUtil.WRITE_FOLDER + File.separator + externalDatabaseUse + "_" + getTodayDate();

        File folder = new File( writeFolder );

        if ( !folder.mkdir() && !folder.exists() ) {
            throw new Exception( "having trouble to create a folder" );
        }

        initOutputfiles( externalDatabaseUse, writeFolder );

        return writeFolder;
    }

    /**
     * Key entry point to map IDs or terms to DO identifiers.
     *
     * @param proposedID       Either a MESH, OMIM or DO ID. If the latter, we look for it directly in DO. If MESH or
     *                         OMIM we try to map it.
     * @param gene             gene
     * @param pubmed           pubmed
     * @param evidenceCode     evidence code
     * @param description      description
     * @param annotatorKeyword will be used to find terms as a fallback.
     * @param externalDatabase external database
     * @param databaseLink     database link
     * @return true if mapping was found
     * @throws Exception IO problems
     */
    boolean findMapping( String proposedID, Gene gene, String pubmed, String evidenceCode, String description,
            String annotatorKeyword, String externalDatabase, String databaseLink ) throws Exception {

        if ( gene == null ) {
            throw new IllegalArgumentException( "Called with a gene being null on line pubmed; " + pubmed );
        }

        boolean mappingFound;

        // do without parents
        mappingFound = this
                .findMapping( proposedID, gene, pubmed, evidenceCode, description, annotatorKeyword, externalDatabase,
                        databaseLink, null );

        if ( !mappingFound && proposedID != null ) {

            OntologyTerm on = medicOntologyService.getTerm( this.changeMedicToUri( proposedID ) );

            if ( on != null ) {
                Collection<OntologyTerm> onParents = on.getParents( true );

                // use omim/mesh parents
                this.findMapping( proposedID, gene, pubmed, evidenceCode, description, annotatorKeyword,
                        externalDatabase, databaseLink, onParents );
            }
        }

        return mappingFound;
    }

    /**
     * is the valueUri existing or obsolete ? Warning only works with DO And HPO
     *
     * @param valueUri value uri
     * @return ontology term
     */
    OntologyTerm findOntologyTermExistAndNotObsolete( String valueUri ) {

        OntologyTerm o = diseaseOntologyService.getTerm( valueUri );
        if ( o == null ) {
            o = humanPhenotypeOntologyService.getTerm( valueUri );
        }

        if ( o == null || o.isObsolete() ) {
            return null;
        }

        return o;
    }

    String geneToSymbol( Integer geneId ) {
        // little cache from previous results just to speed up things
        if ( geneToSymbol.get( geneId ) != null ) {
            return geneToSymbol.get( geneId );
        }

        Gene g = geneService.findByNCBIId( geneId );

        if ( g != null ) {
            geneToSymbol.put( geneId, g.getOfficialSymbol() );
            return g.getOfficialSymbol();
        }

        return null;
    }

    /**
     * Produces a file like gwas.finalResults.tsv. The header is written with columns to be created.
     *
     * @param prefix      e.g. gwas, sfar, omim for the data source
     * @param writeFolder where the file will go
     * @param useScore    score information will be present
     * @param useNegative information on negative evidence will be present
     * @throws IOException IO problems
     */
    void initFinalOutputFile( String prefix, String writeFolder, boolean useScore, boolean useNegative )
            throws IOException {

        String score = "";
        String negative = "";

        if ( useScore ) {
            score = "\tScoreType\tScore\tStrength";
        }
        if ( useNegative ) {
            negative = "\tIsNegative";
        }

        outFinalResults = new BufferedWriter(
                new FileWriter( writeFolder + File.separator + prefix + ".finalResults.tsv" ) );
        outFinalResults
                .write( "GeneSymbol\tGeneId\tPrimaryPubMeds\tEvidenceCode\tComments\tExternalDatabase\tDatabaseLink\tPhenotypeMapping\tOrginalPhenotype\tPhenotypes"
                        + negative + score + "\n" );
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

    /**
     * Goes on the specified urlPath and download the file to place it into the writeFolder
     */
    String downloadFileFromWeb( String pathName, String fileUrlName, String outputPath, String outputFileName ) {

        String fullPathToDownload = pathName + "/" + fileUrlName;

        String outputFileFullPath =
                outputPath + ( outputPath.endsWith( File.separator ) ? "" : File.separator ) + outputFileName;
        log.info( "Trying to download : " + fullPathToDownload + " to " + outputFileFullPath );

        URL url;
        try {
            url = new URL( fullPathToDownload );
            url.openConnection();
        } catch ( IOException e1 ) {
            throw new RuntimeException( e1 );
        }

        try ( InputStream reader = url.openStream();
                FileOutputStream writer = new FileOutputStream( outputFileFullPath ) ) {
            IOUtils.copy( reader, writer );
            log.info( "Download Completed" );
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }

        return outputFileFullPath;
    }

    /**
     * parse the disease ontology file to create the structure needed (omimIdToPhenotypeMapping and
     * meshIdToPhenotypeMapping). We download the DO OBO file since it is easy to parse for MESH and OMIM terms.
     * This isn't a default part of startup because not all importers need it.
     */
    void loadMESHOMIM2DOMappings() throws IOException {

        // download the disease Ontology File, or use an existing one
        File doobo = new File( WRITE_FOLDER + File.separator + "doid.obo" );
        if ( doobo.exists() && doobo.canRead() ) {
            log.info( "Using existing doid.obo file for getting MESH and OMIM mappings to DOID" );
        } else {
            log.info( "Downloading doid.obo file for getting MESH and OMIM mappings to DOID" );
            downloadFileFromWeb( DISEASE_ONT_PATH, DISEASE_ONT_OBO_FILE, WRITE_FOLDER, "doid.obo" );
        }

        Set<String> omimIds = new HashSet<>();
        Set<String> meshIds = new HashSet<>();
        String valueUri = null;

        try ( BufferedReader br = new BufferedReader( new FileReader( doobo ) ) ) {

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
                    } else if ( line.contains( "xref: MSH" ) || line.contains( "xref: MESH" ) ) {
                        tokens = line.split( ":" );
                        meshIds.add( tokens[2].trim() );
                    }

                    // end of a term
                    else if ( line.equalsIgnoreCase( "" ) ) {

                        foundTerm = false;

                        for ( String omimId : omimIds ) {

                            Set<String> h = new HashSet<>();
                            String key = "OMIM:" + omimId;

                            h = checkAndAddValue( valueUri, h, key );
                            omimAndmesh2DO.put( key, h );
                        }

                        for ( String meshId : meshIds ) {

                            String key = "MESH:" + meshId;

                            Set<String> h = new HashSet<>();

                            h = checkAndAddValue( valueUri, h, key );
                            omimAndmesh2DO.put( key, h );
                        }
                    }
                }
            }
        }
    }

    /***
     * write all found in set to files and close files. the reason this is done that way is to not have duplicates for 2
     * files
     *
     * @throws IOException  IO problems
     */
    void writeBuffersAndCloseFiles() throws IOException {

        // write all possible mapping found
        log.info( "Writing to output..." );
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

    /**
     * is the valueUri existing or obsolete ?
     *
     * @param valueUri the value uri of term to check
     * @return term exists and is not obsolete
     */
    private boolean existsAndNotObsolete( String valueUri ) {
        OntologyTerm o = diseaseOntologyService.getTerm( valueUri );
        if ( o == null ) {
            o = humanPhenotypeOntologyService.getTerm( valueUri );
        }
        return o != null && !o.isObsolete();
    }

    /**
     * Initialization
     *
     * @throws IOException IO problems
     */
    private void parseDescriptionToIgnore() throws IOException {

        try ( BufferedReader br = new BufferedReader(
                new InputStreamReader( new ClassPathResource( DESCRIPTION_TO_IGNORE ).getInputStream() ) ) ) {

            String line;

            while ( ( line = br.readLine() ) != null ) {

                line = this.removeSmallNumberAndTxt( line );

                descriptionToIgnore.add( this.removeQuotes( line ).trim() );
            }
        }
    }

    /**
     * Initialization
     *
     * @throws IOException IO problems
     */
    private void parseManualMappingFile() throws IOException {

        try ( BufferedReader br = new BufferedReader(
                new InputStreamReader( new ClassPathResource( MANUAL_MAPPING ).getInputStream() ) ) ) {

            String line;
            // skip first line, the headers
            br.readLine();

            // reads the manual file and put the data in a structure
            while ( ( line = br.readLine() ) != null ) {

                Set<String> col = new HashSet<>();

                String[] tokens = line.split( "\t" );

                String termId = tokens[0].trim().toLowerCase();
                String valueUriStaticFile = tokens[1].trim();
                String valueStaticFile = tokens[2].trim();

                OntologyTerm ontologyTerm = this.findOntologyTermExistAndNotObsolete( valueUriStaticFile );

                if ( ontologyTerm != null ) {

                    if ( valueStaticFile.equalsIgnoreCase( ontologyTerm.getLabel() ) ) {

                        if ( manualDescriptionToValuesUriMapping.get( termId ) != null ) {
                            col = manualDescriptionToValuesUriMapping.get( termId );
                        }

                        col.add( valueUriStaticFile );

                        manualDescriptionToValuesUriMapping.put( termId, col );

                        keyToDescription.put( termId, valueStaticFile );

                    } else {
                        writeError( "Manual ValueURI and Value don't match in file: " + valueStaticFile + "; Expected: "
                                + ontologyTerm.getLabel() );
                    }
                } else {
                    writeError( "Manual mapping file term obsolete or missing: '" + valueUriStaticFile + "' " + " ("
                            + valueStaticFile + ")" );
                }
            }
        }
    }

    /**
     * Initialization parse file and returns a collection of terms found
     *
     * @throws IOException IO problems
     */
    private void parseResultsToIgnore() throws IOException {

        InputStream resultsToIgnoreStream = PhenotypeProcessingUtil.class.getResourceAsStream( RESULTS_TO_IGNORE );
        assert resultsToIgnoreStream != null;

        try ( BufferedReader br = new BufferedReader( new InputStreamReader( resultsToIgnoreStream ) ) ) {
            String line;

            while ( ( line = br.readLine() ) != null ) {
                String[] tokens = line.split( "\t" );
                resultsToIgnore.add( removeQuotes( tokens[0] ) );
            }
        }

    }

    private String changeMedicToUri( String medicTerm ) {

        String randomUri = medicOntologyService.getAllURIs().iterator().next();

        return randomUri.substring( 0, randomUri.lastIndexOf( "/" ) + 1 ) + medicTerm.replace( ':', '_' );
    }

    /*
     * TODO ????
     */
    private String changeToId( String valueUri ) {
        return valueUri.substring( valueUri.lastIndexOf( "/" ) + 1 ).replace( '_', ':' );
    }

    /**
     * @param valueUri value uri
     * @param h        will be modified, but then also returned
     * @param key      key
     * @return h
     */
    private Set<String> checkAndAddValue( String valueUri, Set<String> h, String key ) {
        if ( PhenotypeProcessingUtil.omimAndmesh2DO.get( key ) == null ) {
            if ( existsAndNotObsolete( valueUri ) ) {
                h.add( valueUri );
            }
        } else {
            h = PhenotypeProcessingUtil.omimAndmesh2DO.get( key );
            if ( existsAndNotObsolete( valueUri ) ) {
                h.add( valueUri );
            }
        }
        return h;
    }

    /**
     * Find a description (label) based on an OMIM or MESH id
     *
     * @param meshOrOmimId mesh or omim id
     * @return result or null if nothing was found.
     */
    private String findDescriptionUsingTerm( String meshOrOmimId ) {

        if ( unmappableIds.contains( meshOrOmimId ) )
            return null;

        OntologyTerm ontologyTerm = medicOntologyService.getTerm( this.changeMedicToUri( meshOrOmimId ) );

        if ( ontologyTerm != null ) {
            return ontologyTerm.getLabel();
        }

        String conceptId = meshOrOmimId.substring( meshOrOmimId.indexOf( ":" ) + 1 );

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
            label = AnnotatorClient.findLabelForIdentifier( "OMIM", conceptId );
        } else if ( meshOrOmimId.contains( "MESH:" ) ) {
            label = AnnotatorClient.findLabelForIdentifier( "MESH", conceptId );
        } else {
            log.debug( "diseaseId not OMIM or MESH: " + meshOrOmimId );
            return null;
        }

        if ( label == null ) {
            log.warn( "No mapping found for " + meshOrOmimId );
            unmappableIds.add( meshOrOmimId );
            return null;
        }

        omimIDToLabel.put( conceptId, label );

        return label;
    }

    private String findExtraInfoMeshDescription( String keyword ) {

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

    /**
     * @param termId the search key will only work if lower case, the key is always lower case, this method take care of it
     * @return collection of mappings
     */
    private Collection<String> findManualMappingTermValueUri( String termId ) {
        return manualDescriptionToValuesUriMapping.get( termId.toLowerCase() );
    }

    /**
     * @param proposedID       Either MESH, OMIM or DO (we are trying to map to DO)
     * @param gene             gene
     * @param pubmed           pubmed
     * @param evidenceCode     evidence code
     * @param description      description
     * @param annotatorKeyword annotator keyword
     * @param externalDatabase external database
     * @param databaseLink     database link
     * @param onParents        on parents
     * @return false if not found
     * @throws Exception IO problems
     */
    private boolean findMapping( String proposedID, Gene gene, String pubmed, String evidenceCode, String description,
            String annotatorKeyword, String externalDatabase, String databaseLink, Collection<OntologyTerm> onParents )
            throws Exception {

        boolean mappingFound = false;

        String keywordSearchMeshOrOmimIdLabel = "";

        if ( proposedID != null && !proposedID.isEmpty() ) {

            if ( proposedID.startsWith( "DOID:" ) || proposedID.startsWith( "DOID_" ) ) {
                OntologyTerm term = diseaseOntologyService
                        .getTerm( "http://purl.obolibrary.org/obo/" + proposedID.replace( "DOID:", "DOID_" ) );

                if ( term != null ) {

                    outFinalResults.write( gene.getOfficialSymbol() + "\t" + gene.getNcbiGeneId() + "\t" + pubmed + "\t"
                            + evidenceCode + "\t" + description + "\t" + externalDatabase + "\t" + databaseLink + "\t"
                            + PhenotypeMappingType.DIRECT.getValue() + "\t" + proposedID + "\t" + term.getUri()
                            + "\n" );
                    return true;
                }
                return false;
            }

            // step 1 using omim or mesh id look in the disease file for annotation
            if ( proposedID.startsWith( "MESH:" ) || proposedID.startsWith( "" ) ) {
                mappingFound = this.findOmimMeshInDiseaseOntology( proposedID, gene, pubmed, evidenceCode, description,
                        externalDatabase, databaseLink, onParents );
            }
        }

        // step 2, the manual mapping file, by OMIM id, MESH or by description if no mesh was given
        if ( !mappingFound ) {
            mappingFound = this
                    .findUsingManualMappingFile( proposedID, annotatorKeyword, gene, pubmed, evidenceCode, description,
                            externalDatabase, databaseLink, onParents );
        }

        // search with the given keyword, usually the description
        if ( StringUtils.isNotBlank( annotatorKeyword ) ) {

            // step 3a, use the annotator
            if ( !mappingFound ) {
                mappingFound = this
                        .findWithAnnotator( proposedID, annotatorKeyword, externalDatabase, false, onParents, null );
            }

            // step 3b, use the annotator modify the search
            if ( !mappingFound ) {

                // same thing but lets modify the search
                this.findWithAnnotator( proposedID, annotatorKeyword, externalDatabase, true, onParents, null );
            }
        }

        // lets find the label of the identifier used
        if ( proposedID != null && !proposedID.isEmpty() ) {
            keywordSearchMeshOrOmimIdLabel = this.findDescriptionUsingTerm( proposedID );
        }

        // search with the label for extra chance of success, found using an OMIM or MESH id
        // if the description given is different than the label found in the ontology
        if ( keywordSearchMeshOrOmimIdLabel != null && !keywordSearchMeshOrOmimIdLabel
                .equalsIgnoreCase( annotatorKeyword ) ) {

            // step 3a, use the annotator
            if ( !mappingFound ) {
                mappingFound = this
                        .findWithAnnotator( proposedID, keywordSearchMeshOrOmimIdLabel, externalDatabase, false,
                                onParents, null );
            }

            // step 3b, use the annotator modify the search
            if ( !mappingFound ) {

                // same thing but lets modify the search
                this.findWithAnnotator( proposedID, keywordSearchMeshOrOmimIdLabel, externalDatabase, true, onParents,
                        null );
            }
        }

        return mappingFound;

    }

    /**
     * step 1 using an OMIM or MESH to link to a DO id
     */
    private boolean findOmimMeshInDiseaseOntology( String meshOrOmimId, Gene gene, String pubmed, String evidenceCode,
            String description, String externalDatabase, String databaseLink, Collection<OntologyTerm> onParents )
            throws Exception {

        String mappingType;
        StringBuilder valuesUri = new StringBuilder();
        StringBuilder originalPhenotype = new StringBuilder( meshOrOmimId );

        // this just provides a text version of the term, but only if it is a recognized mesh or omim id.
        String meshOrOmimIdValue = this.findDescriptionUsingTerm( meshOrOmimId );

        // use the ontology to find description
        if ( meshOrOmimIdValue != null ) {
            originalPhenotype.append( " (" ).append( meshOrOmimIdValue.toLowerCase() ).append( ")" );
        }

        // using parents
        if ( onParents != null ) {

            mappingType = PhenotypeMappingType.INFERRED_XREF.toString();

            Map<String, Collection<OntologyTerm>> dieaseOn = this.meshToDiseaseTerms( onParents );

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

    /**
     * @param diseaseId MESH or OMIM id
     * @return collection of ontology terms
     */
    private Collection<OntologyTerm> findOntologyTermsUriWithDiseaseId( String diseaseId ) {

        Collection<OntologyTerm> terms = new HashSet<>();
        Collection<String> valuesUri = omimAndmesh2DO.get( diseaseId );

        if ( valuesUri != null && !valuesUri.isEmpty() ) {

            for ( String valueUri : valuesUri ) {

                OntologyTerm on = this.findOntologyTermExistAndNotObsolete( valueUri );

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
                String termUri = o.getUri();
                if ( termUri == null ) {
                    log.warn( "Ignoring free-text term " + o );
                    continue;
                }
                String meshId = this.changeToId( termUri );
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

    /**
     * step 3 - find using "free text" search via annotator.
     *
     * @param meshOrOmimId     mesh or omim id
     * @param keywordQuery     keyword query
     * @param externalDatabase external database
     * @param modifySearch     modify search
     * @param onParents        will be checked if nothing found directly
     * @param child            FIXME not actually used, should be removed.
     * @return false when not found
     * @throws IOException                  IO problems
     * @throws SAXException                 sax parsing problems
     * @throws ParserConfigurationException configuration problems
     * @throws IllegalStateException        TODO
     */
    private boolean findWithAnnotator( String meshOrOmimId, String keywordQuery, String externalDatabase,
            boolean modifySearch, Collection<OntologyTerm> onParents, String child )
            throws IOException, IllegalStateException, ParserConfigurationException, SAXException {

        if ( StringUtils.isBlank( keywordQuery ) ) {
            log.debug( "Blank keyword query; ID was " + meshOrOmimId ); // seems always null?
            return false;
        }

        String usedChild = "";

        if ( child == null ) {
            usedChild = null;
        }

        if ( onParents != null ) {

            for ( OntologyTerm o : onParents ) {
                boolean found = this
                        .findWithAnnotator( o.getLabel(), keywordQuery, externalDatabase, modifySearch, null,
                                meshOrOmimId );
                if ( found ) {
                    return true;
                }
            }
            return false;

        }

        String searchTerm = keywordQuery.toLowerCase();
        Collection<AnnotatorResponse> annotatorResponses = null;

        String key = meshOrOmimId;

        // we are not dealing with an omim identifier, gwas using this case for example
        if ( key == null ) {
            key = keywordQuery;
        }

        if ( modifySearch ) {
            searchTerm = removeSpecificKeywords( keywordQuery.toLowerCase() );
        }

        if ( StringUtils.isBlank( searchTerm ) ) {
            log.warn( "Search term ended up blank: " + keywordQuery );
            return false;
        }

        if ( !descriptionToIgnore.contains( searchTerm ) ) {

            // we already know the answer for this term
            if ( cacheAnswerFromAnnotator.containsKey( searchTerm ) ) {
                return false;
            }

            // search with the annotator and filter result to take out obsolete terms given

            annotatorResponses = this.removeNotExistAndObsolete( AnnotatorClient.findTerm( searchTerm ) );

            if ( annotatorResponses != null && !annotatorResponses.isEmpty() ) {
                cacheAnswerFromAnnotator.put( searchTerm, annotatorResponses );

                AnnotatorResponse annotatorResponse = annotatorResponses.iterator().next();
                String condition = annotatorResponse.findCondition( modifySearch );

                if ( condition != null ) {

                    OntologyTerm on = this.findOntologyTermExistAndNotObsolete( annotatorResponse.getValueUri() );

                    if ( on != null ) {

                        searchTerm = AnnotatorClient.removeSpecialCharacters( searchTerm ).replaceAll( "\\+", " " );

                        if ( modifySearch ) {
                            searchTerm = searchTerm + "   (" + keywordQuery + ")";
                        }

                        // FIXME usedChild is always either blank or null?
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
                        + keywordQuery + ")\t" );

        if ( annotatorResponses != null && !annotatorResponses.isEmpty() ) {

            for ( AnnotatorResponse ar : annotatorResponses ) {
                logRequestAnnotator.write( ar.getTxtMatched() + ";   " );
            }
        }

        logRequestAnnotator.write( "\n" );
        logRequestAnnotator.flush();

        return false;
    }

    /**
     * For inclusion in path names
     *
     * @return todays date
     */
    private String getTodayDate() {
        DateFormat dateFormat = new SimpleDateFormat( "yyyy-MM-dd_HH.mm", Locale.ENGLISH );
        Calendar cal = Calendar.getInstance();
        return dateFormat.format( cal.getTime() );
    }

    /**
     * Initialize additional files for output. all imported can have up to 3 outputFiles, that are the results
     *
     * @param prefix      prefix
     * @param writeFolder write folder
     * @throws IOException IO problems
     */
    private void initOutputfiles( String prefix, String writeFolder ) throws IOException {

        // 1- the final results
        this.initFinalOutputFile( prefix, writeFolder, false, false );

        // 2- results found with the annotator need to be verify and if ok moved to a manual annotation file
        String outputFilePath = writeFolder + File.separator + "mappingsFound.tsv";
        log.info( "New mappings will be written to " + outputFilePath + "; add them to ManualDescriptionMapping.tsv" );
        outMappingFound = new BufferedWriter( new FileWriter( outputFilePath ) );
        outMappingFound
                .write( "Identifier (KEY)\tPhenotype valueUri (THE KEY MAP TO THIS)\tPhenotype value\tSearch Term used in annotator\tChild Term\tHow we found the mapping\tSource\n" );

        // 3- this a log of request sent to annotator
        logRequestAnnotator = new BufferedWriter(
                new FileWriter( writeFolder + File.separator + "logRequestAnnotatorNotFound.tsv" ) );
    }

    /**
     * load all needed ontologies
     *
     * @throws Exception problems
     */
    private synchronized void init() throws Exception {
        medicOntologyService.startInitializationThread( true, false );
        diseaseOntologyService.startInitializationThread( true, false );
        humanPhenotypeOntologyService.startInitializationThread( true, false );

        int waited = 0;
        int MAX_WAIT_TIME = 60000;
        int WAIT_TIME = 5000;
        while ( !diseaseOntologyService.isOntologyLoaded() && waited * WAIT_TIME < MAX_WAIT_TIME ) {
            this.wait( WAIT_TIME );
            log.info( "Waiting for the Disease Ontology to load" );
            waited++;
        }

        if ( diseaseOntologyService.isOntologyLoaded() ) {
            log.info( "DO loaded" );
        } else {
            throw new IllegalStateException( "DO failed to load in time" );
        }

        waited = 0;
        while ( !humanPhenotypeOntologyService.isOntologyLoaded() && waited * WAIT_TIME < MAX_WAIT_TIME ) {
            this.wait( WAIT_TIME );
            log.info( "Waiting for the HP Ontology to load" );
            waited++;
        }

        if ( diseaseOntologyService.isOntologyLoaded() ) {
            log.info( "HP loaded" );
        } else {
            throw new IllegalStateException( "HP failed to load in time" );
        }

        waited = 0;
        while ( !medicOntologyService.isOntologyLoaded() && waited * WAIT_TIME < MAX_WAIT_TIME ) {
            this.wait( WAIT_TIME );
            log.info( "Waiting for the MEDIC (MESH) Ontology to load" );
            waited++;
        }

        if ( diseaseOntologyService.isOntologyLoaded() ) {
            log.info( "MEDIC (MESH) loaded" );
        } else {
            throw new IllegalStateException( "MEDIC failed to load in time" );
        }

        // rest of initialization.

        // results returned by the annotator to ignore
        parseResultsToIgnore();
        // parse the manual mapping file
        parseManualMappingFile();
        // description we know we are not interested in the result
        parseDescriptionToIgnore();
    }

    /**
     * I don't know what is specifically MESH about this - it seems to not care.
     *
     * @param meshTerms mesh terms
     * @return map of meshIds to DO or HPO terms
     */
    private Map<String, Collection<OntologyTerm>> meshToDiseaseTerms( Collection<OntologyTerm> meshTerms ) {

        Map<String, Collection<OntologyTerm>> diseaseTerms = new HashMap<>();

        for ( OntologyTerm m : meshTerms ) {
            String termUri = m.getUri();
            if ( termUri == null ) {
                log.warn( "Ignoring free-text term: " + m );
                continue;
            }

            String meshId = this.changeToId( termUri );

            Collection<OntologyTerm> onDisease = this.findOntologyTermsUriWithDiseaseId( meshId );

            if ( !onDisease.isEmpty() ) {
                diseaseTerms.put( meshId, onDisease );

            }
        }

        return diseaseTerms;
    }

    /**
     * if a excel file was used values sometime save as "value", always take out the quotation marks
     */
    private String removeQuotes( String txt ) {

        String newTxt = txt.replaceAll( "\"", "" );
        return newTxt.trim();
    }

    /**
     * checks if a value uri usually found with the annotator exists or obsolete, filters the results
     */
    private Collection<AnnotatorResponse> removeNotExistAndObsolete(
            Collection<AnnotatorResponse> annotatorResponses ) {

        Collection<AnnotatorResponse> annotatorResponseWithNoObsolete = new TreeSet<>();

        for ( AnnotatorResponse annotatorResponse : annotatorResponses ) {

            if ( existsAndNotObsolete( annotatorResponse.getValueUri() ) && !resultsToIgnore
                    .contains( annotatorResponse.getValueUri() ) ) {
                annotatorResponseWithNoObsolete.add( annotatorResponse );
            }
        }
        return annotatorResponseWithNoObsolete;
    }

    /**
     * special rules used to format the search term, when using a modified search in the annotator
     */
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

    private void writeError( String errorMessage ) {
        log.error( errorMessage );
        // this gives the summary of errors at the end
        logMessages.add( errorMessage );
    }

}
