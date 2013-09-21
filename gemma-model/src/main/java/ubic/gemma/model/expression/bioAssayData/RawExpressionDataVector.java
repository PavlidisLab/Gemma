/*
 * The Gemma project.
 * 
 * Copyright (c) 2006-2012 University of British Columbia
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
package ubic.gemma.model.expression.bioAssayData;

import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * Data for one design element, across one or more bioassays, for a single quantitation type. For example, the
 * "expression profile" for a probe (gene) across a set of samples
 */
public abstract class RawExpressionDataVector extends
        ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorImpl {

    /**
     * Constructs new instances of {@link ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector}.
     */
    public static final class Factory {
        /**
         * Constructs a new instance of {@link ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector}.
         */
        public static ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector newInstance() {
            return new ubic.gemma.model.expression.bioAssayData.RawExpressionDataVectorImpl();
        }

    }

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = 3282821691174780641L;
    private ExpressionExperiment expressionExperiment;

    /**
     * No-arg constructor added to satisfy javabean contract
     * 
     * @author Paul
     */
    public RawExpressionDataVector() {
    }

    /**
     * 
     */
    @Override
    public ubic.gemma.model.expression.experiment.ExpressionExperiment getExpressionExperiment() {
        return this.expressionExperiment;
    }

    @Override
    public void setExpressionExperiment(
            ubic.gemma.model.expression.experiment.ExpressionExperiment expressionExperiment ) {
        this.expressionExperiment = expressionExperiment;
    }

}