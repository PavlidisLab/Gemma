/*
 * The Gemma project
 * 
 * Copyright (c) 2012 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.model.analysis.expression.diff;

import java.io.Serializable;

/**
 * Value object for differential expression result for one result - corresponds to the
 * DifferentialExpressionAnalysisResults for one gene in one ResultSet (combined for multiple probes), but represents
 * only the "selected" analysisResult. It can represent a 'dummy' (missing value) if the resultSetId and the geneId are
 * populated.
 * 
 * @author anton, paul
 * @version $Id$
 */
public class DiffExprGeneSearchResult implements Serializable {

    private static final long serialVersionUID = -6199218806972657112L;
    private Long analysisResultId = null;
    private Long resultSetId = null;
    private Long geneId = null;
    private int numberOfProbes = 0;
    private int numberOfProbesDiffExpressed = 0;
    private Double score = null;
    private Double pvalue = null;
    private Double correctedPvalue = null;

    public DiffExprGeneSearchResult( Long resultSetId, Long geneId ) {
        super();
        this.resultSetId = resultSetId;
        this.geneId = geneId;
    }

    @Override
    public String toString() {
        return "DiffExprGeneSearchResult ["
                + ( analysisResultId != null ? "analysisResultId=" + analysisResultId + ", " : "" )
                + ( geneId != null ? "geneId=" + geneId + ", " : "" ) + ( pvalue != null ? "pvalue=" + pvalue : "" )
                + "]";
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj ) return true;
        if ( obj == null ) return false;
        if ( getClass() != obj.getClass() ) return false;
        DiffExprGeneSearchResult other = ( DiffExprGeneSearchResult ) obj;
        if ( analysisResultId == null || other.analysisResultId == null ) return false;
        return analysisResultId.equals( other.analysisResultId );
    }

    public Double getCorrectedPvalue() {
        return correctedPvalue;
    }

    public long getGeneId() {
        return geneId;
    }

    public int getNumberOfProbes() {
        return numberOfProbes;
    }

    public int getNumberOfProbesDiffExpressed() {
        return numberOfProbesDiffExpressed;
    }

    public Double getPvalue() {
        return pvalue;
    }

    /**
     * @return the id of the underlying DifferentialExpressionAnalysisResult. This will be null if there is no result
     *         for the resultSet and geneId.
     */
    public Long getResultId() {
        return analysisResultId;
    }

    public Long getResultSetId() {
        return resultSetId;
    }

    public Double getScore() {
        return score;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( analysisResultId == null ) ? 0 : analysisResultId.hashCode() );

        return result;
    }

    /**
     * Set the corrected pvalue (i.e., a q-value)
     * 
     * @param correctedPvalue
     */
    public void setCorrectedPvalue( Double correctedPvalue ) {
        this.correctedPvalue = correctedPvalue;
    }

    public void setGeneId( Long geneId ) {
        this.geneId = geneId;
    }

    public void setNumberOfProbes( int numberOfProbes ) {
        this.numberOfProbes = numberOfProbes;
    }

    public void setNumberOfProbesDiffExpressed( int numberOfProbesDiffExpressed ) {
        this.numberOfProbesDiffExpressed = numberOfProbesDiffExpressed;
    }

    /**
     * Set the <em>uncorrected</em> pvalue.
     * 
     * @param pvalue
     */
    public void setPvalue( Double pvalue ) {
        this.pvalue = pvalue;
    }

    /**
     * Not to be confused with resultSetId. This is the ID of the specific result stored.
     * 
     * @param resultId
     */
    public void setResultId( long resultId ) {
        this.analysisResultId = resultId;
    }

    public void setResultSetId( Long resultSetId ) {
        this.resultSetId = resultSetId;
    }

    public void setScore( Double score ) {
        this.score = score;
    }
}