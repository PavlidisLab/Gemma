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
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import ubic.basecode.ontology.model.OntologyTerm;
import ubic.basecode.ontology.ncbo.AnnotatorClient;
import ubic.basecode.ontology.ncbo.AnnotatorResponse;

public class GwasDatabaseImporter extends ExternalDatabaseEvidenceImporterAbstractCLI {

    // name of the external database
    public static final String GWAS = "GWAS";

    // path of file to download
    public static final String GWAS_URL_PATH = "http://www.genome.gov/admin/";
    public static final String GWAS_FILE = "gwascatalog.txt";

    // path for resources
    public static final String GWAS_FILES_PATH = RESOURCE_PATH + GWAS + File.separator;

    // manual static file mapping
    public static final String MANUAL_MAPPING_GWAS = GWAS_FILES_PATH + "ManualDescriptionMapping.tsv";

    // names and positions of the headers, this will be check with the file to verify all headers
    protected static final String PUBMED_ID = "PUBMEDID";
    protected static final Integer PUBMED_ID_INDEX = 1;
    protected static final String DISEASE_TRAIT = "Disease/Trait";
    protected static final Integer DISEASE_TRAIT_INDEX = 7;
    protected static final String INITIAL_SAMPLE_SIZE = "Initial Sample Size";
    protected static final Integer INITIAL_SAMPLE_SIZE_INDEX = 8;
    protected static final String REPLICATION_SAMPLE_SIZE = "Replication Sample Size";
    protected static final Integer REPLICATION_SAMPLE_SIZE_INDEX = 9;
    protected static final String REPORTED_GENES = "Reported Gene(s)";
    protected static final Integer REPORTED_GENES_INDEX = 13;
    protected static final String MAPPED_GENE = "Mapped_gene";
    protected static final Integer MAPPED_GENE_INDEX = 14;
    protected static final String STRONGEST_SNP = "Strongest SNP-Risk Allele";
    protected static final Integer STRONGEST_SNP_INDEX = 20;
    protected static final String SNPS = "SNPs";
    protected static final Integer SNPS_INDEX = 21;
    protected static final String CONTEXT = "Context";
    protected static final Integer CONTEXT_INDEX = 24;
    protected static final String RISK_ALLELE_FREQUENCY = "Risk Allele Frequency";
    protected static final Integer RISK_ALLELE_FREQUENCY_INDEX = 26;
    protected static final String P_VALUE = "p-Value";
    protected static final Integer P_VALUE_INDEX = 27;
    protected static final String OR_OR_BETA = "OR or beta";
    protected static final Integer OR_OR_BETA_INDEX = 30;
    protected static final String PLATFORM = "Platform [SNPs passing QC]";
    protected static final Integer PLATFORM_INDEX = 32;

    public static void main( String[] args ) throws Exception {

        GwasDatabaseImporter importEvidence = new GwasDatabaseImporter( args );

        // creates the folder where to place the file web downloaded files and final output files
        String writeFolder = importEvidence.createWriteFolder( GWAS );

        // download the GWAS file
        String gwasFile = importEvidence.downloadFileFromWeb( writeFolder, GWAS_URL_PATH, GWAS_FILE );

        // Use the manual description file, to link description to valueUri
        HashMap<String, Collection<String>> descriptionToValueUri = importEvidence.parseFileManualDescriptionFile();

        // process the gwas file
        importEvidence.processGwasFile( writeFolder, gwasFile, descriptionToValueUri );

    }

    // create the mapping from the static file also verify that what is in the file makes sense
    private HashMap<String, Collection<String>> parseFileManualDescriptionFile() throws IOException {

        HashMap<String, Collection<String>> description2ValueUri = new HashMap<String, Collection<String>>();

        BufferedReader br = new BufferedReader( new InputStreamReader(
                OmimDatabaseImporter.class.getResourceAsStream( MANUAL_MAPPING_GWAS ) ) );

        String line = "";

        // reads the manual file and put the data in a stucture
        while ( ( line = br.readLine() ) != null ) {

            Collection<String> col = new HashSet<String>();

            String[] tokens = line.split( "\t" );

            String descriptionStaticFile = tokens[0];
            String valueUriStaticFile = tokens[1];
            String valueStaticFile = tokens[2];

            OntologyTerm ontologyTerm = findOntologyTermExistAndNotObsolote( valueUriStaticFile );

            if ( ontologyTerm != null ) {

                if ( ontologyTerm.getUri().equalsIgnoreCase( valueStaticFile ) ) {

                    if ( description2ValueUri.get( descriptionStaticFile ) == null ) {

                        col.add( valueUriStaticFile );
                    } else {
                        col = description2ValueUri.get( descriptionStaticFile );
                        col.add( valueUriStaticFile );
                    }
                    description2ValueUri.put( descriptionStaticFile, col );
                } else {
                    errorMessages.add( "MANUAL VALUEURI AND VALUE DOESNT MATCH: " + line );
                }
            } else {
                errorMessages.add( "MANUAL MAPPING FILE TERM OBSOLETE OR NOT EXISTANT: " + line );
            }
        }
        return description2ValueUri;
    }

