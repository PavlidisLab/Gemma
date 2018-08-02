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

import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.expression.experiment.FactorValue;

import java.io.Serializable;

/**
 * Represents a contrast between "conditions". In practice, this is the comparison between a factor level and the
 * baseline; for interactions it is the difference of comparisons.
 */
public class ContrastResult implements Identifiable, Serializable {

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = -4310735803120153778L;
    private Double pvalue;
    private Double tStat;
    private Double coefficient;
    private Double logFoldChange;
    private Long id;
    private FactorValue factorValue;
    private FactorValue secondFactorValue;

    /**
     * @return The estimated value from the fit
     */
    public Double getCoefficient() {
        return this.coefficient;
    }

    public void setCoefficient( Double coefficient ) {
        this.coefficient = coefficient;
    }

    /**
     * @return The factorValue for the group of samples that is being compared to baseline. The baseline itself is a property of
     * the ResultSet. For factors that have continuous values, this will be null.
     */
    public FactorValue getFactorValue() {
        return this.factorValue;
    }

    public void setFactorValue( FactorValue factorValue ) {
        this.factorValue = factorValue;
    }

    @Override
    public Long getId() {
        return this.id;
    }

    @SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
    public void setId( Long id ) {
        this.id = id;
    }

    /**
     * @return The fold change relative to the baseline, based on the fitted values. log2-transformed. This will be the same as
     * the coefficient if the data were log transformed when analyzed. This might be null if it wasn't computed.
     */
    public Double getLogFoldChange() {
        return this.logFoldChange;
    }

    public void setLogFoldChange( Double logFoldChange ) {
        this.logFoldChange = logFoldChange;
    }

    public Double getPvalue() {
        return this.pvalue;
    }

    public void setPvalue( Double pvalue ) {
        this.pvalue = pvalue;
    }

    public FactorValue getSecondFactorValue() {
        return this.secondFactorValue;
    }

    public void setSecondFactorValue( FactorValue secondFactorValue ) {
        this.secondFactorValue = secondFactorValue;
    }

    /**
     * @return Serves as the effect size as well as an indicator of the direction of change relative to the baseline
     */
    public Double getTstat() {
        return this.tStat;
    }

    public void setTstat( Double tstat ) {
        this.tStat = tstat;
    }

    /**
     * @return a hash code based on this entity's identifiers.
     */
    @Override
    public int hashCode() {
        int hashCode = 0;
        hashCode = 29 * hashCode + ( id == null ? 0 : id.hashCode() );

        return hashCode;
    }

    /**
     * @return <code>true</code> if the argument is an ContrastResult instance and all identifiers for this entity equal
     * the identifiers of the argument entity. Returns <code>false</code> otherwise.
     */
    @Override
    public boolean equals( Object object ) {
        if ( this == object ) {
            return true;
        }
        if ( !( object instanceof ContrastResult ) ) {
            return false;
        }
        final ContrastResult that = ( ContrastResult ) object;
        return this.id != null && that.getId() != null && this.id.equals( that.getId() );
    }

    @Override
    public String toString() {
        return "Contrast for " + this.getFactorValue() == null ? "[continuous]" : this.getFactorValue().toString();
    }

    public static final class Factory {
        public static ContrastResult newInstance() {
            return new ContrastResult();
        }

        @SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
        public static ContrastResult newInstance( Double pvalue, Double tstat, Double coefficient, Double logFoldChange,
                FactorValue factorValue, FactorValue secondFactorValue ) {
            final ContrastResult entity = new ContrastResult();
            entity.setPvalue( pvalue );
            entity.setTstat( tstat );
            entity.setCoefficient( coefficient );
            entity.setLogFoldChange( logFoldChange );
            entity.setFactorValue( factorValue );
            entity.setSecondFactorValue( secondFactorValue );
            return entity;
        }
    }

}