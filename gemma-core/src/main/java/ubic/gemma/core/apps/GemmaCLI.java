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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.RegexPatternTypeFilter;
import ubic.gemma.core.util.AbstractCLI;

import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Generic command line for Gemma. Commands are referred by shorthand names; this class prints out available commands
 * when given no arguments.
 *
 * @author paul
 */
@SuppressWarnings({"unused", "WeakerAccess"}) // Possible external use
public class GemmaCLI {

    public static void main( String[] args ) {

        /*
         * Build a map from command names to classes.
         */
        Map<CommandGroup, Map<String, String>> commands = new HashMap<>();
        Map<String, Class<? extends AbstractCLI>> commandClasses = new HashMap<>();
        try {

            final ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(
                    false );
            provider.addIncludeFilter( new RegexPatternTypeFilter( Pattern.compile( ".*" ) ) );

            // searching entire hierarchy is 1) slow and 2) generates annoying logging from static initialization code.
            final Set<BeanDefinition> classes = provider.findCandidateComponents( "ubic.gemma.core.apps" );
            classes.addAll( provider.findCandidateComponents( "ubic.gemma.core.loader.association.phenotype" ) );

            for ( BeanDefinition bean : classes ) {
                try {
                    @SuppressWarnings("unchecked")
                    Class<? extends AbstractCLI> aClazz = ( Class<? extends AbstractCLI> ) Class
                            .forName( bean.getBeanClassName() );

                    Object cliInstance = aClazz.newInstance();

                    Method method = aClazz.getMethod( "getCommandName" );
                    String commandName = ( String ) method.invoke( cliInstance, new Object[]{} );
                    if ( commandName == null || StringUtils.isBlank( commandName ) ) {
                        // keep null to avoid printing some commands...
                        continue;
                    }

                    Method method2 = aClazz.getMethod( "getShortDesc" );
                    String desc = ( String ) method2.invoke( cliInstance, new Object[]{} );

                    Method method3 = aClazz.getMethod( "getCommandGroup" );
                    CommandGroup g = ( CommandGroup ) method3.invoke( cliInstance, new Object[]{} );

                    if ( !commands.containsKey( g ) ) {
                        commands.put( g, new TreeMap<String, String>() );
                    }

                    commands.get( g ).put( commandName, desc + " (" + bean.getBeanClassName() + ")" );

                    commandClasses.put( commandName, aClazz );
                } catch ( Exception e ) {
                    // OK, this can happen if we hit a non useful class.
                }
            }
        } catch ( Exception e1 ) {
            System.err.println( "ERROR! Report to developers: " + e1.getMessage() );
            System.exit( 1 );
        }

        if ( args.length == 0 || args[0].equalsIgnoreCase( "--help" ) || args[0].equalsIgnoreCase( "-help" ) || args[0]
                .equalsIgnoreCase( "help" ) ) {
            GemmaCLI.printHelp( commands );
            System.exit( 1 );
        } else {
            LinkedList<String> f = new LinkedList<>( Arrays.asList( args ) );
            String commandRequested = f.remove( 0 );
            String[] argsToPass = f.toArray( new String[]{} );

            if ( !commandClasses.containsKey( commandRequested ) ) {
                System.err.println( "Unrecognized command: " + commandRequested );
                GemmaCLI.printHelp( commands );
                System.err.println( "Unrecognized command: " + commandRequested );
                System.exit( 1 );
            } else {
                try {
                    Class<? extends AbstractCLI> c = commandClasses.get( commandRequested );
                    System.err.println( "========= Gemma CLI invocation of " + commandRequested + " ============" );
                    System.err.println( "Options: " + GemmaCLI.getOptStringForLogging( argsToPass ) );
                    //noinspection JavaReflectionInvocation // It works
                    System.exit( c.getDeclaredConstructor().newInstance().executeCommand( argsToPass ) );
                } catch ( Exception e ) {
                    System.err.println( "Gemma CLI error: " + e.getClass().getName() + " - " + e.getMessage() );
                    System.err.println( ExceptionUtils.getStackTrace( e ) );
                    throw new RuntimeException( e );
                } finally {
                    System.err.println( "========= Gemma CLI run of " + commandRequested + " complete ============" );
                }
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

        System.err.print( "To operate Gemma tools, run a command like:\n\n"
                + "gemma-cli <commandName> [options]\n\n"
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
        System.err.println( "\nTo get help for a specific tool, use \n\ngemma-cli <commandName> --help" );
        System.err.print( "\n" + AbstractCLI.FOOTER + "\n=========================================\n" );
        System.exit( 0 );
    }

    // order here is significant.
    public enum CommandGroup {
        EXPERIMENT, PLATFORM, ANALYSIS, METADATA, PHENOTYPES, SYSTEM, MISC, DEPRECATED
    }
}
