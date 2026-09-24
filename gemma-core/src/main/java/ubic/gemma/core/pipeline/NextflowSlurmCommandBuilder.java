/*
 * The Gemma project.
 *
 * Copyright (c) 2026 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 */
package ubic.gemma.core.pipeline;

import org.springframework.lang.Nullable;
import ubic.gemma.model.pipeline.JobState;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pure command/artifact assembly + Slurm-output parsing for {@link NextflowSlurmScheduler} (R11/R13).
 * No IO, no SSH, no Spring — every method is a deterministic function of its inputs, so the whole
 * class is unit-tested off-cluster (only the {@code ssh … exec} edge in {@link SshCommandRunner} needs
 * the node). Keeping the wire shape here isolates it from the rest of the adapter when the pipeline's
 * CLI drifts.
 *
 * <p>Model (R13): we {@code sbatch} a wrapper script whose body is one {@code nextflow run …}; the
 * head-job id sbatch prints is the {@code SchedulerHandle}. Nextflow itself submits the per-process
 * task jobs (its own {@code slurm} executor); we only launch + poll + cancel the head job.</p>
 */
public class NextflowSlurmCommandBuilder {

    /** nf-core samplesheet header (assets/schema_input.json): {@code sample,study_name,study_path}. */
    private static final String SAMPLESHEET_HEADER = "sample,study_name,study_path";

    /**
     * Files a run leaves at the top of its work-dir. The head job runs with the work-dir as its working
     * directory ({@link #sbatchCommand}), so Nextflow's own {@code .nextflow.log} (and the
     * {@code .nextflow/} cache {@code -resume} reads) land there too, instead of in the SSH account's
     * home. Gemma serves them off the shared mount ({@link NextflowSlurmScheduler#readLog}).
     */
    public static final String HEAD_OUTPUT = "head.out";
    public static final String NEXTFLOW_LOG = ".nextflow.log";
    public static final String TRACE = "trace.txt";
    public static final String REPORT = "report.html";
    /**
     * Written by the wrapper once {@code nextflow run} exits, holding its exit status. Its presence is how
     * Gemma knows the run is over; the value says how. Written to a temporary name and renamed, so a
     * reader never sees a half-written file.
     */
    public static final String EXIT_CODE = "exitcode";

    /** Extracts {@code JobState=<STATE>} from {@code scontrol show job} output. */
    private static final Pattern JOB_STATE = Pattern.compile( "JobState=([A-Z_]+)" );

    /**
     * The {@code nextflow} executable to invoke in the wrapper. Configurable because it is often NOT on
     * a non-login shell's {@code PATH} on the cluster (e.g. scratchy has it at
     * {@code /space/opt/bin/nextflow}), so the sbatch'd wrapper must call it by an explicit path.
     */
    private final String nextflowExecutable;

    /** Default: {@code nextflow} (assumes it's on PATH). Set an absolute path where it isn't. */
    public NextflowSlurmCommandBuilder() {
        this( "nextflow" );
    }

    public NextflowSlurmCommandBuilder( String nextflowExecutable ) {
        this.nextflowExecutable = ( nextflowExecutable == null || nextflowExecutable.isBlank() )
                ? "nextflow" : nextflowExecutable;
    }

    /**
     * One-study samplesheet for a Gemma-dispatched EE that downloads from GEO: {@code sample} and
     * {@code study_name} are the study accession, {@code study_path} empty (R11 — one run per EE).
     */
    public String samplesheetCsv( String studyName ) {
        require( studyName, "studyName" );
        return SAMPLESHEET_HEADER + "\n" + studyName + "," + studyName + ",\n";
    }

