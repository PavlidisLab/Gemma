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

import java.io.Serializable;

@SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
public abstract class Measurement implements Identifiable, Serializable {

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

    @Override
    public int hashCode() {
        int hashCode = 0;
        hashCode = 29 * hashCode + ( id == null ? 0 : id.hashCode() );

        return hashCode;
    }

    @Override
    public boolean equals( Object object ) {
        if ( this == object ) {
            return true;
        }
        if ( !( object instanceof Measurement ) ) {
            return false;
        }
        final Measurement that = ( Measurement ) object;
        return !( this.id == null || that.getId() == null || !this.id.equals( that.getId() ) );
    }

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

    public String getValue() {
        return this.value;
    }

    public void setValue( String value ) {
        this.value = value;
    }

    @SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
    public static final class Factory {

        public static Measurement newInstance() {
            return new MeasurementImpl();
        }

        public static Measurement newInstance( MeasurementType type, String value, MeasurementKind kindCV,
                String otherKind, PrimitiveType representation, Unit unit ) {
            final Measurement entity = new MeasurementImpl();
            entity.setType( type );
            entity.setValue( value );
            entity.setKindCV( kindCV );
            entity.setOtherKind( otherKind );
            entity.setRepresentation( representation );
            entity.setUnit( unit );
            return entity;
        }

        public static Measurement newInstance( MeasurementType type, String value, PrimitiveType representation ) {
            final Measurement entity = new MeasurementImpl();
            entity.setType( type );
            entity.setValue( value );
            entity.setRepresentation( representation );
            return entity;
        }
    }

}