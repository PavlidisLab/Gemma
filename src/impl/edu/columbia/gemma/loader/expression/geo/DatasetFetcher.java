/*
 * The Gemma project
 * 
 * Copyright (c) 2005 Columbia University
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
package edu.columbia.gemma.loader.expression.geo;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.net.ftp.FTP;

import baseCode.util.NetUtils;
import edu.columbia.gemma.common.description.LocalFile;
import edu.columbia.gemma.loader.expression.geo.util.GeoUtil;
import edu.columbia.gemma.loader.loaderutils.FtpFetcher;

/**
 * Retrieve GEO files from the NCBI FTP server.
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 */
public class DatasetFetcher extends FtpFetcher {
    /**
     * @throws ConfigurationException
     */
    public DatasetFetcher() throws ConfigurationException {
        Configuration config = new PropertiesConfiguration( "Gemma.properties" );

        this.localBasePath = ( String ) config.getProperty( "geo.local.datafile.basepath" );
        this.baseDir = ( String ) config.getProperty( "geo.remote.datasetDir" );
    }

    /**
     * @param accession
     */
    public Collection<LocalFile> fetch( String accession ) {
        log.info( "Seeking GDS file for " + accession );

        try {
            if ( f == null || !f.isConnected() ) f = GeoUtil.connect( FTP.BINARY_FILE_TYPE );
            String seekFile = baseDir + "/" + accession + ".soft.gz";
            File newDir = mkdir( accession );
            String outputFileName = newDir + "/" + accession + ".soft.gz";
            success = NetUtils.ftpDownloadFile( f, seekFile, outputFileName, force );
            f.disconnect();

            if ( success ) {
                // get meta-data about the file.
                LocalFile file = fetchedFile( seekFile, outputFileName );
                log.info( "Got " + accession + ".xls.gz" + " for experiment(set) " + accession + ". Output file is "
                        + outputFileName );

                // no need to unpack the file, we process as is.

                Collection<LocalFile> result = new HashSet<LocalFile>();
                result.add( file );
                return result;
            }
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
        log.error( "Couldn't find file." );
        return null;

    }

}
