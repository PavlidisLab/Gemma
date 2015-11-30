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
package ubic.gemma.apps;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.RegexPatternTypeFilter;

import ubic.gemma.util.AbstractCLI;

/**
 * Generic command line for Gemma. Commands are referred by shorthand names; this class prints out available commands
 * when given no arguments.
 * 
 * @author paul
 * @version $Id$
 */
public class GemmaCLI {

    // order here is significant.
    public static enum CommandGroup {
        EXPERIMENT, PLATFORM, ANALYSIS, METADATA, PHENOTYPES, SYSTEM, MISC, DEPRECATED
    };

    /**
     * @param args
     */
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
            final Set<BeanDefinition> classes = provider.findCandidateComponents( "ubic.gemma.apps" );
            classes.addAll( provider.findCandidateComponents( "ubic.gemma.loader.association.phenotype" ) );
            classes.addAll( provider.findCandidateComponents( "chibi.gemmaanalysis" ) );

            for ( BeanDefinition bean : classes ) {
                try {
                    Class<? extends AbstractCLI> aclazz = ( Class<? extends AbstractCLI> ) Class.forName( bean
                            .getBeanClassName() );

                    Object cliinstance = aclazz.newInstance();

                    Method method = aclazz.getMethod( "getCommandName", new Class[] {} );
                    String commandName = ( String ) method.invoke( cliinstance, new Object[] {} );
                    if ( commandName == null ) {
                        // keep null to avoid printing some commands...
                        continue;
                    }

                    Method method2 = aclazz.getMethod( "getShortDesc", new Class[] {} );
                    String desc = ( String ) method2.invoke( cliinstance, new Object[] {} );

                    Method method3 = aclazz.getMethod( "getCommandGroup", new Class[] {} );
                    CommandGroup g = ( CommandGroup ) method3.invoke( cliinstance, new Object[] {} );

                    if ( !commands.containsKey( g ) ) {
                        commands.put( g, new TreeMap<String, String>() );
                    }

                    commands.get( g ).put( commandName, desc + " (" + bean.getBeanClassName() + ")" );

                    commandClasses.put( commandName, aclazz );
                } catch ( Exception e ) {
                    // OK, this can happen if we hit a non useful class.
                }
            }
        } catch ( Exception e1 ) {
            System.err.println( "ERROR! Report to developers: " + e1.getMessage() );
            System.exit( 1 );
        }

        if ( args.length == 0 || args[0].equalsIgnoreCase( "--help" ) || args[0].equalsIgnoreCase( "-help" )
                || args[0].equalsIgnoreCase( "help" ) ) {
            printHelp( commands );
        } else {
            LinkedList<String> f = new LinkedList<String>( Arrays.asList( args ) );
            String commandRequested = f.remove( 0 );
            Object[] argsToPass = f.toArray( new String[] {} );

            if ( !commandClasses.containsKey( commandRequested ) ) {
                System.err.println( "Unrecognized command: " + commandRequested );
                printHelp( commands );
                System.err.println( "Unrecognized command: " + commandRequested );
                System.exit( 1 );
            } else {
                try {
                    Class<?> c = commandClasses.get( commandRequested );
                    Method method = c.getMethod( "main", String[].class );
                    System.err.println( "========= Gemma CLI invocation of " + commandRequested + " ============" );
                    method.invoke( null, ( Object ) argsToPass );
                } catch ( Exception e ) {
                    System.err.println( "Gemma CLI error: " + e.getClass().getName() + " - " + e.getMessage() );
                    throw new RuntimeException( e );
                } finally {
                    System.err.println( "========= Gemma CLI run of " + commandRequested + " complete ============" );
                    System.exit( 0 );
                }
            }
        }
    }

    /**
     * @param commands
     */
    public static void printHelp( Map<CommandGroup, Map<String, String>> commands ) {
        System.err.println( "============ Gemma command line tools ============" );

        System.err.print( "To operate Gemma tools, run a command like:\n\njava [jre options] -classpath ${GEMMA_LIB} "
                + "ubic.gemma.apps.GemmaCLI <commandName> [options]\n\n"
                + "You can use gemmaCli.sh as a shortcut as in 'gemmaCli.sh <commandName> [options]'.\n\n"
                + "Here is a list of available commands, grouped by category:\n" );

        for ( CommandGroup cmdg : CommandGroup.values() ) {
            if ( cmdg.equals( CommandGroup.DEPRECATED ) ) continue;
            if ( !commands.containsKey( cmdg ) ) continue;
            Map<String, String> commandsInGroup = commands.get( cmdg );
            if ( commandsInGroup.isEmpty() ) continue;

            System.err.println( "\n---- " + cmdg.toString() + " ----" );
            for ( String cmd : commandsInGroup.keySet() ) {
                if ( cmd == null ) continue; // just in case... but no command name means "skip";
                System.err.println( cmd + " - " + commandsInGroup.get( cmd ) );
            }
        }
        System.err.println( "\nTo get help for a specific tool, use \n\ngemmaCli.sh <commandName> --help" );
        System.err.print( "\n" + AbstractCLI.FOOTER + "\n=========================================\n" );
    }
}
