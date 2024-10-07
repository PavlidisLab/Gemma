package ubic.gemma.core.loader.expression.singleCell;

import org.apache.commons.csv.CSVRecord;
import ubic.gemma.model.common.description.Categories;
import ubic.gemma.model.common.description.Category;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.bioAssayData.CellTypeAssignment;
import ubic.gemma.model.expression.bioAssayData.SingleCellDimension;

import java.util.List;

/**
 * Parser for cell type assignment.
 * <p>
 * The basic structure of the file is a TSV with the following columns:
 * <ul>
 *     <li>sample_id</li>
 *     <li>cell_id</li>
 *     <li>cell_type</li>
 *     <li>cell_type_Uri</li>
 * </ul>
 * Only one assignment is held in a file.
 * @author poirigui
 */
public class CellTypeAssignmentMetadataParser extends AbstractCellLevelCharacteristicsMetadataParser<CellTypeAssignment> {

    public CellTypeAssignmentMetadataParser( SingleCellDimension singleCellDimension, BioAssayToSampleNameMatcher bioAssayToSampleNameMatcher ) {
        super( singleCellDimension, bioAssayToSampleNameMatcher );
    }

    @Override
    protected Category getCategory( CSVRecord record ) {
        return Categories.CELL_TYPE;
    }

    @Override
    protected String getCategoryId( CSVRecord record ) {
        return "cell type";
    }

    @Override
    protected String getValue( CSVRecord record ) {
        return record.get( "cell_type" );
    }

    @Override
    protected String getValueUri( CSVRecord record ) {
        return record.get( "cell_type_uri" );
    }

    @Override
    protected CellTypeAssignment createCellLevelCharacteristics( List<Characteristic> characteristics, int[] indices ) {
        return CellTypeAssignment.Factory.newInstance( characteristics, indices );
    }
}
