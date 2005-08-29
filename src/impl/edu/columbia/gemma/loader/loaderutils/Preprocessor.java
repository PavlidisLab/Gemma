package edu.columbia.gemma.loader.loaderutils;

import java.util.List;

import edu.columbia.gemma.common.quantitationtype.QuantitationType;
import edu.columbia.gemma.expression.bioAssay.BioAssay;
import edu.columbia.gemma.expression.designElement.DesignElement;

/**
 * Put data in a form that can be used in analysis.
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2005 Columbia University
 * 
 * @author keshav
 * @version $Id$
 */
public interface Preprocessor {
    
    /**
     * 
     * @param bioAssay
     * @param quantitationTypes
     * @param designElements
     */
    public void preprocess( BioAssay bioAssay, List<QuantitationType> quantitationTypes,
            List<DesignElement> designElements );

}
