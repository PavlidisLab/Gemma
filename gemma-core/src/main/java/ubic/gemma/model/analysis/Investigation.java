/*
 * The Gemma project.
 *
 * Copyright (c) 2006-2012 University of British Columbia
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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package ubic.gemma.model.analysis;

import ubic.gemma.model.common.auditAndSecurity.AbstractAuditable;
import ubic.gemma.model.common.auditAndSecurity.Contact;
import ubic.gemma.model.common.auditAndSecurity.Securable;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.experiment.WorkflowState;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * An abstract concept of a scientific study
 */
public abstract class Investigation extends AbstractAuditable implements Securable {

    private Set<Characteristic> characteristics = new HashSet<>();
    private Set<BibliographicReference> otherRelevantPublications = new HashSet<>();
    private Contact owner;
    private BibliographicReference primaryPublication;
    /**
     * Eight-state workflow lifecycle position. See
     * {@link ubic.gemma.model.expression.experiment.WorkflowState} and
     * {@code HANDOFF_WORKFLOW_STATE_STORAGE.md}. Defaults to
     * {@link WorkflowState#Loaded} on legacy rows (the migration backfill is
     * the same — a curator-approved refinement is deferred).
     */
    private WorkflowState workflowState = WorkflowState.Loaded;
    /**
     * Timestamp at which the dataset entered its current
     * {@link #workflowState}. Null on legacy rows that pre-date the column;
     * populated by the workflow service on every transition.
     */
    private Date workflowStateEnteredAt;

    /**
     * @return Annotations that describe the experiment as a whole, for example "tumor" or "brain".
     */
    public Set<Characteristic> getCharacteristics() {
        return this.characteristics;
    }

    public void setCharacteristics( Set<Characteristic> characteristics ) {
        this.characteristics = characteristics;
    }

    /**
     * @return A collection of other publications that are directly relevant to this investigation (e.g., use the same
     *         data but
     *         are not the primary publication for the investigation).
     */
    public Set<BibliographicReference> getOtherRelevantPublications() {
        return this.otherRelevantPublications;
    }

    public void setOtherRelevantPublications( Set<BibliographicReference> otherRelevantPublications ) {
        this.otherRelevantPublications = otherRelevantPublications;
    }

    /**
     * @return The contact who owns this investigation. For publicly acquired data, this is the data submitter or
     *         provider.
     */
    public Contact getOwner() {
        return this.owner;
    }

    public void setOwner( Contact owner ) {
        this.owner = owner;
    }

    /**
     * @return The primary citable publication for this investigation.
     */
    public BibliographicReference getPrimaryPublication() {
        return this.primaryPublication;
    }

    public void setPrimaryPublication( BibliographicReference primaryPublication ) {
        this.primaryPublication = primaryPublication;
    }

    /**
     * @return the current workflow-state position of this investigation.
     *         Never null — legacy rows default to {@link WorkflowState#Loaded}.
     */
    public WorkflowState getWorkflowState() {
        return this.workflowState;
    }

    public void setWorkflowState( WorkflowState workflowState ) {
        this.workflowState = workflowState;
    }

    /**
     * @return timestamp at which the dataset entered its current
     *         {@link #getWorkflowState()}, or {@code null} on legacy rows
     *         that pre-date the column.
     */
    public Date getWorkflowStateEnteredAt() {
        return this.workflowStateEnteredAt;
    }

    public void setWorkflowStateEnteredAt( Date workflowStateEnteredAt ) {
        this.workflowStateEnteredAt = workflowStateEnteredAt;
    }

}