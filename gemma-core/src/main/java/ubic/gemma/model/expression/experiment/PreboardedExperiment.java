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

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import ubic.gemma.model.analysis.Investigation;

/**
 * Subclass of {@link Investigation} representing a proposed-but-not-yet-loaded
 * dataset.
 *
 * <p>Created by {@code POST /preboarded} when the curation-agents runner targets
 * a GEO (or other) accession that has not yet been imported into Gemma. The
 * preboarded carries enough identifying metadata to triage / re-run the agent
 * against it, and accumulates one or more {@code AnnotationSet} rows over
 * time. When the data lands as an {@code ExpressionExperiment}, the preboarded
 * is promoted via {@code POST /preboarded/{id}/promote} — the
 * implementation rebinds the {@code AnnotationSet} FKs to the new EE row
 * (new-row + FK rebind approach; see {@code HANDOFF_PROPOSED_EXPERIMENT_WORKFLOW.md}
 * §"Open questions" item 1 and STATUS file for rationale).</p>
 *
 * <p>Single-table inheritance under {@code INVESTIGATION} with discriminator
 * value {@code PreboardedExperiment}; sibling of {@link ExpressionExperiment}
 * and {@link ExpressionExperimentSubSet}.</p>
 *
 * <p>Defaults its {@link WorkflowState} to {@link WorkflowState#Preboarded} on
 * construction (collapsing handoff states 1+2; see STATUS file). Promotion
 * advances the resulting EE to {@link WorkflowState#Loaded}.</p>
 */
@Entity
@DiscriminatorValue("PreboardedExperiment")
public class PreboardedExperiment extends Investigation {

    @Column(name = "PREBOARDED_ACCESSION", columnDefinition = "VARCHAR(255)")
    private String accession;

    @Column(name = "PREBOARDED_SOURCE", columnDefinition = "VARCHAR(32)")
    private String source = "GEO";
    /**
     * JSON-as-string listing the matcher names that flagged this preboarded
     * during a {@code GeoScrapeService} run (e.g. {@code ["brain","tfperturb"]}).
     * Null for preboardeds created outside the scrape pipeline (e.g. via the
     * curation-agent runner directly).
     */
    @Lob
    @Column(name = "PREBOARDED_MATCHED_CRITERIA", columnDefinition = "TEXT")
    private String matchedCriteria;

    public PreboardedExperiment() {
        super();
        setWorkflowState( WorkflowState.Preboarded );
    }

    /**
     * @return the upstream accession this preboarded targets (e.g. GSE12345).
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
     * @return JSON-as-string listing matcher names from the GEO scrape pipeline
     *         that flagged this preboarded, or {@code null} if not produced by
     *         the scrape pipeline.
     */
    public String getMatchedCriteria() {
        return matchedCriteria;
    }

    public void setMatchedCriteria( String matchedCriteria ) {
        this.matchedCriteria = matchedCriteria;
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
        if ( !( object instanceof PreboardedExperiment ) ) return false;
        PreboardedExperiment other = ( PreboardedExperiment ) object;
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
