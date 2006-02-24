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
import java.util.concurrent.FutureTask;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.net.ftp.FTP;

import edu.columbia.gemma.common.description.LocalFile;
import edu.columbia.gemma.loader.loaderutils.FtpArchiveFetcher;

/**
 * Class to download files for NCBI gene. Pass the name of the file (without the .gz) to the fetch method: for example,
 * gene_info.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class NCBIGeneFileFetcher extends FtpArchiveFetcher {

    public void initConfig() {
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

    public NCBIGeneFileFetcher() {
        initConfig();
        initArchiveHandler( "gzip" );
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

            File newDir = mkdir( identifier );
            final String outputFileName = formLocalFilePath( identifier, newDir );
            final String seekFile = formRemoteFilePath( identifier );

            FutureTask<Boolean> future = this.defineTask( outputFileName, seekFile );
            return this.doTask( future, seekFile, outputFileName, identifier, newDir, ".gz" );
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    /**
     * @param identifier
     * @param newDir
     * @return
     */
    private String formLocalFilePath( String identifier, File newDir ) {
        String outputFileName = newDir + File.separator + identifier + ".gz";
        return outputFileName;
    }

    /**
     * @param identifier
     * @return
     */
    protected String formRemoteFilePath( String identifier ) {
        String seekFile = baseDir + identifier + ".gz";
        return seekFile;
    }

}
