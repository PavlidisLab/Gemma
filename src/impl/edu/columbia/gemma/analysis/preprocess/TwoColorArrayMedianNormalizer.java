package edu.columbia.gemma.analysis.preprocess;

import baseCode.dataStructure.matrix.DoubleMatrixNamed;
import baseCode.util.RCommand;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 */
public class TwoColorArrayMedianNormalizer extends MarrayNormalizer implements TwoChannelNormalizer {

    public TwoColorArrayMedianNormalizer() {
        super();
    }

    public TwoColorArrayMedianNormalizer( RCommand rc ) {
        super( rc );
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.columbia.gemma.analysis.preprocess.TwoChannelNormalizer#normalize(baseCode.dataStructure.matrix.DoubleMatrixNamed,
     *      baseCode.dataStructure.matrix.DoubleMatrixNamed, baseCode.dataStructure.matrix.DoubleMatrixNamed,
     *      baseCode.dataStructure.matrix.DoubleMatrixNamed, baseCode.dataStructure.matrix.DoubleMatrixNamed)
     */
    public DoubleMatrixNamed normalize( DoubleMatrixNamed channelOneSignal, DoubleMatrixNamed channelTwoSignal,
            DoubleMatrixNamed channelOneBackground, DoubleMatrixNamed channelTwoBackground, DoubleMatrixNamed weights ) {
        log.debug( "normalizing..." );
        DoubleMatrixNamed resultObject = mNorm( channelOneSignal, channelTwoSignal, channelOneBackground,
                channelTwoBackground, weights, "median" );
        return resultObject;
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.columbia.gemma.analysis.preprocess.TwoChannelNormalizer#normalize(baseCode.dataStructure.matrix.DoubleMatrixNamed,
     *      baseCode.dataStructure.matrix.DoubleMatrixNamed)
     */
    public DoubleMatrixNamed normalize( DoubleMatrixNamed channelOneSignal, DoubleMatrixNamed channelTwoSignal ) {
        log.debug( "normalizing..." );
        DoubleMatrixNamed resultObject = mNorm( channelOneSignal, channelTwoSignal, "median" );
        return resultObject;
    }

}
