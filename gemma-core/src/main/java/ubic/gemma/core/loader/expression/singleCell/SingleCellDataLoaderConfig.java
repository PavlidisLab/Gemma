package ubic.gemma.core.loader.expression.singleCell;

import lombok.Getter;
import lombok.experimental.SuperBuilder;
import ubic.gemma.core.loader.expression.sequencing.SequencingDataLoaderConfig;
import ubic.gemma.core.loader.expression.singleCell.metadata.GenericMetadataSingleCellDataLoader;
import ubic.gemma.model.common.protocol.Protocol;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.List;

/**
 * Basic configuration for loading single-cell data.
 * @author poirigui
 * @see SingleCellDataLoaderService
 */
@Getter
@SuperBuilder
public class SingleCellDataLoaderConfig extends SequencingDataLoaderConfig {

    /**
     * Ignore individual samples that lack or have incomplete data.
     * <p>
     * This is only implemented for MEX.
     */
    private boolean ignoreSamplesLackingData;

    /**
     * A location where cell type can be found in a format covered by {@link GenericMetadataSingleCellDataLoader}.
     * <p>
     * If null, no cell type assignment will be imported. However, the loader might have other provisions for loading
     * cell types. For example, {@link AnnDataSingleCellDataLoader} can import cell types using a factor name.
     */
    @Nullable
    private Path cellTypeAssignmentFile;

    /**
     * A name to use for the cell type assignment.
     * <p>
     * If null, it is left to the specific loader to decide the name.
     */
    @Nullable
    private String cellTypeAssignmentName;

    /**
     * A protocol to use for the cell type assignment.
     * <p>
     * If non-null, this must be persistent.
     */
    @Nullable
    private Protocol cellTypeAssignmentProtocol;

    /**
     * Name to use for the cell-level characteristics.
     * <p>
     * Must match the number and order of CLCs in {@link #otherCellLevelCharacteristicsFile}.
     */
    @Nullable
    private List<String> otherCellLevelCharacteristicsNames;

    /**
     * A location where additional cell-level characteristics can be loaded.
     */
    @Nullable
    private Path otherCellLevelCharacteristicsFile;

    /**
     * When parsing {@link #cellTypeAssignmentFile} and {@link #otherCellLevelCharacteristicsFile}, use the overlap
     * between the cell IDs from the file and those from the {@link ubic.gemma.model.expression.bioAssayData.SingleCellDimension}
     * to infer sample associations.
     * <p>
     * When this option is set, the sample ID column must be supplied for this strategy to
     * be applied.
     */
    private boolean inferSamplesFromCellIdsOverlap;

    /**
     * When parsing {@link #cellTypeAssignmentFile} and {@link #otherCellLevelCharacteristicsFile}, allow for a missing
     * {@code sample_id} column, in which case barcodes are used to infer the sample a cell belongs to. This strategy
     * will not work in the case of a barcode collision.
     */
    private boolean useCellIdsIfSampleNameIsMissing;

    /**
     * When parsing {@link #cellTypeAssignmentFile} and {@link #otherCellLevelCharacteristicsFile}, ignore cell IDs that
     * cannot be matched to a sample.
     */
    private boolean ignoreUnmatchedCellIds;

    /**
     * If only one CTA is present, mark it as preferred.
     */
    private boolean markSingleCellTypeAssignmentAsPreferred;

    /**
     * Name of the CTA to mark as preferred, or null to ignore.
     * <p>
     * This setting overrides {@link #markSingleCellTypeAssignmentAsPreferred}.
     */
    @Nullable
    private String preferredCellTypeAssignmentName;

    /**
     * Prefer single-precision for storage, even if the data is available with double-precision.
     * <p>
     * This reduces the size of vectors and thus the storage requirement.
     */
    private boolean preferSinglePrecision;
}
