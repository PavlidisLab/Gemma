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

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.net.ftp.FTP;

import ubic.basecode.util.NetUtils;
import ubic.gemma.loader.expression.geo.util.GeoUtil;
import ubic.gemma.loader.util.fetcher.FtpArchiveFetcher;
import ubic.gemma.model.common.description.LocalFile;
import ubic.gemma.util.ConfigUtils;

/**
 * Retrieve and unpack the raw data files for GEO series. These are the CEL and other files (RPT, EXP and maybe DAT) for
 * Affymetrix data sets. For other types of arrays there may also be raw data?
 * 
 * @author pavlidis
 * @version $Id$
 */
public class RawDataFetcher extends FtpArchiveFetcher {

    public RawDataFetcher() {
        super();
        this.setExcludePattern( ".tar" );
        initArchiveHandler( null );
    }

    @Override
    public final void setNetDataSourceUtil() {
        this.netDataSourceUtil = new GeoUtil();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.loader.loaderutils.Fetcher#fetch(java.lang.String)
     */
    public Collection<LocalFile> fetch( String identifier ) {
        try {
            if ( f == null || !f.isConnected() ) f = ( new GeoUtil() ).connect( FTP.BINARY_FILE_TYPE );
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
            long expectedSize = this.getExpectedSize( seekFile );
            FutureTask<Boolean> future = defineTask( outputFileName, seekFile );
            Collection<LocalFile> result = doTask( future, expectedSize, seekFile, outputFileName );
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
    protected String formLocalFilePath( String identifier, File newDir ) {
        return newDir + File.separator + identifier + "_RAW.tar";
    }

    /**
     * @param identifier
     * @return
     */
    protected String formRemoteFilePath( String identifier ) {
        String seekFile = remoteBaseDir + "/" + identifier + "/" + identifier + "_RAW.tar";
        return seekFile;
    }

    /**
     * @throws ConfigurationException
     */
    @Override
    public void initConfig() {
        localBasePath = ConfigUtils.getString( "geo.local.datafile.basepath" );
        remoteBaseDir = ConfigUtils.getString( "geo.remote.rawDataDir" );

        if ( localBasePath == null || localBasePath.length() == 0 )
            throw new RuntimeException( new ConfigurationException( "localBasePath was null or empty" ) );
        if ( remoteBaseDir == null || remoteBaseDir.length() == 0 )
            throw new RuntimeException( new ConfigurationException( "baseDir was null or empty" ) );

    }

}
