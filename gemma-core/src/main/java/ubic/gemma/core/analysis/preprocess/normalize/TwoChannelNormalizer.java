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
package ubic.gemma.core.analysis.preprocess.normalize;

import ubic.basecode.dataStructure.matrix.DoubleMatrix;

/**
 * Interface representing a mechanism for normalizing two-color arrays.
 *
 * @author pavlidis
 */
public interface TwoChannelNormalizer {

    DoubleMatrix<String, String> normalize( DoubleMatrix<String, String> channelOneSignal,
            DoubleMatrix<String, String> channelTwoSignal );

    DoubleMatrix<String, String> normalize( DoubleMatrix<String, String> channelOneSignal,
            DoubleMatrix<String, String> channelTwoSignal, DoubleMatrix<String, String> channelOneBackground,
            DoubleMatrix<String, String> channelTwoBackground, DoubleMatrix<String, String> weights );

}
