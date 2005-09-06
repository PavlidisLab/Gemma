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

import edu.columbia.gemma.expression.arrayDesign.ArrayDesign;
import edu.columbia.gemma.tools.AffyBatch;
import edu.columbia.gemma.tools.RCommander;
import baseCode.dataStructure.matrix.DoubleMatrixNamed;

/**
 * Class to perform background adjustment for Affymetrix arrays, RMA-style.
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 */
public class RMABackgroundAdjuster extends RCommander implements BackgroundAdjuster {

    private ArrayDesign arrayDesign;
    private AffyBatch ab;

    public RMABackgroundAdjuster() {
        super();
        ab = new AffyBatch( rc );
    }

    /**
     * You must call setArrayDesign before using this method.
     * <p>
     * This is equivalent to running bg.correct.rma(affybatch).
     * 
     * @param signal The CEL matrix. The MM values are not changed by this algorithm.
     * @param background - not used by this method.
     * @see edu.columbia.gemma.analysis.preprocess.BackgroundAdjuster#adjust(baseCode.dataStructure.matrix.DoubleMatrixNamed,
     *      baseCode.dataStructure.matrix.DoubleMatrixNamed)
     */
    @SuppressWarnings("unused")
    public DoubleMatrixNamed adjust( DoubleMatrixNamed signal, DoubleMatrixNamed background ) {
        log.debug( "Background correcting..." );

        if ( arrayDesign == null ) throw new IllegalStateException( "Must set arrayDesign first" );
        String abName = ab.makeAffyBatch( signal, arrayDesign );
        rc.voidEval( "m<-exprs(bg.correct.rma(" + abName + " ))" );

        log.info( "Done with background correction" );

        DoubleMatrixNamed resultObject = rc.retrieveMatrix( "m" );

        // clean up.
        rc.remove( abName );
        rc.remove( "m" );

        return resultObject;
    }

    /**
     * @param arrayDesign2
     */
    public void setArrayDesign( ArrayDesign arrayDesign2 ) {
        if ( arrayDesign2 == null ) throw new IllegalArgumentException( "arrayDesign must not be null" );
        this.arrayDesign = arrayDesign2;
    }

}
