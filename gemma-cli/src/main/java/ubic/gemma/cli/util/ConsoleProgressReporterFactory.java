package ubic.gemma.cli.util;

import lombok.extern.apachecommons.CommonsLog;
import ubic.gemma.core.util.LoggingProgressReporter;
import ubic.gemma.core.util.ProgressReporter;
import ubic.gemma.core.util.ProgressReporterFactory;

import java.io.Console;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A console-based progress reporter factory.
 *
 * @author poirigui
 */
@CommonsLog
public class ConsoleProgressReporterFactory implements ProgressReporterFactory {

    private final Console console;
    private final AtomicBoolean consoleInUse = new AtomicBoolean( false );

    public ConsoleProgressReporterFactory( Console console ) {
        this.console = console;
    }

    @Override
    public ProgressReporter createProgressReporter( String what, String logCategory ) {
        if ( consoleInUse.compareAndSet( false, true ) ) {
            return new ConsoleProgressReporter( what, console ) {
                @Override
                public void close() {
                    try {
                        super.close();
                    } finally {
                        consoleInUse.set( false );
                    }
                }
            };
        } else {
            // console is already being used, report progress in logs
            return new LoggingProgressReporter( what, logCategory );
        }
    }
}
