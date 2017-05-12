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
public abstract class Unit implements java.io.Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 6348133346610787608L;

    /**
     * Constructs new instances of {@link Unit}.
     */
    public static final class Factory {

        /**
         * Constructs a new instance of {@link Unit}, taking all possible properties (except the identifier(s))as
         * arguments.
         */
        public static Unit newInstance( String unitNameCV ) {
            final Unit entity = new UnitImpl();
            entity.setUnitNameCV( unitNameCV );
            return entity;
        }
    }

    private String unitNameCV;

    private Long id;

    @Override
    public boolean equals( Object object ) {
        if ( this == object ) {
            return true;
        }
        if ( !( object instanceof Unit ) ) {
            return false;
        }
        final Unit that = ( Unit ) object;
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
    public String getUnitNameCV() {
        return this.unitNameCV;
    }

    @Override
    public int hashCode() {
        int hashCode = 0;
        hashCode = 29 * hashCode + ( id == null ? 0 : id.hashCode() );

        return hashCode;
    }

    public void setId( Long id ) {
        this.id = id;
    }

    public void setUnitNameCV( String unitNameCV ) {
        this.unitNameCV = unitNameCV;
    }

}