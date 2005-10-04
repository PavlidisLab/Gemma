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
package edu.columbia.gemma.loader.expression.mage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.columbia.gemma.expression.designElement.DesignElement;

/**
 * Class to hold information on the dimension data extracted from MAGE-ML files.
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 */
public class BioAssayDimensions {
    private Map<edu.columbia.gemma.expression.bioAssay.BioAssay, List<edu.columbia.gemma.common.quantitationtype.QuantitationType>> quantitationTypeDimensions = new HashMap<edu.columbia.gemma.expression.bioAssay.BioAssay, List<edu.columbia.gemma.common.quantitationtype.QuantitationType>>();

    private Map<edu.columbia.gemma.expression.bioAssay.BioAssay, List<edu.columbia.gemma.expression.designElement.DesignElement>> designElementDimensions = new HashMap<edu.columbia.gemma.expression.bioAssay.BioAssay, List<DesignElement>>();

    public List<edu.columbia.gemma.common.quantitationtype.QuantitationType> getQuantitationTypeDimension(
            edu.columbia.gemma.expression.bioAssay.BioAssay ba ) {
        return quantitationTypeDimensions.get( ba );
    }

    /**
     * @param bioAssayName
     * @param convertedQuantitationTypes
     */
    public void addQuantitationTypeDimension( edu.columbia.gemma.expression.bioAssay.BioAssay bioAssay,
            List<edu.columbia.gemma.common.quantitationtype.QuantitationType> quantitationTypes ) {
        quantitationTypeDimensions.put( bioAssay, quantitationTypes );
    }

    /**
     * @param bioAssay
     * @param designElements
     */
    public void addDesignElementDimension( edu.columbia.gemma.expression.bioAssay.BioAssay bioAssay,
            List<edu.columbia.gemma.expression.designElement.DesignElement> designElements ) {
        designElementDimensions.put( bioAssay, designElements );

    }

    public List<edu.columbia.gemma.expression.designElement.DesignElement> getDesignElementDimension(
            edu.columbia.gemma.expression.bioAssay.BioAssay ba ) {
        return designElementDimensions.get( ba );
    }

}
