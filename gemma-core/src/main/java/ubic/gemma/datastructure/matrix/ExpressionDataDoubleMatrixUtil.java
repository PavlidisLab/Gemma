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
package ubic.gemma.datastructure.matrix;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.math.MatrixStats;
import ubic.gemma.analysis.preprocess.UnknownLogScaleException;
import ubic.gemma.analysis.preprocess.filter.ExpressionExperimentFilter;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import cern.colt.matrix.DoubleMatrix1D;

/**
 * Perform various computations on ExpressionDataMatrices (usually in-place).
 * 
 * @author pavlidis
 * @version $Id$
 */
public class ExpressionDataDoubleMatrixUtil {

    /**
     * 
     */
    private static final int LARGEST_EXPECTED_LOGGED_VALUE = 20;

    private static final double LOGARITHM_BASE = 2.0;

    private static Log log = LogFactory.getLog( ExpressionDataDoubleMatrixUtil.class.getName() );

    /**
     * Log transform if necessary, do any required filtering prior to analysis.
     * 
     * @param quantitationType
     * @param dmatrix
     */
    public static ExpressionDataDoubleMatrix filterAndLogTransform( QuantitationType quantitationType,
            ExpressionDataDoubleMatrix dmatrix ) {

        ScaleType scaleType = findScale( quantitationType, dmatrix.getMatrix() );

        if ( scaleType.equals( ScaleType.LOG2 ) ) {
            log.info( "Data is already on a log2 scale" );
        } else if ( scaleType.equals( ScaleType.LN ) ) {
            log.info( " **** Converting from ln to log2 **** " );
            MatrixStats.convertToLog2( dmatrix.getMatrix(), Math.E );
        } else if ( scaleType.equals( ScaleType.LOG10 ) ) {
            log.info( " **** Converting from log10 to log2 **** " );
            MatrixStats.convertToLog2( dmatrix.getMatrix(), 10 );
        } else if ( scaleType.equals( ScaleType.LINEAR ) ) {
            log.info( " **** LOG TRANSFORMING **** " );
            MatrixStats.logTransform( dmatrix.getMatrix() );
        } else if ( scaleType.equals( ScaleType.COUNT ) ) {
            log.info( " **** Converting from count to log2 counts per million **** " );
            DoubleMatrix1D librarySize = MatrixStats.colSums( dmatrix.getMatrix() );
            MatrixStats.convertToLog2Cpm( dmatrix.getMatrix(), librarySize );
        } else {
            throw new UnknownLogScaleException( "Can't figure out what scale the data are on" );
        }

        /*
         * We do this second because doing it first causes some kind of subtle problem ... (round off? I could not
         * really track this down).
         * 
         * Remove zero-variance rows, but also rows that have lots of equal values even if variance is non-zero. This
         * happens when data is "clipped" (e.g., all values under 10 set to 10).
         */
        int r = dmatrix.rows();
        dmatrix = ExpressionExperimentFilter.zeroVarianceFilter( dmatrix );
        if ( dmatrix.rows() < r ) {
            log.info( ( r - dmatrix.rows() ) + " rows removed due to low variance" );
        }
        r = dmatrix.rows();
        /* FIXME refactor as constant. Idea is to skip this if the data set is really small, but this is totally ad hoc. */
        if ( dmatrix.columns() > 4 ) {
            dmatrix = ExpressionExperimentFilter.tooFewDistinctValues( dmatrix, 0.5 );
            if ( dmatrix.rows() < r ) {
                log.info( ( r - dmatrix.rows() ) + " rows removed due to too many identical values" );
            }
        }

        return dmatrix;

    }

