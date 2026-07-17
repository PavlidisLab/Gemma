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
     * One-study samplesheet for a Gemma-dispatched EE that downloads from GEO: {@code sample} and
     * {@code study_name} are the study accession, {@code study_path} empty (R11 — one run per EE).
     */
    public String samplesheetCsv( String studyName ) {
        require( studyName, "studyName" );
        return SAMPLESHEET_HEADER + "\n" + studyName + "," + studyName + ",\n";
    }

    /**
     * The wrapper script body that {@code sbatch} runs as the head job. It invokes {@code nextflow run}
     * with the shared work-dir ({@code -resume} across attempts, R2), the weblog callback (O3), and the
     * Slurm executor (Nextflow fans its process tasks into the queue itself, R11).
     */
    public String launchScript( String checkoutDir, String profile, String paramsFile,
            String samplesheetPath, String weblogUrl, String workDir ) {
        require( checkoutDir, "checkoutDir" );
        require( profile, "profile" );
        require( paramsFile, "paramsFile" );
        require( samplesheetPath, "samplesheetPath" );
        require( weblogUrl, "weblogUrl" );
        require( workDir, "workDir" );
        String main = checkoutDir + "/main.nf";
        String params = checkoutDir + "/" + paramsFile;
        return "#!/bin/bash\n"
                + "set -euo pipefail\n"
                + "nextflow run " + main
                + " -profile " + profile
                + " -params-file " + params
                + " --input " + samplesheetPath
                + " -process.executor slurm"
                + " -with-weblog " + weblogUrl
                + " -with-trace"
                + " -resume"
                + " -work-dir " + workDir
                + "\n";
    }

    /** {@code sbatch --parsable <script>} — {@code --parsable} makes stdout just the head-job id. */
    public List<String> sbatchCommand( String scriptPath ) {
        require( scriptPath, "scriptPath" );
        return List.of( "sbatch", "--parsable", scriptPath );
    }

    /** {@code squeue -j <id> -h -o %T} — prints the long state name, or nothing once the job leaves the queue. */
    public List<String> squeueCommand( String headJobId ) {
        require( headJobId, "headJobId" );
        return List.of( "squeue", "-j", headJobId, "-h", "-o", "%T" );
    }

    /** {@code sacct -j <id> -n -X -o State} — accounting fallback once a job has left {@code squeue}. */
    public List<String> sacctCommand( String headJobId ) {
        require( headJobId, "headJobId" );
        return List.of( "sacct", "-j", headJobId, "-n", "-X", "-o", "State" );
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

    /** State from {@code sacct -o State}. Blank means Slurm has no record (purged) → {@code null}. */
    @Nullable
    public JobState parseSacctState( @Nullable String sacctStdout ) {
        if ( sacctStdout == null || sacctStdout.isBlank() ) {
            return null;
        }
        return mapSlurmState( firstToken( sacctStdout ) );
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
