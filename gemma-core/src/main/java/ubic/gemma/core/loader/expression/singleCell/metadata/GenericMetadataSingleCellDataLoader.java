package ubic.gemma.core.loader.expression.singleCell.metadata;

import lombok.Setter;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;
import ubic.gemma.core.loader.expression.singleCell.AbstractDelegatingSingleCellDataLoader;
import ubic.gemma.core.loader.expression.singleCell.SingleCellDataLoader;
import ubic.gemma.core.loader.util.mapper.BioAssayMapper;
import ubic.gemma.model.common.protocol.Protocol;
import ubic.gemma.model.expression.bioAssayData.CellLevelCharacteristics;
import ubic.gemma.model.expression.bioAssayData.CellTypeAssignment;
import ubic.gemma.model.expression.bioAssayData.SingleCellDimension;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
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
    private String cellTypeAssignmentDescription;
    @Nullable
    private Protocol cellTypeAssignmentProtocol;

    @Nullable
    private final List<String> otherCellLevelCharacteristicsNames;
    @Nullable
    private final Path otherCellCharacteristicsMetadataFile;

    private BioAssayMapper bioAssayToSampleNameMapper;

    private boolean inferSamplesFromCellIdsOverlap;

    private boolean useCellIdsIfSampleNameIsMissing;

    // needs to be consistent with the default for loaders in general
    private boolean ignoreUnmatchedSamples = true;
    private boolean ignoreUnmatchedCellIds;

    public GenericMetadataSingleCellDataLoader( SingleCellDataLoader delegate, @Nullable Path cellTypeMetadataFile, @Nullable List<String> otherCellLevelCharacteristicsNames, @Nullable Path otherCellCharacteristicsMetadataFile ) {
        super( delegate );
        this.cellTypeMetadataFile = cellTypeMetadataFile;
        this.otherCellLevelCharacteristicsNames = otherCellLevelCharacteristicsNames;
        this.otherCellCharacteristicsMetadataFile = otherCellCharacteristicsMetadataFile;
    }

    /**
     * {@inheritDoc}
     * <p>
     * In addition, unmatched samples when parsing cell type assignment and other cell-level characteristics files will
     * be ignored.
     */
    public void setIgnoreUnmatchedSamples( boolean ignoreUnmatchedSamples ) {
        this.ignoreUnmatchedSamples = ignoreUnmatchedSamples;
        super.setIgnoreUnmatchedSamples( ignoreUnmatchedSamples );
    }

    public void setCellTypeAssignmentName( String cellTypeAssignmentName ) {
        Assert.notNull( cellTypeMetadataFile, "A cell type metadata file must be set to configure a name." );
        Assert.isTrue( StringUtils.isNotBlank( cellTypeAssignmentName ) );
        this.cellTypeAssignmentName = cellTypeAssignmentName;
    }

    public void setCellTypeAssignmentDescription( @Nullable String cellTypeAssignmentDescription ) {
        Assert.notNull( cellTypeMetadataFile, "A cell type metadata file must be set to configure a description." );
        this.cellTypeAssignmentDescription = cellTypeAssignmentDescription;
    }

    public void setCellTypeAssignmentProtocol( @Nullable Protocol cellTypeAssignmentProtocol ) {
        Assert.notNull( cellTypeMetadataFile, "A cell type metadata file must be set to configure a protocol." );
        Assert.isTrue( cellTypeAssignmentProtocol == null || cellTypeAssignmentProtocol.getId() != null,
                "The protocol must be either null or persistent." );
        this.cellTypeAssignmentProtocol = cellTypeAssignmentProtocol;
    }

    @Override
    public void setBioAssayToSampleNameMapper( BioAssayMapper bioAssayToSampleNameMapper ) {
        this.bioAssayToSampleNameMapper = bioAssayToSampleNameMapper;
        super.setBioAssayToSampleNameMapper( bioAssayToSampleNameMapper );
    }

    @Override
    public Set<CellTypeAssignment> getCellTypeAssignments( SingleCellDimension singleCellDimension ) throws IOException {
        if ( cellTypeMetadataFile == null ) {
            return super.getCellTypeAssignments( singleCellDimension );
        }
        Assert.notNull( bioAssayToSampleNameMapper, "A bioAssayToSampleNameMatcher must be set" );
        Assert.notNull( cellTypeAssignmentName, "A cell type assignment name must be set" );
        CellTypeAssignmentMetadataParser parser = new CellTypeAssignmentMetadataParser( singleCellDimension, bioAssayToSampleNameMapper, cellTypeAssignmentName, cellTypeAssignmentDescription, cellTypeAssignmentProtocol );
        configureParser( parser );
        return parser.parse( cellTypeMetadataFile );
    }

    @Override
    public Set<CellLevelCharacteristics> getOtherCellLevelCharacteristics( SingleCellDimension dimension ) throws IOException {
        if ( otherCellCharacteristicsMetadataFile == null ) {
            return super.getOtherCellLevelCharacteristics( dimension );
        }
        Assert.notNull( bioAssayToSampleNameMapper, "A bioAssayToSampleNameMatcher must be set" );
        GenericCellLevelCharacteristicsMetadataParser parser = new GenericCellLevelCharacteristicsMetadataParser( dimension, bioAssayToSampleNameMapper, otherCellLevelCharacteristicsNames );
        configureParser( parser );
        return parser.parse( otherCellCharacteristicsMetadataFile );
    }

    private void configureParser( AbstractCellLevelCharacteristicsMetadataParser<?> parser ) {
        parser.setUseCellIdsIfSampleNameIsMissing( useCellIdsIfSampleNameIsMissing );
        parser.setInferSamplesFromCellIdsOverlap( inferSamplesFromCellIdsOverlap );
        parser.setIgnoreUnmatchedSamples( ignoreUnmatchedSamples );
        parser.setIgnoreUnmatchedCellIds( ignoreUnmatchedCellIds );
    }
}
