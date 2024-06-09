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

import lombok.Value;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.cli.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import ubic.gemma.core.completion.BashCompletionGenerator;
import ubic.gemma.core.completion.CompletionGenerator;
import ubic.gemma.core.completion.FishCompletionGenerator;
import ubic.gemma.core.context.SpringContextUtils;
import ubic.gemma.core.logging.LoggingConfigurer;
import ubic.gemma.core.logging.log4j.Log4jConfigurer;
import ubic.gemma.core.util.BuildInfo;
import ubic.gemma.core.util.CLI;
import ubic.gemma.core.util.HelpUtils;

import javax.annotation.Nullable;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;
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

    private static final String HELP_OPTION = "h", HELP_ALL_OPTION = "ha", COMPLETION_OPTION = "c", COMPLETION_EXECUTABLE_OPTION = "ce", COMPLETION_SHELL_OPTION = "cs", VERSION_OPTION = "version", LOGGER_OPTION = "logger", VERBOSITY_OPTION = "v", PROFILING_OPTION = "profiling", TESTDB_OPTION = "testdb";

    /**
     * Pattern used to match password in the CLI arguments.
     * <p>
     * Passwords are no longer allowed as of 1.29.0, but some users might still supply their passwords.
     */
    private static final Pattern PASSWORD_IN_CLI_MATCHER = Pattern.compile( "(-{1,2}p(?:assword)?)\\s+(.+?)\\b" );

    private static final LoggingConfigurer loggingConfigurer = new Log4jConfigurer();

    private static ApplicationContext ctx = null;

    public static void main( String[] args ) {
        Option logOpt = Option.builder( VERBOSITY_OPTION ).longOpt( "verbosity" ).hasArg().desc( "Set verbosity level for all loggers (0=silent, 5=very verbose; default is custom, see log4j.properties). You can also use the following: " + String.join( ", ", LoggingConfigurer.NAMED_LEVELS ) + "." ).build();
        Option otherLogOpt = Option.builder( LOGGER_OPTION ).longOpt( "logger" ).hasArg().desc( "Configure a specific logger verbosity (0=silent, 5=very verbose; default is custom, see log4j.properties). You can also use the following: " + String.join( ", ", LoggingConfigurer.NAMED_LEVELS ) + ".\nFor example, '--logger ubic.gemma=5', '--logger org.hibernate.SQL=5' or '--logger org.hibernate.SQL=debug'. " ).build();
        Options options = new Options().addOption( HELP_OPTION, "help", false, "Show help" ).addOption( HELP_ALL_OPTION, "help-all", false, "Show complete help with all available CLI commands" ).addOption( COMPLETION_OPTION, "completion", false, "Generate a completion script" ).addOption( COMPLETION_EXECUTABLE_OPTION, "completion-executable", true, "Name of the executable to generate completion for (defaults to gemma-cli)" ).addOption( COMPLETION_SHELL_OPTION, "completion-shell", true, "Indicate which shell to generate completion for. Only fish and bash are supported" ).addOption( VERSION_OPTION, "version", false, "Show Gemma version" ).addOption( otherLogOpt ).addOption( logOpt ).addOption( TESTDB_OPTION, "testdb", false, "Use the test database as described by gemma.testdb.* configuration" ).addOption( PROFILING_OPTION, "profiling", false, "Enable profiling" );
        CommandLine commandLine;
        try {
            commandLine = new DefaultParser().parse( options, args, true );
        } catch ( ParseException e ) {
            exit( 1 );
            return; // that's silly...
        }

        // quick help without loading the context
        if ( commandLine.hasOption( HELP_OPTION ) ) {
            GemmaCLI.printHelp( options, null, new PrintWriter( System.out, true ) );
            exit( 0 );
            return;
        }

        if ( commandLine.hasOption( VERSION_OPTION ) ) {
            BuildInfo buildInfo = BuildInfo.fromClasspath();
            System.out.printf( "Gemma %s%n", buildInfo );
            exit( 0 );
            return;
        }

        if ( commandLine.hasOption( VERBOSITY_OPTION ) ) {
            try {
                try {
                    loggingConfigurer.configureAllLoggers( Integer.parseInt( commandLine.getOptionValue( VERBOSITY_OPTION ) ) );
                } catch ( NumberFormatException e ) {
                    loggingConfigurer.configureAllLoggers( commandLine.getOptionValue( VERBOSITY_OPTION ) );
                }
            } catch ( IllegalArgumentException e ) {
                System.err.printf( "Failed to parse the %s option: %s.%n", VERBOSITY_OPTION, ExceptionUtils.getRootCauseMessage( e ) );
                GemmaCLI.printHelp( options, null, new PrintWriter( System.err, true ) );
                exit( 1 );
                return;
            }
        }

        if ( commandLine.hasOption( LOGGER_OPTION ) ) {
            for ( String value : commandLine.getOptionValues( LOGGER_OPTION ) ) {
                String[] vals = value.split( "=" );
                if ( vals.length != 2 ) {
                    System.err.println( "Logging value must in format [loggerName]=[value]." );
                    exit( 1 );
                }
                String loggerName = vals[0];
                try {
                    try {
                        loggingConfigurer.configureLogger( loggerName, Integer.parseInt( vals[1] ) );
                    } catch ( NumberFormatException e ) {
                        loggingConfigurer.configureLogger( loggerName, vals[1] );
                    }
                } catch ( IllegalArgumentException e ) {
                    System.err.printf( "Failed to parse the %s option for %s: %s.%n", LOGGER_OPTION, loggerName, ExceptionUtils.getRootCauseMessage( e ) );
                    GemmaCLI.printHelp( options, null, new PrintWriter( System.err, true ) );
                    exit( 1 );
                    return;
                }
            }
        }

        loggingConfigurer.apply();

        List<String> profiles = new ArrayList<>();
        profiles.add( "cli" );

        // enable the test database
        if ( commandLine.hasOption( TESTDB_OPTION ) ) {
            profiles.add( "testdb" );
        }

        if ( commandLine.hasOption( PROFILING_OPTION ) ) {
            profiles.add( "profiling" );
        }

        ctx = SpringContextUtils.getApplicationContext( profiles.toArray( new String[0] ) );

        Map<String, Command> commandsByClassName = new HashMap<>();
        Map<String, Command> commandsByName = new HashMap<>();
        SortedMap<CommandGroup, SortedMap<String, Command>> commandGroups = new TreeMap<>( Comparator.comparingInt( CommandGroup::ordinal ) );
        for ( String beanName : ctx.getBeanNamesForType( CLI.class ) ) {
            CLI cliInstance;
            Class<?> beanClass = ctx.getType( beanName );
            try {
                cliInstance = ( CLI ) beanClass.newInstance();
            } catch ( InstantiationException | IllegalAccessException e2 ) {
                log.warn( String.format( "Failed to create %s using reflection, will have to create a complete bean to extract its metadata.", beanClass.getName() ), e2 );
                cliInstance = ctx.getBean( beanName, CLI.class );
            }
            Command cmd = new Command( beanClass, beanName, cliInstance.getCommandName(), cliInstance.getShortDesc(), cliInstance.getOptions(), cliInstance.allowPositionalArguments() );
            commandsByClassName.put( beanClass.getName(), cmd );
            String commandName = cliInstance.getCommandName();
            if ( commandName == null || StringUtils.isBlank( commandName ) ) {
                // keep null to avoid printing some commands...
                continue;
            }
            commandsByName.put( commandName, cmd );
            CommandGroup g = cliInstance.getCommandGroup();
            if ( !commandGroups.containsKey( g ) ) {
                commandGroups.put( g, new TreeMap<>() );
            }
            commandGroups.get( g ).put( commandName, cmd );
        }

        // full help with all the commands
        if ( commandLine.hasOption( HELP_ALL_OPTION ) ) {
            GemmaCLI.printHelp( options, commandGroups, new PrintWriter( System.out, true ) );
            exit( 0 );
            return;
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
                    exit( 1 );
                    return;
                }
            }
            String executableName = commandLine.getOptionValue( COMPLETION_EXECUTABLE_OPTION, "gemma-cli" );
            Set<String> subcommands = new HashSet<>( commandsByName.keySet() );
            subcommands.addAll( commandsByClassName.keySet() );
            if ( shellName.equals( "bash" ) ) {
                completionGenerator = new BashCompletionGenerator( executableName, subcommands );
            } else if ( shellName.equals( "fish" ) ) {
                completionGenerator = new FishCompletionGenerator( executableName, subcommands );
            } else {
                System.err.printf( "Completion is not support for %s.%n", shellName );
                exit( 1 );
                return;
            }
            PrintWriter completionWriter = new PrintWriter( System.out );
            completionGenerator.beforeCompletion( completionWriter );
            completionGenerator.generateCompletion( options, completionWriter );
            for ( SortedMap<String, Command> group : commandGroups.values() ) {
                for ( Command cli : group.values() ) {
                    completionGenerator.generateSubcommandCompletion( cli.getBeanClass().getName(), cli.getOptions(), cli.getShortDesc(), cli.isAllowsPositionalArguments(), completionWriter );
                    if ( cli.getCommandName() != null ) {
                        completionGenerator.generateSubcommandCompletion( cli.getCommandName(), cli.getOptions(), cli.getShortDesc(), cli.isAllowsPositionalArguments(), completionWriter );
                    }
                }
            }
            completionGenerator.afterCompletion( completionWriter );
            completionWriter.flush();
            exit( 0 );
            return;
        }

        // no command is passed
        if ( commandLine.getArgList().isEmpty() ) {
            System.err.println( "No command was supplied." );
            GemmaCLI.printHelp( options, commandGroups, new PrintWriter( System.err, true ) );
            exit( 1 );
            return;
        }

        // the first element of the remaining args is the command and the rest are the arguments
        LinkedList<String> commandArgs = new LinkedList<>( commandLine.getArgList() );
        String commandRequested = commandArgs.remove( 0 );
        String[] argsToPass = commandArgs.toArray( new String[] {} );

        Command command;
        if ( commandsByClassName.containsKey( commandRequested ) ) {
            command = commandsByClassName.get( commandRequested );
        } else if ( commandsByName.containsKey( commandRequested ) ) {
            command = commandsByName.get( commandRequested );
        } else {
            System.err.println( "Unrecognized command: " + commandRequested );
            GemmaCLI.printHelp( options, commandGroups, new PrintWriter( System.err, true ) );
            exit( 1 );
            return;
        }

        int statusCode;
        StopWatch timer = StopWatch.createStarted();
        try {
            System.err.println( "========= Gemma CLI invocation of " + command.getCommandName() + " ============" );
            System.err.println( "Options: " + GemmaCLI.getOptStringForLogging( argsToPass ) );
            CLI cli = ctx.getBean( command.getBeanName(), CLI.class );
            statusCode = cli.executeCommand( argsToPass );
        } catch ( Exception e ) {
            System.err.println( "Gemma CLI error: " + e.getClass().getName() + " - " + e.getMessage() );
            System.err.println( ExceptionUtils.getStackTrace( e ) );
            statusCode = 1;
        } finally {
            System.err.println( "========= Gemma CLI run of " + command + " complete in " + timer.getTime( TimeUnit.SECONDS ) + " seconds ============" );
        }

        exit( statusCode );
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

    private static void printHelp( Options options, @Nullable SortedMap<CommandGroup, SortedMap<String, Command>> commands, PrintWriter writer ) {
        writer.println( "============ Gemma CLI tools ============" );

        StringBuilder footer = new StringBuilder();
        if ( commands != null ) {
            footer.append( '\n' );
            footer.append( "Here is a list of available commands, grouped by category:" ).append( '\n' );
            footer.append( '\n' );
            for ( Map.Entry<CommandGroup, SortedMap<String, Command>> entry : commands.entrySet() ) {
                if ( entry.getKey().equals( CommandGroup.DEPRECATED ) ) continue;
                Map<String, Command> commandsInGroup = entry.getValue();
                footer.append( "---- " ).append( entry.getKey() ).append( " ----" ).append( '\n' );
                int longestCommandInGroup = commandsInGroup.keySet().stream().map( String::length ).max( Integer::compareTo ).orElse( 0 );
                for ( Map.Entry<String, Command> e : commandsInGroup.entrySet() ) {
                    footer.append( e.getKey() ).append( StringUtils.repeat( ' ', longestCommandInGroup - e.getKey().length() ) )
                            // FIXME: use a tabulation, but it creates newlines in IntelliJ's console
                            .append( "    " ).append( StringUtils.defaultIfBlank( e.getValue().getShortDesc(), "No description provided" ) ).append( '\n' );
                }
                footer.append( '\n' );
            }
        } else {
            footer.append( '\n' );
        }

        footer.append( "To get help for a specific tool, use: 'gemma-cli <commandName> --help'." );

        HelpUtils.printHelp( writer, "<commandName>", options, false, null, footer.toString() );
    }

    /**
     * Exit the application with the given status code.
     */
    private static void exit( int statusCode ) {
        if ( ctx instanceof ConfigurableApplicationContext ) {
            ( ( ConfigurableApplicationContext ) ctx ).close();
        }
        System.exit( statusCode );
    }

    // order here is significant.
    public enum CommandGroup {
        EXPERIMENT, PLATFORM, ANALYSIS, METADATA, SYSTEM, MISC, DEPRECATED
    }

    @Value
    private static class Command {
        Class<?> beanClass;
        String beanName;
        @Nullable
        String commandName;
        String shortDesc;
        Options options;
        boolean allowsPositionalArguments;

        @Override
        public String toString() {
            return commandName != null ? commandName : beanClass.getName();
        }
    }
}