    // process the gwas file, line by line
    private void processGwasFile( String writeFolder, String gwasFile,
            HashMap<String, Collection<String>> descriptionToValueUri ) throws Exception {

        // this file have the final data to be imported
        BufferedWriter outFinalResults = new BufferedWriter( new FileWriter( writeFolder + "/finalResults.tsv" ) );

        // headers of the final file
        outFinalResults
                .write( "GeneSymbol\tPrimaryPubMed\tEvidenceCode\tComments\tScore\tStrength\tScoreType\tExternalDatabase\tDatabaseLink\tPhenotypes\n" );

        // this file we are unsure, the correct data needs to be move to the manual mapping
        BufferedWriter outMappingFound = new BufferedWriter( new FileWriter( writeFolder + "/mappingFound.tsv" ) );

        Collection<Long> ontologiesToUse = new HashSet<Long>();
        ontologiesToUse.add( AnnotatorClient.DOID_ONTOLOGY );
        ontologiesToUse.add( AnnotatorClient.HP_ONTOLOGY );
        AnnotatorClient anoClient = new AnnotatorClient( ontologiesToUse );

        BufferedReader br = new BufferedReader( new FileReader( gwasFile ) );

        // check if we have correct headers and organ
        verifyHeaders( br.readLine().split( "\t" ) );

        String line = "";

        // parse the morbid OMIM file
        while ( ( line = br.readLine() ) != null && !line.trim().equalsIgnoreCase( "" ) ) {

            String[] tokens = line.split( "\t" );

            // the geneSymbol found in the file
            String geneSymbol = findGeneSymbol( tokens[REPORTED_GENES_INDEX], tokens[MAPPED_GENE_INDEX] );

            if ( geneSymbol == null ) {
                continue;
            }

            // can we map the description with the static file
            String description = tokens[DISEASE_TRAIT_INDEX];
            if ( descriptionToValueUri.get( description ) != null ) {
                writeFinalFile( outFinalResults, writeFolder, geneSymbol, tokens,
                        descriptionToValueUri.get( description ) );
            } else {

                // something if found by the annotator
                Collection<AnnotatorResponse> responses = removeNotExistAndObsolete( anoClient.findTerm( description ) );

                if ( !responses.isEmpty() ) {

                    String conditionUsed = "";
                    String annotatorValueFound = "";
                    String annotatorValueUriFound = "";

                    AnnotatorResponse annotatorResponseFirstNormal = responses.iterator().next();

                    if ( annotatorResponseFirstNormal.getOntologyUsed().equalsIgnoreCase( "DOID" ) ) {

                        if ( annotatorResponseFirstNormal.isExactMatch() ) {
                            annotatorValueFound = annotatorResponseFirstNormal.getValue();
                            annotatorValueUriFound = annotatorResponseFirstNormal.getValueUri();
                            conditionUsed = "Case 4a: Found Exact With Disease Annotator";

                        } else if ( annotatorResponseFirstNormal.isSynonym() ) {
                            annotatorValueFound = annotatorResponseFirstNormal.getValue();
                            annotatorValueUriFound = annotatorResponseFirstNormal.getValueUri();
                            conditionUsed = "Case 5a: Found Synonym With Disease Annotator Synonym";
                        }
                    } else if ( annotatorResponseFirstNormal.getOntologyUsed().equalsIgnoreCase( "HP" ) ) {

                        if ( annotatorResponseFirstNormal.isExactMatch() ) {
                            annotatorValueFound = annotatorResponseFirstNormal.getValue();
                            annotatorValueUriFound = annotatorResponseFirstNormal.getValueUri();
                            conditionUsed = "Case 6a: Found Exact With HP Annotator";

                        } else if ( annotatorResponseFirstNormal.isSynonym() ) {
                            annotatorValueFound = annotatorResponseFirstNormal.getValue();
                            annotatorValueUriFound = annotatorResponseFirstNormal.getValueUri();
                            conditionUsed = "Case 7a: Found Synonym With HP Annotator Synonym";
                        }
                    }

                    if ( conditionUsed.isEmpty() ) {
                        conditionUsed = "Case 8: Found Mappings, No Match Detected";

                        for ( AnnotatorResponse annotatorResponse : responses ) {

                            annotatorValueFound = annotatorValueFound + annotatorResponse.getValue() + "; ";
                            annotatorValueUriFound = annotatorValueUriFound + annotatorResponse.getValueUri() + "; ";

                        }
                    }

                    outMappingFound.write( description + "\t" + annotatorValueUriFound + "\t" + annotatorValueFound
                            + "\t" + conditionUsed + "\n" );
                }
            }
        }

        outMappingFound.close();

        if ( !errorMessages.isEmpty() ) {

            log.info( "here is the error messages :\n" );

            for ( String err : errorMessages ) {

                log.error( err );
            }
        }

    }

