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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;

import ubic.basecode.util.NetUtils;
import ubic.gemma.loader.expression.arrayExpress.util.ArrayExpressUtil;
import ubic.gemma.loader.util.fetcher.FtpFetcher;
import ubic.gemma.model.common.description.LocalFile;
import ubic.gemma.util.ConfigUtils;

/**
 * FIXME defunct. Should grab urls like http://www.ebi.ac.uk/microarray-as/aer/lob?name=adss&id=181940719, which have to
 * be scraped from pages like
 * http://www.ebi.ac.uk/microarray-as/aer/result?queryFor=PhysicalArrayDesign&aAccession=A-MEXP-15
 * <p>
 * Fetch files for ArrayDesigns from ArrayExpress. Examples:
 * <ul>
 * <li>ftp://ftp.ebi.ac.uk/pub/databases/microarray/data/array/FPMI/A-FPMI-3 (note no composite sequences)
 * <li>ftp://ftp.ebi.ac.uk/pub/databases/microarray/data/array/AFFY/A-AFFY-18 (note no mageml)
 * </ul>
 * 
 * @author pavlidis
 * @version $Id$
 */
public class ArrayDesignFetcher extends FtpFetcher {

    private static final String[] suffixes = { "compositesequences.txt", "reporters.txt" };

    /*
     * (non-Javadoc)
     * @see ubic.gemma.loader.util.fetcher.Fetcher#fetch(java.lang.String)
     */
    @Override
    public Collection<LocalFile> fetch( final String identifier ) {

        ExecutorService service = Executors.newFixedThreadPool( 3 );
        final Collection<LocalFile> results = new HashSet<LocalFile>();

        Collection<Future> futures = new HashSet<Future>();

        for ( String suf : suffixes ) {
            final String suffix = suf;
            log.info( "Attempting Download of " + suffix + " for " + identifier );
            FutureTask<Boolean> fetchThread = new FutureTask<Boolean>( new Callable<Boolean>() {
                @SuppressWarnings("synthetic-access")
                public Boolean call() throws IOException {

                    File newDir = mkdir( identifier );
                    final String outputFileName = formLocalFilePath( identifier, newDir, suffix );
                    final String seekFile = formRemoteFilePath( identifier, suffix );

                    FTPClient client = null;

                    int tries = 0;
                    while ( client == null || !client.isConnected() ) {
                        if ( tries > 3 ) {
                            throw new IOException( "Couldn't get connection" );
                        }
                        client = getNetDataSourceUtil().connect( FTP.BINARY_FILE_TYPE );
                        tries++;
                    }

                    long size = -1;

                    try {
                        size = NetUtils.checkForFile( client, seekFile );

                        LocalFile newFile = LocalFile.Factory.newInstance();
                        newFile.setLocalURL( new File( outputFileName ).toURI().toURL() );
                        newFile.setRemoteURL( new File( formRemoteFilePath( identifier, suffix ) ).toURI().toURL() );
                        newFile.setVersion( new SimpleDateFormat().format( new Date() ) );
                        newFile.setSize( size );
                        results.add( newFile );

                        log.info( "Need file of " + size + " bytes" );
                    } catch ( FileNotFoundException e ) {
                        log.info( seekFile + " Not found on " + ConfigUtils.getString( "arrayExpress.host" ) );
                        // client.disconnect(); // might be okay.
                        return false;
                    }

                    boolean ok = NetUtils.ftpDownloadFile( client, seekFile, outputFileName, isForce() );

                    if ( !ok ) {
                        log.error( "Didn't get file" );
                        client.disconnect();
                        return false;
                    }

                    client.disconnect();
                    return true;

                }
            } );

            futures.add( fetchThread );
            service.execute( fetchThread );

        }

        service.shutdown();

        NumberFormat form = NumberFormat.getPercentInstance();
        while ( !service.isTerminated() ) {

            form.setMinimumFractionDigits( 2 );

            try {
                Thread.sleep( 3000 );
                for ( LocalFile file : results ) {
                    long expectedSize = file.getSize();
                    long actualSize = file.asFile().length();

                    if ( expectedSize == actualSize ) {
                        continue;
                    }

                    log.info( actualSize + "/" + expectedSize + " bytes ("
                            + form.format( ( double ) actualSize / expectedSize ) + ") downloaded for "
                            + file.asFile().getAbsolutePath() );
                }
            } catch ( InterruptedException e ) {
                //
            }
        }

        // just let us know it's done.
        for ( LocalFile file : results ) {
            long expectedSize = file.getSize();
            long actualSize = file.asFile().length();
            log.info( actualSize + "/" + expectedSize + " bytes (" + form.format( ( double ) actualSize / expectedSize )
                    + ") downloaded for " + file.asFile().getAbsolutePath() );
        }
        log.info( "Done" );

        return results;

    }

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
     * @param identifier
     * @suffix e.g. compositesequences, features, reporters
     * @return
     */
    protected String formRemoteFilePath( String identifier, String suffix ) {
        String dirName = identifier.replaceFirst( "-\\d+", "" ).replaceFirst( "A-", "" );
        String seekFile = remoteBaseDir + dirName + "/" + identifier + "/" + identifier + "." + suffix;
        return seekFile;
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
    @SuppressWarnings("unused")
    protected String formLocalFilePath( String identifier, File newDir ) {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.loader.util.fetcher.FtpFetcher#setNetDataSourceUtil()
     */
    @Override
    public void setNetDataSourceUtil() {
        this.netDataSourceUtil = new ArrayExpressUtil();
    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.loader.util.fetcher.AbstractFetcher#formRemoteFilePath(java.lang.String)
     */
    @Override
    @SuppressWarnings("unused")
    protected String formRemoteFilePath( String identifier ) {
        throw new UnsupportedOperationException();
    }

}
