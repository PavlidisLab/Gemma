package ubic.gemma.core.loader.expression.singleCell.metadata;

import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import ubic.gemma.core.loader.util.mapper.BioAssayMapper;
import ubic.gemma.model.common.description.Categories;
import ubic.gemma.model.common.description.Category;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.protocol.Protocol;
import ubic.gemma.model.expression.bioAssayData.CellTypeAssignment;
import ubic.gemma.model.expression.bioAssayData.SingleCellDimension;

import javax.annotation.Nullable;
import java.util.Collections;
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
class CellTypeAssignmentMetadataParser extends AbstractCellLevelCharacteristicsMetadataParser<CellTypeAssignment> {

    @Nullable
    private final String cellTypeAssignmentDescription;
    @Nullable
    private final Protocol cellTypeAssignmentProtocol;

    public CellTypeAssignmentMetadataParser( SingleCellDimension singleCellDimension, BioAssayMapper bioAssayMapper, String cellTypeAssignmentName, @Nullable String cellTypeAssignmentDescription, @Nullable Protocol cellTypeAssignmentProtocol ) {
        super( singleCellDimension, bioAssayMapper, Collections.singletonList( cellTypeAssignmentName ), null, null );
        this.cellTypeAssignmentDescription = cellTypeAssignmentDescription;
        this.cellTypeAssignmentProtocol = cellTypeAssignmentProtocol;
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
        return StringUtils.stripToNull( record.get( "cell_type" ) );
    }

    @Override
    protected String getValueUri( CSVRecord record ) {
        if ( record.isMapped( "cell_type_uri" ) ) {
            return StringUtils.stripToNull( record.get( "cell_type_uri" ) );
        } else {
            return null;
        }
    }

    @Override
    protected CellTypeAssignment createCellLevelCharacteristics( @Nullable String name, @Nullable String descriptionToAppend, List<Characteristic> characteristics, int[] indices ) {
        CellTypeAssignment cta = CellTypeAssignment.Factory.newInstance( name, characteristics, indices );
        if ( cellTypeAssignmentDescription != null ) {
            descriptionToAppend = StringUtils.strip( cellTypeAssignmentDescription ) + "\n\n" + descriptionToAppend;
        }
        cta.setDescription( descriptionToAppend );
        cta.setProtocol( cellTypeAssignmentProtocol );
        return cta;
    }
}
