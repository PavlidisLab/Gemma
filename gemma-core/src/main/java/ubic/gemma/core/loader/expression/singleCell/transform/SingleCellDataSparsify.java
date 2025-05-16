package ubic.gemma.core.loader.expression.singleCell.transform;

public class SingleCellDataSparsify extends AbstractPythonScriptBasedAnnDataTransformation {

    public SingleCellDataSparsify() {
        super( "sparsify" );
    }

    @Override
    public String getDescription() {
        return "Convert an AnnData object to a sparse format using CSR.";
    }
}
