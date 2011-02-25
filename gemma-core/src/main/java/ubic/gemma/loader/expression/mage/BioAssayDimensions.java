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
package ubic.gemma.loader.expression.mage;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.model.expression.bioAssay.BioAssay;

/**
 * Class to hold information on the dimension data extracted from MAGE-ML files.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class BioAssayDimensions {

    private static Log log = LogFactory.getLog( BioAssayDimensions.class.getName() );

    private Map<BioAssay, List<ubic.gemma.model.common.quantitationtype.QuantitationType>> quantitationTypeDimensions = new HashMap<BioAssay, List<ubic.gemma.model.common.quantitationtype.QuantitationType>>();

    private Map<BioAssay, List<ubic.gemma.model.expression.designElement.CompositeSequence>> designElementDimensions = new HashMap<BioAssay, List<ubic.gemma.model.expression.designElement.CompositeSequence>>();

    public Collection<BioAssay> getQuantitationTypeBioAssays() {
        return quantitationTypeDimensions.keySet();
    }

    /**
     * @param ba
     * @return
     */
    public List<ubic.gemma.model.common.quantitationtype.QuantitationType> getQuantitationTypeDimension(
            ubic.gemma.model.expression.bioAssay.BioAssay ba ) {
        List<ubic.gemma.model.common.quantitationtype.QuantitationType> qts = quantitationTypeDimensions.get( ba );
        if ( qts == null ) {
            log.debug( "No quantitation types for " + ba.getName() );
        }
        return qts;
    }

    /**
     * @param bioAssayName
     * @param convertedQuantitationTypes
     */
    public void addQuantitationTypeDimension( ubic.gemma.model.expression.bioAssay.BioAssay bioAssay,
            List<ubic.gemma.model.common.quantitationtype.QuantitationType> quantitationTypes ) {
        quantitationTypeDimensions.put( bioAssay, quantitationTypes );
    }

    /**
     * @param bioAssay
     * @param designElements
     */
    public void addDesignElementDimension( ubic.gemma.model.expression.bioAssay.BioAssay bioAssay,
            List<ubic.gemma.model.expression.designElement.CompositeSequence> designElements ) {
        designElementDimensions.put( bioAssay, designElements );

    }

    /**
     * @param ba
     * @return
     */
    public List<ubic.gemma.model.expression.designElement.CompositeSequence> getDesignElementDimension(
            ubic.gemma.model.expression.bioAssay.BioAssay ba ) {
        List<ubic.gemma.model.expression.designElement.CompositeSequence> dts = designElementDimensions.get( ba );

        if ( dts == null ) {
            throw new RuntimeException( ba.getName() + " was not found in designElementDimensions ("
                    + designElementDimensions.size() + " dimensions available)" );
        }

        return dts;
    }

}
