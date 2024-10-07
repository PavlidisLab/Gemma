package ubic.gemma.core.loader.expression.singleCell;

import lombok.Setter;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.util.Assert;
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
 * @see CellLevelCharacteristicsMetadataParser
 */
@Setter
@CommonsLog
public class GenericMetadataSingleCellDataLoader extends AbstractDelegatingSingleCellDataLoader implements SingleCellDataLoader {

    @Nullable
    private final Path cellTypeMetadataFile;
    @Nullable
    private final Path otherCellCharacteristicsMetadataFile;

    private BioAssayToSampleNameMatcher bioAssayToSampleNameMatcher;

    protected GenericMetadataSingleCellDataLoader( SingleCellDataLoader delegate, @Nullable Path cellTypeMetadataFile, @Nullable Path otherCellCharacteristicsMetadataFile ) {
        super( delegate );
        this.cellTypeMetadataFile = cellTypeMetadataFile;
        this.otherCellCharacteristicsMetadataFile = otherCellCharacteristicsMetadataFile;
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
        return new CellTypeAssignmentMetadataParser( singleCellDimension, bioAssayToSampleNameMatcher )
                .parse( cellTypeMetadataFile );
    }

    @Override
    public Set<CellLevelCharacteristics> getOtherCellLevelCharacteristics( SingleCellDimension dimension ) throws IOException {
        if ( otherCellCharacteristicsMetadataFile == null ) {
            return super.getOtherCellLevelCharacteristics( dimension );
        }
        Assert.notNull( bioAssayToSampleNameMatcher, "A bioAssayToSampleNameMatcher must be set" );
        return new GenericCellLevelCharacteristicsMetadataParser( dimension, bioAssayToSampleNameMatcher )
                .parse( otherCellCharacteristicsMetadataFile );
    }
}
