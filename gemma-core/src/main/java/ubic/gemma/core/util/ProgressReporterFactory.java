package ubic.gemma.core.util;

public interface ProgressReporterFactory {

    /**
     * Obtain a progress reporte for a specific log category.
     */
    ProgressReporter createProgressReporter( String what, String logCategory );
}
