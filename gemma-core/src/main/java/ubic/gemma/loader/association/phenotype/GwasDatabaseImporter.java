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

import ubic.gemma.model.genome.Gene;

public class GwasDatabaseImporter extends ExternalDatabaseEvidenceImporterAbstractCLI {

    // name of the external database
    public static final String GWAS = "GWAS_Catalog";

    public static final String GWAS_FILE = "gwascatalog.txt";
    // path of file to download
    public static final String GWAS_URL_PATH = "http://www.genome.gov/admin/";

    protected static final String CONTEXT = "Context";
    protected static final Integer CONTEXT_INDEX = 24;
    protected static final String DISEASE_TRAIT = "Disease/Trait";
    protected static final Integer DISEASE_TRAIT_INDEX = 7;
    protected static final String INITIAL_SAMPLE_SIZE = "Initial Sample Size";
    protected static final Integer INITIAL_SAMPLE_SIZE_INDEX = 8;
    protected static final String MAPPED_GENE = "Mapped_gene";
    protected static final Integer MAPPED_GENE_INDEX = 14;
    protected static final String OR_OR_BETA = "OR or beta";
    protected static final Integer OR_OR_BETA_INDEX = 30;
    protected static final String P_VALUE = "p-Value";
    protected static final Integer P_VALUE_INDEX = 27;
    protected static final String PLATFORM = "Platform [SNPs passing QC]";
    protected static final Integer PLATFORM_INDEX = 32;
    // names and positions of the headers, this will be check with the file to verify all headers
    protected static final String PUBMED_ID = "PUBMEDID";
    protected static final Integer PUBMED_ID_INDEX = 1;
    protected static final String REPLICATION_SAMPLE_SIZE = "Replication Sample Size";
    protected static final Integer REPLICATION_SAMPLE_SIZE_INDEX = 9;
    protected static final String REPORTED_GENES = "Reported Gene(s)";
    protected static final Integer REPORTED_GENES_INDEX = 13;
    protected static final String RISK_ALLELE_FREQUENCY = "Risk Allele Frequency";
    protected static final Integer RISK_ALLELE_FREQUENCY_INDEX = 26;
    protected static final String SNPS = "SNPs";
    protected static final Integer SNPS_INDEX = 21;
    protected static final String STRONGEST_SNP = "Strongest SNP-Risk Allele";
    protected static final Integer STRONGEST_SNP_INDEX = 20;

    public static void main( String[] args ) throws Exception {

        GwasDatabaseImporter importEvidence = new GwasDatabaseImporter( args );

        // creates the folder where to place the file web downloaded files and final output files
        importEvidence.createWriteFolderWithDate( GWAS );

        // download the GWAS file
        String gwasFile = importEvidence.downloadFileFromWeb( GWAS_URL_PATH, GWAS_FILE );

        // process the gwas file
        importEvidence.processGwasFile( gwasFile );
    }

    public GwasDatabaseImporter( String[] args ) throws Exception {
        super( args );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.AbstractCLI#getCommandName()
     */
    @Override
    public String getCommandName() {
        return "gwasImport";
    }

    private void checkHeader( String valueFile, String valueExpected ) throws Exception {

        if ( !valueFile.equalsIgnoreCase( valueExpected ) ) {
            throw new Exception( "Wrong header found in file, expected: " + valueExpected + "  found:" + valueFile );
        }
    }

    // the rules to choose the gene symbol
    private String findGeneSymbol( String reportedGene, String mappedGene ) throws Exception {

        // TODO
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

        try (BufferedReader br = new BufferedReader( new FileReader( gwasFile ) );) {

            // check if we have correct headers and organ
            verifyHeaders( br.readLine().split( "\t" ) );

            String line = "";

            // parse the morbid OMIM file
            while ( ( line = br.readLine() ) != null && !line.trim().equalsIgnoreCase( "" ) ) {

                String[] tokens = line.split( "\t" );

                // the geneSymbol found in the file
                String geneSymbol = findGeneSymbol( tokens[REPORTED_GENES_INDEX].trim(),
                        tokens[MAPPED_GENE_INDEX].trim() );

                if ( geneSymbol == null ) {
                    continue;
                }

                // case 1: can we map the description with the static file
                String description = tokens[DISEASE_TRAIT_INDEX].trim();

                String comment = DISEASE_TRAIT + ": " + tokens[DISEASE_TRAIT_INDEX] + "; " + INITIAL_SAMPLE_SIZE + ": "
                        + tokens[INITIAL_SAMPLE_SIZE_INDEX] + "; " + REPLICATION_SAMPLE_SIZE + ": "
                        + tokens[REPLICATION_SAMPLE_SIZE_INDEX] + "; " + STRONGEST_SNP + ": "
                        + tokens[STRONGEST_SNP_INDEX] + "; " + SNPS + ": " + tokens[SNPS_INDEX] + "; " + CONTEXT + ": "
                        + tokens[CONTEXT_INDEX] + "; " + RISK_ALLELE_FREQUENCY + ": "
                        + tokens[RISK_ALLELE_FREQUENCY_INDEX] + "; " + P_VALUE + ": " + tokens[P_VALUE_INDEX] + "; "
                        + OR_OR_BETA + ": " + tokens[OR_OR_BETA_INDEX] + "; " + PLATFORM + ": "
                        + tokens[PLATFORM_INDEX];

                String pubmed = tokens[PUBMED_ID_INDEX];

                Gene gene = findGeneUsingSymbolandTaxon( geneSymbol, "human" );

                if ( gene != null ) {
                    findMapping( null, gene, pubmed, "TAS", comment, description, GWAS, "?gene=" + geneSymbol );
                }
            }

            br.close();
            writeBuffersAndCloseFiles();
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
        checkHeader( headers[PUBMED_ID_INDEX], PUBMED_ID );
        checkHeader( headers[DISEASE_TRAIT_INDEX], DISEASE_TRAIT );
        checkHeader( headers[INITIAL_SAMPLE_SIZE_INDEX], INITIAL_SAMPLE_SIZE );
        checkHeader( headers[REPLICATION_SAMPLE_SIZE_INDEX], REPLICATION_SAMPLE_SIZE );
        checkHeader( headers[REPORTED_GENES_INDEX], REPORTED_GENES );
        checkHeader( headers[MAPPED_GENE_INDEX], MAPPED_GENE );
        checkHeader( headers[STRONGEST_SNP_INDEX], STRONGEST_SNP );
        checkHeader( headers[SNPS_INDEX], SNPS );
        checkHeader( headers[CONTEXT_INDEX], CONTEXT );
        checkHeader( headers[RISK_ALLELE_FREQUENCY_INDEX], RISK_ALLELE_FREQUENCY );
        checkHeader( headers[P_VALUE_INDEX], P_VALUE );
        checkHeader( headers[OR_OR_BETA_INDEX], OR_OR_BETA );
        checkHeader( headers[PLATFORM_INDEX], PLATFORM );
    }

}