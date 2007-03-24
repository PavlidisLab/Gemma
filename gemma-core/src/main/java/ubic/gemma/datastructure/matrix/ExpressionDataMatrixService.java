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
package ubic.gemma.datastructure.matrix;

import java.util.Collection;

import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorService;
import ubic.gemma.model.expression.designElement.DesignElement;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * Produces DoubleMatrix objects for ExpressionExperiments.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class ExpressionDataMatrixService {

    DesignElementDataVectorService designElementDataVectorService;

    /**
     * @param ee
     * @param qts
     * @return
     * @deprecated Use ExpressionDataMatrixBuilder instead.
     */
    public ExpressionDataDoubleMatrix getMatrix( ExpressionExperiment ee, Collection<QuantitationType> qts ) {
        return new ExpressionDataDoubleMatrix( ee, qts );
    }

    /**
     * @param expExp
     * @param designElements
     * @param qt
     * @deprecated Use ExpressionDataMatrixBuilder instead.
     * @return
     */
    public ExpressionDataMatrix getMatrix( ExpressionExperiment expExp, Collection<DesignElement> designElements,
            QuantitationType qt ) {
        throw new UnsupportedOperationException( "not implemented yet" );
    }

}
