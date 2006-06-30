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

import java.io.File;
import java.util.List;

import org.apache.commons.cli.AlreadySelectedException;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PatternOptionBuilder;
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
    protected Options options = new Options();
    protected CommandLine commandLine;
    protected static final Log log = LogFactory.getLog( AbstractSpringAwareCLI.class );

    /* support for convenience options */

    protected String host;
    protected int port;
    protected String username;
    protected String password;

    @SuppressWarnings("static-access")
    protected void buildStandardOptions() {
        Option helpOpt = new Option( "h", "help", false, "Print this message" );
        Option testOpt = new Option( "testing", false, "Use the test environment" );

        options.addOption( helpOpt );
        options.addOption( testOpt );
    }

    protected abstract void buildOptions();

    /**
     * This must be called in your main method. It triggers parsing of the command line and processing of the options.
     * 
     * @param args
     * @throws ParseException
     */
    protected final void processCommandLine( String commandName, String[] args ) {
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

        processStandardOptions();

        processOptions();

    }

    /**
     * FIXME this causes subclasses to be unable to safely use 'h', 'p', 'u' and 'P' for their own purposes.
     */
    private void processStandardOptions() {

        if ( commandLine.hasOption( 'h' ) ) {
            this.host = commandLine.getOptionValue( 'h' );
        }

        if ( commandLine.hasOption( 'P' ) ) {
            this.port = getIntegerOptionValue( 'P' );
        }

        if ( commandLine.hasOption( 'u' ) ) {
            this.username = commandLine.getOptionValue( 'u' );
        }

        if ( commandLine.hasOption( 'p' ) ) {
            this.password = commandLine.getOptionValue( 'p' );
        }

    }

    /**
     * Implement this to provide processing of options. It is called at the end of processCommandLine.
     */
    protected abstract void processOptions();

    /**
     * @param command The name of the command as used at the command line.
     */
    protected void printHelp( String command ) {
        HelpFormatter h = new HelpFormatter();
        h.printHelp( command, HEADER, options, FOOTER );
    }

    public List getArgList() {
        return commandLine.getArgList();
    }

    public String[] getArgs() {
        return commandLine.getArgs();
    }

    public boolean hasOption( char opt ) {
        return commandLine.hasOption( opt );
    }

    public boolean hasOption( String opt ) {
        return commandLine.hasOption( opt );
    }

    public Object getOptionObject( char opt ) {
        return commandLine.getOptionObject( opt );
    }

    public Object getOptionObject( String opt ) {
        return commandLine.getOptionObject( opt );
    }

    public Option[] getOptions() {
        return commandLine.getOptions();
    }

    public String getOptionValue( char opt, String defaultValue ) {
        return commandLine.getOptionValue( opt, defaultValue );
    }

    public String getOptionValue( char opt ) {
        return commandLine.getOptionValue( opt );
    }

    public String getOptionValue( String opt, String defaultValue ) {
        return commandLine.getOptionValue( opt, defaultValue );
    }

    public String getOptionValue( String opt ) {
        return commandLine.getOptionValue( opt );
    }

    public String[] getOptionValues( char opt ) {
        return commandLine.getOptionValues( opt );
    }

    public String[] getOptionValues( String opt ) {
        return commandLine.getOptionValues( opt );
    }

    /**
     * Convenience method to add a standard pair of (required) options to intake a user name and password.
     */
    @SuppressWarnings("static-access")
    protected void addUserNameAndPasswordOptions() {
        Option usernameOpt = OptionBuilder.withArgName( "user" ).isRequired().withLongOpt( "user" ).hasArg()
                .withDescription( "User name for accessing the system" ).create( 'u' );

        Option passwordOpt = OptionBuilder.withArgName( "passwd" ).isRequired().withLongOpt( "password" ).hasArg()
                .withDescription( "Password for accessing the system" ).create( 'p' );
        options.addOption( usernameOpt );
        options.addOption( passwordOpt );
    }

    /**
     * Convenience method to add a standard pair of options to intake a host name and port number. *
     * 
     * @param hostRequired Whether the host name is required
     * @param portRequired Whether the port is required
     */
    @SuppressWarnings("static-access")
    protected void addHostAndPortOptions( boolean hostRequired, boolean portRequired ) {
        Option hostOpt = OptionBuilder.withArgName( "host" ).withLongOpt( "host" ).hasArg().withDescription(
                "Hostname to use" ).create( 'h' );

        hostOpt.setRequired( hostRequired );

        Option portOpt = OptionBuilder.withArgName( "port" ).isRequired().withLongOpt( "port" ).hasArg()
                .withDescription( "Port to use on host" ).create( 'P' );

        portOpt.setRequired( portRequired );

        options.addOption( hostOpt );
        options.addOption( portOpt );
    }

    protected double getDoubleOptionValue( String option ) {
        try {
            return Double.parseDouble( commandLine.getOptionValue( option ) );
        } catch ( NumberFormatException e ) {
            System.out.println( invalidOptionString( option ) + ", not a valid double" );
            System.exit( 0 );
        }
        return 0.0;
    }

    protected double getDoubleOptionValue( char option ) {
        try {
            return Double.parseDouble( commandLine.getOptionValue( option ) );
        } catch ( NumberFormatException e ) {
            System.out.println( invalidOptionString( "" + option ) + ", not a valid double" );
            System.exit( 0 );
        }
        return 0.0;
    }

    protected int getIntegerOptionValue( String option ) {
        try {
            return Integer.parseInt( commandLine.getOptionValue( option ) );
        } catch ( NumberFormatException e ) {
            System.out.println( invalidOptionString( option ) + ", not a valid integer" );
            System.exit( 0 );
        }
        return 0;
    }

    private String invalidOptionString( String option ) {
        return "Invalid value '" + commandLine.getOptionValue( option ) + " for option " + option;
    }

    protected int getIntegerOptionValue( char option ) {
        try {
            return Integer.parseInt( commandLine.getOptionValue( option ) );
        } catch ( NumberFormatException e ) {
            System.out.println( invalidOptionString( "" + option ) + ", not a valid integer" );
            System.exit( 0 );
        }
        return 0;
    }

    /**
     * @param c
     * @return
     */
    protected String getFileNameOptionValue( char c ) {
        String fileName = commandLine.getOptionValue( c );
        File f = new File( fileName );
        if ( !f.canRead() ) {
            System.out.println( invalidOptionString( "" + c ) + ", cannot read from file" );
            System.exit( 0 );
        }
        return fileName;
    }

    /**
     * @param c
     * @return
     */
    protected String getFileNameOptionValue( String c ) {
        String fileName = commandLine.getOptionValue( c );
        File f = new File( fileName );
        if ( !f.canRead() ) {
            System.out.println( invalidOptionString( "" + c ) + ", cannot read from file" );
            System.exit( 0 );
        }
        return fileName;
    }
}
