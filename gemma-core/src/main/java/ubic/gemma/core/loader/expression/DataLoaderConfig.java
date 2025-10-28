package ubic.gemma.core.loader.expression;

import lombok.Getter;
import lombok.experimental.SuperBuilder;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;

import javax.annotation.Nullable;
import java.nio.file.Path;

/**
 * @author poirigui
 */
@Getter
@SuperBuilder
public class DataLoaderConfig {

    /**
     * Load data from that specific path instead of looking up standard locations.
     */
    @Nullable
    private Path dataPath;

    /**
     * Name of the QT to use if more than one is present in the data as per {@link DataLoader#getQuantitationTypes()}.
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
     * Mark the quantitation type as recomputed from raw data.
     * @see ubic.gemma.model.common.quantitationtype.QuantitationType#setIsRecomputedFromRawData(boolean)
     */
    private boolean markQuantitationTypeAsRecomputedFromRawData;

    /**
     * Mark the QT as preferred.
     * <p>
     * All other QTs of the same kind will be set to non-preferred as a result.
     * @see ubic.gemma.model.common.quantitationtype.QuantitationType#setIsPreferred(boolean)
     * @see ubic.gemma.model.common.quantitationtype.QuantitationType#setIsSingleCellPreferred(boolean)
     * @see ubic.gemma.model.common.quantitationtype.QuantitationType#setIsMaskedPreferred(boolean)
     */
    private boolean markQuantitationTypeAsPreferred;

    /**
     * A file containing a mapping of sample names to bioassay names.
     * @see ubic.gemma.core.loader.util.mapper.RenamingBioAssayMapper
     */
    @Nullable
    private Path renamingFile;
}
