package ubic.gemma.core.loader.expression.singleCell.transform;

import lombok.Setter;

/**
 * Transpose a single-cell dataset.
 * @author poirigui
 */
@Setter
public class SingleCellDataTranspose extends AbstractPythonScriptBasedAnnDataTransformation {

    public SingleCellDataTranspose() {
        super( "transpose" );
    }

    @Override
    public String getDescription() {
        return "Transpose an AnnData object and ensure it is stored in CSR format if sparse";
    }
}
