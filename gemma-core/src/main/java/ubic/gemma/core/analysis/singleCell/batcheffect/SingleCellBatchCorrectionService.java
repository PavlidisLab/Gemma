package ubic.gemma.core.analysis.singleCell.batcheffect;

import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * @author poirigui
 */
public interface SingleCellBatchCorrectionService {

    /**
     * Perform batch correction and save the results as a new {@link QuantitationType QuantitationType} sharing the same
     * {@link ubic.gemma.model.expression.bioAssayData.SingleCellDimension}.
     * @return the batch-corrected {@link QuantitationType}
     */
    QuantitationType batchCorrect( ExpressionExperiment ee, QuantitationType qt, SingleCellBatchCorrectionMethod method );
}
