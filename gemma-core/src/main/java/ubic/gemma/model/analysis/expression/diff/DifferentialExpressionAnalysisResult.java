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

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;
import org.springframework.lang.Nullable;
import ubic.gemma.model.analysis.AnalysisResult;
import ubic.gemma.model.expression.designElement.CompositeSequence;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Result of an analysis of differences in expression levels -- a single test (e.g., for one gene or one probe), for one
 * factor. These statistics are based on ANOVA-style analysis, with a collection of ContrastResults storing the
 * associated contrasts.
 * <p>
 * Intentionally no {@code @Cache} on this entity or its {@code contrasts} bag. This is the inner level of the
 * two-level ExpressionAnalysisResultSet -&gt; DifferentialExpressionAnalysisResult chain flagged by
 * HIBERNATE6_CASCADE_AUDIT.md risk #3 (matches the AuditTrail/AuditEvent shape fixed in ab8b4c443c): a read-only L2
 * cache on a {@code mutable="false"} child of a {@code mutable="false"} parent causes Hibernate 6 to serve stale
 * empty-bag results to fresh-session reads after cross-tx writes. {@code @Immutable} is retained because the rows
 * ARE write-once-immutable; do not re-add an L2 cache directive without first re-validating the audit's read-path
 * scenario.
 */
@Entity
@Table(name = "DIFFERENTIAL_EXPRESSION_ANALYSIS_RESULT",
        indexes = @Index(name = "DIFFERENTIAL_EXPRESSION_ANALYSIS_RESULT_CORRECTED_PVALUE",
                columnList = "CORRECTED_PVALUE"))
@Access(AccessType.FIELD)
@Immutable
public class DifferentialExpressionAnalysisResult extends AnalysisResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID", columnDefinition = "BIGINT")
    private Long id;

    @Nullable
    @Column(name = "PVALUE", columnDefinition = "DOUBLE")
    private Double pvalue;

    /**
     * Typically actually a qvalue.
     */
    @Nullable
    @Column(name = "CORRECTED_PVALUE", columnDefinition = "DOUBLE")
    private Double correctedPvalue;

    @Nullable
    @Column(name = "RANK", columnDefinition = "DOUBLE")
    private Double rank;

    @Nullable
    @Column(name = "CORRECTED_P_VALUE_BIN", columnDefinition = "INTEGER")
    private Integer correctedPValueBin;

    // FIXME: use cascade=ALL when https://github.com/PavlidisLab/Gemma/issues/825 is resolved
    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "DIFFERENTIAL_EXPRESSION_ANALYSIS_RESULT_FK", columnDefinition = "BIGINT", nullable = false,
            foreignKey = @ForeignKey(name = "CONTRAST_RESULT_DIFFERENTIAL_EXPRESSION_ANALYSIS_RESULT_FKC"))
    @Immutable
    private Set<ContrastResult> contrasts = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "RESULT_SET_FK", nullable = false, columnDefinition = "BIGINT")
    private ExpressionAnalysisResultSet resultSet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PROBE_FK", nullable = false, columnDefinition = "BIGINT")
    private CompositeSequence probe;

    private static int getBin( Double value ) {
        return ( int ) Math.min( 5, Math.floor( -Math.log10( value ) ) );
    }

    @Override
    public int hashCode() {
        return Objects.hash( getResultSet(), getProbe() );
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj )
            return true;
        if ( obj == null )
            return false;
        if ( !( obj instanceof DifferentialExpressionAnalysisResult ) )
            return false;
        DifferentialExpressionAnalysisResult other = ( DifferentialExpressionAnalysisResult ) obj;
        if ( this.getId() != null && other.getId() != null )
            return this.getId().equals( other.getId() );
        return Objects.equals( getResultSet(), other.getResultSet() )
                && Objects.equals( getProbe(), other.getProbe() );
    }

    @Override
    public String toString() {
        return String.format( "DiffExRes Id=%d Probe Id=%d Pvalue=%g Qvalue=%g Rank=%g",
                this.getId(),
                this.getProbe().getId(),
                this.getPvalue(),
                this.getCorrectedPvalue(),
                this.getRank()
        );
    }

    /**
     * @return Contrasts for this result. Depending on configuration, this might only be stored if the Result itself is
     * significant at some given threshold (e.g., nominal p-value of 0.05) (but default is to store everything)
     */
    public Set<ContrastResult> getContrasts() {
        return this.contrasts;
    }

    public void setContrasts( Set<ContrastResult> contrasts ) {
        this.contrasts = contrasts;
    }

    /**
     * @return A false discovery estimate (qvalue)
     */
    @Nullable
    public Double getCorrectedPvalue() {
        return this.correctedPvalue;
    }

    public void setCorrectedPvalue( @Nullable Double correctedPvalue ) {

        if ( correctedPvalue == null )
            return;

        this.correctedPvalue = correctedPvalue;

        /*
         * See bug 2013. Here we ensure that the bin is always set. The maximum value is 5, representing qvalues better
         * than 10e-5. 0.1-1 -> 0; 0.01-0.099 -> 1; 0.001-0.00999 -> 2; 0.0001- 0.000999 -> 3 etc. Thus "p<0.01" is
         * equivalent to "bin >=2"
         */
        this.setCorrectedPValueBin( DifferentialExpressionAnalysisResult.getBin( correctedPvalue ) );
    }

    /**
     * @return an indexable parameter for the corrected qvalue, to speed searches.
     */
    @Nullable
    public Integer getCorrectedPValueBin() {
        return this.correctedPValueBin;
    }

    public void setCorrectedPValueBin( @Nullable Integer correctedPValueBin ) {
        this.correctedPValueBin = correctedPValueBin;
    }

    @Override
    public Long getId() {
        return this.id;
    }

    public void setId( Long id ) {
        this.id = id;
    }

    public CompositeSequence getProbe() {
        return this.probe;
    }

    public void setProbe( CompositeSequence probe ) {
        this.probe = probe;
    }

    /**
     * @return The p-value from the test for rejection of the null hypothesis of no effect
     */
    @Nullable
    public Double getPvalue() {
        return this.pvalue;
    }

    public void setPvalue( @Nullable Double pvalue ) {
        this.pvalue = pvalue;
    }

    /**
     * @return The fractional rank of this result, relative to the others in the ResultSet. Thus the best (lowest p-value) will
     * have a fractional rank of 0.0, the worst wil lbe 1.0.
     */
    @Nullable
    public Double getRank() {
        return this.rank;
    }

    public void setRank( @Nullable Double rank ) {
        this.rank = rank;
    }

    public ExpressionAnalysisResultSet getResultSet() {
        return this.resultSet;
    }

    public void setResultSet( ExpressionAnalysisResultSet resultSet ) {
        this.resultSet = resultSet;
    }

    public static final class Factory {
        public static DifferentialExpressionAnalysisResult newInstance() {
            return new DifferentialExpressionAnalysisResult();
        }
    }

}
