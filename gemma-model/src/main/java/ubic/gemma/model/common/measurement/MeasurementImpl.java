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

    /*
     * (non-Javadoc)
     * 
     * @see Object#equals(Object)
     */
    @Override
    public boolean equals( Object obj ) {
        if ( this == obj ) return true;
        if ( obj == null ) return false;
        if ( getClass() != obj.getClass() ) return false;

        Measurement other = ( Measurement ) obj;
        if ( super.getId() == null ) {
            if ( other.getId() != null ) return false;
        } else if ( !getId().equals( other.getId() ) ) return false;

        if ( getValue() == null ) {
            if ( other.getValue() != null ) return false;
        } else if ( !getValue().equals( other.getValue() ) ) return false;

        if ( getUnit() == null ) {
            if ( other.getUnit() != null ) return false;
        } else if ( !getUnit().equals( other.getUnit() ) ) return false;

        if ( getType() == null ) {
            if ( other.getType() != null ) return false;
        } else if ( !getType().equals( other.getType() ) ) return false;

        if ( getKindCV() == null ) {
            if ( other.getKindCV() != null ) return false;
        } else if ( !getKindCV().equals( other.getKindCV() ) ) return false;

        if ( getOtherKind() == null ) {
            if ( other.getOtherKind() != null ) return false;
        } else if ( !getOtherKind().equals( other.getOtherKind() ) ) return false;
        if ( getRepresentation() == null ) {
            if ( other.getRepresentation() != null ) return false;
        } else if ( !getRepresentation().equals( other.getRepresentation() ) ) return false;

        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( super.getId() == null ) ? 0 : super.getId().hashCode() );

        if ( super.getId() == null ) {
            result = prime * result + ( ( getValue() == null ) ? 0 : getValue().hashCode() );

            result = prime * result + ( ( getRepresentation() == null ) ? 0 : getRepresentation().hashCode() );
            result = prime * result + ( ( getUnit() == null ) ? 0 : getUnit().hashCode() );

            result = prime * result + ( ( getType() == null ) ? 0 : getType().hashCode() );

            result = prime * result + ( ( getKindCV() == null ) ? 0 : getKindCV().hashCode() );
            result = prime * result + ( ( getOtherKind() == null ) ? 0 : getOtherKind().hashCode() );

        }
        return result;
    }

    @Override
    public String toString() {
        return this.getValue();
    }

}