package ubic.gemma.cli.batch;

import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.Closeable;
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

    @Override
    public void write( BatchTaskProcessingResult result ) {
        batchProcessingResults.add( result );
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
