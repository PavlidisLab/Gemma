package ubic.gemma.web.controller.expression.experiment;

import lombok.Value;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssayData.CellLevelCharacteristics;
import ubic.gemma.model.expression.bioAssayData.CellTypeAssignment;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;

import javax.annotation.Nullable;
import java.util.Collection;

@Value
public class SingleCellExpressionDataModel {
    ExpressionExperiment expressionExperiment;
    Collection<CellTypeAssignment> cellTypeAssignments;
    Collection<CellLevelCharacteristics> cellLevelCharacteristics;
    QuantitationType quantitationType;
    CompositeSequence designElement;
    @Nullable
    Gene gene;
    @Nullable
    Long[] assayIds;
    @Nullable
    CellTypeAssignment cellTypeAssignment;
    @Nullable
    CellLevelCharacteristics cellLevelCharacteristics1;
    @Nullable
    Characteristic focusedCharacteristic;
    String keywords;
    @Nullable
    String font;
}
