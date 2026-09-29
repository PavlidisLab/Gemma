package ubic.gemma.cli.util;

import org.apache.commons.cli.Options;
import org.junit.jupiter.api.Test;
import ubic.gemma.cli.batch.BatchTaskExecutorService;
import ubic.gemma.cli.batch.BatchTaskProgressReporter;
import ubic.gemma.cli.batch.SuppressBatchTaskSummaryWriter;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * An {@link Error} that is not an {@link OutOfMemoryError} (those end the JVM through
 * {@code -XX:+ExitOnOutOfMemoryError}) must end a CLI run with a non-zero status, and the batch executor must be shut
 * down, because its threads are not daemons.
 */
class AbstractCLIErrorHandlingTest {

    /**
     * The batch task wrapper caught {@link Exception}, so an {@link Error} recorded nothing and the run exited 0.
     */
    @Test
    void anErrorInABatchTaskIsReportedAsAnError() {
        ubic.gemma.cli.util.test.Assertions.assertThat( new ErrorInBatchTaskCli() )
                .withArguments( "--batch-format", "TSV" )
                .failsWith( 2 )
                .standardOutput()
                .asString( StandardCharsets.UTF_8 )
                .contains( "\tERROR\t" )
                .contains( "broken invariant" );
    }

    /**
     * {@code executeCommand} caught {@link Exception}, so an {@link Error} from {@code doWork()} propagated out of it
     * with the batch executor still running; from GemmaCLI's main() that meant no System.exit() and a JVM kept alive
     * by the executor's non-daemon threads.
     */
    @Test
    void anErrorInDoWorkFailsTheRunAndStopsTheBatchExecutor() throws InterruptedException {
        ErrorInDoWorkCli cli = new ErrorInDoWorkCli();

        ubic.gemma.cli.util.test.Assertions.assertThat( cli )
                .failsWith( 1 )
                .exitCause()
                .hasRootCauseInstanceOf( AssertionError.class );

        assertThat( cli.executor ).isNotNull();
        assertThat( cli.executor.isShutdown() ).isTrue();
        assertThat( cli.executor.awaitTermination( 10, TimeUnit.SECONDS ) )
                .as( "the running batch task was interrupted" ).isTrue();
    }

    /**
     * {@link BatchTaskExecutorService#close()} closed only the progress reporter and left the executor running.
     */
    @Test
    void closingTheBatchExecutorShutsItDown() {
        BatchTaskExecutorService executor = new BatchTaskExecutorService( Executors.newSingleThreadExecutor(),
                new BatchTaskProgressReporter( new SuppressBatchTaskSummaryWriter(), null ) );
        executor.submit( () -> { } );

        executor.close();

        assertThat( executor.isShutdown() ).isTrue();
    }

    private static class ErrorInBatchTaskCli extends AbstractCLI {

        @Override
        protected void buildOptions( Options options ) {
            addBatchOption( options );
        }

        @Override
        protected void doWork() {
            getBatchTaskExecutor().submit( () -> {
                throw new AssertionError( "broken invariant" );
            } );
        }
    }

    private static class ErrorInDoWorkCli extends AbstractCLI {

        private volatile BatchTaskExecutorService executor;

        @Override
        protected void buildOptions( Options options ) {
            addBatchOption( options );
        }

        @Override
        protected void doWork() {
            executor = getBatchTaskExecutor();
            executor.submit( () -> {
                try {
                    Thread.sleep( 60_000 );
                } catch ( InterruptedException e ) {
                    Thread.currentThread().interrupt();
                }
            } );
            throw new AssertionError( "broken invariant" );
        }
    }
}
