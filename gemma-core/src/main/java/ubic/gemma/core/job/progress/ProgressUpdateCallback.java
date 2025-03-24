package ubic.gemma.core.job.progress;

/**
 * Callback used to emit progress updates.
 * @author poirigui
 */
@FunctionalInterface
public interface ProgressUpdateCallback {
    void onProgressUpdate( String message );
}
