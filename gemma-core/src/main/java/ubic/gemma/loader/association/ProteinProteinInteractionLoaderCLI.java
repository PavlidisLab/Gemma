/*
 * The Gemma project
 * 
 * Copyright (c) 2006 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package ubic.gemma.loader.association;

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

import ubic.gemma.model.association.ProteinProteinInteractionDao;
import ubic.gemma.model.common.description.ExternalDatabaseDao;
import ubic.gemma.model.genome.gene.GeneProductDao;
import ubic.gemma.security.authentication.ManualAuthenticationProcessing;
import ubic.gemma.util.SpringContextUtil;

/**
 * Command line interface to retrieve and load literature associations
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2006 University of British Columbia
 * 
 * @author anshu
 * @version $Id$
 */
public class ProteinProteinInteractionLoaderCLI {
    protected static final Log log = LogFactory.getLog( ProteinProteinInteractionLoaderCLI.class );
    protected static BeanFactory ctx = null;
    protected static ManualAuthenticationProcessing manAuthentication = null;

    private static final String USAGE = "[-h] [-u <username>] [-p <password>]  [-t <true|false>] [-l <file>] [-r] ";
    private static final String HEADER = "The Gemma project, Copyright (c) 2006 University of British Columbia";
    private static final String FOOTER = "For more information, see our website at http://www.neurogemma.org";

    // private PersisterHelper mPersister;
    private GeneProductDao gpDao;
    private ProteinProteinInteractionDao ppiDao;
    private ExternalDatabaseDao dbDao;
    private static String username = null;
    private static String password = null;

    /**
     * Command line interface to run the gene parser/loader
     * 
     * @param args
     * @throws ConfigurationException
     * @throws IOException
     */
    public static void main( String args[] ) throws IOException {

        ProteinProteinInteractionLoaderCLI cli = null;

        // options stage
        /* help */
        OptionBuilder.withDescription( "Print help for this application" );
        Option helpOpt = OptionBuilder.create( 'h' );

        /* username */
        OptionBuilder.hasArgs();
        OptionBuilder.withDescription( "Username" );
        Option usernameOpt = OptionBuilder.create( 'u' );

        /* password */
        OptionBuilder.hasArgs();
        OptionBuilder.withDescription( "Password" );
        Option passwordOpt = OptionBuilder.create( 'p' );

        /* environment (test or prod) */
        OptionBuilder.hasArgs();
        OptionBuilder.withDescription( "Set use of test or production environment" );
        Option testOpt = OptionBuilder.create( 't' );

        /* load */
        OptionBuilder.hasArg();
        OptionBuilder.withDescription( "Specify file (requires file arg) and load database" );
        Option loadOpt = OptionBuilder.create( 'l' );

        /* remove */
        OptionBuilder.withDescription( "Remove protein-protein interactions from database" );
        Option removeOpt = OptionBuilder.create( 'r' );

        Options opt = new Options();
        opt.addOption( helpOpt );
        opt.addOption( usernameOpt );
        opt.addOption( passwordOpt );
        opt.addOption( testOpt );
        opt.addOption( loadOpt );
        opt.addOption( removeOpt );

        try {
            // parser stage
            BasicParser parser = new BasicParser();
            CommandLine cl = parser.parse( opt, args );

            /* check if using test or production context */
            if ( cl.hasOption( 't' ) ) {
                boolean isTest = Boolean.parseBoolean( cl.getOptionValue( 't' ) );
                if ( isTest )
                    ctx = SpringContextUtil.getApplicationContext( true );
                else
                    ctx = SpringContextUtil.getApplicationContext( false );

                cli = new ProteinProteinInteractionLoaderCLI();
            }
            // if no ctx is set, default to test environment.
            else {
                ctx = SpringContextUtil.getApplicationContext( true );
                cli = new ProteinProteinInteractionLoaderCLI();
            }

            /* check username and password. */
            if ( cl.hasOption( 'u' ) ) {
                if ( cl.hasOption( 'p' ) ) {
                    username = cl.getOptionValue( 'u' );
                    password = cl.getOptionValue( 'p' );
                    manAuthentication = ( ManualAuthenticationProcessing ) ctx
                            .getBean( "manualAuthenticationProcessing" );
                    manAuthentication.validateRequest( username, password );
                }
            } else {
                log.error( "Not authenticated.  Make sure you entered a valid username and/or password" );
                System.exit( 0 );
            }
            PPIFileParser assocParser = new PPIFileParser( cli.gpDao, cli.ppiDao, cli.dbDao );

            // interrogation stage
            if ( cl.hasOption( 'l' ) ) {

                String filename = cl.getOptionValue( 'l' );
                System.out.println( "option l: " + filename );

                assocParser.parse( filename );

            } else if ( cl.hasOption( 'r' ) ) {
                System.out.println( "option r " );
                assocParser.removeAll();
            } else {
                printHelp( opt );
            }

        } catch ( ParseException e ) {
            printIncorrectUsage( opt, e.toString() );
        }

    }

    public ProteinProteinInteractionLoaderCLI() {
        ctx = SpringContextUtil.getApplicationContext( false );
        // mPersister = new PersisterHelper();
        // geneDao = ( GeneDao ) ctx.getBean( "geneDao" ) ;
        gpDao = ( GeneProductDao ) ctx.getBean( "geneProductDao" );
        ppiDao = ( ProteinProteinInteractionDao ) ctx.getBean( "ProteinProteinInteractionDao" );
        dbDao = ( ExternalDatabaseDao ) ctx.getBean( "externalDatabaseDao" );
    }

    /**
     * @param opt
     */
    private static void printHelp( Options opt ) {
        HelpFormatter h = new HelpFormatter();
        // h.printHelp( "Options Tip", opt );
        h.printHelp( USAGE, HEADER, opt, FOOTER );
    }

    private static void printIncorrectUsage( Options opt, String errorString ) {
        HelpFormatter h = new HelpFormatter();
        h.printHelp( "Incorrect Usage: " + errorString, opt );
    }

}
