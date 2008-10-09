/*
 * The Gemma project
 * 
 * Copyright (c) 2008 University of British Columbia
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
package ubic.gemma.web.controller.visualization;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import ubic.basecode.math.DescriptiveWithMissing;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorService;
import ubic.gemma.model.expression.bioAssayData.DoubleVectorValueObject;
import cern.colt.list.DoubleArrayList;

/**
 * Processes data requests for visualization of expression profiles. Designed for ajax applications
 * <p>
 * Supports:
 * <Ul>
 * <li>Gene pair data, where we want data for a regular x,y scatter plot for two probes from one study.
 * <li>Gene profile data, where the data is on its normal scale, to show as a profile plot.
 * <li>Gene profile data, where the data is processed for display in a heatmap. The data will be normalized and
 * 'clipped'.
 * </ul>
 * 
 * This might be deprecated in favor of DEDV controller
 * 
 * @spring.bean id="visualizationController"
 * @spring.property name="designElementDataVectorService" ref="designElementDataVectorService"
 * @author Paul, klc
 * @version $Id$
 */
public class VisualizationController {

    DesignElementDataVectorService designElementDataVectorService;

    /*
     * API notes. We will want to be able to pass back a _gene_ and and _experiment(s)_ and get back the data. Would
     * also make sense to have a method that simply takes designelementdatavector ids (as a starting point)
     */

    /*
     * The returned objects need to be simple beans holding one or more double[], along with gene information.
     */

    /*
     * Need to return sample information as well for column labels.
     */

    public Collection<ExpressionProfileDataObject> getVectorData( Collection<Long> dedvIds ) {
        List<ExpressionProfileDataObject> result = new ArrayList<ExpressionProfileDataObject>();
        for ( Long id : dedvIds ) {
            DesignElementDataVector vector = this.designElementDataVectorService.load( id );
            DoubleVectorValueObject dvvo = new DoubleVectorValueObject( vector );
            ExpressionProfileDataObject epdo = new ExpressionProfileDataObject( dvvo );

            DoubleArrayList doubleArrayList = new cern.colt.list.DoubleArrayList( epdo.getData() );
            DescriptiveWithMissing.standardize( doubleArrayList );
            epdo.setData( doubleArrayList.elements() );

            result.add( epdo );
        }

        // TODO fill in gene; normalize and clip if desired.; watch for invalid ids.

        return result;
    }

    public void setDesignElementDataVectorService( DesignElementDataVectorService designElementDataVectorService ) {
        this.designElementDataVectorService = designElementDataVectorService;
    }

}
