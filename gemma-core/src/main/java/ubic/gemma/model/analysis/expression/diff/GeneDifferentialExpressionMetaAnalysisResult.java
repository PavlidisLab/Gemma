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

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import ubic.gemma.model.common.AbstractIdentifiable;
import ubic.gemma.model.genome.Gene;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "GENE_DIFFERENTIAL_EXPRESSION_META_ANALYSIS_RESULT")
@SuppressWarnings("unused") // Possible external usage
public class GeneDifferentialExpressionMetaAnalysisResult extends AbstractIdentifiable {

    @Column(name = "META_PVALUE", columnDefinition = "DOUBLE")
    private Double metaPvalue;
    @Column(name = "META_QVALUE", columnDefinition = "DOUBLE")
    private Double metaQvalue;
    @Column(name = "MEAN_LOG_FOLD_CHANGE", columnDefinition = "DOUBLE")
    private Double meanLogFoldChange;
    @Column(name = "META_PVALUE_RANK", columnDefinition = "DOUBLE")
    private Double metaPvalueRank;
    @Column(name = "UPPER_TAIL", nullable = false, columnDefinition = "TINYINT")
    private Boolean upperTail;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "GENE_FK", nullable = false, columnDefinition = "BIGINT")
    private Gene gene;
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "METAANALYSES2RESULTS_USED",
            joinColumns = @JoinColumn(name = "METAANALYSES_FK", columnDefinition = "BIGINT"),
            inverseJoinColumns = @JoinColumn(name = "RESULTS_USED_FK", columnDefinition = "BIGINT"),
            foreignKey = @ForeignKey(name = "DIFFERENTIAL_EXPRESSION_ANALYSIS_RESULT_METAANALYSES_FKC"))
    private Set<DifferentialExpressionAnalysisResult> resultsUsed = new HashSet<>();

    public Gene getGene() {
        return this.gene;
    }

    public void setGene( Gene gene ) {
        this.gene = gene;
    }

    /**
     * @return Note that this value could be misleading; it is possible for the fold change to be positive but the meta-analysis
     * is for down-regulation. Use 'upperTail' to see which direction was inspected.
     */
    public Double getMeanLogFoldChange() {
        return this.meanLogFoldChange;
    }

    public void setMeanLogFoldChange( Double meanLogFoldChange ) {
        this.meanLogFoldChange = meanLogFoldChange;
    }

    public Double getMetaPvalue() {
        return this.metaPvalue;
    }

    public void setMetaPvalue( Double metaPvalue ) {
        this.metaPvalue = metaPvalue;
    }

    /**
     * @return The rank of the gene in the full set of results.
     */
    public Double getMetaPvalueRank() {
        return this.metaPvalueRank;
    }

    public void setMetaPvalueRank( Double metaPvalueRank ) {
        this.metaPvalueRank = metaPvalueRank;
    }

    public Double getMetaQvalue() {
        return this.metaQvalue;
    }

    public void setMetaQvalue( Double metaQvalue ) {
        this.metaQvalue = metaQvalue;
    }

    /**
     * @return The underlying differential expression results that contributed to the meta-analysis result.
     */
    public Set<DifferentialExpressionAnalysisResult> getResultsUsed() {
        return this.resultsUsed;
    }

    public void setResultsUsed( Set<DifferentialExpressionAnalysisResult> resultsUsed ) {
        this.resultsUsed = resultsUsed;
    }

    /**
     * @return If true, indicates the fold change "looked for" was positive (i.e., pvalue measured using the upper tail of the t
     * distribution; the alternative hypothesis is fold change &gt; 0)
     */
    public Boolean getUpperTail() {
        return this.upperTail;
    }

    public void setUpperTail( Boolean upperTail ) {
        this.upperTail = upperTail;
    }

    /**
     * @return a hash code based on this entity's identifiers.
     */
    @Override
    public int hashCode() {
        return Objects.hash( gene );
    }

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
        if ( getId() != null && that.getId() != null ) {
            return getId().equals( that.getId() );
        }
        return false;
    }

    public static final class Factory {
        public static GeneDifferentialExpressionMetaAnalysisResult newInstance() {
            return new GeneDifferentialExpressionMetaAnalysisResult();
        }
    }
}