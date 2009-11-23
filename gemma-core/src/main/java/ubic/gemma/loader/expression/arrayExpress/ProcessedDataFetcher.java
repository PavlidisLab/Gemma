/*
 * The Gemma project
 * 
 * Copyright (c) 2007 University of British Columbia
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
package ubic.gemma.loader.expression.arrayExpress;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.configuration.ConfigurationException;

import ubic.gemma.loader.expression.arrayExpress.util.ArrayExpressUtil;
import ubic.gemma.loader.util.fetcher.FtpArchiveFetcher;
import ubic.gemma.model.common.description.LocalFile;
import ubic.gemma.util.ConfigUtils;

/**
 * Fetches the "processed data" for an ArrayExpress experiment.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class ProcessedDataFetcher extends FtpArchiveFetcher {

    /*
     * Note: Nov 2009. ArrayExpression changed the way they name files. The .1 is added. Probably there is .2
     * sometimes...
     */
    private static final String PROCESSED_DATA_SUFFIX = ".processed.1.zip";

    public ProcessedDataFetcher() {
        super();
        initArchiveHandler( "zip" );
    }

    /**
     * @param files
     * @return
     */
    public Collection<LocalFile> getProcessedDataFile( Collection<LocalFile> files ) {
        Collection<LocalFile> result = new HashSet<LocalFile>();
        for ( LocalFile file : files ) {
            if ( file.getLocalURL().toString().contains( "processed-data" ) ) {
                result.add( file );
            }
        }
        return result;
    }

    @Override
    public void setNetDataSourceUtil() {
        this.netDataSourceUtil = new ArrayExpressUtil();
    }

    @Override
    protected String formLocalFilePath( String identifier, File newDir ) {

        String outputFileName = newDir + System.getProperty( "file.separator" ) + identifier + PROCESSED_DATA_SUFFIX;
        return outputFileName;
    }

    /**
     * @param identifier - e.g. E-MEXP-955
     * @return
     */
    @Override
    public String formRemoteFilePath( String identifier ) {
        String dirName = identifier.replaceFirst( "-\\d+", "" );
        dirName = dirName.replace( "E-", "" );
        String seekFile = remoteBaseDir + "/" + dirName + "/" + identifier + "/" + identifier + PROCESSED_DATA_SUFFIX;
        return seekFile;
    }

    @Override
    protected void initConfig() {

        localBasePath = ConfigUtils.getString( "arrayExpress.local.datafile.basepath" );
        remoteBaseDir = ConfigUtils.getString( "arrayExpress.experiments.baseDir" );

        if ( localBasePath == null || localBasePath.length() == 0 )
            throw new RuntimeException( new ConfigurationException( "localBasePath was null or empty" ) );
        if ( remoteBaseDir == null || remoteBaseDir.length() == 0 )
            throw new RuntimeException( new ConfigurationException( "baseDir was null or empty" ) );

    }

}
