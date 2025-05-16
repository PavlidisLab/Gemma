package ubic.gemma.core.loader.expression.singleCell.transform;

public class SingleCellDataRewrite extends AbstractPythonScriptBasedAnnDataTransformation {

    public SingleCellDataRewrite() {
        super( "rewrite" );
    }

    @Override
    public String getDescription() {
        return "Rewrite an AnnData object such that it is conformant to the latest version of the specification";
    }
}
