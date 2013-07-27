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
package ubic.gemma.loader.expression.arrayExpress;

import java.io.File;
import java.util.Collection;

import org.apache.commons.configuration.ConfigurationException;

import ubic.gemma.loader.expression.arrayExpress.util.ArrayExpressUtil;
import ubic.gemma.loader.util.fetcher.FtpArchiveFetcher;
import ubic.gemma.model.common.description.LocalFile;
import ubic.gemma.util.Settings;

/**
 * ArrayExpress stores files in an FTP site as tarred-gzipped archives. Each tar file contains the MAGE file and the
 * datacube external files. This class can download an experiment, unpack the tar file, and put the resulting files onto
 * a local filesystem.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class DataFileFetcher extends FtpArchiveFetcher {

    /*
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
        Collection<LocalFile> results = null;
        try {
            results = super.fetch( identifier );
        } catch ( Exception e ) {
            log.warn( "Trying alternative file name" );
            results = super.fetch( identifier, formRemoteFilePath( identifier, MAGE_ML_SUFFIX_A ) );
        }
        return results;
    }

    /**
     * @param files
     * @return
     */
    public LocalFile getMageMlFile( Collection<LocalFile> files ) {
        for ( LocalFile file : files ) {
            if ( file.getLocalURL().toString().contains( ".xml" ) ) {
                return file;
            }
        }
        return null;
    }

    /**
     * @param identifier
     * @param newDir
     * @return
     */
    @Override
    public String formLocalFilePath( String identifier, File newDir ) {
        String outputFileName = newDir + System.getProperty( "file.separator" ) + identifier + MAGE_ML_SUFFIX_B;
        return outputFileName;
    }

    /**
     * @param identifier - e.g. E-MEXP-955
     * @return
     */
    @Override
    public String formRemoteFilePath( String identifier ) {
        String suffix = MAGE_ML_SUFFIX_B;
        return formRemoteFilePath( identifier, suffix );
    }

    /**
     * @param identifier
     * @param suffix
     * @return
     */
    private String formRemoteFilePath( String identifier, String suffix ) {
        String dirName = identifier.replaceFirst( "-\\d+", "" );
        dirName = dirName.replace( "E-", "" );
        String seekFile = remoteBaseDir + "/" + dirName + "/" + identifier + "/" + identifier + suffix;
        return seekFile;
    }

    /**
     * @throws ConfigurationException
     */
    @Override
    public void initConfig() {

        localBasePath = Settings.getString( "arrayExpress.local.datafile.basepath" );
        remoteBaseDir = Settings.getString( "arrayExpress.experiments.baseDir" );

        if ( localBasePath == null || localBasePath.length() == 0 )
            throw new RuntimeException( new ConfigurationException( "localBasePath was null or empty" ) );
        if ( remoteBaseDir == null || remoteBaseDir.length() == 0 )
            throw new RuntimeException( new ConfigurationException( "baseDir was null or empty" ) );

    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.loader.util.fetcher.FtpFetcher#setNetDataSourceUtil()
     */
    @Override
    public void setNetDataSourceUtil() {
        this.netDataSourceUtil = new ArrayExpressUtil();

    }
}
