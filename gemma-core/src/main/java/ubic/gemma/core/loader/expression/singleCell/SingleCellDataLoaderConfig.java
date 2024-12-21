package ubic.gemma.core.loader.expression.singleCell;

import lombok.Getter;
import lombok.experimental.SuperBuilder;
import ubic.gemma.model.common.protocol.Protocol;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;

import javax.annotation.Nullable;
import java.nio.file.Path;

/**
 * Basic configuration for loading single-cell data.
 * @author poirigui
 * @see SingleCellDataLoaderService
 */
@Getter
@SuperBuilder
public class SingleCellDataLoaderConfig {

    /**
     * Load single-cell data from that specific path instead of looking up standard locations.
     */
    @Nullable
    private Path dataPath;

    /**
     * Name of the QT to use if more than one is present in the data as per {@link SingleCellDataLoader#getQuantitationTypes()}.
     * <p>
     * This may be set to null, in which case the loader must only detect a single QT.
     */
    @Nullable
    private String quantitationTypeName;

    /**
     * Replace the vectors of an existing QT.
     * <p>
     * Requires {@link #quantitationTypeName} to be set.
     */
    private boolean replaceExistingQuantitationType;

    /**
     * Override the name of the resulting QT.
     */
    @Nullable
    private String quantitationTypeNewName;

    /**
     * Override the type of the resulting QT.
     */
    @Nullable
    private StandardQuantitationType quantitationTypeNewType;

    /**
     * Override the scale type of the resulting QT.
     */
    @Nullable
    private ScaleType quantitationTypeNewScaleType;

    /**
     * Mark the QT as preferred.
     * <p>
     * All other single-cell QT will be set to non-preferred as a result.
     * @see ubic.gemma.model.common.quantitationtype.QuantitationType#setIsSingleCellPreferred(boolean)
     */
    private boolean markQuantitationTypeAsPreferred;

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
     * A location where additional cell-level characteristics can be loaded.
     */
    @Nullable
    private Path otherCellLevelCharacteristicsFile;

    /**
     * When parsing {@link #cellTypeAssignmentFile} and {@link #otherCellLevelCharacteristicsFile}, allow for a missing
     * {@code sample_id} column, in which case barcodes are used to infer the sample a cell belongs to. This strategy
     * will not work in the case of a barcode collision.
     */
    private boolean useCellIdsIfSampleNameIsMissing;

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
}
