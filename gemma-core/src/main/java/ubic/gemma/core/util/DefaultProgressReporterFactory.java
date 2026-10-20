package ubic.gemma.core.util;

public class DefaultProgressReporterFactory implements ProgressReporterFactory {

    @Override
    public ProgressReporter createProgressReporter( String what, String logCategory ) {
        return new LoggingProgressReporter( what, logCategory );
    }
}
