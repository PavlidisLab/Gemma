package ubic.gemma.core.loader.expression.singleCell.metadata;

import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import ubic.gemma.core.loader.util.mapper.BioAssayMapper;
import ubic.gemma.model.common.description.Category;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.bioAssayData.CellLevelCharacteristics;
import ubic.gemma.model.expression.bioAssayData.SingleCellDimension;

import javax.annotation.Nullable;
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

    public GenericCellLevelCharacteristicsMetadataParser( SingleCellDimension singleCellDimension, BioAssayMapper bioAssayMapper, @Nullable List<String> names, @Nullable List<String> defaultValues, @Nullable List<String> defaultValueUris ) {
        super( singleCellDimension, bioAssayMapper, names, defaultValues, defaultValueUris );
    }

    protected Category getCategory( CSVRecord record ) {
        String category = StringUtils.stripToNull( record.get( "category" ) );
        String categoryUri = record.isMapped( "category_uri" ) ? StringUtils.stripToNull( record.get( "category_uri" ) ) : null;
        return new Category( category, categoryUri );
    }

    protected String getCategoryId( CSVRecord record ) {
        String category = StringUtils.stripToNull( record.get( "category" ) );
        String categoryUri = record.isMapped( "category_uri" ) ? StringUtils.stripToNull( record.get( "category_uri" ) ) : null;
        String categoryId;
        if ( record.isMapped( "category_id" ) ) {
            categoryId = StringUtils.stripToNull( record.get( "category_id" ) );
        } else if ( categoryUri != null ) {
            categoryId = categoryUri;
        } else {
            categoryId = category;
        }
        return categoryId;
    }

    @Override
    protected String getValue( CSVRecord record ) {
        return StringUtils.stripToNull( record.get( "value" ) );
    }

    @Override
    protected String getValueUri( CSVRecord record ) {
        if ( record.isMapped( "value_uri" ) ) {
            return StringUtils.stripToNull( record.get( "value_uri" ) );
        } else {
            return null;
        }
    }

    @Override
    protected CellLevelCharacteristics createCellLevelCharacteristics( @Nullable String name, String descriptionToAppend, List<Characteristic> characteristics, int[] indices ) {
        return CellLevelCharacteristics.Factory.newInstance( name, descriptionToAppend, characteristics, indices );
    }
}
