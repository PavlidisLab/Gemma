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

import java.util.Collection;

/**
 * A coexpression analysis at the level of probes
 */
public abstract class ProbeCoexpressionAnalysis extends ubic.gemma.model.analysis.SingleExperimentAnalysisImpl {

    /**
     * Constructs new instances of {@link ubic.gemma.model.analysis.expression.coexpression.ProbeCoexpressionAnalysis}.
     */
    public static final class Factory {
        /**
         * Constructs a new instance of
         * {@link ubic.gemma.model.analysis.expression.coexpression.ProbeCoexpressionAnalysis}.
         */
        public static ubic.gemma.model.analysis.expression.coexpression.ProbeCoexpressionAnalysis newInstance() {
            return new ubic.gemma.model.analysis.expression.coexpression.ProbeCoexpressionAnalysisImpl();
        }

    }

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = 1210363957474375117L;
    private Integer numberOfLinks;

    private Collection<CoexpressionProbe> probesUsed = new java.util.HashSet<>();

    /**
     * No-arg constructor added to satisfy javabean contract
     * 
     * @author Paul
     */
    public ProbeCoexpressionAnalysis() {
    }

    /**
     * The number of links which were stored for this analysis.
     */
    public Integer getNumberOfLinks() {
        return this.numberOfLinks;
    }

    /**
     * Probes that were not filtered based on initial filtering criteria. Not all probes used have links retained.
     */
    public Collection<ubic.gemma.model.analysis.expression.coexpression.CoexpressionProbe> getProbesUsed() {
        return this.probesUsed;
    }

    public void setNumberOfLinks( Integer numberOfLinks ) {
        this.numberOfLinks = numberOfLinks;
    }

    public void setProbesUsed(
            Collection<ubic.gemma.model.analysis.expression.coexpression.CoexpressionProbe> probesUsed ) {
        this.probesUsed = probesUsed;
    }

}