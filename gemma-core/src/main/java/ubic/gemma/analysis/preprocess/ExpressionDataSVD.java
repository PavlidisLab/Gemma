/*
 * The Gemma project
 * 
 * Copyright (c) 2008 University of British Columbia
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

import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import cern.colt.matrix.linalg.Algebra;
import ubic.basecode.dataStructure.matrix.DenseDoubleMatrix;
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.math.SingularValueDecomposition;
import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.model.expression.designElement.DesignElement;

/**
 * @author paul
 * @version $Id$
 */
public class ExpressionDataSVD {
    SingularValueDecomposition<DesignElement, Integer> svd;
    private ExpressionDataDoubleMatrix expressionData;

    /**
     * @param expressionData
     * @param rescale If true, the data matrix will be rescaled to mean zero, variance one, on a per-column (sample)
     *        basis.
     */
    public ExpressionDataSVD( ExpressionDataDoubleMatrix expressionData, boolean rescale ) {
        this.expressionData = expressionData;
        DoubleMatrix<DesignElement, Integer> matrix = expressionData.getMatrix();
        this.svd = new SingularValueDecomposition<DesignElement, Integer>( matrix );

    }

    /**
     * @return the s
     */
    public DoubleMatrix<Integer, Integer> getS() {
        return svd.getS();
    }

    /**
     * @return the u
     */
    public DoubleMatrix<DesignElement, Integer> getU() {
        return svd.getU();
    }

    /**
     * Provide a reconstructed matrix removing the first N components.
     * 
     * @param numComponentsToRemove The number of components to remove, starting from the largest eigenvalue.
     * @return
     */
    public ExpressionDataDoubleMatrix removeHighestComponents( int numComponentsToRemove ) {
        DoubleMatrix<Integer, Integer> copy = svd.getS().copy();

        for ( int i = 0; i < numComponentsToRemove; i++ ) {
            copy.set( i, i, 0.0 );
        }

        double[][] rawU = svd.getU().getRawMatrix();
        double[][] rawS = copy.getRawMatrix();
        double[][] rawV = svd.getV().getRawMatrix();

        DoubleMatrix2D u = new DenseDoubleMatrix2D( rawU );
        DoubleMatrix2D s = new DenseDoubleMatrix2D( rawS );
        DoubleMatrix2D v = new DenseDoubleMatrix2D( rawV );

        Algebra a = new Algebra();
        DoubleMatrix<DesignElement, Integer> reconstructed = new DenseDoubleMatrix<DesignElement, Integer>( a.mult(
                a.mult( u, s ), a.transpose( v ) ).toArray() );
        reconstructed.setRowNames( this.expressionData.getMatrix().getRowNames() );
        reconstructed.setColumnNames( this.expressionData.getMatrix().getColNames() );

        return new ExpressionDataDoubleMatrix( this.expressionData, reconstructed );

    }

}
