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

import java.io.File;
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
import org.apache.tools.ant.taskdefs.Expand;
import org.apache.tools.ant.taskdefs.Untar;
import org.apache.tools.ant.taskdefs.Untar.UntarCompressionMethod;

import ubic.basecode.util.FileTools;
import ubic.gemma.model.common.description.LocalFile;

/**
 * Fetcher that can fetch archives (e.g., tar.gz) and unpack them.
 * 
 * @author pavlidis
 * @version $Id$
 */
public abstract class FtpArchiveFetcher extends FtpFetcher implements ArchiveFetcher {

    private String excludePattern;

    protected static Log log = LogFactory.getLog( FtpArchiveFetcher.class.getName() );
    public Expand expander;
    protected boolean doDelete = false;
    protected FTPClient f;

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.loader.loaderutils.ArchiveFetcher#deleteAfterUnpack(boolean)
     */
    public void setDeleteAfterUnpack( boolean doDelete ) {
        this.doDelete = doDelete;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.loader.loaderutils.Fetcher#setForce(boolean)
     */
    public void setForce( boolean force ) {
        this.force = force;
    }

    /**
     * @param outputFileName
     */
    protected void cleanUp( File outputFile ) {
        if ( this.doDelete ) outputFile.delete();
    }

    /**
     * @param methodName e.g., "gzip". If null, ignored
     */
    protected void initArchiveHandler( String methodName ) {

        if ( methodName != null ) {
            if ( methodName.equals( "gz" ) ) {
                expander = null;
            } else if ( methodName.equals( "tar.gz" ) ) {
                expander = new Untar();
                expander.setProject( new Project() );
                UntarCompressionMethod method = new UntarCompressionMethod();
                method.setValue( "gzip" );
                ( ( Untar ) expander ).setCompression( method );
            } else {
                expander = new Untar();
                expander.setProject( new Project() );
                UntarCompressionMethod method = new UntarCompressionMethod();
                method.setValue( methodName );
                ( ( Untar ) expander ).setCompression( method );
            }
        }
    }

    /**
     * @param identifier
     * @param newDir
     * @param excludePattern A string used to filter the listed files (exclusion by file name ending). If null, not
     *        used. (FIXME: make this more flexible, case here is specific to ArrayExpress).
     * @return
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    protected Collection<LocalFile> listFiles( String remoteFile, File newDir ) throws IOException {

        Collection<LocalFile> result = new HashSet<LocalFile>();
        for ( File file : ( Collection<File> ) FileTools.listDirectoryFiles( newDir ) ) {
            if ( excludePattern != null && file.getPath().endsWith( excludePattern ) ) continue;
            log.info( "\t" + file.getCanonicalPath() );
            LocalFile newFile = LocalFile.Factory.newInstance();
            newFile.setLocalURL( file.toURI().toURL() );
            newFile.setRemoteURL( new File( remoteFile ).toURI().toURL() );
            newFile.setVersion( new SimpleDateFormat().format( new Date() ) );
            result.add( newFile );
        }
        return result;
    }

    @Override
    protected Collection<LocalFile> doTask( FutureTask<Boolean> future, long expectedSize, String seekFileName,
            String outputFileName ) {
        Executors.newSingleThreadExecutor().execute( future );
        try {
            File outputFile = new File( outputFileName );
            waitForDownload( future, expectedSize, outputFile );
            if ( future.get().booleanValue() ) {
                if ( log.isInfoEnabled() ) log.info( "Unpacking " + outputFile );
                unPack( outputFile );
                cleanUp( outputFile );
                if ( outputFile.isDirectory() ) return listFiles( seekFileName, outputFile );

                return listFiles( seekFileName, outputFile.getParentFile() );
            }
        } catch ( ExecutionException e ) {
            throw new RuntimeException( "Couldn't fetch " + seekFileName, e );
        } catch ( InterruptedException e ) {
            throw new RuntimeException( "Interrupted: Couldn't fetch " + seekFileName, e );
        } catch ( IOException e ) {
            throw new RuntimeException( "IOException: Couldn't fetch " + seekFileName, e );
        }
        throw new RuntimeException( "Couldn't fetch " + seekFileName );
    }

    /**
     * @param newDir
     * @param seekFile
     */
    protected void unPack( final File outputFile ) {
        FutureTask<Boolean> future = new FutureTask<Boolean>( new Callable<Boolean>() {
            @SuppressWarnings("synthetic-access")
            public Boolean call() {
                log.info( "Unpacking " + outputFile );
                if ( expander != null ) {
                    expander.setSrc( outputFile );
                    expander.setDest( outputFile.getParentFile() );
                    expander.perform();
                } else { // gzip.
                    try {
                        FileTools.unGzipFile( outputFile.getAbsolutePath() );
                    } catch ( IOException e ) {
                        throw new RuntimeException( e );
                    }
                }
                return Boolean.TRUE;
            }

        } );

        Executors.newSingleThreadExecutor().execute( future );

        StopWatch s = new StopWatch();
        s.start();
        while ( !future.isDone() ) {
            try {
                Thread.sleep( INFO_UPDATE_INTERVAL );
            } catch ( InterruptedException ie ) {
                ;
            }
            // log.info( FileTools.listDirectoryFiles( newDir ).size() - 1 + " files unpacked " );
            log.info( "Unpacking archive ... " + Math.floor( s.getTime() / 1000.0 ) + " seconds elapsed" );
        }
    }

    /**
     * @param excludePattern the excludePattern to set
     */
    public void setExcludePattern( String excludePattern ) {
        this.excludePattern = excludePattern;
    }

}
