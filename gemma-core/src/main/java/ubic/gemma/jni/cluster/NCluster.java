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

import ubic.gemma.loader.util.parser.BasicLineParser;
import ubic.gemma.loader.util.parser.TabDelimParser;

/**
 * @author keshav
 * @version $Id$
 */
public class NCluster {
    private static Log log = LogFactory.getLog( NCluster.class );
    
    /**
     * dist       (input) char
     * Defines which distance measure is used, as given by the table:
     * dist=='e': Euclidean distance
     * dist=='b': City-block distance
     * dist=='c': correlation
     * dist=='a': absolute value of the correlation
     * dist=='u': uncentered correlation
     * dist=='x': absolute uncentered correlation
     * dist=='s': Spearman's rank correlation
     * dist=='k': Kendall's tau
     * For other values of dist, the default (Euclidean distance) is used.
     * 
     * method     (input) char
     * Defines which hierarchical clustering method is used:
     * method=='s': pairwise single-linkage clustering
     * method=='m': pairwise maximum- (or complete-) linkage clustering
     * method=='a': pairwise average-linkage clustering
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
    public native int[][] computeCompleteLinkage( int rows, int cols, int transpose, char dist, char method,
            double matrix[][] );

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
    public static double[][] readTabFile( String filename ) {
        BasicLineParser parser = new TabDelimParser();
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
            values[i] = new double[2];
            values[i][0] = Double.parseDouble( array[2] );
            values[i][1] = Double.parseDouble( array[4] );
            i++;
        }

        return values;
    }

    /**
     * @param args
     */
    public static void main( String[] args ) {
        // FIXME Paul, I will move this.

        String filename = baseDir + ( String ) config.getProperty( "aTestDataSet_no_headers" );

        double[][] data = readTabFile( filename );// just a pointer ... must copy it

        NCluster cluster = new NCluster();

        /* uncomment me to use this example */
        // double[][] dataCopy = new double[7][2];
        //        
        // double[] t0 = { 1, 0 };
        // double[] t1 = { 4, 0 };
        // double[] t2 = { 5, 0 };
        // double[] t3 = { 9, 0 };
        // double[] t4 = { 10, 0 };
        // double[] t5 = { 10, 0 };
        // double[] t6 = { 3, 0 };
        //        
        // dataCopy[0] = t0;
        // dataCopy[1] = t1;
        // dataCopy[2] = t2;
        // dataCopy[3] = t3;
        // dataCopy[4] = t4;
        // dataCopy[5] = t5;
        // dataCopy[6] = t6;
        StopWatch sw = new StopWatch();
        sw.start();
        cluster.computeCompleteLinkage( data.length, data[0].length, 0, 'e', 'm', data );
        sw.stop();
        log.warn( sw.getTime() ); // 7 seconds
    }

}
