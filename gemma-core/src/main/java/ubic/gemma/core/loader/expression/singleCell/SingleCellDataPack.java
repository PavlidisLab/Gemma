package ubic.gemma.core.loader.expression.singleCell;

/**
 * Pack an AnnData object by removing unnecessary zeroes.
 * @author poirigui
 */
public class SingleCellDataPack extends AbstractPythonScriptBasedAnnDataTransformation {
    public SingleCellDataPack() {
        super( "pack" );
    }
}
