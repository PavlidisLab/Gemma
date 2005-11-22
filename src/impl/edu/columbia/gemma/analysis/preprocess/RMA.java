package edu.columbia.gemma.analysis.preprocess;

import baseCode.dataStructure.matrix.DoubleMatrixNamed;
import edu.columbia.gemma.expression.arrayDesign.ArrayDesign;
import edu.columbia.gemma.tools.AffyBatch;
import edu.columbia.gemma.tools.RCommander;

/**
 * Perform Robust Multiarray Average probe-level summarization of Affymetrix microarray data.
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 */
public class RMA extends RCommander implements ProbeSummarizer {

    private ArrayDesign arrayDesign = null;
    private AffyBatch ab;

    public enum pmCorrectMethod {
        MAS, PMONLY, SUBTRACTMM
    }

    public RMA() {
        super();
        ab = new AffyBatch( rc );
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
     * @see edu.columbia.gemma.analysis.preprocess.ProbeSummarizer#summarize(baseCode.dataStructure.matrix.DoubleMatrixNamed)
     */
    public DoubleMatrixNamed summarize( DoubleMatrixNamed dataMatrix ) {
        log.debug( "Summarizing..." );

        if ( arrayDesign == null ) throw new IllegalStateException( "Must set arrayDesign first" );
        String abName = ab.makeAffyBatch( dataMatrix, arrayDesign );
        rc.voidEval( "m<-exprs(expresso(" + abName + ", bg.correct=FALSE, normalize=FALSE, "
                + "pmcorrect.method=\"pmonly\", summary.method=\"medianpolish\"))" );

        log.info( "Done with RMA" );

        DoubleMatrixNamed resultObject = rc.retrieveMatrix( "m" );

        // clean up.
        rc.voidEval( "rm(m)" );

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
