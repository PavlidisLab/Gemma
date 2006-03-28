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

import ubic.basecode.dataStructure.matrix.DoubleMatrixNamed;

/**
 * @author pavlidis
 * @version $Id$
 */
public class TwoColorArrayLoessNormalizer extends MarrayNormalizer implements TwoChannelNormalizer {

    public TwoColorArrayLoessNormalizer() {
        super();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.analysis.preprocess.TwoChannelNormalizer#normalize(baseCode.dataStructure.matrix.DoubleMatrixNamed,
     *      baseCode.dataStructure.matrix.DoubleMatrixNamed, baseCode.dataStructure.matrix.DoubleMatrixNamed,
     *      baseCode.dataStructure.matrix.DoubleMatrixNamed, baseCode.dataStructure.matrix.DoubleMatrixNamed)
     */
    public DoubleMatrixNamed normalize( DoubleMatrixNamed channelOneSignal, DoubleMatrixNamed channelTwoSignal,
            DoubleMatrixNamed channelOneBackground, DoubleMatrixNamed channelTwoBackground, DoubleMatrixNamed weights ) {
        log.debug( "normalizing..." );
        DoubleMatrixNamed resultObject = normalize( channelOneSignal, channelTwoSignal, channelOneBackground,
                channelTwoBackground, weights, "loess" );
        return resultObject;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.analysis.preprocess.TwoChannelNormalizer#normalize(baseCode.dataStructure.matrix.DoubleMatrixNamed,
     *      baseCode.dataStructure.matrix.DoubleMatrixNamed)
     */
    public DoubleMatrixNamed normalize( DoubleMatrixNamed channelOneSignal, DoubleMatrixNamed channelTwoSignal ) {
        log.debug( "normalizing..." );
        DoubleMatrixNamed resultObject = normalize( channelOneSignal, channelTwoSignal, "loess" );
        return resultObject;
    }
}
