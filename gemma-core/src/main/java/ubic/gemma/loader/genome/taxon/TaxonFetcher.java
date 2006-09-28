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
package ubic.gemma.loader.genome.taxon;

import java.io.File;
import java.io.IOException;
import java.net.SocketException;
import java.util.Collection;
import java.util.concurrent.FutureTask;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.net.ftp.FTP;

import ubic.gemma.loader.genome.gene.ncbi.NCBIUtil;
import ubic.gemma.loader.util.fetcher.FtpArchiveFetcher;
import ubic.gemma.model.common.description.LocalFile;
import ubic.gemma.util.ConfigUtils;

/**
 * Taxon information from NCBI comes as a tar.gz archive; only the names.dmp file is of interest. From
 * ftp://ftp.ncbi.nih.gov/pub/taxonomy/taxdump.tar.gz
 * 
 * @author pavlidis
 * @version $Id$
 */
public class TaxonFetcher extends FtpArchiveFetcher {

    @Override
    protected String formRemoteFilePath( String identifier ) {
        return baseDir + "taxdump.tar.gz";
    }

    public TaxonFetcher() {
        super();
        initArchiveHandler( "tar.gz" );
    }

    @Override
    protected void initConfig() {
        this.localBasePath = ConfigUtils.getString( "ncbi.local.datafile.basepath" );
        this.baseDir = ConfigUtils.getString( "ncbi.remote.taxon.basedir" );

        if ( baseDir == null ) throw new RuntimeException( new ConfigurationException( "Failed to get basedir" ) );
        if ( localBasePath == null )
            throw new RuntimeException( new ConfigurationException( "Failed to get localBasePath" ) );
    }

    /**
     * Fetch the Taxon bundle from NCBI.
     * 
     * @return
     */
    public Collection<LocalFile> fetch() {
        return this.fetch( "taxon" );
    }

    /**
     * @param identifier ignored, as there is only one file.
     */
    public Collection<LocalFile> fetch( String identifier ) {
        try {
            if ( this.f == null || !this.f.isConnected() ) f = ( new NCBIUtil() ).connect( FTP.BINARY_FILE_TYPE );

            File newDir = mkdir( identifier );
            final String outputFileName = formLocalFilePath( newDir );
            final String seekFile = formRemoteFilePath( identifier );

            FutureTask<Boolean> future = this.defineTask( outputFileName, seekFile );
            long expectedSize = this.getExpectedSize( seekFile );
            return this.doTask( future, expectedSize, outputFileName, identifier, newDir, ".gz" );

        } catch ( SocketException e ) {
            throw new RuntimeException( e );
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    private String formLocalFilePath( File newDir ) {
        return newDir + File.separator + "taxdump.tar.gz";
    }
}
