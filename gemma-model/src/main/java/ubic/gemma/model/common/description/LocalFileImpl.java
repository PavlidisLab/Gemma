/*
 * The Gemma project.
 * 
 * Copyright (c) 2006 University of British Columbia
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

/**
 * @author pavlidis
 * @version $Id$
 * @see ubic.gemma.model.common.description.LocalFile
 */
public class LocalFileImpl extends LocalFile {

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = -2781283721621657394L;

    /**
     * Attempt to create a java.io.File from the local URI. If it doesn't look like a URI, it is just treated as a path.
     * 
     * @see ubic.gemma.model.common.description.LocalFile#asFile()
     */
    @Override
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
    @Override
    public Boolean canRead() {
        return this.asFile().canRead();
    }

    /**
     * @see ubic.gemma.model.common.description.LocalFile#canWrite()
     */
    @Override
    public Boolean canWrite() {
        return this.asFile().canWrite();
    }

    /*
     * (non-Javadoc)
     * 
     * @see Object#toString()
     */
    @Override
    public String toString() {
        return this.asFile() == null ? super.toString() : this.asFile().toString();
    }

}