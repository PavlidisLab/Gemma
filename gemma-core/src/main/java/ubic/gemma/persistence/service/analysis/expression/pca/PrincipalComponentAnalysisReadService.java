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
package ubic.gemma.persistence.service.analysis.expression.pca;

import org.springframework.lang.Nullable;
import ubic.gemma.model.analysis.expression.pca.PrincipalComponentAnalysis;
import ubic.gemma.model.analysis.expression.pca.ProbeLoading;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import java.util.List;

/**
 * Read-only retrieval service for {@link PrincipalComponentAnalysis}.
 * <p>
 * Phase 3 of the {@link PrincipalComponentAnalysisService} decomposition
 * (strangler fig). This service houses the three read methods previously
 * implemented directly on the {@code PrincipalComponentAnalysisServiceImpl}
 * facade: {@code getTopLoadedProbes}, {@code loadForExperiment}, and
 * {@code existsByExperiment}. Each delegates to the matching
 * {@link PrincipalComponentAnalysisDao} method (with
 * {@code loadForExperiment} additionally logging when more than one PCA is
 * found and returning an arbitrary one for backward compatibility).
 * <p>
 * Write-side methods ({@code create}, {@code removeForExperiment}, and the
 * inherited {@code BaseImmutableService} mutators) stay on the
 * {@link PrincipalComponentAnalysisService} facade.
 * <p>
 * Callers should generally keep using {@link PrincipalComponentAnalysisService}
 * as the facade -- the facade delegates to this service. Direct injection is
 * appropriate where a class is logically read-only.
 * <p>
 * ACL / {@code @Secured} annotations live on
 * {@link PrincipalComponentAnalysisService} (the caller-facing facade
 * interface); enforcement happens at the facade proxy boundary, not here. The
 * read methods on the facade carry
 * {@code @Secured({"IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ"})}.
 * The new read impl is unsecured at the AOP boundary on purpose --
 * intra-{@code gemma-core} callers that already hold an authenticated session
 * bypass duplicate ACL checks.
 *
 * @see PrincipalComponentAnalysisService
 */
public interface PrincipalComponentAnalysisReadService {

    List<ProbeLoading> getTopLoadedProbes( ExpressionExperiment ee, int component, int count );

    @Nullable
    PrincipalComponentAnalysis loadForExperiment( ExpressionExperiment ee );

    boolean existsByExperiment( ExpressionExperiment ee );
}
