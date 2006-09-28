/*
 * The Gemma project
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
package ubic.gemma.loader.util.fetcher;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.model.common.description.LocalFile;

/**
 * @author pavlidis
 * @version $Id$
 */
public abstract class AbstractFetcher implements Fetcher {

    protected static Log log = LogFactory.getLog( AbstractFetcher.class.getName() );
    protected String localBasePath = null;
    protected String baseDir = null;
    protected boolean force = false;

    /**
     * Set to true if downloads should proceed even if the file already exists.
     * 
     * @param force
     */
    public void setForce( boolean force ) {
        this.force = force;
    }

    /**
     * Like mkdir(accession) but for cases where there is no accession.
     * 
     * @return
     */
    protected File mkdir() throws IOException {
        return this.mkdir( null );
    }

    /**
     * Create a directory according to the current accession number and set path information, including any nonexisting
     * parent directories. If the path cannot be used, we use a temporary directory.
     * 
     * @param accession
     * @return new directory
     * @throws IOException
     */
    protected File mkdir( String accession ) throws IOException {
        File newDir = null;
        File targetPath = null;

        if ( localBasePath != null ) {

            targetPath = new File( localBasePath );

            if ( !( targetPath.exists() && targetPath.canRead() ) ) {
                log.warn( "Attempting to create directory '" + localBasePath + "'" );
                targetPath.mkdirs();
            }

            if ( accession == null ) {
                newDir = targetPath;
            } else {
                newDir = new File( targetPath + File.separator + accession );
            }

        }

        if ( localBasePath == null || !targetPath.canRead() ) {
            log.warn( "Could not create output directory " + newDir );

            File tmpDir;
            String systemTempDir = System.getProperty( "java.io.tmpdir" );
            if ( accession == null ) {
                tmpDir = new File( systemTempDir );
            } else {
                tmpDir = new File( systemTempDir + File.separator + accession );
            }
            log.warn( "Will use local temporary directory: " + tmpDir.getAbsolutePath() );
            newDir = tmpDir;
        }

        if ( newDir == null ) {
            throw new IOException( "Could not create target directory, was null" );
        }
        if ( !newDir.exists() && !newDir.mkdirs() ) {
            throw new IOException( "Could not create target directory " + newDir.getAbsolutePath() );
        }
        if ( !newDir.canWrite() ) {
            throw new IOException( "Cannot write to target directory " + newDir.getAbsolutePath() );
        }

        log.info( "New dir is " + newDir );

        return newDir;
    }

    /**
     * @param seekFile
     * @return
     */
    protected LocalFile fetchedFile( String seekFile ) {
        return this.fetchedFile( seekFile, seekFile );
    }

    /**
     * @param seekFilePath Absolute path to the file for download
     * @param outputFilePath Absolute path to the download location.
     * @return
     */
    protected LocalFile fetchedFile( String seekFilePath, String outputFilePath ) {
        LocalFile file = LocalFile.Factory.newInstance();
        file.setVersion( new SimpleDateFormat().format( new Date() ) );
        try {
            file.setRemoteURL( ( new File( seekFilePath ) ).toURI().toURL() );
            file.setLocalURL( ( new File( outputFilePath ).toURI().toURL() ) );
        } catch ( MalformedURLException e ) {
            throw new RuntimeException( e );
        }
        return file;
    }

    /**
     * @return Returns the localBasePath.
     */
    public String getLocalBasePath() {
        return this.localBasePath;
    }

    /**
     * @return the force
     */
    public boolean isForce() {
        return this.force;
    }

}
