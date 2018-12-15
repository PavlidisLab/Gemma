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
 * this importer cannot automatically download files it expects the files to already be there
 *
 * @author nicolas
 */
public class SfariDatabaseImporterCli extends ExternalDatabaseEvidenceImporterAbstractCLI {

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
    public SfariDatabaseImporterCli() throws Exception {
        super();
    }

    public static void main( String[] args ) throws Exception {
        @SuppressWarnings("unused")
        SfariDatabaseImporterCli importEvidence = new SfariDatabaseImporterCli();
        Exception e = importEvidence.doWork( args );
        if ( e != null ) {
            e.printStackTrace();
        }
        /*  */

    }

    @Override
    public String getCommandName() {
        return "sfariDownload";
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
        Exception e1 = super.processCommandLine( args );
        if ( e1 != null ) return e1;
        e1 = super.init();
        if ( e1 != null ) return e1;

        try {
            writeFolder = ppUtil.createWriteFolderIfDoesntExist( SFARI );

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
        return "Creates a .tsv file of lines of evidence from SFARI, to be used with evidenceImport to import into Phenocarta.";
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

        writeFolder = PhenotypeProcessingUtil.WRITE_FOLDER + File.separator + SfariDatabaseImporterCli.SFARI;

        File folder = new File( writeFolder );

        if ( !folder.exists() ) {
            throw new Exception( "cannot find the SFARI Folder: " + folder.getAbsolutePath() );
        }

        // PP found this file at http://autism.mindspec.org/autdb/submitsearch?selfld_0=GENE_CHR_NUM&selfldv_0=1&numOfFields=1&userAction=search&tableName=AUT_HG&submit=Submit+Query
        autismGeneDataset = new File( writeFolder + File.separator + "gene-summary.csv" );
        if ( !autismGeneDataset.exists() ) {
            throw new Exception( "cannot find file: " + autismGeneDataset.getAbsolutePath() );
        }

        // PP has not been able to locate this.
        geneScore = new File( writeFolder + File.separator + "gene-score.csv" );
        if ( !geneScore.exists() ) {
            throw new Exception( "cannot find file: " + autismGeneDataset.getAbsolutePath() );
        }
    }

    /**
     * Parse the 'gene-summary.csv' file
     * @throws Exception
     */
    private void processSfariGeneFile() throws Exception {

        ppUtil.initFinalOutputFile( writeFolder, true, true );

        try (BufferedReader brAutismGeneDataset = new BufferedReader( new FileReader( autismGeneDataset ) )) {

            String header = StringUtil.cvs2tsv( brAutismGeneDataset.readLine() );

            String[] headersTokens = header.split( "\t" );

            ArrayList<String> headersSet = new ArrayList<>();

            Collections.addAll( headersSet, headersTokens );

            if ( !headersSet.contains( SfariDatabaseImporterCli.GENE_SYMBOL_HEADER ) || !headersSet
                    .contains( SfariDatabaseImporterCli.ENTREZ_GENE_ID_HEADER )
                    || !headersSet
                            .contains( SfariDatabaseImporterCli.SUPPORT_FOR_AUTISM_HEADER )
                    || !headersSet
                            .contains( SfariDatabaseImporterCli.EVIDENCE_OF_SUPPORT_HEADER )
                    || !headersSet
                            .contains( SfariDatabaseImporterCli.POSITIVE_REFERENCE_HEADER )
                    || !headersSet
                            .contains( SfariDatabaseImporterCli.NEGATIVE_REFERENCE_HEADER )
                    || !headersSet
                            .contains( SfariDatabaseImporterCli.PRIMARY_REFERENCE_HEADER )
                    || !headersSet
                            .contains( SfariDatabaseImporterCli.MOST_CITED_HEADER )
                    || !headersSet
                            .contains( SfariDatabaseImporterCli.MOST_RECENT_HEADER )
                    || !headersSet
                            .contains( SfariDatabaseImporterCli.SUPPORTING_HEADER ) ) {
                throw new Exception( "Some headers not find in the autism gene dataset" );
            }

            Integer geneSymbolIndex = headersSet.indexOf( SfariDatabaseImporterCli.GENE_SYMBOL_HEADER );
            Integer entrezGeneIDIndex = headersSet.indexOf( SfariDatabaseImporterCli.ENTREZ_GENE_ID_HEADER );
            Integer supportForAutismIndex = headersSet.indexOf( SfariDatabaseImporterCli.SUPPORT_FOR_AUTISM_HEADER );
            Integer evidenceOfSupportIndex = headersSet.indexOf( SfariDatabaseImporterCli.EVIDENCE_OF_SUPPORT_HEADER );
            Integer positiveReferenceIndex = headersSet.indexOf( SfariDatabaseImporterCli.POSITIVE_REFERENCE_HEADER );
            Integer negativeReferenceIndex = headersSet.indexOf( SfariDatabaseImporterCli.NEGATIVE_REFERENCE_HEADER );
            Integer primaryReferenceIndex = headersSet.indexOf( SfariDatabaseImporterCli.PRIMARY_REFERENCE_HEADER );
            @SuppressWarnings("unused")
            Integer mostCitedIndex = headersSet
                    .indexOf( SfariDatabaseImporterCli.MOST_CITED_HEADER );
            @SuppressWarnings("unused")
            Integer mostRecentIndex = headersSet
                    .indexOf( SfariDatabaseImporterCli.MOST_RECENT_HEADER );
            Integer supportingIndex = headersSet.indexOf( SfariDatabaseImporterCli.SUPPORTING_HEADER );

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
            ppUtil.outFinalResults.close();
        }
    }

    /**
     * Parse the gene-score.csv file
     * @throws Exception
     */
    private void processSfariScoreFile() throws Exception {

        @SuppressWarnings("resource")
        BufferedReader brGeneScore = new BufferedReader( new FileReader( geneScore ) );

        // read headers
        String headersScore = StringUtil.cvs2tsv( brGeneScore.readLine() );

        String[] headersTokens = headersScore.split( "\t" );

        ArrayList<String> headersSet = new ArrayList<>();

        for ( String token : headersTokens ) {
            headersSet.add( token.trim() );
        }

        if ( !headersSet.contains( SfariDatabaseImporterCli.GENE_SYMBOL_SCORE_HEADER ) || !headersSet
                .contains( SfariDatabaseImporterCli.SCORE_HEADER )
                || !headersSet
                        .contains( SfariDatabaseImporterCli.SCORE_DETAILS_HEADER )
                || !headersSet
                        .contains( SfariDatabaseImporterCli.DESCRIPTION_SCORE_HEADER ) ) {
            throw new Exception( "Some headers not find" );
        }

        Integer geneSymbolIndex = headersSet.indexOf( SfariDatabaseImporterCli.GENE_SYMBOL_SCORE_HEADER );
        Integer scoreIndex = headersSet.indexOf( SfariDatabaseImporterCli.SCORE_HEADER );
        Integer scoreDetailsIndex = headersSet.indexOf( SfariDatabaseImporterCli.SCORE_DETAILS_HEADER );
        Integer descriptionScoreIndex = headersSet.indexOf( SfariDatabaseImporterCli.DESCRIPTION_SCORE_HEADER );

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

    /**
     * FIXME get rid of this if possible
     * 
     * @param  geneSymbol
     * @param  nbciID
     * @return
     */
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

        ppUtil.outFinalResults.write( geneSymbol + "\t" );
        ppUtil.outFinalResults.write( nbciID + "\t" );
        ppUtil.outFinalResults.write( allPubmeds + "\t" );
        ppUtil.outFinalResults.write( "TAS" + "\t" );
        ppUtil.outFinalResults.write( description + descriptionInScore + "\t" );
        ppUtil.outFinalResults.write( "SFARI" + "\t" );
        ppUtil.outFinalResults.write( geneSymbol + "\t" );
        ppUtil.outFinalResults.write( "\t\t" );
        ppUtil.outFinalResults.write( "autism spectrum disorder" + "\t" );
        ppUtil.outFinalResults.write( negative + "\t" );
        this.writeScore( scoreVO );
        ppUtil.outFinalResults.newLine();
    }

    private void writeScore( ScoreValueObject scoreVO ) throws IOException {

        if ( scoreVO != null ) {
            ppUtil.outFinalResults.write( scoreVO.getScoreName() + "\t" );
            ppUtil.outFinalResults.write( scoreVO.getScoreValue() + "\t" );
            ppUtil.outFinalResults.write( scoreVO.getStrength() + "\t" );
        }
    }
}
