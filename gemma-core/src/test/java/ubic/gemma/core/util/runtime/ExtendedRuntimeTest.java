package ubic.gemma.core.util.runtime;

import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

public class ExtendedRuntimeTest {

    @Test
    public void test() throws IOException {
        ExtendedRuntime extendedRuntime = ExtendedRuntime.getRuntime();
        assumeThat( extendedRuntime ).isInstanceOf( LinuxRuntime.class );
        assertThat( ExtendedRuntime.getRuntime().getPid() )
                .isGreaterThan( 0 );
        assertThat( ExtendedRuntime.getRuntime().getCpuInfo() )
                .isNotEmpty()
                .allSatisfy( cpuInfo -> {
                    assertThat( cpuInfo.getFlags() ).isNotEmpty();
                } );
        assertThat( ExtendedRuntime.getRuntime().getFileLockInfo() )
                .isNotEmpty()
                .allSatisfy( lockInfo -> {
                    assertThat( lockInfo.getId() ).isNotEmpty();
                    assertThat( lockInfo.getInode() ).isGreaterThan( 0 );
                    assertThat( lockInfo.getPid() ).isGreaterThan( 0 );
                } );
        assertThat( ExtendedRuntime.getRuntime().getMemInfo().getAvailableMemory() )
                .isGreaterThan( 0 );
    }
}