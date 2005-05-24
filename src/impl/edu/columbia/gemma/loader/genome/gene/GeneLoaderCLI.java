package edu.columbia.gemma.loader.genome.gene;

import java.io.IOException;
import java.util.Map;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.configuration.ConfigurationException;

/**
 * Command line interface to gene parsing and loading
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2005 Columbia University
 * 
 * @author keshav
 * @version $Id$
 */
public class GeneLoaderCLI {
    static GeneLoader geneLoader;
    static GeneParser geneParser;

    /**
     * Command line interface to run the gene parser/loader
     * 
     * @param args
     * @throws ConfigurationException
     * @throws IOException
     */
    public static void main( String args[] ) throws ConfigurationException, IOException {
        try {
            // options stage
            Options opt = new Options();
            opt.addOption( "h", false, "Print help for this application" );
            opt.addOption( "p", true, "Parse file" );
            opt.addOption( "l", true, "1) Specify files\n" + "2) Load database with entries from file" );
            opt.addOption( "r", false, "Remove genes from database" );
            // parser stage
            BasicParser parser = new BasicParser();
            CommandLine cl = parser.parse( opt, args );
            // interrogation stage
            if ( cl.hasOption( 'h' ) ) {
                HelpFormatter f = new HelpFormatter();
                f.printHelp( "OptionsTip", opt );

            } else if ( cl.hasOption( 'p' ) ) {
                geneParser = new GeneParserImpl();
                geneParser.parseFile( cl.getOptionValue( 'f' ) );

            } else if ( cl.hasOption( 'l' ) ) {
                geneParser = new GeneParserImpl();
                geneLoader = new GeneLoaderImpl();
                Map map;
                String[] filenames = cl.getOptionValues( 'l' );
                System.err.println( filenames.length );
                for ( int i = 0; i < filenames.length - 1; i++ ) {
                    System.err.println( filenames[i] );
                    geneParser.parseFile( filenames[i] );
                    i++;
                }
                map = geneParser.parseFile( filenames[filenames.length - 1] );
                geneLoader.create( map.values() );

            } else if ( cl.hasOption( 'r' ) ) {
                geneLoader = new GeneLoaderImpl();
                geneLoader.removeAll();
            } else {
                // System.out.println(cl.getOptionValue("f"));
            }
        } catch ( ParseException e ) {
            e.printStackTrace();
        }
    }
}
