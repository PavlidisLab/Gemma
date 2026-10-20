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
package ubic.gemma.persistence.service.expression.experiment;

import ubic.gemma.model.expression.experiment.ExperimentalFactor;

/**
 * Read-only retrieval service for {@link ExperimentalFactor}.
 * <p>
 * Phase 3 of the {@link ExperimentalFactorService} decomposition (strangler
 * fig). This service houses the single custom read method previously
 * implemented directly on the {@code ExperimentalFactorServiceImpl} facade:
 * {@code thaw}, which delegates to {@link ExperimentalFactorDao#thaw(ExperimentalFactor)}.
 * <p>
 * Sibling read surfaces live in {@link ExperimentalDesignReadService}; the
 * read methods here operate on {@link ExperimentalFactor} directly and do not
 * overlap.
 * <p>
 * Write-side methods (the {@code remove} overrides plus the inherited
 * {@code BaseService} / {@code AbstractVoEnabledService} mutators) stay on the
 * {@link ExperimentalFactorService} facade.
 * <p>
 * Callers should generally keep using {@link ExperimentalFactorService} as the
 * facade -- the facade delegates to this service. Direct injection is
 * appropriate where a class is logically read-only.
 * <p>
 * ACL / {@code @Secured} annotations live on {@link ExperimentalFactorService}
 * (the caller-facing facade interface); enforcement happens at the facade
 * proxy boundary, not here. The new read impl is unsecured at the AOP boundary
 * on purpose -- intra-{@code gemma-core} callers that already hold an
 * authenticated session bypass duplicate ACL checks.
 *
 * @see ExperimentalFactorService
 */
public interface ExperimentalFactorReadService {

    ExperimentalFactor thaw( ExperimentalFactor ef );
}
