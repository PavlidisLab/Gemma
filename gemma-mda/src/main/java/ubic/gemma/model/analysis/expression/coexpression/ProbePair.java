/*
 * The Gemma project Copyright (c) 2008 University of British Columbia Licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing permissions and limitations
 * under the License.
 */
package ubic.gemma.model.analysis.expression.coexpression;

import java.io.Serializable;

/**
 * Lightweight object to store very raw information about probe links.
 * 
 * @author paul
 * @version $Id$
 */
public class ProbePair implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private Long queryProbeId;
    private Long targetProbeId;
    private double score;
    private double pvalue;

    public ProbePair( Long queryProbeId, Long targetProbeId, double score, double pvalue ) {
        super();
        this.queryProbeId = queryProbeId;
        this.targetProbeId = targetProbeId;
        this.score = score;
        this.pvalue = pvalue;
    }

    /**
     * @return the pvalue
     */
    protected double getPvalue() {
        return pvalue;
    }

    /**
     * @return the queryProbeId
     */
    protected Long getQueryProbeId() {
        return queryProbeId;
    }

    /**
     * @return the score
     */
    protected double getScore() {
        return score;
    }

    /**
     * @return the targetProbeId
     */
    protected Long getTargetProbeId() {
        return targetProbeId;
    }

    /**
     * @param pvalue the pvalue to set
     */
    protected void setPvalue( double pvalue ) {
        this.pvalue = pvalue;
    }

    /**
     * @param queryProbeId the queryProbeId to set
     */
    protected void setQueryProbeId( Long queryProbeId ) {
        this.queryProbeId = queryProbeId;
    }

    /**
     * @param score the score to set
     */
    protected void setScore( double score ) {
        this.score = score;
    }

    /**
     * @param targetProbeId the targetProbeId to set
     */
    protected void setTargetProbeId( Long targetProbeId ) {
        this.targetProbeId = targetProbeId;
    }

}
