package ubic.gemma.core.association.phenotype.fileUpload.literatureEvidence;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@SuppressWarnings({ "WeakerAccess", "unused" }) // Possible external use
public class SFARILineInfo {

    // Column names as they appear in the file
    private static final String GENE_SYMBOL_HEADER = "Gene Symbol";
    private static final String ENTREZ_GENE_ID_HEADER = "Entrez GeneID";
    private static final String SUPPORT_FOR_AUTISM_HEADER = "Support for Autism";
    private static final String EVIDENCE_OF_SUPPORT_HEADER = "Evidence of Support";
    private static final String POSITIVE_REFERENCE_HEADER = "Positive Reference";
    private static final String NEGATIVE_REFERENCE_HEADER = "Negative Reference";
    // there is a mistake here but the mistake is in the file! Primay should be Primary
    private static final String PRIMARY_REFERENCE_HEADER = "Primay Reference";
    private static final String MOST_CITED_HEADER = "Most cited";
    private static final String MOST_RECENT_HEADER = "Most Recent";
    private static final String SUPPORTING_HEADER = "Supporting";

    // INDEX OF EACH COLUMN, TO B DETERMINE ON READING FILE
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

    private final String geneSymbol;
    private final String nbciID;
    private final String description;

    private final Set<String> literaturePubMed = new HashSet<>();
    private final Set<String> literaturePubMedNegative = new HashSet<>();

    public SFARILineInfo( String line ) {

        String[] lineTokens = line.split( "\t" );
        this.geneSymbol = lineTokens[SFARILineInfo.geneSymbolIndex];
        this.nbciID = lineTokens[SFARILineInfo.entrezGeneIDIndex];
        this.description = lineTokens[SFARILineInfo.supportForAutismIndex] + " ; "
                + lineTokens[SFARILineInfo.evidenceOfSupportIndex];

        this.addAllPubmed( lineTokens[SFARILineInfo.positiveReferenceIndex], this.literaturePubMed );
        this.addAllPubmed( lineTokens[SFARILineInfo.primaryReferenceIndex], this.literaturePubMed );
        this.addAllPubmed( lineTokens[SFARILineInfo.mostCitedIndex], this.literaturePubMed );
        this.addAllPubmed( lineTokens[SFARILineInfo.mostRecentIndex], this.literaturePubMed );
        this.addAllPubmed( lineTokens[SFARILineInfo.supportingIndex], this.literaturePubMed );

        this.addAllPubmed( lineTokens[SFARILineInfo.negativeReferenceIndex], this.literaturePubMedNegative );
    }

    public static void setIndex( String headers ) throws Exception {

        String[] headersTokens = headers.split( "\t" );

        ArrayList<String> headersSet = new ArrayList<>();

        Collections.addAll( headersSet, headersTokens );

        if ( !headersSet.contains( SFARILineInfo.GENE_SYMBOL_HEADER ) || !headersSet
                .contains( SFARILineInfo.ENTREZ_GENE_ID_HEADER ) || !headersSet
                .contains( SFARILineInfo.SUPPORT_FOR_AUTISM_HEADER ) || !headersSet
                .contains( SFARILineInfo.EVIDENCE_OF_SUPPORT_HEADER ) || !headersSet
                .contains( SFARILineInfo.POSITIVE_REFERENCE_HEADER ) || !headersSet
                .contains( SFARILineInfo.NEGATIVE_REFERENCE_HEADER ) || !headersSet
                .contains( SFARILineInfo.PRIMARY_REFERENCE_HEADER ) || !headersSet
                .contains( SFARILineInfo.MOST_CITED_HEADER ) || !headersSet.contains( SFARILineInfo.MOST_RECENT_HEADER )
                || !headersSet.contains( SFARILineInfo.SUPPORTING_HEADER ) ) {
            throw new Exception( "Some headers not find" );
        }

        SFARILineInfo.geneSymbolIndex = headersSet.indexOf( SFARILineInfo.GENE_SYMBOL_HEADER );
        SFARILineInfo.entrezGeneIDIndex = headersSet.indexOf( SFARILineInfo.ENTREZ_GENE_ID_HEADER );
        SFARILineInfo.supportForAutismIndex = headersSet.indexOf( SFARILineInfo.SUPPORT_FOR_AUTISM_HEADER );
        SFARILineInfo.evidenceOfSupportIndex = headersSet.indexOf( SFARILineInfo.EVIDENCE_OF_SUPPORT_HEADER );
        SFARILineInfo.positiveReferenceIndex = headersSet.indexOf( SFARILineInfo.POSITIVE_REFERENCE_HEADER );
        SFARILineInfo.negativeReferenceIndex = headersSet.indexOf( SFARILineInfo.NEGATIVE_REFERENCE_HEADER );
        SFARILineInfo.primaryReferenceIndex = headersSet.indexOf( SFARILineInfo.PRIMARY_REFERENCE_HEADER );
        SFARILineInfo.mostCitedIndex = headersSet.indexOf( SFARILineInfo.MOST_CITED_HEADER );
        SFARILineInfo.mostRecentIndex = headersSet.indexOf( SFARILineInfo.MOST_RECENT_HEADER );
        SFARILineInfo.supportingIndex = headersSet.indexOf( SFARILineInfo.SUPPORTING_HEADER );
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

        for ( String pubmedId : pubmedIds ) {
            if ( !pubmedId.trim().equalsIgnoreCase( "" ) ) {

                if ( mySet.contains( pubmedId.trim() ) ) {
                    System.err.println( "SAME FOUND ***" + pubmedId.trim() );
                }

                mySet.add( pubmedId.trim() );
            }
        }
    }

}
