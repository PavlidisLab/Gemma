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

import ubic.gemma.core.apps.GemmaCLI.CommandGroup;
import ubic.gemma.model.genome.Gene;

import java.io.BufferedReader;
import java.io.FileReader;

public class GwasDatabaseImporter extends ExternalDatabaseEvidenceImporterAbstractCLI {

    // name of the external database
    private static final String GWAS = "GWAS_Catalog";

    private static final String GWAS_FILE = "gwascatalog.txt";
    // path of file to download
    private static final String GWAS_URL_PATH = "http://www.genome.gov/admin/";

    private static final String CONTEXT = "Context";
    private static final Integer CONTEXT_INDEX = 24;
    private static final String DISEASE_TRAIT = "Disease/Trait";
    private static final Integer DISEASE_TRAIT_INDEX = 7;
    private static final String INITIAL_SAMPLE_SIZE = "Initial Sample Description"; // *** changed from Initial Sample
    // Size
    // to reflect updated file
    private static final Integer INITIAL_SAMPLE_SIZE_INDEX = 8;

    private static final String REPLICATION_SAMPLE_SIZE = "Replication Sample Description"; // *** changed from
    // Replication Sample Size
    // to reflect updated file

    private static final String MAPPED_GENE = "Mapped_gene";
    private static final Integer MAPPED_GENE_INDEX = 14;
    private static final String OR_OR_BETA = "OR or beta";
    private static final Integer OR_OR_BETA_INDEX = 30;
    private static final String P_VALUE = "p-Value";
    private static final Integer P_VALUE_INDEX = 27;
    private static final String PLATFORM = "Platform [SNPs passing QC]";
    private static final Integer PLATFORM_INDEX = 32;
    // names and positions of the headers, this will be check with the file to verify all headers
    private static final String PUBMED_ID = "PUBMEDID";
    private static final Integer PUBMED_ID_INDEX = 1;
    private static final Integer REPLICATION_SAMPLE_SIZE_INDEX = 9;
    private static final String REPORTED_GENES = "Reported Gene(s)";
    private static final Integer REPORTED_GENES_INDEX = 13;
    private static final String RISK_ALLELE_FREQUENCY = "Risk Allele Frequency";
    private static final Integer RISK_ALLELE_FREQUENCY_INDEX = 26;
    private static final String SNPS = "SNPs";
    private static final Integer SNPS_INDEX = 21;
    private static final String STRONGEST_SNP = "Strongest SNP-Risk Allele";
    private static final Integer STRONGEST_SNP_INDEX = 20;

    @SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
    public GwasDatabaseImporter( String[] args ) throws Exception {
        super( args );
    }

    public static void main( String[] args ) throws Exception {

        GwasDatabaseImporter importEvidence = new GwasDatabaseImporter( args );
        Exception e = importEvidence.doWork( args );
        if ( e != null ) {
            e.printStackTrace();
        }
    }

    @Override
    public String getCommandName() {
        return "gwasImport";
    }

    @Override
    public CommandGroup getCommandGroup() {
        return null;
    }

    @Override
    protected void buildOptions() {
        super.buildOptions();
    }

    @Override
    protected Exception doWork( String[] args ) {
        // creates the folder where to place the file web downloaded files and final output files
        try {
            this.createWriteFolderWithDate( GwasDatabaseImporter.GWAS );
            // download the GWAS file
            String gwasFile = this
                    .downloadFileFromWeb( GwasDatabaseImporter.GWAS_URL_PATH, GwasDatabaseImporter.GWAS_FILE );
            // process the gwas file
            this.processGwasFile( gwasFile );
        } catch ( Exception e ) {
            e.printStackTrace();
            return e;
        }
        return null;
    }

    @Override
    public String getShortDesc() {
        return "Creates a .tsv file of lines of evidence from GWAS publications, to be used with EvidenceImporterCLI.java to import into Phenocarta.";
    }

    @Override
    protected void processOptions() {
        super.processOptions();
    }

    private void checkHeader( String valueFile, String valueExpected ) throws Exception {

        if ( !valueFile.equalsIgnoreCase( valueExpected ) ) {
            throw new Exception( "Wrong header found in file, expected: " + valueExpected + "  found:" + valueFile );
        }
    }

    // the rules to choose the gene symbol
    private String findGeneSymbol( String reportedGene, String mappedGene ) {

        String geneSymbol = null;

        if ( !reportedGene.isEmpty() && !mappedGene.isEmpty() ) {

            if ( reportedGene.equalsIgnoreCase( mappedGene ) ) {
                geneSymbol = reportedGene;
            }
        }

        return geneSymbol;
    }

