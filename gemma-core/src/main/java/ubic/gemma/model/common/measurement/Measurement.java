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

import ubic.gemma.model.common.AbstractIdentifiable;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;

import javax.annotation.Nullable;
import javax.persistence.Transient;
import java.util.Objects;

public class Measurement extends AbstractIdentifiable {

    private MeasurementType type;
    /**
     * The measurement value.
     * <p>
     * Null indicates a missing value.
     */
    @Nullable
    private String value;
    private MeasurementKind kindCV;
    private String otherKind;
    private PrimitiveType representation;
    private Unit unit;

    public MeasurementKind getKindCV() {
        return this.kindCV;
    }

    public void setKindCV( MeasurementKind kindCV ) {
        this.kindCV = kindCV;
    }

    public String getOtherKind() {
        return this.otherKind;
    }

    public void setOtherKind( String otherKind ) {
        this.otherKind = otherKind;
    }

    public PrimitiveType getRepresentation() {
        return this.representation;
    }

    public void setRepresentation( PrimitiveType representation ) {
        this.representation = representation;
    }

    public MeasurementType getType() {
        return this.type;
    }

    public void setType( MeasurementType type ) {
        this.type = type;
    }

    public Unit getUnit() {
        return this.unit;
    }

    public void setUnit( Unit unit ) {
        this.unit = unit;
    }

    @Nullable
    public String getValue() {
        return this.value;
    }

    public void setValue( @Nullable String value ) {
        this.value = value;
    }

    @Transient
    public int getValueAsInt() {
        ensureRepresentation( PrimitiveType.INT );
        return value != null ? Integer.parseInt( value ) : 0;
    }

    @Transient
    public long getValueAsLong() {
        ensureRepresentation( PrimitiveType.LONG );
        return value != null ? Long.parseLong( value ) : 0L;
    }

    @Transient
    public float getValueAsFloat() {
        ensureRepresentation( PrimitiveType.FLOAT );
        return value != null ? Float.parseFloat( value ) : Float.NaN;
    }

    /**
     * Retrieve the value of this measurement as a double.
     * <p>
     * Any missing value (i.e. null) will be returned as a {@link Double#NaN}.
     */
    @Transient
    public double getValueAsDouble() {
        ensureRepresentation( PrimitiveType.DOUBLE );
        return value != null ? Double.parseDouble( value ) : Double.NaN;
    }

    public void setValueAsDouble( double value ) {
        ensureRepresentation( PrimitiveType.DOUBLE );
        this.value = String.valueOf( value );
    }

    private void ensureRepresentation( PrimitiveType representation ) {
        if ( this.representation != representation ) {
            throw new IllegalStateException( "THis measurement stores values of type " + this.representation + ", but " + representation + " was requested." );
        }
    }


    @Override
    public int hashCode() {
        return Objects.hash( getValue(), getRepresentation(), getUnit(), getType(), getKindCV(), getOtherKind() );
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj )
            return true;
        if ( !( obj instanceof Measurement ) )
            return false;
        Measurement other = ( Measurement ) obj;
        if ( getId() != null && other.getId() != null )
            return getId().equals( other.getId() );
        return Objects.equals( getValue(), other.getValue() )
                && Objects.equals( getUnit(), other.getUnit() )
                && Objects.equals( getType(), other.getType() )
                && Objects.equals( getRepresentation(), other.getRepresentation() )
                && Objects.equals( getKindCV(), other.getKindCV() )
                && Objects.equals( getOtherKind(), other.getOtherKind() );
    }

    @Override
    public String toString() {
        return this.getValue();
    }

    public static final class Factory {

        public static Measurement newInstance() {
            return new Measurement();
        }

        public static Measurement newInstance( MeasurementType type, String value, MeasurementKind kindCV,
                String otherKind, PrimitiveType representation, Unit unit ) {
            final Measurement entity = new Measurement();
            entity.setType( type );
            entity.setValue( value );
            entity.setKindCV( kindCV );
            entity.setOtherKind( otherKind );
            entity.setRepresentation( representation );
            entity.setUnit( unit );
            return entity;
        }

        public static Measurement newInstance( MeasurementType type, String value, PrimitiveType representation ) {
            final Measurement entity = new Measurement();
            entity.setType( type );
            entity.setValue( value );
            entity.setRepresentation( representation );
            return entity;
        }
    }

}