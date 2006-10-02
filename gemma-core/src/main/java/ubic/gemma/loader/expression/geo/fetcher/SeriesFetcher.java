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
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.net.ftp.FTP;

import ubic.basecode.util.NetUtils;
import ubic.gemma.loader.expression.geo.util.GeoUtil;
import ubic.gemma.model.common.description.LocalFile;
import ubic.gemma.util.ConfigUtils;

public class SeriesFetcher extends GeoFetcher {

    // /**
    // * @param accession
    // * @throws SocketException
    // * @throws IOException
    // */
    // public Collection<LocalFile> fetch( String accession ) {
    // log.info( "Seeking GSE file for " + accession );
    //
    // try {
    // if ( this.ftpClient == null || !this.ftpClient.isConnected() )
    // ftpClient = ( new GeoUtil() ).connect( FTP.BINARY_FILE_TYPE );
    // File newDir = mkdir( accession );
    //
    // File outputFile = new File( newDir, accession + "_family.soft.gz" );
    // String outputFileName = outputFile.getAbsolutePath();
    //
    // String seekFile = formRemoteFilePath( accession );
    // boolean success = NetUtils.ftpDownloadFile( ftpClient, seekFile, outputFile, force );
    // ftpClient.disconnect();
    //
    // if ( success ) {
    // LocalFile file = fetchedFile( seekFile, outputFileName );
    // log.info( "Retrieved " + seekFile + " for experiment(set) " + accession + " .Output file is "
    // + outputFileName );
    // Collection<LocalFile> result = new HashSet<LocalFile>();
    // result.add( file );
    // return result;
    // }
    // } catch ( IOException e ) {
    // throw new RuntimeException( e );
    //        }
    //        log.error( "Failed" );
    //        return null;
    //
    //    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.loader.util.fetcher.AbstractFetcher#formRemoteFilePath(java.lang.String)
     */
    @Override
    protected String formRemoteFilePath( String identifier ) {
        return remoteBaseDir + identifier + "/" + identifier + "_family.soft.gz";
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.loader.util.fetcher.AbstractFetcher#initConfig()
     */
    @Override
    protected void initConfig() {
        this.localBasePath = ConfigUtils.getString( "geo.local.datafile.basepath" );
        this.remoteBaseDir = ConfigUtils.getString( "geo.remote.seriesDir" );
    }
}
