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

import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.math.MatrixNormalizer;

/**
 * Perform quantile normalization on a matrix, as described in:
 * <p>
 * Bolstad, B (2001) _Probe Level Quantile Normalization of High Density Oligonucleotide Array Data_. Unpublished
 * manuscript <a href="http://oz.berkeley.edu/~bolstad/stuff/qnorm.pdf">PDF</a>
 * </p>
 * <p>
 * Bolstad, B. M., Irizarry R. A., Astrand, M, and Speed, T. P. (2003) _A Comparison of Normalization Methods for High
 * Density Oligonucleotide Array Data Based on Bias and Variance._ Bioinformatics 19(2) ,pp 185-193. <a
 * href="http://www.stat.berkeley.edu/~bolstad/normalize/normalize.html">web page</a>.
 * <p>
 * However, note that this deals with missing values differently than the Bioconductor implementation.
 * 
 * @author pavlidis
 * @version $Id$
 * @see ubic.baseCode.math.MatrixNormalizer
 */
public class QuantileNormalizer<R, C> implements Normalizer<R, C> {

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.analysis.preprocess.Normalizer#normalize(baseCode.dataStructure.matrix.DoubleMatrix)
     */
    @Override
    public DoubleMatrix<R, C> normalize( DoubleMatrix<R, C> dataMatrix ) {

        MatrixNormalizer<R, C> m = new MatrixNormalizer<R, C>();
        return m.quantileNormalize( dataMatrix );

    }
}
