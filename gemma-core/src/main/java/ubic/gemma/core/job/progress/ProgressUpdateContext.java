package ubic.gemma.core.job.progress;

import ubic.gemma.core.logging.log4j.ProgressUpdateAppender;

public interface ProgressUpdateContext extends AutoCloseable {

    static ProgressUpdateContext createContext( ProgressUpdateCallback callback ) {
        return ProgressUpdateAppender.createContext( callback );
    }

    void reportProgressUpdate( String message );

    @Override
    void close();
}
