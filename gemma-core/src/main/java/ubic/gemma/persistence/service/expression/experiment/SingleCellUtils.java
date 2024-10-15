package ubic.gemma.persistence.service.expression.experiment;

import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.bioAssayData.CellTypeAssignment;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.expression.experiment.StatementUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

class SingleCellUtils {

    /**
     * Map the cell types from a cell type assignment to factor values in a cell type factor.
     * @throws IllegalStateException if there is not a 1-1 mapping between the two
     */
    public static Map<Characteristic, FactorValue> mapCellTypeAssignmentToCellTypeFactor( CellTypeAssignment cta, ExperimentalFactor cellTypeFactor ) {
        Map<Characteristic, FactorValue> mappedCellTypeFactors = new HashMap<>();
        for ( Characteristic cellType : cta.getCellTypes() ) {
            Set<FactorValue> matchedFvs = cellTypeFactor.getFactorValues().stream()
                    .filter( fv -> fv.getCharacteristics().stream().anyMatch( s -> StatementUtils.hasSubject( s, cellType ) ) )
                    .collect( Collectors.toSet() );
            if ( matchedFvs.isEmpty() ) {
                throw new IllegalStateException( cellType + "matches no factor values in " + cellTypeFactor );
            } else if ( matchedFvs.size() > 1 ) {
                throw new IllegalStateException( cellType + "matches more than one factor values in " + cellTypeFactor );
            }
            mappedCellTypeFactors.put( cellType, matchedFvs.iterator().next() );
        }
        return mappedCellTypeFactors;
    }
}
