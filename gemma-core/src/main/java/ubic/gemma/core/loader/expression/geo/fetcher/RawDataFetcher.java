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
package ubic.gemma.core.loader.expression.geo.fetcher;

import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.net.ftp.FTP;
import ubic.basecode.util.NetUtils;
import ubic.gemma.core.config.Settings;
import ubic.gemma.core.loader.expression.geo.service.GeoUtils;
import ubic.gemma.core.loader.expression.geo.util.GeoUtil;
import ubic.gemma.core.loader.util.fetcher.AbstractFetcher;
import ubic.gemma.core.loader.util.fetcher.FtpArchiveFetcher;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.Callable;

/**
 * Retrieve and unpack the raw data files for GEO series. These are the CEL and other files (RPT, EXP and maybe DAT) for
 * Affymetrix data sets. For other types of arrays there may also be raw data?
 *
 * @author pavlidis
 */
public class RawDataFetcher extends FtpArchiveFetcher {

    public RawDataFetcher() {
        super();
        this.setExcludePattern( ".tar" );
        this.initArchiveHandler( "tar" );
    }

    public boolean checkForFile( String identifier ) {
        try {
            if ( this.ftpClient == null || !this.ftpClient.isConnected() )
                this.ftpClient = ( new GeoUtil() ).connect( FTP.BINARY_FILE_TYPE );
            assert this.ftpClient != null;
            final String seekFile = this.formRemoteFilePath( identifier );
            try {
                NetUtils.checkForFile( this.ftpClient, seekFile );
                return true;
            } catch ( FileNotFoundException e ) {
                this.ftpClient.disconnect(); // important to do this!
                return false;
            }
        } catch ( Exception e ) {
            return false;
        }
    }

    /**
     * @param  identifier The url for the supplementary file.
     * @return local files
     */
    @Override
    public Collection<File> fetch( String identifier ) {
        try {
            if ( this.ftpClient == null || !this.ftpClient.isConnected() )
                this.ftpClient = ( new GeoUtil() ).connect( FTP.BINARY_FILE_TYPE );
            assert this.ftpClient != null;
            File newDir = this.mkdir( identifier );
            newDir = new File( newDir, "rawDataFiles" );
            if ( !newDir.canRead() && !newDir.mkdir() )
                throw new IOException( "Could not create the raw data subdirectory" );
            final String outputFileName = this.formLocalFilePath( identifier, newDir );
            final String seekFile = this.formRemoteFilePath( identifier );
            try {
                NetUtils.checkForFile( this.ftpClient, seekFile );
            } catch ( FileNotFoundException e ) {
                // that's okay, just return.
                AbstractFetcher.log
                        .info( "There is apparently no raw data archive for " + identifier + "(sought: " + seekFile
                                + ")" );
                if ( !newDir.delete() ) {
                    throw new IOException( "Could not delete file " + newDir.getPath() );
                }
                this.ftpClient.disconnect(); // important to do this!
                return null;
            }
            if ( this.ftpClient == null || !this.ftpClient.isConnected() ) {
                throw new IOException( "Lost FTP connection" );
            }
            long expectedSize = this.getExpectedSize( seekFile );
            Callable<Boolean> future = this.defineTask( outputFileName, seekFile );
            Collection<File> result = this.doTask( future, expectedSize, seekFile, outputFileName );

            if ( result == null || result.isEmpty() ) {
                throw new IOException( "Files were not obtained, or download was cancelled." );
            }

            log.info( result.size() + " files obtained from archive" );

            return result;
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }

    }

    @Override
    public final void setNetDataSourceUtil() {
        this.netDataSourceUtil = new GeoUtil();
    }

    @Override
    protected String formLocalFilePath( String identifier, File newDir ) {
        return newDir + File.separator + identifier + "_RAW.tar";
    }

    @Override
    protected String formRemoteFilePath( String identifier ) {
        return remoteBaseDir + "/" + GeoUtils.formShortenedFtpDirName( identifier ) + "/" + identifier + "/suppl/" + identifier + "_RAW.tar";
    }

    @Override
    public void initConfig() {
        localBasePath = Settings.getString( "geo.local.datafile.basepath" );
        remoteBaseDir = Settings.getString( "geo.remote.rawDataDir" );
        if ( StringUtils.isBlank( localBasePath ) )
            throw new RuntimeException( new ConfigurationException( "localBasePath was null or empty" ) );
        if ( StringUtils.isBlank( remoteBaseDir ) )
            throw new RuntimeException( new ConfigurationException( "baseDir was null or empty" ) );
    }
}
