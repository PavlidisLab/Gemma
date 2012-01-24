package ubic.gemma.association.phenotype.fileUpload.literatureEvidence;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class SFARILineInfo {

    // DEFNIFE COLUMNS NAMES AS THEY APPEAR IN FILE
    private static String geneSymbolHeader = "Gene Symbol";
    private static String entrezGeneIDHeader = "Entrez GeneID";
    private static String supportForAutismHeader = "Support for Autism";
    private static String evidenceOfSupportHeader = "Evidence of Support";
    private static String positiveReferenceHeader = "Positive Reference";
    private static String negativeReferenceHeader = "Negative Reference";
    // there is a mistake here but the mistake in in the file Primay should be Primary
    private static String primaryReferenceHeader = "Primay Reference";
    private static String mostCitedHeader = "Most cited";
    private static String mostRecentHeader = "Most Recent";
    private static String supportingHeader = "Supporting";

    // INDEX OF EACH COLUMN, TO B DERTERMINE ON READING FILE
    private static Integer geneSymbolIndex = -1;
    private static Integer entrezGeneIDIndex = -1;
    private static Integer supportForAutismIndex = -1;
    private static Integer evidenceOfSupportIndex = -1;
    private static Integer positiveReferenceIndex = -1;
    private static Integer negativeReferenceIndex = -1;
    private static Integer primaryReferenceIndex = -1;
    private static Integer mostCitedIndex = -1;
    private static Integer mostRecentIndex = -1;
    private static Integer supportingIndex = -1;

    private String geneSymbol = "";
    private String nbciID = "";
    private String description = "";

    private Set<String> literaturePubMed = new HashSet<String>();
    private Set<String> literaturePubMedNegative = new HashSet<String>();

    public static void setIndex( String headers ) throws Exception {

        String[] headersTokens = headers.split( "\t" );

        ArrayList<String> headersSet = new ArrayList<String>();

        for ( String token : headersTokens ) {
            headersSet.add( token );
        }

        if ( !headersSet.contains( geneSymbolHeader ) || !headersSet.contains( entrezGeneIDHeader )
                || !headersSet.contains( supportForAutismHeader ) || !headersSet.contains( evidenceOfSupportHeader )
                || !headersSet.contains( positiveReferenceHeader ) || !headersSet.contains( negativeReferenceHeader )
                || !headersSet.contains( primaryReferenceHeader ) || !headersSet.contains( mostCitedHeader )
                || !headersSet.contains( mostRecentHeader ) || !headersSet.contains( supportingHeader ) ) {
            throw new Exception( "Some headers not find" );
        }

        geneSymbolIndex = headersSet.indexOf( geneSymbolHeader );
        entrezGeneIDIndex = headersSet.indexOf( entrezGeneIDHeader );
        supportForAutismIndex = headersSet.indexOf( supportForAutismHeader );
        evidenceOfSupportIndex = headersSet.indexOf( evidenceOfSupportHeader );
        positiveReferenceIndex = headersSet.indexOf( positiveReferenceHeader );
        negativeReferenceIndex = headersSet.indexOf( negativeReferenceHeader );
        primaryReferenceIndex = headersSet.indexOf( primaryReferenceHeader );
        mostCitedIndex = headersSet.indexOf( mostCitedHeader );
        mostRecentIndex = headersSet.indexOf( mostRecentHeader );
        supportingIndex = headersSet.indexOf( supportingHeader );
    }

    public SFARILineInfo( String line ) {

        String[] lineTokens = line.split( "\t" );
        this.geneSymbol = lineTokens[geneSymbolIndex];
        this.nbciID = lineTokens[entrezGeneIDIndex];
        this.description = lineTokens[supportForAutismIndex] + " ; " + lineTokens[evidenceOfSupportIndex];

        addAllPubmed( lineTokens[positiveReferenceIndex], this.literaturePubMed );
        addAllPubmed( lineTokens[primaryReferenceIndex], this.literaturePubMed );
        addAllPubmed( lineTokens[mostCitedIndex], this.literaturePubMed );
        addAllPubmed( lineTokens[mostRecentIndex], this.literaturePubMed );
        addAllPubmed( lineTokens[supportingIndex], this.literaturePubMed );

        addAllPubmed( lineTokens[negativeReferenceIndex], this.literaturePubMedNegative );
    }

    public static void writeFinalHeader( BufferedWriter outputSFARI ) throws IOException {

        outputSFARI
                .write( "Gene Name\tGene ID\tExperimental Source (PMID)\tEvidenceCode\tComment\tAssossiationType\tisNegative\tExternal Database\tExternal Database ID\tPhenotype\n" );
    }

    public void writeFinalLine( BufferedWriter outputSFARI ) throws IOException {

        for ( String pudmed : this.literaturePubMed ) {

            outputSFARI.write( this.geneSymbol + "\t" );
            outputSFARI.write( this.nbciID + "\t" );
            outputSFARI.write( pudmed + "\t" );

            outputSFARI.write( "TAS" + "\t" );
            outputSFARI.write( this.description + "\t" );
            outputSFARI.write( "" + "\t" );
            outputSFARI.write( "" + "\t" );

            outputSFARI.write( "SFARI" + "\t" );
            outputSFARI.write( this.geneSymbol + "\t" );

            outputSFARI.write( "autism spectrum disorder" + "\t" );
            outputSFARI.newLine();
        }

        for ( String pudmed : this.literaturePubMedNegative ) {

            outputSFARI.write( this.geneSymbol + "\t" );
            outputSFARI.write( this.nbciID + "\t" );
            outputSFARI.write( pudmed + "\t" );

            outputSFARI.write( "TAS" + "\t" );
            outputSFARI.write( this.description + "\t" );
            outputSFARI.write( "" + "\t" );
            outputSFARI.write( "1" + "\t" );

            outputSFARI.write( "SFARI" + "\t" );
            outputSFARI.write( this.geneSymbol + "\t" );

            outputSFARI.write( "autism spectrum disorder" + "\t" );
            outputSFARI.newLine();
        }
    }

    private void addAllPubmed( String linePubmedIds, Set<String> mySet ) {

        String[] pubmedIds = linePubmedIds.split( "," );

        for ( int i = 0; i < pubmedIds.length; i++ ) {
            if ( !pubmedIds[i].trim().equalsIgnoreCase( "" ) ) {

                if ( mySet.contains( pubmedIds[i].trim() ) ) {
                    System.err.println( "SAME FOUND ***" + pubmedIds[i].trim() );
                }

                mySet.add( pubmedIds[i].trim() );
            }
        }
    }

}