    /**
     * The wrapper script body that {@code sbatch} runs as the head job. It invokes {@code nextflow run}
     * with the shared work-dir ({@code -resume}, R2) and the Slurm executor (Nextflow fans its process
     * tasks into the queue itself, R11), then records the exit status in {@link #EXIT_CODE}.
     * <p>
     * The {@code TERM} trap keeps the wrapper alive through {@code scancel}: bash runs a trap only after
     * its foreground child exits, so Nextflow gets to cancel its own tasks and the exit code is still
     * written. (A trap, not an ignore: an ignored signal would be inherited by Nextflow.) A head job
     * killed outright — out of memory, over its time limit — writes nothing; the scheduler's poll covers
     * that case from Slurm.
     *
     * @param weblogUrl where Nextflow posts live events, or null to run without the weblog
     */
    public String launchScript( String checkoutDir, String profile, String paramsFile,
            String samplesheetPath, @Nullable String weblogUrl, String workDir ) {
        require( checkoutDir, "checkoutDir" );
        require( profile, "profile" );
        require( paramsFile, "paramsFile" );
        require( samplesheetPath, "samplesheetPath" );
        require( workDir, "workDir" );
        String main = checkoutDir + "/main.nf";
        String params = checkoutDir + "/" + paramsFile;
        String exitCode = workDir + "/" + EXIT_CODE;
        return "#!/bin/bash\n"
                + "set -uo pipefail\n"
                + "trap 'true' TERM\n"
                + nextflowExecutable + " run " + main
                + " -profile " + profile
                + " -params-file " + params
                + " --input " + samplesheetPath
                + " -process.executor slurm"
                + ( weblogUrl != null ? " -with-weblog " + weblogUrl : "" )
                + " -with-trace " + TRACE
                + " -with-report " + REPORT
                + " -resume"
                + " -work-dir " + workDir
                + "\n"
                + "rc=$?\n"
                + "printf '%s\\n' \"$rc\" > " + exitCode + ".tmp && mv " + exitCode + ".tmp " + exitCode + "\n"
                + "exit \"$rc\"\n";
    }

    /**
     * Summary of a run's {@link #TRACE} file: one row per finished task, found by the header's
     * {@code status} and {@code name} columns rather than by position (the column set is configurable).
     *
     * @return null for a missing, empty or header-only trace
     */
    @Nullable
    public TraceProgress parseTrace( @Nullable String traceTsv ) {
        if ( traceTsv == null || traceTsv.isBlank() ) {
            return null;
        }
        String[] lines = traceTsv.split( "\n" );
        List<String> header = List.of( lines[0].trim().split( "\t" ) );
        int status = header.indexOf( "status" );
        int name = header.indexOf( "name" );
        if ( status < 0 ) {
            return null;
        }
        int completed = 0, cached = 0, failed = 0, rows = 0;
        String last = null;
        for ( int i = 1; i < lines.length; i++ ) {
            if ( lines[i].isBlank() ) continue;
            String[] cols = lines[i].split( "\t", -1 );
            if ( cols.length <= status ) continue; // a row still being written
            rows++;
            switch ( cols[status].trim() ) {
                case "COMPLETED": completed++; break;
                case "CACHED": cached++; break;
                case "FAILED":
                case "ABORTED": failed++; break;
                default: break;
            }
            if ( name >= 0 && cols.length > name ) {
                last = cols[name].trim();
            }
        }
        return rows == 0 ? null : new TraceProgress( completed, cached, failed, last );
    }

    /** Counts of finished tasks in a trace, and the most recent one's name (e.g. {@code LOAD_CTA (GSE124952)}). */
    @lombok.Value
    public static class TraceProgress {
        int completed;
        int cached;
        int failed;
        @Nullable
        String lastTask;
    }

    /**
     * The wrapper's {@link #EXIT_CODE} file content as a number.
     *
     * @return null if the content isn't one (blank or garbled: treat as not written yet)
     */
    @Nullable
    public Integer parseExitCode( @Nullable String content ) {
        if ( content == null || content.isBlank() ) {
            return null;
        }
        try {
            return Integer.valueOf( content.trim() );
        } catch ( NumberFormatException e ) {
            return null;
        }
    }

    /**
     * {@code sbatch --parsable --chdir <workDir> --output <workDir>/head.out <script>}. {@code --parsable}
     * makes stdout just the head-job id; {@code --chdir} makes the work-dir Nextflow's launch directory;
     * {@code --output} puts the head job's stdout/stderr (Nextflow's console, including its error
     * report) next to them. Without the last two, Slurm writes to the directory {@code sbatch} was
     * called from — the SSH account's home.
     */
    public List<String> sbatchCommand( String scriptPath, String workDir ) {
        require( scriptPath, "scriptPath" );
        require( workDir, "workDir" );
        return List.of( "sbatch", "--parsable", "--chdir", workDir, "--output", workDir + "/" + HEAD_OUTPUT, scriptPath );
    }

    /** {@code squeue -j <id> -h -o %T} — prints the long state name, or nothing once the job leaves the queue. */
    public List<String> squeueCommand( String headJobId ) {
        require( headJobId, "headJobId" );
        return List.of( "squeue", "-j", headJobId, "-h", "-o", "%T" );
    }

