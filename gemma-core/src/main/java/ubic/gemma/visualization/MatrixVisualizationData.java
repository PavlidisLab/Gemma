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

import ubic.gemma.model.expression.designElement.CompositeSequenceService;
import ubic.gemma.model.expression.designElement.CompositeSequenceServiceImpl;
import ubic.gemma.model.expression.designElement.DesignElement;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * @author keshav
 * @version $Id$
 */
public class MatrixVisualizationData {

    private ExpressionExperiment expressionExperiment = null;

    private Collection<DesignElement> designElements = null;

    private CompositeSequenceService compositeSequenceService = null;

    /**
     * 
     *
     */
    public MatrixVisualizationData() {
        compositeSequenceService = new CompositeSequenceServiceImpl();
    }

    /**
     * @param expressionExperiment
     * @param designElements
     */
    public MatrixVisualizationData( ExpressionExperiment expressionExperiment, Collection<DesignElement> designElements ) {
        compositeSequenceService = new CompositeSequenceServiceImpl();// FIXME you could 'springify' this instead.

        this.expressionExperiment = expressionExperiment;
        this.designElements = designElements;

        for ( Object designElement : designElements ) {
            // ((CompositeSequence) designElement).getDesignElementDataVectors();//TODO make association between
            // DesignElement and DesignElementDataVector bi-directional.
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

}
