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

import ubic.gemma.model.common.quantitationtype.PrimitiveType;

/**
 * 
 */
public abstract class Measurement implements java.io.Serializable {

    /**
     * Constructs new instances of {@link Measurement}.
     */
    public static final class Factory {
        /**
         * Constructs a new instance of {@link Measurement}.
         */
        public static Measurement newInstance() {
            return new MeasurementImpl();
        }

        /**
         * Constructs a new instance of {@link Measurement}, taking all possible
         * properties (except the identifier(s))as arguments.
         */
        public static Measurement newInstance(
                MeasurementType type, String value,
                MeasurementKind kindCV, String otherKind,
                PrimitiveType representation,
                Unit unit ) {
            final Measurement entity = new MeasurementImpl();
            entity.setType( type );
            entity.setValue( value );
            entity.setKindCV( kindCV );
            entity.setOtherKind( otherKind );
            entity.setRepresentation( representation );
            entity.setUnit( unit );
            return entity;
        }

        /**
         * Constructs a new instance of {@link Measurement}, taking all required
         * and/or read-only properties as arguments.
         */
        public static Measurement newInstance(
                MeasurementType type, String value,
                PrimitiveType representation ) {
            final Measurement entity = new MeasurementImpl();
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
    private MeasurementType type;

    private String value;

    private MeasurementKind kindCV;

    private String otherKind;

    private PrimitiveType representation;

    private Long id;

    private Unit unit;

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
    public MeasurementKind getKindCV() {
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
    public PrimitiveType getRepresentation() {
        return this.representation;
    }

    /**
     * 
     */
    public MeasurementType getType() {
        return this.type;
    }

    /**
     * 
     */
    public Unit getUnit() {
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

    public void setKindCV( MeasurementKind kindCV ) {
        this.kindCV = kindCV;
    }

    public void setOtherKind( String otherKind ) {
        this.otherKind = otherKind;
    }

    public void setRepresentation( PrimitiveType representation ) {
        this.representation = representation;
    }

    public void setType( MeasurementType type ) {
        this.type = type;
    }

    public void setUnit( Unit unit ) {
        this.unit = unit;
    }

    public void setValue( String value ) {
        this.value = value;
    }

}