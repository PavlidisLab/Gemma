package ubic.gemma.core.loader.expression.singleCell;

import lombok.Setter;
import org.apache.commons.lang3.ArrayUtils;

@Setter
public class SingleCellDataSortBySample extends AbstractPythonScriptBasedAnnDataTransformation {

    private String sampleColumnName;

    public SingleCellDataSortBySample() {
        super( "sort-by-sample" );
    }

    @Override
    protected String[] createScriptArgs() {
        return ArrayUtils.add( super.createScriptArgs(), sampleColumnName );
    }
}
