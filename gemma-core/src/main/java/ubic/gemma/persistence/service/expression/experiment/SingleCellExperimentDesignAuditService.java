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

import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * Co-bean carrying the {@code ExperimentalDesignUpdatedEvent} emissions for the
 * cell-type-factor create / remove flows in
 * {@link SingleCellExpressionExperimentServiceImpl}.
 * <p>
 * Hoisted out of {@code SingleCellExpressionExperimentServiceImpl} so the
 * emission methods are invoked through a Spring proxy and the {@code @Audited}
 * aspect can intercept them — the previous {@code private createCellTypeFactor
 * / removeCellTypeFactor} helpers were self-invoked via {@code this.} from
 * multiple call sites (5+ for create, 2 for remove) and were therefore invisible
 * to AOP, blocking declarative migration.
 *
 * <p>Each method here is a thin pass-through: the caller still does all the
 * domain work (constructing the factor, calling
 * {@code experimentalFactorService.create / remove}, updating the experimental
 * design) and then asks this co-bean to write the audit row. The audit emission
 * is decoupled from the domain side-effects so the proxy boundary only covers
 * the audit write.
 *
 * @see ubic.gemma.core.security.audit.Audited
 */
public interface SingleCellExperimentDesignAuditService {

    /**
     * Record an {@code ExperimentalDesignUpdatedEvent} against the EE noting
     * the creation of a cell-type factor from a preferred cell-type assignment.
     *
     * @param ee              experiment to audit
     * @param cellTypeFactor  the freshly created cell-type factor
     * @param ctlDescription  the preferred CTA's {@code toString} (passed by caller so the
     *                        message matches the historical imperative form verbatim)
     */
    void recordCellTypeFactorCreated( ExpressionExperiment ee, ExperimentalFactor cellTypeFactor, String ctlDescription );

    /**
     * Record an {@code ExperimentalDesignUpdatedEvent} against the EE noting
     * the removal of an existing cell-type factor.
     *
     * @param ee                      experiment to audit
     * @param removedCellTypeFactor   the cell-type factor that was removed
     */
    void recordCellTypeFactorRemoved( ExpressionExperiment ee, ExperimentalFactor removedCellTypeFactor );
}
