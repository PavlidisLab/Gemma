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

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Expand;
import org.apache.tools.ant.taskdefs.Untar;
import org.apache.tools.ant.taskdefs.Untar.UntarCompressionMethod;
import ubic.basecode.util.FileTools;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.*;

/**
 * Fetcher that can fetch archives (e.g., tar.gz) and unpack them.
 *
 * @author pavlidis
 */
@SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
public abstract class FtpArchiveFetcher extends FtpFetcher implements ArchiveFetcher {

    public Expand expander;
    protected boolean doDelete = false;
    private String excludePattern;

    @Override
    public void setDeleteAfterUnpack( boolean doDelete ) {
        this.doDelete = doDelete;
    }

    /**
     * @param excludePattern the excludePattern to set
     */
    public void setExcludePattern( String excludePattern ) {
        this.excludePattern = excludePattern;
    }

    @Override
    public void setForce( boolean force ) {
        this.force = force;
    }

    protected void cleanUp( File outputFile ) {
        if ( this.doDelete ) {
            AbstractFetcher.log.info( "Cleaning up " + outputFile.getName() );
            FileUtils.deleteQuietly( outputFile );
        }
    }

    @Override
    protected Collection<File> doTask( FutureTask<Boolean> future, long expectedSize, String seekFileName,
            String outputFileName ) {
        Executors.newSingleThreadExecutor().execute( future );
        try {
            File outputFile = new File( outputFileName );
            boolean ok = this.waitForDownload( future, expectedSize, outputFile );

            if ( !ok ) {
                // probably cancelled.
                return null;
            } else if ( future.get() ) {
                AbstractFetcher.log.info( "Unpacking " + outputFile );
                this.unPack( outputFile );
                this.cleanUp( outputFile );
                if ( outputFile.isDirectory() )
                    return this.listFiles( seekFileName, outputFile, null );

                return this.listFiles( seekFileName, outputFile.getParentFile(), null );
            }
        } catch ( ExecutionException e ) {
            future.cancel( true );
            throw new RuntimeException(
                    "Couldn't fetch " + seekFileName + " from " + this.getNetDataSourceUtil().getHost(), e );
        } catch ( InterruptedException e ) {
            future.cancel( true );
            throw new RuntimeException(
                    "Interrupted: Couldn't fetch " + seekFileName + " from " + this.getNetDataSourceUtil().getHost(),
                    e );
        } catch ( IOException e ) {
            future.cancel( true );
            throw new RuntimeException(
                    "IOException: Couldn't fetch " + seekFileName + " from " + this.getNetDataSourceUtil().getHost(),
                    e );
        }
        future.cancel( true );
        throw new RuntimeException(
                "Couldn't fetch " + seekFileName + " from " + this.getNetDataSourceUtil().getHost() );
    }

    /**
     * @param methodName e.g., "gzip". If null, ignored
     */
    protected void initArchiveHandler( String methodName ) {

        if ( methodName != null ) {
            switch ( methodName ) {
                case "gz":
                    expander = null;
                    break;
                case "tar.gz": {
                    expander = new Untar();
                    expander.setProject( new Project() );
                    UntarCompressionMethod method = new UntarCompressionMethod();
                    method.setValue( "gzip" );
                    ( ( Untar ) expander ).setCompression( method );
                    break;
                }
                case "zip":
                    expander = null;
                    break;
                default: {
                    // tar...
                    expander = new Untar();
                    expander.setProject( new Project() );
                    UntarCompressionMethod method = new UntarCompressionMethod();
                    // method.setValue( methodName );
                    ( ( Untar ) expander ).setCompression( method );
                    break;
                }
            }
        }
    }

    protected Collection<File> listFiles( String remoteFile, File newDir, Collection<File> result )
            throws IOException {

        if ( result == null )
            result = new HashSet<>();
        for ( File file : FileTools.listDirectoryFiles( newDir ) ) {
            if ( excludePattern != null && file.getPath().endsWith( excludePattern ) )
                continue;
            // log.info( "\t" + file.getCanonicalPath() );
            result.add( file );
        }

        // recurse into subdirectories.
        for ( File file : FileTools.listSubDirectories( newDir ) ) {
            this.listFiles( remoteFile, file, result );
        }
        return result;
    }

    protected void unPack( final File toUnpack ) {
        FutureTask<Boolean> future = new FutureTask<>( new Callable<Boolean>() {
            @Override
            @SuppressWarnings("synthetic-access")
            public Boolean call() {
                File extractedFile = new File( FileTools.chompExtension( toUnpack.getAbsolutePath() ) );
                /*
                 * Decide if an existing file is plausibly usable. Err on the side of caution.
                 */
                if ( allowUseExisting && extractedFile.canRead() && extractedFile.length() >= toUnpack.length()
                        && !FileUtils.isFileNewer( toUnpack, extractedFile ) ) {
                    AbstractFetcher.log.warn( "Expanded file exists, skipping re-expansion: " + extractedFile );
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
                Thread.sleep( AbstractFetcher.INFO_UPDATE_INTERVAL );
            } catch ( InterruptedException ie ) {
                future.cancel( true );
                return;
            }
            AbstractFetcher.log
                    .info( "Unpacking archive ... " + Math.floor( s.getTime() / 1000.0 ) + " seconds elapsed" );
        }
    }

}
