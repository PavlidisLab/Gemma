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

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.lang3.StringUtils;
import ubic.basecode.ontology.ncbo.OmimAnnotatorClient;
import ubic.basecode.util.FileTools;
import ubic.gemma.core.apps.GemmaCLI.CommandGroup;
import ubic.gemma.core.util.AbstractCLI;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.persistence.util.EntityUtils;

import java.io.*;
import java.util.*;

/**
 * *
 *
 * @author nicolas
 */
@Deprecated
public class OmimDatabaseImporterCli extends ExternalDatabaseEvidenceImporterAbstractCLI {

    // name of the external database
    private static final String OMIM = "OMIM";

    private static final String OMIM_FILE_MIM = "mim2gene.txt";

    // ********************************************************************************
    // the OMIM files to download
    private static final String OMIM_FILE_MORBID = "morbidmap";

    // FIXME INVALID URL
    private static final String OMIM_URL_PATH = "ftp://ftp.omim.org/OMIM/";// "ftp://faf.grcf.jhmi.edu/OMIM/";

    // ********************************************************************************

    @Override
    public CommandGroup getCommandGroup() {
        return CommandGroup.PHENOTYPES;
    }

    @Override
    public String getCommandName() {
        return "omimDownload";
    }

    @Override
    public String getShortDesc() {
        return "Creates a .tsv file of lines of evidence from OMIM, to be used with evidenceImport to import into Phenocarta.";
    }

    @Override
    protected void buildOptions( Options options ) {
        // No-op
    }

    @Override
    protected void processOptions( CommandLine commandLine ) {

    }

    @Override
    protected void doWork() throws Exception {

        // this gets the context, so we can access beans
        super.init();

        // creates the folder to place the downloaded files and final output files
        this.writeFolder = ppUtil.createWriteFolderIfDoesntExist( OmimDatabaseImporterCli.OMIM );
        // download the OMIM File called morbid
        String morbidmap = this.ppUtil.downloadFileFromWeb( OmimDatabaseImporterCli.OMIM_URL_PATH, OmimDatabaseImporterCli.OMIM_FILE_MORBID,
                writeFolder,
                OMIM_FILE_MORBID + ".tsv" );
        // download the OMIM File called mim2gene
        String mim2gene = ppUtil
                .downloadFileFromWeb( OmimDatabaseImporterCli.OMIM_URL_PATH, OmimDatabaseImporterCli.OMIM_FILE_MIM, writeFolder,
                        OMIM_FILE_MORBID + ".tsv" );
        // find the OMIM and Mesh terms by download a version of the disease ontology
        ppUtil.loadMESHOMIM2DOMappings();
        // return common publications between a OMIM gene and OMIM phenotype
        Map<Long, Collection<Long>> omimIdToPubmeds = this.findCommonPubmed( morbidmap );
        // process the omim files to create the final output
        this.processOmimFiles( morbidmap, mim2gene, omimIdToPubmeds );
    }

    /**
     * specific to OMIM we need to combine lines with the same ncbiGeneId + omimPhenotypeId
     *
     */
    private void combinePhenotypes() throws IOException {

        // this method will erase the old final file and create one where some lines got combined
        BufferedReader br = new BufferedReader( new InputStreamReader(
                FileTools.getInputStreamFromPlainOrCompressedFile( writeFolder + "/finalResults.tsv" ) ) );

        try ( BufferedWriter bw = new BufferedWriter( new FileWriter( writeFolder + "/finalResultsOmimCombine.tsv" ) ) ) {

            String line = br.readLine();

            // headers
            bw.write( line + "\n" );

            HashMap<String, String> lineCombine = new HashMap<>();

            while ( ( line = br.readLine() ) != null ) {

                String[] tokens = line.split( "\t" );

                String ncbiGeneId = tokens[1];
                String omimPhenotypeId = tokens[6];

                String key = ncbiGeneId + omimPhenotypeId;

                if ( lineCombine.get( key ) == null ) {
                    lineCombine.put( key, line );
                } else {

                    String commonLine = lineCombine.get( key );

                    String valueUri = tokens[9];
                    String valueUri2 = commonLine.split( "\t" )[9];

                    AbstractCLI.log.info( "combine: " + valueUri );

                    String valueUriCombine = this.combineUri( valueUri, valueUri2 );

                    tokens[9] = valueUriCombine;

                    commonLine = StringUtils.join( tokens, "\t" );

                    lineCombine.put( key, commonLine );
                }

            }

            for ( String lineF : lineCombine.values() ) {

                bw.write( lineF + "\n" );
            }

            bw.close();

            File f = new File( writeFolder + "/finalResults.tsv" );
            File fCombine = new File( writeFolder + "/finalResultsOmimCombine.tsv" );
            EntityUtils.renameFile( fCombine, f );
        }
    }

    /**
     */
    private String combineUri( String valueUri, String valueUri2 ) {

        Set<String> combineValue = new HashSet<>();

        for ( String value : valueUri.split( ";" ) ) {

            if ( !value.trim().isEmpty() ) {
                combineValue.add( value );
            }
        }

        for ( String value : valueUri2.split( ";" ) ) {

            if ( !value.trim().isEmpty() ) {
                combineValue.add( value );
            }
        }

        return StringUtils.join( combineValue, ";" );
    }

