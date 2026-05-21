/*
 * The gemma project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 */
package ubic.gemma.persistence.service.expression.experiment;

import org.springframework.lang.Nullable;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssayData.SingleCellDimension;
import ubic.gemma.model.expression.bioAssayData.SingleCellDimensionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.BaseDao;

import java.util.List;

/**
 * DAO for the denormalized {@link SingleCellDimensionExperiment} link table. Backs the migration of
 * the 30+ "scan SCEDV, group by SingleCellDimension" HQLs in
 * {@link ExpressionExperimentDaoImpl} — see PERF_PROBE_REPORT_ROUND4 finding B1.
 *
 * @see SingleCellDimensionExperiment
 */
public interface SingleCellDimensionExperimentDao extends BaseDao<SingleCellDimensionExperiment> {

    /**
     * Look up the {@link SingleCellDimension} attached to the given {@code (ee, qt)} pair via the
     * link table.
     * <p>
     * Returns {@code null} if no link row exists. Replaces the legacy
     * {@code select scedv.singleCellDimension from SingleCellExpressionDataVector ... group by ...}
     * HQL with a single-row indexed lookup.
     */
    @Nullable
    SingleCellDimension findDimensionByEEAndQt( ExpressionExperiment ee, QuantitationType qt );

    /**
     * Look up the link row for {@code (ee, qt)}, or {@code null}.
     */
    @Nullable
    SingleCellDimensionExperiment findByEEAndQt( ExpressionExperiment ee, QuantitationType qt );

    /**
     * All link rows attached to the given EE.
     */
    List<SingleCellDimensionExperiment> findByEE( ExpressionExperiment ee );

    /**
     * Insert (or no-op if already present) a link row for {@code (ee, qt, scd)}. Idempotent under
     * the current unique constraint on {@code (EE_FK, QT_FK)} — re-recording the same triple is a
     * no-op; recording a different SCD for an existing {@code (ee, qt)} updates the row.
     */
    SingleCellDimensionExperiment record( ExpressionExperiment ee, QuantitationType qt, SingleCellDimension scd );

    /**
     * Remove the link row for {@code (ee, qt)}, if any. Returns the number of rows deleted (0 or 1).
     */
    int removeByEEAndQt( ExpressionExperiment ee, QuantitationType qt );

    /**
     * Remove every link row attached to the given EE. Returns the number of rows deleted.
     */
    int removeByEE( ExpressionExperiment ee );

    /**
     * Remove every link row that references the given dimension. Returns the number of rows deleted.
     */
    int removeBySingleCellDimension( SingleCellDimension scd );
}
