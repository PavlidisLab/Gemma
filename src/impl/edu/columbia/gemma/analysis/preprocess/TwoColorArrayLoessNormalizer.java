/*
 * The Gemma project
 * 
 * Copyright (c) 2005 Columbia University
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
package edu.columbia.gemma.analysis.preprocess;

import edu.columbia.gemma.tools.MArrayRaw;
import edu.columbia.gemma.tools.RCommander;
import baseCode.dataStructure.matrix.DoubleMatrixNamed;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 */
public class TwoColorArrayLoessNormalizer extends RCommander implements Normalizer {

    public TwoColorArrayLoessNormalizer() {
        super();
        rc.voidEval( "library(marray)" );
    }

    /**
     * @see edu.columbia.gemma.analysis.preprocess.Normalizer#normalize(baseCode.dataStructure.matrix.DoubleMatrixNamed)
     */
    public DoubleMatrixNamed normalize( DoubleMatrixNamed dataMatrix ) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @param channelOneSignal
     * @param channelTwoSignal
     * @param channelOneBackground
     * @param channelTwoBackground
     * @param weights
     * @return
     */
    public DoubleMatrixNamed normalize( DoubleMatrixNamed channelOneSignal, DoubleMatrixNamed channelTwoSignal,
            DoubleMatrixNamed channelOneBackground, DoubleMatrixNamed channelTwoBackground, DoubleMatrixNamed weights ) {
        log.debug( "normalizing..." );
        MArrayRaw mRaw = new MArrayRaw( this.rc );
        mRaw.makeMArrayLayout( channelOneSignal.rows() );
        String mRawVarName = mRaw.makeMArrayRaw( channelOneSignal, channelTwoSignal, channelOneBackground,
                channelTwoBackground, weights );

        String normalizedMatrixVarName = "normalized." + channelOneSignal.hashCode();
        rc.voidEval( normalizedMatrixVarName + "<-maM(maNorm(" + mRawVarName + ", norm=\"loess\" ))" );
        log.info( "Done normalizing" );

        // the normalized
        DoubleMatrixNamed resultObject = rc.retrieveMatrix( normalizedMatrixVarName );

        // clean up.
        rc.remove( mRawVarName );
        rc.remove( normalizedMatrixVarName );
        return resultObject;
    }
}
