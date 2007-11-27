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

import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.designElement.DesignElement;

/**
 * Perform various compuations on ExpressionDataMatrices (usually in-place).
 * 
 * @author pavlidis
 * @version $Id$
 */
public class ExpressionDataDoubleMatrixUtil {

    private static final double LOGARITHM_BASE = 2.0;

    /**
     * Subtract two conformant matrices. The rows and columns do not have to be in the same order, but they do have to
     * have the same row and column keys. The result is stored in a.
     * 
     * @param a
     * @param b
     */
    public static void subtractMatrices( ExpressionDataDoubleMatrix a, ExpressionDataDoubleMatrix b ) {
        checkConformant( a, b );
        int columns = a.columns();
        for ( ExpressionDataMatrixRowElement el : a.getRowElements() ) {
            int rowNum = el.getIndex();
            DesignElement del = el.getDesignElement();
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
            DesignElement del = el.getDesignElement();
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
     * Add two conformant matrices. The rows and columns do not have to be in the same order, but they do have to have
     * the same row and column keys. The result is stored in a.
     * 
     * @param a
     * @param b
     */
    public static void addMatrices( ExpressionDataDoubleMatrix a, ExpressionDataDoubleMatrix b ) {
        checkConformant( a, b );
        int columns = a.columns();
        for ( ExpressionDataMatrixRowElement el : a.getRowElements() ) {
            DesignElement del = el.getDesignElement();
            for ( int i = 0; i < columns; i++ ) {
                BioAssay bm = a.getBioAssaysForColumn( i ).iterator().next();
                double valA = a.get( del, bm );
                double valB = b.get( del, bm );
                a.set( del, bm, valA + valB );
            }
        }
    }

    private static void checkConformant( ExpressionDataMatrix a, ExpressionDataMatrix b ) {
        if ( a.rows() != b.rows() )
            throw new IllegalArgumentException( "Unequal row counts: " + a.rows() + " != " + b.rows() );
        if ( a.columns() != b.columns() )
            throw new IllegalArgumentException( "Unequal column counts: " + a.columns() + " != " + b.columns() );
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
            DesignElement del = el.getDesignElement();
            for ( int i = 0; i < columns; i++ ) {
                BioAssay bm = matrix.getBioAssaysForColumn( i ).iterator().next();
                double valA = matrix.get( del, bm );
                matrix.set( del, bm, valA / dividend );

            }
        }
    }

    /**
     * Use the mask matrix to turn some values in a conformant matrix to NaN. The rows and columns do not have to be in
     * the same order, but they do have to have the same row and column keys. The result is stored in a.
     * 
     * @param matrix
     * @param mask if null, masking is not attempted.
     */
    public static void maskMatrix( ExpressionDataDoubleMatrix matrix, ExpressionDataBooleanMatrix mask ) {
        if ( mask == null ) return;
        checkConformant( matrix, mask );
        int columns = matrix.columns();
        for ( ExpressionDataMatrixRowElement el : matrix.getRowElements() ) {
            DesignElement del = el.getDesignElement();
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
