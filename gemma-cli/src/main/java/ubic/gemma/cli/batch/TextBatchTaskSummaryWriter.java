package ubic.gemma.cli.batch;

import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author poirigui
 */
public class TextBatchTaskSummaryWriter implements BatchTaskSummaryWriter {

    private final List<BatchTaskProcessingResult> batchProcessingResults = Collections.synchronizedList( new ArrayList<>() );
    private final Appendable dest;

    public TextBatchTaskSummaryWriter( Appendable dest ) {
        this.dest = dest;
    }

    /**
     * Record the result for the grouped summary, <b>and emit it immediately</b>.
     * <p>
     * 🛑 The grouped summary can only be written at {@link #close()} — it counts and sections the results, so
     * it cannot exist until they all do. That made this writer the one format whose entire record lived in
     * memory until the end of the run, which was survivable only as long as the JVM always reached the end.
     * It no longer does: the CLI runs with {@code -XX:+ExitOnOutOfMemoryError}, and that calls
     * {@code os::exit()} — no {@code close()}, no shutdown hook, nothing. A sweep that OOM'd on item 7 of 22
     * would have lost the record of the six that succeeded, which is worse than the hung JVM the flag was
     * added to prevent.
     * <p>
     * TEXT is the default format (anything without {@code -batchOutputFile}), so this was the default
     * exposure. TSV never had it — {@link TsvBatchTaskSummaryWriter} flushes each row as it is printed.
     * <p>
     * The per-result line is the same {@link #formatResult} the summary uses, so the two agree; the summary
     * still follows at close with the counts and the grouping.
     */
    @Override
    public void write( BatchTaskProcessingResult result ) throws IOException {
        batchProcessingResults.add( result );
        dest.append( result.getResultType().name() ).append( "\t" ).append( formatResult( result ) ).append( "\n" );
        if ( dest instanceof Flushable ) {
            // An OutputStreamWriter over stdout buffers; unflushed is indistinguishable from unwritten when
            // the JVM exits abruptly, which is the whole case this exists for.
            ( ( Flushable ) dest ).flush();
        }
    }

    @Override
    public void close() throws IOException {
        try {
            writeSummary();
        } finally {
            if ( dest instanceof Closeable ) {
                ( ( Closeable ) dest ).close();
            }
        }
    }

    private void writeSummary() throws IOException {
        if ( batchProcessingResults.isEmpty() ) {
            return;
        }

        List<BatchTaskProcessingResult> successObjects = batchProcessingResults.stream()
                .filter( bp -> bp.getResultType() == BatchTaskProcessingResult.ResultType.SUCCESS )
                .collect( Collectors.toList() );
        if ( !successObjects.isEmpty() ) {
            dest.append( "---------------------\nSuccessfully processed " )
                    .append( String.valueOf( successObjects.size() ) )
                    .append( " objects:\n" );
            for ( BatchTaskProcessingResult result : successObjects ) {
                dest.append( formatResult( result ) ).append( "\n" );
            }
            dest.append( "---------------------\n" );
        }

        List<BatchTaskProcessingResult> warningObjects = batchProcessingResults.stream()
                .filter( batchProcessingResult -> batchProcessingResult.getResultType() == BatchTaskProcessingResult.ResultType.WARNING )
                .collect( Collectors.toList() );
        if ( !warningObjects.isEmpty() ) {
            if ( !successObjects.isEmpty() ) {
                dest.append( "\n" );
            }
            dest.append( "---------------------\nWarnings occurred during the processing of " )
                    .append( String.valueOf( warningObjects.size() ) )
                    .append( " objects:\n" );
            for ( BatchTaskProcessingResult result : warningObjects ) {
                dest.append( formatResult( result ) ).append( "\n" );
            }
            dest.append( "---------------------\n" );
        }

        List<BatchTaskProcessingResult> errorObjects = batchProcessingResults.stream()
                .filter( batchProcessingResult -> batchProcessingResult.getResultType() == BatchTaskProcessingResult.ResultType.ERROR )
                .collect( Collectors.toList() );
        if ( !errorObjects.isEmpty() ) {
            if ( !successObjects.isEmpty() || !warningObjects.isEmpty() ) {
                dest.append( "\n" );
            }
            dest.append( "---------------------\nErrors occurred during the processing of " )
                    .append( String.valueOf( errorObjects.size() ) )
                    .append( " objects:\n" );
            for ( BatchTaskProcessingResult result : errorObjects ) {
                dest.append( formatResult( result ) ).append( "\n" );
            }
            dest.append( "---------------------\n" );
        }

    }

    private String formatResult( BatchTaskProcessingResult result ) {
        StringBuilder buf = new StringBuilder();
        buf.append( result.getSource() != null ? result.getSource() : "Unknown object" );
        if ( result.getMessage() != null ) {
            buf.append( "\t" )
                    // FIXME We don't want newlines here at all, but I'm not sure what condition this is meant to fix exactly.
                    .append( result.getMessage().replace( "\n", "\n\t" ) );
        }
        if ( result.getThrowable() != null ) {
            buf.append( "\t" )
                    .append( "Reason: " )
                    .append( ExceptionUtils.getRootCauseMessage( result.getThrowable() ) );
        }
        return buf.toString();
    }
}
