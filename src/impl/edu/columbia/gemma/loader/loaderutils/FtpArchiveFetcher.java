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
package edu.columbia.gemma.loader.loaderutils;

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

import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Untar;
import org.apache.tools.ant.taskdefs.Untar.UntarCompressionMethod;

import baseCode.util.FileTools;
import baseCode.util.NetUtils;
import edu.columbia.gemma.common.description.LocalFile;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 */
public abstract class FtpArchiveFetcher extends AbstractFetcher implements ArchiveFetcher {
    protected static Log log = LogFactory.getLog( FtpArchiveFetcher.class.getName() );
    public Untar untarrer;
    protected boolean doDelete = false;
    protected FTPClient f;

    /*
     * (non-Javadoc)
     * 
     * @see edu.columbia.gemma.loader.loaderutils.ArchiveFetcher#deleteAfterUnpack(boolean)
     */
    @SuppressWarnings("hiding")
    public void setDeleteAfterUnpack( boolean doDelete ) {
        this.doDelete = doDelete;
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.columbia.gemma.loader.loaderutils.Fetcher#setForce(boolean)
     */
    public void setForce( boolean force ) {
        this.force = force;
    }

    /**
     * @param outputFileName
     */
    protected void cleanUp( String outputFileName ) {
        if ( this.doDelete ) ( new File( outputFileName ) ).delete();
    }

    /**
     * @param outputFileName
     * @param seekFile
     * @return
     */
    protected FutureTask<Boolean> defineTask( final String outputFileName, final String seekFile ) {
        FutureTask<Boolean> future = new FutureTask<Boolean>( new Callable<Boolean>() {
            @SuppressWarnings("synthetic-access")
            public Boolean call() throws FileNotFoundException, IOException {
                log.info( "Fetching " + seekFile );
                return new Boolean( NetUtils.ftpDownloadFile( f, seekFile, outputFileName, force ) );
            }
        } );
        return future;
    }

    /**
     * @param future
     * @param outputFileName
     * @param identifier
     * @param newDir
     * @param excludePattern
     * @return
     */
    protected Collection<LocalFile> doTask( FutureTask<Boolean> future, String outputFileName, String identifier,
            File newDir, String excludePattern ) {
        Executors.newSingleThreadExecutor().execute( future );
        try {
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
                return listFiles( identifier, newDir, excludePattern );
            }
        } catch ( ExecutionException e ) {
            throw new RuntimeException( "Couldn't fetch file for " + identifier, e );
        } catch ( InterruptedException e ) {
            throw new RuntimeException( "Interrupted: Couldn't fetch file for " + identifier, e );
        } catch ( IOException e ) {
            throw new RuntimeException( "Couldn't fetch file for " + identifier, e );
        }
        throw new RuntimeException( "Couldn't fetch file for " + identifier );
    }

    protected abstract String formRemoteFilePath( String identifier );

    /**
     * @param methodName e.g., "gzip". If null, ignored
     */
    protected void initArchiveHandler( String methodName ) {
        untarrer = new Untar();
        untarrer.setProject( new Project() );
        if ( methodName != null ) {
            UntarCompressionMethod method = new UntarCompressionMethod();
            method.setValue( methodName );
            untarrer.setCompression( method );
        }
    }

    protected abstract void initConfig();

    /**
     * @param identifier
     * @param newDir
     * @param excludePattern A string used to filter the listed files (exclusion by file name ending). If null, not
     *        used. (FIXME: make this more flexible, case here is specific to ArrayExpress).
     * @return
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    protected Collection<LocalFile> listFiles( String identifier, File newDir, String excludePattern )
            throws IOException {
        log.info( "Got files for experiment " + identifier + ":" );
        Collection<LocalFile> result = new HashSet<LocalFile>();
        for ( File file : ( Collection<File> ) FileTools.listDirectoryFiles( newDir ) ) {
            if ( excludePattern != null && file.getPath().endsWith( excludePattern ) ) continue;
            log.info( "\t" + file.getCanonicalPath() );
            LocalFile newFile = LocalFile.Factory.newInstance();
            newFile.setLocalURI( file.getPath() );
            newFile.setRemoteURI( formRemoteFilePath( identifier ) );
            newFile.setVersion( new SimpleDateFormat().format( new Date() ) );
            result.add( newFile );
        }
        return result;
    }

    protected void unPack( final File newDir, final String seekFile ) {
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

}
