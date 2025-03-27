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
import ubic.gemma.model.common.description.LocalFile;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.concurrent.*;

/**
 * A generic class for fetching files via HTTP and writing them to a local file system.
 *
 * @author pavlidis
 */
@SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
public class HttpFetcher extends AbstractFetcher {

    @Override
    public Collection<LocalFile> fetch( String url ) {
        return this.fetch( url, null );
    }

    public Collection<LocalFile> fetch( String url, String outputFileName ) {
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

            Future<?> future = Executors.newSingleThreadExecutor().submit( () -> {
                AbstractFetcher.log.info( "Fetching " + url );
                try ( InputStream inputStream = new URL( url ).openStream();
                        OutputStream outputStream = new FileOutputStream( output ) ) {
                    IOUtils.copy( inputStream, outputStream );
                } catch ( IOException e ) {
                    throw new RuntimeException( e );
                }
            } );
            while ( true ) {
                try {
                    future.get( AbstractFetcher.INFO_UPDATE_INTERVAL, TimeUnit.MILLISECONDS );
                    return this.listFiles( url, output );
                } catch ( TimeoutException ignored ) {
                    AbstractFetcher.log.info( ( new File( output ).length() + " bytes read" ) );
                } catch ( InterruptedException e ) {
                    future.cancel( true );
                    throw new RuntimeException( "Interrupted: Couldn't fetch file for " + url, e );
                } catch ( ExecutionException | IOException e ) {
                    throw new RuntimeException( "Couldn't fetch file for " + url, e );
                }
            }
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
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

    protected Collection<LocalFile> listFiles( String seekFile, String outputFileName ) throws IOException {
        Collection<LocalFile> result = new HashSet<>();
        File file = new File( outputFileName );
        AbstractFetcher.log.info( "Downloaded: " + file );
        LocalFile newFile = LocalFile.Factory.newInstance();
        newFile.setLocalURL( file.toURI() );
        newFile.setRemoteURL( URI.create( seekFile ) );
        newFile.setVersion( new SimpleDateFormat().format( new Date() ) );
        result.add( newFile );
        return result;
    }

}
