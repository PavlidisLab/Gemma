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

public abstract class FileFormat implements java.io.Serializable {

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = 9105276589707517468L;
    private String formatIdentifier;
    private Long id;

    /**
     * No-arg constructor added to satisfy javabean contract
     *
     * @author Paul
     */
    public FileFormat() {
    }

    @Override
    public boolean equals( Object object ) {
        if ( this == object ) {
            return true;
        }
        if ( !( object instanceof FileFormat ) ) {
            return false;
        }
        final FileFormat that = ( FileFormat ) object;
        if ( this.id == null || that.getId() == null || !this.id.equals( that.getId() ) ) {
            return false;
        }
        return true;
    }

    public String getFormatIdentifier() {
        return this.formatIdentifier;
    }

    public void setFormatIdentifier( String formatIdentifier ) {
        this.formatIdentifier = formatIdentifier;
    }

    public Long getId() {
        return this.id;
    }

    public void setId( Long id ) {
        this.id = id;
    }

    @Override
    public int hashCode() {
        int hashCode = 0;
        hashCode = 29 * hashCode + ( id == null ? 0 : id.hashCode() );

        return hashCode;
    }

    public static final class Factory {

        public static ubic.gemma.model.common.description.FileFormat newInstance() {
            return new ubic.gemma.model.common.description.FileFormatImpl();
        }

        public static ubic.gemma.model.common.description.FileFormat newInstance( String formatIdentifier ) {
            final ubic.gemma.model.common.description.FileFormat entity = new ubic.gemma.model.common.description.FileFormatImpl();
            entity.setFormatIdentifier( formatIdentifier );
            return entity;
        }
    }

}