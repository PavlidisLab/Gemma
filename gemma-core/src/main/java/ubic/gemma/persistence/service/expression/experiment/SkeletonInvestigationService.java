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
import ubic.gemma.model.expression.experiment.SkeletonInvestigation;

import java.util.List;

/**
 * Service surface for {@link SkeletonInvestigation} CRUD + accession resolution
 * + promotion. See {@code HANDOFF_PROPOSED_EXPERIMENT_WORKFLOW.md} §"Required
 * endpoints".
 */
public interface SkeletonInvestigationService {

    /**
     * Create a new skeleton for the given accession.
     *
     * <p>Throws {@link AccessionAlreadyExistsException} if an existing
     * {@link SkeletonInvestigation} OR {@link ExpressionExperiment} already
     * carries the same accession. The exception carries the existing entity's
     * id and type so the REST layer can mint a 409 response per spec.</p>
     *
     * <p>Emits a {@code SkeletonCreatedEvent} audit row against the new
     * skeleton's own audit trail.</p>
     */
    SkeletonInvestigation createSkeleton( String accession, @Nullable String source,
            @Nullable String identifyingMetadata )
            throws AccessionAlreadyExistsException;

    /**
     * @return the skeleton with the given id, or {@code null}.
     */
    @Nullable
    SkeletonInvestigation load( Long id );

    /**
     * @return the skeleton with the given accession, or {@code null}.
     */
    @Nullable
    SkeletonInvestigation findByAccession( String accession );

    /**
     * @return list of skeletons with the given accession (defensive — only
     *         one is expected; the create path enforces uniqueness).
     */
    List<SkeletonInvestigation> findAllByAccession( String accession );

    /**
     * Look up an existing {@link ExpressionExperiment} carrying the given
     * accession. Used by the {@code POST /skeletons} 409 path: if the data
     * is already loaded as an EE, the caller should write against the EE
     * directly rather than create a skeleton.
     *
     * @return the existing EE with this accession, or {@code null}.
     */
    @Nullable
    ExpressionExperiment findExpressionExperimentByAccession( String accession );

    /**
     * Promote the skeleton to a loaded {@link ExpressionExperiment}.
     *
     * <p>The implementation rebinds every {@code AgentProposal} attached to
     * the skeleton so it points at the EE row instead (new-row + FK rebind
     * approach; see {@code STATUS_PROPOSED_EXPERIMENT_WORKFLOW.md} for the
     * trade-off discussion). The skeleton's workflow state is advanced to
     * {@code Loaded} (terminal marker — the row is retained for history;
     * it carries no curatable artifacts) and the EE's workflow state is
     * advanced to {@code Loaded} too.</p>
     *
     * <p>Emits a {@code SkeletonPromotedEvent} on the EE's audit trail
     * (post-promotion the EE is the authoritative parent of the trail).
     * The {@code ee} argument is first so the {@code @Audited} aspect
     * picks the EE as the audit target rather than the skeleton.</p>
     *
     * <p>Throws {@link SkeletonAlreadyPromotedException} if the skeleton's
     * workflow state is already {@code Loaded} (or beyond).</p>
     *
     * @return the promoted-from / promoted-to pair + counts the REST layer
     *         needs for the 200 response.
     */
    PromotionResult promote( ExpressionExperiment ee, SkeletonInvestigation skeleton )
            throws SkeletonAlreadyPromotedException;

    /**
     * Return value of {@link #promote(SkeletonInvestigation, ExpressionExperiment)}.
     */
    class PromotionResult {
        private final Long skeletonId;
        private final Long eeId;
        private final int proposalsRebound;

        public PromotionResult( Long skeletonId, Long eeId, int proposalsRebound ) {
            this.skeletonId = skeletonId;
            this.eeId = eeId;
            this.proposalsRebound = proposalsRebound;
        }

        public Long getSkeletonId() {
            return skeletonId;
        }

        public Long getEeId() {
            return eeId;
        }

        public int getProposalsRebound() {
            return proposalsRebound;
        }
    }

    /**
     * Thrown by {@link #createSkeleton(String, String, String)} when an
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

        /** Either {@code "skeleton"} or {@code "expression_experiment"}. */
        public String getExistingType() {
            return existingType;
        }
    }

    /**
     * Thrown by {@link #promote(SkeletonInvestigation, ExpressionExperiment)}
     * when the skeleton has already been promoted.
     */
    class SkeletonAlreadyPromotedException extends Exception {
        private final Long skeletonId;

        public SkeletonAlreadyPromotedException( Long skeletonId ) {
            super( "Skeleton " + skeletonId + " has already been promoted." );
            this.skeletonId = skeletonId;
        }

        public Long getSkeletonId() {
            return skeletonId;
        }
    }
}
