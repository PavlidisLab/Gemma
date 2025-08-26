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
package ubic.gemma.model.analysis.expression.coexpression;

import ubic.gemma.model.analysis.SingleExperimentAnalysis;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * A coexpression analysis of one experiment. Note that this is used to store meta-data about the analysis, the actual
 * results are not attached to this.
 */
public class CoexpressionAnalysis extends SingleExperimentAnalysis<ExpressionExperiment> {

    private CoexpCorrelationDistribution coexpCorrelationDistribution;
    /**
     * At gene level.
     */
    private Integer numberOfLinks;

    public CoexpressionAnalysis() {
    }

    public CoexpCorrelationDistribution getCoexpCorrelationDistribution() {
        return coexpCorrelationDistribution;
    }

    public void setCoexpCorrelationDistribution( CoexpCorrelationDistribution coexpCorrelationDistribution ) {
        this.coexpCorrelationDistribution = coexpCorrelationDistribution;
    }

    /**
     * @return The number of links which were stored for this analysis.
     */
    public Integer getNumberOfLinks() {
        return this.numberOfLinks;
    }

    /**
     * @param numberOfLinks at the gene level.
     */
    public void setNumberOfLinks( Integer numberOfLinks ) {
        this.numberOfLinks = numberOfLinks;
    }

    @Override
    public boolean equals( Object object ) {
        if ( this == object )
            return true;
        if ( !( object instanceof CoexpressionAnalysis ) )
            return false;
        CoexpressionAnalysis that = ( CoexpressionAnalysis ) object;
        if ( getId() != null && that.getId() != null )
            return getId().equals( that.getId() );
        return false;
    }

    public static final class Factory {
        public static CoexpressionAnalysis newInstance() {
            return new CoexpressionAnalysis();
        }
    }

}