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
package ubic.gemma.core.loader.genome.gene.ncbi;

import ubic.gemma.core.config.Settings;
import ubic.gemma.core.loader.util.fetcher.FtpArchiveFetcher;

import java.io.File;

/**
 * Class to download files for NCBI gene. Pass the name of the file (without the .gz) to the fetch method: for example,
 * gene_info.
 *
 * @author pavlidis
 */
public class NCBIGeneFileFetcher extends FtpArchiveFetcher {

    private String remoteBaseDir;

    public NCBIGeneFileFetcher() {
        super();
        this.setExcludePattern( ".gz" );
        this.setAllowUseExisting( true );
        initArchiveHandler( "gz" );
    }

    @Override
    public void initConfig() {
        this.localBasePath = Settings.getString( "ncbi.local.datafile.basepath" );
        this.remoteBaseDir = Settings.getString( "ncbi.remote.gene.basedir" );
    }

    @Override
    protected String formLocalFilePath( String identifier, File newDir ) {
        return newDir + File.separator + identifier + ".gz";
    }

    @Override
    protected String formRemoteFilePath( String identifier ) {
        return remoteBaseDir + identifier + ".gz";
    }

    @Override
    public void setNetDataSourceUtil() {
        this.netDataSourceUtil = new NCBIUtil();
    }

    public void setDoDownload( boolean doDownload ) {
        this.setAvoidDownload( !doDownload );
    }

}
