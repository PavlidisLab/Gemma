/*
 * The Gemma project
 * 
 * Copyright (c) 2005 Columbia University
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
package edu.columbia.gemma.loader.expression.arrayExpress;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.net.ftp.FTP;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Untar;
import org.apache.tools.ant.taskdefs.Untar.UntarCompressionMethod;

import baseCode.util.FileTools;
import baseCode.util.NetUtils;
import edu.columbia.gemma.common.description.LocalFile;
import edu.columbia.gemma.loader.expression.arrayExpress.util.ArrayExpressUtil;
import edu.columbia.gemma.loader.loaderutils.FtpArchiveFetcher;

/**
 * ArrayExpress stores files in an FTP site as tarred-gzipped archives. Each tar file contains the MAGE file and the
 * datacube external files. This class can download an experiment, unpack the tar file, and put the resulting files onto
 * a local filesystem.
 * <p>
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 */
public class DataFileFetcher extends FtpArchiveFetcher {

    private final Untar untarrer;

    public DataFileFetcher() throws ConfigurationException {
        untarrer = new Untar();
        untarrer.setProject( new Project() );
        UntarCompressionMethod method = new UntarCompressionMethod();
        method.setValue( "gzip" );
        untarrer.setCompression( method );

        Configuration config = new PropertiesConfiguration( "Gemma.properties" );
        localBasePath = ( String ) config.getProperty( "arrayExpress.local.datafile.basepath" );
        baseDir = ( String ) config.getProperty( "arrayExpress.experiments.baseDir" );

    }

    /**
     * @param identifier The accession value for the experiment, such as "SMDB-14"
     * @param discardArchive Whether to delete the downloaded archive after unpacking its contents
     * @return
     * @throws SocketException
     * @throws IOException
     */
    public Collection<LocalFile> fetch( String identifier ) {

        try {
            if ( f == null || !f.isConnected() ) f = ArrayExpressUtil.connect( FTP.BINARY_FILE_TYPE );

            File newDir = mkdir( identifier );
            final String outputFileName = formLocalFilePath( identifier, newDir );
            final String seekFile = formRemoteFilePath( identifier );

            FutureTask<Boolean> future = new FutureTask<Boolean>( new Callable<Boolean>() {
                @SuppressWarnings("synthetic-access")
                public Boolean call() throws FileNotFoundException, IOException {
                    log.info( "Fetching " + seekFile );
                    return new Boolean( NetUtils.ftpDownloadFile( f, seekFile, outputFileName, force ) );
                }
            } );

            Executors.newSingleThreadExecutor().execute( future );

            while ( !future.isDone() ) {
                try {
                    Thread.sleep( 1000 );
                } catch ( InterruptedException ie ) {
                    ;
                }
                log.info( ( new File( outputFileName ).length() + " bytes read" ) );
            }
            if ( future.get().booleanValue() ) {
                log.info( "Unpacking " + outputFileName );
                unPack( newDir, outputFileName );
                cleanUp( outputFileName );
                Collection<LocalFile> result = listFiles( identifier, newDir );
                return result;
            }
            log.error( "Couldn't fetch file for " + identifier );
            return null;
        } catch ( ExecutionException e ) {
            log.error( "Couldn't fetch file for " + identifier, e );
            return null;
        } catch ( InterruptedException e ) {
            log.error( "Interrupted: Couldn't fetch file for " + identifier, e );
            return null;
        } catch ( IOException e ) {
            log.error( "Couldn't fetch file for " + identifier, e );
            return null;
        }

    }

    /**
     * @param files
     * @return
     */
    public LocalFile getMageMlFile( Collection<LocalFile> files ) {
        for ( LocalFile file : files ) {
            if ( file.getLocalURI().endsWith( ".xml" ) ) {
                return file;
            }
        }
        return null;
    }

    /**
     * @param discardArchive
     * @param outputFileName
     */
    private void cleanUp( String outputFileName ) {
        if ( this.doDelete ) ( new File( outputFileName ) ).delete();
    }

    /**
     * @param identifier
     * @param newDir
     * @return
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    private Collection<LocalFile> listFiles( String identifier, File newDir ) throws IOException {
        log.info( "Got files for experiment " + identifier + ":" );
        Collection<LocalFile> result = new HashSet<LocalFile>();
        for ( File file : ( Collection<File> ) FileTools.listDirectoryFiles( newDir ) ) {
            if ( file.getPath().endsWith( ".mageml.tgz" ) ) continue;
            log.info( "\t" + file.getCanonicalPath() );
            LocalFile newFile = LocalFile.Factory.newInstance();
            newFile.setLocalURI( file.getPath() );
            newFile.setRemoteURI( formRemoteFilePath( identifier ) );
            newFile.setVersion( new SimpleDateFormat().format( new Date() ) );
            result.add( newFile );
        }
        return result;
    }

    /**
     * @param newDir
     * @param seekFile
     */
    private void unPack( final File newDir, final String seekFile ) {
        FutureTask<Boolean> future = new FutureTask<Boolean>( new Callable<Boolean>() {
            @SuppressWarnings("synthetic-access")
            public Boolean call() {
                log.info( "Fetching " + seekFile );
                untarrer.setSrc( new File( seekFile ) );
                untarrer.setDest( newDir );
                untarrer.perform();
                return Boolean.TRUE;
            }
        } );

        Executors.newSingleThreadExecutor().execute( future );

        StopWatch s = new StopWatch();
        s.start();
        while ( !future.isDone() ) {
            try {
                Thread.sleep( 2000 );
            } catch ( InterruptedException ie ) {
                ;
            }
            // log.info( FileTools.listDirectoryFiles( newDir ).size() - 1 + " files unpacked " );
            log.info( "Unpacking archive ... " + Math.floor( s.getTime() / 1000.0 ) + " seconds elapsed" );
        }
    }

    /**
     * @param identifier
     * @param newDir
     * @return
     */
    private String formLocalFilePath( String identifier, File newDir ) {
        String outputFileName = newDir + System.getProperty( "file.separator" ) + "E-" + identifier + ".mageml.tgz";
        return outputFileName;
    }

    /**
     * @param identifier
     * @return
     */
    private String formRemoteFilePath( String identifier ) {
        String dirName = identifier.replaceFirst( "-\\d+", "" );
        String seekFile = baseDir + "/" + dirName + "/" + "E-" + identifier + "/" + "E-" + identifier + ".mageml.tgz";
        return seekFile;
    }
}
