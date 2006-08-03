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
package ubic.gemma.loader.expression.arrayExpress;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.FutureTask;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.net.ftp.FTP;

import ubic.gemma.loader.expression.arrayExpress.util.ArrayExpressUtil;
import ubic.gemma.loader.util.fetcher.FtpArchiveFetcher;
import ubic.gemma.model.common.description.LocalFile;

/**
 * ArrayExpress stores files in an FTP site as tarred-gzipped archives. Each tar file contains the MAGE file and the
 * datacube external files. This class can download an experiment, unpack the tar file, and put the resulting files onto
 * a local filesystem.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class DataFileFetcher extends FtpArchiveFetcher {

    public DataFileFetcher() {
        initConfig();
        initArchiveHandler( "gzip" );
    }

    /**
     * @param identifier The accession value for the experiment, such as "SMDB-14"
     * @param discardArchive Whether to delete the downloaded archive after unpacking its contents
     * @return
     * @throws SocketException
     * @throws IOException
     */
    public Collection<LocalFile> fetch( String identifier ) {

        try {
            if ( f == null || !f.isConnected() ) {
                f = ArrayExpressUtil.connect( FTP.BINARY_FILE_TYPE );
            }

            File newDir = mkdir( identifier );
            final String outputFileName = formLocalFilePath( identifier, newDir );
            final String seekFile = formRemoteFilePath( identifier );

            FutureTask<Boolean> future = this.defineTask( outputFileName, seekFile );
            return this.doTask( future, seekFile, outputFileName, identifier, newDir, ".mageml.tgz" );

        } catch ( IOException e ) {
            throw new RuntimeException( "Couldn't fetch file for " + identifier, e );
        }

    }

    /**
     * @param files
     * @return
     */
    public LocalFile getMageMlFile( Collection<LocalFile> files ) {
        for ( LocalFile file : files ) {
            if ( file.getLocalURI().endsWith( ".xml" ) ) {
                return file;
            }
        }
        return null;
    }

    /**
     * @param identifier
     * @param newDir
     * @return
     */
    private String formLocalFilePath( String identifier, File newDir ) {
        String outputFileName = newDir + System.getProperty( "file.separator" ) + "E-" + identifier + ".mageml.tgz";
        return outputFileName;
    }

    /**
     * @param identifier
     * @return
     */
    protected String formRemoteFilePath( String identifier ) {
        String dirName = identifier.replaceFirst( "-\\d+", "" );
        String seekFile = baseDir + "/" + dirName + "/" + "E-" + identifier + "/" + "E-" + identifier + ".mageml.tgz";
        return seekFile;
    }

    /**
     * @throws ConfigurationException
     */
    protected void initConfig() {
        Configuration config;
        try {
            config = new PropertiesConfiguration( "Gemma.properties" );

            localBasePath = ( String ) config.getProperty( "arrayExpress.local.datafile.basepath" );
            baseDir = ( String ) config.getProperty( "arrayExpress.experiments.baseDir" );

            if ( localBasePath == null || localBasePath.length() == 0 )
                throw new ConfigurationException( "localBasePath was null or empty" );
            if ( baseDir == null || baseDir.length() == 0 )
                throw new ConfigurationException( "baseDir was null or empty" );
        } catch ( ConfigurationException e ) {
            throw new RuntimeException( e );
        }
    }

}
