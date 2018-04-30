package ubic.gemma.core.loader.association.phenotype;

import ubic.basecode.ontology.model.OntologyTerm;
import ubic.gemma.core.apps.GemmaCLI.CommandGroup;
import ubic.gemma.core.util.AbstractCLI;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

/* this importer cannot automatically download files it expects the files to already be there */

public class DgaDatabaseImporter extends ExternalDatabaseEvidenceImporterAbstractCLI {

    // to find to file go to : http://dga.nubic.northwestern.edu/pages/download.php

    private static final String DGA_FILE_NAME = "IDMappings.rdf";

    // name of the external database
    private static final String DGA = "DGA";
    private final HashMap<String, HashSet<OntologyTerm>> commonLines = new HashMap<>();
    private final HashSet<String> linesToExclude = new HashSet<>();
    private File dgaFile = null;

    @SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
    public DgaDatabaseImporter( String[] args ) throws Exception {
        super( args );
    }

    public static void main( String[] args ) throws Exception {
        DgaDatabaseImporter databaseImporter = new DgaDatabaseImporter( args );
        Exception e = databaseImporter.doWork( args );
        if ( e != null ) {
            e.printStackTrace();
        }
    }

    @Override
    public String getCommandName() {
        return "dgaImport";
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
            this.checkForDGAFile();
            this.findTermsWithParents();
            this.processDGAFile();
        } catch ( Exception e ) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public String getShortDesc() {
        return "Creates a .tsv file of lines of evidence from DGA, to be used with EvidenceImporterCLI.java to import into Phenocarta.";
    }

    @Override
    protected void processOptions() {
        super.processOptions();
    }

    /* this importer cannot automatically download files it expects the files to already be there */
    private void checkForDGAFile() throws Exception {

        writeFolder =
                ExternalDatabaseEvidenceImporterAbstractCLI.WRITE_FOLDER + File.separator + DgaDatabaseImporter.DGA;

        File folder = new File( writeFolder );

        if ( !folder.exists() ) {
            throw new Exception( "cannot find the DGA Folder" + folder.getAbsolutePath() );
        }

        // file expected
        dgaFile = new File( writeFolder + File.separator + DgaDatabaseImporter.DGA_FILE_NAME );
        if ( !dgaFile.exists() ) {
            throw new Exception( "cannot find file: " + dgaFile.getAbsolutePath() );
        }
    }

    private int findHowManyParents( OntologyTerm o, int increment ) {

        if ( o.getParents( true ).size() != 0 ) {
            return this.findHowManyParents( o.getParents( true ).iterator().next(), ++increment );
        }

        return increment;
    }

    // find string between first > and <
    private String findStringBetweenSpecialCharacter( String line ) {
        String newLine = line.substring( line.indexOf( ">" ) + 1, line.length() );
        if ( newLine.contains( "<" ) ) {
            newLine = newLine.substring( 0, newLine.indexOf( "<" ) );
        }
        return newLine;
    }

    private String findStringBetweenSpecialCharacter( String line, String keyword ) throws Exception {

        if ( !line.contains( keyword ) ) {
            throw new Exception( keyword + " not found in File ??? " + line );
        }

        return this.findStringBetweenSpecialCharacter( line );
    }

    // extra step to take out redundant terms, if a child term is more specific dont keep the parent, if 2 lines share
    // same pubmed, gene and gene RIF take the most specific uri
    // example: same pubed, same gene 1-leukemia 2-myeloid leukemia 3-acute myeloid leukemia, keep only 3-
    private void findTermsWithParents() throws Exception {

        try (BufferedReader dgaReader = new BufferedReader( new FileReader( dgaFile ) )) {
            String line;

            while ( ( line = dgaReader.readLine() ) != null ) {

                // found a term
                if ( line.contains( "DOID" ) ) {
                    // this being of the url could change make sure its still correct if something doesn't work
                    String valueUri =
                            "http://purl.obolibrary.org/obo/DOID_" + this.findStringBetweenSpecialCharacter( line );

                    String geneId = this.findStringBetweenSpecialCharacter( dgaReader.readLine(), "GeneID" );
                    String pubMedID = this.findStringBetweenSpecialCharacter( dgaReader.readLine(), "PubMedID" );
                    String geneRIF = this.findStringBetweenSpecialCharacter( dgaReader.readLine(), "GeneRIF" );

                    OntologyTerm o = this.findOntologyTermExistAndNotObsolote( valueUri );

                    if ( o != null ) {

                        String key = geneId + pubMedID + geneRIF;
                        HashSet<OntologyTerm> valuesUri = new HashSet<>();

                        if ( commonLines.get( key ) != null ) {
                            valuesUri = commonLines.get( key );
                        }

                        valuesUri.add( o );

                        commonLines.put( key, valuesUri );
                    }
                }
            }
        }

        for ( String key : commonLines.keySet() ) {

            AbstractCLI.log.info( "Checking for lines that are ontology duplicated: " + key );

            HashSet<OntologyTerm> ontologyTerms = commonLines.get( key );

            HashSet<String> allUri = new HashSet<>();

            for ( OntologyTerm o : ontologyTerms ) {
                allUri.add( o.getUri() );
            }

            for ( OntologyTerm o : ontologyTerms ) {

                // get all kids terms
                Collection<OntologyTerm> childrenOntology = o.getChildren( false );

                for ( OntologyTerm onChil : childrenOntology ) {

                    // then this line is a parent dont keep it there is more specific child term
                    if ( allUri.contains( onChil.getUri() ) ) {
                        // result of this method, set of lines to exclude in the checkForDGAFile() step :
                        linesToExclude.add( key + o.getUri() );
                    }
                }
            }
        }
    }

