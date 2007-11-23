/*
 * The Gemma project
 * 
 * Copyright (c) 2007 University of British Columbia
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
package ubic.gemma.analysis.stats;

import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.dataStructure.matrix.DoubleMatrix2DNamedFactory;
import ubic.basecode.dataStructure.matrix.DoubleMatrixNamed;
import ubic.basecode.gui.ColorMatrix;
import ubic.basecode.gui.JMatrixDisplay;
import ubic.basecode.io.reader.DoubleMatrixReader;
import ubic.basecode.math.MatrixStats;
import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.model.expression.bioAssay.BioAssay;

/**
 * Given an ExpressionDataMatrix, compute the correlation of the columns (samples).
 * 
 * @author Paul
 * @version $Id$
 */
public class ExpressionDataSampleCorrelation {

    private static Log log = LogFactory.getLog( ExpressionDataSampleCorrelation.class.getName() );

    /**
     * @param matrix
     * @return
     */
    public static DoubleMatrixNamed getMatrix( ExpressionDataDoubleMatrix matrix ) {
        int cols = matrix.columns();
        double[][] rawcols = new double[cols][];

        for ( int i = 0; i < cols; i++ ) {
            Double[] colo = matrix.getColumn( i );
            rawcols[i] = new double[colo.length];
            for ( int j = 0; j < colo.length; j++ ) {
                rawcols[i][j] = colo[j];
            }
        }

        DoubleMatrixNamed columns = DoubleMatrix2DNamedFactory.dense( rawcols );
        DoubleMatrixNamed mat = MatrixStats.correlationMatrix( columns );

        List<Object> colElements = new ArrayList<Object>();
        for ( int i = 0; i < cols; i++ ) {
            Collection<BioAssay> bas = matrix.getBioAssaysForColumn( i );
            colElements.add( bas.iterator().next() );
        }
        mat.setRowNames( colElements );
        mat.setColumnNames( colElements );
        return mat;
    }

    /**
     * Generate images of sample correlation
     * 
     * @param matrix
     * @param location directory where files will be saved
     * @param fileBaseName root name for files (without .png ending)
     */
    public static void createMatrixImages( DoubleMatrixNamed matrix, File location, String fileBaseName )
            throws IOException {

        int numRows = matrix.rows();

        DoubleMatrixNamed clippedHard = clipData( matrix, 0.8, 1.0 );

        DoubleMatrixNamed clippedSoft = clipData( matrix, 0.4, 1.0 );

        ColorMatrix hard = new ColorMatrix( clippedHard );
        ColorMatrix soft = new ColorMatrix( clippedSoft );

        int smallSize = 2;
        if ( numRows > 50 ) smallSize = 1;
        if ( numRows < 10 ) smallSize = 4;

        writeImage( hard, location, fileBaseName + ".hictsm.png", smallSize, false );
        writeImage( soft, location, fileBaseName + ".loctsm.png", smallSize, false );

        writeImage( hard, location, fileBaseName + ".hictlg.png", 8, true );
        writeImage( soft, location, fileBaseName + ".loctlg.png", 8, true );

    }

    private static void writeImage( ColorMatrix matrix, File location, String fileName, int size, boolean addlabels )
            throws IOException {
        JMatrixDisplay writer = new JMatrixDisplay( matrix );
        writer.setCellSize( new Dimension( size, size ) );
        File f = new File( location, fileName );
        writer.saveImage( matrix, f.getAbsolutePath(), addlabels, false );
        log.info( "Wrote " + f.getAbsolutePath() );
    }

    /**
     * Clip matrix so values are within the limits; this operates on a copy.
     */
    private static DoubleMatrixNamed clipData( DoubleMatrixNamed data, double lowThresh, double highThresh ) {

        // create a copy
        DoubleMatrixNamed copy = DoubleMatrix2DNamedFactory.dense( data );

        // clip the copy and return it.
        for ( int i = 0; i < copy.rows(); i++ ) {
            for ( int j = 0; j < copy.columns(); j++ ) {
                double val = copy.getQuick( i, j );
                if ( val > highThresh ) {
                    val = highThresh;
                } else if ( val < lowThresh ) {
                    val = lowThresh;
                }
                copy.setQuick( i, j, val );
            }
        }
        return copy;
    }

    /**
     * @param is input stream with tab-delimited expression data. This is here for testing.
     * @return
     * @throws IOException
     */
    protected static DoubleMatrixNamed getMatrixFromExperimentFile( InputStream is ) throws IOException {
        DoubleMatrixReader reader = new DoubleMatrixReader();
        DoubleMatrixNamed matrix = ( DoubleMatrixNamed ) reader.read( is );
        int cols = matrix.columns();
        double[][] rawcols = new double[cols][];

        for ( int i = 0; i < cols; i++ ) {
            rawcols[i] = matrix.getColumn( i );
        }

        DoubleMatrixNamed columns = DoubleMatrix2DNamedFactory.dense( rawcols );
        return MatrixStats.correlationMatrix( columns ); // This has col/row names after the samples.
    }

}
