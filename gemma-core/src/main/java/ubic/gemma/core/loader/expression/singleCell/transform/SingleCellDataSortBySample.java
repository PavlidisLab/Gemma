package ubic.gemma.core.loader.expression.singleCell.transform;

import lombok.Setter;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.util.Assert;

@Setter
public class SingleCellDataSortBySample extends AbstractPythonScriptBasedAnnDataTransformation {

    private String sampleColumnName;

    public SingleCellDataSortBySample() {
        super( "sort-by-sample" );
    }

    @Override
    protected String[] createPythonScriptArgs() {
        Assert.notNull( sampleColumnName, "A sample column name must be provided for sorting." );
        return ArrayUtils.add( super.createPythonScriptArgs(), sampleColumnName );
    }

    @Override
    public String getDescription() {
        return "Sort an AnnData object by sample";
    }
}
