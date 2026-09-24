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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ubic.gemma.core.pipeline.SshCommandRunner.CommandResult;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.pipeline.JobState;
import ubic.gemma.model.pipeline.SchedulerKind;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link NextflowSlurmScheduler} with a fake {@link SshCommandRunner} (no cluster) and a
 * mocked EE lookup. Work-dir files are written to a real {@link TempDir}. Covers submit (files + sbatch
 * + handle), poll (squeue → sacct fallback → unknown), cancel, and the error paths.
 */
class NextflowSlurmSchedulerTest {

    private static final String SECRET = "callback-secret";

    /** Fake runner: records every remote command and returns a canned result keyed by argv[0]. */
    static class FakeSsh implements SshCommandRunner {
        final List<List<String>> calls = new ArrayList<>();
        final Map<String, CommandResult> byVerb = new HashMap<>();

        void on( String verb, int exit, String stdout, String stderr ) {
            byVerb.put( verb, new CommandResult( exit, stdout, stderr ) );
        }

        @Override
        public CommandResult run( List<String> remoteCommand ) {
            calls.add( remoteCommand );
            return byVerb.getOrDefault( remoteCommand.get( 0 ), new CommandResult( 0, "", "" ) );
        }

        List<String> lastCallStartingWith( String verb ) {
            for ( int i = calls.size() - 1; i >= 0; i-- ) {
                if ( calls.get( i ).get( 0 ).equals( verb ) ) {
                    return calls.get( i );
                }
            }
            return null;
        }
    }

    @TempDir
    Path workDirBase;

    private ExpressionExperimentService eeService;
    private FakeSsh ssh;
    private NextflowSlurmScheduler scheduler;

    @BeforeEach
    void setUp() {
        eeService = mock( ExpressionExperimentService.class );
        ssh = new FakeSsh();
        scheduler = new NextflowSlurmScheduler( eeService, ssh,
                "/pipe/sc-annotation", workDirBase.toString(), "conda", "nextflow", "http://gemma:8080/", SECRET );
        ExpressionExperiment ee = mock( ExpressionExperiment.class );
        when( ee.getShortName() ).thenReturn( "GSE124952" );
        when( eeService.load( 55L ) ).thenReturn( ee );
    }

    @Test
    void submit_withoutCallbackSecret_throwsBeforeSubmitting() {
        NextflowSlurmScheduler unconfigured = new NextflowSlurmScheduler( eeService, ssh,
                "/pipe/sc-annotation", workDirBase.toString(), "conda", "nextflow", "http://gemma:8080/", "" );
        assertThatThrownBy( () -> unconfigured.submit( req( "{\"organism\":\"hs\"}" ) ) )
                .isInstanceOf( PipelineSchedulerException.class )
                .hasMessageContaining( "gemma.pipeline.callback.token" );
        assertThat( ssh.lastCallStartingWith( "sbatch" ) ).isNull();
    }

    @Test
    void readLog_pagesHeadOutputWithACursor() throws Exception {
        Path jobDir = Files.createDirectories( workDirBase.resolve( "7" ) );
        Files.writeString( jobDir.resolve( "head.out" ), "N E X T F L O W\nexecutor > slurm\n" );
        SchedulerHandle h = new SchedulerHandle( SchedulerKind.NEXTFLOW, "98765" );

        LogChunk first = scheduler.readLog( 7L, h, 0, 5 );
        assertThat( first.getText() ).isEqualTo( "N E X" );
        assertThat( first.getNextOffset() ).isEqualTo( 5 );
        assertThat( first.isEof() ).isFalse();

        LogChunk rest = scheduler.readLog( 7L, h, first.getNextOffset(), 1024 );
        assertThat( first.getText() + rest.getText() ).isEqualTo( "N E X T F L O W\nexecutor > slurm\n" );
        assertThat( rest.isEof() ).isTrue();

        // Nothing new yet: an empty chunk at the same cursor, so a poller can simply retry.
        LogChunk idle = scheduler.readLog( 7L, h, rest.getNextOffset(), 1024 );
        assertThat( idle.getText() ).isEmpty();
        assertThat( idle.getNextOffset() ).isEqualTo( rest.getNextOffset() );
        assertThat( idle.isEof() ).isTrue();
    }

