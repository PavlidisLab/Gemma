package ubic.gemma.job.executor.common;

/**
 * Created with IntelliJ IDEA.
 * User: anton
 * Date: 08/01/13
 * Time: 11:05 AM
 * To change this template use File | Settings | File Templates.
 */
public interface ProgressUpdateCallback {
    public void addProgressUpdate( String message );
}
