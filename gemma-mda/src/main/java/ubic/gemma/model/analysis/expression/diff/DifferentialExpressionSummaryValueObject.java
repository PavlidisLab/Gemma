/* The Gemma project
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

package ubic.gemma.model.analysis.expression.diff;

import java.util.Collection;
import java.util.HashSet;

import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExperimentalFactorValueObject;

public class DifferentialExpressionSummaryValueObject implements java.io.Serializable  {

    /**
     * 
     */
    private static final long serialVersionUID = 2063274043081170625L
    ;
    private Collection<ExperimentalFactorValueObject> experimentalFactors;
    private Double qValue;
    private Double threshold;
    private int numberOfDiffExpressedProbes;
    private long resultSetId;

    public DifferentialExpressionSummaryValueObject() {
        super();
    }

    public DifferentialExpressionSummaryValueObject( Collection<ExperimentalFactorValueObject> experimentalFactors,
            Double qValue, Double threshold, int numberOfDiffExpressedProbes, long resultSetId ) {
        this();
        this.experimentalFactors = experimentalFactors;
        this.qValue = qValue;
        this.threshold = threshold;
        this.numberOfDiffExpressedProbes = numberOfDiffExpressedProbes;
        this.resultSetId = resultSetId;

    }

    public Collection<ExperimentalFactorValueObject> getExperimentalFactors() {
        return experimentalFactors;
    }

    public void setExperimentalFactors( Collection<ExperimentalFactor> experimentalFactors ) {

        this.experimentalFactors = new HashSet<ExperimentalFactorValueObject>();
        ExperimentalFactorValueObject efvo = null;

        for ( ExperimentalFactor eF : experimentalFactors ) {
            efvo = new ExperimentalFactorValueObject( eF );
            this.experimentalFactors.add( efvo );
        }
    }

    public void setExperimentalFactorsByValueObject( Collection<ExperimentalFactorValueObject> experimentalFactors ) {

        this.experimentalFactors = experimentalFactors;
    }

    public Double getQValue() {
        return qValue;
    }

    public void setQValue( Double value ) {
        qValue = value;
    }

    public Double getThreshold() {
        return threshold;
    }

    public void setThreshold( Double threshold ) {
        this.threshold = threshold;
    }

    public int getNumberOfDiffExpressedProbes() {
        return numberOfDiffExpressedProbes;
    }

    public void setNumberOfDiffExpressedProbes( int numberOfDiffExpressedProbes ) {
        this.numberOfDiffExpressedProbes = numberOfDiffExpressedProbes;
    }

    public long getResultSetId() {
        return resultSetId;
    }

    public void setResultSetId( long resultSetId ) {
        this.resultSetId = resultSetId;
    }

}
