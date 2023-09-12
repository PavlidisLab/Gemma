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
import ubic.basecode.util.FileTools;
import ubic.gemma.core.apps.GemmaCLI.CommandGroup;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

@Deprecated
public class RgdDatabaseImporterCli extends ExternalDatabaseEvidenceImporterAbstractCLI {

    private static final String RGD_FILE_HUMAN = "homo_genes_rdo";

    private static final String RGD_FILE_MOUSE = "mus_genes_rdo";
    private static final String RGD_FILE_RAT = "rattus_genes_rdo";
    // path of files to download
    private static final String RGD_URL_PATH = "ftp://ftp.rgd.mcw.edu/pub/data_release/annotated_rgd_objects_by_ontology/";
    // name of the external database
    private static final String RGD = "RGD";

    @Override
    public String getCommandName() {
        return "rgdDownload";
    }

    @Override
    public CommandGroup getCommandGroup() {
        return CommandGroup.PHENOTYPES;
    }

    @Override
    protected void buildOptions( Options options ) {
        // No-op
    }

    @Override
    protected void doWork() throws Exception {
        // this gets the context, so we can access beans
        super.init();

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
                    .downloadFileFromWeb( RgdDatabaseImporterCli.RGD_URL_PATH, RgdDatabaseImporterCli.RGD_FILE_RAT, writeFolder,
                            RGD_FILE_RAT + ".tsv" );

            // find the OMIM and Mesh terms from the disease ontology file
            ppUtil.loadMESHOMIM2DOMappings();

            // process the rgd files
            this.processRGDFiles( rgdHuman, rgdMouse, rgdRat );
        } catch ( Exception e ) {
            e.printStackTrace();
        }
    }

    @Override
    public String getShortDesc() {
        return "Creates a .tsv file of lines of evidence from RGD, to be used with evidenceImport to import into Phenocarta.";
    }

    @Override
    protected void processOptions( CommandLine commandLine ) {

    }

    private void processRGDFile( String taxonName, String fileName ) throws Exception {

        Taxon taxon = taxonService.findByCommonName( taxonName );

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

                Gene gene = null;
                try {
                    gene = geneService.findByOfficialSymbol( geneSymbol, taxon );
                } catch ( org.hibernate.NonUniqueResultException e ) { // temporary
                    Collection<Gene> nonuniques = geneService.findByOfficialSymbol( geneSymbol );
                    log.info( "Multiple " + taxonName + " genes matched " + geneSymbol );

                    for ( Gene gene2 : nonuniques ) {
                        if ( gene2.getTaxon().equals( taxon ) ) {
                            System.err.println( gene2 );
                        }
                    }
                    continue;
                }

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