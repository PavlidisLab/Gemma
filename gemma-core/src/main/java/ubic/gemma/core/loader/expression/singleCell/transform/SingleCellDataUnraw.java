package ubic.gemma.core.loader.expression.singleCell.transform;

public class SingleCellDataUnraw extends AbstractPythonScriptBasedAnnDataTransformation {

    public SingleCellDataUnraw() {
        super( "unraw" );
    }

    @Override
    public String getDescription() {
        return "Make the raw data in AnnData the main X layer";
    }
}
