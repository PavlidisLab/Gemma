package ubic.gemma.persistence.service.expression.experiment;

import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssayData.SingleCellExpressionDataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import java.util.Collection;
import java.util.HashSet;

/**
 * Tests covering integration of single-cell.
 */
public class SingleCellExpressionExperimentServiceTest {

    private ExpressionExperimentService expressionExperimentService;

    public void testAddPreferredVectors() {
        ExpressionExperiment ee = new ExpressionExperiment();
        QuantitationType existingQt = new QuantitationType();
        existingQt.setIsPreferred( true );
        ee.getQuantitationTypes().add( existingQt );

        QuantitationType qt = new QuantitationType();
        qt.setIsPreferred( true );
        Collection<SingleCellExpressionDataVector> vectors = new HashSet<>();
        SingleCellExpressionDataVector v = new SingleCellExpressionDataVector();
        v.setExpressionExperiment( ee );
        v.setQuantitationType( qt );
        vectors.add( v );

        expressionExperimentService.addSingleCellDataVectors( ee, qt, vectors );
    }
}
