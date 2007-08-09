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
package ubic.gemma.analysis.preprocess;

import ubic.basecode.dataStructure.matrix.DoubleMatrix2DNamedFactory;
import ubic.basecode.dataStructure.matrix.DoubleMatrixNamed2D;

/**
 * Implements simplest background subtraction, with the option to set small values to a preset value.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class SimpleBackgroundSubtraction implements BackgroundAdjuster {

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
     * @see ubic.gemma.analysis.preprocess.BackgroundAdjuster#adjust(baseCode.dataStructure.matrix.DoubleMatrixNamed2D,
     *      baseCode.dataStructure.matrix.DoubleMatrixNamed2D)
     * @see #setLowerLimit(double)
     */
    public DoubleMatrixNamed2D adjust( DoubleMatrixNamed2D signal, DoubleMatrixNamed2D background ) {
        int rows = signal.rows();
        int cols = signal.columns();
        if ( rows != background.rows() ) throw new IllegalArgumentException();
        if ( cols != background.columns() ) throw new IllegalArgumentException();

        DoubleMatrixNamed2D result = DoubleMatrix2DNamedFactory.fastrow( rows, cols );

        for ( int i = 0; i < rows; i++ ) {
            for ( int j = 0; j < rows; j++ ) {
                result.setQuick( i, j, Math
                        .max( this.lowerLimit, signal.getQuick( i, j ) - background.getQuick( i, j ) ) );
            }
        }
        return result;
    }

}
