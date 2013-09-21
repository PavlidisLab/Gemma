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
package ubic.gemma.model.analysis.expression;

import java.util.Collection;
import java.util.HashSet;

import ubic.gemma.model.analysis.AnalysisResultSet;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;

/**
 * 
 */
public abstract class FactorAssociatedAnalysisResultSet extends AnalysisResultSet {

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = 821072688513147160L;

    private Collection<ExperimentalFactor> experimentalFactors = new HashSet<ExperimentalFactor>();

    /**
     * No-arg constructor added to satisfy javabean contract
     * 
     * @author Paul
     */
    public FactorAssociatedAnalysisResultSet() {
    }

    /**
     * 
     */
    public Collection<ExperimentalFactor> getExperimentalFactors() {
        return this.experimentalFactors;
    }

    public void setExperimentalFactors( Collection<ExperimentalFactor> experimentalFactors ) {
        this.experimentalFactors = experimentalFactors;
    }

}