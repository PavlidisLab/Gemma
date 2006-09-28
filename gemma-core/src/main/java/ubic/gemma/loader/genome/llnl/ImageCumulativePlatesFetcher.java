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
package ubic.gemma.loader.genome.llnl;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.net.ftp.FTP;

import ubic.basecode.util.NetUtils;
import ubic.gemma.loader.util.fetcher.FtpFetcher;
import ubic.gemma.model.common.description.LocalFile;
import ubic.gemma.util.ConfigUtils;

/**
 * Fetches from ftp://image.llnl.gov/image/outgoing/arrayed_plate_data/cumulative/. Identifier to pass is a date string
 * like 20060901.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class ImageCumulativePlatesFetcher extends FtpFetcher {

    public ImageCumulativePlatesFetcher() {
        initConfig();
    }

    protected void initConfig() {
        this.localBasePath = ConfigUtils.getString( "llnl.image.local.datafile.basepath" );
        this.baseDir = ConfigUtils.getString( "llnl.image.remote.gene.basedir" );

        if ( baseDir == null ) throw new RuntimeException( new ConfigurationException( "Failed to get basedir" ) );
        if ( localBasePath == null )
            throw new RuntimeException( new ConfigurationException( "Failed to get localBasePath" ) );

    }

    public Collection<LocalFile> fetch( String identifier ) {
        try {
            if ( this.ftpClient == null || !this.ftpClient.isConnected() )
                ftpClient = ( new ImageLlnlUtil() ).connect( FTP.BINARY_FILE_TYPE );
            File newDir = mkdir( identifier );

            File outputFile = new File( newDir, "cumulative_arrayed_plates." + identifier + ".gz" );
            String outputFileName = outputFile.getAbsolutePath();

            // String seekFile = baseDir + "/" + accession + "_family.soft.gz";
            String seekFile = baseDir + "cumulative_arrayed_plates." + identifier + ".gz";
            boolean success = NetUtils.ftpDownloadFile( ftpClient, seekFile, outputFile, force );
            ftpClient.disconnect();

            if ( success ) {
                LocalFile file = fetchedFile( seekFile, outputFileName );
                log.info( "Retrieved " + seekFile + " " + identifier + ". Output file is " + outputFileName );
                Collection<LocalFile> result = new HashSet<LocalFile>();
                result.add( file );
                return result;
            }
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
        log.error( "Failed" );
        return null;
    }

}
