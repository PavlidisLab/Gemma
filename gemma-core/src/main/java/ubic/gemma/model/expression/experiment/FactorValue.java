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
package ubic.gemma.model.expression.experiment;

import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.springframework.util.Assert;
import ubic.gemma.core.analysis.expression.diff.BaselineSelection;
import ubic.gemma.model.common.AbstractIdentifiable;
import ubic.gemma.model.common.auditAndSecurity.SecuredChild;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.measurement.Measurement;
import ubic.gemma.persistence.util.IdentifiableUtils;

import javax.annotation.Nullable;
import javax.persistence.Transient;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The value for a ExperimentalFactor, representing a specific instance of the factor, such as "10 ug/kg" or "mutant"
 */
@Indexed
public class FactorValue extends AbstractIdentifiable implements SecuredChild {

    /**
     * Comparator for factor values.
     * <p>
     * This comparator assumes that two FVs belong to the same factor.
     */
    public static Comparator<FactorValue> COMPARATOR = Comparator
            .comparing( FactorValue::getOrdering, Comparator.nullsLast( Comparator.naturalOrder() ) )
            .thenComparing( FactorValue::getMeasurement, Comparator.nullsLast( Measurement.COMPARATOR ) )
            // try to put baseline first, this includes checking for the FactorValue.isBaseline field
            .thenComparing( BaselineSelection::isBaselineCondition, Comparator.nullsLast( Comparator.reverseOrder() ) )
            .thenComparing( ( a, b ) -> {
                if ( a.getCharacteristics().size() == 1 && b.getCharacteristics().size() == 1 ) {
                    // singleton statements
                    return a.getCharacteristics().iterator().next().compareTo( b.getCharacteristics().iterator().next() );
                } else {
                    return 0;
                }
            } )
            // favour simpler FVs
            .thenComparing( FactorValue::getCharacteristics, Comparator.comparingInt( Set::size ) )
            // deprecated value, but last resort
            .thenComparing( FactorValue::getValue, Comparator.nullsLast( String.CASE_INSENSITIVE_ORDER ) );

    private ExperimentalFactor experimentalFactor;
    @Nullable
    @Deprecated
    private String value;
    @Nullable
    private Boolean isBaseline;
    @Nullable
    private Measurement measurement;
    private Set<Statement> characteristics = new HashSet<>();
    @Deprecated
    private Set<Characteristic> oldStyleCharacteristics = new HashSet<>();
    /**
     * If this is an ordinal factor, the ordering of this value among others.
     * <p>
     * This takes precedence over any other ordering.
     */
    @Nullable
    private Integer ordering;

    private boolean needsAttention;

    private ExpressionExperiment securityOwner = null;

    @Override
    @DocumentId
    public Long getId() {
        return super.getId();
    }

    public ExperimentalFactor getExperimentalFactor() {
        return this.experimentalFactor;
    }

    public void setExperimentalFactor( ExperimentalFactor experimentalFactor ) {
        this.experimentalFactor = experimentalFactor;
    }

    /**
     * Indicate if this factor value is a "forced" baseline or non-baseline condition.
     * <p>
     * This is ignored if the factor is continuous.
     */
    @Nullable
    public Boolean getIsBaseline() {
        return this.isBaseline;
    }

    public void setIsBaseline( @Nullable Boolean isBaseline ) {
        this.isBaseline = isBaseline;
    }

    /**
     * @deprecated use {@link #getMeasurement()} or {@link #getCharacteristics()} to retrieve the value.
     */
    @Nullable
    @Deprecated
    public String getValue() {
        return this.value;
    }

    @Deprecated
    public void setValue( @Nullable String value ) {
        this.value = value;
    }

    /**
     * If this is a continuous factor, a measurement representing its value.
     */
    @Nullable
    public Measurement getMeasurement() {
        return this.measurement;
    }

    public void setMeasurement( @Nullable Measurement measurement ) {
        this.measurement = measurement;
    }

    /**
     * Collection of {@link Statement} describing this factor value.
     */
    @IndexedEmbedded
    public Set<Statement> getCharacteristics() {
        return this.characteristics;
    }

    public void setCharacteristics( Set<Statement> characteristics ) {
        this.characteristics = characteristics;
    }

    /**
     * Old-style characteristics from the 1.30 series.
     * <p>
     * This will be removed when all the characteristics are ported to the new style using {@link Statement}.
     */
    @Deprecated
    public Set<Characteristic> getOldStyleCharacteristics() {
        return oldStyleCharacteristics;
    }

