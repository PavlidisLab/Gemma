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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.measurement.Measurement;

import javax.persistence.Transient;
import java.io.Serializable;
import java.util.Set;

/**
 * The value for a ExperimentalFactor, representing a specific instance of the factor, such as "10 ug/kg" or "mutant"
 */
@Indexed
public class FactorValue implements Identifiable, Serializable, gemma.gsec.model.SecuredChild {

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = -3783172994360698631L;
    private ExpressionExperiment securityOwner = null;
    /**
     * Use {@link #characteristics} instead.
     */
    @Deprecated
    private String value;
    private Boolean isBaseline;
    private Long id;
    private ExperimentalFactor experimentalFactor;
    private Measurement measurement;
    private Set<Characteristic> characteristics = new java.util.HashSet<>();
    @Deprecated
    private Set<Characteristic> statements = new java.util.HashSet<>();

    /**
     * No-arg constructor added to satisfy javabean contract
     *
     * @author Paul
     */
    public FactorValue() {
    }

    @Override
    public int hashCode() {
        if ( this.getId() != null )
            return this.getId().hashCode();

        HashCodeBuilder builder = new HashCodeBuilder( 17, 7 ).append( this.getExperimentalFactor() ).append( this.getMeasurement() );
        if ( this.getCharacteristics() != null ) {
            for ( Characteristic c : this.getCharacteristics() ) {
                
                if (c == null) {
                    continue;
                }
                
                assert c != null;
                
                builder.append( c.hashCode() );
            }
        }
        return builder.toHashCode();
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

        return this.checkGuts( that );

    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        // this can be null in tests or with half-setup transient objects
        buf.append( "FactorValue " ).append( this.getId() ).append( ": " );

        if ( this.getExperimentalFactor() != null )
            buf.append( this.getExperimentalFactor().getName() ).append( ":" );
        if ( this.getCharacteristics().size() > 0 ) {
            for ( Characteristic c : this.getCharacteristics() ) {
                buf.append( c.getValue() );
                if ( this.getCharacteristics().size() > 1 )
                    buf.append( " | " );
            }
        } else if ( this.getMeasurement() != null ) {
            buf.append( this.getMeasurement().getValue() );
        } else if ( StringUtils.isNotBlank( this.getValue() ) ) {
            buf.append( this.getValue() );
        }
        return buf.toString();
    }

    @Override
    @DocumentId
    public Long getId() {
        return this.id;
    }

    public void setId( Long id ) {
        this.id = id;
    }

    @Transient
    @Override
    public ExpressionExperiment getSecurityOwner() {
        return securityOwner;
    }

    public void setSecurityOwner( ExpressionExperiment ee ) {
        this.securityOwner = ee;
    }

    @IndexedEmbedded
    public Set<Characteristic> getCharacteristics() {
        return this.characteristics;
    }

    public void setCharacteristics( Set<Characteristic> characteristics ) {
        this.characteristics = characteristics;
    }

    public ExperimentalFactor getExperimentalFactor() {
        return this.experimentalFactor;
    }

    public void setExperimentalFactor( ExperimentalFactor experimentalFactor ) {
        this.experimentalFactor = experimentalFactor;
    }

    /**
     * @return True if this is to be considered the baseline condition. This is ignored if the factor is numeric
     *         (non-categorical).
     */
    public Boolean getIsBaseline() {
        return this.isBaseline;
    }

    public void setIsBaseline( Boolean isBaseline ) {
        this.isBaseline = isBaseline;
    }

    public Measurement getMeasurement() {
        return this.measurement;
    }

    public void setMeasurement( ubic.gemma.model.common.measurement.Measurement measurement ) {
        this.measurement = measurement;
    }

    /**
     * @deprecated use {@link #getCharacteristics()} instead.
     */
    @Deprecated
    public String getValue() {
        return this.value;
    }

    /**
     * @deprecated use {@link #setCharacteristics(Set)} ()} instead.
     */
    @Deprecated
    public void setValue( String value ) {
        this.value = value;
    }

    @Deprecated
    public Set<Characteristic> getStatements() {
        return statements;
    }

    @Deprecated
    public void setStatements( Set<Characteristic> statements ) {
        this.statements = statements;
    }

    /**
     * Produce a descriptive string for this factor value.
     */
    @Transient
    public String getDescriptiveString() {
        if ( this.characteristics != null && !this.characteristics.isEmpty() ) {
            StringBuilder fvString = new StringBuilder();
            boolean first = true;
            for ( Characteristic c : this.characteristics ) {
                if ( !first ) {
                    fvString.append( " " );
                }
                fvString.append( StringUtils.strip( c.getValue() ) );
                first = false;
            }
            return fvString.toString();
        } else if ( this.measurement != null ) {
            return StringUtils.strip( this.measurement.getValue() );
        } else if ( StringUtils.isNotBlank( this.value ) ) {
            return StringUtils.strip( this.value );
        }
        return "absent";
    }

    private boolean checkGuts( FactorValue that ) {

        if ( this.getExperimentalFactor() != null ) {
            if ( that.getExperimentalFactor() == null )
                return false;
            if ( !this.getExperimentalFactor().equals( that.getExperimentalFactor() ) ) {
                return false;
            }
        }

        if ( this.getCharacteristics().size() > 0 ) {
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