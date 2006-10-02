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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.lang.StringUtils;

import ubic.basecode.util.FileTools;
import ubic.gemma.loader.util.fetcher.FtpArchiveFetcher;
import ubic.gemma.model.common.description.LocalFile;
import ubic.gemma.util.ConfigUtils;

/**
 * Class to download files for NCBI gene. Pass the name of the file (without the .gz) to the fetch method: for example,
 * gene_info.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class NCBIGeneFileFetcher extends FtpArchiveFetcher {

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
        initArchiveHandler( "gz" );
    }

    // /**
    // * @param file
    // * @return
    // */
    // public Collection<LocalFile> fetch( URL file ) {
    // if ( file == null ) {
    // throw new IllegalArgumentException();
    // }
    //
    // String[] chunks = StringUtils.splitByWholeSeparator( file.getPath(), "/" );
    // assert chunks.length > 1;
    // String identifier = FileTools.chompExtension( chunks[chunks.length - 1] );
    //
    // log.info( "Seeking Ncbi " + file.toString() + " file, using identifier " + identifier );
    //
    // try {
    // File newDir = mkdir( identifier );
    //
    // String outputFileName = newDir + File.separator + identifier + ".gz";
    //
    // log.warn( "output file name is " + outputFileName );
    //
    // OutputStream out = new FileOutputStream( new File( outputFileName ) );
    //
    // LocalFile localFile = LocalFile.Factory.newInstance();
    //
    // localFile.setLocalURL( ( new File( outputFileName ).toURI().toURL() ) );
    //
    // Collection<LocalFile> result = new HashSet<LocalFile>();
    //
    // result.add( localFile );
    //
    // InputStream is = file.openStream();
    // byte[] buf = new byte[1024];
    // int len;
    // while ( ( len = is.read( buf ) ) > 0 ) {
    // out.write( buf, 0, len );
    // }
    // is.close();
    //
    // String finalOutputPath = FileTools.unGzipFile( outputFileName );
    //
    // localFile.setLocalURL( ( new File( finalOutputPath ).toURI().toURL() ) );
    // localFile.setSize( new File( finalOutputPath ).length() );
    //
    //            return result;
    //
    //        } catch ( IOException e ) {
    //            throw new RuntimeException( e );
    //        }
    //    }

    /**
     * @param identifier
     * @param newDir
     * @return
     */
    protected String formLocalFilePath( String identifier, File newDir ) {
        return newDir + File.separator + identifier + ".gz";
    }

    /**
     * @param identifier
     * @return
     */
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
