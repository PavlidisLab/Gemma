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

package ubic.gemma.core.loader.genome.gene.ncbi.homology;

import ubic.gemma.core.config.Settings;
import ubic.gemma.core.loader.genome.gene.ncbi.NCBIUtil;
import ubic.gemma.core.loader.util.fetcher.FtpFetcher;

import java.io.File;

/**
 * Grabs urls like ftp:///ftp.ncbi.nih.gov/pub/HomoloGene/current/homologene.data
 *
 * @author klc
 */
public class HomologeneFetcher extends FtpFetcher {

    private String remoteBaseDir = null;

    @Override
    protected void initConfig() {
        localBasePath = Settings.getString( "ncbi.local.homologene.basepath" );
        remoteBaseDir = Settings.getString( "ncbi.remote.homologene.basedir" );
    }

    @Override
    protected String formLocalFilePath( String identifier, File newDir ) {
        return newDir + File.separator + identifier;
    }

    @Override
    protected String formRemoteFilePath( String identifier ) {
        return remoteBaseDir + identifier;
    }

    @Override
    public void setNetDataSourceUtil() {
        this.netDataSourceUtil = new NCBIUtil();
    }
}
