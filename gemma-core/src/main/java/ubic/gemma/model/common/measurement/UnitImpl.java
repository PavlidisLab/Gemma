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
 * @see ubic.gemma.model.common.measurement.Unit
 */
public class UnitImpl extends ubic.gemma.model.common.measurement.Unit {
    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = -6113027641969468902L;

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( this.getId() == null ) ? 0 : this.getId().hashCode() );
        result = prime * result + ( ( this.getUnitNameCV() == null ) ? 0 : this.getUnitNameCV().hashCode() );
        return result;
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj )
            return true;
        if ( obj == null )
            return false;
        if ( this.getClass() != obj.getClass() )
            return false;
        Unit other = ( Unit ) obj;
        if ( this.getId() == null ) {
            if ( other.getId() != null )
                return false;
        } else if ( !this.getId().equals( other.getId() ) )
            return false;
        if ( this.getUnitNameCV() == null ) {
            return other.getUnitNameCV() == null;
        } else
            return this.getUnitNameCV().equals( other.getUnitNameCV() );
    }

    @Override
    public String toString() {
        return this.getUnitNameCV();
    }

}