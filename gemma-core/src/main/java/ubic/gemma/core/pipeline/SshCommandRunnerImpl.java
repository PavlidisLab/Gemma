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

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Real {@link SshCommandRunner}: shells out to the system {@code ssh} client to run a command on the
 * Slurm submit node (R4/R13). This is the only cluster-touching part of the Nextflow adapter — it is
 * NOT unit-tested (the scheduler's logic is tested with a fake runner); its correctness is covered by
 * the end-to-end task-7 acceptance run.
 *
 * <p>SSH auth is public-key, non-interactive (R5): {@code -i <key>}, {@code BatchMode=yes}, pinned
 * {@code known_hosts}. Connection coordinates come from config; during dev these point at the current
 * account (R9), later at the dedicated service account (O1).</p>
 */
@Component
@Profile("scheduler-nextflow")
@Slf4j
public class SshCommandRunnerImpl implements SshCommandRunner {

    private final String submitHost;
    private final String submitUser;
    private final String sshKeyPath;
    private final String knownHostsPath;
    private final int timeoutSeconds;

    @Autowired
    public SshCommandRunnerImpl(
            @Value("${gemma.pipeline.nextflow.submitHost:}") String submitHost,
            @Value("${gemma.pipeline.nextflow.submitUser:}") String submitUser,
            @Value("${gemma.pipeline.nextflow.sshKeyPath:}") String sshKeyPath,
            @Value("${gemma.pipeline.nextflow.knownHostsPath:}") String knownHostsPath,
            @Value("${gemma.pipeline.nextflow.sshTimeoutSeconds:60}") int timeoutSeconds ) {
        this.submitHost = submitHost;
        this.submitUser = submitUser;
        this.sshKeyPath = sshKeyPath;
        this.knownHostsPath = knownHostsPath;
        this.timeoutSeconds = timeoutSeconds;
    }

    @Override
    public CommandResult run( List<String> remoteCommand ) throws PipelineSchedulerException {
        if ( submitHost.isBlank() ) {
            throw new PipelineSchedulerException( "gemma.pipeline.nextflow.submitHost is not configured" );
        }
        List<String> argv = new ArrayList<>();
        argv.add( "ssh" );
        argv.add( "-o" );
        argv.add( "BatchMode=yes" );
        if ( !sshKeyPath.isBlank() ) {
            argv.add( "-i" );
            argv.add( sshKeyPath );
        }
        if ( !knownHostsPath.isBlank() ) {
            argv.add( "-o" );
            argv.add( "UserKnownHostsFile=" + knownHostsPath );
        }
        argv.add( submitUser.isBlank() ? submitHost : submitUser + "@" + submitHost );
        argv.add( "--" );
        argv.addAll( remoteCommand );
        try {
            Process p = new ProcessBuilder( argv ).start();
            String stdout = new String( p.getInputStream().readAllBytes(), StandardCharsets.UTF_8 );
            String stderr = new String( p.getErrorStream().readAllBytes(), StandardCharsets.UTF_8 );
            if ( !p.waitFor( timeoutSeconds, TimeUnit.SECONDS ) ) {
                p.destroyForcibly();
                throw new PipelineSchedulerException( "ssh command timed out after " + timeoutSeconds + "s: " + remoteCommand );
            }
            int exit = p.exitValue();
            log.debug( "ssh {} -> exit {}", remoteCommand, exit );
            return new CommandResult( exit, stdout, stderr );
        } catch ( IOException e ) {
            throw new PipelineSchedulerException( "ssh transport failed: " + e.getMessage(), e );
        } catch ( InterruptedException e ) {
            Thread.currentThread().interrupt();
            throw new PipelineSchedulerException( "ssh command interrupted", e );
        }
    }
}
