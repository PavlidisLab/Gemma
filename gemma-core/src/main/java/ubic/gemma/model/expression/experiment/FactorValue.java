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

import gemma.gsec.model.Securable;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.measurement.Measurement;

import java.io.Serializable;
import java.util.Collection;

/**
 * The value for a ExperimentalFactor, representing a specific instance of the factor, such as "10 ug/kg" or "mutant"
 */
public abstract class FactorValue implements Identifiable, Serializable, gemma.gsec.model.SecuredChild {

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = -3783172994360698631L;
    private ExpressionExperiment securityOwner = null;
    private String value;
    private Boolean isBaseline;
    private Long id;
    private ExperimentalFactor experimentalFactor;
    private Measurement measurement;
    private Collection<Characteristic> characteristics = new java.util.HashSet<>();

    /* ********************************
     * Constructors
     * ********************************/

    /**
     * No-arg constructor added to satisfy javabean contract
     *
     * @author Paul
     */
    public FactorValue() {
    }

    /* ********************************
     * Public methods
     * ********************************/

    @Override
    public boolean equals( Object object ) {
        if ( this == object ) {
            return true;
        }
        if ( !( object instanceof FactorValue ) ) {
            return false;
        }
        final FactorValue that = ( FactorValue ) object;
        if ( this.id == null || that.getId() == null || !this.id.equals( that.getId() ) ) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hashCode = 0;
        hashCode = 29 * hashCode + ( id == null ? 0 : id.hashCode() );

        return hashCode;
    }

    @Override
    public Long getId() {
        return this.id;
    }

    public void setId( Long id ) {
        this.id = id;
    }

    @Override
    public Securable getSecurityOwner() {
        return securityOwner;
    }

    public void setSecurityOwner( ExpressionExperiment ee ) {
        this.securityOwner = ee;
    }

    public Collection<Characteristic> getCharacteristics() {
        return this.characteristics;
    }

    public void setCharacteristics( Collection<Characteristic> characteristics ) {
        this.characteristics = characteristics;
    }

    public ExperimentalFactor getExperimentalFactor() {
        return this.experimentalFactor;
    }

    public void setExperimentalFactor( ExperimentalFactor experimentalFactor ) {
        this.experimentalFactor = experimentalFactor;
    }

    /**
     * <p>
     * True if this is to be considered the baseline condition. This is ignored if the factor is numeric
     * (non-categorical).
     * </p>
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

    public String getValue() {
        return this.value;
    }

    public void setValue( String value ) {
        this.value = value;
    }

    public String getDescriptiveString() {
        if ( this.characteristics != null && this.characteristics.size() > 0 ) {
            StringBuilder fvString = new StringBuilder();
            for ( Characteristic c : this.characteristics ) {
                fvString.append( c.getValue() ).append( " " );
            }
            return fvString.toString();
        } else if ( this.measurement != null ) {
            return this.measurement.getValue();
        } else if ( this.value != null && !this.value.isEmpty() ) {
            return this.value;
        }

        return "absent ";
    }

    /* ********************************
     * Static classes
     * ********************************/

    /**
     * Constructs new instances of {@link FactorValue}.
     */
    public static final class Factory {
        /**
         * Constructs a new instance of {@link FactorValue}.
         */
        public static FactorValue newInstance() {
            return new FactorValueImpl();
        }

        /**
         * Constructs a new instance of {@link FactorValue}, taking all required and/or read-only properties as
         * arguments.
         */
        public static FactorValue newInstance( ExperimentalFactor experimentalFactor ) {
            final FactorValue entity = new FactorValueImpl();
            entity.setExperimentalFactor( experimentalFactor );
            return entity;
        }
    }

}