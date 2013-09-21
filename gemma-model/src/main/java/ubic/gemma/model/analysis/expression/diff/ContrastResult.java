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

/**
 * 
 */
public abstract class ContrastResult implements java.io.Serializable {

    /**
     * Constructs new instances of {@link ubic.gemma.model.analysis.expression.diff.ContrastResult}.
     */
    public static final class Factory {
        /**
         * Constructs a new instance of {@link ubic.gemma.model.analysis.expression.diff.ContrastResult}.
         */
        public static ubic.gemma.model.analysis.expression.diff.ContrastResult newInstance() {
            return new ubic.gemma.model.analysis.expression.diff.ContrastResultImpl();
        }

        /**
         * Constructs a new instance of {@link ubic.gemma.model.analysis.expression.diff.ContrastResult}, taking all
         * possible properties (except the identifier(s))as arguments.
         */
        public static ubic.gemma.model.analysis.expression.diff.ContrastResult newInstance( Double pvalue,
                Double tstat, Double coefficient, Double logFoldChange,
                ubic.gemma.model.expression.experiment.FactorValue factorValue,
                ubic.gemma.model.expression.experiment.FactorValue secondFactorValue ) {
            final ubic.gemma.model.analysis.expression.diff.ContrastResult entity = new ubic.gemma.model.analysis.expression.diff.ContrastResultImpl();
            entity.setPvalue( pvalue );
            entity.setTstat( tstat );
            entity.setCoefficient( coefficient );
            entity.setLogFoldChange( logFoldChange );
            entity.setFactorValue( factorValue );
            entity.setSecondFactorValue( secondFactorValue );
            return entity;
        }
    }

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = 5200377592403068104L;
    private Double pvalue;

    private Double tstat;

    private Double coefficient;

    private Double logFoldChange;

    private Long id;

    private ubic.gemma.model.expression.experiment.FactorValue factorValue;

    private ubic.gemma.model.expression.experiment.FactorValue secondFactorValue;

    /**
     * No-arg constructor added to satisfy javabean contract
     * 
     * @author Paul
     */
    public ContrastResult() {
    }

    /**
     * Returns <code>true</code> if the argument is an ContrastResult instance and all identifiers for this entity equal
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
        if ( this.id == null || that.getId() == null || !this.id.equals( that.getId() ) ) {
            return false;
        }
        return true;
    }

    /**
     * <p>
     * The estimated value from the fit
     * </p>
     */
    public Double getCoefficient() {
        return this.coefficient;
    }

    /**
     * <p>
     * The factorValue for the group of samples that is being compared to baseline. The baseline itself is a property of
     * the ResultSet. For factors that have continuous values, this will be null.
     * </p>
     */
    public ubic.gemma.model.expression.experiment.FactorValue getFactorValue() {
        return this.factorValue;
    }

    /**
     * 
     */
    public Long getId() {
        return this.id;
    }

    /**
     * <p>
     * The fold change relative to the baseline, based on the fitted values. log2-transformed. This will be the same as
     * the coefficient if the data were log transformed when analyzed. This might be null if it wasn't computed.
     * </p>
     */
    public Double getLogFoldChange() {
        return this.logFoldChange;
    }

    /**
     * 
     */
    public Double getPvalue() {
        return this.pvalue;
    }

    /**
     * 
     */
    public ubic.gemma.model.expression.experiment.FactorValue getSecondFactorValue() {
        return this.secondFactorValue;
    }

    /**
     * <p>
     * Serves as the effect size as well as an indicator of the direction of change relative to the baseline
     * </p>
     */
    public Double getTstat() {
        return this.tstat;
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

    public void setCoefficient( Double coefficient ) {
        this.coefficient = coefficient;
    }

    public void setFactorValue( ubic.gemma.model.expression.experiment.FactorValue factorValue ) {
        this.factorValue = factorValue;
    }

    public void setId( Long id ) {
        this.id = id;
    }

    public void setLogFoldChange( Double logFoldChange ) {
        this.logFoldChange = logFoldChange;
    }

    public void setPvalue( Double pvalue ) {
        this.pvalue = pvalue;
    }

    public void setSecondFactorValue( ubic.gemma.model.expression.experiment.FactorValue secondFactorValue ) {
        this.secondFactorValue = secondFactorValue;
    }

    public void setTstat( Double tstat ) {
        this.tstat = tstat;
    }

}