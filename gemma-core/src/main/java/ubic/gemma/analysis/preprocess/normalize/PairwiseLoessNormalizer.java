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

import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.gemma.analysis.util.RCommander;

/**
 * Perform pairwise LOESS normalization on the columns of a matrix.
 * 
 * @author pavlidis
 * @version $Id$
 * @deprecated because we don't like to use the R integration
 */
@Deprecated
public class PairwiseLoessNormalizer extends RCommander implements Normalizer<String, String> {

    public PairwiseLoessNormalizer() throws IOException {
        super();
        rc.voidEval( "library(affy)" );
    }

    /**
     * This uses the default settings.
     * 
     * @see ubic.gemma.model.analysis.preprocess.Normalizer#normalize(baseCode.dataStructure.matrix.DoubleMatrix)
     */
    @Override
    public DoubleMatrix<String, String> normalize( DoubleMatrix<String, String> dataMatrix ) {
        log.debug( "Normalizing..." );
        String matrixvar = rc.assignMatrix( dataMatrix );
        rc.voidEval( "result<-normalize.loess(" + matrixvar + ")" );
        return rc.retrieveMatrix( "result" );
    }

}
