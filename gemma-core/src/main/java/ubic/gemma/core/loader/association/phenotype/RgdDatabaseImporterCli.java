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

import ubic.basecode.util.FileTools;
import ubic.gemma.core.apps.GemmaCLI.CommandGroup;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

public class RgdDatabaseImporterCli extends ExternalDatabaseEvidenceImporterAbstractCLI {

    private static final String RGD_FILE_HUMAN = "homo_genes_rdo";

    private static final String RGD_FILE_MOUSE = "mus_genes_rdo";
    private static final String RGD_FILE_RAT = "rattus_genes_rdo";
    // path of files to download
    private static final String RGD_URL_PATH = "ftp://ftp.rgd.mcw.edu/pub/data_release/annotated_rgd_objects_by_ontology/";
    // name of the external database
    private static final String RGD = "RGD";

    @SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
    public RgdDatabaseImporterCli() throws Exception {
        super();
    }

    public static void main( String[] args ) throws Exception {

        RgdDatabaseImporterCli importEvidence = new RgdDatabaseImporterCli();
        Exception e = importEvidence.doWork( args );
        if ( e != null ) {
            e.printStackTrace();
        }

    }

    @Override
    public String getCommandName() {
        return "rgdDownload";
    }

    @Override
    public CommandGroup getCommandGroup() {
        return CommandGroup.PHENOTYPES;
    }

    @Override
    protected void buildOptions() {
        // No-op
    }

    @Override
    protected Exception doWork( String[] args ) {
        // this gets the context, so we can access beans
        Exception e1 = super.processCommandLine( args );
        if ( e1 != null ) return e1;
        e1 = super.init();
        if ( e1 != null ) return e1;

        try {
            // creates the folder where to place the file web downloaded files and final output files
            this.writeFolder = ppUtil.createWriteFolderWithDate( RgdDatabaseImporterCli.RGD );

            String rgdHuman = ppUtil
                    .downloadFileFromWeb( RgdDatabaseImporterCli.RGD_URL_PATH, RgdDatabaseImporterCli.RGD_FILE_HUMAN, writeFolder,
                            RGD_FILE_HUMAN + ".tsv" );
            String rgdMouse = ppUtil
                    .downloadFileFromWeb( RgdDatabaseImporterCli.RGD_URL_PATH, RgdDatabaseImporterCli.RGD_FILE_MOUSE, writeFolder,
                            RGD_FILE_MOUSE + ".tsv" );
            String rgdRat = ppUtil
                    .downloadFileFromWeb( RgdDatabaseImporterCli.RGD_URL_PATH, RgdDatabaseImporterCli.RGD_FILE_RAT, writeFolder, RGD_FILE_RAT + ".tsv" );

            // find the OMIM and Mesh terms from the disease ontology file
            ppUtil.findOmimAndMeshMappingUsingOntologyFile( writeFolder );

            // process the rgd files
            this.processRGDFiles( rgdHuman, rgdMouse, rgdRat );
        } catch ( Exception e ) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public String getShortDesc() {
        return "Creates a .tsv file of lines of evidence from RGD, to be used with evidenceImport to import into Phenocarta.";
    }

    @Override
    protected void processOptions() {
        super.processOptions();
    }

    private void processRGDFile( String taxon, String fileName ) throws Exception {

        Taxon rat = taxonService.findByCommonName( "rat" );

        BufferedReader br = new BufferedReader(
                new InputStreamReader( FileTools.getInputStreamFromPlainOrCompressedFile( fileName ) ) );

        String line = null;

        //!{ As of December 2016, the gene_association.rgd file only contains 'RGD' in column 1 and RGD gene identifiers in column 2. } + 

        int numLines = 0;
        int numUsableLines = 0;
        // reads the manual file and put the data in a structure
        while ( ( line = br.readLine() ) != null ) {
            if ( line.indexOf( '!' ) != -1 ) {
                continue;
            }

            String[] tokens = line.split( "\t" );

            String geneSymbol = this.removeSpecialSymbol( tokens[2] ).trim();

            // this contains multiple pubmeds delimited by |
            String rawpubmedIDs = tokens[5];
            List<String> pmids = new ArrayList<>();
            for ( String pubmedID : rawpubmedIDs.split( "|" ) ) {
                if ( pubmedID.startsWith( "PMID:" ) ) {
                    pubmedID = pubmedID.replaceFirst( "PMID:", "" );
                    pmids.add( pubmedID );
                }
            }
            String pubmedReady = StringUtils.join( pmids, ";" ); // separation by semicolon should be more clearly documented... see EvidenceImporterCLI

            String evidenceCode = tokens[6].trim();
            String comment = tokens[3].trim();
            String databaseLink = "?term=" + tokens[4].trim() + "&id=" + tokens[1].trim(); // I don't know what is expected to be in field 4, but currently it is the DOID
            //  String meshOrOmimId = tokens[10].trim();
            String doID = tokens[4];

            if ( !evidenceCode.equalsIgnoreCase( "ISS" ) && !evidenceCode.equalsIgnoreCase( "NAS" ) && !evidenceCode
                    .equalsIgnoreCase( "IEA" ) && !doID.equals( "" ) && StringUtils.isBlank( pubmedReady ) ) {

                //   Gene gene = ppUtil.findGeneUsingSymbolandTaxon( geneSymbol, taxon );
                Gene gene = geneService.findByOfficialSymbol( geneSymbol, rat );

                if ( gene != null ) {

                    if ( ppUtil.findMapping( doID, gene, pubmedReady, evidenceCode, comment, null, RgdDatabaseImporterCli.RGD,
                            databaseLink ) ) {
                        numUsableLines++;
                    }
                }
            }
            numLines++;

        }

        log.info( "Parsed and found mappings for " + numUsableLines + "/" + numLines + " RDG records" );
    }

    private void processRGDFiles( String rgdHuman, String rgdMouse, String rgdRat ) throws Exception {
        this.processRGDFile( "human", rgdHuman );
        this.processRGDFile( "mouse", rgdMouse );
        this.processRGDFile( "rat", rgdRat );
        ppUtil.writeBuffersAndCloseFiles();
    }

    private String removeSpecialSymbol( String geneId ) {
        int index1 = geneId.indexOf( "<sup>" );

        if ( index1 != -1 ) {
            return geneId.substring( 0, index1 );
        }
        return geneId;
    }
}