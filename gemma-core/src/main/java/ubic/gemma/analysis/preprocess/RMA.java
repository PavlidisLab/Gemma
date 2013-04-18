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

import java.io.IOException;

import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.gemma.analysis.util.AffyBatch;
import ubic.gemma.analysis.util.RCommander;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;

/**
 * Perform Robust Multiarray Average probe-level summarization of Affymetrix microarray data.
 * 
 * @author pavlidis
 * @version $Id$
 * @deprecated because it uses R, which we are trying to avoid (brittle); and we are not currently using this.
 */
@Deprecated
public class RMA extends RCommander implements ProbeSummarizer {

    private ArrayDesign arrayDesign = null;
    private AffyBatch ab;

    public enum pmCorrectMethod {
        MAS, PMONLY, SUBTRACTMM
    }

    public RMA() throws IOException {
        super();
        ab = new AffyBatch( this.rc );
    }

    private pmCorrectMethod pmMethod = pmCorrectMethod.PMONLY;

    /**
     * You must call setArrayDesign() before calling this method. You may also optionally set the PM correction method
     * ("pmonly" is the default). Note that this only performs the median polish summarization step of RMA, not the
     * entire analysis. The CEL matrix should be background corrected and normalized first, if desired.
     * <p>
     * With the defaults this method is equivalent to using the affy package call
     * <code>exprs(expresso(affybatch, bg.correct=FALSE, normalize=FALSE, pmcorrect.method="pmonly", summary.method="medianpolish"))</code>
     * 
     * @param dataMatrix The CEL value matrix
     * @return RMA-processed matrix.
     * @see ubic.gemma.model.analysis.preprocess.ProbeSummarizer#summarize(baseCode.dataStructure.matrix.DoubleMatrix)
     */
    @Override
    public DoubleMatrix<String, String> summarize( DoubleMatrix<String, String> dataMatrix ) {
        log.debug( "Summarizing..." );

        if ( arrayDesign == null ) throw new IllegalStateException( "Must set arrayDesign first" );
        String abName = ab.makeAffyBatch( dataMatrix, arrayDesign );
        String varname = "m";

        rc.voidEval( varname + "<-exprs(expresso(" + abName + ", bg.correct=FALSE, normalize=FALSE, "
                + "pmcorrect.method='pmonly', summary.method='medianpolish'))" );

        log.info( "Done with RMA" );

        DoubleMatrix<String, String> resultObject = rc.retrieveMatrix( varname );

        // clean up.
        rc.voidEval( "rm(" + varname + ")" );

        return resultObject;
    }

    /**
     * @param arrayDesign2
     */
    public void setArrayDesign( ArrayDesign arrayDesign2 ) {
        if ( arrayDesign2 == null ) throw new IllegalArgumentException( "arrayDesign must not be null" );
        this.arrayDesign = arrayDesign2;
    }

    /**
     * @return Returns the pmMethod.
     */
    public pmCorrectMethod getPmMethod() {
        return this.pmMethod;
    }

    /**
     * @param pmMethod The pmMethod to set.
     */
    public void setPmMethod( pmCorrectMethod pmMethod ) {
        this.pmMethod = pmMethod;
    }

}