    @Test
    void readLog_beforeTheJobStarted_isEmptyNotAnError() throws Exception {
        LogChunk c = scheduler.readLog( 7L, new SchedulerHandle( SchedulerKind.NEXTFLOW, "1" ), 0, 1024 );
        assertThat( c.getText() ).isEmpty();
        assertThat( c.getNextOffset() ).isZero();
        assertThat( c.isEof() ).isTrue();
    }

    @Test
    void readLog_neverSplitsAMultiByteCharacter() throws Exception {
        Path jobDir = Files.createDirectories( workDirBase.resolve( "7" ) );
        Files.writeString( jobDir.resolve( "head.out" ), "ab✓cd" ); // ✓ is 3 bytes
        SchedulerHandle h = new SchedulerHandle( SchedulerKind.NEXTFLOW, "1" );

        LogChunk c = scheduler.readLog( 7L, h, 0, 4 ); // would end inside ✓
        assertThat( c.getText() ).isEqualTo( "ab" );
        assertThat( c.getNextOffset() ).isEqualTo( 2 );
        assertThat( scheduler.readLog( 7L, h, c.getNextOffset(), 1024 ).getText() ).isEqualTo( "✓cd" );
    }

    @Test
    void readArtifact_servesOnlyTheListedWorkDirFiles() throws Exception {
        Path jobDir = Files.createDirectories( workDirBase.resolve( "7" ) );
        Files.writeString( jobDir.resolve( "report.html" ), "<html>report</html>" );
        Files.writeString( jobDir.resolve( "samplesheet.csv" ), "sample,study_name,study_path\n" );
        SchedulerHandle h = new SchedulerHandle( SchedulerKind.NEXTFLOW, "1" );

        Artifact report = scheduler.readArtifact( 7L, h, "report.html" );
        assertThat( report ).isNotNull();
        assertThat( report.getContentType() ).startsWith( "text/html" );
        assertThat( new String( report.getContent(), StandardCharsets.UTF_8 ) ).isEqualTo( "<html>report</html>" );

        assertThat( scheduler.readArtifact( 7L, h, "samplesheet.csv" ) ).as( "exists, but not listed" ).isNull();
        assertThat( scheduler.readArtifact( 7L, h, "trace.txt" ) ).as( "listed, not written yet" ).isNull();
    }

    @Test
    void readArtifact_overTheSizeLimit_throwsInsteadOfLoadingIt() throws Exception {
        Path jobDir = Files.createDirectories( workDirBase.resolve( "7" ) );
        try ( RandomAccessFile f = new RandomAccessFile( jobDir.resolve( ".nextflow.log" ).toFile(), "rw" ) ) {
            f.setLength( NextflowSlurmScheduler.MAX_ARTIFACT_BYTES + 1 ); // sparse: no real 50 MB written
        }
        assertThatThrownBy( () -> scheduler.readArtifact( 7L, new SchedulerHandle( SchedulerKind.NEXTFLOW, "1" ), ".nextflow.log" ) )
                .isInstanceOf( PipelineSchedulerException.class )
                .hasMessageContaining( "artifact limit" );
    }

    private SubmitRequest req( String paramsJson ) {
        return new SubmitRequest( 7L, "sc-annotation", 55L, paramsJson );
    }

