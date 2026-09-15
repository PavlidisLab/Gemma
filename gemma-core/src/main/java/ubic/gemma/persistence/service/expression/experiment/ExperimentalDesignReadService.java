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

import org.springframework.lang.Nullable;
import ubic.gemma.model.expression.experiment.ExperimentalDesign;

/**
 * Read-only retrieval service for {@link ExperimentalDesign}.
 * <p>
 * Phase 3 of the {@link ExperimentalDesignService} decomposition (strangler
 * fig). This service houses the two custom read methods previously implemented
 * directly on the {@code ExperimentalDesignServiceImpl} facade:
 * {@code loadWithExperimentalFactors} and
 * {@code getRandomExperimentalDesignThatNeedsAttention}. The first wraps
 * {@link ExperimentalDesignDao#load(Long)} with a Hibernate initialization of
 * the experimental-factors collection; the second delegates straight to
 * {@link ExperimentalDesignDao#getRandomExperimentalDesignThatNeedsAttention(ExperimentalDesign)}.
 * <p>
 * Write-side methods (the inherited {@code BaseService} mutators
 * {@code update}, {@code remove}, etc. coming from
 * {@code SecurableBaseService}) stay on the {@link ExperimentalDesignService}
 * facade.
 * <p>
 * Callers should generally keep using {@link ExperimentalDesignService} as the
 * facade -- the facade delegates to this service. Direct injection is
 * appropriate where a class is logically read-only.
 * <p>
 * ACL / {@code @Secured} annotations live on {@link ExperimentalDesignService}
 * (the caller-facing facade interface); enforcement happens at the facade
 * proxy boundary, not here. The read methods on the facade carry
 * {@code @Secured("IS_AUTHENTICATED_ANONYMOUSLY")} plus a
 * {@code @PostAuthorize hasPermission(...,'READ')} check for
 * {@code loadWithExperimentalFactors}, and {@code @Secured("GROUP_ADMIN")} for
 * {@code getRandomExperimentalDesignThatNeedsAttention}. The new read impl is
 * unsecured at the AOP boundary on purpose -- intra-{@code gemma-core} callers
 * that already hold an authenticated session bypass duplicate ACL checks.
 *
 * @see ExperimentalDesignService
 */
public interface ExperimentalDesignReadService {

    @Nullable
    ExperimentalDesign loadWithExperimentalFactors( Long id );

    @Nullable
    ExperimentalDesign getRandomExperimentalDesignThatNeedsAttention( ExperimentalDesign excludeDesign );
}
