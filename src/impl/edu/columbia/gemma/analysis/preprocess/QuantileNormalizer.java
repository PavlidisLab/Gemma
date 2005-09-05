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

import baseCode.dataStructure.matrix.DoubleMatrixNamed;
import edu.columbia.gemma.tools.RCommander;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 */
public class QuantileNormalizer extends RCommander implements Normalizer {

    public QuantileNormalizer() {
        super();
        rc.voidEval( "library(affy)" );
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.columbia.gemma.analysis.preprocess.Normalizer#normalize(baseCode.dataStructure.matrix.DenseDoubleMatrix2DNamed)
     */
    public DoubleMatrixNamed normalize( DoubleMatrixNamed dataMatrix ) {
        log.debug( "Normalizing..." );
        String matrixvar = rc.assignMatrix( dataMatrix );
        rc.voidEval( "result<-normalize.quantiles(" + matrixvar + ")" );
        return rc.retrieveMatrix( "result" );
    }
}