    /**
     * {@code scontrol show job <id>} — fallback once a job has left {@code squeue}. Preferred over
     * {@code sacct} because it does NOT need Slurm accounting (which is disabled on our cluster:
     * {@code AccountingStorageType=(null)}). Covers active + recently-terminal jobs, up to
     * {@code MinJobAge} (300 s) after completion, after which Slurm forgets the job entirely.
     */
    public List<String> scontrolShowJobCommand( String headJobId ) {
        require( headJobId, "headJobId" );
        return List.of( "scontrol", "show", "job", headJobId );
    }

    public List<String> scancelCommand( String headJobId ) {
        require( headJobId, "headJobId" );
        return List.of( "scancel", headJobId );
    }

    /**
     * Parse {@code sbatch --parsable} stdout into the head-job id. Output is {@code "<jobid>"} or
     * {@code "<jobid>;<cluster>"}; we take the id before any {@code ;}.
     *
     * @throws PipelineSchedulerException if stdout has no numeric job id
     */
    public String parseSbatchJobId( @Nullable String stdout ) throws PipelineSchedulerException {
        String s = stdout == null ? "" : stdout.trim();
        int semi = s.indexOf( ';' );
        if ( semi >= 0 ) {
            s = s.substring( 0, semi ).trim();
        }
        if ( !s.matches( "\\d+" ) ) {
            throw new PipelineSchedulerException( "could not parse sbatch job id from: '" + stdout + "'" );
        }
        return s;
    }

    /**
     * State from {@code squeue -o %T}. Blank output means the job is no longer queued/running (finished
     * or purged) → {@code null}, so the caller falls back to {@code sacct}.
     */
    @Nullable
    public JobState parseSqueueState( @Nullable String squeueStdout ) {
        if ( squeueStdout == null || squeueStdout.isBlank() ) {
            return null;
        }
        return mapSlurmState( firstToken( squeueStdout ) );
    }

    /**
     * State from {@code scontrol show job} output — extracts the {@code JobState=<STATE>} token. Blank
     * output / no such token (job purged after {@code MinJobAge}, or an "Invalid job id" error) →
     * {@code null}, i.e. Slurm no longer knows the job.
     */
    @Nullable
    public JobState parseScontrolState( @Nullable String scontrolStdout ) {
        if ( scontrolStdout == null || scontrolStdout.isBlank() ) {
            return null;
        }
        Matcher m = JOB_STATE.matcher( scontrolStdout );
        return m.find() ? mapSlurmState( m.group( 1 ) ) : null;
    }

    /**
     * Map a Slurm state name (long form, as emitted by {@code squeue -o %T} and {@code sacct -o State})
     * to a Gemma {@link JobState}. Unknown states → {@code null} (surface the gap rather than guess).
     * Handles the {@code sacct} {@code "CANCELLED+"} / {@code "CANCELLED by 123"} variants.
     */
    @Nullable
    public JobState mapSlurmState( String slurmState ) {
        if ( slurmState == null || slurmState.isBlank() ) {
            return null;
        }
        // sacct suffixes state with '+' when truncated and "CANCELLED" as "CANCELLED by <uid>".
        String s = slurmState.trim().toUpperCase( Locale.ROOT );
        int space = s.indexOf( ' ' );
        if ( space >= 0 ) {
            s = s.substring( 0, space );
        }
        if ( s.endsWith( "+" ) ) {
            s = s.substring( 0, s.length() - 1 );
        }
        switch ( s ) {
            case "PENDING":
                return JobState.QUEUED;
            case "RUNNING":
            case "COMPLETING":
            case "CONFIGURING":
            case "SUSPENDED":       // no SUSPENDED job state modelled — treat as still running
                return JobState.RUNNING;
            case "COMPLETED":
                return JobState.DONE;
            case "FAILED":
            case "NODE_FAIL":
            case "BOOT_FAIL":
            case "OUT_OF_MEMORY":
            case "DEADLINE":
            case "TIMEOUT":
                return JobState.FAILED;
            case "CANCELLED":
                return JobState.CANCELLED;
            default:
                return null;
        }
    }

    private static String firstToken( String s ) {
        return s.trim().split( "\\s+", 2 )[0];
    }

    private static void require( String v, String name ) {
        if ( v == null || v.isBlank() ) {
            throw new IllegalArgumentException( name + " is required" );
        }
    }
}