    private boolean lineToInclude( String key ) {
        return !linesToExclude.contains( key );
    }

    private void processDGAFile() throws Exception {

        this.initFinalOutputFile( false, true );

        try (BufferedReader dgaReader = new BufferedReader( new FileReader( dgaFile ) )) {
            String line;

            while ( ( line = dgaReader.readLine() ) != null ) {

                // found a term
                if ( line.contains( "DOID" ) ) {
                    // this being of the url could change make sure its still correct if something doesn't work
                    String valueUri =
                            "http://purl.obolibrary.org/obo/DOID_" + this.findStringBetweenSpecialCharacter( line );

                    String geneId = this.findStringBetweenSpecialCharacter( dgaReader.readLine(), "GeneID" );
                    String pubMedID = this.findStringBetweenSpecialCharacter( dgaReader.readLine(), "PubMedID" );
                    String geneRIF = this.findStringBetweenSpecialCharacter( dgaReader.readLine(), "GeneRIF" );

                    OntologyTerm o = this.findOntologyTermExistAndNotObsolote( valueUri );

                    if ( o != null ) {

                        String geneSymbol = this.geneToSymbol( new Integer( geneId ) );
                        // gene do exist
                        if ( geneSymbol != null ) {

                            String key = geneId + pubMedID + geneRIF + o.getUri();

                            // if deep >3 always keep
                            int howDeepIdTerm = this.findHowManyParents( o, 0 );

                            // keep leaf or deep enough or uri=DOID_162(cancer)
                            if ( !( ( o.getChildren( true ).size() != 0 && howDeepIdTerm < 2 ) || o.getUri()
                                    .contains( "DOID_162" ) ) ) {

                                // negative
                                if ( ( geneRIF.contains( " is not " ) || geneRIF.contains( " not associated " )
                                        || geneRIF.contains( " no significant " ) || geneRIF
                                        .contains( " no association " ) || geneRIF.contains( " not significant " )
                                        || geneRIF.contains( " not expressed " ) ) && !geneRIF
                                        .contains( "is associated" ) && !geneRIF.contains( "is significant" )
                                        && !geneRIF.contains( "is not only" ) && !geneRIF.contains( "is expressed" ) ) {

                                    if ( this.lineToInclude( key ) ) {
                                        outFinalResults
                                                .write( geneSymbol + "\t" + geneId + "\t" + pubMedID + "\t" + "IEA"
                                                        + "\t" + "GeneRIF: " + geneRIF + "\t" + DgaDatabaseImporter.DGA
                                                        + "\t" + "" + "\t" + "" + "\t" + "" + "\t" + o.getUri() + "\t"
                                                        + "1" + "\n" );
                                    }

                                }
                                // positive
                                else {
                                    if ( this.lineToInclude( key ) ) {
                                        outFinalResults
                                                .write( geneSymbol + "\t" + geneId + "\t" + pubMedID + "\t" + "IEA"
                                                        + "\t" + "GeneRIF: " + geneRIF + "\t" + DgaDatabaseImporter.DGA
                                                        + "\t" + "" + "\t" + "" + "\t" + "" + "\t" + o.getUri() + "\t"
                                                        + "" + "\n" );
                                    }
                                }

                                outFinalResults.flush();
                            }
                        } else {
                            AbstractCLI.log.info( "gene NCBI no found in Gemma discard this eidence: ncbi: " + geneId );
                        }

                    } else {
                        AbstractCLI.log.info( "Ontology term not found in Ontology or obsolete : " + valueUri
                                + " (normal that this happen sometimes)" );
                    }
                }
            }
            dgaReader.close();
            outFinalResults.close();
        }
    }
}