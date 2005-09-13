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

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.columbia.gemma.common.quantitationtype.QuantitationType;
import edu.columbia.gemma.expression.bioAssay.BioAssay;
import edu.columbia.gemma.expression.designElement.DesignElement;
import edu.columbia.gemma.loader.loaderutils.Preprocessor;

/**
 * <hr>
 * <p>
 * An {@link edu.columbia.gemma.loader.expression.mage.RawDataParser} is required by this implementation, so the raw
 * data files can be parsed correctly.
 * </p>
 * <p>
 * Copyright (c) 2004 - 2005 Columbia University
 * 
 * @author keshav
 * @version $Id$
 * @spring.bean id="mageMLPreprocessor" singleton="false"
 */
public class MageMLPreprocessor implements Preprocessor {

    RawDataParser rdp = null;

    /**
     * @return Returns the intensityList (from RawDataParser).
     */
    public List getIntensityList() {
        return rdp.getIntensityList();
    }

    /**
     * @return Returns the stdevList (from RawDataParser).
     */
    public List getStdevList() {
        return rdp.getStdevList();
    }

    /**
     * @return Returns the pixelList (from RawDataParser).
     */
    public List getPixelList() {
        return rdp.getPixelList();
    }

    /**
     * @return Returns the outlierList (from RawDataParser).
     */
    public List getOutlierList() {
        return rdp.getOutlierList();
    }

    /**
     * @return Returns the maskedList (from RawDataParser).
     */
    public List getMaskedList() {
        return rdp.getMaskedList();
    }

    /**
     * instantiate a RawDataProcessor.
     */
    public MageMLPreprocessor() {
        rdp = new RawDataParser();
    }

    /**
     * Creates a list of 2D arrays from raw data and the 3 dimensions - BioAssay, QuantitationType, DesignElement.
     * Calling any of the methods {@link #convertListOfDoubleArraysToMatrix( List list, int cols, int rows )},
     * {@link #convertListOfIntArraysToMatrix( List list, int cols, int rows )}, or
     * {@link #convertListOfBooleanArraysToMatrix( List list, int cols, int rows )} after this will convert this list to
     * a matrix. Raw files: Column 1 - int - X Column 2 - int - Y Column 3 - double - Intensity Column 4 - double -
     * Standard Deviation Column 5 - int - Pixels Column 6 - boolean - Outlier Column 7 - boolean - Masked
     * 
     * @param bioAssays
     * @param quantitationTypes
     * @param designElements
     */
    @SuppressWarnings("unchecked")
    public void preprocess( BioAssay bioAssay, List<QuantitationType> quantitationTypes,
            List<DesignElement> designElements, InputStream is ) {
        Log log = LogFactory.getLog( MageMLPreprocessor.class.getName() );

        log.info( " preprocessing the data ..." );

        log.debug( "There are " + quantitationTypes.size() + " quantitation types for bioassay " + bioAssay );

        try {
            // TODO set this to designElements.size()
            rdp.setNumOfDesignElements( 1000 );
            rdp.parse( is );
        } catch ( IOException e ) {
            System.err.print( "error with parse method when parsing raw data file" );
            e.printStackTrace();
        }

    }

    /**
     * Argument list is a list of double[][]. Returns a 2D double[][] - a matrix.
     * 
     * @param list
     * @param cols
     * @param rows
     * @return double[][]
     */
    @SuppressWarnings("unused")
    public double[][] convertListOfDoubleArraysToMatrix( List list, int cols, int rows ) {
        return rdp.convertListOfDoubleArraysToMatrix( list, cols, rows );
    }

    /**
     * Argument list is a list of int[][]. Returns a 2D int[][] - a matrix.
     * 
     * @param list
     * @param cols
     * @param rows
     * @return int[][]
     */
    @SuppressWarnings("unused")
    public int[][] convertListOfIntArraysToMatrix( List list, int cols, int rows ) {
        return rdp.convertListOfIntArraysToMatrix( list, cols, rows );
    }

    /**
     * Argument list is a list of boolean[][]. Returns a 2D boolean[][] - a matrix.
     * 
     * @param list
     * @param cols
     * @param rows
     * @return boolean[][]
     */
    @SuppressWarnings("unused")
    public boolean[][] convertListOfBooleanArraysToMatrix( List list, int cols, int rows ) {
        return rdp.convertListOfBooleanArraysToMatrix( list, cols, rows );
    }

    /**
     * Writes the double[][] matrix out to a file.
     * 
     * @param matrix
     * @param filename
     */
    public void log2DDoubleMatrixToFile( double[][] matrix, String filename ) {
        rdp.log2DDoubleMatrixToFile( matrix, filename );
    }

    /**
     * Writes the int[][] matrix out to a file.
     * 
     * @param matrix
     * @param filename
     */
    public void log2DIntMatrixToFile( int[][] matrix, String filename ) {
        rdp.log2DIntMatrixToFile( matrix, filename );
    }

    /**
     * Writes the boolean[][] matrix out to a file.
     * 
     * @param matrix
     * @param filename
     */
    public void log2DBooleanMatrixToFile( boolean[][] matrix, String filename ) {
        rdp.log2DBooleanMatrixToFile( matrix, filename );
    }
}
