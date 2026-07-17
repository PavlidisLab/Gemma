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

import org.junit.jupiter.api.Test;
import ubic.gemma.model.pipeline.JobState;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit test for the pure command/parse logic of the Nextflow-on-Slurm adapter. No IO, no cluster.
 */
class NextflowSlurmCommandBuilderTest {

    private final NextflowSlurmCommandBuilder b = new NextflowSlurmCommandBuilder();

    @Test
    void samplesheet_isOneStudyRow() {
        assertThat( b.samplesheetCsv( "GSE124952" ) )
                .isEqualTo( "sample,study_name,study_path\nGSE124952,GSE124952,\n" );
    }

    @Test
    void launchScript_hasAllNextflowFlags() {
        String s = b.launchScript( "/pipe/sc-annotation", "conda", "params.hs.json",
                "/space/gemmaData/pipeline/7/samplesheet.csv",
                "http://gemma/rest/v2/internal/pipeline/jobs/7/weblog",
                "/space/gemmaData/pipeline/7" );
        assertThat( s ).startsWith( "#!/bin/bash\nset -euo pipefail\n" );
        assertThat( s ).contains( "nextflow run /pipe/sc-annotation/main.nf" );  // default executable
        assertThat( s ).doesNotContain( "\n\n" );
        assertThat( s ).contains( "-profile conda" );
        assertThat( s ).contains( "-params-file /pipe/sc-annotation/params.hs.json" );
        assertThat( s ).contains( "--input /space/gemmaData/pipeline/7/samplesheet.csv" );
        assertThat( s ).contains( "-process.executor slurm" );
        assertThat( s ).contains( "-with-weblog http://gemma/rest/v2/internal/pipeline/jobs/7/weblog" );
        assertThat( s ).contains( "-resume" );
        assertThat( s ).contains( "-work-dir /space/gemmaData/pipeline/7" );
    }

    @Test
    void launchScript_usesConfiguredNextflowExecutable() {
        // Where nextflow isn't on the non-login PATH (e.g. scratchy), the wrapper must call it by
        // absolute path — driven by gemma.pipeline.nextflow.executable.
        String s = new NextflowSlurmCommandBuilder( "/space/opt/bin/nextflow" )
                .launchScript( "/pipe", "conda", "params.hs.json", "/w/samplesheet.csv", "http://g/weblog", "/w" );
        assertThat( s ).contains( "/space/opt/bin/nextflow run /pipe/main.nf" );
        assertThat( s ).doesNotContain( "\nnextflow run" );
    }

    @Test
    void slurmCommands_areWellFormed() {
        assertThat( b.sbatchCommand( "/x/launch.sh" ) ).containsExactly( "sbatch", "--parsable", "/x/launch.sh" );
        assertThat( b.squeueCommand( "42" ) ).containsExactly( "squeue", "-j", "42", "-h", "-o", "%T" );
        assertThat( b.scontrolShowJobCommand( "42" ) ).containsExactly( "scontrol", "show", "job", "42" );
        assertThat( b.scancelCommand( "42" ) ).containsExactly( "scancel", "42" );
    }

    @Test
    void parseSbatchJobId_takesIdBeforeSemicolon() throws Exception {
        assertThat( b.parseSbatchJobId( "12345\n" ) ).isEqualTo( "12345" );
        assertThat( b.parseSbatchJobId( "12345;cluster0\n" ) ).isEqualTo( "12345" );
    }

    @Test
    void parseSbatchJobId_rejectsNonNumeric() {
        assertThatThrownBy( () -> b.parseSbatchJobId( "Submitted batch job oops" ) )
                .isInstanceOf( PipelineSchedulerException.class );
        assertThatThrownBy( () -> b.parseSbatchJobId( "" ) )
                .isInstanceOf( PipelineSchedulerException.class );
    }

    @Test
    void squeueState_blankMeansGoneFromQueue() {
        assertThat( b.parseSqueueState( "" ) ).isNull();
        assertThat( b.parseSqueueState( "   \n" ) ).isNull();
        assertThat( b.parseSqueueState( "RUNNING\n" ) ).isEqualTo( JobState.RUNNING );
        assertThat( b.parseSqueueState( "PENDING\n" ) ).isEqualTo( JobState.QUEUED );
    }

    @Test
    void scontrolState_extractsJobStateToken() {
        // Realistic `scontrol show job` output — one line of many key=value tokens.
        String running = "JobId=42 JobName=gemma UserId=x(1) JobState=RUNNING Reason=None Dependency=(null)";
        assertThat( b.parseScontrolState( running ) ).isEqualTo( JobState.RUNNING );
        assertThat( b.parseScontrolState( "JobId=42 JobState=COMPLETED Reason=None" ) ).isEqualTo( JobState.DONE );
        assertThat( b.parseScontrolState( "JobId=42 JobState=CANCELLED Reason=None" ) ).isEqualTo( JobState.CANCELLED );
        // Job purged / "Invalid job id specified" → no JobState token → null.
        assertThat( b.parseScontrolState( "slurm_load_jobs error: Invalid job id specified" ) ).isNull();
        assertThat( b.parseScontrolState( "" ) ).isNull();
    }

    @Test
    void mapSlurmState_coversTheStateSpace() {
        assertThat( b.mapSlurmState( "PENDING" ) ).isEqualTo( JobState.QUEUED );
        assertThat( b.mapSlurmState( "RUNNING" ) ).isEqualTo( JobState.RUNNING );
        assertThat( b.mapSlurmState( "COMPLETING" ) ).isEqualTo( JobState.RUNNING );
        assertThat( b.mapSlurmState( "COMPLETED" ) ).isEqualTo( JobState.DONE );
        assertThat( b.mapSlurmState( "TIMEOUT" ) ).isEqualTo( JobState.FAILED );
        assertThat( b.mapSlurmState( "OUT_OF_MEMORY" ) ).isEqualTo( JobState.FAILED );
        assertThat( b.mapSlurmState( "NODE_FAIL" ) ).isEqualTo( JobState.FAILED );
        assertThat( b.mapSlurmState( "CANCELLED" ) ).isEqualTo( JobState.CANCELLED );
        // Unknown / future states surface as null rather than a wrong guess.
        assertThat( b.mapSlurmState( "SOME_NEW_STATE" ) ).isNull();
        assertThat( b.mapSlurmState( "" ) ).isNull();
    }

    @Test
    void blankArgs_areRejected() {
        assertThatThrownBy( () -> b.samplesheetCsv( " " ) ).isInstanceOf( IllegalArgumentException.class );
        assertThatThrownBy( () -> b.sbatchCommand( null ) ).isInstanceOf( IllegalArgumentException.class );
    }
}
