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

import java.io.File;
import java.net.URISyntaxException;
import java.util.Collection;

/**
 * 
 */
public abstract class LocalFile implements java.io.Serializable, gemma.gsec.model.Securable {

    /**
     * Constructs new instances of {@link ubic.gemma.model.common.description.LocalFile}.
     */
    public static final class Factory {
        /**
         * Constructs a new instance of {@link ubic.gemma.model.common.description.LocalFile}.
         */
        public static LocalFile newInstance() {
            return new LocalFileImpl();
        }

    }

    private java.net.URL localURL;

    private java.net.URL remoteURL;

    private String version;

    private Long size;

    private Long id;

    private FileFormat format;

    private Collection<LocalFile> sourceFiles = new java.util.HashSet<>();

    /**
     * Attempt to create a java.io.File from the local URI. If it doesn't look like a URI, it is just treated as a path.
     * 
     * @see ubic.gemma.model.common.description.LocalFile#asFile()
     */
    public java.io.File asFile() {

        if ( this.getLocalURL() == null ) {
            return null;
        }

        try {
            return new File( this.getLocalURL().toURI() );
        } catch ( URISyntaxException e ) {
            throw new RuntimeException( e );
        }

    }

    /**
     * @see ubic.gemma.model.common.description.LocalFile#canRead()
     */
    public Boolean canRead() {
        return this.asFile().canRead();
    }

    /**
     * @see ubic.gemma.model.common.description.LocalFile#canWrite()
     */
    public Boolean canWrite() {
        return this.asFile().canWrite();
    }

    /**
     * Returns <code>true</code> if the argument is an LocalFile instance and all identifiers for this entity equal the
     * identifiers of the argument entity. Returns <code>false</code> otherwise.
     */
    @Override
    public boolean equals( Object object ) {
        if ( this == object ) {
            return true;
        }
        if ( !( object instanceof LocalFile ) ) {
            return false;
        }
        final LocalFile that = ( LocalFile ) object;
        if ( this.id == null || that.getId() == null || !this.id.equals( that.getId() ) ) {
            return false;
        }
        return true;
    }

    /**
     * 
     */
    public ubic.gemma.model.common.description.FileFormat getFormat() {
        return this.format;
    }

    /**
     * 
     */
    @Override
    public Long getId() {
        return this.id;
    }

    /**
     * The location of the file on a local server
     */
    public java.net.URL getLocalURL() {
        return this.localURL;
    }

    /**
     * Source where the file was downloaded from.
     */
    public java.net.URL getRemoteURL() {
        return this.remoteURL;
    }

    /**
     * 
     */
    public Long getSize() {
        return this.size;
    }

    /**
     * Any files which were used to create this file.
     */
    public Collection<ubic.gemma.model.common.description.LocalFile> getSourceFiles() {
        return this.sourceFiles;
    }

    /**
     * The version identifier for the file; this could be as simple as the date of creation
     */
    public String getVersion() {
        return this.version;
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

    public void setFormat( ubic.gemma.model.common.description.FileFormat format ) {
        this.format = format;
    }

    public void setId( Long id ) {
        this.id = id;
    }

    public void setLocalURL( java.net.URL localURL ) {
        this.localURL = localURL;
    }

    public void setRemoteURL( java.net.URL remoteURL ) {
        this.remoteURL = remoteURL;
    }

    public void setSize( Long size ) {
        this.size = size;
    }

    public void setSourceFiles( Collection<ubic.gemma.model.common.description.LocalFile> sourceFiles ) {
        this.sourceFiles = sourceFiles;
    }

    public void setVersion( String version ) {
        this.version = version;
    }

}