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

package ubic.gemma.loader.genome.gene;

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

import ubic.gemma.model.genome.GeneDao;
import ubic.gemma.model.genome.gene.GeneProductDao;
import ubic.gemma.security.authentication.ManualAuthenticationService;
import ubic.gemma.util.SpringContextUtil;

/**
 * Command line interface to protein parsing and loading
 * <hr>
 * <p>
 * 
 * @author anshu
 * @version $Id$
 * @deprecated
 */
@Deprecated
public class ProteinLoaderCLI {

    // TODO extend AbstractCLI

    protected static final Log log = LogFactory.getLog( ProteinLoaderCLI.class );
    protected static ManualAuthenticationService manAuthentication = null;
    protected static BeanFactory ctx = null;

    private static final String USAGE = "[-h] [-u <username>] [-p <password>]  [-t <true|false>] [-x <file>] [-l <file>] [-r] ";
    private static final String HEADER = "The Gemma project, Copyright (c) 2006 University of British Columbia";
    private static final String FOOTER = "For more information, see our website at http://www.neurogemma.org";
    // private PersisterHelper mPersister;
    private GeneDao geneDao;
    private GeneProductDao gpDao;
    private static String username = null;
    private static String password = null;

    // FIXME this should use the SDOG (source domain object generator)

    /**
     * Command line interface to run the gene parser/loader
     * 
     * @param args
     * @throws ConfigurationException
     * @throws IOException
     */
    public static void main( String args[] ) throws IOException {
        ProteinLoaderCLI cli = null;

        try {
            /* OPTIONS STAGE */

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

            /* parse and load */
            OptionBuilder.hasArgs();
            OptionBuilder.withDescription( "Specify file (requires file arg) and load database with entries from file" );
            Option loadOpt = OptionBuilder.create( 'l' );

            /* remove */
            OptionBuilder.withDescription( "Remove gene products (proteins, etc.) from database" );
            Option removeOpt = OptionBuilder.create( 'r' );

            Options opt = new Options();
            opt.addOption( helpOpt );
            opt.addOption( usernameOpt );
            opt.addOption( passwordOpt );
            opt.addOption( testOpt );
            opt.addOption( loadOpt );
            opt.addOption( removeOpt );

            /* COMMAND LINE PARSER STAGE */
            BasicParser parser = new BasicParser();
            CommandLine cl = parser.parse( opt, args );

            /* INTERROGATION STAGE */
            if ( cl.hasOption( 'h' ) ) {
                printHelp( opt );
                System.exit( 0 );

            }

            /* check if using test or production context */
            if ( cl.hasOption( 't' ) ) {
                boolean isTest = Boolean.parseBoolean( cl.getOptionValue( 't' ) );
                if ( isTest )
                    ctx = SpringContextUtil.getApplicationContext( true, false, false );
                else
                    ctx = SpringContextUtil.getApplicationContext( false, false, false );

                cli = new ProteinLoaderCLI();
            }
            // if no ctx is set, default to test environment.
            else {
                ctx = SpringContextUtil.getApplicationContext( true, false, false );
                cli = new ProteinLoaderCLI();
            }

            /* check username and password. */
            if ( cl.hasOption( 'u' ) ) {
                if ( cl.hasOption( 'p' ) ) {
                    username = cl.getOptionValue( 'u' );
                    password = cl.getOptionValue( 'p' );
                    manAuthentication = ( ManualAuthenticationService ) ctx.getBean( "manualAuthenticationProcessing" );
                    manAuthentication.validateRequest( username, password );
                }
            } else {
                log.error( "Not authenticated.  Make sure you entered a valid username and/or password" );
                System.exit( 0 );
            }

            ProteinFileParser protInfoParser = new ProteinFileParser( cli.geneDao, cli.gpDao );
            /* check load option. */
            if ( cl.hasOption( 'l' ) ) {

                String filename = cl.getOptionValue( 'l' );
                System.out.println( "option l: " + filename );

                protInfoParser.parse( filename );

            }

            /* check remove option. */
            else if ( cl.hasOption( 'r' ) ) {
                protInfoParser.removeAll();
            }
            /* defaults to print help. */
            else {
                printHelp( opt );
            }
        } catch ( ParseException e ) {
            e.printStackTrace();
        }
    }

    public ProteinLoaderCLI() {
        ctx = SpringContextUtil.getApplicationContext( false, false, false );
        geneDao = ( GeneDao ) ctx.getBean( "geneDao" );
        gpDao = ( GeneProductDao ) ctx.getBean( "geneProductDao" );
        // dbDao = ( ExternalDatabaseDao ) ctx.getBean( "externalDatabaseDao" ) ;
    }

    /**
     * @param opt
     */
    private static void printHelp( Options opt ) {
        HelpFormatter h = new HelpFormatter();
        // h.setWidth( 80 );
        h.printHelp( USAGE, HEADER, opt, FOOTER );
    }

}