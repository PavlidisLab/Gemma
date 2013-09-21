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
package ubic.gemma.model.analysis;

import ubic.gemma.model.analysis.expression.ExpressionAnalysis;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;

/**
 * 
 */
public abstract class MultiExperimentAnalysis extends ExpressionAnalysis implements gemma.gsec.model.Securable {

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = 3685073398229124310L;

    private ubic.gemma.model.analysis.expression.ExpressionExperimentSet expressionExperimentSetAnalyzed;

    /**
     * No-arg constructor added to satisfy javabean contract
     * 
     * @author Paul
     */
    public MultiExperimentAnalysis() {
    }

    /**
     * 
     */
    public ExpressionExperimentSet getExpressionExperimentSetAnalyzed() {
        return this.expressionExperimentSetAnalyzed;
    }

    public void setExpressionExperimentSetAnalyzed( ExpressionExperimentSet expressionExperimentSetAnalyzed ) {
        this.expressionExperimentSetAnalyzed = expressionExperimentSetAnalyzed;
    }

}