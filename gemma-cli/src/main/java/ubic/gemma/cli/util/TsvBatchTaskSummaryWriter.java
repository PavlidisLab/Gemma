package ubic.gemma.cli.util;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.IOException;

/**
 * @author poirigui
 */
class TsvBatchTaskSummaryWriter implements BatchTaskSummaryWriter {

    private final CSVPrinter printer;

    TsvBatchTaskSummaryWriter( Appendable dest ) throws IOException {
        this.printer = new CSVPrinter( dest, CSVFormat.TDF );
    }

    @Override
    public void write( BatchTaskProcessingResult result ) throws IOException {
        printer.printRecord(
                result.getSource(),
                result.getResultType().name(),
                result.getMessage(),
                result.getThrowable() != null ? ExceptionUtils.getRootCauseMessage( result.getThrowable() ) : null );
        printer.flush();
    }

    @Override
    public void close() throws IOException {
        printer.close();
    }
}
