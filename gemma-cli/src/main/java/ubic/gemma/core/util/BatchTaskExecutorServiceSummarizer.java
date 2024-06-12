package ubic.gemma.core.util;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Summarizes the processing of a batch of tasks.
 * @author poirigui
 */
class BatchTaskExecutorServiceSummarizer {

    private final BatchTaskExecutorService batchTaskExecutorService;

    BatchTaskExecutorServiceSummarizer( BatchTaskExecutorService batchTaskExecutorService ) {
        this.batchTaskExecutorService = batchTaskExecutorService;
    }

    void summarizeBatchProcessingToText( Appendable dest ) throws IOException {
        List<BatchTaskExecutorService.BatchProcessingResult> successObjects = batchTaskExecutorService.getBatchProcessingResults().stream().filter( bp -> !bp.isError() ).collect( Collectors.toList() );
        if ( !successObjects.isEmpty() ) {
            StringBuilder buf = new StringBuilder();
            buf.append( "\n---------------------\nSuccessfully processed " ).append( successObjects.size() )
                    .append( " objects:\n" );
            for ( BatchTaskExecutorService.BatchProcessingResult result : successObjects ) {
                buf.append( result ).append( "\n" );
            }
            buf.append( "---------------------\n" );
            dest.append( buf );
        }

        List<BatchTaskExecutorService.BatchProcessingResult> errorObjects = batchTaskExecutorService.getBatchProcessingResults().stream().filter( BatchTaskExecutorService.BatchProcessingResult::isError ).collect( Collectors.toList() );
        if ( !errorObjects.isEmpty() ) {
            StringBuilder buf = new StringBuilder();
            buf.append( "\n---------------------\nErrors occurred during the processing of " )
                    .append( errorObjects.size() ).append( " objects:\n" );
            for ( BatchTaskExecutorService.BatchProcessingResult result : errorObjects ) {
                buf.append( result ).append( "\n" );
            }
            buf.append( "---------------------\n" );
            dest.append( buf );
        }
    }

    void summarizeBatchProcessingToTsv( Appendable dest ) throws IOException {
        try ( CSVPrinter printer = new CSVPrinter( dest, CSVFormat.TDF ) ) {
            for ( BatchTaskExecutorService.BatchProcessingResult result : batchTaskExecutorService.getBatchProcessingResults() ) {
                printer.printRecord(
                        result.getSource(),
                        result.isError() ? "ERROR" : "SUCCESS",
                        result.getMessage(),
                        result.getThrowable() != null ? ExceptionUtils.getRootCauseMessage( result.getThrowable() ) : null );
            }
        }
    }
}
