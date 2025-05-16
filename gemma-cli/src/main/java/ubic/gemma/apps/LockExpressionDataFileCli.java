package ubic.gemma.apps;

import org.apache.commons.cli.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import ubic.gemma.core.analysis.service.ExpressionDataFileService;
import ubic.gemma.core.analysis.service.ExpressionDataFileUtils;
import ubic.gemma.core.util.locking.FileLockInfoUtils;
import ubic.gemma.core.util.locking.FileLockManager;
import ubic.gemma.core.util.locking.LockedPath;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * This CLI allows one to lock an experiment data or metadata file.
 * @author poirigui
 */
public class LockExpressionDataFileCli extends ExpressionExperimentManipulatingCLI {

    @Autowired
    private ExpressionDataFileService expressionDataFileService;

    @Autowired
    private FileLockManager fileLockManager;

    @Value("${gemma.appdata.home}/metadata")
    private Path metadataDir;

    @Value("${gemma.appdata.home}/dataFiles")
    private Path dataDir;

    private boolean metadata;
    private boolean exclusive;
    private String filename;
    @Nullable
    private Long timeoutMs;

    public LockExpressionDataFileCli() {
        setSingleExperimentMode();
        setAllowPositionalArguments();
    }

    @Override
    public String getCommandName() {
        return "lockDataFile";
    }

    @Override
    public String getShortDesc() {
        return "Acquire a lock on an experiment data or metadata file.";
    }

    @Override
    protected void buildExperimentOptions( Options options ) {
        options.addOption( "metadata", "metadata", false, "Lock a metadata file" );
        options.addOption( "exclusive", "exclusive", false, "Acquire an exclusive lock (for writing)" );
        options.addOption( Option.builder( "timeout" ).longOpt( "timeout" ).hasArg().type( Long.class )
                .desc( "Timeout in milliseconds to acquire the lock" ).build() );
    }

    @Override
    protected void processExperimentOptions( CommandLine commandLine ) throws ParseException {
        metadata = commandLine.hasOption( "metadata" );
        exclusive = commandLine.hasOption( "exclusive" );
        timeoutMs = commandLine.getParsedOptionValue( "timeout" );
        if ( commandLine.getArgList().size() != 1 ) {
            throw new MissingOptionException( "Exactly one file must be provided as a positional argument." );
        }
        filename = commandLine.getArgList().get( 0 );
    }

    @Override
    protected void processExpressionExperiment( ExpressionExperiment ee ) {
        log.info( "Acquiring " + ( exclusive ? "exclusive" : "shared" ) + " lock on " + filename + "..." );
        logLockStatus( ee );
        try ( LockedPath path = getLock( ee, filename, exclusive ) ) {
            // wait until Ctrl-C
            log.info( ( exclusive ? "Exclusive" : "Shared" ) + " lock acquired on " + path.getPath().getFileName() + ". Press Ctrl-C to interrupt." );
            Thread.sleep( Long.MAX_VALUE );
        } catch ( IOException e ) {
            addErrorObject( ee, "Failed to acquire a " + ( exclusive ? "exclusive" : "shared" ) + " lock on " + filename + ".", e );
        } catch ( InterruptedException | TimeoutException e ) {
            throw new RuntimeException( e );
        }
    }

    private void logLockStatus( ExpressionExperiment ee ) {
        Path p;
        if ( metadata ) {
            p = metadataDir
                    .resolve( ExpressionDataFileUtils.getEEFolderName( ee ) )
                    .resolve( filename );
        } else {
            p = dataDir.resolve( filename );
        }
        try {
            log.info( FileLockInfoUtils.format( fileLockManager.getLockInfo( p ) ) );
        } catch ( IOException e ) {
            log.warn( "Failed to get lock info for " + p, e );
        }
    }

    private LockedPath getLock( ExpressionExperiment ee, String filename, boolean exclusive ) throws IOException, InterruptedException, TimeoutException {
        if ( timeoutMs != null ) {
            if ( metadata ) {
                return expressionDataFileService.getMetadataFile( ee, filename, exclusive, timeoutMs, TimeUnit.MILLISECONDS );
            } else {
                return expressionDataFileService.getDataFile( filename, exclusive, timeoutMs, TimeUnit.MILLISECONDS );
            }
        } else {
            if ( metadata ) {
                return expressionDataFileService.getMetadataFile( ee, filename, exclusive );
            } else {
                return expressionDataFileService.getDataFile( filename, exclusive );
            }
        }
    }
}
