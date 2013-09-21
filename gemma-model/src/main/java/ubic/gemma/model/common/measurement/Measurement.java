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
package ubic.gemma.model.common.measurement;

/**
 * 
 */
public abstract class Measurement implements java.io.Serializable {

    /**
     * Constructs new instances of {@link ubic.gemma.model.common.measurement.Measurement}.
     */
    public static final class Factory {
        /**
         * Constructs a new instance of {@link ubic.gemma.model.common.measurement.Measurement}.
         */
        public static ubic.gemma.model.common.measurement.Measurement newInstance() {
            return new ubic.gemma.model.common.measurement.MeasurementImpl();
        }

        /**
         * Constructs a new instance of {@link ubic.gemma.model.common.measurement.Measurement}, taking all possible
         * properties (except the identifier(s))as arguments.
         */
        public static ubic.gemma.model.common.measurement.Measurement newInstance(
                ubic.gemma.model.common.measurement.MeasurementType type, String value,
                ubic.gemma.model.common.measurement.MeasurementKind kindCV, String otherKind,
                ubic.gemma.model.common.quantitationtype.PrimitiveType representation,
                ubic.gemma.model.common.measurement.Unit unit ) {
            final ubic.gemma.model.common.measurement.Measurement entity = new ubic.gemma.model.common.measurement.MeasurementImpl();
            entity.setType( type );
            entity.setValue( value );
            entity.setKindCV( kindCV );
            entity.setOtherKind( otherKind );
            entity.setRepresentation( representation );
            entity.setUnit( unit );
            return entity;
        }

        /**
         * Constructs a new instance of {@link ubic.gemma.model.common.measurement.Measurement}, taking all required
         * and/or read-only properties as arguments.
         */
        public static ubic.gemma.model.common.measurement.Measurement newInstance(
                ubic.gemma.model.common.measurement.MeasurementType type, String value,
                ubic.gemma.model.common.quantitationtype.PrimitiveType representation ) {
            final ubic.gemma.model.common.measurement.Measurement entity = new ubic.gemma.model.common.measurement.MeasurementImpl();
            entity.setType( type );
            entity.setValue( value );
            entity.setRepresentation( representation );
            return entity;
        }
    }

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = 1833568047451431226L;
    private ubic.gemma.model.common.measurement.MeasurementType type;

    private String value;

    private ubic.gemma.model.common.measurement.MeasurementKind kindCV;

    private String otherKind;

    private ubic.gemma.model.common.quantitationtype.PrimitiveType representation;

    private Long id;

    private ubic.gemma.model.common.measurement.Unit unit;

    /**
     * No-arg constructor added to satisfy javabean contract
     * 
     * @author Paul
     */
    public Measurement() {
    }

    /**
     * Returns <code>true</code> if the argument is an Measurement instance and all identifiers for this entity equal
     * the identifiers of the argument entity. Returns <code>false</code> otherwise.
     */
    @Override
    public boolean equals( Object object ) {
        if ( this == object ) {
            return true;
        }
        if ( !( object instanceof Measurement ) ) {
            return false;
        }
        final Measurement that = ( Measurement ) object;
        if ( this.id == null || that.getId() == null || !this.id.equals( that.getId() ) ) {
            return false;
        }
        return true;
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
    public ubic.gemma.model.common.measurement.MeasurementKind getKindCV() {
        return this.kindCV;
    }

    /**
     * 
     */
    public String getOtherKind() {
        return this.otherKind;
    }

    /**
     * 
     */
    public ubic.gemma.model.common.quantitationtype.PrimitiveType getRepresentation() {
        return this.representation;
    }

    /**
     * 
     */
    public ubic.gemma.model.common.measurement.MeasurementType getType() {
        return this.type;
    }

    /**
     * 
     */
    public ubic.gemma.model.common.measurement.Unit getUnit() {
        return this.unit;
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

    public void setId( Long id ) {
        this.id = id;
    }

    public void setKindCV( ubic.gemma.model.common.measurement.MeasurementKind kindCV ) {
        this.kindCV = kindCV;
    }

    public void setOtherKind( String otherKind ) {
        this.otherKind = otherKind;
    }

    public void setRepresentation( ubic.gemma.model.common.quantitationtype.PrimitiveType representation ) {
        this.representation = representation;
    }

    public void setType( ubic.gemma.model.common.measurement.MeasurementType type ) {
        this.type = type;
    }

    public void setUnit( ubic.gemma.model.common.measurement.Unit unit ) {
        this.unit = unit;
    }

    public void setValue( String value ) {
        this.value = value;
    }

}