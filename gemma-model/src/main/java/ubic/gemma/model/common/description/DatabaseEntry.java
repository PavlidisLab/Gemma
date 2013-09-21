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
 * <p>
 * A reference to a record in a database.
 * </p>
 */
public abstract class DatabaseEntry implements java.io.Serializable {

    /**
     * Constructs new instances of {@link ubic.gemma.model.common.description.DatabaseEntry}.
     */
    public static final class Factory {
        /**
         * Constructs a new instance of {@link ubic.gemma.model.common.description.DatabaseEntry}.
         */
        public static ubic.gemma.model.common.description.DatabaseEntry newInstance() {
            return new ubic.gemma.model.common.description.DatabaseEntryImpl();
        }

        /**
         * Constructs a new instance of {@link ubic.gemma.model.common.description.DatabaseEntry}, taking all possible
         * properties (except the identifier(s))as arguments.
         */
        public static ubic.gemma.model.common.description.DatabaseEntry newInstance( String accession,
                String accessionVersion, String Uri,
                ubic.gemma.model.common.description.ExternalDatabase externalDatabase ) {
            final ubic.gemma.model.common.description.DatabaseEntry entity = new ubic.gemma.model.common.description.DatabaseEntryImpl();
            entity.setAccession( accession );
            entity.setAccessionVersion( accessionVersion );
            entity.setUri( Uri );
            entity.setExternalDatabase( externalDatabase );
            return entity;
        }

        /**
         * Constructs a new instance of {@link ubic.gemma.model.common.description.DatabaseEntry}, taking all required
         * and/or read-only properties as arguments.
         */
        public static ubic.gemma.model.common.description.DatabaseEntry newInstance(
                ubic.gemma.model.common.description.ExternalDatabase externalDatabase ) {
            final ubic.gemma.model.common.description.DatabaseEntry entity = new ubic.gemma.model.common.description.DatabaseEntryImpl();
            entity.setExternalDatabase( externalDatabase );
            return entity;
        }
    }

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = 5418961655066735636L;
    private String accession;

    private String accessionVersion;

    private String Uri;

    private Long id;

    private ubic.gemma.model.common.description.ExternalDatabase externalDatabase;

    /**
     * No-arg constructor added to satisfy javabean contract
     * 
     * @author Paul
     */
    public DatabaseEntry() {
    }

    /**
     * Returns <code>true</code> if the argument is an DatabaseEntry instance and all identifiers for this entity equal
     * the identifiers of the argument entity. Returns <code>false</code> otherwise.
     */
    @Override
    public boolean equals( Object object ) {
        if ( this == object ) {
            return true;
        }
        if ( !( object instanceof DatabaseEntry ) ) {
            return false;
        }
        final DatabaseEntry that = ( DatabaseEntry ) object;
        if ( this.id == null || that.getId() == null || !this.id.equals( that.getId() ) ) {
            return false;
        }
        return true;
    }

    /**
     * <p>
     * The id
     * </p>
     */
    public String getAccession() {
        return this.accession;
    }

    /**
     * 
     */
    public String getAccessionVersion() {
        return this.accessionVersion;
    }

    /**
     * 
     */
    public ubic.gemma.model.common.description.ExternalDatabase getExternalDatabase() {
        return this.externalDatabase;
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
    public String getUri() {
        return this.Uri;
    }

    /**
     * Returns a hash code based on this entity's identifiers.
     */
    @Override
    public int hashCode() {
        int hashCode = 0;
        hashCode = 29 * hashCode + ( id == null ? 0 : id.hashCode() );

        return hashCode;
    }

    public void setAccession( String accession ) {
        this.accession = accession;
    }

    public void setAccessionVersion( String accessionVersion ) {
        this.accessionVersion = accessionVersion;
    }

    public void setExternalDatabase( ubic.gemma.model.common.description.ExternalDatabase externalDatabase ) {
        this.externalDatabase = externalDatabase;
    }

    public void setId( Long id ) {
        this.id = id;
    }

    public void setUri( String Uri ) {
        this.Uri = Uri;
    }

}