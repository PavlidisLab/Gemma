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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.dataStructure.matrix.DoubleMatrixNamed;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorService;
import ubic.gemma.model.expression.designElement.DesignElement;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * Produces DoubleMatrix objects for ExpressionExperiments.
 * 
 * @author pavlidis
 * @version $Id$
 * @spring.bean id="expressionDataMatrixService"
 * @spring.property name="designElementDataVectorService" ref = "designElementDataVectorService"
 */
public class ExpressionDataMatrixService {

    private static Log log = LogFactory.getLog( ExpressionDataMatrixService.class.getName() );

    DesignElementDataVectorService designElementDataVectorService;

    /**
     * @param expExp
     * @param qt
     * @return
     * @deprecated use getMatrix instead
     */
    @SuppressWarnings("unchecked")
    public DoubleMatrixNamed getDoubleNamedMatrix( ExpressionExperiment expExp, QuantitationType qt ) {
        Collection<DesignElementDataVector> vectors = this.designElementDataVectorService.findAllForMatrix( expExp, qt );
        if ( vectors == null || vectors.size() == 0 ) {
            log.warn( "No vectors for " + expExp + " and " + qt );
            return null;
        }
        return new ExpressionDataDoubleMatrix( expExp, qt ).getNamedMatrix();
    }

    /**
     * @param expExp
     * @param qt
     * @return
     */
    @SuppressWarnings("unchecked")
    public ExpressionDataMatrix getMatrix( ExpressionExperiment expExp, QuantitationType qt ) {
        Collection<DesignElementDataVector> vectors = this.designElementDataVectorService.findAllForMatrix( expExp, qt );
        if ( vectors == null || vectors.size() == 0 ) {
            log.warn( "No vectors for " + expExp + " and " + qt );
            return null;
        }

        // designElementDataVectorService.thaw(vectors); // FIXME this is needed.

        return new ExpressionDataDoubleMatrix( vectors, qt );

    }

    /**
     * @param expExp
     * @param designElements
     * @param qt
     * @return
     */
    public ExpressionDataMatrix getMatrix( ExpressionExperiment expExp, Collection<DesignElement> designElements,
            QuantitationType qt ) {
        throw new UnsupportedOperationException( "not implemented yet" );
    }

    /**
     * @param designElementDataVectorService The designElementDataVectorService to set.
     */
    public void setDesignElementDataVectorService( DesignElementDataVectorService designElementDataVectorService ) {
        this.designElementDataVectorService = designElementDataVectorService;
    }

}
