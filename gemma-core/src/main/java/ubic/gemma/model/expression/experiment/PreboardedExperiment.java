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
 * <p>This is a read-side compatibility port from {@code phase2-acl-migrate}:
 * just enough Hibernate metadata to MAP an INVESTIGATION row whose
 * discriminator is {@code PreboardedExperiment} without throwing a
 * {@code WrongClassException} on a hotfix-1.32.7 instance that points at a
 * database where the phase2 schema has been applied. The full workflow
 * (creation endpoint, AgentProposal, promotion, WorkflowState lifecycle)
 * lands with the phase2 ship.</p>
 *
 * <p>Single-table inheritance under {@code INVESTIGATION} with discriminator
 * value {@code PreboardedExperiment}; sibling of {@link ExpressionExperiment}
 * and {@link ExpressionExperimentSubSet}.</p>
 */
public class PreboardedExperiment extends Investigation {

    private String accession;
    private String source = "GEO";
    /**
     * Free-form JSON payload of identifying metadata the agent harvested before
     * loading (title, summary, submitter, pubmed id, etc.). Stored as LONGTEXT.
     */
    private String identifyingMetadata;

    public PreboardedExperiment() {
        super();
    }

    /**
     * @return the upstream accession this preboarded targets (e.g. GSE12345).
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
