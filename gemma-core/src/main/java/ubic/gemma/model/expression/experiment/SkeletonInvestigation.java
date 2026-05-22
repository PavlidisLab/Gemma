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
 */
package ubic.gemma.model.expression.experiment;

import ubic.gemma.model.analysis.Investigation;

/**
 * Subclass of {@link Investigation} representing a proposed-but-not-yet-loaded
 * dataset.
 *
 * <p>Created by {@code POST /skeletons} when the curation-agents runner targets
 * a GEO (or other) accession that has not yet been imported into Gemma. The
 * skeleton carries enough identifying metadata to triage / re-run the agent
 * against it, and accumulates one or more {@code AgentProposal} rows over
 * time. When the data lands as an {@code ExpressionExperiment}, the skeleton
 * is promoted via {@code POST /skeletons/{id}/promote} — the
 * implementation rebinds the {@code AgentProposal} FKs to the new EE row
 * (new-row + FK rebind approach; see {@code HANDOFF_PROPOSED_EXPERIMENT_WORKFLOW.md}
 * §"Open questions" item 1 and STATUS file for rationale).</p>
 *
 * <p>Single-table inheritance under {@code INVESTIGATION} with discriminator
 * value {@code SkeletonInvestigation}; sibling of {@link ExpressionExperiment}
 * and {@link ExpressionExperimentSubSet}.</p>
 *
 * <p>Defaults its {@link WorkflowState} to {@link WorkflowState#Skeleton} on
 * construction (collapsing handoff states 1+2; see STATUS file). Promotion
 * advances the resulting EE to {@link WorkflowState#Loaded}.</p>
 */
public class SkeletonInvestigation extends Investigation {

    private String accession;
    private String source = "GEO";
    /**
     * Free-form JSON payload of identifying metadata the agent harvested before
     * loading (title, summary, submitter, pubmed id, etc.). Stored as LONGTEXT
     * — the structured proposal lives separately in {@code AgentProposal}.
     */
    private String identifyingMetadata;

    public SkeletonInvestigation() {
        super();
        setWorkflowState( WorkflowState.Skeleton );
    }

    /**
     * @return the upstream accession this skeleton targets (e.g. GSE12345).
     *         Required; the create endpoint enforces it.
     */
    public String getAccession() {
        return accession;
    }

    public void setAccession( String accession ) {
        this.accession = accession;
    }

    /**
     * @return the upstream source this accession belongs to. Defaults to
     *         {@code "GEO"}; future values may include {@code "ArrayExpress"},
     *         {@code "manual"}.
     */
    public String getSource() {
        return source;
    }

    public void setSource( String source ) {
        this.source = source;
    }

    /**
     * @return the identifying metadata blob (title, summary, submitter,
     *         pubmed id, etc.) as a JSON string, or {@code null} if the
     *         agent did not harvest it.
     */
    public String getIdentifyingMetadata() {
        return identifyingMetadata;
    }

    public void setIdentifyingMetadata( String identifyingMetadata ) {
        this.identifyingMetadata = identifyingMetadata;
    }

    @Override
    public int hashCode() {
        if ( getId() != null ) {
            return getId().hashCode();
        }
        if ( accession != null ) {
            return accession.hashCode();
        }
        return System.identityHashCode( this );
    }

    @Override
    public boolean equals( Object object ) {
        if ( this == object ) return true;
        if ( !( object instanceof SkeletonInvestigation ) ) return false;
        SkeletonInvestigation other = ( SkeletonInvestigation ) object;
        if ( getId() != null && other.getId() != null ) {
            return getId().equals( other.getId() );
        }
        // Fall back to accession when both are transient; identity otherwise.
        if ( accession != null && other.accession != null ) {
            return accession.equals( other.accession );
        }
        return false;
    }
}
