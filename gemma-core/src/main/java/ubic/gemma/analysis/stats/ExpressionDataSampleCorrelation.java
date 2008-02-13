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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.dataStructure.matrix.DenseDoubleMatrix2DNamed;
import ubic.basecode.dataStructure.matrix.DoubleMatrix2DNamedFactory;
import ubic.basecode.dataStructure.matrix.DoubleMatrixNamed;
import ubic.basecode.gui.ColorMatrix;
import ubic.basecode.gui.JMatrixDisplay;
import ubic.basecode.io.reader.DoubleMatrixReader;
import ubic.basecode.io.writer.MatrixWriter;
import ubic.basecode.math.MatrixStats;
import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.datastructure.matrix.ExpressionDataMatrixColumnSort;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.util.ConfigUtils;

/**
 * Given an ExpressionDataMatrix, compute the correlation of the columns (samples) and also create images.sf
 * 
 * @author Paul
 * @version $Id$
 */
public class ExpressionDataSampleCorrelation {

    private static final String FILE_SUFFIX = "_corrmat";

    private static final int LARGE_CELL_SIZE = 10;

    /**
     * Suffix on lower contrast, large cell image (sample names will be shown)
     */
    public static final String LARGE_LOWCONTRAST = ".loctlg.png";

    /**
     * Suffix on lower contrast, small cell image
     */
    public static final String SMALL_LOWCONTRAST = ".loctsm.png";

    /**
     * Name of the directory where the correlation matrices will be stored (within the configured analysis results area)
     */
    public static final String CORRMAT_DIR_NAME = "corrmat";

    /**
     * Suffix on high contrast, small cell image.
     */
    public static final String SMALL_HIGHCONTRAST = ".hictsm.png";

    /**
     * Suffix on high contrast, large cell image (sample names will be shown)
     */
    public static final String LARGE_HIGHCONTRAST = ".hictlg.png";

    private static Log log = LogFactory.getLog( ExpressionDataSampleCorrelation.class.getName() );

