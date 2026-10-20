package ubic.gemma.cli.util;

import ubic.gemma.core.util.AbstractProgressReporter;

import java.io.Console;

import static ubic.gemma.core.util.NetUtils.bytePerSecondToDisplaySize;

public class ConsoleProgressReporter extends AbstractProgressReporter {

    private final Object what;
    private final Console console;

    private final long startTimeNanos;

    public ConsoleProgressReporter( Object what, Console console ) {
        this.what = what;
        this.console = console;
        this.startTimeNanos = System.nanoTime();
        setProgressIncrementToReportInBytes( 1e3 );     // every KB
        setProgressIncrementToReportInPercent( 0.005 ); // every half %
    }

    @Override
    protected void doReportProgress( double progressInPercent, long progressInBytes, long maxSizeInBytes, boolean atEnd ) {
        console.printf( "%s %.2f%% [%d/%d] @ %s\r" + ( atEnd ? "\n" : "\r" ),
                what,
                100 * progressInPercent,
                progressInBytes, maxSizeInBytes,
                bytePerSecondToDisplaySize( 1e9 * progressInBytes / ( System.nanoTime() - startTimeNanos ) ) );
        console.printf( AnsiEscapeCodes.progress( Math.round( 100.0f * ( float ) progressInPercent ) ) );
    }

    @Override
    protected void doReportUnknownProgress( long progressInBytes, boolean atEnd ) {
        console.printf( "%s [%d/?] @ %s" + ( atEnd ? "\n" : "\r" ),
                what,
                progressInBytes,
                bytePerSecondToDisplaySize( 1e9 * progressInBytes / ( System.nanoTime() - startTimeNanos ) ) );
        // indeterminate if there is no max task estimate or if we are over the max
        console.printf( AnsiEscapeCodes.indeterminateProgress() );
    }

    @Override
    public void close() {
        console.printf( AnsiEscapeCodes.clearProgress() );
    }
}
