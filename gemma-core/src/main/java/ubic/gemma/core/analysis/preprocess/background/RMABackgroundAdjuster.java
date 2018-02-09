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
package ubic.gemma.core.analysis.preprocess.background;

import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.gemma.core.analysis.util.AffyBatch;
import ubic.gemma.core.analysis.util.RCommander;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;

import java.io.IOException;

/**
 * Class to perform background adjustment for Affymetrix arrays, RMA-style.
 *
 * @author pavlidis
 */
public class RMABackgroundAdjuster extends RCommander implements BackgroundAdjuster<String, String> {

    private ArrayDesign arrayDesign = null;
    private AffyBatch ab = null;

    public RMABackgroundAdjuster() throws IOException {
        super();
        ab = new AffyBatch( this.rc );
    }

    /**
     * You must call setArrayDesign before using this method.
     * This is equivalent to running bg.correct.rma(affybatch).
     *
     * @param signal     The CEL matrix. The MM values are not changed by this algorithm.
     * @param background - not used by this method.
     * @return adjusted double matrix
     * @see ubic.gemma.core.analysis.preprocess.background.BackgroundAdjuster#adjust(DoubleMatrix, DoubleMatrix)
     */
    @Override
    public DoubleMatrix<String, String> adjust( DoubleMatrix<String, String> signal,
            DoubleMatrix<String, String> background ) {
        log.debug( "Background correcting..." );

        if ( arrayDesign == null )
            throw new IllegalStateException( "Must set arrayDesign first" );
        String abName = ab.makeAffyBatch( signal, arrayDesign );

        rc.voidEval( "m<-exprs(bg.correct.rma(" + abName + " ))" );

        log.info( "Done with background correction" );

        DoubleMatrix<String, String> resultObject = rc.retrieveMatrix( "m" );

        // clean up.
        rc.remove( abName );
        rc.remove( "m" );

        return resultObject;
    }

    /**
     * @param arrayDesign2 the array design
     */
    public void setArrayDesign( ArrayDesign arrayDesign2 ) {
        if ( arrayDesign2 == null )
            throw new IllegalArgumentException( "arrayDesign must not be null" );
        this.arrayDesign = arrayDesign2;
    }

}