    /**
     * Compute the sample correlation matrix, save it to configured file location, and create PNG images.
     * 
     * @param eeDoubleMatrix
     * @param ee
     */
    public static void process( ExpressionDataDoubleMatrix eeDoubleMatrix, ExpressionExperiment ee ) {
        DoubleMatrixNamed cormat = getMatrix( eeDoubleMatrix );
        String fileBaseName = getMatrixFileBaseName( ee );
        try {
            ExpressionDataSampleCorrelation.createMatrixImages( cormat, getStorageDirectory(), fileBaseName );
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    /**
     * @param matrix a n x m
     * @return m x m symmetric matrix containing correlations of the columns. Names are the column names from the
     *         original matrix.
     */
    @SuppressWarnings("unchecked")
    public static DoubleMatrixNamed getMatrix( ExpressionDataDoubleMatrix matrix ) {
        int cols = matrix.columns();
        double[][] rawcols = new double[cols][];

        List<BioMaterial> ordered = ExpressionDataMatrixColumnSort.orderByExperimentalDesign( matrix );

        /*
         * Transpose the matrix, basically.
         */
        int m = 0;
        for ( BioMaterial bioMaterial : ordered ) {
            int i = matrix.getColumnIndex( bioMaterial );
            Double[] colo = matrix.getColumn( i );
            rawcols[m] = new double[colo.length];
            for ( int j = 0; j < colo.length; j++ ) {
                rawcols[m][j] = colo[j];
            }
            m++;
        }

        DoubleMatrixNamed<Object, Object> columns = new DenseDoubleMatrix2DNamed<Object, Object>( rawcols );
        DoubleMatrixNamed<BioAssay, BioAssay> mat = MatrixStats.correlationMatrix( columns );

        List<BioAssay> colElements = new ArrayList<BioAssay>();
        for ( BioMaterial bioMaterial : ordered ) {
            int i = matrix.getColumnIndex( bioMaterial );
            Collection<BioAssay> bas = matrix.getBioAssaysForColumn( i );
            colElements.add( bas.iterator().next() );
        }
        mat.setRowNames( colElements );
        mat.setColumnNames( colElements );
        return mat;
    }

    /**
     * @param ee
     * @return
     */
    public static String getMatrixFileBaseName( ExpressionExperiment ee ) {
        String fileBaseName = ee.getShortName() + FILE_SUFFIX;
        return fileBaseName;
    }

    /**
     * Generate images of sample correlation (also saes text file)
     * 
     * @param matrix
     * @param location directory where files will be saved
     * @param fileBaseName root name for files (without .png ending)
     */
    protected static void createMatrixImages( DoubleMatrixNamed<String, String> matrix, File location,
            String fileBaseName ) throws IOException {

        writeMatrix( matrix, location, fileBaseName + ".txt" );

        int numRows = matrix.rows();

        DoubleMatrixNamed clippedHard = clipData( matrix, 0.8, 1.0 );

        DoubleMatrixNamed clippedSoft = clipData( matrix, 0.4, 1.0 );

        ColorMatrix hard = new ColorMatrix( clippedHard );
        ColorMatrix soft = new ColorMatrix( clippedSoft );

        int smallSize = 2;
        if ( numRows > 50 ) smallSize = 1;
        if ( numRows < 10 ) smallSize = 4;

        writeImage( hard, location, fileBaseName + SMALL_HIGHCONTRAST, smallSize, false );
        writeImage( soft, location, fileBaseName + SMALL_LOWCONTRAST, smallSize, false );

        writeImage( hard, location, fileBaseName + LARGE_HIGHCONTRAST, LARGE_CELL_SIZE, true );
        writeImage( soft, location, fileBaseName + LARGE_LOWCONTRAST, LARGE_CELL_SIZE, true );

    }

    /**
     * Save the matrix itself to a text file.
     * 
     * @param matrix
     * @param location directory
     * @param file
     * @throws IOException
     */
    private static void writeMatrix( DoubleMatrixNamed matrix, File location, String file ) throws IOException {
        File f = new File( location, file );
        OutputStream o;

        o = new FileOutputStream( f );
        MatrixWriter writer = new ubic.basecode.io.writer.MatrixWriter( o );
        writer.writeMatrix( matrix, true );
        o.flush();
        o.close();
        log.info( "Wrote " + f.length() + " bytes to " + f.getAbsolutePath() );
    }

    /**
     * @param matrix
     * @param location
     * @param fileName
     * @param size
     * @param addlabels
     * @throws IOException
     */
    private static void writeImage( ColorMatrix matrix, File location, String fileName, int size, boolean addlabels )
            throws IOException {
        JMatrixDisplay writer = new JMatrixDisplay( matrix );
        writer.setCellSize( new Dimension( size, size ) );
        File f = new File( location, fileName );
        writer.saveImage( matrix, f.getAbsolutePath(), addlabels, false );
    }

    /**
     * Clip matrix so values are within the limits; this operates on a copy.
     */
    private static DoubleMatrixNamed<String, String> clipData( DoubleMatrixNamed<String, String> data,
            double lowThresh, double highThresh ) {

        // create a copy
        DoubleMatrixNamed<String, String> copy = DoubleMatrix2DNamedFactory.dense( data );

        // clip the copy and return it.
        for ( int i = 0; i < copy.rows(); i++ ) {
            for ( int j = 0; j < copy.columns(); j++ ) {
                double val = copy.get( i, j );
                if ( val > highThresh ) {
                    val = highThresh;
                } else if ( val < lowThresh ) {
                    val = lowThresh;
                }
                copy.set( i, j, val );
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

    /**
     * 
     */
    public static File getStorageDirectory() throws IOException {
        File dir = new File( ConfigUtils.getAnalysisStoragePath() + File.separatorChar
                + ExpressionDataSampleCorrelation.CORRMAT_DIR_NAME );
        if ( !dir.exists() ) {
            boolean success = dir.mkdir();
            if ( !success ) {
                throw new IOException( "Could not create directory to store results: " + dir );
            }
        }
        return dir;
    }

}
