/*
 * The gemma project
 *
 * Copyright (c) 2014 University of British Columbia
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

import ubic.basecode.util.StringUtil;
import ubic.gemma.core.apps.GemmaCLI.CommandGroup;
import ubic.gemma.core.util.AbstractCLI;
import ubic.gemma.model.genome.gene.phenotype.valueObject.ScoreValueObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * *
 *
 * @author nicolas
 */
public class SfariDatabaseImporter extends ExternalDatabaseEvidenceImporterAbstractCLI {

    // name of the external database
    private static final String SFARI = "SFARI";
    private static final String DESCRIPTION_SCORE_HEADER = "Evidence";
    private static final String ENTREZ_GENE_ID_HEADER = "Entrez GeneID";
    private static final String EVIDENCE_OF_SUPPORT_HEADER = "Evidence of Support";
    // ****************************************************************************************
    // the gene file
    // DEFNIFE COLUMNS NAMES AS THEY APPEAR IN FILE
    private static final String GENE_SYMBOL_HEADER = "Gene Symbol";
    // there are found using the score file
    // ****************************************************************************************
    private static final String GENE_SYMBOL_SCORE_HEADER = "Gene Symbol";
    private static final String MOST_CITED_HEADER = "Most cited";
    private static final String MOST_RECENT_HEADER = "Most Recent";
    private static final String NEGATIVE_REFERENCE_HEADER = "Negative Reference";
    private static final String POSITIVE_REFERENCE_HEADER = "Positive Reference";
    // there is a mistake here but the mistake in in the file Primay should be Primary
    private static final String PRIMARY_REFERENCE_HEADER = "Primay Reference";
    private static final String SCORE_DETAILS_HEADER = "Score Details";
    private static final String SCORE_HEADER = "Score";
    private static final String SUPPORT_FOR_AUTISM_HEADER = "Support for Autism";
    private static final String SUPPORTING_HEADER = "Supporting";
    // gene Id ---> description
    private final Map<String, String> gene2Description = new HashMap<>();
    // gene Id ---> score
    private final Map<String, ScoreValueObject> gene2Score = new HashMap<>();
    // messy file, need to be corrected each time not a real csv file, some conflicts with extra character "
    private File autismGeneDataset = null;
    private File geneScore = null;

    @SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
    public SfariDatabaseImporter( String[] args ) throws Exception {
        super( args );
        Exception e = this.doWork( args );
        if ( e != null ) {
            e.printStackTrace();
        }
        /* this importer cannot automatically download files it expects the files to already be there */
    }

    public static void main( String[] args ) throws Exception {
        @SuppressWarnings("unused") SfariDatabaseImporter importEvidence = new SfariDatabaseImporter( args );
    }

