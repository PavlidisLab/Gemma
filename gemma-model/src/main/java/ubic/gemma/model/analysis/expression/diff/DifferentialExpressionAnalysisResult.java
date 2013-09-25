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

import java.io.Serializable;
import java.util.Collection;

import ubic.gemma.model.expression.designElement.CompositeSequence;

/**
 * Result of an analysis of differences in expression levels -- a single test (e.g., for one gene or one probe).
 */
public abstract class DifferentialExpressionAnalysisResult implements Serializable {

    /**
     * Constructs new instances of {@link DifferentialExpressionAnalysisResult}.
     */
    public static final class Factory {
        /**
         * Constructs a new instance of {@link DifferentialExpressionAnalysisResult}.
         */
        public static DifferentialExpressionAnalysisResult newInstance() {
            return new DifferentialExpressionAnalysisResultImpl();
        }

    }

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = 4986999013709498648L;
    private Double pvalue;

    private Double correctedPvalue;

    private Double rank;

    private Integer correctedPValueBin;

    private Long id;

    private Collection<ContrastResult> contrasts = new java.util.HashSet<>();

    private ExpressionAnalysisResultSet resultSet;

    private CompositeSequence probe;

    /**
     * No-arg constructor added to satisfy javabean contract
     * 
     * @author Paul
     */
    public DifferentialExpressionAnalysisResult() {
    }

    /**
     * Returns <code>true</code> if the argument is an DifferentialExpressionAnalysisResult instance and all identifiers
     * for this entity equal the identifiers of the argument entity. Returns <code>false</code> otherwise.
     */
    @Override
    public boolean equals( Object object ) {
        if ( this == object ) {
            return true;
        }
        if ( !( object instanceof DifferentialExpressionAnalysisResult ) ) {
            return false;
        }
        final DifferentialExpressionAnalysisResult that = ( DifferentialExpressionAnalysisResult ) object;
        if ( this.id == null || that.getId() == null || !this.id.equals( that.getId() ) ) {
            return false;
        }
        return true;
    }

    /**
     * Contrasts for this result. These might only be stored if the Result itself is significant at some given threshold
     * (e.g., nominal p-value of 0.05)
     */
    public Collection<ContrastResult> getContrasts() {
        return this.contrasts;
    }

    /**
     * A false discovery estimate (qvalue), Bonferroni-corrected pvalue or other corrected pvalue. The details of how
     * this was computed would be found in the protocol.
     */
    public Double getCorrectedPvalue() {
        return this.correctedPvalue;
    }

    /**
     * <p>
     * Gives an indexable parameter for the corrected qvalue, to speed searches.
     * </p>
     */
    public Integer getCorrectedPValueBin() {
        return this.correctedPValueBin;
    }

    /**
     * 
     */
    public Long getId() {
        return this.id;
    }

    /**
     * 
     */
    public ubic.gemma.model.expression.designElement.CompositeSequence getProbe() {
        return this.probe;
    }

    /**
     * <p>
     * The p-value from the test for rejection of the null hypothesis of no effect
     * </p>
     */
    public Double getPvalue() {
        return this.pvalue;
    }

    /**
     * <p>
     * The fractional rank of this result, relative to the others in the ResultSet. Thus the best (lowest p-value) will
     * have a fractional rank of 0.0, the worst wil lbe 1.0.
     * </p>
     */
    public Double getRank() {
        return this.rank;
    }

    /**
     * 
     */
    public ExpressionAnalysisResultSet getResultSet() {
        return this.resultSet;
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

    public void setContrasts( Collection<ContrastResult> contrasts ) {
        this.contrasts = contrasts;
    }

    public void setCorrectedPvalue( Double correctedPvalue ) {
        this.correctedPvalue = correctedPvalue;
    }

    public void setCorrectedPValueBin( Integer correctedPValueBin ) {
        this.correctedPValueBin = correctedPValueBin;
    }

    public void setId( Long id ) {
        this.id = id;
    }

    public void setProbe( ubic.gemma.model.expression.designElement.CompositeSequence probe ) {
        this.probe = probe;
    }

    public void setPvalue( Double pvalue ) {
        this.pvalue = pvalue;
    }

    public void setRank( Double rank ) {
        this.rank = rank;
    }

    public void setResultSet( ExpressionAnalysisResultSet resultSet ) {
        this.resultSet = resultSet;
    }

}