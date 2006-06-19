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
            parser.parse( is, false );
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
     * @param args
     */
    public static void main( String[] args ) {// TODO refactor to use commons configuration

        double[][] data = null;
        boolean test = false;
        if ( args[0].equalsIgnoreCase( "t" ) || args[0].equalsIgnoreCase( "true" ) ) test = true;

        String filename = args[1];
        char distance = args[2].charAt( 0 );
        char method = args[3].charAt( 0 );

        if ( test )
            data = testData();
        else
            data = readTabFile( filename );

        NCluster cluster = new NCluster();

        StopWatch sw = new StopWatch();
        sw.start();
        cluster.treeCluster( data.length, data[0].length, 0, distance, method, data );
        sw.stop();
        log.warn( sw.getTime() );
    }

}