    @Override
    public String getCommandName() {
        return "sfariImport";
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

        try {
            this.checkForSfariFiles();
            this.processSfariScoreFile();
            this.processSfariGeneFile();
        } catch ( Exception e ) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public String getShortDesc() {
        return "Creates a .tsv file of lines of evidence from SFARI, to be used with EvidenceImporterCLI.java to import into Phenocarta.";
    }

    @Override
    protected void processOptions() {
        super.processOptions();
    }

    private void addAllPubmed( String linePubmedIds, Set<String> mySet ) throws Exception {

        String[] pubmedIds = linePubmedIds.split( "," );

        for ( String pubmedId : pubmedIds ) {

            if ( !pubmedId.trim().equalsIgnoreCase( "" ) ) {
                System.out.println( linePubmedIds );

                Integer pubM = new Integer( pubmedId.trim() );

                if ( pubM < 1000 ) {
                    throw new Exception( "Why pubM < 1000" );
                }

                if ( mySet.contains( pubmedId.trim() ) ) {
                    AbstractCLI.log.error( "SAME FOUND ***" + pubmedId.trim() );
                    System.exit( -1 );
                }

                mySet.add( pubmedId.trim() );
            }
        }
    }

    private void checkForSfariFiles() throws Exception {

        writeFolder =
                ExternalDatabaseEvidenceImporterAbstractCLI.WRITE_FOLDER + File.separator + SfariDatabaseImporter.SFARI;

        File folder = new File( writeFolder );

        if ( !folder.exists() ) {
            throw new Exception( "cannot find the SFARI Folder" + folder.getAbsolutePath() );
        }

        // first file expected
        autismGeneDataset = new File( writeFolder + File.separator + "autism-gene-dataset.csv" );
        if ( !autismGeneDataset.exists() ) {
            throw new Exception( "cannot find file: " + autismGeneDataset.getAbsolutePath() );
        }

        // second file expected
        geneScore = new File( writeFolder + File.separator + "gene-score.csv" );
        if ( !geneScore.exists() ) {
            throw new Exception( "cannot find file: " + autismGeneDataset.getAbsolutePath() );
        }
    }

    private void processSfariGeneFile() throws Exception {

        this.initFinalOutputFile( true, true );

        try (BufferedReader brAutismGeneDataset = new BufferedReader( new FileReader( autismGeneDataset ) )) {

            String header = StringUtil.cvs2tsv( brAutismGeneDataset.readLine() );

            String[] headersTokens = header.split( "\t" );

            ArrayList<String> headersSet = new ArrayList<>();

            Collections.addAll( headersSet, headersTokens );

            if ( !headersSet.contains( SfariDatabaseImporter.GENE_SYMBOL_HEADER ) || !headersSet
                    .contains( SfariDatabaseImporter.ENTREZ_GENE_ID_HEADER ) || !headersSet
                    .contains( SfariDatabaseImporter.SUPPORT_FOR_AUTISM_HEADER ) || !headersSet
                    .contains( SfariDatabaseImporter.EVIDENCE_OF_SUPPORT_HEADER ) || !headersSet
                    .contains( SfariDatabaseImporter.POSITIVE_REFERENCE_HEADER ) || !headersSet
                    .contains( SfariDatabaseImporter.NEGATIVE_REFERENCE_HEADER ) || !headersSet
                    .contains( SfariDatabaseImporter.PRIMARY_REFERENCE_HEADER ) || !headersSet
                    .contains( SfariDatabaseImporter.MOST_CITED_HEADER ) || !headersSet
                    .contains( SfariDatabaseImporter.MOST_RECENT_HEADER ) || !headersSet
                    .contains( SfariDatabaseImporter.SUPPORTING_HEADER ) ) {
                throw new Exception( "Some headers not find in the autism gene dataset" );
            }

            Integer geneSymbolIndex = headersSet.indexOf( SfariDatabaseImporter.GENE_SYMBOL_HEADER );
            Integer entrezGeneIDIndex = headersSet.indexOf( SfariDatabaseImporter.ENTREZ_GENE_ID_HEADER );
            Integer supportForAutismIndex = headersSet.indexOf( SfariDatabaseImporter.SUPPORT_FOR_AUTISM_HEADER );
            Integer evidenceOfSupportIndex = headersSet.indexOf( SfariDatabaseImporter.EVIDENCE_OF_SUPPORT_HEADER );
            Integer positiveReferenceIndex = headersSet.indexOf( SfariDatabaseImporter.POSITIVE_REFERENCE_HEADER );
            Integer negativeReferenceIndex = headersSet.indexOf( SfariDatabaseImporter.NEGATIVE_REFERENCE_HEADER );
            Integer primaryReferenceIndex = headersSet.indexOf( SfariDatabaseImporter.PRIMARY_REFERENCE_HEADER );
            @SuppressWarnings("unused") Integer mostCitedIndex = headersSet
                    .indexOf( SfariDatabaseImporter.MOST_CITED_HEADER );
            @SuppressWarnings("unused") Integer mostRecentIndex = headersSet
                    .indexOf( SfariDatabaseImporter.MOST_RECENT_HEADER );
            Integer supportingIndex = headersSet.indexOf( SfariDatabaseImporter.SUPPORTING_HEADER );

            String line;

            while ( ( line = brAutismGeneDataset.readLine() ) != null ) {

                line = StringUtil.cvs2tsv( line ) + "\t end";

                String[] lineTokens = line.split( "\t" );
                String geneSymbol = lineTokens[geneSymbolIndex];
                String nbciID = lineTokens[entrezGeneIDIndex];
                String description = lineTokens[supportForAutismIndex] + " ; " + lineTokens[evidenceOfSupportIndex];
                Set<String> literaturePubMed = new HashSet<>();
                Set<String> literaturePubMedNegative = new HashSet<>();
                System.out.println( line );
                this.addAllPubmed( lineTokens[positiveReferenceIndex], literaturePubMed );
                this.addAllPubmed( lineTokens[primaryReferenceIndex], literaturePubMed );
                // addAllPubmed( lineTokens[mostCitedIndex], this.literaturePubMed );
                // addAllPubmed( lineTokens[mostRecentIndex], this.literaturePubMed );
                this.addAllPubmed( lineTokens[supportingIndex], literaturePubMed );

                this.addAllPubmed( lineTokens[negativeReferenceIndex], literaturePubMedNegative );

                nbciID = this.treatSpecialCases( geneSymbol, nbciID );

                ScoreValueObject scoreVO = gene2Score.get( geneSymbol );

                String descriptionInScore = gene2Description.get( geneSymbol );

                if ( descriptionInScore == null ) {
                    descriptionInScore = "";
                } else {
                    descriptionInScore = " " + descriptionInScore;
                }

                if ( !literaturePubMed.isEmpty() ) {
                    this.writeFinalSfari( literaturePubMed, geneSymbol, nbciID, description, descriptionInScore,
                            scoreVO, false );
                }

                if ( !literaturePubMedNegative.isEmpty() ) {
                    this.writeFinalSfari( literaturePubMedNegative, geneSymbol, nbciID, description, descriptionInScore,
                            scoreVO, true );
                }
            }

            brAutismGeneDataset.close();
            outFinalResults.close();
        }
    }

    private void processSfariScoreFile() throws Exception {

        @SuppressWarnings("resource") BufferedReader brGeneScore = new BufferedReader( new FileReader( geneScore ) );

        // read headers
        String headersScore = StringUtil.cvs2tsv( brGeneScore.readLine() );

        String[] headersTokens = headersScore.split( "\t" );

        ArrayList<String> headersSet = new ArrayList<>();

        for ( String token : headersTokens ) {
            headersSet.add( token.trim() );
        }

        if ( !headersSet.contains( SfariDatabaseImporter.GENE_SYMBOL_SCORE_HEADER ) || !headersSet
                .contains( SfariDatabaseImporter.SCORE_HEADER ) || !headersSet
                .contains( SfariDatabaseImporter.SCORE_DETAILS_HEADER ) || !headersSet
                .contains( SfariDatabaseImporter.DESCRIPTION_SCORE_HEADER ) ) {
            throw new Exception( "Some headers not find" );
        }

        Integer geneSymbolIndex = headersSet.indexOf( SfariDatabaseImporter.GENE_SYMBOL_SCORE_HEADER );
        Integer scoreIndex = headersSet.indexOf( SfariDatabaseImporter.SCORE_HEADER );
        Integer scoreDetailsIndex = headersSet.indexOf( SfariDatabaseImporter.SCORE_DETAILS_HEADER );
        Integer descriptionScoreIndex = headersSet.indexOf( SfariDatabaseImporter.DESCRIPTION_SCORE_HEADER );

        String line;
        int lineNumer = 1;

        while ( ( line = brGeneScore.readLine() ) != null ) {

            line = StringUtil.cvs2tsv( line );

            String[] lineTokens = line.split( "\t" );

            AbstractCLI.log.info( "Reading Score file line: " + lineNumer++ );

            // getting out the info for 1 line
            String geneSymbol = lineTokens[geneSymbolIndex];
            String scoreDetails = lineTokens[scoreDetailsIndex];
            String score = lineTokens[scoreIndex];
            String description = lineTokens[descriptionScoreIndex];

            Double strength;

            if ( score.equalsIgnoreCase( "S" ) ) {
                score = scoreDetails;
            }

            if ( score.equalsIgnoreCase( "1S" ) || score.equalsIgnoreCase( "1" ) ) {
                strength = 1D;
            } else if ( score.equalsIgnoreCase( "2S" ) || score.equalsIgnoreCase( "2" ) ) {
                strength = 0.8D;
            } else if ( score.equalsIgnoreCase( "3S" ) || score.equalsIgnoreCase( "3" ) ) {
                strength = 0.6D;
            } else if ( score.equalsIgnoreCase( "4S" ) || score.equalsIgnoreCase( "4" ) ) {
                strength = 0.4D;
            } else if ( score.equalsIgnoreCase( "5S" ) || score.equalsIgnoreCase( "5" ) ) {
                strength = 0.2D;
            } else if ( score.equalsIgnoreCase( "6S" ) || score.equalsIgnoreCase( "6" ) ) {
                strength = 0D;
            } else if ( score.equalsIgnoreCase( "S" ) ) {
                strength = 0D;
            } else {
                throw new Exception( "Score: " + score );

            }

            ScoreValueObject s = new ScoreValueObject( strength, score, "SFARIGeneScore" );
            gene2Score.put( geneSymbol, s );
            gene2Description.put( geneSymbol, description );
        }

        brGeneScore.close();
    }

    private String treatSpecialCases( String geneSymbol, String nbciID ) {

        if ( geneSymbol.equalsIgnoreCase( "ATP2B2" ) && nbciID.equalsIgnoreCase( "108733" ) ) {

            return "491";

        } else if ( geneSymbol.equalsIgnoreCase( "TNIP2" ) && nbciID.equalsIgnoreCase( "610669" ) ) {

            return "79155";
        }
        return nbciID;

    }

    private void writeFinalSfari( Set<String> literaturePubMed, String geneSymbol, String nbciID, String description,
            String descriptionInScore, ScoreValueObject scoreVO, boolean isNegative ) throws IOException {

        String negative = "";

        if ( isNegative ) {
            negative = "1";
        }

        StringBuilder allPubmeds = new StringBuilder();

        for ( String pudmed : literaturePubMed ) {
            allPubmeds.append( pudmed ).append( ";" );
        }

        outFinalResults.write( geneSymbol + "\t" );
        outFinalResults.write( nbciID + "\t" );
        outFinalResults.write( allPubmeds + "\t" );
        outFinalResults.write( "TAS" + "\t" );
        outFinalResults.write( description + descriptionInScore + "\t" );
        outFinalResults.write( "SFARI" + "\t" );
        outFinalResults.write( geneSymbol + "\t" );
        outFinalResults.write( "\t\t" );
        outFinalResults.write( "autism spectrum disorder" + "\t" );
        outFinalResults.write( negative + "\t" );
        this.writeScore( scoreVO );
        outFinalResults.newLine();
    }

    private void writeScore( ScoreValueObject scoreVO ) throws IOException {

        if ( scoreVO != null ) {
            outFinalResults.write( scoreVO.getScoreName() + "\t" );
            outFinalResults.write( scoreVO.getScoreValue() + "\t" );
            outFinalResults.write( scoreVO.getStrength() + "\t" );
        }
    }
}
