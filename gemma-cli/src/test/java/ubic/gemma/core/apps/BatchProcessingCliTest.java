package ubic.gemma.core.apps;

import org.apache.commons.cli.Options;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import ubic.gemma.core.util.AbstractCLI;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static ubic.gemma.core.util.test.Assertions.assertThat;

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
