package ubic.gemma.core.loader.expression.singleCell;

import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import ubic.gemma.model.common.description.Category;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.bioAssayData.CellLevelCharacteristics;
import ubic.gemma.model.expression.bioAssayData.SingleCellDimension;

import java.util.List;

/**
 * Parser for generic cell-level characteristics.
 * <p>
 * The basic structure of the file is a TSV with the following columns:
 * <ul>
 *     <li>sample_id</li>
 *     <li>cell_id</li>
 *     <li>category</li>
 *     <li>category_uri</li>
 *     <li>category_id</li>
 *     <li>value</li>
 *     <li>value_uri</li>
 * </ul>
 * Characteristics are grouped by category ID which falls back to category URI or category if missing.
 * @author poirigui
 */
class GenericCellLevelCharacteristicsMetadataParser extends AbstractCellLevelCharacteristicsMetadataParser<CellLevelCharacteristics> {

    public GenericCellLevelCharacteristicsMetadataParser( SingleCellDimension singleCellDimension, BioAssayToSampleNameMatcher bioAssayToSampleNameMatcher, boolean useCellIdsIfSampleNameIsMissing ) {
        super( singleCellDimension, bioAssayToSampleNameMatcher, useCellIdsIfSampleNameIsMissing );
    }

    protected Category getCategory( CSVRecord record ) {
        String category = StringUtils.stripToNull( record.get( "category" ) );
        String categoryUri = StringUtils.stripToNull( record.get( "category_uri" ) );
        return new Category( category, categoryUri );
    }

    protected String getCategoryId( CSVRecord record ) {
        String category = StringUtils.stripToNull( record.get( "category" ) );
        String categoryUri = StringUtils.stripToNull( record.get( "category_uri" ) );
        String categoryId;
        try {
            categoryId = StringUtils.stripToNull( record.get( "category_id" ) );
        } catch ( IllegalArgumentException e ) {
            categoryId = null;
        }
        if ( categoryId == null ) {
            categoryId = categoryUri != null ? categoryUri : category;
        }
        return categoryId;
    }

    @Override
    protected String getValue( CSVRecord record ) {
        return StringUtils.stripToNull( record.get( "value" ) );
    }

    @Override
    protected String getValueUri( CSVRecord record ) {
        return StringUtils.stripToNull( record.get( "value_uri" ) );
    }

    @Override
    protected CellLevelCharacteristics createCellLevelCharacteristics( List<Characteristic> characteristics, int[] indices ) {
        return CellLevelCharacteristics.Factory.newInstance( characteristics, indices );
    }
}
