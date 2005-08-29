package edu.columbia.gemma.loader.expression.mage;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.columbia.gemma.common.quantitationtype.QuantitationType;
import edu.columbia.gemma.expression.bioAssay.BioAssay;
import edu.columbia.gemma.expression.designElement.DesignElement;
import edu.columbia.gemma.loader.loaderutils.Preprocessor;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2005 Columbia University
 * 
 * @author keshav
 * @version $Id$
 * @spring.bean id="mageMLPreprocessor" singleton="false"
 */
public class MageMLPreprocessor implements Preprocessor {

    /**
     * Create 2D matrices from raw data and the 3 dimensions - BioAssay, QuantitationType, DesignElement 
     * Raw files:
     * Column 1 - int - X 
     * Column 2 - int - Y 
     * Column 3 - double - Intensity
     * Column 4 - double - Standard Deviation
     * Column 5 - boolean - Pixels
     * Column 6 - boolean - Outlier
     * @param bioAssays
     * @param quantitationTypes
     * @param designElements
     * TODO this is a work in progress - the javadoc column names are for my benefit.  I will clean this up. 
     */
    public void preprocess( BioAssay bioAssay, List<QuantitationType> quantitationTypes,
            List<DesignElement> designElements ) {
        Log log = LogFactory.getLog( MageMLPreprocessor.class.getName() );

        log.info( " preprocessing the data ..." );

        log.debug( "There are " + quantitationTypes.size() + " quantitation types for bioassay " + bioAssay );
        for ( Object qt : quantitationTypes ) {
            log.debug( "QuantitationType: " + qt );
        }

        log.debug( "There are " + designElements.size() + " design elements for bioassay " + bioAssay );
        for ( Object de : designElements ) {
            log.debug( "DesignElement: " + de );
        }

    }

}
