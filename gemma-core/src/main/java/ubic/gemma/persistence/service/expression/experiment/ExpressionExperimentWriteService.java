/*
 * The Gemma project.
 *
 * Copyright (c) 2006 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 */
package ubic.gemma.persistence.service.expression.experiment;

import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssayData.MeanVarianceRelation;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValue;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;

/**
 * Thin write/mutation service for {@link ExpressionExperiment}.
 * <p>
 * Phase 2 of the {@link ExpressionExperimentService} decomposition (strangler fig). This service
 * houses the design-mutation (bucket E) and lifecycle (bucket F) methods that were previously on
 * {@link ExpressionExperimentServiceImpl}: factor / factor-value / characteristic add/remove,
 * quantitation-type + mean-variance updates, and the heavy {@code remove} that cascades through
 * subsets, DEA, sample-coex, PCA, and EE sets.
 * <p>
 * Callers should generally keep using {@link ExpressionExperimentService} as the facade -- the
 * facade delegates to this service. Direct injection is appropriate where a class would otherwise
 * create a Spring dependency cycle through the heavier facade.
 * <p>
 * ACL / {@code @Secured} annotations live on {@link ExpressionExperimentService} (the
 * caller-facing facade interface); enforcement happens at the facade proxy boundary.
 *
 * @see ExpressionExperimentService
 * @see ExpressionExperimentReadService
 */
public interface ExpressionExperimentWriteService {

    // ---------------------------------------------------------------------
    // Bucket E -- design mutation (factors / factor-values / characteristics
    // / quantitation type updates / mean-variance updates)
    // ---------------------------------------------------------------------

    ExperimentalFactor addFactor( ExpressionExperiment ee, ExperimentalFactor factor );

    FactorValue addFactorValue( ExpressionExperiment ee, FactorValue fv );

    void addFactorValues( ExpressionExperiment ee, Map<BioMaterial, FactorValue> fvs );

    void addCharacteristic( ExpressionExperiment ee, Characteristic vc );

    void removeCharacteristics( ExpressionExperiment ee, Collection<Characteristic> characteristicsToRemove );

    void updateQuantitationType( ExpressionExperiment ee, QuantitationType qt, @Nullable QuantitationType previousPreferredQt );

    MeanVarianceRelation updateMeanVarianceRelation( ExpressionExperiment ee, MeanVarianceRelation mvr );

    // ---------------------------------------------------------------------
    // Bucket F -- lifecycle (remove)
    // ---------------------------------------------------------------------

    /**
     * Deletes an experiment and all of its associated objects, including coexpression links. Some
     * types of associated objects may need to be deleted before this can be run (example: analyses
     * involving multiple experiments; these will not be deleted automatically).
     */
    void remove( ExpressionExperiment ee );

    void remove( Collection<ExpressionExperiment> entities );
}
