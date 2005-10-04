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
package edu.columbia.gemma.loader.genome.gene.ncbi;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
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
 * Copyright (c) 2004-2005 Columbia University
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
            String outputFileName = this.localBasePath + identifier + ".gz";
            success = NetUtils.ftpDownloadFile( f, seekFile, outputFileName, force );

            if ( success ) {
                // get meta-data about the file.
                LocalFile file = LocalFile.Factory.newInstance();
                file.setVersion( new SimpleDateFormat().format( new Date() ) );
                file.setRemoteURI( seekFile );
                file.setLocalURI( "file://" + outputFileName.replaceAll( "\\\\", "/" ) );
                // file.setSize( outputFile.length() );
                log.info( "Retrieved " + seekFile + ", output file is " + outputFileName );
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
