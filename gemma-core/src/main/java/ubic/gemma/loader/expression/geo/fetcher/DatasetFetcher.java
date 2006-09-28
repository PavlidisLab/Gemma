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

/**
 * Retrieve GEO GDS files from the NCBI FTP server.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class DatasetFetcher extends GeoFetcher {
    /**
     * 
     */
    protected static final String SOFT_GZ = ".soft.gz";

    /**
     * @throws ConfigurationException
     */
    public DatasetFetcher() {
        this.localBasePath = ConfigUtils.getString( "geo.local.datafile.basepath" );
        this.baseDir = ConfigUtils.getString( "geo.remote.datasetDir" );
    }

    /**
     * @param accession
     */
    public Collection<LocalFile> fetch( String accession ) {
        log.info( "Seeking GDS file for " + accession );

        try {
            if ( ftpClient == null || !ftpClient.isConnected() )
                ftpClient = ( new GeoUtil() ).connect( FTP.BINARY_FILE_TYPE );
            String seekFile = baseDir + "/" + accession + SOFT_GZ;
            File newDir = mkdir( accession );
            File outputFile = new File( newDir, accession + SOFT_GZ );
            boolean success = NetUtils.ftpDownloadFile( ftpClient, seekFile, outputFile, force );
            ftpClient.disconnect();

            if ( success ) {
                // get meta-data about the file.
                LocalFile file = fetchedFile( seekFile, outputFile.getAbsolutePath() );
                log.info( "Got " + accession + SOFT_GZ + " for experiment(set) " + accession + ". Output file is "
                        + outputFile.getAbsolutePath() );

                // no need to unpack the file, we process as is.

                Collection<LocalFile> result = new HashSet<LocalFile>();
                result.add( file );
                return result;
            }
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
        throw new RuntimeException( "Couldn't find file for " + accession );
    }

}
