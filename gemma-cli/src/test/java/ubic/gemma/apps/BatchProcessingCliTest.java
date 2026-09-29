package ubic.gemma.apps;

import org.apache.commons.cli.Options;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import ubic.gemma.cli.util.AbstractCLI;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static ubic.gemma.cli.util.test.Assertions.assertThat;

public class BatchProcessingCliTest {

    @Test
    public void testParallelCli() {
        assertThat( new ParallelCli() )
                .withArguments( "--batch-report-frequency", "1" )
                .succeeds()
                .standardOutput()
                .asString( StandardCharsets.UTF_8 )
                .contains( "Successfully processed 100 objects:" );
    }

    @Test
    public void testSequentialCli() {
        assertThat( new SequentialCli() )
                .withArguments( "--batch-report-frequency", "1" )
                .succeeds()
                .standardOutput()
                .asString( StandardCharsets.UTF_8 )
                .contains( "Successfully processed 100 objects:" );
    }

    @Test
    public void testParallelAndSequentialCli() {
        assertThat( new ParallelAndSequentialCli() )
                .withArguments( "--batch-report-frequency", "1" )
                .succeeds()
                .standardOutput()
                .asString( StandardCharsets.UTF_8 )
                .contains( "Successfully processed 200 objects:" );
    }

    @Test
    public void testReporting() throws IOException {
        assertThat( new SequentialCli() )
                .withArguments( "--batch-format", "TSV" )
                .succeeds()
                .standardOutput()
                .asString( StandardCharsets.UTF_8 )
                .contains( "1\tSUCCESS\t" )
                .contains( "50\tSUCCESS\t" )
                .contains( "99\tSUCCESS\t" );
        assertThat( new SequentialCli() )
                .withArguments( "--batch-format", "SUPPRESS" )
                .succeeds()
                .standardOutput()
                .isEmpty();
        Path tmpFile = Files.createTempFile( "test", null );
        assertThat( new SequentialCli() )
                // will produce TSV by default when a file is specified
                .withArguments( "--batch-output-file", tmpFile.toString() )
                .succeeds()
                .standardOutput()
                .isEmpty();
        Assertions.assertThat( tmpFile )
                .content()
                .contains( "1\tSUCCESS\t" )
                .contains( "50\tSUCCESS\t" )
                .contains( "99\tSUCCESS\t" );
    }

    /**
     * 🛑 A batch task that reports only a warning has reported. It used to not count, and the
     * executor's "the task said nothing, call it a success" fallback then invented a second row —
     * so a run whose every task was skipped printed a WARNING row and a "Batch task #N" SUCCESS row
     * beside it, and the SUCCESS rows are what a summary gets aggregated by.
     */
    @Test
    public void testAWarningIsNotAlsoCountedAsASuccess() {
        assertThat( new WarningOnlyCli() )
                .withArguments( "--batch-format", "TSV" )
                .succeeds()
                .standardOutput()
                .asString( StandardCharsets.UTF_8 )
                .contains( "\tWARNING\t" )
                .doesNotContain( "SUCCESS" )
                .doesNotContain( "Batch task #" );
    }

    /**
     * 🛑 TEXT is the default format, and its grouped summary can only be written at close — it counts and
     * sections the results. That made the whole record live in memory until the run ended, which stopped
     * being survivable when the CLI gained {@code -XX:+ExitOnOutOfMemoryError}: that calls {@code os::exit()},
     * so there is no close and no shutdown hook, and a sweep that OOM'd on item 7 of 22 would have lost the
     * record of the six that worked. Worse than the hung JVM the flag was added to prevent.
     * <p>
     * Each result is therefore emitted as it happens as well as being grouped at the end. The assertion that
     * matters is the ORDER: a per-result line appears before the summary header, which is only true if it was
     * written during the run rather than assembled at close.
     */
    @Test
    public void testEachResultIsWrittenAsItHappensNotOnlyAtTheEnd() {
        String out = new String( assertThat( new SequentialCli() )
                .withArguments( "--batch-report-frequency", "1" )
                .succeeds()
                .standardOutput()
                .actual(), StandardCharsets.UTF_8 );
        int firstRow = out.indexOf( "SUCCESS\t0" );
        int summary = out.indexOf( "Successfully processed 100 objects:" );
        Assertions.assertThat( firstRow ).as( "a per-result line is emitted during the run" ).isNotNegative();
        Assertions.assertThat( summary ).as( "the grouped summary still follows" ).isGreaterThan( firstRow );
    }

    private static class WarningOnlyCli extends AbstractCLI {

        @Override
        protected void buildOptions( Options options ) {
            addBatchOption( options );
        }

        @Override
        protected void doWork() {
            for ( int i = 0; i < 10; i++ ) {
                int finalI = i;
                getBatchTaskExecutor().submit( () -> addWarningObject( finalI, "skipped" ) );
            }
        }
    }

    private static class ParallelCli extends AbstractCLI {

        @Override
        protected void buildOptions( Options options ) {
            addBatchOption( options );
        }

        @Override
        protected void doWork() {
            for ( int i = 0; i < 100; i++ ) {
                int finalI = i;
                getBatchTaskExecutor().submit( () -> {
                    try {
                        Thread.sleep( 1 );
                        addSuccessObject( finalI );
                    } catch ( InterruptedException e ) {
                        throw new RuntimeException( e );
                    }
                } );
            }
        }
    }

    private static class SequentialCli extends AbstractCLI {

        @Override
        protected void buildOptions( Options options ) {
            addBatchOption( options );
        }

        @Override
        protected void doWork() {
            for ( int i = 0; i < 100; i++ ) {
                int finalI = i;
                getBatchTaskExecutor().submit( () -> {
                    try {
                        Thread.sleep( 1 );
                        addSuccessObject( finalI );
                    } catch ( InterruptedException e ) {
                        throw new RuntimeException( e );
                    }
                } );
            }
        }
    }

    private static class ParallelAndSequentialCli extends AbstractCLI {

        @Override
        protected void buildOptions( Options options ) {
            addBatchOption( options );
        }

        @Override
        protected void doWork() throws Exception {
            for ( int i = 0; i < 100; i++ ) {
                int finalI = i;
                getBatchTaskExecutor().submit( () -> {
                    try {
                        Thread.sleep( 1 );
                        addSuccessObject( finalI );
                    } catch ( InterruptedException e ) {
                        throw new RuntimeException( e );
                    }
                } );
            }
            for ( int i = 0; i < 100; i++ ) {
                addSuccessObject( 100 + i );
                Thread.sleep( 1 );
            }
        }
    }
}
