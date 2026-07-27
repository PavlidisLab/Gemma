package ubic.gemma.rest;

import lombok.Getter;
import lombok.Setter;import ubic.gemma.core.job.SubmittedTask;
import ubic.gemma.core.job.TaskCommand;
import ubic.gemma.core.job.TaskResult;
import ubic.gemma.core.loader.util.AlreadyExistsInSystemException;
import ubic.gemma.core.tasks.analysis.diffex.DifferentialExpressionAnalysisRemoveTaskCommand;
import ubic.gemma.core.tasks.analysis.diffex.DifferentialExpressionAnalysisTaskCommand;
import ubic.gemma.core.tasks.analysis.expression.BatchInfoFetchTaskCommand;
import ubic.gemma.core.tasks.analysis.expression.PreprocessTaskCommand;
import ubic.gemma.core.tasks.analysis.expression.SvdTaskCommand;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import org.springframework.lang.Nullable;
import java.io.IOException;
import java.util.Date;
import java.util.Queue;
import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;

/**
 * Snapshot of a {@link SubmittedTask} for the REST API.
 * <p>
 * Returned by the dispatch endpoints (with HTTP 202 + a {@code Location} header) and by
 * {@code GET /tasks/{taskId}}. The underlying task store is in-memory only and tasks are evicted roughly
 * 10 minutes after completion, so {@code GET /tasks/{taskId}} returns 404 once the window elapses.
 */
@Getter
@Setter
public class TaskStatusValueObject {

    private String taskId;
    /**
     * Identifier of the experiment the task is operating on, when derivable from the task command.
     */
    @Nullable
    private Long experimentId;
    /**
     * Pipeline step the task corresponds to: {@code preprocess}, {@code pca}, {@code batchInfo}, {@code dea},
     * {@code deaRemove}. {@code null} for tasks whose command class isn't a known pipeline command.
     */
    @Nullable
    private String step;
    /**
     * Lower-cased {@link SubmittedTask.Status} value:
     * {@code queued}, {@code running}, {@code completed}, {@code failed}, {@code cancelling}, {@code unknown}.
     */
    private String status;
    @Nullable
    private Date submittedAt;
    @Nullable
    private Date startedAt;
    @Nullable
    private Date completedAt;
    /**
     * Last progress update emitted by the task. Empty string when nothing is available.
     */
    private String message;
    /**
     * Structured failure detail. Non-{@code null} exactly when {@link #status} is {@code failed}; {@code null}
     * otherwise. Lets a client distinguish (and act on) the many ways an import/pipeline task can go wrong —
     * a network hiccup, an accession that already exists, an invalid accession, a super-series gate, etc. —
     * instead of seeing an opaque {@code failed}.
     */
    @Nullable
    private TaskError error;

    public TaskStatusValueObject() {
    }

    public TaskStatusValueObject( SubmittedTask task ) {
        this.taskId = task.getTaskId();
        this.status = task.getStatus() != null ? task.getStatus().name().toLowerCase() : "unknown";
        this.submittedAt = task.getSubmissionTime();
        this.startedAt = task.getStartTime();
        this.completedAt = task.getFinishTime();
        this.message = lastMessage( task );
        this.error = failureFor( task );
        TaskCommand cmd = task.getTaskCommand();
        if ( cmd != null ) {
            this.step = stepFor( cmd );
            this.experimentId = experimentIdFor( cmd );
        }
    }

    private static String lastMessage( SubmittedTask task ) {
        String last = task.getLastProgressUpdates();
        if ( last != null && !last.isEmpty() ) {
            return last;
        }
        Queue<String> updates = task.getProgressUpdates();
        if ( updates != null && !updates.isEmpty() ) {
            String tail = null;
            for ( String s : updates ) {
                tail = s;
            }
            return tail != null ? tail : "";
        }
        return "";
    }

    @Nullable
    private static String stepFor( TaskCommand cmd ) {
        // Order matters: subclass checks must come before their parents.
        if ( cmd instanceof DifferentialExpressionAnalysisRemoveTaskCommand ) {
            return "deaRemove";
        }
        if ( cmd instanceof DifferentialExpressionAnalysisTaskCommand ) {
            return "dea";
        }
        if ( cmd instanceof PreprocessTaskCommand ) {
            return ( ( PreprocessTaskCommand ) cmd ).diagnosticsOnly() ? "pca" : "preprocess";
        }
        if ( cmd instanceof BatchInfoFetchTaskCommand ) {
            return "batchInfo";
        }
        if ( cmd instanceof SvdTaskCommand ) {
            return "pca";
        }
        return null;
    }

