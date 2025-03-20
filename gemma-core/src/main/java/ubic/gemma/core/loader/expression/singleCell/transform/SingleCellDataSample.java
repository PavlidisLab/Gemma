package ubic.gemma.core.loader.expression.singleCell.transform;

import lombok.Setter;
import org.apache.commons.lang3.ArrayUtils;

@Setter
public class SingleCellDataSample extends AbstractPythonScriptBasedAnnDataTransformation {

    private int numberOfCells;
    private int numberOfGenes;

    public SingleCellDataSample() {
        super( "sample" );
    }

    @Override
    protected String[] createScriptArgs() {
        return ArrayUtils.addAll( super.createScriptArgs(), String.valueOf( numberOfCells ), String.valueOf( numberOfGenes ) );
    }

    @Override
    public String getDescription() {
        return "Randomly sample cells and genes from an AnnData object";
    }
}