    @Deprecated
    public void setOldStyleCharacteristics( Set<Characteristic> oldCharacteristics ) {
        this.oldStyleCharacteristics = oldCharacteristics;
    }

    /**
     * Indicate if this factor value needs attention.
     * <p>
     * If this is the case, there might be a {@link ubic.gemma.model.common.auditAndSecurity.eventType.FactorValueNeedsAttentionEvent}
     * event attached to the owning {@link ExpressionExperiment} with additional details.
     */
    public boolean getNeedsAttention() {
        return needsAttention;
    }

    public void setNeedsAttention( boolean troubled ) {
        this.needsAttention = troubled;
    }

    @Nullable
    public Integer getOrdering() {
        return ordering;
    }

    public void setOrdering( @Nullable Integer ordering ) {
        this.ordering = ordering;
    }

    @Transient
    @Override
    public ExpressionExperiment getSecurityOwner() {
        return securityOwner;
    }

    public void setSecurityOwner( ExpressionExperiment ee ) {
        this.securityOwner = ee;
    }

    @Override
    public int hashCode() {
        // experimentalFactor is lazy-loaded, so it cannot be used in the hashCode() implementation
        return Objects.hash( getMeasurement(), getCharacteristics() );
    }

    @Override
    public boolean equals( Object object ) {
        if ( this == object )
            return true;
        if ( !( object instanceof FactorValue ) )
            return false;
        FactorValue that = ( FactorValue ) object;
        if ( this.getId() != null && that.getId() != null )
            return this.getId().equals( that.getId() );
        /*
         * at this point, we know we have two FactorValues, at least one of which is transient, so we have to look at
         * the fields; pain in butt
         */
        return IdentifiableUtils.equals( getExperimentalFactor(), that.getExperimentalFactor() )
                && Objects.equals( getMeasurement(), that.getMeasurement() )
                && Objects.equals( getCharacteristics(), that.getCharacteristics() )
                && Objects.equals( getValue(), that.getValue() );
    }

    @Override
    public String toString() {
        return String.format( "FactorValue%s%s%s%s%s%s",
                getId() != null ? " Id=" + getId() : "",
                value != null ? " Value=" + value : "",
                measurement != null ? " Measurement=" + measurement : "",
                !characteristics.isEmpty() ? " Characteristics=[" + characteristics.stream().sorted().map( Statement::toString ).collect( Collectors.joining( ", " ) ) + "]" : "",
                isBaseline != null ? " Baseline=" + isBaseline : "",
                needsAttention ? " [Needs Attention]" : ""
        );
    }

    public static final class Factory {

        public static FactorValue newInstance() {
            return new FactorValue();
        }

        public static FactorValue newInstance( ExperimentalFactor experimentalFactor ) {
            final FactorValue entity = new FactorValue();
            entity.setExperimentalFactor( experimentalFactor );
            return entity;
        }

        /**
         * Create a FactorValue with a single characteristic.
         */
        public static FactorValue newInstance( ExperimentalFactor factor, Characteristic c ) {
            return newInstance( factor, c instanceof Statement ? ( Statement ) c : Statement.Factory.newInstance( c ) );
        }

        /**
         * Create a FactorValue with a single statement.
         */
        public static FactorValue newInstance( ExperimentalFactor factor, Statement c ) {
            Assert.isTrue( factor.getType() == FactorType.CATEGORICAL,
                    "Only categorical factors can be created with a single characteristic." );
            FactorValue entity = newInstance( factor );
            entity.getCharacteristics().add( c );
            return entity;
        }


        /**
         * Create a FactorValue with a measurement.
         */
        public static FactorValue newInstance( ExperimentalFactor factor, Measurement measurement ) {
            Assert.isTrue( factor.getType() == FactorType.CONTINUOUS, "Only continuous factors can have a measurement." );
            FactorValue entity = newInstance( factor );
            entity.setMeasurement( measurement );
            return entity;
        }

        /**
         * Create a factor value with a value.
         *
         * @deprecated this is deprecated, create a factor with a single characteristic instead with
         * {@link #newInstance(ExperimentalFactor, Characteristic)}
         */
        @Deprecated
        public static FactorValue newInstance( ExperimentalFactor factor, String value ) {
            FactorValue entity = newInstance( factor );
            entity.setValue( value );
            return entity;
        }
    }

}