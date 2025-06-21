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
package ubic.gemma.web.controller.expression.experiment;

import ubic.gemma.model.expression.experiment.ExperimentalFactorValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;

import java.util.Collection;
import java.util.HashSet;

/**
 * @author keshav
 *
 */
public class ExpressionExperimentExperimentalFactorValueObject {

    private ExpressionExperimentValueObject expressionExperiment;

    private final Collection<ExperimentalFactorValueObject> experimentalFactors;

    private int numFactors;

    public ExpressionExperimentExperimentalFactorValueObject() {
        experimentalFactors = new HashSet<>();
    }

    public Collection<ExperimentalFactorValueObject> getExperimentalFactors() {
        return experimentalFactors;
    }

    public ExpressionExperimentValueObject getExpressionExperiment() {
        return expressionExperiment;
    }

    public int getNumFactors() {
        return numFactors;
    }

    public void setExpressionExperiment( ExpressionExperimentValueObject expressionExperiment ) {
        this.expressionExperiment = expressionExperiment;
    }

    public void setNumFactors( int numFactors ) {
        this.numFactors = numFactors;
    }

}
