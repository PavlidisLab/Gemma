/*
 * The Gemma project
 * 
 * Copyright (c) 2005 Columbia University
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
package edu.columbia.gemma.loader.expression.mage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.columbia.gemma.loader.loaderutils.Parser;

/**
 * Parse the raw files from array express.
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2005 Columbia University
 * 
 * @author keshav
 * @version $Id$
 */
public class RawDataParser implements Parser {
    private static Log log = LogFactory.getLog( RawDataParser.class.getName() );

    protected static final int ALERT_FREQUENCY = 1000; // TODO put in interface since this is a constant

    int numOfBioAssays = 0;
    int numOfDesignElements = 0;

    protected static int X = 0;
    protected static int Y = 0;
    protected static int INTENSITY = 0;
    protected static int STDEV = 0;
    protected static int PIXELS = 0;
    protected static int OUTLIER = 0;
    protected static int MASKED = 0;

    private List intensityList = null;
    private List stdevList = null;
    private List pixelList = null;
    private List outlierList = null;
    private List maskedList = null;

    // private List arrayListX = null;
    // private List arrayListY = null;

    /*
     * Quantitation types as data structures. Using arrayList because it is ordered and more efficient than LinkedList
     * implementation.
     */
    public RawDataParser() {

        /* read indicies to parse from configuration file. */
        Configuration conf = null;
        try {
            conf = new PropertiesConfiguration( "Gemma.properties" );
        } catch ( ConfigurationException e ) {
            throw new RuntimeException( e );
        }

        X = conf.getInt( "x" );

        Y = conf.getInt( "y" );

        INTENSITY = conf.getInt( "intensity" );

        STDEV = conf.getInt( "stdev" );

        PIXELS = conf.getInt( "pixels" );

        OUTLIER = conf.getInt( "outlier" );

        MASKED = conf.getInt( "masked" );

        intensityList = new ArrayList();
        stdevList = new ArrayList();
        pixelList = new ArrayList();
        outlierList = new ArrayList();
        maskedList = new ArrayList();
        // xList = new ArrayList();
        // yList = new ArrayList();

    }

    /**
     * @param file
     * @throws IOException
     */
    public void parse( File f ) throws IOException {
        InputStream is = new FileInputStream( f );
        parse( is );
    }

    @SuppressWarnings("unused")
    public void parse( String filename ) {
        // TODO implement and remove the SuppressedWarnings("unused") from this method. I have added it for now
        // so you will not see any warnings.

    }

    /**
     * @param is
     * @throws IOException FIXME purposely reading only 1000 lines from the raw data files for test purposes ... remove
     *         this
     */
    @SuppressWarnings("unchecked")
    public void parse( InputStream is ) throws IOException {

        if ( is == null ) throw new IllegalArgumentException( "InputStream was null" );
        BufferedReader br = new BufferedReader( new InputStreamReader( is ) );

        String line = null;
        int count = 0;
        /*
         * getNumOfDesignElements() will return a value that will represent the number of columns in the resulting
         * matrix.
         */
        double[] intensities = new double[getNumOfDesignElements()];
        double[] stdevs = new double[getNumOfDesignElements()];
        int[] pixels = new int[getNumOfDesignElements()];
        boolean[] outliers = new boolean[getNumOfDesignElements()];
        boolean[] masked = new boolean[getNumOfDesignElements()];

        while ( ( ( line = br.readLine() ) != null ) && count < 1000 ) {
            String[] values = StringUtils.split( line, " " );

            /*
             * quantitationType[count] (ie. intensities[count] forms one column in resulting matrix. We will end up with
             * 12 columns and 1000 rows.
             */

            intensities[count] = Double.parseDouble( values[INTENSITY] );
            stdevs[count] = Double.parseDouble( values[STDEV] );
            pixels[count] = Integer.parseInt( values[PIXELS] );
            outliers[count] = Boolean.parseBoolean( values[MASKED] );
            masked[count] = Boolean.parseBoolean( values[MASKED] );

            // arrayListX.add( Integer.parseInt( values[X] ) );
            // arrayListY.add( Integer.parseInt( values[Y] ) );

            count++;

            logDetails( count );
        }

        /* Adding arrays to respective lists. Each array represents a row in the corresponding matrix */
        intensityList.add( intensities );
        stdevList.add( stdevs );
        pixelList.add( pixels );
        outlierList.add( outliers );
        maskedList.add( masked );

        br.close();

    }

    @SuppressWarnings("unchecked")
    public Collection<Object> getResults() {
        Collection<Object> col = new HashSet();
        col.add( intensityList );
        col.add( stdevList );
        col.add( pixelList );
        col.add( outlierList );
        col.add( maskedList );
        return col;
    }

    /**
     * Each element in list is an array, which represents the columns of the matrix.
     * 
     * @param list
     * @param cols
     * @param rows
     * @return double[][]
     */
    double[][] convertListOfDoubleArraysToMatrix( List list, int cols, int rows ) {
        // TODO the cols will soon be defined by the designElement dimension
        cols = ( ( double[] ) list.get( 0 ) ).length;

        double[][] matrix = new double[rows][cols];
        for ( int i = 0; i < list.size(); i++ ) {
            log.debug( "list " + i + " (an array) size: " + ( ( double[] ) list.get( i ) ).length );
            matrix[i] = ( ( double[] ) list.get( i ) );
            log.debug( "matrix[" + i + "] size: " + matrix[i].length );
        }

        return matrix;
    }

