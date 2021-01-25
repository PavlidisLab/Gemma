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
import ubic.gemma.model.genome.Taxon;

import java.io.BufferedReader;
import java.io.FileReader;

public class GwasDatabaseImporterCli extends ExternalDatabaseEvidenceImporterAbstractCLI {

    // name of the external database
    private static final String GWAS = "GWAS_Catalog";

    private static final String GWAS_FILE = "gwas_catalog.tsv";
    // path of file to download; this now directly yields a file of unknown name like gwas_catalog_v1.0-associations_e93_r2018-11-26.tsv
    private static final String GWAS_URL_PATH = "https://www.ebi.ac.uk/gwas/api/search/downloads/full/";

    // see https://www.ebi.ac.uk/gwas/docs/fileheaders#_file_headers_for_catalog_version_1_0_0. Newer version 1.0.2 includes EFO mappings, but that doesn't help us much since we want DO.

    //0 DATE ADDED TO CATALOG   
    //1 PUBMEDID    
    //2 FIRST AUTHOR    
    //3 DATE    
    //4 JOURNAL 
    //5 LINK    
    //6 STUDY   
    //7 DISEASE/TRAIT   
    //8 INITIAL SAMPLE SIZE 
    //9 REPLICATION SAMPLE SIZE 
    //10 REGION  
    //11 CHR_ID  
    //12 CHR_POS 
    //13 REPORTED GENE(S)    
    //14 MAPPED_GENE 
    //15 UPSTREAM_GENE_ID    
    //16 DOWNSTREAM_GENE_ID  
    //17 SNP_GENE_IDS    
    //18 UPSTREAM_GENE_DISTANCE  
    //19 DOWNSTREAM_GENE_DISTANCE    
    //20 STRONGEST SNP-RISK ALLELE   
    //21 SNPS    
    //22 MERGED  
    //23 SNP_ID_CURRENT  
    //24 CONTEXT 
    //25 INTERGENIC  
    //26 RISK ALLELE FREQUENCY   
    //27 P-VALUE 
    //28 PVALUE_MLOG 
    //29 P-VALUE (TEXT)  
    //30 OR or BETA  
    //31 95% CI (TEXT)   
    //32 PLATFORM [SNPS PASSING QC]  
    //33 CNV

    private static final String CONTEXT = "CONTEXT";
    private static final Integer CONTEXT_INDEX = 24;
    private static final String DISEASE_TRAIT = "DISEASE/TRAIT";
    private static final Integer DISEASE_TRAIT_INDEX = 7;
    private static final String INITIAL_SAMPLE_SIZE = "INITIAL SAMPLE SIZE";
    private static final Integer INITIAL_SAMPLE_SIZE_INDEX = 8;
    private static final String REPLICATION_SAMPLE_SIZE = "REPLICATION SAMPLE SIZE";
    private static final Integer REPLICATION_SAMPLE_SIZE_INDEX = 9;
    private static final String MAPPED_GENE = "MAPPED_GENE";
    private static final Integer MAPPED_GENE_INDEX = 14;
    private static final String OR_OR_BETA = "OR or BETA";
    private static final Integer OR_OR_BETA_INDEX = 30;
    private static final String P_VALUE = "P-VALUE";
    private static final Integer P_VALUE_INDEX = 27;
    private static final String PLATFORM = "PLATFORM [SNPS PASSING QC]";
    private static final Integer PLATFORM_INDEX = 32;
    private static final String PUBMED_ID = "PUBMEDID";
    private static final Integer PUBMED_ID_INDEX = 1;
    private static final String REPORTED_GENES = "REPORTED GENE(S)";
    private static final Integer REPORTED_GENES_INDEX = 13;
    private static final String RISK_ALLELE_FREQUENCY = "RISK ALLELE FREQUENCY";
    private static final Integer RISK_ALLELE_FREQUENCY_INDEX = 26;
    private static final String SNPS = "SNPS";
    private static final Integer SNPS_INDEX = 21;
    private static final String STRONGEST_SNP = "STRONGEST SNP-RISK ALLELE";
    private static final Integer STRONGEST_SNP_INDEX = 20;

    @Override
    public String getCommandName() {
        return "gwasDownload";
    }

    @Override
    public CommandGroup getCommandGroup() {
        return CommandGroup.PHENOTYPES;
    }

    @Override
    protected void doWork() throws Exception {
        super.init();

        // creates the folder where to place the file web downloaded files and final output files
        this.writeFolder = ppUtil.createWriteFolderWithDate( GwasDatabaseImporterCli.GWAS );
        // download the GWAS file
        String gwasFile = this.ppUtil
                .downloadFileFromWeb( GwasDatabaseImporterCli.GWAS_URL_PATH, "", writeFolder, GWAS_FILE );
        // process the gwas file
        this.processGwasFile( gwasFile );
    }

    @Override
    public String getShortDesc() {
        return "Creates a .tsv file of lines of evidence from GWAS publications, to be used with evidenceImport to import into Phenocarta.";
    }

