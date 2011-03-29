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
package ubic.gemma.analysis.preprocess.normalize;

import java.io.IOException;

import ubic.basecode.dataStructure.matrix.DenseDoubleMatrix;
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.gemma.analysis.util.RCommander;

/**
 * Perform quantile normalization on a matrix, as described in:
 * <p>
 * Bolstad, B (2001) _Probe Level Quantile Normalization of High Density Oligonucleotide Array Data_. Unpublished
 * manuscript <a href="http://oz.berkeley.edu/~bolstad/stuff/qnorm.pdf">PDF</a>
 * </p>
 * <p>
 * Bolstad, B. M., Irizarry R. A., Astrand, M, and Speed, T. P. (2003) _A Comparison of Normalization Methods for High
 * Density Oligonucleotide Array Data Based on Bias and Variance._ Bioinformatics 19(2) ,pp 185-193. <a
 * href="http://www.stat.berkeley.edu/~bolstad/normalize/normalize.html">web page</a>
 * 
 * @author pavlidis
 * @version $Id$
 */
public class QuantileNormalizer<R, C> extends RCommander implements Normalizer<R, C> {

    public QuantileNormalizer() throws IOException {
        super();
        this.rc.voidEval( "library(preprocessCore)" );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.analysis.preprocess.Normalizer#normalize(baseCode.dataStructure.matrix.DenseDoubleMatrix2DNamed)
     */
    public DoubleMatrix<R, C> normalize( DoubleMatrix<R, C> dataMatrix ) {
        log.debug( "Normalizing..." );
        /*
         * FIXME this will not work correctly with missing values
         */
        String matrixvar = this.rc.assignMatrix( dataMatrix );
        this.rc.voidEval( "result<-normalize.quantiles(" + matrixvar + ")" );
        DoubleMatrix<String, String> plainResult = rc.retrieveMatrix( "result" );

        /*
         * IMPORTANT this assumes that normalize.quantiles gives us the matrix back with the same row/col ordering as
         * the original!
         */
        DoubleMatrix<R, C> finalResult = new DenseDoubleMatrix<R, C>( plainResult.getRawMatrix() );
        finalResult.setColumnNames( dataMatrix.getColNames() );
        finalResult.setRowNames( dataMatrix.getRowNames() );

        return finalResult;

    }
}
