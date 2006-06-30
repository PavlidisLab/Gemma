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
package ubic.gemma.loader.util;

import org.apache.commons.cli.AlreadySelectedException;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.UnrecognizedOptionException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author pavlidis
 * @version $Id$
 */
public abstract class AbstractCLI {

    private static final String HEADER = "Options:";
    private static final String FOOTER = "The Gemma project, Copyright (c) 2006 University of British Columbia\n"
            + "For more information, visit http://www.neurogemma.org/";
    protected static Options options = new Options();
    protected static CommandLine commandLine;
    protected static final Log log = LogFactory.getLog( AbstractSpringAwareCLI.class );

    @SuppressWarnings("static-access")
    protected void buildStandardOptions() {
        Option helpOpt = new Option( "h", "help", false, "Print this message" );
        Option testOpt = new Option( "testing", false, "Use the test environment" );

        options.addOption( helpOpt );
        options.addOption( testOpt );

    }

    protected abstract void buildOptions();

    /**
     * This must be called in your main method.
     * 
     * @param args
     * @throws ParseException
     */
    protected final void initCommandParse( String commandName, String[] args ) {
        /* COMMAND LINE PARSER STAGE */
        BasicParser parser = new BasicParser();

        if ( args == null ) {
            printHelp( commandName );
            System.exit( 0 );
        }

        try {
            commandLine = parser.parse( options, args );
        } catch ( ParseException e ) {

            if ( e instanceof MissingOptionException ) {
                System.out.println( "Required option(s) were not supplied: " + e.getMessage() );
            } else if ( e instanceof AlreadySelectedException ) {
                System.out.println( "The option(s) " + e.getMessage() + " were already selected" );
            } else if ( e instanceof MissingArgumentException ) {
                System.out.println( "Missing argument(s) " + e.getMessage() );
            } else if ( e instanceof UnrecognizedOptionException ) {
                System.out.println( "Unrecognized option " + e.getMessage() );
            } else {
                e.printStackTrace();
            }

            printHelp( commandName );

            System.exit( 0 );
        }

        /* INTERROGATION STAGE */
        if ( commandLine.hasOption( 'h' ) ) {
            printHelp( commandName );
            System.exit( 0 );
        }

        processOptions();

    }

    /**
     * Override this to provide processing of options. It is called at the end of initCommandParse
     */
    protected abstract void processOptions();

    /**
     * @param command The name of the command as used at the command line.
     */
    protected static void printHelp( String command ) {
        HelpFormatter h = new HelpFormatter();
        h.printHelp( command, HEADER, options, FOOTER );
    }

}
