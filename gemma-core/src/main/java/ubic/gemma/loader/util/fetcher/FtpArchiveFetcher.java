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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.time.StopWatch;
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
    public Expand expander;
    protected boolean doDelete = false;

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
    @Override
    public void setForce( boolean force ) {
        this.force = force;
    }

    /**
     * @param outputFileName
     */
    protected void cleanUp( File outputFile ) {
        if ( this.doDelete ) {
            log.info( "Cleaning up " + outputFile.getName() );
            outputFile.delete();
        }
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
            } else if ( methodName.equals( "zip" ) ) {
                expander = null;
            } else {
                // tar...
                expander = new Untar();
                expander.setProject( new Project() );
                UntarCompressionMethod method = new UntarCompressionMethod();
                // method.setValue( methodName );
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
    protected Collection<LocalFile> listFiles( String remoteFile, File newDir, Collection<LocalFile> result )
            throws IOException {

        if ( result == null ) result = new HashSet<LocalFile>();
        for ( File file : FileTools.listDirectoryFiles( newDir ) ) {
            if ( excludePattern != null && file.getPath().endsWith( excludePattern ) ) continue;
            // log.info( "\t" + file.getCanonicalPath() );
            LocalFile newFile = LocalFile.Factory.newInstance();
            newFile.setLocalURL( file.toURI().toURL() );
            newFile.setRemoteURL( new File( remoteFile ).toURI().toURL() );
            newFile.setVersion( new SimpleDateFormat().format( new Date() ) );
            result.add( newFile );
        }

        // recurse into subdirectories.
        for ( File file : FileTools.listSubDirectories( newDir ) ) {
            listFiles( remoteFile, file, result );
        }
        return result;
    }

    @Override
    protected Collection<LocalFile> doTask( FutureTask<Boolean> future, long expectedSize, String seekFileName,
            String outputFileName ) {
        Executors.newSingleThreadExecutor().execute( future );
        try {
            File outputFile = new File( outputFileName );
            boolean ok = waitForDownload( future, expectedSize, outputFile );

            if ( !ok ) {
                // probably cancelled.
                return null;
            } else if ( future.get().booleanValue() ) {
                log.info( "Unpacking " + outputFile );
                unPack( outputFile );
                cleanUp( outputFile );
                if ( outputFile.isDirectory() ) return listFiles( seekFileName, outputFile, null );

                return listFiles( seekFileName, outputFile.getParentFile(), null );
            }
        } catch ( ExecutionException e ) {
            future.cancel( true );
            throw new RuntimeException( "Couldn't fetch " + seekFileName + " from "
                    + this.getNetDataSourceUtil().getHost(), e );
        } catch ( InterruptedException e ) {
            future.cancel( true );
            throw new RuntimeException( "Interrupted: Couldn't fetch " + seekFileName + " from "
                    + this.getNetDataSourceUtil().getHost(), e );
        } catch ( IOException e ) {
            future.cancel( true );
            throw new RuntimeException( "IOException: Couldn't fetch " + seekFileName + " from "
                    + this.getNetDataSourceUtil().getHost(), e );
        }
        future.cancel( true );
        throw new RuntimeException( "Couldn't fetch " + seekFileName + " from " + this.getNetDataSourceUtil().getHost() );
    }

    /**
     * @param newDir
     * @param seekFile
     */
    protected void unPack( final File toUnpack ) {
        FutureTask<Boolean> future = new FutureTask<Boolean>( new Callable<Boolean>() {
            @SuppressWarnings("synthetic-access")
            public Boolean call() {
                File extractedFile = new File( FileTools.chompExtension( toUnpack.getAbsolutePath() ) );
                /*
                 * Decide if an existing file is plausibly usable. Err on the side of caution.
                 */
                if ( allowUseExisting && extractedFile.canRead() && extractedFile.length() >= toUnpack.length()
                        && !FileUtils.isFileNewer( toUnpack, extractedFile ) ) {
                    log.warn( "Expanded file exists, skipping re-expansion: " + extractedFile );
                    return Boolean.TRUE;
                }

                if ( expander != null ) {
                    expander.setSrc( toUnpack );
                    expander.setDest( toUnpack.getParentFile() );
                    expander.perform();
                } else if ( toUnpack.getAbsolutePath().toLowerCase().endsWith( "zip" ) ) {
                    try {
                        FileTools.unZipFiles( toUnpack.getAbsolutePath() );
                    } catch ( IOException e ) {
                        throw new RuntimeException( e );
                    }

                } else { // gzip.
                    try {
                        FileTools.unGzipFile( toUnpack.getAbsolutePath() );
                    } catch ( IOException e ) {
                        throw new RuntimeException( e );
                    }
                }

                return Boolean.TRUE;
            }

        } );
        ExecutorService executor = Executors.newSingleThreadExecutor();

        executor.execute( future );
        executor.shutdown();

        StopWatch s = new StopWatch();
        s.start();
        while ( !future.isDone() && !future.isCancelled() ) {
            try {
                Thread.sleep( INFO_UPDATE_INTERVAL );
            } catch ( InterruptedException ie ) {
                future.cancel( true );
                return;
            }
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
