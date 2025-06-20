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
package ubic.gemma.cli.main;

import lombok.Value;
import org.apache.commons.cli.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.BeanInstantiationException;
import org.springframework.beans.BeanUtils;
import org.springframework.context.*;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import ubic.gemma.cli.completion.*;
import ubic.gemma.cli.logging.LoggingConfigurer;
import ubic.gemma.cli.logging.log4j.Log4jConfigurer;
import ubic.gemma.cli.util.*;
import ubic.gemma.core.context.SpringContextUtils;
import ubic.gemma.core.util.BuildInfo;
import ubic.gemma.core.util.concurrent.ThreadUtils;

import javax.annotation.Nullable;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Generic command line for Gemma. Commands are referred by shorthand names; this class prints out available commands
 * when given no arguments.
 *
 * @author paul
 */
public class GemmaCLI {

    static {
        // set this as early as possible, otherwise logging might be broken
        if ( System.getProperty( "gemma.log.dir" ) == null ) {
            System.err.println( "The 'gemma.log.dir' system property is not set, will default to the current folder." );
            System.setProperty( "gemma.log.dir", "." );
        }
    }

    /**
     * Name of the Gemma CLI executable.
     */
    public static String GEMMA_CLI_EXE = "gemma-cli";

    private static final String
            HELP_OPTION = "h",
            HELP_ALL_OPTION = "ha",
            COMPLETION_OPTION = "c",
            COMPLETION_EXECUTABLE_OPTION = "ce",
            COMPLETION_SHELL_OPTION = "cs",
            COMPLETE_LOGGERS = "cl",
            COMPLETE_CONFIG = "cc",
            VERSION_OPTION = "version",
            LOGGER_OPTION = "logger",
            VERBOSITY_OPTION = "v",
            PROFILING_OPTION = "profiling",
            TESTDB_OPTION = "testdb";

    private static final LoggingConfigurer loggingConfigurer = new Log4jConfigurer();

    private static final MessageSourceResolvable EMPTY_MESSAGE = new DefaultMessageSourceResolvable( null, null, "" );

    private static ApplicationContext ctx = null;

    public static void main( String[] args ) {
        Option logOpt = Option.builder( VERBOSITY_OPTION )
                .longOpt( "verbosity" ).hasArg()
                .desc( "Set verbosity level for all loggers (0=silent, 5=very verbose; default is custom, see log4j2.xml). You can also use the following: " + String.join( ", ", LoggingConfigurer.NAMED_LEVELS ) + "." )
                .converter( EnumeratedStringConverter.of( getLogLevelsWithDescriptions() ) )
                .build();
        Option otherLogOpt = Option.builder( LOGGER_OPTION )
                .longOpt( "logger" ).hasArg()
                .desc( "Configure a specific logger verbosity (0=silent, 5=very verbose; default is custom, see log4j2.xml). You can also use the following: " + String.join( ", ", LoggingConfigurer.NAMED_LEVELS ) + ".\nFor example, '--logger ubic.gemma=5', '--logger org.hibernate.SQL=5' or '--logger org.hibernate.SQL=debug'. " )
                .converter( EnumeratedByCommandStringConverter.of( GEMMA_CLI_EXE, "-" + COMPLETE_LOGGERS ) )
                .build();
        Options options = new Options()
                .addOption( HELP_OPTION, "help", false, "Show help" )
                .addOption( HELP_ALL_OPTION, "help-all", false, "Show complete help with all available CLI commands" )
                .addOption( COMPLETION_OPTION, "completion", false, "Generate a completion script" )
                .addOption( COMPLETION_EXECUTABLE_OPTION, "completion-executable", true, "Name of the executable to generate completion for (defaults to " + GEMMA_CLI_EXE + ")" )
                .addOption( Option.builder( COMPLETION_SHELL_OPTION )
                        .longOpt( "completion-shell" )
                        .hasArg()
                        .converter( EnumeratedStringConverter.of( "bash", "fish" ) )
                        .desc( "Indicate which shell to generate completion for. Only fish and bash are supported" )
                        .build() )
                .addOption( COMPLETE_LOGGERS, "complete-loggers", false, "List all available logger that can be used for -" + LOGGER_OPTION + "." )
                .addOption( COMPLETE_CONFIG, "complete-config", false, "List all available configuration properties" )
                .addOption( VERSION_OPTION, "version", false, "Show Gemma version" )
                .addOption( otherLogOpt )
                .addOption( logOpt )
                .addOption( TESTDB_OPTION, "testdb", false, "Use the test database as described by gemma.testdb.* configuration" )
                .addOption( PROFILING_OPTION, "profiling", false, "Enable profiling" );
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
            System.out.printf( "Gemma %s%n", buildInfo );
            System.exit( 0 );
            return;
        }

