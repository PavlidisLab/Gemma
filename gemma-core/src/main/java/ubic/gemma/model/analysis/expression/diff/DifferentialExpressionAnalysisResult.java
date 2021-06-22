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

import ubic.gemma.model.analysis.AnalysisResult;
import ubic.gemma.model.expression.designElement.CompositeSequence;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;

/**
 * Result of an analysis of differences in expression levels -- a single test (e.g., for one gene or one probe), for one
 * factor. These statistics are based on ANOVA-style analysis, with a collection of ContrastResults storing the
 * associated contrasts.
 */
@SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
public class DifferentialExpressionAnalysisResult extends AnalysisResult implements Serializable {
    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = 8952834115689524169L;
    private Double pvalue;
    /**
     * Typically actually a qvalue.
     */
    private Double correctedPvalue;
    private Double rank;
    private Integer correctedPValueBin;
    private Long id;
    private Collection<ContrastResult> contrasts = new HashSet<>();
    private ExpressionAnalysisResultSet resultSet;
    private CompositeSequence probe;

    /**
     * No-arg constructor added to satisfy javabean contract
     */
    public DifferentialExpressionAnalysisResult() {
    }

    private static int getBin( Double value ) {
        return ( int ) Math.min( 5, Math.floor( -Math.log10( value ) ) );
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( this.getId() == null ) ? 0 : this.getId().hashCode() );

        if ( this.getId() == null ) {
            result = prime * result + ( ( this.getResultSet() == null ) ? 0 : this.getResultSet().hashCode() );
            result = prime * result + ( ( this.getProbe() == null ) ? 0 : this.getProbe().hashCode() );
        }
        return result;
    }

    @SuppressWarnings("SimplifiableIfStatement") // Better readability
    @Override
    public boolean equals( Object obj ) {
        if ( this == obj )
            return true;
        if ( obj == null )
            return false;
        if ( this.getClass() != obj.getClass() )
            return false;
        DifferentialExpressionAnalysisResult other = ( DifferentialExpressionAnalysisResult ) obj;

        if ( this.getId() == null ) {
            if ( other.getId() != null )
                return false;
        } else if ( !this.getId().equals( other.getId() ) ) {
            return false;
        } else {
            return this.getId().equals( other.getId() );
        }

        // fallback.
        if ( this.getResultSet() == null ) {
            if ( other.getResultSet() != null )
                return false;
        } else if ( !this.getResultSet().equals( other.getResultSet() ) )
            return false;
        if ( this.getProbe() == null ) {
            return other.getProbe() == null;
        } else
            return this.getProbe().equals( other.getProbe() );
    }

    @Override
    public String toString() {
        return "DiffExRes[" + this.getId() + "]: " + this.getProbe() + " p=" + String.format( "%g", this.getPvalue() )
                + " ressetId=" + ( this.getResultSet() == null ? "" : this.getResultSet().getId() );
    }

    /**
     * @return Contrasts for this result. Depending on configuration, this might only be stored if the Result itself is
     * significant at some given threshold (e.g., nominal p-value of 0.05) (but default is to store everything)
     */
    public Collection<ContrastResult> getContrasts() {
        return this.contrasts;
    }

    public void setContrasts( Collection<ContrastResult> contrasts ) {
        this.contrasts = contrasts;
    }

    /**
     * @return A false discovery estimate (qvalue)
     */
    public Double getCorrectedPvalue() {
        return this.correctedPvalue;
    }

    public void setCorrectedPvalue( Double correctedPvalue ) {

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
    public Integer getCorrectedPValueBin() {
        return this.correctedPValueBin;
    }

    public void setCorrectedPValueBin( Integer correctedPValueBin ) {
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
    public Double getPvalue() {
        return this.pvalue;
    }

    public void setPvalue( Double pvalue ) {
        this.pvalue = pvalue;
    }

    /**
     * @return The fractional rank of this result, relative to the others in the ResultSet. Thus the best (lowest p-value) will
     * have a fractional rank of 0.0, the worst wil lbe 1.0.
     */
    public Double getRank() {
        return this.rank;
    }

    public void setRank( Double rank ) {
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