    // the final file format
    private void writeFinalFile( BufferedWriter outFinalResults, String writeFolder, String geneSymbol,
            String[] tokens, Collection<String> valuesUri ) throws IOException {

        String comment = DISEASE_TRAIT + ": " + tokens[DISEASE_TRAIT_INDEX] + "; " + INITIAL_SAMPLE_SIZE + ": "
                + tokens[INITIAL_SAMPLE_SIZE_INDEX] + "; " + REPLICATION_SAMPLE_SIZE + ": "
                + tokens[REPLICATION_SAMPLE_SIZE_INDEX] + "; " + STRONGEST_SNP + ": " + tokens[STRONGEST_SNP_INDEX]
                + "; " + SNPS + ": " + tokens[SNPS_INDEX] + "; " + CONTEXT + ": " + tokens[CONTEXT_INDEX] + "; "
                + RISK_ALLELE_FREQUENCY + ": " + tokens[RISK_ALLELE_FREQUENCY_INDEX] + "; " + P_VALUE + ": "
                + tokens[P_VALUE_INDEX] + "; " + OR_OR_BETA + ": " + tokens[OR_OR_BETA_INDEX] + "; " + PLATFORM + ": "
                + tokens[PLATFORM_INDEX];

        String valuesUrisFormated = "";

        for ( String v : valuesUri ) {
            valuesUrisFormated += v + ";";
        }

        outFinalResults.write( geneSymbol + "\t" + tokens[PUBMED_ID_INDEX] + "\tTAS\t" + comment + "\t"
                + tokens[P_VALUE_INDEX] + "\t0.4\tP-value\tGWAS_Catalog\t?gene=" + geneSymbol + "\t"
                + valuesUrisFormated + "\n" );
    }

    // the rules to choose the gene symbol
    private String findGeneSymbol( String reportedGene, String mappedGene ) throws Exception {

        // TODO
        String geneSymbol = null;
        if ( false ) {

            if ( !reportedGene.isEmpty() && !mappedGene.isEmpty() ) {

                if ( reportedGene.equalsIgnoreCase( mappedGene ) ) {
                    geneSymbol = reportedGene;
                } else if ( reportedGene.indexOf( mappedGene ) != -1 ) {
                    geneSymbol = mappedGene;
                } else if ( mappedGene.indexOf( reportedGene ) != -1 ) {
                    geneSymbol = reportedGene;
                }
            }
        } else {
            geneSymbol = reportedGene;

        }

        if ( geneSymbol == null ) {
            return null;
        }

        // does this gene exist in gemma ?
        if ( this.geneService.findByOfficialSymbol( geneSymbol, taxonService.findByCommonName( "human" ) ) != null ) {
            return geneSymbol;
        }

        return null;
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

    private void checkHeader( String valueFile, String valueExpected ) throws Exception {

        if ( !valueFile.equalsIgnoreCase( valueExpected ) ) {
            throw new Exception( "Wrong header foudn in file, expected: " + valueExpected + "  found:" + valueFile );

        }
    }

    public GwasDatabaseImporter( String[] args ) throws Exception {
        super( args );
    }

}