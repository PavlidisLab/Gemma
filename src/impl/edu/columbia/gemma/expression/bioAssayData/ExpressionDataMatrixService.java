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
package edu.columbia.gemma.expression.bioAssayData;

import edu.columbia.gemma.expression.arrayDesign.ArrayDesign;
import edu.columbia.gemma.expression.experiment.ExpressionExperiment;
import baseCode.dataStructure.matrix.DoubleMatrixNamed;

/**
 * TODO - DOCUMENT ME
 * 
 * @author pavlidis
 * @version $Id$
 * @spring.bean id="bioAssayDataMatrixService"
 * @spring.property  name="designElementDataVectorService" ref = "designElementDataVectorService"
 */
public class ExpressionDataMatrixService {

    DesignElementDataVectorService  designElementDataVectorService;
    
    /**
     * @param designElementDataVectorService The designElementDataVectorService to set.
     */
    public void setDesignElementDataVectorService( DesignElementDataVectorService designElementDataVectorService ) {
        this.designElementDataVectorService = designElementDataVectorService;
    }

    /**
     * @param assayDimension
     * @param arrayDesign
     * @return
     */
    public DoubleMatrixNamed getMatrix( BioAssayDimension assayDimension, ArrayDesign arrayDesign ) {
        
        /*
         * get all the design elements for the arraydesign
         * find design
         */
        
        return null;
    }

    /**
     * @param expExp
     * @param assayDimension
     * @return
     */
    public DoubleMatrixNamed getMatrix( ExpressionExperiment expExp, BioAssayDimension assayDimension ) {
        return null;
    }

}
