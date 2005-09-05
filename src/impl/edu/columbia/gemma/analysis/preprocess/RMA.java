package edu.columbia.gemma.analysis.preprocess;

import baseCode.dataStructure.matrix.DoubleMatrixNamed;
import edu.columbia.gemma.expression.arrayDesign.ArrayDesign;
import edu.columbia.gemma.tools.AffyBatch;
import edu.columbia.gemma.tools.RCommander;

/**
 * Perform Robust Multiarray Average analysis of Affymetrix microarray data.
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 */
public class RMA extends RCommander implements ProbeSummarizer {

    private ArrayDesign arrayDesign;
    private AffyBatch ab;

    public RMA() {
        super();
        ab = new AffyBatch();
    }

    /**
     * You must call setArrayDesign() before calling this method.
     * 
     * @param dataMatrix The CEL value matrix
     * @see edu.columbia.gemma.analysis.preprocess.ProbeSummarizer#summarize(baseCode.dataStructure.matrix.DoubleMatrixNamed)
     */
    public DoubleMatrixNamed summarize( DoubleMatrixNamed dataMatrix ) {
        log.debug( "Summarizing..." );

        if ( arrayDesign == null ) throw new IllegalStateException( "Must set arrayDesign first" );
        String abName = ab.makeAffyBatch( dataMatrix, arrayDesign );
        rc.voidEval( "v<-rma(" + abName + ")" );
        log.info( "Done with RMA" );
        rc.voidEval( "m<-exprs(v)" );

        DoubleMatrixNamed resultObject = rc.retrieveMatrix( "m" );

        // clean up.
        rc.voidEval( "rm(v)" );
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

}