    /**
     * Each element in list is an array, which represents the columns of the matrix.
     * 
     * @param list
     * @param cols
     * @param rows
     * @return int[][]
     */
    int[][] convertListOfIntArraysToMatrix( List list, int cols, int rows ) {
        // TODO the cols will soon be defined by the designElement dimension
        cols = ( ( int[] ) list.get( 0 ) ).length;

        int[][] matrix = new int[rows][cols];
        for ( int i = 0; i < list.size(); i++ ) {
            log.debug( "list " + i + " (an array) size: " + ( ( int[] ) list.get( i ) ).length );
            matrix[i] = ( ( int[] ) list.get( i ) );
            log.debug( "matrix[" + i + "] size: " + matrix[i].length );
        }

        return matrix;
    }

    /**
     * Each element in list is an array, which represents the columns of the matrix.
     * 
     * @param list
     * @param cols
     * @param rows
     * @return boolean[][]
     */
    boolean[][] convertListOfBooleanArraysToMatrix( List list, int cols, int rows ) {
        // TODO the cols will soon be defined by the designElement dimension
        cols = ( ( boolean[] ) list.get( 0 ) ).length;

        boolean[][] matrix = new boolean[rows][cols];
        for ( int i = 0; i < list.size(); i++ ) {
            log.debug( "list " + i + " (an array) size: " + ( ( boolean[] ) list.get( i ) ).length );
            matrix[i] = ( ( boolean[] ) list.get( i ) );
            log.debug( "matrix[" + i + "] size: " + matrix[i].length );
        }

        return matrix;
    }

    /**
     * Write the details of the double[][] array to a file.
     */
    public void log2DDoubleMatrixToFile( double[][] matrix, String filename ) {
        try {

            /*
             * use PrintWriter as opposed to a BufferedWriter so we can print all primitive data types.
             */
            PrintWriter out = new PrintWriter( new FileWriter( filename ) );
            log.debug( "matrix length (length of a 2D array) " + matrix.length );

            for ( int i = 0; i < matrix.length; i++ ) {
                for ( int j = 0; j < matrix[i].length; j++ ) {
                    log.debug( "matrix[" + i + "]" + "[" + j + "]" + matrix[i][j] );
                    out.print( matrix[i][j] );

                    // if not at the end of a line
                    if ( j != matrix[i].length - 1 ) out.write( " " );
                }
                out.println();
            }

            out.close();
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    /**
     * Write the details of the int[][] array to a file.
     */
    public void log2DIntMatrixToFile( int[][] matrix, String filename ) {
        try {
            /*
             * use PrintWriter as opposed to a BufferedWriter so we can print all primitive data types.
             */
            PrintWriter out = new PrintWriter( new FileWriter( filename ) );
            log.debug( "matrix length (length of a 2D array) " + matrix.length );

            for ( int i = 0; i < matrix.length; i++ ) {
                for ( int j = 0; j < matrix[i].length; j++ ) {
                    log.debug( "matrix[" + i + "]" + "[" + j + "]" + matrix[i][j] );
                    out.print( matrix[i][j] );

                    // if not at the end of a line
                    if ( j != matrix[i].length - 1 ) out.write( " " );
                }
                out.println();
            }

            out.close();
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    /**
     * Write the details of the boolean[][] array to a file.
     */
    public void log2DBooleanMatrixToFile( boolean[][] matrix, String filename ) {
        try {
            /*
             * use PrintWriter as opposed to a BufferedWriter so we can print all primitive data types.
             */
            PrintWriter out = new PrintWriter( new FileWriter( filename ) );
            log.debug( "matrix length (length of a 2D array) " + matrix.length );

            for ( int i = 0; i < matrix.length; i++ ) {
                for ( int j = 0; j < matrix[i].length; j++ ) {
                    log.debug( "matrix[" + i + "]" + "[" + j + "]" + matrix[i][j] );

                    out.print( matrix[i][j] );

                    // if not at the end of a line
                    if ( j != matrix[i].length - 1 ) out.write( " " );
                }
                out.println();
            }

            out.close();
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    /**
     * Logs details for each of the array lists. The parameter count is used to determine the frequency of logging. X Y
     * Intensity Stdev Pixels Outlier Masked
     * 
     * @param count
     */
    private void logDetails( int count ) {
        if ( count % ALERT_FREQUENCY == 0 ) log.debug( "Read in " + count + " items..." );
    }

    /**
     * @return Returns the numOfBioAssays.
     */
    public int getNumOfBioAssays() {
        return numOfBioAssays;
    }

    /**
     * @return Returns the numOfDesignElements.
     */
    public int getNumOfDesignElements() {
        return numOfDesignElements;
    }

    /**
     * @param numOfDesignElements The numOfDesignElements to set.
     */
    void setNumOfDesignElements( int numOfDesignElements ) {
        this.numOfDesignElements = numOfDesignElements;
    }

    /**
     * @return Returns the arrayListIntensity.
     */
    public List getIntensityList() {
        return intensityList;
    }

    /**
     * @return Returns the stdevList.
     */
    public List getStdevList() {
        return stdevList;
    }

    /**
     * @return Returns the pixelList.
     */
    public List getPixelList() {
        return pixelList;
    }

    /**
     * @return Returns the outlierList.
     */
    public List getOutlierList() {
        return outlierList;
    }

    /**
     * @return Returns the maskedList.
     */
    public List getMaskedList() {
        return maskedList;
    }

}
