/*
 * The Gemma project
 * 
 * Copyright (c) 2006 Columbia University
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
package ubic.gemma.visualization;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.CompositeSequenceService;
import ubic.gemma.model.expression.designElement.DesignElement;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * @author keshav
 * @version $Id$
 * @spring.bean id="matrixVisualizationData"
 * @spring.property name="compositeSequnceService" ref="compositeSequenceService"
 * @spring.property name="expressionExperiment" ref="expressionExperiment"
 */
public class MatrixVisualizationData {
    private Log log = LogFactory.getLog( this.getClass() );

    private ExpressionExperiment expressionExperiment = null;

    private Collection<DesignElement> designElements = null;

    private CompositeSequenceService compositeSequenceService = null;

    private Map metadata = new HashMap();

    /**
     * 
     *
     */
    public MatrixVisualizationData() {

    }

    /**
     * @param expressionExperiment
     * @param designElements
     */
    public MatrixVisualizationData( ExpressionExperiment expressionExperiment, Collection<DesignElement> designElements ) {

        this.expressionExperiment = expressionExperiment;
        this.designElements = designElements;

        for ( Object designElement : designElements ) {
            // FIXME I have made the association between DesignElement and DesignElementDataVector bi-directional.
            String key = ( ( CompositeSequence ) designElement ).getName();
            Collection<DesignElementDataVector> deDataVectors = ( ( CompositeSequence ) designElement )
                    .getDesignElementDataVectors();
            metadata.put( key, deDataVectors );
        }

        Collection<String> keySet = metadata.keySet();
        for ( String key : keySet ) {
            // key.getData();
            log.debug( "key: " + key );
        }
    }

    /**
     * @return Collection<DesignElement>
     */
    public Collection<DesignElement> getDesignElements() {
        return designElements;
    }

    /**
     * @param designElements
     */
    public void setDesignElements( Collection<DesignElement> designElements ) {
        this.designElements = designElements;
    }

    /**
     * @return ExpressionExperiment
     */
    public ExpressionExperiment getExpressionExperiment() {
        return expressionExperiment;
    }

    /**
     * @param expressionExperiment
     */
    public void setExpressionExperiment( ExpressionExperiment expressionExperiment ) {
        this.expressionExperiment = expressionExperiment;
    }

    /**
     * @return CompositeSequenceService
     */
    public CompositeSequenceService getCompositeSequenceService() {
        return compositeSequenceService;
    }

    /**
     * @param compositeSequenceService
     */
    public void setCompositeSequenceService( CompositeSequenceService compositeSequenceService ) {
        this.compositeSequenceService = compositeSequenceService;
    }

}
