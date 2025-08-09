package ubic.gemma.core.analysis.singleCell.batcheffect;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import ubic.gemma.core.datastructure.matrix.SingleCellDesignMatrix;
import ubic.gemma.core.datastructure.matrix.SingleCellExpressionDataMatrix;
import ubic.gemma.core.datastructure.matrix.SingleCellExpressionDataMatrixUtils;
import ubic.gemma.core.util.r.REngineFactory;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssayData.CellLevelCharacteristics;
import ubic.gemma.model.expression.bioAssayData.SingleCellDimension;
import ubic.gemma.model.expression.bioAssayData.SingleCellExpressionDataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.experiment.SingleCellExpressionExperimentService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Service
public class SingleCellBatchCorrectionServiceImpl implements SingleCellBatchCorrectionService {

    @Autowired
    private SingleCellExpressionExperimentService singleCellExpressionExperimentService;

    @Autowired
    private REngineFactory rEngineFactory;

    @Override
    @Transactional
    public QuantitationType batchCorrect( ExpressionExperiment ee, QuantitationType qt, SingleCellBatchCorrectionMethod method ) {
        Assert.notNull( ee.getExperimentalDesign(), ee + " does not have experimental design. It is required to perform batch correction." );
        BatchCorrection m = createBatchCorrection( method );
        SingleCellDimension dimension = singleCellExpressionExperimentService.getSingleCellDimension( ee, qt );
        if ( dimension == null ) {
            throw new IllegalArgumentException( qt + " does not have single cell dimension." );
        }
        List<SingleCellExpressionDataVector> vectors = new ArrayList<>( singleCellExpressionExperimentService.getSingleCellDataVectors( ee, qt ) );
        SingleCellExpressionDataMatrix<?> dataMatrix = SingleCellExpressionDataMatrix.getMatrix( vectors );
        Collection<CellLevelCharacteristics> clcs = new ArrayList<>();
        // TODO: select relevant CTAs and CLCs
        clcs.addAll( dimension.getCellTypeAssignments() );
        clcs.addAll( dimension.getCellLevelCharacteristics() );
        SingleCellDesignMatrix designMatrix = SingleCellDesignMatrix.from( dimension, ee.getExperimentalDesign(), clcs );
        SingleCellExpressionDataMatrix<?> correctedMatrix = m.perform( dataMatrix, designMatrix );
        QuantitationType correctedQt = correctedMatrix.getQuantitationType();
        List<SingleCellExpressionDataVector> correctedVectors = SingleCellExpressionDataMatrixUtils.toVectors( correctedMatrix );
        String details = "Batch correction using " + method + " for " + ee.getShortName() + " on quantitation type " + qt.getName();
        singleCellExpressionExperimentService.addSingleCellDataVectors( ee, correctedQt, correctedVectors, details );
        return correctedQt;
    }

    private BatchCorrection createBatchCorrection( SingleCellBatchCorrectionMethod method ) {
        switch ( method ) {
            case HARMONY:
                return new Harmony( rEngineFactory );
            case COMBAT:
                return new ComBat();
            default:
                throw new IllegalArgumentException( "Unknown batch correction method: " + method );
        }
    }
}
