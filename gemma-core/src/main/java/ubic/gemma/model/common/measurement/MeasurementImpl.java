/*
 * The Gemma project.
 *
 * Copyright (c) 2006-2007 University of British Columbia
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
 * @see ubic.gemma.model.common.measurement.Measurement
 */
public class MeasurementImpl extends ubic.gemma.model.common.measurement.Measurement {
    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = -5572865478492871637L;

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj )
            return true;
        if ( obj == null )
            return false;
        if ( this.getClass() != obj.getClass() )
            return false;

        Measurement other = ( Measurement ) obj;
        if ( super.getId() == null ) {
            if ( other.getId() != null )
                return false;
        } else if ( !this.getId().equals( other.getId() ) )
            return false;

        if ( this.getValue() == null ) {
            if ( other.getValue() != null )
                return false;
        } else if ( !this.getValue().equals( other.getValue() ) )
            return false;

        if ( this.getUnit() == null ) {
            if ( other.getUnit() != null )
                return false;
        } else if ( !this.getUnit().equals( other.getUnit() ) )
            return false;

        if ( this.getType() == null ) {
            if ( other.getType() != null )
                return false;
        } else if ( !this.getType().equals( other.getType() ) )
            return false;

        if ( this.getKindCV() == null ) {
            if ( other.getKindCV() != null )
                return false;
        } else if ( !this.getKindCV().equals( other.getKindCV() ) )
            return false;

        if ( this.getOtherKind() == null ) {
            if ( other.getOtherKind() != null )
                return false;
        } else if ( !this.getOtherKind().equals( other.getOtherKind() ) )
            return false;
        if ( this.getRepresentation() == null ) {
            return other.getRepresentation() == null;
        } else
            return this.getRepresentation().equals( other.getRepresentation() );

    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( super.getId() == null ) ? 0 : super.getId().hashCode() );

        if ( super.getId() == null ) {
            result = prime * result + ( ( this.getValue() == null ) ? 0 : this.getValue().hashCode() );

            result =
                    prime * result + ( ( this.getRepresentation() == null ) ? 0 : this.getRepresentation().hashCode() );
            result = prime * result + ( ( this.getUnit() == null ) ? 0 : this.getUnit().hashCode() );

            result = prime * result + ( ( this.getType() == null ) ? 0 : this.getType().hashCode() );

            result = prime * result + ( ( this.getKindCV() == null ) ? 0 : this.getKindCV().hashCode() );
            result = prime * result + ( ( this.getOtherKind() == null ) ? 0 : this.getOtherKind().hashCode() );

        }
        return result;
    }

    @Override
    public String toString() {
        return this.getValue();
    }

}