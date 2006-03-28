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

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.net.ftp.FTP;

import ubic.basecode.util.NetUtils;
import ubic.gemma.loader.expression.geo.util.GeoUtil;
import ubic.gemma.model.common.description.LocalFile;

/**
 * Fetch GEO "GPLXXX_family.soft.gz" files
 * 
 * @author pavlidis
 * @version $Id$
 */
public class PlatformFetcher extends GeoFetcher {

    /**
     * 
     */
    protected static final String SOFT_GZ = ".soft.gz";

    /**
     * @throws ConfigurationException
     */
    public PlatformFetcher() throws ConfigurationException {
        Configuration config = new PropertiesConfiguration( "Gemma.properties" );

        this.localBasePath = ( String ) config.getProperty( "geo.local.datafile.basepath" );
        this.baseDir = ( String ) config.getProperty( "geo.remote.platformDir" );
        if ( baseDir == null ) {
            throw new ConfigurationException( "geo.remote.platformDir was not defined in resource bundle" );
        }
    }

    /**
     * @param accession
     */
    public Collection<LocalFile> fetch( String accession ) {
        log.info( "Seeking file for " + accession );

        try {
            if ( f == null || !f.isConnected() ) f = GeoUtil.connect( FTP.BINARY_FILE_TYPE );
            String seekFile = baseDir + "/" + accession + "_family" + SOFT_GZ;
            File newDir = mkdir( accession );
            File outputFile = new File( newDir, accession + SOFT_GZ );
            success = NetUtils.ftpDownloadFile( f, seekFile, outputFile, force );
            f.disconnect();

            if ( success ) {
                // get meta-data about the file.
                LocalFile file = fetchedFile( seekFile, outputFile.getAbsolutePath() );
                log.info( "Got " + accession + SOFT_GZ + " for platform " + accession + ". Output file is "
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
