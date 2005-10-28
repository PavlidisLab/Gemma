
package edu.columbia.gemma.loader.association;


import java.io.IOException;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanFactory;

import edu.columbia.gemma.util.SpringContextUtil;
import edu.columbia.gemma.genome.GeneDao;
import edu.columbia.gemma.association.LiteratureAssociationDao;
import edu.columbia.gemma.loader.loaderutils.PersisterHelper;


/**
 * Command line interface to retrieve and load literature associations
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2005 Columbia University
 * 
 * @author anshu 
 * @version $Id$
 */
public class LiteratureAssociationLoaderCLI {
    protected static final Log log = LogFactory.getLog( LiteratureAssociationLoaderCLI.class );

    //private PersisterHelper mPersister;
    private GeneDao geneDao;
    private LiteratureAssociationDao laDao;
    

    /**
     * Command line interface to run the gene parser/loader
     * 
     * @param args
     * @throws ConfigurationException
     * @throws IOException
     */
    public static void main( String args[] ) throws IOException {
       
        LiteratureAssociationLoaderCLI cli = new LiteratureAssociationLoaderCLI();

        // options stage
        OptionBuilder.withDescription( "Print help for this application" );
        Option helpOpt = OptionBuilder.create( 'h' );

        OptionBuilder.hasArg();
        OptionBuilder.withDescription( "Specify file (requires file arg) and load database" );
        Option loadOpt = OptionBuilder.create( 'l' );

        OptionBuilder.withDescription( "Remove literature associations from database" );
        Option removeOpt = OptionBuilder.create( 'r' );

        Options opt = new Options();
        opt.addOption( helpOpt );
        opt.addOption( loadOpt );
        opt.addOption( removeOpt );

        
        try {
            // parser stage

            BasicParser parser = new BasicParser();
            CommandLine cl = parser.parse( opt, args );

            LitAssociationFileParserImpl assocParser = new LitAssociationFileParserImpl(LitAssociationFileParserImpl.PERSIST_CONCURRENTLY,cli.geneDao,cli.laDao);

            // interrogation stage
            if ( cl.hasOption( 'l' ) ) {

                String filename = cl.getOptionValue( 'l' );
                System.out.println("option l: "+filename);

                assocParser.parse(filename);
                //cli.getGenePersister().persist( geneInfoParser.getResults() );

            } else if ( cl.hasOption( 'r' ) ) {
                System.out.println("option r ");
                assocParser.removeAll();
            } else {
                printHelp( opt );
            }
            
        } catch ( ParseException e ) {
            printIncorrectUsage(opt,e.toString());
        }
        
    }

    public LiteratureAssociationLoaderCLI() {
        BeanFactory ctx = SpringContextUtil.getApplicationContext( false );
        //mPersister = new PersisterHelper();
        geneDao = ( GeneDao ) ctx.getBean( "geneDao" ) ;
        laDao = ( LiteratureAssociationDao ) ctx.getBean( "LiteratureAssociationDao" ) ;
    }

    /**
     * @param opt
     */
    private static void printHelp( Options opt ) {
        HelpFormatter h = new HelpFormatter();
        h.printHelp( "Options Tip", opt );
    }

    private static void printIncorrectUsage( Options opt, String errorString ) {
        HelpFormatter h = new HelpFormatter();
        h.printHelp( "Incorrect Usage: "+errorString, opt );
    }

}
