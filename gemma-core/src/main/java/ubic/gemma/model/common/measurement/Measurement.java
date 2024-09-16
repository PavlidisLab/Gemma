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

import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Objects;

@SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
public class Measurement implements Identifiable, Serializable {

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = 1833568047451431226L;
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
    private Long id;
    private Unit unit;

    @Override
    public Long getId() {
        return this.id;
    }

    public void setId( Long id ) {
        this.id = id;
    }

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


    @SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
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