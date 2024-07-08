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

import gemma.gsec.model.SecuredChild;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.measurement.Measurement;

import javax.annotation.Nullable;
import javax.persistence.Transient;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The value for a ExperimentalFactor, representing a specific instance of the factor, such as "10 ug/kg" or "mutant"
 */
@Indexed
public class FactorValue implements Identifiable, SecuredChild, Serializable {

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = -3783172994360698631L;

    private Long id;
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

    private boolean needsAttention;

    private ExpressionExperiment securityOwner = null;

    /**
     * No-arg constructor added to satisfy javabean contract
     *
     * @author Paul
     */
    public FactorValue() {
    }

    @Override
    @DocumentId
    public Long getId() {
        return this.id;
    }

    public void setId( Long id ) {
        this.id = id;
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
        if ( object == null )
            return false;
        if ( this == object )
            return true;
        if ( !( object instanceof FactorValue ) )
            return false;
        FactorValue that = ( FactorValue ) object;
        if ( this.getId() != null && that.getId() != null )
            return this.getId().equals( that.getId() );

        if ( that.getId() == null && this.getId() != null )
            return false;

        /*
         * at this point, we know we have two FactorValues, at least one of which is transient, so we have to look at
         * the fields; pain in butt
         */

        if ( this.getExperimentalFactor() != null ) {
            if ( that.getExperimentalFactor() == null )
                return false;
            if ( !this.getExperimentalFactor().equals( that.getExperimentalFactor() ) ) {
                return false;
            }
        }

        if ( !this.getCharacteristics().isEmpty() ) {
            if ( that.getCharacteristics().size() != this.getCharacteristics().size() )
                return false;

            for ( Characteristic c : this.getCharacteristics() ) {
                boolean match = false;
                for ( Characteristic c2 : that.getCharacteristics() ) {
                    if ( c.equals( c2 ) ) {
                        if ( match ) {
                            return false;
                        }
                        match = true;
                    }
                }
                if ( !match )
                    return false;
            }

        }

        if ( this.getMeasurement() != null ) {
            if ( that.getMeasurement() == null )
                return false;
            if ( !this.getMeasurement().equals( that.getMeasurement() ) )
                return false;
        }

        if ( this.getValue() != null ) {
            return that.getValue() != null && this.getValue().equals( that.getValue() );
        }

        // everything is empty...
        return true;
    }

    @Override
    public String toString() {
        return String.format( "FactorValue%s%s%s%s%s%s",
                id != null ? " Id=" + id : "",
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
    }

}