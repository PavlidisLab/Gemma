package ubic.gemma.core.association.phenotype.fileUpload.literatureEvidence;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;

public class SFARIIntermediateFileParser {

    public static void main( String[] args ) throws Exception {

        try (BufferedWriter outputSFARI = new BufferedWriter( new FileWriter(
                "./gemma-core/src/main/java/ubic/gemma/association/phenotype/fileUpload/literatureEvidence/outputSFARI.tsv" ) )) {

            try (BufferedReader br = new BufferedReader( new FileReader(
                    "./gemma-core/src/main/java/ubic/gemma/association/phenotype/fileUpload/literatureEvidence/autism-gene-dataset.csv" ) )) {

                String headers = SFARIIntermediateFileParser.cvs2tsv( br.readLine() );

                // define index of header
                SFARILineInfo.setIndex( headers );
                SFARILineInfo.writeFinalHeader( outputSFARI );

                String line;
                int lineNumber = 1;

                while ( ( line = br.readLine() ) != null ) {

                    System.out.println( "Line: " + lineNumber++ );

                    String finalLine = SFARIIntermediateFileParser.cvs2tsv( line ) + "\t end";

                    SFARILineInfo sfariLineInfo = new SFARILineInfo( finalLine );
                    sfariLineInfo.writeFinalLine( outputSFARI );

                }

            }
        }
    }

    @SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
    public static String cvs2tsv( String line ) {

        StringBuilder newLine = new StringBuilder( line );

        boolean change = true;

        for ( int position = 0; position < newLine.length(); position++ ) {

            if ( newLine.charAt( position ) == ',' && change ) {
                newLine.setCharAt( position, '\t' );
            } else if ( newLine.charAt( position ) == '"' ) {

                change = !change;
            }
        }

        return newLine.toString().replaceAll( "\"", "" );
    }

}
