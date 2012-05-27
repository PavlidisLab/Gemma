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
package ubic.gemma.analysis.preprocess.background;

import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.dataStructure.matrix.FastRowAccessDoubleMatrix;

/**
 * Implements simplest background subtraction, with the option to set small values to a preset value.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class SimpleBackgroundSubtraction<R, C> implements BackgroundAdjuster<R, C> {

    private double lowerLimit = Double.MIN_VALUE;

    /**
     * Set a lower limit to the value that can be obtained. This can be used to prevent negative values (set to zero),
     * or to set values less than 10 to be 10 (set this to 10), for example.
     * 
     * @param lowerLimit The lowerLimit to set.
     */
    public void setLowerLimit( double lowerLimit ) {
        this.lowerLimit = lowerLimit;
    }

    /**
     * The values of the background matrix are subtracted from the signal, element-wise, subject to any constraint
     * supplied by the lowerlimit.
     * 
     * @see ubic.gemma.analysis.preprocess.background.BackgroundAdjuster#adjust(baseCode.dataStructure.matrix.DoubleMatrix,
     *      baseCode.dataStructure.matrix.DoubleMatrix)
     * @see #setLowerLimit(double)
     */
    @Override
    public DoubleMatrix<R, C> adjust( DoubleMatrix<R, C> signal, DoubleMatrix<R, C> background ) {
        int rows = signal.rows();
        int cols = signal.columns();
        if ( rows != background.rows() ) throw new IllegalArgumentException();
        if ( cols != background.columns() ) throw new IllegalArgumentException();

        DoubleMatrix<R, C> result = new FastRowAccessDoubleMatrix<R, C>( rows, cols );

        for ( int i = 0; i < rows; i++ ) {
            for ( int j = 0; j < cols; j++ ) {
                result.set( i, j, Math.max( this.lowerLimit, signal.get( i, j ) - background.get( i, j ) ) );
            }
        }
        return result;
    }

}
