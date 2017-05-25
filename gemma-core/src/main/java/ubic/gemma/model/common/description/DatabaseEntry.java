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
public class DatabaseEntry implements java.io.Serializable {

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = 5418961655066735636L;
    private String accession;
    private String accessionVersion;
    private String Uri;
    private Long id;
    private ubic.gemma.model.common.description.ExternalDatabase externalDatabase;

    /* ********************************
     * Constructors
     * ********************************/

    /**
     * No-arg constructor added to satisfy javabean contract
     *
     * @author Paul
     */
    public DatabaseEntry() {
    }

    /* ********************************
     * Object override methods
     * ********************************/

    /**
     * Returns <code>true</code> if the argument is an DatabaseEntry instance and all identifiers for this entity equal
     * the identifiers of the argument entity. Returns <code>false</code> otherwise.
     */
    @Override
    public boolean equals( Object object ) {
        if ( !( object instanceof DatabaseEntry ) )
            return false;

        DatabaseEntry that = ( DatabaseEntry ) object;

        if ( this.getId() != null && that.getId() != null )
            return super.equals( object );

        if ( this.getAccession() != null && that.getAccession() != null && !this.getAccession()
                .equals( that.getAccession() ) )
            return false;

        if ( this.getAccessionVersion() != null && that.getAccessionVersion() != null && !this.getAccessionVersion()
                .equals( that.getAccessionVersion() ) )
            return false;

        if ( this.getExternalDatabase() != null && that.getExternalDatabase() != null && !this.getExternalDatabase()
                .equals( that.getExternalDatabase() ) )
            return false;

        return true;
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
     * @see ubic.gemma.model.common.description.DatabaseEntry#toString()
     */
    @Override
    public String toString() {
        return ( this.getAccession() + " " ) + ( this.getExternalDatabase() == null ?
                "[no external database]" :
                this.getExternalDatabase().getName() ) + ( this.getId() == null ? "" : " (Id=" + this.getId() + ")" );
    }

    /* ********************************
     * Public methods
     * ********************************/

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

    public ubic.gemma.model.common.description.ExternalDatabase getExternalDatabase() {
        return this.externalDatabase;
    }

    public void setExternalDatabase( ubic.gemma.model.common.description.ExternalDatabase externalDatabase ) {
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

    /* ********************************
     * Public static classes
     * ********************************/

    /**
     * Constructs new instances of {@link ubic.gemma.model.common.description.DatabaseEntry}.
     */
    public static final class Factory {
        /**
         * Constructs a new instance of {@link ubic.gemma.model.common.description.DatabaseEntry}.
         */
        public static ubic.gemma.model.common.description.DatabaseEntry newInstance() {
            return new ubic.gemma.model.common.description.DatabaseEntry();
        }

        /**
         * Constructs a new instance of {@link ubic.gemma.model.common.description.DatabaseEntry}, taking all possible
         * properties (except the identifier(s))as arguments.
         */
        public static ubic.gemma.model.common.description.DatabaseEntry newInstance( String accession,
                String accessionVersion, String Uri,
                ubic.gemma.model.common.description.ExternalDatabase externalDatabase ) {
            final ubic.gemma.model.common.description.DatabaseEntry entity = new ubic.gemma.model.common.description.DatabaseEntry();
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
            final ubic.gemma.model.common.description.DatabaseEntry entity = new ubic.gemma.model.common.description.DatabaseEntry();
            entity.setExternalDatabase( externalDatabase );
            return entity;
        }
    }

}