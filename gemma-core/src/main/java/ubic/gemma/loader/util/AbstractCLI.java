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
import java.util.Collection;
import java.util.List;

import org.apache.commons.cli.AlreadySelectedException;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.OptionGroup;
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
    private Options options = new Options();
    private CommandLine commandLine;
    protected static final Log log = LogFactory.getLog( AbstractSpringAwareCLI.class );

    /* support for convenience options */

    protected String host;
    protected int port;
    protected String username;
    protected String password;

    public AbstractCLI() {
        this.buildStandardOptions();
        this.buildOptions();
    }

    @SuppressWarnings("static-access")
    protected void buildStandardOptions() {
        log.debug( "Creating standard options" );
        Option helpOpt = new Option( "h", "help", false, "Print this message" );
        Option testOpt = new Option( "testing", false, "Use the test environment" );

        options.addOption( helpOpt );
        options.addOption( testOpt );
    }

    protected abstract void buildOptions();

    /**
     * This must be called in your main method. It triggers parsing of the command line and processing of the options.
     * Check the error code to decide whether execution of your program should proceed.
     * 
     * @param args
     * @return int error code. Zero is normal condition.
     * @throws ParseException
     */
    protected final int processCommandLine( String commandName, String[] args ) {
        /* COMMAND LINE PARSER STAGE */
        BasicParser parser = new BasicParser();

        if ( args == null ) {
            printHelp( commandName );
            return -1;
        }

        try {
            commandLine = parser.parse( options, args );
        } catch ( ParseException e ) {
            if ( e instanceof MissingOptionException ) {
                System.out.println( "Required option(s) were not supplied: " + e.getMessage() );

            } else if ( e instanceof AlreadySelectedException ) {
                System.out.println( "The option(s) " + e.getMessage() + " were already selected" );
            } else if ( e instanceof MissingArgumentException ) {
                System.out.println( "Missing argument: " + e.getMessage() );
            } else if ( e instanceof UnrecognizedOptionException ) {
                System.out.println( e.getMessage() );
            } else {
                e.printStackTrace();
            }

            printHelp( commandName );

            if ( log.isDebugEnabled() ) {
                log.debug( e );
            }

            return -1;
        }

        /* INTERROGATION STAGE */
        if ( commandLine.hasOption( 'h' ) ) {
            printHelp( commandName );
            return -1;
        }

        processStandardOptions();
        processOptions();

        return 0;

    }

    /**
     * Stop exeucting the CLI.
     * <p>
     * FIXME figure out a way to do this without stopping subsequent unit tests, for example.
     */
    protected void bail( int errorCode ) {
        System.exit( errorCode );
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
            bail( 0 );
        }
        return 0.0;
    }

    protected double getDoubleOptionValue( char option ) {
        try {
            return Double.parseDouble( commandLine.getOptionValue( option ) );
        } catch ( NumberFormatException e ) {
            System.out.println( invalidOptionString( "" + option ) + ", not a valid double" );
            bail( 0 );
        }
        return 0.0;
    }

    protected int getIntegerOptionValue( String option ) {
        try {
            return Integer.parseInt( commandLine.getOptionValue( option ) );
        } catch ( NumberFormatException e ) {
            System.out.println( invalidOptionString( option ) + ", not a valid integer" );
            bail( 0 );
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
            bail( 0 );
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
            bail( 0 );
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
            bail( 0 );
        }
        return fileName;
    }

    /**
     * @param opt
     * @return
     * @see org.apache.commons.cli.Options#addOption(org.apache.commons.cli.Option)
     */
    public Options addOption( Option opt ) {
        return this.options.addOption( opt );
    }

    /**
     * @param opt
     * @param hasArg
     * @param description
     * @return
     * @see org.apache.commons.cli.Options#addOption(java.lang.String, boolean, java.lang.String)
     */
    public Options addOption( String opt, boolean hasArg, String description ) {
        return this.options.addOption( opt, hasArg, description );
    }

    /**
     * @param opt
     * @param longOpt
     * @param hasArg
     * @param description
     * @return
     * @see org.apache.commons.cli.Options#addOption(java.lang.String, java.lang.String, boolean, java.lang.String)
     */
    public Options addOption( String opt, String longOpt, boolean hasArg, String description ) {
        return this.options.addOption( opt, longOpt, hasArg, description );
    }

    /**
     * @param group
     * @return
     * @see org.apache.commons.cli.Options#addOptionGroup(org.apache.commons.cli.OptionGroup)
     */
    public Options addOptionGroup( OptionGroup group ) {
        return this.options.addOptionGroup( group );
    }

    /**
     * @param opt
     * @return
     * @see org.apache.commons.cli.Options#getOption(java.lang.String)
     */
    public Option getOption( String opt ) {
        return this.options.getOption( opt );
    }

    /**
     * @param opt
     * @return
     * @see org.apache.commons.cli.Options#getOptionGroup(org.apache.commons.cli.Option)
     */
    public OptionGroup getOptionGroup( Option opt ) {
        return this.options.getOptionGroup( opt );
    }

    /**
     * @return
     * @see org.apache.commons.cli.Options#getOptions()
     */
    public Collection getOptions() {
        return this.options.getOptions();
    }

    /**
     * @return
     * @see org.apache.commons.cli.Options#getRequiredOptions()
     */
    public List getRequiredOptions() {
        return this.options.getRequiredOptions();
    }
}
