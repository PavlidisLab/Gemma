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
package ubic.gemma.loader.util.fetcher;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

import org.apache.commons.lang3.StringUtils;

import ubic.gemma.model.common.description.LocalFile;
import ubic.gemma.util.Settings;

/**
 * A generic class for fetching files via HTTP and writing them to a local file system.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class HttpFetcher extends AbstractFetcher {

    /**
     * @pram url
     * @see ubic.gemma.loader.loaderutils.Fetcher#fetch(java.lang.String)
     */
    @Override
    public Collection<LocalFile> fetch( String url ) {
        return fetch( url, null );
    }

    /**
     * @param url
     * @param outputFileName
     * @return
     */
    public Collection<LocalFile> fetch( String url, String outputFileName ) {
        log.info( "Seeking " + url );

        this.localBasePath = Settings.getDownloadPath();

        try {

            String host = ( new URL( url ) ).getHost();
            String filePath = ( new URL( url ) ).getPath();

            if ( StringUtils.isBlank( host ) ) {
                throw new IllegalArgumentException( url + " was not parsed into a valid URL" );
            }

            if ( StringUtils.isBlank( filePath ) ) {
                filePath = "index.html";
            }

            filePath = url.replace( host, "" );
            filePath = filePath.replace( "http://", "" );
            filePath = filePath.replace( '?', '_' );
            filePath = filePath.replace( '=', '_' );
            filePath = filePath.replace( '&', '_' );
            filePath = filePath.replace( '/', '_' );
            filePath = filePath.replaceFirst( "^_", "" );

            File newDir = mkdir( host );

            final String output;
            if ( outputFileName == null ) {
                output = formLocalFilePath( filePath, newDir );
            } else {
                output = outputFileName;
            }

            // FIXME if the file doesn't exist, we still get a response with a 404.
            FutureTask<Boolean> future = this.defineTask( output, url );
            return this.doTask( future, url, output );
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
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
     * @param outputFileName
     * @param seekFile
     * @return
     */
    protected FutureTask<Boolean> defineTask( final String outputFileName, final String seekFile ) {
        FutureTask<Boolean> future = new FutureTask<Boolean>( new Callable<Boolean>() {
            @Override
            @SuppressWarnings("synthetic-access")
            public Boolean call() throws FileNotFoundException, IOException {
                log.info( "Fetching " + seekFile );
                URL urlPattern = new URL( seekFile );

                InputStream inputStream = new BufferedInputStream( urlPattern.openStream() );
                OutputStream outputStream = new FileOutputStream( new File( outputFileName ) );

                final byte[] buffer = new byte[65536];
                int read = -1;

                while ( ( read = inputStream.read( buffer ) ) > -1 ) {
                    outputStream.write( buffer, 0, read );
                }
                outputStream.close();
                return Boolean.TRUE;
            }
        } );
        return future;
    }

    /**
     * @param future
     * @param seekFile
     * @param outputFileName
     * @return
     */
    protected Collection<LocalFile> doTask( FutureTask<Boolean> future, String seekFile, String outputFileName ) {
        Executors.newSingleThreadExecutor().execute( future );
        try {

            while ( !future.isDone() ) {
                try {
                    Thread.sleep( INFO_UPDATE_INTERVAL );
                } catch ( InterruptedException ie ) {

                }
                log.info( ( new File( outputFileName ).length() + " bytes read" ) );
            }
            if ( future.get().booleanValue() ) {
                return listFiles( seekFile, outputFileName );
            }
        } catch ( ExecutionException e ) {
            throw new RuntimeException( "Couldn't fetch file for " + seekFile, e );
        } catch ( InterruptedException e ) {
            throw new RuntimeException( "Interrupted: Couldn't fetch file for " + seekFile, e );
        } catch ( IOException e ) {
            throw new RuntimeException( "Couldn't fetch file for " + seekFile, e );
        }
        throw new RuntimeException( "Couldn't fetch file for " + seekFile );
    }

    /**
     * @param seekFile
     * @param outputFileName
     * @return
     * @throws IOException
     */
    protected Collection<LocalFile> listFiles( String seekFile, String outputFileName ) throws IOException {
        Collection<LocalFile> result = new HashSet<LocalFile>();
        File file = new File( outputFileName );
        log.info( "Downloaded: " + file );
        LocalFile newFile = LocalFile.Factory.newInstance();
        newFile.setLocalURL( file.toURI().toURL() );
        newFile.setRemoteURL( new URL( seekFile ) );
        newFile.setVersion( new SimpleDateFormat().format( new Date() ) );
        result.add( newFile );
        return result;
    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.loader.util.fetcher.AbstractFetcher#formRemoteFilePath(java.lang.String)
     */
    @Override
    protected String formRemoteFilePath( String identifier ) {
        return identifier;
    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.loader.util.fetcher.AbstractFetcher#initConfig()
     */
    @Override
    protected void initConfig() {
        return;
    }

}
