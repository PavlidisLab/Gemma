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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.pipeline.JobState;
import ubic.gemma.model.pipeline.SchedulerKind;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Real Nextflow-on-Slurm scheduler for the sc-annotation pipeline (task 7). Selected by
 * {@code spring.profiles.active=scheduler-nextflow}; replaces the throwing stub.
 *
 * <p>Model (see {@code docs/pipeline-compute/NEXTFLOW_DISPATCH_RESOLUTIONS.md}):</p>
 * <ul>
 *   <li><b>One {@code nextflow run} per EE</b> (R11) — one {@code PipelineJob} ⇒ one run ⇒ one head job.</li>
 *   <li><b>SSH-to-submit-node</b> (R4) via {@link SshCommandRunner}: the container has no Slurm client.</li>
 *   <li><b>Head process as a Slurm job</b> (R13): {@code sbatch --parsable} a wrapper; the head-job id
 *       is the {@link SchedulerHandle}. Cancel {@code scancel}, poll {@code squeue}/{@code sacct}.</li>
 *   <li><b>Shared work-dir on the {@code /space} mount</b> (R2/R10): the samplesheet + wrapper are
 *       written to {@code <workDirBase>/<jobId>/} — the same absolute path the node sees — and
 *       {@code -work-dir} there gives {@code -resume} across attempts.</li>
 *   <li>Live status is pushed by {@code -with-weblog} → the internal {@code /weblog} ingest (O3), NOT
 *       polled here; {@link #poll} is only the reconciler fallback.</li>
 * </ul>
 *
 * <p>All command assembly + Slurm parsing lives in {@link NextflowSlurmCommandBuilder} (pure,
 * unit-tested); this class wires config + SSH + the EE lookup and does the work-dir file writes.</p>
 */
@Component
@Profile("scheduler-nextflow")
@Primary
@Slf4j
public class NextflowSlurmScheduler implements PipelineScheduler {

    /**
     * Work-dir files served as artifacts, with their content types. Nothing outside this map is read, so a
     * name can never reach an arbitrary file under the work-dir (task outputs, the {@code .nextflow/}
     * cache). Pipeline outputs worth serving (MultiQC, Cell Ranger {@code web_summary.html}) join once
     * a real run shows where they land.
     */
    private static final Map<String, String> ARTIFACTS = Map.of(
            NextflowSlurmCommandBuilder.HEAD_OUTPUT, "text/plain; charset=UTF-8",
            NextflowSlurmCommandBuilder.NEXTFLOW_LOG, "text/plain; charset=UTF-8",
            NextflowSlurmCommandBuilder.TRACE, "text/tab-separated-values; charset=UTF-8",
            NextflowSlurmCommandBuilder.REPORT, "text/html; charset=UTF-8" );

    /**
     * Artifacts are returned whole ({@link Artifact} holds a {@code byte[]}), so refuse anything bigger
     * than this rather than load it into the heap; {@link #readLog} pages through files of any size.
     */
    static final long MAX_ARTIFACT_BYTES = 50L * 1024 * 1024;

    /** How much of the end of {@code head.out} a failure carries: enough for Nextflow's error report. */
    static final int FAILURE_TAIL_BYTES = 4096;

    private final ExpressionExperimentService expressionExperimentService;
    private final SshCommandRunner ssh;
    private final NextflowSlurmCommandBuilder commands;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final String checkoutDir;
    private final String workDirBase;
    private final String profile;
    @Nullable
    private final String weblogBaseUrl;
    private final String callbackSecret;
    private final long slurmCheckIdleMillis;

    /**
     * When {@link #poll} last asked Slurm about each job, so a quiet job costs one SSH round-trip per idle
     * window rather than one per reconciler tick. In memory only: after a restart each job is checked
     * once more than strictly needed.
     */
    private final Map<Long, Long> lastSlurmCheck = new ConcurrentHashMap<>();

    @Autowired
    public NextflowSlurmScheduler(
            ExpressionExperimentService expressionExperimentService,
            SshCommandRunner ssh,
            @Value("${gemma.pipeline.nextflow.checkoutDir:}") String checkoutDir,
            @Value("${gemma.pipeline.nextflow.workDirBase:${gemma.appdata.home}/pipeline}") String workDirBase,
            @Value("${gemma.pipeline.nextflow.profile:conda}") String profile,
            @Value("${gemma.pipeline.nextflow.executable:nextflow}") String nextflowExecutable,
            // Root URL the compute-node weblog posts to. Blank (the default) runs without the weblog: job
            // state comes from the work-dir files either way (poll), the weblog only adds live progress.
            @Value("${gemma.pipeline.nextflow.weblogBaseUrl:}") String weblogBaseUrl,
            // Keys the per-job token in the weblog URL (PipelineCallbackTokens); the same secret the
            // callback endpoint verifies against. Only needed with the weblog.
            @Value("${gemma.pipeline.callback.token:}") String callbackSecret,
            @Value("${gemma.pipeline.nextflow.slurmCheckIdleMinutes:10}") int slurmCheckIdleMinutes ) {
        this.expressionExperimentService = expressionExperimentService;
        this.ssh = ssh;
        this.commands = new NextflowSlurmCommandBuilder( nextflowExecutable );
        this.checkoutDir = checkoutDir;
        this.workDirBase = workDirBase;
        this.profile = profile;
        this.weblogBaseUrl = weblogBaseUrl == null || weblogBaseUrl.isBlank() ? null : weblogBaseUrl;
        this.callbackSecret = callbackSecret;
        this.slurmCheckIdleMillis = TimeUnit.MINUTES.toMillis( slurmCheckIdleMinutes );
    }

    @Override
    public SchedulerKind kind() {
        return SchedulerKind.NEXTFLOW;
    }

    @Override
    public SchedulerHandle submit( SubmitRequest req ) throws PipelineSchedulerException {
        if ( checkoutDir.isBlank() ) {
            throw new PipelineSchedulerException( "gemma.pipeline.nextflow.checkoutDir is not configured" );
        }
        // Without the secret a weblog-enabled run could not authenticate its events.
        if ( weblogBaseUrl != null && callbackSecret.isBlank() ) {
            throw new PipelineSchedulerException( "gemma.pipeline.nextflow.weblogBaseUrl is set but gemma.pipeline.callback.token is not" );
        }
        ExpressionExperiment ee = expressionExperimentService.load( req.getExperimentId() );
        if ( ee == null ) {
            throw new PipelineSchedulerException( "no experiment " + req.getExperimentId() + " for job " + req.getGemmaJobId() );
        }
        String studyName = ee.getShortName();
        if ( studyName == null || studyName.isBlank() ) {
            throw new PipelineSchedulerException( "experiment " + req.getExperimentId() + " has no shortName to use as study name" );
        }
        String paramsFile = resolveParamsFile( req.getParamsJson() );
        Long jobId = req.getGemmaJobId();

        // Write the samplesheet + wrapper to the per-job work-dir on the shared /space mount (R10) —
        // the same absolute path the submit node reads. Per-job dir so each run's -resume cache is
        // isolated (R11).
        Path workDir = workDir( jobId );
        Path samplesheet = workDir.resolve( "samplesheet.csv" );
        Path script = workDir.resolve( "launch.sh" );
        try {
            Files.createDirectories( workDir );
            Files.writeString( samplesheet, commands.samplesheetCsv( studyName ), StandardCharsets.UTF_8 );
            Files.writeString( script, commands.launchScript( checkoutDir, profile, paramsFile,
                    samplesheet.toString(), weblogBaseUrl != null ? weblogUrl( jobId ) : null, workDir.toString() ), StandardCharsets.UTF_8 );
        } catch ( IOException e ) {
            throw new PipelineSchedulerException( "failed to write work-dir files under " + workDir + ": " + e.getMessage(), e );
        }

        SshCommandRunner.CommandResult res = ssh.run( commands.sbatchCommand( script.toString(), workDir.toString() ) );
        if ( !res.isSuccess() ) {
            throw new PipelineSchedulerException( "sbatch failed (exit " + res.getExitCode() + "): " + res.getStderr().trim() );
        }
        String headJobId = commands.parseSbatchJobId( res.getStdout() );
        log.info( "submitted job {} (EE {} '{}') as Slurm head job {}", jobId, req.getExperimentId(), studyName, headJobId );
        return new SchedulerHandle( SchedulerKind.NEXTFLOW, headJobId );
    }

    /**
     * Job state, read from the work-dir first and from Slurm only when the files can't say.
     * <ol>
     * <li>The wrapper's {@code exitcode} file settles it: 0 is DONE, anything else FAILED, with the end of
     * {@code head.out} (Nextflow's error report) as the message.</li>
     * <li>While the files are still changing, the job is QUEUED until Slurm creates {@code head.out} and
     * RUNNING after, with progress from {@code trace.txt}. No SSH.</li>
     * <li>Once they have been quiet for {@code slurmCheckIdleMinutes}, ask Slurm whether the head job is
     * still alive — at most once per idle window per job. If Slurm says it ended but there is no
     * {@code exitcode}, keep reporting it as running: NFS may not show a just-written file for up to a
     * minute, and a head job that really died (killed without running the wrapper's last lines) turns
     * into a null — terminal-unknown — once {@code scontrol} forgets it, ~300 s later.</li>
     * </ol>
     * Slurm accounting is disabled on our cluster, so there is no {@code sacct} to ask after that.
     */
    @Override
    @Nullable
    public JobSnapshot poll( Long gemmaJobId, SchedulerHandle handle ) throws PipelineSchedulerException {
        Path dir = workDir( gemmaJobId );
        Integer exitCode = commands.parseExitCode( readIfExists( dir.resolve( NextflowSlurmCommandBuilder.EXIT_CODE ) ) );
        if ( exitCode != null ) {
            lastSlurmCheck.remove( gemmaJobId );
            if ( exitCode == 0 ) {
                return new JobSnapshot( JobState.DONE, "exit 0", null );
            }
            return new JobSnapshot( JobState.FAILED, "exit " + exitCode, failurePayload( exitCode, dir ) );
        }

        String progress = progressPayload( dir );
        boolean started = Files.exists( dir.resolve( NextflowSlurmCommandBuilder.HEAD_OUTPUT ) );
        JobState fromFiles = started ? JobState.RUNNING : JobState.QUEUED;
        long now = System.currentTimeMillis();
        if ( now - lastActivity( dir ) < slurmCheckIdleMillis
                || now - lastSlurmCheck.getOrDefault( gemmaJobId, 0L ) < slurmCheckIdleMillis ) {
            return new JobSnapshot( fromFiles, null, progress );
        }

        lastSlurmCheck.put( gemmaJobId, now );
        SshCommandRunner.CommandResult sq = ssh.run( commands.squeueCommand( handle.getId() ) );
        JobState state = commands.parseSqueueState( sq.getStdout() );
        if ( state != null ) {
            return new JobSnapshot( state, sq.getStdout().trim(), progress );
        }
        SshCommandRunner.CommandResult sc = ssh.run( commands.scontrolShowJobCommand( handle.getId() ) );
        state = commands.parseScontrolState( sc.getStdout() );
        String raw = sc.getStdout().trim();
        if ( state == JobState.CANCELLED ) {
            return new JobSnapshot( JobState.CANCELLED, raw, null ); // e.g. cancelled while still pending
        }
        if ( state != null ) {
            return new JobSnapshot( fromFiles, raw, progress );
        }
        lastSlurmCheck.remove( gemmaJobId );
        log.warn( "poll: job {} has no {} and Slurm has no record of head job {}",
                gemmaJobId, NextflowSlurmCommandBuilder.EXIT_CODE, handle.getId() );
        return null;
    }

    /** Newest modification time among the files a live run keeps touching ({@code launch.sh} for one not started yet). */
    private static long lastActivity( Path dir ) {
        long newest = 0;
        for ( String name : new String[] { "launch.sh", NextflowSlurmCommandBuilder.HEAD_OUTPUT,
                NextflowSlurmCommandBuilder.TRACE, NextflowSlurmCommandBuilder.NEXTFLOW_LOG } ) {
            try {
                FileTime t = Files.getLastModifiedTime( dir.resolve( name ) );
                newest = Math.max( newest, t.toMillis() );
            } catch ( IOException ignored ) {
                // not written (yet)
            }
        }
        return newest;
    }

    @Nullable
    private String progressPayload( Path dir ) throws PipelineSchedulerException {
        NextflowSlurmCommandBuilder.TraceProgress p = commands.parseTrace(
                readIfExists( dir.resolve( NextflowSlurmCommandBuilder.TRACE ) ) );
        if ( p == null ) {
            return null;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        if ( p.getLastTask() != null ) {
            payload.put( "stage", p.getLastTask() );
        }
        payload.put( "tasksCompleted", p.getCompleted() );
        payload.put( "tasksCached", p.getCached() );
        payload.put( "tasksFailed", p.getFailed() );
        return toJson( payload );
    }

    /** The {@code error} payload the service expects: {@code failureClass} (always UNKNOWN — the pipeline doesn't classify), exit code, and the end of {@code head.out}. */
    private String failurePayload( int exitCode, Path dir ) throws PipelineSchedulerException {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put( "failureClass", "UNKNOWN" );
        payload.put( "exitCode", exitCode );
        payload.put( "message", tail( dir.resolve( NextflowSlurmCommandBuilder.HEAD_OUTPUT ), FAILURE_TAIL_BYTES ) );
        return toJson( payload );
    }

    private String toJson( Map<String, Object> payload ) throws PipelineSchedulerException {
        try {
            return objectMapper.writeValueAsString( payload );
        } catch ( IOException e ) {
            throw new PipelineSchedulerException( "could not serialize event payload: " + e.getMessage(), e );
        }
    }

    @Nullable
    private static String readIfExists( Path file ) throws PipelineSchedulerException {
        try {
            return Files.readString( file, StandardCharsets.UTF_8 );
        } catch ( NoSuchFileException e ) {
            return null;
        } catch ( IOException e ) {
            throw new PipelineSchedulerException( "failed to read " + file + ": " + e.getMessage(), e );
        }
    }

    /** Last {@code maxBytes} of a file, starting at a line boundary when it had to cut; empty if missing. */
    private static String tail( Path file, int maxBytes ) throws PipelineSchedulerException {
        try ( SeekableByteChannel ch = Files.newByteChannel( file ) ) {
            long size = ch.size();
            long from = Math.max( 0, size - maxBytes );
            byte[] buf = new byte[( int ) ( size - from )];
            ch.position( from );
            try ( InputStream in = Channels.newInputStream( ch ) ) {
                int read = 0;
                while ( read < buf.length ) {
                    int n = in.read( buf, read, buf.length - read );
                    if ( n < 0 ) break;
                    read += n;
                }
            }
            String text = new String( buf, StandardCharsets.UTF_8 );
            int nl = text.indexOf( '\n' );
            return from > 0 && nl >= 0 ? text.substring( nl + 1 ) : text;
        } catch ( NoSuchFileException e ) {
            return "";
        } catch ( IOException e ) {
            throw new PipelineSchedulerException( "failed to read " + file + ": " + e.getMessage(), e );
        }
    }

    @Override
    public void cancel( SchedulerHandle handle ) throws PipelineSchedulerException {
        SshCommandRunner.CommandResult res = ssh.run( commands.scancelCommand( handle.getId() ) );
        if ( !res.isSuccess() ) {
            // scancel of an already-finished/purged job is non-fatal — the job is gone either way.
            log.warn( "scancel of head job {} returned exit {}: {}", handle.getId(), res.getExitCode(), res.getStderr().trim() );
        }
    }

    @Override
    public boolean supportsLog() {
        return true;
    }

    /**
     * Pages through the head job's console output ({@code head.out}): Nextflow's progress lines and, on
     * failure, its error report — what a curator reads first. Read straight off the shared mount (R10).
     * Before Slurm has started the job the file doesn't exist; that is an empty chunk, not an error.
     * A slice never ends inside a UTF-8 sequence: the cursor stops before it and the next read picks it up.
     */
    @Override
    public LogChunk readLog( Long gemmaJobId, SchedulerHandle handle, long offset, int limit ) throws PipelineSchedulerException {
        Path file = workDir( gemmaJobId ).resolve( NextflowSlurmCommandBuilder.HEAD_OUTPUT );
        long from = Math.max( offset, 0 );
        try ( SeekableByteChannel ch = Files.newByteChannel( file ) ) {
            long size = ch.size();
            if ( from >= size ) {
                return new LogChunk( "", from, true );
            }
            int want = ( int ) Math.min( Math.max( limit, 0 ), size - from );
            byte[] buf = new byte[want];
            ch.position( from );
            int read = 0;
            try ( InputStream in = Channels.newInputStream( ch ) ) {
                while ( read < want ) {
                    int n = in.read( buf, read, want - read );
                    if ( n < 0 ) break;
                    read += n;
                }
            }
            int usable = completeUtf8Prefix( buf, read );
            long next = from + usable;
            return new LogChunk( new String( buf, 0, usable, StandardCharsets.UTF_8 ), next, next >= size );
        } catch ( NoSuchFileException e ) {
            return new LogChunk( "", from, true );
        } catch ( IOException e ) {
            throw new PipelineSchedulerException( "failed to read " + file + ": " + e.getMessage(), e );
        }
    }

    @Override
    public boolean supportsArtifacts() {
        return true;
    }

    @Override
    @Nullable
    public Artifact readArtifact( Long gemmaJobId, SchedulerHandle handle, String name ) throws PipelineSchedulerException {
        String contentType = ARTIFACTS.get( name );
        if ( contentType == null ) {
            return null;
        }
        Path file = workDir( gemmaJobId ).resolve( name );
        try {
            long size = Files.size( file );
            if ( size > MAX_ARTIFACT_BYTES ) {
                throw new PipelineSchedulerException( String.format( "%s is %d bytes, over the %d-byte artifact limit; page it through the log endpoint or read it on the mount",
                        file, size, MAX_ARTIFACT_BYTES ) );
            }
            return new Artifact( name, contentType, Files.readAllBytes( file ) );
        } catch ( NoSuchFileException e ) {
            return null;
        } catch ( IOException e ) {
            throw new PipelineSchedulerException( "failed to read " + file + ": " + e.getMessage(), e );
        }
    }

    private Path workDir( Long jobId ) {
        return Path.of( workDirBase, String.valueOf( jobId ) );
    }

    /**
     * Length of the longest prefix of {@code buf[0, len)} that doesn't end inside a multi-byte UTF-8
     * sequence. Only the last three bytes can belong to a truncated one.
     */
    static int completeUtf8Prefix( byte[] buf, int len ) {
        for ( int back = 1; back <= Math.min( 3, len ); back++ ) {
            int b = buf[len - back] & 0xFF;
            if ( ( b & 0xC0 ) == 0x80 ) {
                continue; // continuation byte: keep looking for the lead
            }
            int seqLen = b >= 0xF0 ? 4 : b >= 0xE0 ? 3 : b >= 0xC0 ? 2 : 1;
            return seqLen > back ? len - back : len;
        }
        return len;
    }

    /**
     * Pick the organism params-file from the batch's {@code paramsJson}: an explicit {@code paramsFile},
     * or an {@code organism} of {@code hs/human} → {@code params.hs.json}, {@code mm/mouse} →
     * {@code params.mm.json}. The organism decision stays with the caller (matches how the pipeline is
     * launched per-organism today) rather than being inferred from the EE taxon here.
     */
    private String resolveParamsFile( @Nullable String paramsJson ) throws PipelineSchedulerException {
        if ( paramsJson != null && !paramsJson.isBlank() ) {
            try {
                JsonNode node = objectMapper.readTree( paramsJson );
                JsonNode explicit = node.get( "paramsFile" );
                if ( explicit != null && explicit.isTextual() && !explicit.asText().isBlank() ) {
                    return explicit.asText();
                }
                JsonNode organism = node.get( "organism" );
                if ( organism != null && organism.isTextual() ) {
                    switch ( organism.asText().trim().toLowerCase( Locale.ROOT ) ) {
                        case "hs":
                        case "human":
                        case "homo sapiens":
                            return "params.hs.json";
                        case "mm":
                        case "mouse":
                        case "mus musculus":
                            return "params.mm.json";
                        default:
                            break;
                    }
                }
            } catch ( IOException e ) {
                throw new PipelineSchedulerException( "could not parse paramsJson for organism/params-file: " + e.getMessage(), e );
            }
        }
        throw new PipelineSchedulerException( "paramsJson must specify 'paramsFile' or a recognized 'organism' (hs/mm)" );
    }

    private String weblogUrl( Long jobId ) {
        String base = weblogBaseUrl.endsWith( "/" ) ? weblogBaseUrl.substring( 0, weblogBaseUrl.length() - 1 ) : weblogBaseUrl;
        // The token rides in the path, not a query string or header: -with-weblog can't set headers, and
        // a path segment doesn't depend on Nextflow preserving the query.
        return base + "/rest/v2/internal/pipeline/jobs/" + jobId + "/weblog/"
                + PipelineCallbackTokens.forJob( callbackSecret, jobId );
    }
}
