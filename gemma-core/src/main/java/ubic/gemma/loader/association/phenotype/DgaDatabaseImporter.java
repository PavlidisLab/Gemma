package ubic.gemma.loader.association.phenotype;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import ubic.basecode.ontology.model.OntologyTerm;

/* this importer cannot automatically download files it expects the files to already be there */
public class DgaDatabaseImporter extends ExternalDatabaseEvidenceImporterAbstractCLI {

    // to find to file go to : http://dga.nubic.northwestern.edu/pages/download.php

    // name of the external database
    protected static final String DGA = "DGA";

    // ********************************************************************************
    public static final String DGA_FILE_NAME = "IDMappings.rdf";

    private File dgaFile = null;

    public DgaDatabaseImporter( String[] args ) throws Exception {
        super( args );
        checkForDGAFile();
        processDGAFile();
    }

    public static void main( String[] args ) throws Exception {
        @SuppressWarnings("unused")
        DgaDatabaseImporter databaseImporter = new DgaDatabaseImporter( args );
    }

    /* this importer cannot automatically download files it expects the files to already be there */
    private void checkForDGAFile() throws Exception {

        writeFolder = WRITE_FOLDER + File.separator + DGA;

        File folder = new File( writeFolder );

        if ( !folder.exists() ) {
            throw new Exception( "cannot find the DGA Folder" + folder.getAbsolutePath() );
        }

        // file expected
        dgaFile = new File( writeFolder + File.separator + DGA_FILE_NAME );
        if ( !dgaFile.exists() ) {
            throw new Exception( "cannot find file: " + dgaFile.getAbsolutePath() );
        }
    }

    private void processDGAFile() throws Exception {

        outFinalResults = new BufferedWriter( new FileWriter( writeFolder + "/finalResults.tsv" ) );
        outNotFound = new BufferedWriter( new FileWriter( writeFolder + "/notFound.tsv" ) );
        outNotFound.write( "Term is not a leaf and have < 2 parent" );

        BufferedReader dgaReader = new BufferedReader( new FileReader( dgaFile ) );
        String line = "";

        // write the correct header in the output File
        writeOutputFileHeaders5();

        while ( ( line = dgaReader.readLine() ) != null ) {

            // found a term
            if ( line.indexOf( "DOID" ) != -1 ) {
                // this being of the url could change make sure its still correct if something doesn't work
                String valueUri = "http://purl.obolibrary.org/obo/DOID_" + findStringBetweenSpecialCharacter( line );

                String geneId = findStringBetweenSpecialCharacter( dgaReader.readLine(), "GeneID" );
                String pubMedID = findStringBetweenSpecialCharacter( dgaReader.readLine(), "PubMedID" );
                String geneRIF = findStringBetweenSpecialCharacter( dgaReader.readLine(), "GeneRIF" );

                OntologyTerm o = findOntologyTermExistAndNotObsolote( valueUri );

                if ( o != null ) {

                    String geneSymbol = geneToSymbol( new Integer( geneId ) );
                    // gene do exist
                    if ( geneSymbol != null ) {

                        // if deep >3 always keep
                        int howDeepIdTerm = findHowManyParents( o, 0 );

                        // keep leaf or deep enough or uri=DOID_162(cancer)
                        if ( ( o.getChildren( true ).size() != 0 && howDeepIdTerm < 2 )
                                || o.getUri().indexOf( "DOID_162" ) != -1 ) {
                            outNotFound.write( o.getLabel() + "\t" + o.getUri() + "\n" );
                        }

                        else {
                            outFinalResults.write( o.getUri() + "\t" + o.getLabel() + "\t" + geneSymbol + "\t" + geneId
                                    + "\t" + pubMedID + "\t" + "GeneRIF: " + geneRIF + "\t" + "IEA" + "\t" + "" + "\t"
                                    + DGA + "\n" );
                        }
                    } else {
                        log.info( "gene NCBI no found in Gemma discard this eidence: ncbi: " + geneId );
                    }

                } else {
                    log.info( "Ontology term not found in Ontology or obsolete : " + valueUri
                            + " (normal that this happen sometimes)" );
                }
            }
        }
        dgaReader.close();
        outFinalResults.close();
        outNotFound.close();
    }

    // find string between first > and <
    private String findStringBetweenSpecialCharacter( String line ) {
        String newLine = line.substring( line.indexOf( ">" ) + 1, line.length() );
        if ( newLine.indexOf( "<" ) != -1 ) {
            newLine = newLine.substring( 0, newLine.indexOf( "<" ) );
        }
        return newLine;
    }

    private String findStringBetweenSpecialCharacter( String line, String keyword ) throws Exception {

        // System.out.println( line );

        if ( line.indexOf( keyword ) == -1 ) {
            throw new Exception( keyword + " not found in File ??? " + line );
        }

        return findStringBetweenSpecialCharacter( line );
    }

    private int findHowManyParents( OntologyTerm o, int increment ) {

        if ( o.getParents( true ).size() != 0 ) {
            return findHowManyParents( o.getParents( true ).iterator().next(), ++increment );
        }

        return increment;
    }

}