    /**
     * @param quantitationType
     * @param namedMatrix
     * @return ScaleType
     * @see ExpressionExperimentFilter for a related implementation.
     */
    public static ScaleType findScale( QuantitationType quantitationType,
            DoubleMatrix<CompositeSequence, BioMaterial> namedMatrix ) {

        if ( quantitationType.getScale() != null ) {
            if ( quantitationType.getScale().equals( ScaleType.LOG2 ) ) {
                return ScaleType.LOG2;
            } else if ( quantitationType.getScale().equals( ScaleType.LOG10 ) ) {
                return ScaleType.LOG10;
            } else if ( quantitationType.getScale().equals( ScaleType.LN ) ) {
                return ScaleType.LN;
            } else if ( quantitationType.getScale().equals( ScaleType.COUNT ) ) {
                return ScaleType.COUNT;
            } else if ( quantitationType.getScale().equals( ScaleType.LOGBASEUNKNOWN ) ) {
                throw new UnknownLogScaleException(
                        "Sorry, data on an unknown log scale is not supported. Please check the quantitation types, "
                                + "and make sure the data is expressed in terms of log2 or un-logged data  ("
                                + quantitationType + ")" );
            }
        }

        if ( namedMatrix.rows() == 0 || namedMatrix.columns() == 0 ) {
            throw new UnknownLogScaleException( "Cannot figure out scale without data (" + quantitationType + ")" );
        }

        for ( int i = 0; i < namedMatrix.rows(); i++ ) {
            for ( int j = 0; j < namedMatrix.columns(); j++ ) {
                double v = namedMatrix.get( i, j );
                if ( v > LARGEST_EXPECTED_LOGGED_VALUE ) {
                    log.debug( "Data has large values, doesn't look log transformed" );
                    return ScaleType.LINEAR;
                }
            }
        }

        throw new UnknownLogScaleException( "Data look log tranformed, not sure about base (" + quantitationType + ")" );

    }

    /**
     * Subtract two matrices. Ideally, they matrices are conformant, but if they are not (as some rows are sometimes
     * missing for some quantitation types), this method attempts to handle it anyway (see below). The rows and columns
     * do not have to be in the same order, but they do have to have the same column keys and row keys (with the
     * exception of missing rows). The result is stored in a. (a - b).
     * <p>
     * If the number of rows are not the same, and/or the rows have different keys in the two matrices, some rows will
     * simply not get subtracted and a warning will be issued.
     * 
     * @param a
     * @param b throws IllegalArgumentException if the matrices are not column-conformant.
     */
    public static void subtractMatrices( ExpressionDataDoubleMatrix a, ExpressionDataDoubleMatrix b ) {
        // checkConformant( a, b );
        if ( a.columns() != b.columns() )
            throw new IllegalArgumentException( "Unequal column counts: " + a.columns() + " != " + b.columns() );

        int columns = a.columns();
        for ( ExpressionDataMatrixRowElement el : a.getRowElements() ) {
            int rowNum = el.getIndex();
            CompositeSequence del = el.getDesignElement();

            if ( b.getRow( del ) == null ) {
                log.warn( "Matrix 'b' is missing a row for " + del + ", it will not be subtracted" );
                continue;
            }

            for ( int i = 0; i < columns; i++ ) {
                BioAssay assay = a.getBioAssaysForColumn( i ).iterator().next();
                double valA = a.get( del, assay );
                double valB = b.get( del, assay );
                a.set( rowNum, i, valA - valB );
            }
        }
    }

    /**
     * Log-transform the values in the matrix (base 2). Non-positive values (which have no logarithm defined) are
     * entered as NaN.
     * 
     * @param matrix
     */
    public static void logTransformMatrix( ExpressionDataDoubleMatrix matrix ) {
        int columns = matrix.columns();
        double log2 = Math.log( LOGARITHM_BASE );
        for ( ExpressionDataMatrixRowElement el : matrix.getRowElements() ) {
            CompositeSequence del = el.getDesignElement();
            for ( int i = 0; i < columns; i++ ) {
                BioAssay bm = matrix.getBioAssaysForColumn( i ).iterator().next();
                double valA = matrix.get( del, bm );
                if ( valA <= 0 ) {
                    matrix.set( del, bm, Double.NaN );
                } else {
                    matrix.set( del, bm, Math.log( valA ) / log2 );
                }
            }
        }

    }

