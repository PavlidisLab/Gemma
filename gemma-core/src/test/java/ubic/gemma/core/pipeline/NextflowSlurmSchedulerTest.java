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
                "/pipe/sc-annotation", workDirBase.toString(), "conda", "http://gemma:8080/" );
        ExpressionExperiment ee = mock( ExpressionExperiment.class );
        when( ee.getShortName() ).thenReturn( "GSE124952" );
        when( eeService.load( 55L ) ).thenReturn( ee );
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
        assertThat( script ).contains( "-with-weblog http://gemma:8080/rest/v2/internal/pipeline/jobs/7/weblog" );
        assertThat( script ).contains( "-work-dir " + jobDir );

        // sbatch was invoked on the wrapper we wrote.
        assertThat( ssh.lastCallStartingWith( "sbatch" ) )
                .containsExactly( "sbatch", "--parsable", jobDir.resolve( "launch.sh" ).toString() );
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
    void poll_fallsBackToSacctWhenSqueueEmpty() throws Exception {
        ssh.on( "squeue", 0, "", "" );        // gone from the queue
        ssh.on( "sacct", 0, "COMPLETED\n", "" );
        JobSnapshot snap = scheduler.poll( new SchedulerHandle( SchedulerKind.NEXTFLOW, "42" ) );
        assertThat( snap ).isNotNull();
        assertThat( snap.getState() ).isEqualTo( JobState.DONE );
    }

    @Test
    void poll_unknownToBothReturnsNull() throws Exception {
        ssh.on( "squeue", 0, "", "" );
        ssh.on( "sacct", 0, "", "" );
        assertThat( scheduler.poll( new SchedulerHandle( SchedulerKind.NEXTFLOW, "42" ) ) ).isNull();
    }

    @Test
    void cancel_issuesScancel() throws Exception {
        scheduler.cancel( new SchedulerHandle( SchedulerKind.NEXTFLOW, "42" ) );
        assertThat( ssh.lastCallStartingWith( "scancel" ) ).containsExactly( "scancel", "42" );
    }
}