    @Override
    protected void processOptions() {
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
        Taxon human = taxonService.findByCommonName( "human" );

        try ( BufferedReader br = new BufferedReader( new FileReader( gwasFile ) ) ) {

            // check if we have correct headers and organ
            this.verifyHeaders( br.readLine().split( "\t" ) );

            String line;

            // parse the morbid OMIM file
            while ( ( line = br.readLine() ) != null && !line.trim().equalsIgnoreCase( "" ) ) {

                String[] tokens = line.split( "\t" );

                // the geneSymbol found in the file
                String geneSymbol = this.findGeneSymbol( tokens[GwasDatabaseImporterCli.REPORTED_GENES_INDEX].trim(),
                        tokens[GwasDatabaseImporterCli.MAPPED_GENE_INDEX].trim() );

                if ( geneSymbol == null ) {
                    continue;
                }

                // case 1: can we map the description with the static file
                String description = tokens[GwasDatabaseImporterCli.DISEASE_TRAIT_INDEX].trim();

                String comment = GwasDatabaseImporterCli.DISEASE_TRAIT + ": " + tokens[GwasDatabaseImporterCli.DISEASE_TRAIT_INDEX]
                        + "; " + GwasDatabaseImporterCli.INITIAL_SAMPLE_SIZE + ": "
                        + tokens[GwasDatabaseImporterCli.INITIAL_SAMPLE_SIZE_INDEX] + "; "
                        + GwasDatabaseImporterCli.REPLICATION_SAMPLE_SIZE + ": "
                        + tokens[GwasDatabaseImporterCli.REPLICATION_SAMPLE_SIZE_INDEX] + "; "
                        + GwasDatabaseImporterCli.STRONGEST_SNP + ": "
                        + tokens[GwasDatabaseImporterCli.STRONGEST_SNP_INDEX] + "; " + GwasDatabaseImporterCli.SNPS
                        + ": " + tokens[GwasDatabaseImporterCli.SNPS_INDEX] + "; " + GwasDatabaseImporterCli.CONTEXT
                        + ": " + tokens[GwasDatabaseImporterCli.CONTEXT_INDEX] + "; "
                        + GwasDatabaseImporterCli.RISK_ALLELE_FREQUENCY + ": "
                        + tokens[GwasDatabaseImporterCli.RISK_ALLELE_FREQUENCY_INDEX] + "; "
                        + GwasDatabaseImporterCli.P_VALUE + ": " + tokens[GwasDatabaseImporterCli.P_VALUE_INDEX]
                        + "; " + GwasDatabaseImporterCli.OR_OR_BETA + ": "
                        + tokens[GwasDatabaseImporterCli.OR_OR_BETA_INDEX] + "; " + GwasDatabaseImporterCli.PLATFORM
                        + ": " + tokens[GwasDatabaseImporterCli.PLATFORM_INDEX];

                String pubmed = tokens[GwasDatabaseImporterCli.PUBMED_ID_INDEX];

                Gene gene = this.geneService.findByOfficialSymbol( geneSymbol, human );

                if ( gene != null ) {
                    ppUtil.findMapping( null, gene, pubmed, "TAS", comment, description, GwasDatabaseImporterCli.GWAS,
                            "?gene=" + geneSymbol );
                }
            }

            br.close();
            ppUtil.writeBuffersAndCloseFiles();
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
        this.checkHeader( headers[GwasDatabaseImporterCli.PUBMED_ID_INDEX], GwasDatabaseImporterCli.PUBMED_ID );
        this.checkHeader( headers[GwasDatabaseImporterCli.DISEASE_TRAIT_INDEX], GwasDatabaseImporterCli.DISEASE_TRAIT );
        this.checkHeader( headers[GwasDatabaseImporterCli.INITIAL_SAMPLE_SIZE_INDEX],
                GwasDatabaseImporterCli.INITIAL_SAMPLE_SIZE );
        this.checkHeader( headers[GwasDatabaseImporterCli.REPLICATION_SAMPLE_SIZE_INDEX],
                GwasDatabaseImporterCli.REPLICATION_SAMPLE_SIZE );
        this.checkHeader( headers[GwasDatabaseImporterCli.REPORTED_GENES_INDEX], GwasDatabaseImporterCli.REPORTED_GENES );
        this.checkHeader( headers[GwasDatabaseImporterCli.MAPPED_GENE_INDEX], GwasDatabaseImporterCli.MAPPED_GENE );
        this.checkHeader( headers[GwasDatabaseImporterCli.STRONGEST_SNP_INDEX], GwasDatabaseImporterCli.STRONGEST_SNP );
        this.checkHeader( headers[GwasDatabaseImporterCli.SNPS_INDEX], GwasDatabaseImporterCli.SNPS );
        this.checkHeader( headers[GwasDatabaseImporterCli.CONTEXT_INDEX], GwasDatabaseImporterCli.CONTEXT );
        this.checkHeader( headers[GwasDatabaseImporterCli.RISK_ALLELE_FREQUENCY_INDEX],
                GwasDatabaseImporterCli.RISK_ALLELE_FREQUENCY );
        this.checkHeader( headers[GwasDatabaseImporterCli.P_VALUE_INDEX], GwasDatabaseImporterCli.P_VALUE );
        this.checkHeader( headers[GwasDatabaseImporterCli.OR_OR_BETA_INDEX], GwasDatabaseImporterCli.OR_OR_BETA );
        this.checkHeader( headers[GwasDatabaseImporterCli.PLATFORM_INDEX], GwasDatabaseImporterCli.PLATFORM );
    }

    /*
     * (non-Javadoc)
     *
     * @see ubic.gemma.core.util.AbstractCLI#buildOptions()
     */
    @Override
    protected void buildOptions() {
        // No-op
    }
}