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
package ubic.gemma.core.loader.expression.arrayExpress;

import org.apache.commons.configuration2.ex.ConfigurationException;
import ubic.gemma.core.loader.expression.arrayExpress.util.ArrayExpressUtil;
import ubic.gemma.core.loader.util.fetcher.FtpArchiveFetcher;
import ubic.gemma.model.common.description.LocalFile;
import ubic.gemma.core.config.Settings;

import java.io.File;
import java.util.Collection;

/**
 * ArrayExpress stores files in an FTP site as tarred-gzipped archives. Each tar file contains the MAGE file and the
 * datacube external files. This class can download an experiment, unpack the tar file, and put the resulting files onto
 * a local filesystem.
 *
 * @author pavlidis
 */
@SuppressWarnings("unused") // Possible external use
public class DataFileFetcher extends FtpArchiveFetcher {

    /**
     * They seem to vary/change this. As of 1/2008, SMDB-14 had the .tgz suffix.
     */
    private static final String MAGE_ML_SUFFIX_A = ".mageml.tar.gz";
    private static final String MAGE_ML_SUFFIX_B = ".mageml.tgz";

    public DataFileFetcher() {
        super();
        this.setExcludePattern( MAGE_ML_SUFFIX_A );
        this.setExcludePattern( MAGE_ML_SUFFIX_B );
        initArchiveHandler( "tar.gz" );
    }

    @Override
    public Collection<LocalFile> fetch( String identifier ) {
        Collection<LocalFile> results;
        try {
            results = super.fetch( identifier );
        } catch ( Exception e ) {
            log.warn( "Trying alternative file name" );
            results = super.fetch( identifier, formRemoteFilePath( identifier, MAGE_ML_SUFFIX_A ) );
        }
        return results;
    }

    @Override
    public String formLocalFilePath( String identifier, File newDir ) {
        return newDir + System.getProperty( "file.separator" ) + identifier + MAGE_ML_SUFFIX_B;
    }

    /**
     * @param identifier - e.g. E-MEXP-955
     * @return remote file path
     */
    @Override
    public String formRemoteFilePath( String identifier ) {
        return formRemoteFilePath( identifier, MAGE_ML_SUFFIX_B );
    }

    public LocalFile getMageMlFile( Collection<LocalFile> files ) {
        for ( LocalFile file : files ) {
            if ( file.getLocalURL().toString().contains( ".xml" ) ) {
                return file;
            }
        }
        return null;
    }

    @Override
    public void initConfig() {

        localBasePath = Settings.getString( "arrayExpress.local.datafile.basepath" );
        remoteBaseDir = Settings.getString( "arrayExpress.experiments.baseDir" );

        if ( localBasePath == null || localBasePath.length() == 0 )
            throw new RuntimeException( new ConfigurationException( "localBasePath was null or empty" ) );
        if ( remoteBaseDir == null || remoteBaseDir.length() == 0 )
            throw new RuntimeException( new ConfigurationException( "baseDir was null or empty" ) );

    }

    @Override
    public void setNetDataSourceUtil() {
        this.netDataSourceUtil = new ArrayExpressUtil();

    }

    private String formRemoteFilePath( String identifier, String suffix ) {
        String dirName = identifier.replaceFirst( "-\\d+", "" );
        dirName = dirName.replace( "E-", "" );
        return remoteBaseDir + "/" + dirName + "/" + identifier + "/" + identifier + suffix;
    }
}
