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
package ubic.gemma.model.analysis.expression.diff;

import java.util.Collection;

import ubic.gemma.model.genome.Gene;

/**
 * 
 */
public abstract class GeneDifferentialExpressionMetaAnalysisResult implements java.io.Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 4971245573216792849L;

    /**
     * Constructs new instances of {@link GeneDifferentialExpressionMetaAnalysisResult}.
     */
    public static final class Factory {
        /**
         * Constructs a new instance of {@link GeneDifferentialExpressionMetaAnalysisResult}.
         */
        public static GeneDifferentialExpressionMetaAnalysisResult newInstance() {
            return new GeneDifferentialExpressionMetaAnalysisResultImpl();
        }

    }

    private Double metaPvalue;

    private Double metaQvalue;

    private Double meanLogFoldChange;

    private Double metaPvalueRank;

    private Boolean upperTail;

    private Long id;

    private Gene gene;

    private Collection<DifferentialExpressionAnalysisResult> resultsUsed = new java.util.HashSet<>();

    /**
     * Returns <code>true</code> if the argument is an GeneDifferentialExpressionMetaAnalysisResult instance and all
     * identifiers for this entity equal the identifiers of the argument entity. Returns <code>false</code> otherwise.
     */
    @Override
    public boolean equals( Object object ) {
        if ( this == object ) {
            return true;
        }
        if ( !( object instanceof GeneDifferentialExpressionMetaAnalysisResult ) ) {
            return false;
        }
        final GeneDifferentialExpressionMetaAnalysisResult that = ( GeneDifferentialExpressionMetaAnalysisResult ) object;
        if ( this.id == null || that.getId() == null || !this.id.equals( that.getId() ) ) {
            return false;
        }
        return true;
    }

    /**
     * 
     */
    public Gene getGene() {
        return this.gene;
    }

    /**
     * 
     */
    public Long getId() {
        return this.id;
    }

    /**
     * Note that this value could be misleading; it is possible for the fold change to be positive but the meta-analysis
     * is for down-regulation. Use 'upperTail' to see which direction was inspected.
     */
    public Double getMeanLogFoldChange() {
        return this.meanLogFoldChange;
    }

    /**
     * 
     */
    public Double getMetaPvalue() {
        return this.metaPvalue;
    }

    /**
     * The rank of the gene in the full set of results.
     */
    public Double getMetaPvalueRank() {
        return this.metaPvalueRank;
    }

    /**
     * 
     */
    public Double getMetaQvalue() {
        return this.metaQvalue;
    }

    /**
     * The underlying differential expression results that contributed to the meta-analysis result.
     */
    public Collection<DifferentialExpressionAnalysisResult> getResultsUsed() {
        return this.resultsUsed;
    }

    /**
     * If true, indicates the fold change "looked for" was positive (i.e., pvalue measured using the upper tail of the t
     * distribution; the alternative hypothesis is fold change > 0)
     */
    public Boolean getUpperTail() {
        return this.upperTail;
    }

    /**
     * Returns a hash code based on this entity's identifiers.
     */
    @Override
    public int hashCode() {
        int hashCode = 0;
        hashCode = 29 * hashCode + ( id == null ? 0 : id.hashCode() );

        return hashCode;
    }

    public void setGene( Gene gene ) {
        this.gene = gene;
    }

    public void setId( Long id ) {
        this.id = id;
    }

    public void setMeanLogFoldChange( Double meanLogFoldChange ) {
        this.meanLogFoldChange = meanLogFoldChange;
    }

    public void setMetaPvalue( Double metaPvalue ) {
        this.metaPvalue = metaPvalue;
    }

    public void setMetaPvalueRank( Double metaPvalueRank ) {
        this.metaPvalueRank = metaPvalueRank;
    }

    public void setMetaQvalue( Double metaQvalue ) {
        this.metaQvalue = metaQvalue;
    }

    public void setResultsUsed( Collection<DifferentialExpressionAnalysisResult> resultsUsed ) {
        this.resultsUsed = resultsUsed;
    }

    public void setUpperTail( Boolean upperTail ) {
        this.upperTail = upperTail;
    }

}