    @Nullable
    private static Long experimentIdFor( TaskCommand cmd ) {
        ExpressionExperiment ee = null;
        if ( cmd instanceof DifferentialExpressionAnalysisTaskCommand ) {
            ee = ( ( DifferentialExpressionAnalysisTaskCommand ) cmd ).getExpressionExperiment();
        } else if ( cmd instanceof PreprocessTaskCommand ) {
            ee = ( ( PreprocessTaskCommand ) cmd ).getExpressionExperiment();
        } else if ( cmd instanceof BatchInfoFetchTaskCommand ) {
            ee = ( ( BatchInfoFetchTaskCommand ) cmd ).getExpressionExperiment();
        } else if ( cmd instanceof SvdTaskCommand ) {
            ee = ( ( SvdTaskCommand ) cmd ).getExpressionExperiment();
        }
        return ee != null ? ee.getId() : null;
    }

    /** Guard against a self-referential / cyclic cause chain when walking {@link Throwable#getCause()}. */
    private static final int MAX_CAUSE_DEPTH = 25;

    /**
     * Recover the failure detail for a {@code FAILED} task. Returns {@code null} for any other status.
     * <p>
     * The executing task catches the thrown exception and stashes it inside the {@link TaskResult}
     * (as its {@code answer}), so for a task that has already finished failing {@link SubmittedTask#getResult()}
     * returns immediately (its future is complete) with that exception rather than blocking or re-throwing. We
     * still guard the checked signatures and the rarer path where the future itself completed exceptionally.
     */
    @Nullable
    private static TaskError failureFor( SubmittedTask task ) {
        if ( task.getStatus() != SubmittedTask.Status.FAILED ) {
            return null;
        }
        Throwable ex = null;
        try {
            TaskResult result = task.getResult();
            if ( result != null ) {
                ex = result.getException();
            }
        } catch ( ExecutionException e ) {
            // future completed exceptionally (failure outside the task body); the cause is the real error
            ex = e.getCause() != null ? e.getCause() : e;
        } catch ( InterruptedException e ) {
            Thread.currentThread().interrupt();
            return null;
        } catch ( RuntimeException e ) {
            ex = e;
        }
        // status is FAILED but the exception could not be recovered — still hand back a consistent error object
        if ( ex == null ) {
            return TaskError.generic( lastMessageOrNull( task ) );
        }
        return TaskError.from( ex );
    }

    @Nullable
    private static String lastMessageOrNull( SubmittedTask task ) {
        String m = task.getLastProgressUpdates();
        return m != null && !m.isEmpty() ? m : null;
    }

    /**
     * Machine-readable classification of a failed task, so a REST client can react to the failure (link to the
     * existing experiment, correct the accession, retry a transient network error, or surface the raw detail) rather
     * than parsing a free-text message. Present only when {@link TaskStatusValueObject#getStatus()} is {@code failed}.
     */
    @Getter
    @Setter
    public static class TaskError {

        /**
         * Stable, machine-readable code. Known values:
         * <ul>
         *     <li>{@code ALREADY_EXISTS} — the accession is already loaded (see {@link #existingExperimentId}).</li>
         *     <li>{@code INVALID_ACCESSION} — GEO/ArrayExpress rejected the accession as unknown or malformed.</li>
         *     <li>{@code NETWORK_ERROR} — a fetch from the remote source (e.g. NCBI GEO FTP) failed; usually transient.</li>
         *     <li>{@code BLACKLISTED} — the accession is on Gemma's blacklist and will not be imported.</li>
         *     <li>{@code SUPERSERIES_NOT_ALLOWED} / {@code SUBSERIES_NOT_ALLOWED} — a super/sub-series gate blocked the
         *         load; retry with the corresponding allow-flag if the load is intended.</li>
         *     <li>{@code UNSUPPORTED_TAXON} — no supported taxon remained after filtering the samples.</li>
         *     <li>{@code TASK_FAILED} — anything else; consult {@link #message} and the server logs.</li>
         * </ul>
         */
        private String code;
        /** Human-readable description of the failure. */
        private String message;
        /** Simple class name of the underlying exception, for diagnostics. */
        private String exceptionType;
        /** For {@code ALREADY_EXISTS}, the id of the experiment that already holds the accession, when resolvable. */
        @Nullable
        private Long existingExperimentId;

