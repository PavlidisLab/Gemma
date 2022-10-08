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

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.cli.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.context.SecurityContextHolder;
import ubic.gemma.core.util.AbstractCLI;
import ubic.gemma.core.util.CLI;
import ubic.gemma.persistence.util.SpringContextUtil;

import javax.annotation.Nullable;
import java.io.PrintWriter;
import java.util.*;

/**
 * Generic command line for Gemma. Commands are referred by shorthand names; this class prints out available commands
 * when given no arguments.
 *
 * @author paul
 */
@SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
@CommonsLog
public class GemmaCLI {

    private static final String
            HELP_OPTION = "h",
            HELP_ALL_OPTION = "ha",
            TESTING_OPTION = "testing"; // historically named '-testing', but now '--testing' is also accepted

    public static void main( String[] args ) {
        Options options = new Options()
                .addOption( HELP_OPTION, "help", false, "Show help" )
                .addOption( HELP_ALL_OPTION, false, "Show complete help with all available CLI commands" )
                .addOption( TESTING_OPTION, "testing", false, "Use the test environment" );
        CommandLine commandLine;
        try {
            commandLine = new DefaultParser().parse( options, args, true );
        } catch ( ParseException e ) {
            System.exit( 1 );
            return; // that's silly...
        }

        // quick help without loading the context
        if ( commandLine.hasOption( HELP_OPTION ) ) {
            GemmaCLI.printHelp( options, null );
            System.exit( 1 );
        }

        // check for the -testing flag to load the appropriate application context
        /* webapp */
        ApplicationContext ctx = SpringContextUtil.getApplicationContext( commandLine.hasOption( TESTING_OPTION ),
                "classpath*:ubic/gemma/cliContext-component-scan.xml" );

        /*
         * Guarantee that the security settings are uniform throughout the application (all threads).
         */
        SecurityContextHolder.setStrategyName( SecurityContextHolder.MODE_INHERITABLETHREADLOCAL );

        /*
         * Build a map from command names to classes.
         */
        Map<String, CLI> commandBeans = ctx.getBeansOfType( CLI.class );
        Map<String, CLI> commandsByName = new HashMap<>();
        SortedMap<CommandGroup, SortedMap<String, CLI>> commandGroups = new TreeMap<>( Comparator.comparingInt( CommandGroup::ordinal ) );
        for ( CLI cliInstance : commandBeans.values() ) {
            String commandName = cliInstance.getCommandName();
            if ( commandName == null || StringUtils.isBlank( commandName ) ) {
                // keep null to avoid printing some commands...
                continue;
            }
            commandsByName.put( commandName, cliInstance );
            CommandGroup g = cliInstance.getCommandGroup();
            if ( !commandGroups.containsKey( g ) ) {
                commandGroups.put( g, new TreeMap<>() );
            }
            commandGroups.get( g ).put( commandName, cliInstance );
        }

        // no command is passed
        if ( commandLine.getArgList().isEmpty() ) {
            System.err.println( "No command was supplied." );
            GemmaCLI.printHelp( options, commandGroups );
            System.exit( 1 );
        }

        // the first element of the remaining args is the command and the rest are the arguments
        LinkedList<String> commandArgs = new LinkedList<>( commandLine.getArgList() );
        String commandRequested = commandArgs.remove( 0 );
        String[] argsToPass = commandArgs.toArray( new String[] {} );

        if ( !commandsByName.containsKey( commandRequested ) ) {
            System.err.println( "Unrecognized command: " + commandRequested );
            GemmaCLI.printHelp( options, commandGroups );
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
     */
    public static String getOptStringForLogging( Object[] argsToPass ) {
        return java.util.regex.Pattern.compile( "(-{1,2}p(?:assword)?)\\s+(.+?)\\b" )
                .matcher( StringUtils.join( argsToPass, " " ) ).replaceAll( "$1 XXXXXX" );
    }

    public static void printHelp( Options options, @Nullable SortedMap<CommandGroup, SortedMap<String, CLI>> commands ) {
        System.err.println( "============ Gemma CLI tools ============" );

        StringBuilder footer = new StringBuilder();
        if ( commands != null ) {
            footer.append( "Here is a list of available commands, grouped by category:" ).append( '\n' );
            footer.append( '\n' );
            for ( Map.Entry<CommandGroup, SortedMap<String, CLI>> entry : commands.entrySet() ) {
                if ( entry.getKey().equals( CommandGroup.DEPRECATED ) )
                    continue;
                Map<String, CLI> commandsInGroup = entry.getValue();
                footer.append( "---- " ).append( entry.getKey() ).append( " ----" ).append( '\n' );
                for ( Map.Entry<String, CLI> e : commandsInGroup.entrySet() ) {
                    footer.append( e.getKey() ).append( " - " ).append( e.getValue().getShortDesc() ).append( '\n' );
                }
                footer.append( '\n' );
            }
        }

        footer.append( "To get help for a specific tool, use: gemma-cli <commandName> --help\n" );
        footer.append( '\n' );
        footer.append( AbstractCLI.FOOTER );

        new HelpFormatter().printHelp( new PrintWriter( System.err, true ), 150, "gemma-cli <commandName> [options]",
                AbstractCLI.HEADER, options, HelpFormatter.DEFAULT_LEFT_PAD, HelpFormatter.DEFAULT_DESC_PAD, footer.toString() );
    }

    // order here is significant.
    public enum CommandGroup {
        EXPERIMENT, PLATFORM, ANALYSIS, METADATA, PHENOTYPES, SYSTEM, MISC, DEPRECATED
    }
}
