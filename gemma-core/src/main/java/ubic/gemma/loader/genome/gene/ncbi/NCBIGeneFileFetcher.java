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
package ubic.gemma.loader.genome.gene.ncbi;

import java.io.File;

import org.apache.commons.configuration.ConfigurationException;

import ubic.gemma.loader.util.fetcher.FtpArchiveFetcher;
import ubic.gemma.util.ConfigUtils;

/**
 * Class to download files for NCBI gene. Pass the name of the file (without the .gz) to the fetch method: for example,
 * gene_info.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class NCBIGeneFileFetcher extends FtpArchiveFetcher {

    @Override
    public void initConfig() {
        this.localBasePath = ConfigUtils.getString( "ncbi.local.datafile.basepath" );
        this.remoteBaseDir = ConfigUtils.getString( "ncbi.remote.gene.basedir" );

        if ( remoteBaseDir == null )
            throw new RuntimeException( new ConfigurationException( "Failed to get basedir" ) );
        if ( localBasePath == null )
            throw new RuntimeException( new ConfigurationException( "Failed to get localBasePath" ) );

    }

    public NCBIGeneFileFetcher() {
        super();
        this.setExcludePattern( ".gz" );
        this.setAllowUseExisting( true );
        initArchiveHandler( "gz" );
    }

    /**
     * @param identifier
     * @param newDir
     * @return
     */
    @Override
    protected String formLocalFilePath( String identifier, File newDir ) {
        return newDir + File.separator + identifier + ".gz";
    }

    /**
     * @param identifier
     * @return
     */
    @Override
    protected String formRemoteFilePath( String identifier ) {
        return remoteBaseDir + identifier + ".gz";
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
