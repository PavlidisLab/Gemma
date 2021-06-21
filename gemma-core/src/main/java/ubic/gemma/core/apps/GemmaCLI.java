/*
 * The Gemma project
 *
 * Copyright (c) 2008 University of British Columbia
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
package ubic.gemma.core.apps;

import org.apache.commons.cli.*;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.context.SecurityContextHolder;
import ubic.gemma.core.util.AbstractCLI;
import ubic.gemma.core.util.CLI;
import ubic.gemma.persistence.util.SpringContextUtil;

import java.util.*;

/**
 * Generic command line for Gemma. Commands are referred by shorthand names; this class prints out available commands
 * when given no arguments.
 *
 * @author paul
 */
@SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
public class GemmaCLI {

    private static final String HELP_OPTION = "help",
            TESTING_OPTION = "testing";


    private static ApplicationContext ctx;

    public static void main( String[] args ) {
        Options options = new Options()
                .addOption( HELP_OPTION, "--help", false, "Show Help" )
                .addOption( TESTING_OPTION, "--testing", false, "Enable testing environment" );
        CommandLineParser parser = new DefaultParser();
        CommandLine commandLine;
        try {
            commandLine = parser.parse( options, args, true );
        } catch ( ParseException e ) {
            System.exit( 1 );
            return; // that's silly...
        }

        // check for the -testing flag to load the appropriate application context
        ctx = SpringContextUtil.getApplicationContext( commandLine.hasOption( TESTING_OPTION ), false /* webapp */, new String[] {
                "classpath*:ubic/gemma/cliContext-component-scan.xml",
                "classpath*:ubic/gemma/cliContext-jms.xml",
                "classpath*:ubic/gemma/cliContext-scheduler.xml" } );

        /*
         * Guarantee that the security settings are uniform throughout the application (all threads).
         */
        SecurityContextHolder.setStrategyName( SecurityContextHolder.MODE_GLOBAL );

        /*
         * Build a map from command names to classes.
         */
        Map<String, CLI> commandBeans = ctx.getBeansOfType( CLI.class );
        Map<CommandGroup, Map<String, String>> commandGroups = new HashMap<>();
        Map<String, CLI> commandsByName = new HashMap<>();
        for ( Map.Entry<String, CLI> entry : commandBeans.entrySet() ) {
            String beanName = entry.getKey();
            CLI cliInstance = entry.getValue();
            String commandName = cliInstance.getCommandName();
            if ( commandName == null || StringUtils.isBlank( commandName ) ) {
                // keep null to avoid printing some commands...
                continue;
            }

            String desc = cliInstance.getShortDesc();

            CommandGroup g = cliInstance.getCommandGroup();

            if ( !commandGroups.containsKey( g ) ) {
                commandGroups.put( g, new TreeMap<>() );
            }

            commandsByName.put( commandName, cliInstance );
            commandGroups.get( g ).put( commandName, desc + " (" + beanName + ")" );
        }

        if ( commandLine.hasOption( HELP_OPTION ) ) {
            GemmaCLI.printHelp( commandGroups );
            System.exit( 1 );
        }

        // no command is passed
        if ( commandLine.getArgList().isEmpty() ) {
            GemmaCLI.printHelp( commandGroups );
            System.exit( 1 );
        }

        // the first element of the remaining args is the command and the rest are the arguments
        LinkedList<String> commandArgs = new LinkedList<>( commandLine.getArgList() );
        String commandRequested = commandArgs.remove( 0 );
        String[] argsToPass = commandArgs.toArray( new String[] {} );

        if ( !commandsByName.containsKey( commandRequested ) ) {
            System.err.println( "Unrecognized command: " + commandRequested );
            GemmaCLI.printHelp( commandGroups );
            System.err.println( "Unrecognized command: " + commandRequested );
            System.exit( 1 );
        } else {
            try {
                CLI cli = commandsByName.get( commandRequested );
                System.err.println( "========= Gemma CLI invocation of " + commandRequested + " ============" );
                System.err.println( "Options: " + GemmaCLI.getOptStringForLogging( argsToPass ) );
                System.exit( cli.executeCommand( argsToPass ) );
            } catch ( Exception e ) {
                System.err.println( "Gemma CLI error: " + e.getClass().getName() + " - " + e.getMessage() );
                System.err.println( ExceptionUtils.getStackTrace( e ) );
                throw new RuntimeException( e );
            } finally {
                System.err.println( "========= Gemma CLI run of " + commandRequested + " complete ============" );
            }
        }
    }

    /**
     * Mask password for logging
     *
     * @param argsToPass
     * @return
     */
    public static String getOptStringForLogging( Object[] argsToPass ) {
        return java.util.regex.Pattern.compile( "(-{1,2}p(?:assword)?)\\s+(.+?)\\b" )
                .matcher( StringUtils.join( argsToPass, " " ) ).replaceAll( "$1 XXXXXX" );
    }

    /**
     * Print help and exit
     *
     * @param commands
     */
    public static void printHelp( Map<CommandGroup, Map<String, String>> commands ) {
        System.err.println( "============ Gemma command line tools ============" );

        System.err.print( "To operate Gemma tools, run a command like:\n\njava [jre options] -classpath ${GEMMA_LIB} "
                + "GemmaCLI <commandName> [options]\n\n"
                + "You can use gemmaCli.sh as a shortcut as in 'gemmaCli.sh <commandName> [options]'.\n\n"
                + "Here is a list of available commands, grouped by category:\n" );

        for ( CommandGroup commandGroup : CommandGroup.values() ) {
            if ( commandGroup.equals( CommandGroup.DEPRECATED ) )
                continue;
            if ( !commands.containsKey( commandGroup ) )
                continue;
            Map<String, String> commandsInGroup = commands.get( commandGroup );
            if ( commandsInGroup.isEmpty() )
                continue;

            System.err.println( "\n---- " + commandGroup.toString() + " ----" );
            for ( String cmd : commandsInGroup.keySet() ) {
                if ( cmd == null )
                    continue; // just in case... but no command name means "skip";
                System.err.println( cmd + " - " + commandsInGroup.get( cmd ) );
            }
        }
        System.err.println( "\nTo get help for a specific tool, use \n\ngemmaCli.sh <commandName> --help" );
        System.err.print( "\n" + AbstractCLI.FOOTER + "\n=========================================\n" );
        System.exit( 0 );
    }

    // order here is significant.
    public enum CommandGroup {
        EXPERIMENT, PLATFORM, ANALYSIS, METADATA, PHENOTYPES, SYSTEM, MISC, DEPRECATED
    }
}