    @Test
    void submit_writesWorkdirFiles_sbatches_andReturnsHeadJobHandle() throws Exception {
        ssh.on( "sbatch", 0, "98765\n", "" );

        SchedulerHandle h = scheduler.submit( req( "{\"organism\":\"hs\"}" ) );

        assertThat( h.getKind() ).isEqualTo( SchedulerKind.NEXTFLOW );
        assertThat( h.getId() ).isEqualTo( "98765" );

        Path jobDir = workDirBase.resolve( "7" );
        assertThat( Files.readString( jobDir.resolve( "samplesheet.csv" ) ) )
                .isEqualTo( "sample,study_name,study_path\nGSE124952,GSE124952,\n" );
        String script = Files.readString( jobDir.resolve( "launch.sh" ) );
        assertThat( script ).contains( "-params-file /pipe/sc-annotation/params.hs.json" );
        assertThat( script ).contains( "-with-weblog http://gemma:8080/rest/v2/internal/pipeline/jobs/7/weblog/"
                + PipelineCallbackTokens.forJob( SECRET, 7L ) );
        assertThat( script ).as( "the shared secret itself never reaches the work-dir" ).doesNotContain( SECRET );
        assertThat( script ).contains( "-work-dir " + jobDir );

        // sbatch was invoked on the wrapper we wrote.
        assertThat( ssh.lastCallStartingWith( "sbatch" ) )
                .containsExactly( "sbatch", "--parsable", "--chdir", jobDir.toString(),
                        "--output", jobDir.resolve( "head.out" ).toString(), jobDir.resolve( "launch.sh" ).toString() );
    }

    @Test
    void submit_sbatchFailure_throws() {
        ssh.on( "sbatch", 1, "", "sbatch: error: Invalid partition" );
        assertThatThrownBy( () -> scheduler.submit( req( "{\"organism\":\"hs\"}" ) ) )
                .isInstanceOf( PipelineSchedulerException.class )
                .hasMessageContaining( "Invalid partition" );
    }

    @Test
    void submit_missingExperiment_throws() {
        when( eeService.load( 999L ) ).thenReturn( null );
        assertThatThrownBy( () -> scheduler.submit( new SubmitRequest( 7L, "sc-annotation", 999L, "{\"organism\":\"hs\"}" ) ) )
                .isInstanceOf( PipelineSchedulerException.class )
                .hasMessageContaining( "no experiment" );
    }

    @Test
    void submit_unresolvableOrganism_throws() {
        assertThatThrownBy( () -> scheduler.submit( req( "{}" ) ) )
                .isInstanceOf( PipelineSchedulerException.class )
                .hasMessageContaining( "organism" );
        assertThatThrownBy( () -> scheduler.submit( req( null ) ) )
                .isInstanceOf( PipelineSchedulerException.class );
    }

    @Test
    void submit_explicitParamsFile_isUsed() throws Exception {
        ssh.on( "sbatch", 0, "1\n", "" );
        scheduler.submit( req( "{\"paramsFile\":\"params.mm.json\"}" ) );
        assertThat( Files.readString( workDirBase.resolve( "7" ).resolve( "launch.sh" ) ) )
                .contains( "-params-file /pipe/sc-annotation/params.mm.json" );
    }

    @Test
    void poll_runningFromSqueue() throws Exception {
        ssh.on( "squeue", 0, "RUNNING\n", "" );
        JobSnapshot snap = scheduler.poll( new SchedulerHandle( SchedulerKind.NEXTFLOW, "42" ) );
        assertThat( snap ).isNotNull();
        assertThat( snap.getState() ).isEqualTo( JobState.RUNNING );
    }

    @Test
    void poll_fallsBackToScontrolWhenSqueueEmpty() throws Exception {
        ssh.on( "squeue", 0, "", "" );        // gone from the queue
        ssh.on( "scontrol", 0, "JobId=42 JobState=COMPLETED Reason=None", "" );
        JobSnapshot snap = scheduler.poll( new SchedulerHandle( SchedulerKind.NEXTFLOW, "42" ) );
        assertThat( snap ).isNotNull();
        assertThat( snap.getState() ).isEqualTo( JobState.DONE );
    }

    @Test
    void poll_unknownToBothReturnsNull() throws Exception {
        ssh.on( "squeue", 0, "", "" );
        ssh.on( "scontrol", 1, "", "slurm_load_jobs error: Invalid job id specified" );
        assertThat( scheduler.poll( new SchedulerHandle( SchedulerKind.NEXTFLOW, "42" ) ) ).isNull();
    }

    @Test
    void cancel_issuesScancel() throws Exception {
        scheduler.cancel( new SchedulerHandle( SchedulerKind.NEXTFLOW, "42" ) );
        assertThat( ssh.lastCallStartingWith( "scancel" ) ).containsExactly( "scancel", "42" );
    }
}
