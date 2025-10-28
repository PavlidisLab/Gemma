package ubic.gemma.core.util.runtime;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.SystemUtils;

import java.io.IOException;

/**
 * @author poirigui
 */
@CommonsLog
public abstract class ExtendedRuntime {

    private static ExtendedRuntime currentRuntime;

    public static ExtendedRuntime getRuntime() {
        if ( currentRuntime == null ) {
            if ( SystemUtils.IS_OS_LINUX ) {
                currentRuntime = new LinuxRuntime();
            } else {
                log.warn( "Unsupported OS: " + SystemUtils.OS_NAME + " for extended runtime features, a dummy runtime will be created." );
                currentRuntime = new DummyRuntime();
            }
        }
        return currentRuntime;
    }

    public abstract int getPid() throws IOException;

    public abstract FileLockInfo[] getFileLockInfo() throws IOException;

    public abstract CpuInfo[] getCpuInfo() throws IOException;

    public abstract MemInfo getMemInfo() throws IOException;
}
