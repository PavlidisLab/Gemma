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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

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

    private final ExpressionExperimentService expressionExperimentService;
    private final SshCommandRunner ssh;
    private final NextflowSlurmCommandBuilder commands;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final String checkoutDir;
    private final String workDirBase;
    private final String profile;
    private final String weblogBaseUrl;

    @Autowired
    public NextflowSlurmScheduler(
            ExpressionExperimentService expressionExperimentService,
            SshCommandRunner ssh,
            @Value("${gemma.pipeline.nextflow.checkoutDir:}") String checkoutDir,
            @Value("${gemma.pipeline.nextflow.workDirBase:${gemma.appdata.home}/pipeline}") String workDirBase,
            @Value("${gemma.pipeline.nextflow.profile:conda}") String profile,
            @Value("${gemma.pipeline.nextflow.executable:nextflow}") String nextflowExecutable,
            // Base URL the compute-node weblog POSTs back to. Defaults to gemma.hosturl, but is a
            // SEPARATE knob because the cluster may reach Gemma at a different address than clients do
            // — e.g. an SSH tunnel endpoint on the submit node when a firewall blocks the direct port.
            @Value("${gemma.pipeline.nextflow.weblogBaseUrl:${gemma.hosturl:}}") String weblogBaseUrl ) {
        this.expressionExperimentService = expressionExperimentService;
        this.ssh = ssh;
        this.commands = new NextflowSlurmCommandBuilder( nextflowExecutable );
        this.checkoutDir = checkoutDir;
        this.workDirBase = workDirBase;
        this.profile = profile;
        this.weblogBaseUrl = weblogBaseUrl;
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
        Path workDir = Path.of( workDirBase, String.valueOf( jobId ) );
        Path samplesheet = workDir.resolve( "samplesheet.csv" );
        Path script = workDir.resolve( "launch.sh" );
        try {
            Files.createDirectories( workDir );
            Files.writeString( samplesheet, commands.samplesheetCsv( studyName ), StandardCharsets.UTF_8 );
            Files.writeString( script, commands.launchScript( checkoutDir, profile, paramsFile,
                    samplesheet.toString(), weblogUrl( jobId ), workDir.toString() ), StandardCharsets.UTF_8 );
        } catch ( IOException e ) {
            throw new PipelineSchedulerException( "failed to write work-dir files under " + workDir + ": " + e.getMessage(), e );
        }

        SshCommandRunner.CommandResult res = ssh.run( commands.sbatchCommand( script.toString() ) );
        if ( !res.isSuccess() ) {
            throw new PipelineSchedulerException( "sbatch failed (exit " + res.getExitCode() + "): " + res.getStderr().trim() );
        }
        String headJobId = commands.parseSbatchJobId( res.getStdout() );
        log.info( "submitted job {} (EE {} '{}') as Slurm head job {}", jobId, req.getExperimentId(), studyName, headJobId );
        return new SchedulerHandle( SchedulerKind.NEXTFLOW, headJobId );
    }

    @Override
    @Nullable
    public JobSnapshot poll( SchedulerHandle handle ) throws PipelineSchedulerException {
        // squeue first (live); fall back to `scontrol show job` once the job has left the queue
        // (accounting/sacct is disabled on our cluster; scontrol needs no accounting but forgets the
        // job after MinJobAge ~300s — beyond that the terminal state comes from the weblog push).
        SshCommandRunner.CommandResult sq = ssh.run( commands.squeueCommand( handle.getId() ) );
        JobState state = commands.parseSqueueState( sq.getStdout() );
        String raw = sq.getStdout().trim();
        if ( state == null ) {
            SshCommandRunner.CommandResult sc = ssh.run( commands.scontrolShowJobCommand( handle.getId() ) );
            state = commands.parseScontrolState( sc.getStdout() );
            raw = sc.getStdout().trim();
        }
        if ( state == null ) {
            // Neither squeue nor scontrol knows this job — SPI contract: null ⇒ terminal-unknown.
            log.warn( "poll: Slurm has no record of head job {}", handle.getId() );
            return null;
        }
        return new JobSnapshot( state, raw.isEmpty() ? null : raw, null );
    }

    @Override
    public void cancel( SchedulerHandle handle ) throws PipelineSchedulerException {
        SshCommandRunner.CommandResult res = ssh.run( commands.scancelCommand( handle.getId() ) );
        if ( !res.isSuccess() ) {
            // scancel of an already-finished/purged job is non-fatal — the job is gone either way.
            log.warn( "scancel of head job {} returned exit {}: {}", handle.getId(), res.getExitCode(), res.getStderr().trim() );
        }
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
        return base + "/rest/v2/internal/pipeline/jobs/" + jobId + "/weblog";
    }
}
