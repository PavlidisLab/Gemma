/*
 * The Gemma project
 *
 * Copyright (c) 2026 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 */
package ubic.gemma.persistence.service.expression.experiment;

import ubic.gemma.model.expression.experiment.WorkflowState;

import java.util.Collections;
import java.util.Set;

/**
 * Thrown when
 * {@link WorkflowService#advance} is
 * asked for a transition the state machine forbids (e.g. {@code Discovery
 * -> Public}). The REST layer maps this to a 409 Conflict with a body
 * listing {@link #getAllowedNextStates()}.
 */
public class DisallowedWorkflowTransitionException extends RuntimeException {

    private final WorkflowState currentState;
    private final WorkflowState targetState;
    private final Set<WorkflowState> allowedNextStates;

    public DisallowedWorkflowTransitionException( WorkflowState currentState, WorkflowState targetState,
            Set<WorkflowState> allowedNextStates ) {
        super( "Disallowed workflow transition " + currentState + " -> " + targetState
                + "; allowed next states: " + allowedNextStates );
        this.currentState = currentState;
        this.targetState = targetState;
        this.allowedNextStates = Collections.unmodifiableSet( allowedNextStates );
    }

    public WorkflowState getCurrentState() {
        return currentState;
    }

    public WorkflowState getTargetState() {
        return targetState;
    }

    public Set<WorkflowState> getAllowedNextStates() {
        return allowedNextStates;
    }
}
