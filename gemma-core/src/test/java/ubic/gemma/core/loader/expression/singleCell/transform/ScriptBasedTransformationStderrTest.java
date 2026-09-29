package ubic.gemma.core.loader.expression.singleCell.transform;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static ubic.gemma.core.util.test.TestProcessUtils.FILL_STDERR;
import static ubic.gemma.core.util.test.TestProcessUtils.writeShellScript;

/**
 * A shell script in place of Python, writing more to standard error than a pipe buffer holds while standard output is
 * still open. {@link AbstractScriptBasedTransformation} read standard output to its end first, so the two waited on
 * each other forever.
 */
@DisabledOnOs(OS.WINDOWS)
class ScriptBasedTransformationStderrTest {

    @TempDir
    Path tmp;

    @Test
    void aFailingScriptThatWritesALotToStandardErrorIsReported() throws Exception {
        Path interpreter = writeShellScript( tmp, "python", "cat > /dev/null\n"
                + "echo 'reading the input'\n"
                + FILL_STDERR + "\n"
                + "echo 'ValueError: no cells' >&2\n"
                + "exit 1" );
        // any bundled script will do: the stand-in reads and ignores it
        AbstractScriptBasedTransformation transformation = new AbstractScriptBasedTransformation( "filter-10x-mex.py" ) {
            @Override
            protected String[] createScriptArgs() {
                return new String[] { interpreter.toString() };
            }

            @Override
            protected Map<String, String> createEnvironmentVariables() {
                return Collections.emptyMap();
            }

            @Override
            public String getDescription() {
                return "test";
            }
        };

        assertTimeoutPreemptively( Duration.ofSeconds( 60 ), () ->
                assertThatThrownBy( transformation::perform )
                        .hasMessageStartingWith( "Transformation failed:" )
                        .hasMessageEndingWith( "ValueError: no cells\n" ) );
    }
}