        if ( commandLine.hasOption( COMPLETE_LOGGERS ) ) {
            CompletionUtils.writeCompletions( new LoggerCompletionSource( loggingConfigurer ), System.out );
            System.exit( 0 );
            return;
        }

        if ( commandLine.hasOption( COMPLETE_CONFIG ) ) {
            CompletionUtils.writeCompletions( new SettingsCompletionSource( true ), System.out );
            System.exit( 0 );
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
                    try {
                        loggingConfigurer.configureLogger( loggerName, Integer.parseInt( vals[1] ) );
                    } catch ( NumberFormatException e ) {
                        loggingConfigurer.configureLogger( loggerName, vals[1] );
                    }
                } catch ( IllegalArgumentException e ) {
                    System.err.printf( "Failed to parse the %s option for %s: %s.%n", LOGGER_OPTION, loggerName, ExceptionUtils.getRootCauseMessage( e ) );
                    GemmaCLI.printHelp( options, null, new PrintWriter( System.err, true ) );
                    System.exit( 1 );
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

        // register a shutdown hook to perform a graceful shutdown on SIGTERM or System.exit()
        Runtime.getRuntime().addShutdownHook( ThreadUtils.newThread( () -> {
            if ( ctx instanceof ConfigurableApplicationContext ) {
                ( ( ConfigurableApplicationContext ) ctx ).close();
            }
        } ) );

        Map<String, Command> commandsByName = new HashMap<>();
        Map<String, Command> commandsByClassName = new HashMap<>();
        Map<String, Command> commandsByAlias = new HashMap<>();
        SortedMap<CLI.CommandGroup, SortedMap<String, Command>> commandGroups = new TreeMap<>( Comparator.comparingInt( CLI.CommandGroup::ordinal ) );
        for ( String beanName : ctx.getBeanNamesForType( CLI.class ) ) {
            CLI cliInstance;
            Class<?> beanClass = ctx.getType( beanName );
            try {
                cliInstance = ( CLI ) BeanUtils.instantiate( beanClass );
                // provide some useful context for the CLI that they can use for generating options
                if ( cliInstance instanceof ApplicationContextAware ) {
                    ( ( ApplicationContextAware ) cliInstance ).setApplicationContext( ctx );
                }
                if ( cliInstance instanceof EnvironmentAware ) {
                    ( ( EnvironmentAware ) cliInstance ).setEnvironment( ctx.getEnvironment() );
                }
                if ( cliInstance instanceof MessageSourceAware ) {
                    ( ( MessageSourceAware ) cliInstance ).setMessageSource( ctx );
                }
            } catch ( BeanInstantiationException e2 ) {
                System.err.printf( "Failed to create %s using reflection, will have to create a complete bean to extract its metadata.%n", beanClass.getName() );
                e2.printStackTrace( System.err );
                cliInstance = ctx.getBean( beanName, CLI.class );
            }
            Command cmd = new Command( beanClass, beanName, cliInstance.getCommandName(),
                    cliInstance.getCommandAliases(), cliInstance.getShortDesc(), cliInstance.getOptions(),
                    cliInstance.allowPositionalArguments() );
            commandsByClassName.put( beanClass.getName(), cmd );
            String commandName = cliInstance.getCommandName();
            if ( StringUtils.isNotBlank( commandName ) ) {
                if ( commandsByName.put( commandName, cmd ) != null ) {
                    System.err.printf( "Non-unique command name '%s' for %s. It is also associated to %s.%n",
                            commandName, beanClass.getName(), commandsByName.get( commandName ).getBeanClass().getName() );
                    System.exit( 1 );
                }
                CLI.CommandGroup g = cliInstance.getCommandGroup();
                if ( !commandGroups.containsKey( g ) ) {
                    commandGroups.put( g, new TreeMap<>() );
                }
                commandGroups.get( g ).put( commandName, cmd );
            }
            for ( String alias : cliInstance.getCommandAliases() ) {
                if ( commandsByName.containsKey( alias ) ) {
                    System.err.printf( "Alias '%s' for %s is already used as a command name for %s.%n", alias,
                            beanClass.getName(), commandsByName.get( alias ).getBeanClass().getName() );
                    System.exit( 1 );
                }
                if ( commandsByAlias.put( alias, cmd ) != null ) {
                    System.err.printf( "Non-unique alias '%s' for %s. It is also associated to %s.%n", alias,
                            beanClass.getName(), commandsByAlias.get( alias ).getBeanClass().getName() );
                    System.exit( 1 );
                }
            }
        }

        // full help with all the commands
        if ( commandLine.hasOption( HELP_ALL_OPTION ) ) {
            GemmaCLI.printHelp( options, commandGroups, new PrintWriter( System.out, true ) );
            System.exit( 0 );
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
                    System.exit( 1 );
                    return;
                }
            }
            String executableName = commandLine.getOptionValue( COMPLETION_EXECUTABLE_OPTION, GEMMA_CLI_EXE );
            Set<String> subcommands = new HashSet<>( commandsByName.keySet() );
            subcommands.addAll( commandsByClassName.keySet() );
            if ( shellName.equals( "bash" ) ) {
                completionGenerator = new BashCompletionGenerator( executableName, subcommands );
            } else if ( shellName.equals( "fish" ) ) {
                completionGenerator = new FishCompletionGenerator( executableName, subcommands, ctx, Locale.getDefault() );
            } else {
                System.err.printf( "Completion is not support for %s.%n", shellName );
                System.exit( 1 );
                return;
            }
            PrintWriter completionWriter = new PrintWriter( System.out );
            completionGenerator.beforeCompletion( completionWriter );
            completionGenerator.generateCompletion( options, completionWriter );
            for ( SortedMap<String, Command> group : commandGroups.values() ) {
                for ( Command cli : group.values() ) {
                    List<String> aliases = new ArrayList<>( 2 + cli.getCommandAliases().size() );
                    aliases.add( cli.getBeanClass().getName() );
                    if ( StringUtils.isNotBlank( cli.getCommandName() ) ) {
                        aliases.add( cli.getCommandName() );
                    }
                    aliases.addAll( cli.getCommandAliases() );
                    for ( String alias : aliases ) {
                        completionGenerator.generateSubcommandCompletion( alias, cli.getOptions(),
                                cli.getShortDesc(), cli.isAllowsPositionalArguments(), completionWriter );
                    }
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
        } else if ( commandsByAlias.containsKey( commandRequested ) ) {
            command = commandsByAlias.get( commandRequested );
        } else {
            System.err.println( "Unrecognized command: " + commandRequested );
            GemmaCLI.printHelp( options, commandGroups, new PrintWriter( System.err, true ) );
            System.exit( 1 );
            return;
        }

        int statusCode;
        StopWatch timer = StopWatch.createStarted();
        try {
            System.err.println( "========= Gemma CLI invocation of " + command.getCommandName() + " ============" );
            System.err.println( "Options: " + GemmaCLI.getOptStringForLogging( argsToPass ) );
            CLI cli = ctx.getBean( command.getBeanName(), CLI.class );
            statusCode = cli.executeCommand( new SystemCliContext( commandRequested, argsToPass ) );
        } catch ( Exception e ) {
            System.err.println( "Gemma CLI error: " + e.getClass().getName() + " - " + e.getMessage() );
            System.err.println( ExceptionUtils.getStackTrace( e ) );
            statusCode = 1;
        } finally {
            System.err.println( "========= Gemma CLI run of " + command + " complete in " + timer.getTime( TimeUnit.SECONDS ) + " seconds ============" );
        }

        System.exit( statusCode );
    }

