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
package ubic.gemma.loader.expression.geo.fetcher;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.SocketException;
import java.util.Collection;
import java.util.concurrent.FutureTask;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.net.ftp.FTP;

import ubic.basecode.util.NetUtils;
import ubic.gemma.loader.expression.geo.util.GeoUtil;
import ubic.gemma.loader.util.fetcher.FtpArchiveFetcher;
import ubic.gemma.model.common.description.LocalFile;

/**
 * Retrieve and unpack the raw data files for GEO series. These are the CEL and other files (RPT, EXP and maybe DAT) for
 * Affymetrix data sets. For other types of arrays there may also be raw data?
 * 
 * @author pavlidis
 * @version $Id$
 */
public class RawDataFetcher extends FtpArchiveFetcher {

    public RawDataFetcher() {
        initConfig();
        initArchiveHandler( null );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.loader.loaderutils.Fetcher#fetch(java.lang.String)
     */
    public Collection<LocalFile> fetch( String identifier ) {
        try {
            if ( f == null || !f.isConnected() ) f = GeoUtil.connect( FTP.BINARY_FILE_TYPE );
            assert f != null;
            File newDir = mkdir( identifier );
            newDir = new File( newDir, "rawDataFiles" );
            if ( !newDir.canRead() && !newDir.mkdir() )
                throw new IOException( "Could not create the raw data subdirectory" );
            final String outputFileName = formLocalFilePath( identifier, newDir );
            final String seekFile = formRemoteFilePath( identifier );
            try {
                NetUtils.checkForFile( f, seekFile );
            } catch ( FileNotFoundException e ) {
                // that's okay, just return.
                log.info( "There is apparently no raw data archive for " + identifier );
                newDir.delete(); // nothing there.
                f.disconnect(); // important to do this!
                return null;
            }
            FutureTask<Boolean> future = defineTask( outputFileName, seekFile );
            Collection<LocalFile> result = doTask( future, seekFile, outputFileName, identifier, newDir, ".tar" );
            f.disconnect();
            return result;
        } catch ( SocketException e ) {
            throw new RuntimeException( e );
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }

    }

    /**
     * @param identifier
     * @param newDir
     * @return
     */
    private String formLocalFilePath( String identifier, File newDir ) {
        String outputFileName = newDir + File.separator + identifier + "_RAW.tar";
        return outputFileName;
    }

    /**
     * @param identifier
     * @return
     */
    protected String formRemoteFilePath( String identifier ) {
        String seekFile = baseDir + "/" + identifier + "/" + identifier + "_RAW.tar";
        return seekFile;
    }

    /**
     * @throws ConfigurationException
     */
    protected void initConfig() {
        Configuration config;
        try {
            config = new PropertiesConfiguration( "Gemma.properties" );

            localBasePath = ( String ) config.getProperty( "geo.local.datafile.basepath" );
            baseDir = ( String ) config.getProperty( "geo.remote.rawDataDir" );

            if ( localBasePath == null || localBasePath.length() == 0 )
                throw new RuntimeException( new ConfigurationException( "localBasePath was null or empty" ) );
            if ( baseDir == null || baseDir.length() == 0 )
                throw new RuntimeException( new ConfigurationException( "baseDir was null or empty" ) );
        } catch ( ConfigurationException e ) {
            throw new RuntimeException( e );
        }
    }

}
