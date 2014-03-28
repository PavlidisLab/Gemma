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

/**
 * A coexpression analysis of one experiment. Note that this is used to store meta-data about the analysis, the actual
 * results are not attached to this.
 */
public abstract class CoexpressionAnalysis extends SingleExperimentAnalysis {

    private static final long serialVersionUID = -2036918876881877628L;

    /**
     * Constructs new instances of {@link CoexpressionAnalysis}.
     */
    public static final class Factory {
        /**
         * Constructs a new instance of {@link CoexpressionAnalysis}.
         */
        public static CoexpressionAnalysis newInstance() {
            return new CoexpressionAnalysisImpl();
        }
    }

    private CoexpCorrelationDistribution coexpCorrelationDistribution;

    /**
     * At gene level.
     */
    private Integer numberOfLinks;

    public CoexpCorrelationDistribution getCoexpCorrelationDistribution() {
        return coexpCorrelationDistribution;
    }

    /**
     * The number of links which were stored for this analysis.
     */
    public Integer getNumberOfLinks() {
        return this.numberOfLinks;
    }

    /**
     * @param coexpCorrelationDistribution
     */
    public void setCoexpCorrelationDistribution( CoexpCorrelationDistribution coexpCorrelationDistribution ) {
        this.coexpCorrelationDistribution = coexpCorrelationDistribution;
    }

    /**
     * @param numberOfLinks at the gene level.
     */
    public void setNumberOfLinks( Integer numberOfLinks ) {
        this.numberOfLinks = numberOfLinks;
    }

}