    /**
     * Add two matrices. Ideally, they matrices are conformant, but if they are not (as some rows are sometimes missing
     * for some quantitation types), this method attempts to handle it anyway (see below). The rows and columns do not
     * have to be in the same order, but they do have to have the same column keys and row keys (with the exception of
     * missing rows). The result is stored in a.
     * <p>
     * If the number of rows are not the same, and/or the rows have different keys in the two matrices, some rows will
     * simply not get added and a warning will be issued.
     * 
     * @param a
     * @param b throws IllegalArgumentException if the matrices are not column-conformant.
     */
    public static void addMatrices( ExpressionDataDoubleMatrix a, ExpressionDataDoubleMatrix b ) {
        // checkConformant( a, b );
        if ( a.columns() != b.columns() )
            throw new IllegalArgumentException( "Unequal column counts: " + a.columns() + " != " + b.columns() );
        int columns = a.columns();
        for ( ExpressionDataMatrixRowElement el : a.getRowElements() ) {
            CompositeSequence del = el.getDesignElement();

            if ( b.getRow( del ) == null ) {
                log.warn( "Matrix 'b' is missing a row for " + del + ", this row will not be added" );
                continue;
            }
            for ( int i = 0; i < columns; i++ ) {
                BioAssay bm = a.getBioAssaysForColumn( i ).iterator().next();
                double valA = a.get( del, bm );
                double valB = b.get( del, bm );
                a.set( del, bm, valA + valB );
            }
        }
    }

    /**
     * Divide all values by the dividend
     * 
     * @param matrix
     * @param dividend
     * @throws IllegalArgumentException if dividend == 0.
     */
    public static void scalarDivideMatrix( ExpressionDataDoubleMatrix matrix, double dividend ) {
        if ( dividend == 0 ) throw new IllegalArgumentException( "Can't divide by zero" );
        int columns = matrix.columns();
        for ( ExpressionDataMatrixRowElement el : matrix.getRowElements() ) {
            CompositeSequence del = el.getDesignElement();
            for ( int i = 0; i < columns; i++ ) {
                BioAssay bm = matrix.getBioAssaysForColumn( i ).iterator().next();
                double valA = matrix.get( del, bm );
                matrix.set( del, bm, valA / dividend );

            }
        }
    }

    /**
     * Use the mask matrix to turn some values in a matrix to NaN. Ideally, they matrices are conformant, but if they
     * are not (as some rows are sometimes missing for some quantitation types), this method attempts to handle it
     * anyway (see below). The rows and columns do not have to be in the same order, but they do have to have the same
     * column keys and row keys (with the exception of missing rows). The result is stored in matrix.
     * 
     * @param matrix
     * @param mask if null, masking is not attempted.
     */
    public static void maskMatrix( ExpressionDataDoubleMatrix matrix, ExpressionDataBooleanMatrix mask ) {
        if ( mask == null ) return;
        // checkConformant( a, b );
        if ( matrix.columns() != mask.columns() )
            throw new IllegalArgumentException( "Unequal column counts: " + matrix.columns() + " != " + mask.columns() );
        int columns = matrix.columns();
        for ( ExpressionDataMatrixRowElement el : matrix.getRowElements() ) {
            CompositeSequence del = el.getDesignElement();
            if ( mask.getRow( del ) == null ) {
                log.warn( "Mask Matrix is missing a row for " + del );
                continue;
            }
            for ( int i = 0; i < columns; i++ ) {
                BioAssay bm = matrix.getBioAssaysForColumn( i ).iterator().next();
                boolean present = mask.get( del, bm );
                if ( !present ) {
                    matrix.set( del, bm, Double.NaN );
                }

            }
        }
    }

}
