package ubic.gemma.model.expression.bioAssay;

import ubic.gemma.model.common.description.Category;

import java.util.Collection;

/**
 * Categories for {@link BioAssay}s.
 * @author poirigui
 * @see BioAssayUtils#createCharacteristicMap(Collection)
 */
public class BioAssayCategories {

    // for all assays
    public static final Category PROCESSING_DATE = new Category( "processing date", null );
    public static final Category IS_OUTLIER = new Category( "is outlier", null );

    // for sequencing assays
    public static final Category SEQUENCE_READ_LENGTH = new Category( "sequence read length", null );
    public static final Category SEQUENCE_READ_COUNT = new Category( "sequence read count", null );
    public static final Category SEQUENCE_PAIRED_READS = new Category( "sequence paired reads", null );

    // for single-cell assays
    public static final Category NUMBER_OF_CELLS = new Category( "number of cells", null );
    public static final Category NUMBER_OF_DESIGN_ELEMENTS = new Category( "number of design elements", null );
    public static final Category NUMBER_OF_CELLS_BY_DESIGN_ELEMENTS = new Category( "number of cells by design elements", null );
}
