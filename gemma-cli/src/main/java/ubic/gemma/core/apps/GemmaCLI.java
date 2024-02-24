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
import ubic.gemma.core.logging.LoggingConfigurer;
import ubic.gemma.core.logging.log4j.Log4jConfigurer;
import ubic.gemma.core.util.*;
import ubic.gemma.persistence.util.SpringContextUtil;
import ubic.gemma.persistence.util.SpringProfiles;

import javax.annotation.Nullable;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
            COMPLETION_OPTION = "c",
            COMPLETION_SHELL_OPTION = "cs",
            VERSION_OPTION = "version",
            LOGGER_OPTION = "logger",
            VERBOSITY_OPTION = "v",
            TESTING_OPTION = "testing"; // historically named '-testing', but now '--testing' is also accepted

    /**
     * Pattern used to match password in the CLI arguments.
     * <p>
     * Passwords are no longer allowed as of 1.29.0, but some users might still supply their passwords.
     */
    private static final Pattern PASSWORD_IN_CLI_MATCHER = Pattern.compile( "(-{1,2}p(?:assword)?)\\s+(.+?)\\b" );

    private static final LoggingConfigurer loggingConfigurer = new Log4jConfigurer();

    public static void main( String[] args ) {
        Option logOpt = Option.builder( VERBOSITY_OPTION )
                .longOpt( "verbosity" ).hasArg()
                .desc( "Set verbosity level for all loggers (0=silent, 5=very verbose; default is custom, see log4j.properties)" )
                .type( Number.class )
                .build();
        Option otherLogOpt = Option.builder( LOGGER_OPTION )
                .longOpt( "logger" ).hasArg()
                .desc( "Configure a specific logger verbosity (0=silent, 5=very verbose; default is custom, see log4j.properties). For example, '--logger ubic.gemma=5' or '--logger org.hibernate.SQL=5'" )
                .build();
        Options options = new Options()
                .addOption( HELP_OPTION, "help", false, "Show help" )
                .addOption( HELP_ALL_OPTION, "help-all", false, "Show complete help with all available CLI commands" )
                .addOption( COMPLETION_OPTION, "completion", false, "Generate a completion script" )
                .addOption( COMPLETION_SHELL_OPTION, "completion-shell", true, "Indicate which shell to generate completion for. Only fish and bash are supported" )
                .addOption( VERSION_OPTION, "version", false, "Show Gemma version" )
                .addOption( otherLogOpt )
                .addOption( logOpt )
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
            GemmaCLI.printHelp( options, null, new PrintWriter( System.out, true ) );
            System.exit( 0 );
            return;
        }

        if ( commandLine.hasOption( VERSION_OPTION ) ) {
            BuildInfo buildInfo = BuildInfo.fromClasspath();
            System.out.printf( "Gemma version %s%n", buildInfo );
            System.exit( 0 );
            return;
        }

        if ( commandLine.hasOption( VERBOSITY_OPTION ) ) {
            try {
                loggingConfigurer.configureAllLoggers( ( ( Number ) commandLine.getParsedOptionValue( VERBOSITY_OPTION ) ).intValue() );
            } catch ( ParseException | IllegalArgumentException e ) {
                System.err.printf( "Failed to parse the %s option: %s.%n", VERBOSITY_OPTION,
                        ExceptionUtils.getRootCauseMessage( e ) );
                GemmaCLI.printHelp( options, null, new PrintWriter( System.err, true ) );
                System.exit( 1 );
                return;
            }
        }

        if ( commandLine.hasOption( LOGGER_OPTION ) ) {
            for ( String value : commandLine.getOptionValues( LOGGER_OPTION ) ) {
                String[] vals = value.split( "=" );
                if ( vals.length != 2 ) {
                    System.err.println( "Logging value must in format [loggerName]=[value]." );
                    System.exit( 1 );
                }
                String loggerName = vals[0];
                try {
                    loggingConfigurer.configureLogger( loggerName, Integer.parseInt( vals[1] ) );
                } catch ( IllegalArgumentException e ) {
                    System.err.printf( "Failed to parse the %s option for %s: %s.%n", LOGGER_OPTION,
                            loggerName,
                            ExceptionUtils.getRootCauseMessage( e ) );
                    GemmaCLI.printHelp( options, null, new PrintWriter( System.err, true ) );
                    System.exit( 1 );
                    return;
                }
            }
        }

        loggingConfigurer.apply();

        /*
         * Guarantee that the security settings are uniform throughout the application (all threads).
         */
        SecurityContextHolder.setStrategyName( SecurityContextHolder.MODE_INHERITABLETHREADLOCAL );

        List<String> profiles = new ArrayList<>();
        profiles.add( "cli" );

        // check for the -testing/--testing flag to load the appropriate application context
        if ( commandLine.hasOption( TESTING_OPTION ) ) {
            profiles.add( SpringProfiles.TEST );
        }

        ApplicationContext ctx = SpringContextUtil.getApplicationContext( profiles.toArray( new String[0] ) );

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

        if ( commandLine.hasOption( COMPLETION_OPTION ) ) {
            CompletionGenerator completionGenerator;
            String shellName;
            if ( commandLine.hasOption( COMPLETION_SHELL_OPTION ) ) {
                shellName = commandLine.getOptionValue( COMPLETION_SHELL_OPTION );
            } else {
                // attempt to guess the intended shell from $SHELL
                String shell = System.getenv( "SHELL" );
                if ( StringUtils.isNotBlank( shell ) ) {
                    shellName = Paths.get( System.getenv( "SHELL" ) ).getFileName().toString();
                } else {
                    System.err.println( "The $SHELL environment variable is not set, could not determine the shell to generate completion for." );
                    System.exit( 1 );
                    return;
                }
            }
            if ( shellName.equals( "bash" ) ) {
                completionGenerator = new BashCompletionGenerator( commandsByName.keySet() );
            } else if ( shellName.equals( "fish" ) ) {
                completionGenerator = new FishCompletionGenerator( commandsByName.keySet() );
            } else {
                System.err.printf( "Completion is not support for %s.%n", shellName );
                System.exit( 1 );
                return;
            }
            PrintWriter completionWriter = new PrintWriter( System.out );
            completionGenerator.beforeCompletion( completionWriter );
            completionGenerator.generateCompletion( options, completionWriter );
            for ( SortedMap<String, CLI> group : commandGroups.values() ) {
                for ( CLI cli : group.values() ) {
                    completionGenerator.generateSubcommandCompletion( cli.getCommandName(), cli.getOptions(), cli.getShortDesc(), cli.allowPositionalArguments(), completionWriter );
                }
            }
            completionGenerator.afterCompletion( completionWriter );
            completionWriter.flush();
            System.exit( 0 );
            return;
        }

        // no command is passed
        if ( commandLine.getArgList().isEmpty() ) {
            System.err.println( "No command was supplied." );
            GemmaCLI.printHelp( options, commandGroups, new PrintWriter( System.err, true ) );
            System.exit( 1 );
        }

        // the first element of the remaining args is the command and the rest are the arguments
        LinkedList<String> commandArgs = new LinkedList<>( commandLine.getArgList() );
        String commandRequested = commandArgs.remove( 0 );
        String[] argsToPass = commandArgs.toArray( new String[] {} );

        int statusCode;
        if ( !commandsByName.containsKey( commandRequested ) ) {
            System.err.println( "Unrecognized command: " + commandRequested );
            GemmaCLI.printHelp( options, commandGroups, new PrintWriter( System.err, true ) );
            statusCode = 1;
        } else {
            try {
                CLI cli = commandsByName.get( commandRequested );
                System.err.println( "========= Gemma CLI invocation of " + commandRequested + " ============" );
                System.err.println( "Options: " + GemmaCLI.getOptStringForLogging( argsToPass ) );
                statusCode = cli.executeCommand( argsToPass );
            } catch ( Exception e ) {
                System.err.println( "Gemma CLI error: " + e.getClass().getName() + " - " + e.getMessage() );
                System.err.println( ExceptionUtils.getStackTrace( e ) );
                statusCode = 1;
            } finally {
                System.err.println( "========= Gemma CLI run of " + commandRequested + " complete ============" );
            }
        }

        System.exit( statusCode );
    }

    /**
     * Mask password for logging
     */
    static String getOptStringForLogging( Object[] argsToPass ) {
        Matcher matcher = PASSWORD_IN_CLI_MATCHER.matcher( StringUtils.join( argsToPass, " " ) );
        if ( matcher.matches() ) {
            log.warn( "It seems that you still supply the -p/--password argument through the CLI. This feature has been removed for security purposes in Gemma 1.29." );
        }
        return matcher.replaceAll( "$1 XXXXXX" );
    }

    private static void printHelp( Options options, @Nullable SortedMap<CommandGroup, SortedMap<String, CLI>> commands, PrintWriter writer ) {
        writer.println( "============ Gemma CLI tools ============" );

        StringBuilder footer = new StringBuilder();
        if ( commands != null ) {
            footer.append( '\n' );
            footer.append( "Here is a list of available commands, grouped by category:" ).append( '\n' );
            footer.append( '\n' );
            for ( Map.Entry<CommandGroup, SortedMap<String, CLI>> entry : commands.entrySet() ) {
                if ( entry.getKey().equals( CommandGroup.DEPRECATED ) )
                    continue;
                Map<String, CLI> commandsInGroup = entry.getValue();
                footer.append( "---- " ).append( entry.getKey() ).append( " ----" ).append( '\n' );
                int longestCommandInGroup = commandsInGroup.keySet().stream()
                        .map( String::length )
                        .max( Integer::compareTo )
                        .orElse( 0 );
                for ( Map.Entry<String, CLI> e : commandsInGroup.entrySet() ) {
                    footer.append( e.getKey() )
                            .append( StringUtils.repeat( ' ', longestCommandInGroup - e.getKey().length() ) )
                            // FIXME: use a tabulation, but it creates newlines in IntelliJ's console
                            .append( "    " )
                            .append( StringUtils.defaultIfBlank( e.getValue().getShortDesc(), "No description provided" ) )
                            .append( '\n' );
                }
                footer.append( '\n' );
            }
        }

        footer.append( "To get help for a specific tool, use: gemma-cli <commandName> --help\n" );
        footer.append( '\n' );
        footer.append( AbstractCLI.FOOTER );

        new HelpFormatter().printHelp( writer, 150, "gemma-cli <commandName> [options]",
                AbstractCLI.HEADER, options, HelpFormatter.DEFAULT_LEFT_PAD, HelpFormatter.DEFAULT_DESC_PAD, footer.toString() );
    }

    // order here is significant.
    public enum CommandGroup {
        EXPERIMENT, PLATFORM, ANALYSIS, METADATA, PHENOTYPES, SYSTEM, MISC, DEPRECATED
    }
}
