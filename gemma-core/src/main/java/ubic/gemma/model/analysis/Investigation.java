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

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.Inheritance;
import jakarta.persistence.Lob;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
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
@Entity
@Table(name = "INVESTIGATION", indexes = {
        @Index(name = "INVESTIGATION_NAME", columnList = "NAME"),
        @Index(name = "INVESTIGATION_WORKFLOW_STATE", columnList = "WORKFLOW_STATE"),
        @Index(name = "INVESTIGATION_NUMBER_OF_DATA_VECTORS", columnList = "NUMBER_OF_DATA_VECTORS"),
        @Index(name = "INVESTIGATION_NUMBER_OF_SAMPLES", columnList = "NUMBER_OF_SAMPLES"),
        @Index(name = "INVESTIGATION_PREBOARDED_ACCESSION", columnList = "PREBOARDED_ACCESSION")
})
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "class", length = 255)
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public abstract class Investigation extends AbstractAuditable implements Securable {

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "INVESTIGATION_FK", columnDefinition = "BIGINT",
            foreignKey = @ForeignKey(name = "CHARACTERISTIC_INVESTIGATION_FKC"))
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private Set<Characteristic> characteristics = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "RELEVANT_PUBLICATIONS",
            joinColumns = @JoinColumn(name = "INVESTIGATIONS_FK", columnDefinition = "BIGINT"),
            inverseJoinColumns = @JoinColumn(name = "OTHER_RELEVANT_PUBLICATIONS_FK", columnDefinition = "BIGINT"),
            foreignKey = @ForeignKey(name = "BIBLIOGRAPHIC_REFERENCE_INVESTIGATIONS_FKC"))
    private Set<BibliographicReference> otherRelevantPublications = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "OWNER_FK", columnDefinition = "BIGINT")
    private Contact owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PRIMARY_PUBLICATION_FK", columnDefinition = "BIGINT")
    private BibliographicReference primaryPublication;

    /**
     * Eight-state workflow lifecycle position. See
     * {@link ubic.gemma.model.expression.experiment.WorkflowState} and
     * {@code HANDOFF_WORKFLOW_STATE_STORAGE.md}. Defaults to
     * {@link WorkflowState#Loaded} on legacy rows (the migration backfill is
     * the same — a curator-approved refinement is deferred).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "WORKFLOW_STATE", nullable = false, columnDefinition = "VARCHAR(32)")
    private WorkflowState workflowState = WorkflowState.Loaded;

    /**
     * Timestamp at which the dataset entered its current
     * {@link #workflowState}. Null on legacy rows that pre-date the column;
     * populated by the workflow service on every transition.
     */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "WORKFLOW_STATE_ENTERED_AT", columnDefinition = "DATETIME")
    private Date workflowStateEnteredAt;

    /**
     * Verbatim upstream metadata for this investigation, as a JSON document — for a GEO experiment,
     * the series and per-sample fields as the submitter wrote them.
     * <p>
     * This is an immutable cache of what the source said, not curation: it is rebuildable from GEO and
     * carries no judgement of ours. Our own (mutable, unrebuildable) curation belongs on
     * {@code AnnotationSet} instead.
     * <p>
     * It lives on {@code Investigation} rather than on {@code PreboardedExperiment} because the same
     * payload is wanted for imported experiments, not only preboarded ones — the agent needs the raw
     * per-sample view to tell "the submitter copy-pasted this column" from "this column is a real
     * factor", which the converter's flattened {@code BioMaterial.characteristics} cannot answer.
     * <p>
     * Opaque to Gemma: the schema is owned by the agents repo and versioned by
     * {@link #sourceMetadataSchemaVersion}, the same arrangement as
     * {@code Characteristic.SUPPORTING_EVIDENCE}. Keys are camelCase, normalized once at ingestion on
     * the consuming side.
     */
    @Lob
    @Column(name = "SOURCE_METADATA", columnDefinition = "LONGTEXT")
    private String sourceMetadata;

    /**
     * Schema version of {@link #sourceMetadata}, or null when no payload is stored.
     * <p>
     * Versioned rather than additive-only because the payload's shape is expected to change and old
     * rows must stay readable — a consumer has to be able to ask which era a row is from. Schema v1 was
     * agreed with CAB on 2026-08-09.
     */
    @Column(name = "SOURCE_METADATA_SCHEMA_VERSION", columnDefinition = "SMALLINT")
    private Integer sourceMetadataSchemaVersion;

    public String getSourceMetadata() {
        return sourceMetadata;
    }

    public void setSourceMetadata( String sourceMetadata ) {
        this.sourceMetadata = sourceMetadata;
    }

    public Integer getSourceMetadataSchemaVersion() {
        return sourceMetadataSchemaVersion;
    }

    public void setSourceMetadataSchemaVersion( Integer sourceMetadataSchemaVersion ) {
        this.sourceMetadataSchemaVersion = sourceMetadataSchemaVersion;
    }

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
