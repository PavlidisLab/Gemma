package ubic.gemma.core.loader.expression.singleCell;

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
}
