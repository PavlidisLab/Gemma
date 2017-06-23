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

import ubic.gemma.model.common.Identifiable;

import java.io.Serializable;

/**
 * <p>
 * A reference to a record in a database.
 * </p>
 */
public class DatabaseEntry implements Identifiable, Serializable {

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = 5418961655066735636L;
    private String accession;
    private String accessionVersion;
    private String Uri;
    private Long id;
    private ExternalDatabase externalDatabase;

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
        return !( this.id == null || that.getId() == null || !this.id.equals( that.getId() ) );
    }

    @Override
    public int hashCode() {
        if ( this.getId() != null )
            return super.hashCode();

        int hashCode = 0;
        if ( this.getAccession() != null )
            hashCode = 29 * this.getAccession().hashCode();

        if ( this.getAccessionVersion() != null )
            hashCode += this.getAccessionVersion().hashCode();

        if ( this.getExternalDatabase() != null )
            hashCode += this.getExternalDatabase().hashCode();

        return hashCode;
    }

    /**
     * @see DatabaseEntry#toString()
     */
    @Override
    public String toString() {
        return ( this.getAccession() + " " ) + ( this.getExternalDatabase() == null ?
                "[no external database]" :
                this.getExternalDatabase().getName() ) + ( this.getId() == null ? "" : " (Id=" + this.getId() + ")" );
    }

    /**
     * The id
     */
    public String getAccession() {
        return this.accession;
    }

    public void setAccession( String accession ) {
        this.accession = accession;
    }

    public String getAccessionVersion() {
        return this.accessionVersion;
    }

    public void setAccessionVersion( String accessionVersion ) {
        this.accessionVersion = accessionVersion;
    }

    public ExternalDatabase getExternalDatabase() {
        return this.externalDatabase;
    }

    public void setExternalDatabase( ExternalDatabase externalDatabase ) {
        this.externalDatabase = externalDatabase;
    }

    public Long getId() {
        return this.id;
    }

    public void setId( Long id ) {
        this.id = id;
    }

    public String getUri() {
        return this.Uri;
    }

    public void setUri( String Uri ) {
        this.Uri = Uri;
    }

    /**
     * Constructs new instances of {@link DatabaseEntry}.
     */
    public static final class Factory {
        /**
         * Constructs a new instance of {@link DatabaseEntry}.
         */
        public static DatabaseEntry newInstance() {
            return new DatabaseEntry();
        }

        /**
         * Constructs a new instance of {@link DatabaseEntry}, taking all possible
         * properties (except the identifier(s))as arguments.
         */
        public static DatabaseEntry newInstance( String accession, String accessionVersion, String Uri,
                ExternalDatabase externalDatabase ) {
            final DatabaseEntry entity = new DatabaseEntry();
            entity.setAccession( accession );
            entity.setAccessionVersion( accessionVersion );
            entity.setUri( Uri );
            entity.setExternalDatabase( externalDatabase );
            return entity;
        }

        /**
         * Constructs a new instance of {@link DatabaseEntry}, taking all required
         * and/or read-only properties as arguments.
         */
        public static DatabaseEntry newInstance( ExternalDatabase externalDatabase ) {
            final DatabaseEntry entity = new DatabaseEntry();
            entity.setExternalDatabase( externalDatabase );
            return entity;
        }
    }

}