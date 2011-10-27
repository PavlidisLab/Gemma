/*
 * The Gemma project
 * 
 * Copyright (c) 2009 University of British Columbia
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

package ubic.gemma.loader.genome.gene.ncbi.homology;

import java.io.File;

import org.apache.commons.configuration.ConfigurationException;

import ubic.gemma.loader.genome.gene.ncbi.NCBIUtil;
import ubic.gemma.loader.util.fetcher.FtpFetcher;
import ubic.gemma.util.ConfigUtils;

/**
 * Grabs urls like ftp:///ftp.ncbi.nih.gov/pub/HomoloGene/current/homologene.data
 * 
 * @author klc
 * @version $Id: HomologeneFetcher.java
 */
public class HomologeneFetcher extends FtpFetcher {

    /**
     * @throws ConfigurationException
     */
    @Override
    protected void initConfig() {

        localBasePath = ConfigUtils.getString( "ncbi.local.homologene.basepath" );
        remoteBaseDir = ConfigUtils.getString( "ncbi.remote.homologene.basedir" );

        if ( localBasePath == null || localBasePath.length() == 0 )
            throw new RuntimeException( new ConfigurationException( "localBasePath was null or empty" ) );
        if ( remoteBaseDir == null || remoteBaseDir.length() == 0 )
            throw new RuntimeException( new ConfigurationException( "baseDir was null or empty" ) );

    }

    /**
     * @param identifier
     * @param newDir
     * @return
     */
    @Override
    protected String formLocalFilePath( String identifier, File newDir ) {
        return newDir + File.separator + identifier;
    }

    /**
     * @param identifier
     * @return
     */
    @Override
    protected String formRemoteFilePath( String identifier ) {
        return remoteBaseDir + identifier;
    }

    @Override
    public void setNetDataSourceUtil() {
        this.netDataSourceUtil = new NCBIUtil();

    }

}
