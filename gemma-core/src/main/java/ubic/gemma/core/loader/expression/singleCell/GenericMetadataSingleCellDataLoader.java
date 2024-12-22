package ubic.gemma.core.loader.expression.singleCell;

import lombok.Setter;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;
import ubic.gemma.model.common.protocol.Protocol;
import ubic.gemma.model.expression.bioAssayData.CellLevelCharacteristics;
import ubic.gemma.model.expression.bioAssayData.CellTypeAssignment;
import ubic.gemma.model.expression.bioAssayData.SingleCellDimension;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

/**
 * A generic loader that can be used to load single cell with a tabular metadata file.
 * <p>
 * This loader supports cell type assignments and generic cell-level characteristics and fallback to a delegate if the
 * corresponding path is unset.
 * @author poirigui
 * @see CellTypeAssignmentMetadataParser
 * @see GenericCellLevelCharacteristicsMetadataParser
 */
@Setter
@CommonsLog
public class GenericMetadataSingleCellDataLoader extends AbstractDelegatingSingleCellDataLoader implements SingleCellDataLoader {

    private static final String DEFAULT_CELL_TYPE_ASSIGNMENT_NAME = "cell type";

    @Nullable
    private final Path cellTypeMetadataFile;
    private String cellTypeAssignmentName = DEFAULT_CELL_TYPE_ASSIGNMENT_NAME;
    @Nullable
    private Protocol cellTypeAssignmentProtocol;

    @Nullable
    private final Path otherCellCharacteristicsMetadataFile;

    private BioAssayToSampleNameMatcher bioAssayToSampleNameMatcher;

    private boolean useCellIdsIfSampleNameIsMissing;

    protected GenericMetadataSingleCellDataLoader( SingleCellDataLoader delegate, @Nullable Path cellTypeMetadataFile, @Nullable Path otherCellCharacteristicsMetadataFile ) {
        super( delegate );
        this.cellTypeMetadataFile = cellTypeMetadataFile;
        this.otherCellCharacteristicsMetadataFile = otherCellCharacteristicsMetadataFile;
    }

    public void setCellTypeAssignmentName( String cellTypeAssignmentName ) {
        Assert.notNull( cellTypeMetadataFile, "A cell type metadata file must be set to configure a name." );
        Assert.isTrue( StringUtils.isNotBlank( cellTypeAssignmentName ) );
        this.cellTypeAssignmentName = cellTypeAssignmentName;
    }

    public void setCellTypeAssignmentProtocol( @Nullable Protocol cellTypeAssignmentProtocol ) {
        Assert.notNull( cellTypeMetadataFile, "A cell type metadata file must be set to configure a protocol." );
        Assert.isTrue( cellTypeAssignmentProtocol == null || cellTypeAssignmentProtocol.getId() != null,
                "The protocol must be either null or persistent." );
        this.cellTypeAssignmentProtocol = cellTypeAssignmentProtocol;
    }

    @Override
    public void setBioAssayToSampleNameMatcher( BioAssayToSampleNameMatcher sampleNameComparator ) {
        this.bioAssayToSampleNameMatcher = sampleNameComparator;
        super.setBioAssayToSampleNameMatcher( sampleNameComparator );
    }

    @Override
    public Set<CellTypeAssignment> getCellTypeAssignments( SingleCellDimension singleCellDimension ) throws IOException {
        if ( cellTypeMetadataFile == null ) {
            return super.getCellTypeAssignments( singleCellDimension );
        }
        Assert.notNull( bioAssayToSampleNameMatcher, "A bioAssayToSampleNameMatcher must be set" );
        return new CellTypeAssignmentMetadataParser( singleCellDimension, bioAssayToSampleNameMatcher, cellTypeAssignmentName, cellTypeAssignmentProtocol, useCellIdsIfSampleNameIsMissing )
                .parse( cellTypeMetadataFile );
    }

    @Override
    public Set<CellLevelCharacteristics> getOtherCellLevelCharacteristics( SingleCellDimension dimension ) throws IOException {
        if ( otherCellCharacteristicsMetadataFile == null ) {
            return super.getOtherCellLevelCharacteristics( dimension );
        }
        Assert.notNull( bioAssayToSampleNameMatcher, "A bioAssayToSampleNameMatcher must be set" );
        return new GenericCellLevelCharacteristicsMetadataParser( dimension, bioAssayToSampleNameMatcher, useCellIdsIfSampleNameIsMissing )
                .parse( otherCellCharacteristicsMetadataFile );
    }
}
