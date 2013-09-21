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

import java.io.Serializable;
import java.util.Collection;

import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.measurement.Measurement;

/**
 * The value for a ExperimentalFactor, representing a specific instance of the factor, such as "10 ug/kg" or "mutant"
 */
public abstract class FactorValue implements Serializable, gemma.gsec.model.SecuredChild {

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

    private ExpressionExperiment securityOwner = null;

    @Override
    public Securable getSecurityOwner() {
        return securityOwner;
    }

    public void setSecurityOwner( ExpressionExperiment ee ) {
        this.securityOwner = ee;
    }

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = -3783172994360698631L;
    private String value;

    private Boolean isBaseline;

    private Long id;

    private ExperimentalFactor experimentalFactor;

    private Measurement measurement;

    private Collection<Characteristic> characteristics = new java.util.HashSet<>();

    /**
     * No-arg constructor added to satisfy javabean contract
     * 
     * @author Paul
     */
    public FactorValue() {
    }

    /**
     * Returns <code>true</code> if the argument is an FactorValue instance and all identifiers for this entity equal
     * the identifiers of the argument entity. Returns <code>false</code> otherwise.
     */
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

    /**
     * 
     */
    public Collection<Characteristic> getCharacteristics() {
        return this.characteristics;
    }

    /**
     * 
     */
    public ExperimentalFactor getExperimentalFactor() {
        return this.experimentalFactor;
    }

    /**
     * 
     */
    @Override
    public Long getId() {
        return this.id;
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

    /**
     * 
     */
    public Measurement getMeasurement() {
        return this.measurement;
    }

    /**
     * 
     */
    public String getValue() {
        return this.value;
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

    public void setCharacteristics( Collection<Characteristic> characteristics ) {
        this.characteristics = characteristics;
    }

    public void setExperimentalFactor( ExperimentalFactor experimentalFactor ) {
        this.experimentalFactor = experimentalFactor;
    }

    public void setId( Long id ) {
        this.id = id;
    }

    public void setIsBaseline( Boolean isBaseline ) {
        this.isBaseline = isBaseline;
    }

    public void setMeasurement( ubic.gemma.model.common.measurement.Measurement measurement ) {
        this.measurement = measurement;
    }

    public void setValue( String value ) {
        this.value = value;
    }

}