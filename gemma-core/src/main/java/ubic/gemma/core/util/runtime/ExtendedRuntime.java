package ubic.gemma.core.util.runtime;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.SystemUtils;

import java.io.IOException;

/**
 * @author poirigui
 */
@Slf4j
public abstract class ExtendedRuntime {

    // Double-checked locking with volatile: getRuntime() is hit from multiple request threads
    // (FileLockManagerImpl.getAllLockInfos, getLockInfo). Without volatile, a reader could
    // observe a partially-constructed LinuxRuntime; without sync, two threads can each
    // construct an instance and race on the assignment.
    private static volatile ExtendedRuntime currentRuntime;

    public static ExtendedRuntime getRuntime() {
        ExtendedRuntime local = currentRuntime;
        if ( local == null ) {
            synchronized ( ExtendedRuntime.class ) {
                local = currentRuntime;
                if ( local == null ) {
                    if ( SystemUtils.IS_OS_LINUX ) {
                        local = new LinuxRuntime();
                    } else {
                        log.warn( "Unsupported OS: " + SystemUtils.OS_NAME + " for extended runtime features, a dummy runtime will be created." );
                        local = new DummyRuntime();
                    }
                    currentRuntime = local;
                }
            }
        }
        return local;
    }

    public abstract int getPid() throws IOException;

    public abstract FileLockInfo[] getFileLockInfo() throws IOException;

    public abstract CpuInfo[] getCpuInfo() throws IOException;

    public abstract MemInfo getMemInfo() throws IOException;
}
