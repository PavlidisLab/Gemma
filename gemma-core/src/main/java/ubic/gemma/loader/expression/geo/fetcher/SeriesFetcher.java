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
import ubic.gemma.loader.util.fetcher.FtpFetcher;
import ubic.gemma.model.common.description.LocalFile;
import ubic.gemma.util.ConfigUtils;

public class SeriesFetcher extends FtpFetcher {

    /**
     * 
     */
    public SeriesFetcher() {
        this.localBasePath = ConfigUtils.getString( "geo.local.datafile.basepath" );
        this.baseDir = ConfigUtils.getString( "geo.remote.seriesDir" );
    }

    /**
     * @param accession
     * @throws SocketException
     * @throws IOException
     */
    public Collection<LocalFile> fetch( String accession ) {
        log.info( "Seeking GSE  file for " + accession );

        try {
            if ( this.f == null || !this.f.isConnected() ) f = GeoUtil.connect( FTP.BINARY_FILE_TYPE );
            File newDir = mkdir( accession );

            File outputFile = new File( newDir, accession + "_family.soft.gz" );
            String outputFileName = outputFile.getAbsolutePath();

            // String seekFile = baseDir + "/" + accession + "_family.soft.gz";
            String seekFile = baseDir + accession + "/" + accession + "_family.soft.gz";
            boolean success = NetUtils.ftpDownloadFile( f, seekFile, outputFile, force );

            if ( success ) {
                LocalFile file = fetchedFile( seekFile, outputFileName );
                log.info( "Retrieved " + seekFile + " for experiment(set) " + accession + " .Output file is "
                        + outputFileName );
                Collection<LocalFile> result = new HashSet<LocalFile>();
                result.add( file );
                return result;
            }
        } catch ( IOException e ) {
            log.error( e, e );
        }
        log.error( "Failed" );
        return null;

    }
}