    private static Map<String, MessageSourceResolvable> getLogLevelsWithDescriptions() {
        Map<String, MessageSourceResolvable> result = new LinkedHashMap<>( 2 * LoggingConfigurer.NAMED_LEVELS.length );
        for ( int i = 0; i < LoggingConfigurer.NAMED_LEVELS.length; i++ ) {
            result.put( LoggingConfigurer.NAMED_LEVELS[i], EMPTY_MESSAGE );
        }
        for ( int i = 0; i < LoggingConfigurer.NAMED_LEVELS.length; i++ ) {
            result.put( LoggingConfigurer.NUMBERED_LEVELS[i], new DefaultMessageSourceResolvable( null, null, LoggingConfigurer.NAMED_LEVELS[i] ) );
        }
        return result;
    }

    /**
     * Mask password for logging
     */
    static String getOptStringForLogging( String[] argsToPass ) {
        return Arrays.stream( argsToPass )
                .map( ShellUtils::quoteIfNecessary )
                .collect( Collectors.joining( " " ) );
    }

    private static void printHelp( Options options, @Nullable SortedMap<CLI.CommandGroup, SortedMap<String, Command>> commands, PrintWriter writer ) {
        writer.println( "============ Gemma CLI tools ============" );

        StringBuilder footer = new StringBuilder();
        if ( commands != null ) {
            footer.append( "Here is a list of available commands, grouped by category:" ).append( '\n' );
            footer.append( '\n' );
            for ( Map.Entry<CLI.CommandGroup, SortedMap<String, Command>> entry : commands.entrySet() ) {
                if ( entry.getKey().equals( CLI.CommandGroup.DEPRECATED ) ) continue;
                Map<String, Command> commandsInGroup = entry.getValue();
                footer.append( "---- " ).append( entry.getKey() ).append( " ----" ).append( '\n' );
                int longestCommandInGroup = commandsInGroup.keySet().stream().map( String::length ).max( Integer::compareTo ).orElse( 0 );
                for ( Map.Entry<String, Command> e : commandsInGroup.entrySet() ) {
                    footer.append( e.getKey() ).append( StringUtils.repeat( ' ', longestCommandInGroup - e.getKey().length() ) )
                            .append( StringUtils.repeat( ' ', HelpFormatter.DEFAULT_DESC_PAD ) )
                            .append( StringUtils.defaultIfBlank( e.getValue().getShortDesc(), "No description provided" ) )
                            .append( '\n' );
                }
                footer.append( '\n' );
            }
        } else {
            footer.append( '\n' );
        }

        footer.append( "To get help for a specific tool, use: '" ).append( GEMMA_CLI_EXE ).append( " <commandName> --help'." );

        HelpUtils.printHelp( writer, GEMMA_CLI_EXE + " [options] [commandName] [commandOptions]", options, null, footer.toString() );
    }

    @Value
    private static class Command {
        Class<?> beanClass;
        String beanName;
        @Nullable
        String commandName;
        List<String> commandAliases;
        @Nullable
        String shortDesc;
        Options options;
        boolean allowsPositionalArguments;

        @Override
        public String toString() {
            return commandName != null ? commandName : beanClass.getName();
        }
    }
}
