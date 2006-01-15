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
package edu.columbia.gemma.loader.genome.gene.ncbi;

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
import edu.columbia.gemma.loader.loaderutils.FtpFetcher;

/**
 * Class to download files for NCBI gene. Pass the name of the file (without the .gz) to the fetch method: for example,
 * gene_info.
 * <hr>
 * <p>
 * Copyright (c) 2004-2006 University of British Columbia
 * 
 * @author pavlidis
 * @version $Id$
 */
public class NCBIGeneFileFetcher extends FtpFetcher {

    public NCBIGeneFileFetcher() {
        Configuration config;
        try {
            config = new PropertiesConfiguration( "Gemma.properties" );
            this.localBasePath = ( String ) config.getProperty( "ncbi.local.datafile.basepath" );
            this.baseDir = ( String ) config.getProperty( "ncbi.remote.gene.basedir" );

            if ( baseDir == null ) throw new ConfigurationException( "Failed to get basedir" );
            if ( localBasePath == null ) throw new ConfigurationException( "Failed to get localBasePath" );
        } catch ( ConfigurationException e ) {
            throw new RuntimeException( e );
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.columbia.gemma.loader.loaderutils.Fetcher#fetch(java.lang.String)
     */
    public Collection<LocalFile> fetch( String identifier ) {
        log.info( "Seeking Ncbi " + identifier + " file " );

        try {
            if ( this.f == null || !this.f.isConnected() ) f = NCBIUtil.connect( FTP.BINARY_FILE_TYPE );

            String seekFile = baseDir + "/" + identifier + ".gz";
            File outputDir = new File( this.localBasePath );
            String outputFileName = null;
            if ( !outputDir.canWrite() ) {
                outputFileName = File.createTempFile( identifier, "gz" ).getAbsolutePath();
            } else {
                outputFileName = outputDir + File.separator + identifier + ".gz";
            }
            success = NetUtils.ftpDownloadFile( f, seekFile, outputFileName, force );

            if ( success ) {
                // get meta-data about the file.
                LocalFile file = fetchedFile( seekFile, outputFileName );
                log.info( "Retrieved " + seekFile + ", output file is " + outputFileName );
                Collection<LocalFile> result = new HashSet<LocalFile>();
                result.add( file );
                return result;
            }
            throw new IOException( "Failed to get the file" );
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
    }

 
}