        static TaskError generic( @Nullable String message ) {
            TaskError e = new TaskError();
            e.code = "TASK_FAILED";
            e.message = message != null ? message : "The task failed; no error detail was captured. Check the server logs.";
            e.exceptionType = "";
            return e;
        }

        static TaskError from( Throwable ex ) {
            TaskError e = new TaskError();
            e.exceptionType = ex.getClass().getSimpleName();

            // Already-exists carries the offending existing experiment(s); expose the id so the UI can link.
            Throwable alreadyExists = findInChain( ex, t -> t instanceof AlreadyExistsInSystemException );
            if ( alreadyExists != null ) {
                e.code = "ALREADY_EXISTS";
                e.message = describe( alreadyExists );
                e.existingExperimentId = existingExperimentId( ( AlreadyExistsInSystemException ) alreadyExists );
                return e;
            }

            // A network fetch failed; the IOException is buried under RuntimeException/ExecutionException wrappers.
            Throwable io = findInChain( ex, t -> t instanceof IOException );
            if ( io != null ) {
                String detail = io.getMessage();
                e.code = "NETWORK_ERROR";
                e.message = "Could not fetch data from the remote source (e.g. NCBI GEO FTP). This is frequently "
                        + "transient — retrying the import often succeeds. Underlying error: " + io.getClass().getSimpleName()
                        + ( detail != null ? ": " + detail : "" );
                return e;
            }

            // InvalidAccessionException is package-private in gemma-core, so match it by simple name.
            if ( findInChain( ex, t -> "InvalidAccessionException".equals( t.getClass().getSimpleName() ) ) != null ) {
                e.code = "INVALID_ACCESSION";
                e.message = describe( ex );
                return e;
            }

            // Remaining GEO gates are only distinguishable by their message text.
            String description = describe( ex );
            String lower = description.toLowerCase();
            if ( lower.contains( "blacklisted" ) ) {
                e.code = "BLACKLISTED";
            } else if ( lower.contains( "superseries" ) ) {
                e.code = "SUPERSERIES_NOT_ALLOWED";
            } else if ( lower.contains( "subseries" ) ) {
                e.code = "SUBSERIES_NOT_ALLOWED";
            } else if ( lower.contains( "unsupported taxa" ) || lower.contains( "unsupported taxon" ) ) {
                e.code = "UNSUPPORTED_TAXON";
            } else {
                e.code = "TASK_FAILED";
            }
            e.message = description;
            return e;
        }

        @Nullable
        private static Throwable findInChain( Throwable ex, Predicate<Throwable> test ) {
            Throwable t = ex;
            int depth = 0;
            while ( t != null && depth++ < MAX_CAUSE_DEPTH ) {
                if ( test.test( t ) ) {
                    return t;
                }
                t = t.getCause();
            }
            return null;
        }

        /** The message of the deepest cause that carries one, falling back to the outermost exception's type. */
        private static String describe( Throwable ex ) {
            Throwable t = ex, best = ex;
            int depth = 0;
            while ( t != null && depth++ < MAX_CAUSE_DEPTH ) {
                if ( t.getMessage() != null && !t.getMessage().trim().isEmpty() ) {
                    best = t;
                }
                t = t.getCause();
            }
            String m = best.getMessage();
            return m != null && !m.trim().isEmpty() ? m : best.getClass().getSimpleName();
        }

        @Nullable
        private static Long existingExperimentId( AlreadyExistsInSystemException ex ) {
            Object data = ex.getData();
            if ( data instanceof ExpressionExperiment ) {
                return ( ( ExpressionExperiment ) data ).getId();
            }
            if ( data instanceof Iterable<?> ) {
                for ( Object o : ( Iterable<?> ) data ) {
                    if ( o instanceof ExpressionExperiment ) {
                        return ( ( ExpressionExperiment ) o ).getId();
                    }
                }
            }
            return null;
        }
    }
}