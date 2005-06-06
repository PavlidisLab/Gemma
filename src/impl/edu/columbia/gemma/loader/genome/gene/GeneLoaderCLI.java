package edu.columbia.gemma.loader.genome.gene;

import java.io.IOException;
import java.util.Map;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanFactory;

import edu.columbia.gemma.genome.GeneDao;
import edu.columbia.gemma.genome.TaxonDao;
import edu.columbia.gemma.loader.loaderutils.Utilities;
import edu.columbia.gemma.util.SpringContextUtil;

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
    protected static final Log log = LogFactory.getLog( Parser.class );

    /**
     * Command line interface to run the gene parser/loader
     * 
     * @param args
     * @throws ConfigurationException
     * @throws IOException
     */
    public static void main( String args[] ) throws ConfigurationException, IOException {
        StopWatch stopwatch;
        try {
            // options stage
            OptionBuilder.withDescription( "Print help for this application" );
            Option helpOpt = OptionBuilder.create( 'h' );

            OptionBuilder.hasArg();
            OptionBuilder.withDescription( "Parse File (requires file arg)" );
            Option parseOpt = OptionBuilder.create( 'p' );

            OptionBuilder.hasArgs();
            OptionBuilder.withDescription( "1) Specify files\n" + "2) Load database with entries from file" );
            Option loadOpt = OptionBuilder.create( 'l' );

            OptionBuilder.withDescription( "Remove genes from database" );
            Option removeOpt = OptionBuilder.create( 'r' );

            Options opt = new Options();
            opt.addOption( helpOpt );
            opt.addOption( parseOpt );
            opt.addOption( loadOpt );
            opt.addOption( removeOpt );

            // parser stage
            BasicParser parser = new BasicParser();
            CommandLine cl = parser.parse( opt, args );

            BeanFactory ctx = SpringContextUtil.getApplicationContext();

            GeneLoaderImpl geneLoader;

            GeneParserImpl geneParser = new GeneParserImpl();
            GeneMappings geneMappings = new GeneMappings( ( TaxonDao ) ctx.getBean( "taxonDao" ) );
            geneParser.setGeneMappings( geneMappings );

            // interrogation stage
            if ( cl.hasOption( 'h' ) ) {
                printHelp( opt );

            } else if ( cl.hasOption( 'p' ) ) {
                geneParser.parseFile( cl.getOptionValue( 'p' ) );
            } else if ( cl.hasOption( 'l' ) ) {
                geneLoader = new GeneLoaderImpl();
                geneLoader.setGeneDao( ( GeneDao ) ctx.getBean( "geneDao" ) );

                Map map;
                String[] filenames = cl.getOptionValues( 'l' );

                stopwatch = new StopWatch();
                stopwatch.start();
                log.info( "Timer started" );

                for ( int i = 0; i < filenames.length - 1; i++ ) {
                    geneParser.parseFile( filenames[i] );
                    i++;
                }
                map = geneParser.parseFile( filenames[filenames.length - 1] );
                geneLoader.create( map.values() );

                stopwatch.stop();
                Utilities.displayTime( stopwatch );

            } else if ( cl.hasOption( 'r' ) ) {
                geneLoader = new GeneLoaderImpl();
                geneLoader.setGeneDao( ( GeneDao ) ctx.getBean( "geneDao" ) );
                geneLoader.removeAll();
            } else {
                printHelp( opt );
            }
        } catch ( ParseException e ) {
            e.printStackTrace();
        }
    }

    /**
     * @param opt
     */
    private static void printHelp( Options opt ) {
        HelpFormatter h = new HelpFormatter();
        h.printHelp( "Options Tip", opt );
    }

}
