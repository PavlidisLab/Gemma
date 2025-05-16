package ubic.gemma.model.expression.bioAssayData;

import ubic.gemma.model.expression.bioAssay.BioAssay;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SingleCellDimensionUtils {

    /**
     * Create an index of bioassays to their cell IDs and their position in the bioassays.
     * <p>
     * This is useful for quickly finding the position of a cell ID in a corresponding {@link SingleCellExpressionDataVector}.
     */
    public static Map<BioAssay, Map<String, Integer>> createIndex( SingleCellDimension singleCellDimension ) {
        Map<BioAssay, Map<String, Integer>> index = new HashMap<>();
        List<BioAssay> bioAssays = singleCellDimension.getBioAssays();
        for ( int i = 0; i < bioAssays.size(); i++ ) {
            BioAssay ba = bioAssays.get( i );
            int bioAssayOffset = singleCellDimension.getBioAssaysOffset()[i];
            List<String> cellIdsBySample = singleCellDimension.getCellIdsBySample( i );
            for ( int j = 0; j < cellIdsBySample.size(); j++ ) {
                String cellId = cellIdsBySample.get( j );
                index.computeIfAbsent( ba, ignored -> new HashMap<>() )
                        .put( cellId, bioAssayOffset + j );
            }
        }
        return index;
    }

    /**
     * Create a reverse index of cell IDs to their corresponding position in bioassays.
     * <p>
     * This is useful to find the occurrences of a given barcode in a {@link SingleCellExpressionDataVector}.
     */
    public static Map<String, Map<BioAssay, Integer>> createReverseIndex( SingleCellDimension singleCellDimension ) {
        Map<String, Map<BioAssay, Integer>> reverseIndex = new HashMap<>();
        List<BioAssay> bioAssays = singleCellDimension.getBioAssays();
        for ( int i = 0; i < bioAssays.size(); i++ ) {
            BioAssay ba = bioAssays.get( i );
            int bioAssayOffset = singleCellDimension.getBioAssaysOffset()[i];
            List<String> cellIdsBySample = singleCellDimension.getCellIdsBySample( i );
            for ( int j = 0; j < cellIdsBySample.size(); j++ ) {
                String cellId = cellIdsBySample.get( j );
                reverseIndex.computeIfAbsent( cellId, ignored -> new HashMap<>() )
                        .put( ba, bioAssayOffset + j );
            }
        }
        return reverseIndex;
    }
}
