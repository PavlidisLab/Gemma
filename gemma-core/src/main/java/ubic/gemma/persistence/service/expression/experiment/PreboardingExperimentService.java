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
package ubic.gemma.persistence.service.expression.experiment;

import org.springframework.lang.Nullable;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.PreboardingExperiment;

import java.util.List;

/**
 * Service surface for {@link PreboardingExperiment} CRUD + accession resolution
 * + promotion. See {@code HANDOFF_PROPOSED_EXPERIMENT_WORKFLOW.md} §"Required
 * endpoints".
 */
public interface PreboardingExperimentService {

    /**
     * Create a new preboarding for the given accession.
     *
     * <p>Throws {@link AccessionAlreadyExistsException} if an existing
     * {@link PreboardingExperiment} OR {@link ExpressionExperiment} already
     * carries the same accession. The exception carries the existing entity's
     * id and type so the REST layer can mint a 409 response per spec.</p>
     *
     * <p>Emits a {@code PreboardingCreatedEvent} audit row against the new
     * preboarding's own audit trail.</p>
     */
    PreboardingExperiment createPreboarding( String accession, @Nullable String source,
            @Nullable String identifyingMetadata )
            throws AccessionAlreadyExistsException;

    /**
     * @return the preboarding with the given id, or {@code null}.
     */
    @Nullable
    PreboardingExperiment load( Long id );

    /**
     * @return the preboarding with the given accession, or {@code null}.
     */
    @Nullable
    PreboardingExperiment findByAccession( String accession );

    /**
     * @return list of preboarding with the given accession (defensive — only
     *         one is expected; the create path enforces uniqueness).
     */
    List<PreboardingExperiment> findAllByAccession( String accession );

    /**
     * Look up an existing {@link ExpressionExperiment} carrying the given
     * accession. Used by the {@code POST /preboarding} 409 path: if the data
     * is already loaded as an EE, the caller should write against the EE
     * directly rather than create a preboarding.
     *
     * @return the existing EE with this accession, or {@code null}.
     */
    @Nullable
    ExpressionExperiment findExpressionExperimentByAccession( String accession );

    /**
     * Promote the preboarding to a loaded {@link ExpressionExperiment}.
     *
     * <p>The implementation rebinds every {@code AgentProposal} attached to
     * the preboarding so it points at the EE row instead (new-row + FK rebind
     * approach; see {@code STATUS_PROPOSED_EXPERIMENT_WORKFLOW.md} for the
     * trade-off discussion). The preboarding's workflow state is advanced to
     * {@code Loaded} (terminal marker — the row is retained for history;
     * it carries no curatable artifacts) and the EE's workflow state is
     * advanced to {@code Loaded} too.</p>
     *
     * <p>Emits a {@code PreboardingPromotedEvent} on the EE's audit trail
     * (post-promotion the EE is the authoritative parent of the trail).
     * The {@code ee} argument is first so the {@code @Audited} aspect
     * picks the EE as the audit target rather than the preboarding.</p>
     *
     * <p>Throws {@link PreboardingAlreadyPromotedException} if the preboarding's
     * workflow state is already {@code Loaded} (or beyond).</p>
     *
     * @return the promoted-from / promoted-to pair + counts the REST layer
     *         needs for the 200 response.
     */
    PromotionResult promote( ExpressionExperiment ee, PreboardingExperiment preboarding )
            throws PreboardingAlreadyPromotedException;

    /**
     * Return value of {@link #promote(PreboardingExperiment, ExpressionExperiment)}.
     */
    class PromotionResult {
        private final Long preboardingId;
        private final Long eeId;
        private final int proposalsRebound;

        public PromotionResult( Long preboardingId, Long eeId, int proposalsRebound ) {
            this.preboardingId = preboardingId;
            this.eeId = eeId;
            this.proposalsRebound = proposalsRebound;
        }

        public Long getPreboardingId() {
            return preboardingId;
        }

        public Long getEeId() {
            return eeId;
        }

        public int getProposalsRebound() {
            return proposalsRebound;
        }
    }

    /**
     * Thrown by {@link #createPreboarding(String, String, String)} when an
     * existing entity carries the same accession.
     */
    class AccessionAlreadyExistsException extends Exception {
        private final Long existingId;
        private final String existingType;

        public AccessionAlreadyExistsException( String accession, Long existingId, String existingType ) {
            super( "An entity with accession " + accession + " already exists "
                    + "(type=" + existingType + ", id=" + existingId + ")." );
            this.existingId = existingId;
            this.existingType = existingType;
        }

        public Long getExistingId() {
            return existingId;
        }

        /** Either {@code "preboarding"} or {@code "expression_experiment"}. */
        public String getExistingType() {
            return existingType;
        }
    }

    /**
     * Thrown by {@link #promote(PreboardingExperiment, ExpressionExperiment)}
     * when the preboarding has already been promoted.
     */
    class PreboardingAlreadyPromotedException extends Exception {
        private final Long preboardingId;

        public PreboardingAlreadyPromotedException( Long preboardingId ) {
            super( "Preboarding " + preboardingId + " has already been promoted." );
            this.preboardingId = preboardingId;
        }

        public Long getPreboardingId() {
            return preboardingId;
        }
    }
}
