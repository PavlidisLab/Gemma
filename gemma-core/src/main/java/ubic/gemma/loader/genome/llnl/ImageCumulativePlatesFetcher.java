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
package ubic.gemma.loader.genome.llnl;

import java.io.File;

import org.apache.commons.configuration.ConfigurationException;

import ubic.gemma.loader.util.fetcher.FtpFetcher;
import ubic.gemma.util.ConfigUtils;

/**
 * Fetches from ftp://image.llnl.gov/image/outgoing/arrayed_plate_data/cumulative/. Identifier to pass is a date string
 * like 20060901.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class ImageCumulativePlatesFetcher extends FtpFetcher {

    public ImageCumulativePlatesFetcher() {
        this.force = false;
        initConfig();
    }

    @Override
    public void initConfig() {
        this.localBasePath = ConfigUtils.getString( "llnl.image.local.datafile.basepath" );
        this.remoteBaseDir = ConfigUtils.getString( "llnl.image.remote.gene.basedir" );

        if ( remoteBaseDir == null )
            throw new RuntimeException( new ConfigurationException( "Failed to get basedir" ) );
        if ( localBasePath == null )
            throw new RuntimeException( new ConfigurationException( "Failed to get localBasePath" ) );

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.loader.util.fetcher.AbstractFetcher#formLocalFilePath(java.lang.String, java.io.File)
     */
    @Override
    protected String formLocalFilePath( String identifier, File newDir ) {
        return newDir.getAbsolutePath() + File.separatorChar + "cumulative_arrayed_plates." + identifier + ".gz";
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.loader.util.fetcher.FtpFetcher#setNetDataSourceUtil()
     */
    @Override
    public void setNetDataSourceUtil() {
        this.netDataSourceUtil = new ImageLlnlUtil();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.loader.util.fetcher.AbstractFetcher#formRemoteFilePath(java.lang.String)
     */
    @Override
    protected String formRemoteFilePath( String identifier ) {
        return remoteBaseDir + "cumulative_arrayed_plates." + identifier + ".gz";
    }

}
