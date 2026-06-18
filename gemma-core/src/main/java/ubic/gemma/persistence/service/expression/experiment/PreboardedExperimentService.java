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
import ubic.gemma.model.expression.experiment.PreboardedExperiment;

import java.util.List;

/**
 * Service surface for {@link PreboardedExperiment} CRUD + accession resolution
 * + promotion. See {@code HANDOFF_PROPOSED_EXPERIMENT_WORKFLOW.md} §"Required
 * endpoints".
 */
public interface PreboardedExperimentService {

    /**
     * Create a new preboarded for the given accession.
     *
     * <p>Throws {@link AccessionAlreadyExistsException} if an existing
     * {@link PreboardedExperiment} OR {@link ExpressionExperiment} already
     * carries the same accession. The exception carries the existing entity's
     * id and type so the REST layer can mint a 409 response per spec.</p>
     *
     * <p>Emits a {@code PreboardedCreatedEvent} audit row against the new
     * preboarded's own audit trail.</p>
     */
    PreboardedExperiment createPreboarded( String accession, @Nullable String source,
            @Nullable String identifyingMetadata )
            throws AccessionAlreadyExistsException;

    /**
     * @return the preboarded with the given id, or {@code null}.
     */
    @Nullable
    PreboardedExperiment load( Long id );

    /**
     * @return the preboarded with the given accession, or {@code null}.
     */
    @Nullable
    PreboardedExperiment findByAccession( String accession );

    /**
     * @return list of preboarded with the given accession (defensive — only
     *         one is expected; the create path enforces uniqueness).
     */
    List<PreboardedExperiment> findAllByAccession( String accession );

    /**
     * Look up an existing {@link ExpressionExperiment} carrying the given
     * accession. Used by the {@code POST /preboarded} 409 path: if the data
     * is already loaded as an EE, the caller should write against the EE
     * directly rather than create a preboarded.
     *
     * @return the existing EE with this accession, or {@code null}.
     */
    @Nullable
    ExpressionExperiment findExpressionExperimentByAccession( String accession );

    /**
     * Promote the preboarded to a loaded {@link ExpressionExperiment}.
     *
     * <p>The implementation rebinds every {@code AnnotationSet} attached to
     * the preboarded so it points at the EE row instead (new-row + FK rebind
     * approach; see {@code STATUS_PROPOSED_EXPERIMENT_WORKFLOW.md} for the
     * trade-off discussion). The preboarded's workflow state is advanced to
     * {@code Loaded} (terminal marker — the row is retained for history;
     * it carries no curatable artifacts) and the EE's workflow state is
     * advanced to {@code Loaded} too.</p>
     *
     * <p>Emits a {@code PreboardedPromotedEvent} on the EE's audit trail
     * (post-promotion the EE is the authoritative parent of the trail).
     * The {@code ee} argument is first so the {@code @Audited} aspect
     * picks the EE as the audit target rather than the preboarded.</p>
     *
     * <p>Throws {@link PreboardedAlreadyPromotedException} if the preboarded's
     * workflow state is already {@code Loaded} (or beyond).</p>
     *
     * @return the promoted-from / promoted-to pair + counts the REST layer
     *         needs for the 200 response.
     */
    PromotionResult promote( ExpressionExperiment ee, PreboardedExperiment preboarded )
            throws PreboardedAlreadyPromotedException;

    /**
     * Return value of {@link #promote(PreboardedExperiment, ExpressionExperiment)}.
     */
    class PromotionResult {
        private final Long preboardedId;
        private final Long eeId;
        private final int annotationSetsRebound;

        public PromotionResult( Long preboardedId, Long eeId, int annotationSetsRebound ) {
            this.preboardedId = preboardedId;
            this.eeId = eeId;
            this.annotationSetsRebound = annotationSetsRebound;
        }

        public Long getPreboardedId() {
            return preboardedId;
        }

        public Long getEeId() {
            return eeId;
        }

        public int getAnnotationSetsRebound() {
            return annotationSetsRebound;
        }
    }

    /**
     * Thrown by {@link #createPreboarded(String, String, String)} when an
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

        /** Either {@code "preboarded"} or {@code "expression_experiment"}. */
        public String getExistingType() {
            return existingType;
        }
    }

    /**
     * Thrown by {@link #promote(PreboardedExperiment, ExpressionExperiment)}
     * when the preboarded has already been promoted.
     */
    class PreboardedAlreadyPromotedException extends Exception {
        private final Long preboardedId;

        public PreboardedAlreadyPromotedException( Long preboardedId ) {
            super( "Preboarded " + preboardedId + " has already been promoted." );
            this.preboardedId = preboardedId;
        }

        public Long getPreboardedId() {
            return preboardedId;
        }
    }
}
