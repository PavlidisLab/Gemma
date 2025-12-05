package ubic.gemma.core.loader.expression.singleCell;

import lombok.Builder;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import ubic.gemma.core.loader.expression.sequencing.SequencingDataLoaderConfig;
import ubic.gemma.core.loader.expression.singleCell.metadata.GenericMetadataSingleCellDataLoader;
import ubic.gemma.model.common.protocol.Protocol;
import ubic.gemma.model.expression.bioAssayData.SingleCellDimension;

import javax.annotation.Nullable;
import java.io.Console;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * Basic configuration for loading single-cell data.
 *
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
     * A description to use for the cell type assignment.
     */
    @Nullable
    private String cellTypeAssignmentDescription;

    /**
     * A protocol to use for the cell type assignment.
     * <p>
     * If non-null, this must be persistent.
     */
    @Nullable
    private Protocol cellTypeAssignmentProtocol;

    /**
     * If there is already a cell type assignment with the same name, replace it with the new one.
     * <p>
     * Note that CTAs with a {@code null} name cannot be replaced.
     */
    private boolean replaceExistingCellTypeAssignment;

    /**
     * If true, ignore existing cell type assignment with the same name.
     */
    private boolean ignoreExistingCellTypeAssignment;

    /**
     * A location where additional cell-level characteristics can be loaded.
     */
    @Nullable
    private Path otherCellLevelCharacteristicsFile;

    /**
     * Name to use for the cell-level characteristics.
     * <p>
     * Must match the number and order of CLCs in {@link #otherCellLevelCharacteristicsFile}.
     */
    @Nullable
    private List<String> otherCellLevelCharacteristicsNames;

    /**
     * If there are already other CLCs with the same names, replace them with the new ones.
     * <p>
     * Note that other CLCs with a {@code null} name cannot be replaced.
     */
    private boolean replaceExistingOtherCellLevelCharacteristics;

    /**
     * If true, ignore existing other CLCs with the same names.
     */
    private boolean ignoreExistingOtherCellLevelCharacteristics;

    /**
     * When parsing {@link #cellTypeAssignmentFile} and {@link #otherCellLevelCharacteristicsFile}, use the overlap
     * between the cell IDs from the file and those from the {@link SingleCellDimension}
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
     * Re-create the cell type factor if necessary.
     */
    @Builder.Default
    private boolean recreateCellTypeFactorIfNecessary = true;

    /**
     * When re-creating, ignore a compatible cell type factor that may already exist.
     * <p>
     * Requires {@link #recreateCellTypeFactorIfNecessary} to be set.
     */
    private boolean ignoreCompatibleCellTypeFactor;

    /**
     * Prefer single-precision for storage, even if the data is available with double-precision.
     * <p>
     * This reduces the size of vectors and thus the storage requirement.
     */
    private boolean preferSinglePrecision;

    /**
     * Skip single-cell data transformations, unless they are absolutely necessary.
     */
    private boolean skipTransformations;

    /**
     * Executor service to use to transform single-cell data.
     */
    @Nullable
    public ExecutorService transformExecutor;

    /**
     * Console to use for reporting progress.
     */
    @Nullable
    private Console console;
}
