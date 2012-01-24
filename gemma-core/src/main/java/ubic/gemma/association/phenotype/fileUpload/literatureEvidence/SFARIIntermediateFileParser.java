package ubic.gemma.association.phenotype.fileUpload.literatureEvidence;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;

public class SFARIIntermediateFileParser {

    public static void main( String[] args ) throws Exception {

        BufferedWriter outputSFARI = new BufferedWriter(
                new FileWriter(
                        "./gemma-core/src/main/java/ubic/gemma/association/phenotype/fileUpload/literatureEvidence/outputSFARI.tsv" ) );

        BufferedReader br = new BufferedReader(
                new FileReader(
                        "./gemma-core/src/main/java/ubic/gemma/association/phenotype/fileUpload/literatureEvidence/autism-gene-dataset.csv" ) );

        String headers = cvs2tsv( br.readLine() );

        // define index of header
        SFARILineInfo.setIndex( headers );
        SFARILineInfo.writeFinalHeader( outputSFARI );

        String line = "";
        int lineNumer = 1;

        while ( ( line = br.readLine() ) != null ) {

            System.out.println( "Line: " + lineNumer++ );

            String finalLine = cvs2tsv( line ) + "\t end";

            SFARILineInfo sfariLineInfo = new SFARILineInfo( finalLine );
            sfariLineInfo.writeFinalLine( outputSFARI );

        }

        outputSFARI.close();

    }

    public static String cvs2tsv( String line ) {

        StringBuffer newLine = new StringBuffer( line );

        boolean change = true;

        for ( int position = 0; position < newLine.length(); position++ ) {

            if ( newLine.charAt( position ) == ',' && change ) {
                newLine.setCharAt( position, '\t' );
            } else if ( newLine.charAt( position ) == '"' ) {

                if ( change ) {
                    change = false;
                } else {
                    change = true;
                }
            }
        }

        return newLine.toString().replaceAll( "\"", "" );
    }

}
