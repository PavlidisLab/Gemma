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

import org.apache.commons.configuration.ConfigurationException;

import ubic.gemma.loader.expression.arrayExpress.util.ArrayExpressUtil;
import ubic.gemma.loader.util.fetcher.FtpFetcher;
import ubic.gemma.util.ConfigUtils;

/**
 * Grabs urls like http://www.ebi.ac.uk/microarray-as/ae/files/A-AFFY-45/A-AFFY-45.adf.txt
 * 
 * @author pavlidis
 * @version $Id$
 */
public class ArrayDesignFetcher extends FtpFetcher {

    /**
     * @param identifier
     * @param newDir
     * @suffix e.g. compositesequence, features, reporters
     * @return
     */
    protected String formLocalFilePath( String identifier, File newDir, String suffix ) {
        String outputFileName = newDir + System.getProperty( "file.separator" ) + identifier + "." + suffix;
        log.info( "Download to " + outputFileName );
        return outputFileName;
    }

    /**
     * @throws ConfigurationException
     */
    @Override
    protected void initConfig() {

        localBasePath = ConfigUtils.getString( "arrayExpress.local.datafile.basepath" );
        remoteBaseDir = ConfigUtils.getString( "arrayExpress.arraydesign.baseDir" );

        if ( localBasePath == null || localBasePath.length() == 0 )
            throw new RuntimeException( new ConfigurationException( "localBasePath was null or empty" ) );
        if ( remoteBaseDir == null || remoteBaseDir.length() == 0 )
            throw new RuntimeException( new ConfigurationException( "baseDir was null or empty" ) );

    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.loader.util.fetcher.AbstractFetcher#formLocalFilePath(java.lang.String, java.io.File)
     */
    @Override
    protected String formLocalFilePath( String identifier, File newDir ) {
        return newDir + File.separator + identifier + ".arrayDesignDetails.txt";
    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.loader.util.fetcher.AbstractFetcher#formRemoteFilePath(java.lang.String)
     */
    @Override
    protected String formRemoteFilePath( String identifier ) {
        String dirName = identifier.replaceFirst( "-\\d+", "" ).replaceFirst( "A-", "" );
        String seekFile = remoteBaseDir + dirName + "/" + identifier + "/" + identifier + ".adf.txt";
        return seekFile;
    }

    @Override
    public void setNetDataSourceUtil() {
        this.netDataSourceUtil = new ArrayExpressUtil();

    }

}
