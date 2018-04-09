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
package ubic.gemma.model.common.description;

/**
 * @author paul
 */
public abstract class PublicationType implements java.io.Serializable {
    private static final long serialVersionUID = -7520632580702206897L;
    private String type;
    private Long id;

    /**
     * No-arg constructor added to satisfy javabean contract
     */
    @SuppressWarnings("WeakerAccess") // Required by Spring
    public PublicationType() {
    }

    public Long getId() {
        return this.id;
    }

    @SuppressWarnings("unused") // Possible external use
    public void setId( Long id ) {
        this.id = id;
    }

    public String getType() {
        return this.type;
    }

    public void setType( String type ) {
        this.type = type;
    }

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
        if ( !( object instanceof PublicationType ) ) {
            return false;
        }
        final PublicationType that = ( PublicationType ) object;
        return this.id != null && that.getId() != null && this.id.equals( that.getId() );
    }

    public static final class Factory {

        public static PublicationType newInstance() {
            return new PublicationTypeImpl();
        }

        @SuppressWarnings("unused") // Possible external use
        public static PublicationType newInstance( String type ) {
            final PublicationType entity = new PublicationTypeImpl();
            entity.setType( type );
            return entity;
        }
    }

}