    // process the gwas file, line by line
    private void processGwasFile( String gwasFile ) throws Exception {

        try (BufferedReader br = new BufferedReader( new FileReader( gwasFile ) )) {

            // check if we have correct headers and organ
            this.verifyHeaders( br.readLine().split( "\t" ) );

            String line;

            // parse the morbid OMIM file
            while ( ( line = br.readLine() ) != null && !line.trim().equalsIgnoreCase( "" ) ) {

                String[] tokens = line.split( "\t" );

                // the geneSymbol found in the file
                String geneSymbol = this.findGeneSymbol( tokens[GwasDatabaseImporter.REPORTED_GENES_INDEX].trim(),
                        tokens[GwasDatabaseImporter.MAPPED_GENE_INDEX].trim() );

                if ( geneSymbol == null ) {
                    continue;
                }

                // case 1: can we map the description with the static file
                String description = tokens[GwasDatabaseImporter.DISEASE_TRAIT_INDEX].trim();

                String comment =
                        GwasDatabaseImporter.DISEASE_TRAIT + ": " + tokens[GwasDatabaseImporter.DISEASE_TRAIT_INDEX]
                                + "; " + GwasDatabaseImporter.INITIAL_SAMPLE_SIZE + ": "
                                + tokens[GwasDatabaseImporter.INITIAL_SAMPLE_SIZE_INDEX] + "; "
                                + GwasDatabaseImporter.REPLICATION_SAMPLE_SIZE + ": "
                                + tokens[GwasDatabaseImporter.REPLICATION_SAMPLE_SIZE_INDEX] + "; "
                                + GwasDatabaseImporter.STRONGEST_SNP + ": "
                                + tokens[GwasDatabaseImporter.STRONGEST_SNP_INDEX] + "; " + GwasDatabaseImporter.SNPS
                                + ": " + tokens[GwasDatabaseImporter.SNPS_INDEX] + "; " + GwasDatabaseImporter.CONTEXT
                                + ": " + tokens[GwasDatabaseImporter.CONTEXT_INDEX] + "; "
                                + GwasDatabaseImporter.RISK_ALLELE_FREQUENCY + ": "
                                + tokens[GwasDatabaseImporter.RISK_ALLELE_FREQUENCY_INDEX] + "; "
                                + GwasDatabaseImporter.P_VALUE + ": " + tokens[GwasDatabaseImporter.P_VALUE_INDEX]
                                + "; " + GwasDatabaseImporter.OR_OR_BETA + ": "
                                + tokens[GwasDatabaseImporter.OR_OR_BETA_INDEX] + "; " + GwasDatabaseImporter.PLATFORM
                                + ": " + tokens[GwasDatabaseImporter.PLATFORM_INDEX];

                String pubmed = tokens[GwasDatabaseImporter.PUBMED_ID_INDEX];

                Gene gene = this.findGeneUsingSymbolandTaxon( geneSymbol, "human" );

                if ( gene != null ) {
                    this.findMapping( null, gene, pubmed, "TAS", comment, description, GwasDatabaseImporter.GWAS,
                            "?gene=" + geneSymbol );
                }
            }

            br.close();
            this.writeBuffersAndCloseFiles();
        }
    }

    @SuppressWarnings("unused")
    private String removeParentheses( String txt ) {

        int index1 = txt.indexOf( "(" );
        int index2 = txt.indexOf( ")" );

        if ( index1 != -1 && index2 != -1 ) {

            return txt.substring( 0, index1 ) + txt.substring( index2 + 1, txt.length() );

        }
        return txt;
    }

    private void verifyHeaders( String[] headers ) throws Exception {
        this.checkHeader( headers[GwasDatabaseImporter.PUBMED_ID_INDEX], GwasDatabaseImporter.PUBMED_ID );
        this.checkHeader( headers[GwasDatabaseImporter.DISEASE_TRAIT_INDEX], GwasDatabaseImporter.DISEASE_TRAIT );
        this.checkHeader( headers[GwasDatabaseImporter.INITIAL_SAMPLE_SIZE_INDEX],
                GwasDatabaseImporter.INITIAL_SAMPLE_SIZE );
        this.checkHeader( headers[GwasDatabaseImporter.REPLICATION_SAMPLE_SIZE_INDEX],
                GwasDatabaseImporter.REPLICATION_SAMPLE_SIZE );
        this.checkHeader( headers[GwasDatabaseImporter.REPORTED_GENES_INDEX], GwasDatabaseImporter.REPORTED_GENES );
        this.checkHeader( headers[GwasDatabaseImporter.MAPPED_GENE_INDEX], GwasDatabaseImporter.MAPPED_GENE );
        this.checkHeader( headers[GwasDatabaseImporter.STRONGEST_SNP_INDEX], GwasDatabaseImporter.STRONGEST_SNP );
        this.checkHeader( headers[GwasDatabaseImporter.SNPS_INDEX], GwasDatabaseImporter.SNPS );
        this.checkHeader( headers[GwasDatabaseImporter.CONTEXT_INDEX], GwasDatabaseImporter.CONTEXT );
        this.checkHeader( headers[GwasDatabaseImporter.RISK_ALLELE_FREQUENCY_INDEX],
                GwasDatabaseImporter.RISK_ALLELE_FREQUENCY );
        this.checkHeader( headers[GwasDatabaseImporter.P_VALUE_INDEX], GwasDatabaseImporter.P_VALUE );
        this.checkHeader( headers[GwasDatabaseImporter.OR_OR_BETA_INDEX], GwasDatabaseImporter.OR_OR_BETA );
        this.checkHeader( headers[GwasDatabaseImporter.PLATFORM_INDEX], GwasDatabaseImporter.PLATFORM );
    }
}