package ubic.gemma.core.loader.expression.singleCell;

import java.nio.file.Path;

/**
 * A generic loader that can be used to load single cell with a tabular metadata file.
 * @author poirigui
 */
public class GenericMetadataSingleCellDataLoader extends AbstractDelegatingSingleCellDataLoader implements SingleCellDataLoader {

    public static final String DEFAULT_SAMPLE_FACTOR_NAME = "sample_id";
    public static final String DEFAULT_CELL_ID_FACTOR_NAME = "cell_id";

    private final Path metadataFile;

    private String sampleFactorName = DEFAULT_SAMPLE_FACTOR_NAME;
    private String cellIdFactorName = DEFAULT_CELL_ID_FACTOR_NAME;

    protected GenericMetadataSingleCellDataLoader( SingleCellDataLoader delegate, Path metadataFile ) {
        super( delegate );
        this.metadataFile = metadataFile;
    }

    public void setSampleFactorName( String sampleFactorName ) {
        this.sampleFactorName = sampleFactorName;
    }

    public void setCellIdFactorName( String cellIdFactorName ) {
        this.cellIdFactorName = cellIdFactorName;
    }
}
