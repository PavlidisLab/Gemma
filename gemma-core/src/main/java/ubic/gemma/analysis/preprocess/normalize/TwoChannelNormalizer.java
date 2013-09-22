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

/**
 * Interface representing a mechanism for normalizing two-color arrays.
 * 
 * @author pavlidis
 * @version $Id$
 */
public interface TwoChannelNormalizer {

    /**
     * Normalization without consideration of background or weights.
     * 
     * @param channelOneSignal
     * @param channelTwoSignal
     * @return
     */
    public DoubleMatrix<String, String> normalize( DoubleMatrix<String, String> channelOneSignal,
            DoubleMatrix<String, String> channelTwoSignal );

    /**
     * @param channelOneSignal
     * @param channelTwoSignal
     * @param channelOneBackground
     * @param channelTwoBackground
     * @param weights Allows different data points to have different weights in the normalization algorithm.
     * @return
     */
    public DoubleMatrix<String, String> normalize( DoubleMatrix<String, String> channelOneSignal,
            DoubleMatrix<String, String> channelTwoSignal, DoubleMatrix<String, String> channelOneBackground,
            DoubleMatrix<String, String> channelTwoBackground, DoubleMatrix<String, String> weights );

}
