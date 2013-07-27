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
import java.util.Collection;

import org.apache.commons.configuration.ConfigurationException;

import ubic.gemma.loader.genome.gene.ncbi.NCBIUtil;
import ubic.gemma.loader.util.fetcher.FtpArchiveFetcher;
import ubic.gemma.model.common.description.LocalFile;
import ubic.gemma.util.Settings;

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
        return remoteBaseDir + "taxdump.tar.gz";
    }

    public TaxonFetcher() {
        super();
        this.setExcludePattern( ".gz" );
        initArchiveHandler( "tar.gz" );
    }

    @Override
    public void initConfig() {
        this.localBasePath = Settings.getString( "ncbi.local.datafile.basepath" );
        this.remoteBaseDir = Settings.getString( "ncbi.remote.taxon.basedir" );

        if ( remoteBaseDir == null )
            throw new RuntimeException( new ConfigurationException( "Failed to get basedir" ) );
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

    @Override 
    protected String formLocalFilePath( String unused, File newDir ) {
        return newDir + File.separator + "taxdump.tar.gz";
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.loader.util.fetcher.FtpFetcher#setNetDataSourceUtil()
     */
    @Override
    public void setNetDataSourceUtil() {
        this.netDataSourceUtil = new NCBIUtil();
    }
}
