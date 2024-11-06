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

import java.util.Objects;

public class Unit extends AbstractIdentifiable {

    private String unitNameCV;

    public String getUnitNameCV() {
        return this.unitNameCV;
    }

    public void setUnitNameCV( String unitNameCV ) {
        this.unitNameCV = unitNameCV;
    }

    @Override
    public int hashCode() {
        return Objects.hash( unitNameCV );
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj )
            return true;
        if ( obj == null )
            return false;
        if ( !( obj instanceof Unit ) )
            return false;
        Unit other = ( Unit ) obj;
        if ( getId() != null && other.getId() != null )
            return getId().equals( other.getId() );
        return Objects.equals( getUnitNameCV(), other.getUnitNameCV() );
    }

    @Override
    public String toString() {
        return this.getUnitNameCV();
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