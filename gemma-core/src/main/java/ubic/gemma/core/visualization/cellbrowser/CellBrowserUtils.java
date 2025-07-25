package ubic.gemma.core.visualization.cellbrowser;

import ubic.gemma.model.expression.bioAssay.BioAssay;

import static ubic.gemma.core.datastructure.matrix.io.ExpressionDataWriterUtils.constructAssayName;

public class CellBrowserUtils {

    public static String constructCellId( BioAssay bioAssay, String cellId, boolean useBioAssayIds ) {
        String sampleId;
        if ( useBioAssayIds ) {
            sampleId = String.valueOf( bioAssay.getId() );
        } else {
            sampleId = constructAssayName( bioAssay );
        }
        return sampleId + "_" + cellId;
    }
}
