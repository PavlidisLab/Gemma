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
package ubic.gemma.core.loader.util.fetcher;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import ubic.gemma.core.config.Settings;

import java.io.*;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

/**
 * A generic class for fetching files via HTTP and writing them to a local file system.
 *
 * @author pavlidis
 */
@SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
public class HttpFetcher extends AbstractFetcher {

    @Override
    public Collection<File> fetch( String url ) {
        return this.fetch( url, null );
    }

    public Collection<File> fetch( String url, String outputFileName ) {
        AbstractFetcher.log.info( "Seeking " + url );

        this.localBasePath = Settings.getDownloadPath();

        try {

            String host = ( new URL( url ) ).getHost();
            String filePath;

            if ( StringUtils.isBlank( host ) ) {
                throw new IllegalArgumentException( url + " was not parsed into a valid URL" );
            }

            filePath = url.replace( host, "" );
            filePath = filePath.replace( "http://", "" );
            filePath = filePath.replace( '?', '_' );
            filePath = filePath.replace( '=', '_' );
            filePath = filePath.replace( '&', '_' );
            filePath = filePath.replace( '/', '_' );
            filePath = filePath.replaceFirst( "^_", "" );

            File newDir = this.mkdir( host );

            final String output;
            if ( outputFileName == null ) {
                output = this.formLocalFilePath( filePath, newDir );
            } else {
                output = outputFileName;
            }

            FutureTask<Boolean> future = this.defineTask( output, url );
            return this.doTask( future, url, output );
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    protected FutureTask<Boolean> defineTask( final String outputFileName, final String seekFile ) {
        return new FutureTask<>( new Callable<Boolean>() {
            @Override
            @SuppressWarnings("synthetic-access")
            public Boolean call() throws IOException {
                AbstractFetcher.log.info( "Fetching " + seekFile );
                URL urlPattern = new URL( seekFile );

                try ( InputStream inputStream = urlPattern.openStream();
                        OutputStream outputStream = new FileOutputStream( outputFileName ) ) {
                    IOUtils.copy( inputStream, outputStream );
                    return Boolean.TRUE;
                }
            }
        } );
    }

    protected Collection<File> doTask( FutureTask<Boolean> future, String seekFile, String outputFileName ) {
        Executors.newSingleThreadExecutor().execute( future );
        try {

            while ( !future.isDone() ) {
                try {
                    Thread.sleep( AbstractFetcher.INFO_UPDATE_INTERVAL );
                } catch ( InterruptedException ignored ) {

                }
                AbstractFetcher.log.info( ( new File( outputFileName ).length() + " bytes read" ) );
            }
            if ( future.get() ) {
                return this.listFiles( seekFile, outputFileName );
            }
        } catch ( ExecutionException | IOException e ) {
            throw new RuntimeException( "Couldn't fetch file for " + seekFile, e );
        } catch ( InterruptedException e ) {
            throw new RuntimeException( "Interrupted: Couldn't fetch file for " + seekFile, e );
        }
        throw new RuntimeException( "Couldn't fetch file for " + seekFile );
    }

    @Override
    protected String formLocalFilePath( String identifier, File newDir ) {
        return newDir + File.separator + identifier;
    }

    @Override
    protected String formRemoteFilePath( String identifier ) {
        return identifier;
    }

    @Override
    protected void initConfig() {
    }

    protected Collection<File> listFiles( String seekFile, String outputFileName ) throws IOException {
        Collection<File> result = new HashSet<>();
        File file = new File( outputFileName );
        AbstractFetcher.log.info( "Downloaded: " + file );
        result.add( file );
        return result;
    }
}
