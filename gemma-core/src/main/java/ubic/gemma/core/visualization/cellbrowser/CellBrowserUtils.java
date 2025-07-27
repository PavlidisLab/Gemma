package ubic.gemma.core.visualization.cellbrowser;

import ubic.basecode.util.StringUtil;
import ubic.gemma.model.expression.bioAssay.BioAssay;

public class CellBrowserUtils {

    /**
     * Construct a cell ID for the Cell Browser.
     * @param useBioAssayIds    use the BioAssay ID as the sample ID instead of the short name (or name)
     * @param useRawColumnNames if true, the sample ID and cell ID will be concatenanted as-is, otherwise
     * {@link StringUtil#makeNames(String)} will be used it make it R-friendly
     */
    public static String constructCellId( BioAssay bioAssay, String cellId, boolean useBioAssayIds, boolean useRawColumnNames ) {
        String sampleId;
        if ( useBioAssayIds ) {
            sampleId = String.valueOf( bioAssay.getId() );
        } else {
            sampleId = bioAssay.getShortName() != null ? bioAssay.getShortName() : bioAssay.getName();
        }
        if ( useRawColumnNames ) {
            return sampleId + "_" + cellId;
        } else {
            return StringUtil.makeNames( sampleId + "_" + cellId );
        }
    }
}
