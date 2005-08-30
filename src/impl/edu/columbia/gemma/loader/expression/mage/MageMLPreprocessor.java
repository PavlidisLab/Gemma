package edu.columbia.gemma.loader.expression.mage;

import java.util.ArrayList;
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
     * Create 2D matrices from raw data and the 3 dimensions - BioAssay, QuantitationType, DesignElement Raw files:
     * Column 1 - int - X Column 2 - int - Y Column 3 - double - Intensity Column 4 - double - Standard Deviation Column
     * 5 - int - Pixels Column 6 - boolean - Outlier Column 7 - boolean - Masked
     * 
     * @param bioAssays
     * @param quantitationTypes
     * @param designElements TODO this is a work in progress - the javadoc column names are for my benefit. I will clean
     *        this up.
     */
    @SuppressWarnings("unchecked")
    public void preprocess( BioAssay bioAssay, List<QuantitationType> quantitationTypes,
            List<DesignElement> designElements ) {
        Log log = LogFactory.getLog( MageMLPreprocessor.class.getName() );

        log.info( " preprocessing the data ..." );

        log.debug( "There are " + quantitationTypes.size() + " quantitation types for bioassay " + bioAssay );

        List<QuantitationType> arrayList = new ArrayList();// used ArrayList because it is equivalent to Vector
                                                            // (ordered and more efficient than LinkedList).
        for ( QuantitationType qt : quantitationTypes ) {
            log.debug( "QuantitationType: " + qt );
            //TODO send qt to the file as a title
            //arrayList.add(  );
        }

        log.debug( "There are " + designElements.size() + " design elements for bioassay " + bioAssay );
        for ( DesignElement de : designElements ) {
            log.debug( "DesignElement: " + de );
        }

    }

    // you will want to parse the raw file, where each row is a design element and each column is quantitation type.
    // the output will be a file where the title is the quantitation type, the x-axis is the design element (is this
    // each
    // row in the raw data file?

}
