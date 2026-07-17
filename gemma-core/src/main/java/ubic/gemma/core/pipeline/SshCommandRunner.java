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

import lombok.Value;

import java.util.List;

/**
 * The one impure edge of {@link NextflowSlurmScheduler}: run a command on the Slurm submit node over
 * SSH (R4/R13 — Gemma's container has no Slurm client, so it dispatches by SSH-ing to a node). Behind
 * an interface so the scheduler's submit/poll/cancel logic is unit-testable with a fake runner; the
 * real {@link SshCommandRunnerImpl} shells out to {@code ssh} and is the only part that needs the node.
 */
public interface SshCommandRunner {

    /**
     * Run {@code remoteCommand} (already tokenized argv) on the submit node and return its result. The
     * implementation is responsible for wrapping it in the SSH invocation (host, user, key, batch mode).
     *
     * @throws PipelineSchedulerException if the SSH transport itself fails (connect/auth/timeout) — a
     *                                    non-zero <em>remote</em> exit is returned in the result, not thrown.
     */
    CommandResult run( List<String> remoteCommand ) throws PipelineSchedulerException;

    /** Result of a remote command: the remote process's exit code and captured streams. */
    @Value
    class CommandResult {
        int exitCode;
        String stdout;
        String stderr;

        public boolean isSuccess() {
            return exitCode == 0;
        }
    }
}
