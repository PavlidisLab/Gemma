package ubic.gemma.core.loader.expression.singleCell;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.util.Assert;
import ubic.gemma.core.loader.expression.singleCell.metadata.GenericMetadataSingleCellDataLoader;
import ubic.gemma.core.loader.util.mapper.BioAssayMapper;

@CommonsLog
public class GenericMetadataSingleCellDataLoaderConfigurer implements SingleCellDataLoaderConfigurer<GenericMetadataSingleCellDataLoader> {

    private final SingleCellDataLoaderConfigurer<?> delegate;
    private final BioAssayMapper bioAssayMapper;

    public GenericMetadataSingleCellDataLoaderConfigurer( SingleCellDataLoaderConfigurer<?> delegate, BioAssayMapper bioAssayMapper ) {
        this.delegate = delegate;
        this.bioAssayMapper = bioAssayMapper;
    }

    @Override
    public GenericMetadataSingleCellDataLoader configureLoader( SingleCellDataLoaderConfig config ) {
        // wrap with a generic loader to load additional metadata
        Assert.isTrue( config.getCellTypeAssignmentFile() != null || config.getOtherCellLevelCharacteristicsFile() != null,
                "At least one of cellTypeAssignmentPath or otherCellCharacteristicsPath must be set" );
        if ( config.getCellTypeAssignmentFile() != null ) {
            log.info( "Loading cell type assignments from " + config.getCellTypeAssignmentFile() );
        }
        if ( config.getOtherCellLevelCharacteristicsFile() != null ) {
            log.info( "Loading additional cell-level characteristics from " + config.getOtherCellLevelCharacteristicsFile() );
        }
        GenericMetadataSingleCellDataLoader loader = new GenericMetadataSingleCellDataLoader( delegate.configureLoader( config ), config.getCellTypeAssignmentFile(), config.getOtherCellLevelCharacteristicsFile() );
        // this needs to be set so that the delegate can use it
        loader.setBioAssayToSampleNameMapper( bioAssayMapper );
        if ( config.getCellTypeAssignmentName() != null ) {
            loader.setCellTypeAssignmentName( config.getCellTypeAssignmentName() );
        }
        if ( config.getCellTypeAssignmentProtocol() != null ) {
            loader.setCellTypeAssignmentProtocol( config.getCellTypeAssignmentProtocol() );
        }
        loader.setInferSamplesFromCellIdsOverlap( config.isInferSamplesFromCellIdsOverlap() );
        loader.setUseCellIdsIfSampleNameIsMissing( config.isUseCellIdsIfSampleNameIsMissing() );
        loader.setIgnoreUnmatchedCellIds( config.isIgnoreUnmatchedCellIds() );
        return loader;
    }

}
