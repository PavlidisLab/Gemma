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

import java.io.Serializable;

@SuppressWarnings("WeakerAccess") // Possible frontend use
public class Unit implements Identifiable, Serializable {

    private static final long serialVersionUID = 6348133346610787608L;
    private String unitNameCV;
    private Long id;

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

    @Override
    public Long getId() {
        return this.id;
    }

    @SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
    public void setId( Long id ) {
        this.id = id;
    }

    public String getUnitNameCV() {
        return this.unitNameCV;
    }

    public void setUnitNameCV( String unitNameCV ) {
        this.unitNameCV = unitNameCV;
    }

    @SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
    public static final class Factory {

        public static Unit newInstance( String unitNameCV ) {
            final Unit entity = new Unit();
            entity.setUnitNameCV( unitNameCV );
            return entity;
        }
    }

}