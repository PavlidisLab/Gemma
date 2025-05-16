package ubic.gemma.core.job;

/**
 * These hooks are used to update status of the running task.
 */
interface TaskLifecycleHandler {

    /**
     * Whenever the task execution begins.
     */
    default void onStart() {
    }

    /**
     * When progress is made on the task.
     */
    default void onProgress( String message ) {
    }

    /**
     * On successful completion.
     */
    default void onSuccess() {
    }

    /**
     * On failure.
     */
    default void onFailure( Exception e ) {
    }

    /**
     * On completion, regardless of failure.
     */
    default void onComplete() {
    }
}
