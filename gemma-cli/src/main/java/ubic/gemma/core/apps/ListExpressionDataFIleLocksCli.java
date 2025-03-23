package ubic.gemma.core.apps;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import ubic.gemma.core.util.AbstractCLI;
import ubic.gemma.core.util.locking.FileLockInfo;
import ubic.gemma.core.util.locking.FileLockInfoUtils;
import ubic.gemma.core.util.locking.FileLockManager;

import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * List all locks within Gemma's data directories.
 */
public class ListExpressionDataFIleLocksCli extends AbstractCLI {

    @Autowired
    private FileLockManager fileLockManager;

    @Value("${gemma.appdata.home}/metadata")
    private Path metadataDir;

    @Value("${gemma.appdata.home}/dataFiles")
    private Path dataDir;

    @Override
    public String getCommandName() {
        return "listDataFileLocks";
    }

    @Override
    public String getShortDesc() {
        return "List all locks over data and metadata files.";
    }

    @Override
    protected void doWork() throws Exception {
        try ( Stream<FileLockInfo> s = fileLockManager.getAllLockInfosByWalking( dataDir, 1 ) ) {
            s.forEach( this::printLockInfo );
        }
        try ( Stream<FileLockInfo> s = fileLockManager.getAllLockInfosByWalking( metadataDir, 2 ) ) {
            s.forEach( this::printLockInfo );
        }
    }

    private void printLockInfo( FileLockInfo lockInfo ) {
        getCliContext().getOutputStream().println( FileLockInfoUtils.format( lockInfo ) );
        getCliContext().getOutputStream().println();
    }
}