    /**
     * return all common pubmed between an omimGeneId and a omimPhenotypeId
     *
     */
    private Collection<Long> findCommonPubmed( Long omimGeneId, Long omimPhenotypeId,
            Map<Long, Collection<Long>> omimIdToPubmeds ) {

        Collection<Long> pubmedFromGeneId = omimIdToPubmeds.get( omimGeneId );
        Collection<Long> pubmedFromPhenotypeId = omimIdToPubmeds.get( omimPhenotypeId );

        if ( pubmedFromGeneId != null && pubmedFromPhenotypeId != null ) {
            Collection<Long> commonElements = new HashSet<>( pubmedFromGeneId );
            commonElements.retainAll( pubmedFromPhenotypeId );

            return commonElements;
        }

        return new HashSet<>();
    }

    /**
     * process all OMIM files to get the data out and manipulates it
     *
     */
    private Map<Long, Collection<Long>> findCommonPubmed( String morbidmap )
            throws NumberFormatException, IOException, InterruptedException {

        // all omimID (gene or phenotype)
        Set<Long> allOmimId = new HashSet<>();

        try ( BufferedReader br = new BufferedReader( new FileReader( morbidmap ) ) ) {
            String line;

            // parse the morbid OMIM file
            while ( ( line = br.readLine() ) != null ) {

                String[] tokens = line.split( "\\|" );

                int pos = tokens[0].lastIndexOf( "," );

                // if there is a database link
                if ( pos != -1 ) {

                    String omimPhenotypeId = tokens[0].substring( pos + 1, tokens[0].length() ).trim().split( " " )[0];
                    // OMIM gene id
                    String omimGeneId = tokens[2].trim();

                    if ( ppUtil.notInteger( omimPhenotypeId ) || Integer.parseInt( omimPhenotypeId ) < 100 ) {
                        continue;
                    }

                    allOmimId.add( new Long( omimGeneId ) );
                    allOmimId.add( new Long( omimPhenotypeId ) );
                }
            }
        }

        return this.populateOmimIdsToPubmeds( allOmimId );
    }

    private Map<String, String> parseFileOmimIdToGeneNCBI( String mim2gene ) throws IOException {

        String line;
        Map<String, String> omimIdToGeneNCBI = new HashMap<>();

        try ( BufferedReader br = new BufferedReader( new FileReader( mim2gene ) ) ) {

            while ( ( line = br.readLine() ) != null ) {

                String[] tokens = line.split( "\t" );

                if ( !tokens[2].trim().equals( "-" ) && !tokens[3].trim().equals( "-" ) && !tokens[2].trim()
                        .equals( "" ) && !tokens[3].trim().equals( "" ) ) {
                    omimIdToGeneNCBI.put( tokens[0], tokens[2] );
                }
            }
        }
        return omimIdToGeneNCBI;
    }

    // this gets all publication for a omimID (gene or phenotype)
    private Map<Long, Collection<Long>> populateOmimIdsToPubmeds( Set<Long> allOmimId ) throws InterruptedException {

        // omimID --> all list of publications
        HashMap<Long, Collection<Long>> omimIdToPubmeds = new HashMap<>();

        // HashSet to ArrayList, so no duplicate but can use list methods
        // allOmimId contains all OMIM id (phenotype and gene)
        ArrayList<Long> allOmimIdList = new ArrayList<>( allOmimId );

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

        return omimIdToPubmeds;
    }

    // process all OMIM files to get the data out and manipulates it
    private void processOmimFiles( String morbidmap, String mim2gene, Map<Long, Collection<Long>> omimIdToPubmeds )
            throws Exception {

        // mapping find using mim2gene file, Omim id ---> Gene NCBI
        Map<String, String> omimIdToGeneNCBI = this.parseFileOmimIdToGeneNCBI( mim2gene );

        String line;

        try ( BufferedReader br = new BufferedReader( new FileReader( morbidmap ) ) ) {

            // parse the morbid OMIM file
            while ( ( line = br.readLine() ) != null ) {

                String[] tokens = line.split( "\\|" );

                int pos = tokens[0].lastIndexOf( "," );

                String pubmedIds = "";

                // if there is a database link
                if ( pos != -1 ) {

                    // OMIM description find in file, the annotator use description
                    String description = tokens[0].substring( 0, pos ).trim();

                    // evidence code we will use
                    String evidenceCode = "TAS";
                    // the OMIM id, (also is the database link)
                    String omimPhenotypeId = tokens[0].substring( pos + 1, tokens[0].length() ).trim().split( " " )[0];
                    String omimId = "OMIM:" + omimPhenotypeId;
                    // OMOM gene id
                    String omimGeneId = tokens[2].trim();
                    // omimGeneid ---> ncbi id
                    String ncbiGeneId = omimIdToGeneNCBI.get( omimGeneId );

                    // is the omimGeneId found in the other file
                    if ( ncbiGeneId != null ) {

                        // if there is no omim id given we cannot do anything with this line (happens often)
                        if ( ppUtil.notInteger( omimPhenotypeId ) || Integer.parseInt( omimPhenotypeId ) < 100 ) {
                            continue;
                        }

                        Gene gene = ppUtil.geneService.findByNCBIId( new Integer( ncbiGeneId ) );

                        if ( gene != null ) {

                            Collection<Long> commonsPubmeds = this
                                    .findCommonPubmed( new Long( omimGeneId ), new Long( omimPhenotypeId ),
                                            omimIdToPubmeds );

                            if ( !commonsPubmeds.isEmpty() ) {
                                pubmedIds = StringUtils.join( commonsPubmeds, ";" );
                            }

                            ppUtil.findMapping( omimId, gene, pubmedIds, evidenceCode, description, description,
                                    OmimDatabaseImporterCli.OMIM, omimPhenotypeId );

                        }
                    }
                }
            }

            br.close();

            ppUtil.writeBuffersAndCloseFiles();
        }

        // special thing to do with OMIM, for the same ncbiGeneId + omimPhenotypeId, combine the phenotype
        this.combinePhenotypes();
    }
}