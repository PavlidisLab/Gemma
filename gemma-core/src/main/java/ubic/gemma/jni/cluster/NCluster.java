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
package ubic.gemma.jni.cluster;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.loader.util.parser.TabDelimParser;

/**
 * @author keshav
 * @version $Id$
 */
public class NCluster {
    private static Log log = LogFactory.getLog( NCluster.class );

    private static final String USAGE = "[-h] [-t <true|false>] [-f <filename>] [-d <e|c|s>] [-m <s|m|a|c>] ";
    private static final String HEADER = "The Gemma project, Copyright (c) 2006 University of British Columbia";
    private static final String FOOTER = "For more information, see our website at http://www.neurogemma.org";

    /**
     * dist (input) char Defines which distance measure is used, as given by the table: dist=='e': Euclidean distance
     * dist=='b': City-block distance dist=='c': correlation dist=='a': absolute value of the correlation dist=='u':
     * uncentered correlation dist=='x': absolute uncentered correlation dist=='s': Spearman's rank correlation
     * dist=='k': Kendall's tau For other values of dist, the default (Euclidean distance) is used. method (input) char
     * Defines which hierarchical clustering method is used: method=='s': pairwise single-linkage clustering
     * method=='m': pairwise maximum- (or complete-) linkage clustering method=='a': pairwise average-linkage clustering
     * method=='c': pairwise centroid-linkage clustering
     * 
     * @param rows
     * @param cols
     * @param transpose
     * @param dist
     * @param method
     * @param matrix
     * @return int[][]
     */
    public native int[][] treeCluster( int rows, int cols, int transpose, char dist, char method, double matrix[][] );

    static Configuration config = null;
    static String baseDir = null;
    static {
        try {
            config = new PropertiesConfiguration( "Gemma.properties" );
        } catch ( ConfigurationException e ) {
            System.err.println( "Could not read properites file " + config );
            e.printStackTrace();
        }

        baseDir = ( String ) config.getProperty( "gemma.baseDir" );
        String localBasePath = ( String ) config.getProperty( "cluster.dll.path" );
        System.load( baseDir + localBasePath );
    }

    /**
     * @param filename
     * @return double [][]
     */
    private static double[][] readTabFile( String filename ) {
        TabDelimParser parser = new TabDelimParser();
        InputStream is;
        Collection results = new HashSet();
        try {
            is = new FileInputStream( new File( filename ) );
            parser.parse( is );
            results = parser.getResults();
        } catch ( Exception e ) {
            e.printStackTrace();
        }

        int i = 0;
        double[][] values = new double[results.size()][];
        for ( Object result : results ) {
            String[] array = ( String[] ) result;
            values[i] = new double[array.length];
            for ( int j = 0; j < values[i].length; j++ ) {
                values[i][j] = Double.parseDouble( array[j] );
            }
            i++;
        }

        return values;
    }

    /**
     * @return
     */
    private static double[][] testData() {
        // filename = baseDir + ( String ) config.getProperty( "aTestDataSet_no_headers" );
        double values[][] = new double[7][2];
        /* a simple test example */
        double[] d0 = { 1, 0 };
        double[] d1 = { 4, 0 };
        double[] d2 = { 5, 0 };
        double[] d3 = { 9, 0 };
        double[] d4 = { 10, 0 };
        double[] d5 = { 10, 0 };
        double[] d6 = { 3, 0 };

        values[0] = d0;
        values[1] = d1;
        values[2] = d2;
        values[3] = d3;
        values[4] = d4;
        values[5] = d5;
        values[6] = d6;

        return values;
    }

    /**
     * @param opt
     */
    private static void printHelp( Options opt ) {
        HelpFormatter h = new HelpFormatter();
        // h.setWidth( 80 );
        h.printHelp( USAGE, HEADER, opt, FOOTER );
    }

    /**
     * @param args
     */
    public static void main( String[] args ) {

        String filename = null;
        String distance = "e";
        String method = "m";

        double[][] data = null;

        NCluster cluster = new NCluster();

        try {
            /* OPTIONS STAGE */

            /* help */
            OptionBuilder.withDescription( "Print help for this application" );
            Option helpOpt = OptionBuilder.create( 'h' );

            /* environment (test or prod) */
            OptionBuilder.hasArgs();
            OptionBuilder.withDescription( "Run test example" );
            Option testOpt = OptionBuilder.create( 't' );

            /* parse */
            OptionBuilder.hasArg();
            OptionBuilder.withDescription( "Filename" );
            Option fileOpt = OptionBuilder.create( 'f' );

            /* distance */
            OptionBuilder.withDescription( "Distance" );
            Option distanceOpt = OptionBuilder.create( 'd' );

            /* method */
            OptionBuilder.withDescription( "Method" );
            Option methodOpt = OptionBuilder.create( 'm' );

            Options opt = new Options();
            opt.addOption( helpOpt );
            opt.addOption( testOpt );
            opt.addOption( fileOpt );
            opt.addOption( distanceOpt );
            opt.addOption( methodOpt );

            /* COMMAND LINE PARSER STAGE */
            BasicParser parser = new BasicParser();
            CommandLine cl = parser.parse( opt, args );

            /* INTERROGATION STAGE */
            if ( cl.getOptions().length == 0 || cl.hasOption( 'h' ) ) {
                printHelp( opt );
                System.exit( 0 );
            }

            /* check if using test data */
            if ( cl.hasOption( 't' ) ) {
                boolean test = Boolean.parseBoolean( cl.getOptionValue( 't' ) );
                if ( test )
                    data = testData();
                else {
                    if ( cl.hasOption( 'f' ) ) {
                        filename = cl.getOptionValue( 'f' );
                        data = readTabFile( filename );
                    } else {
                        data = testData();
                        System.out.println( "File not specified ... using test data" );
                    }
                }
            }

            if ( cl.hasOption( 'd' ) ) {
                distance = cl.getOptionValue( 'd' );// TODO validate, fix issue with this option
                log.warn( distance );
            } else {
                System.out.println( "Distance measure not specified ... using euclidean" );
            }

            if ( cl.hasOption( 'm' ) ) {
                method = cl.getOptionValue( 'm' );// TODO validate, fix issue with this option
                log.warn( method );
            } else {
                System.out.println( "Linkage not specified ... using complete (maximum) linkage" );
            }

        } catch ( Exception e ) {
            e.printStackTrace();
        }

        StopWatch sw = new StopWatch();
        sw.start();
        cluster.treeCluster( data.length, data[0].length, 0, distance.charAt( 0 ), method.charAt( 0 ), data );
        sw.stop();
        log.warn( sw.getTime() );
    }

}
