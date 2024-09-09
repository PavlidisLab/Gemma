package ubic.gemma.core.loader.expression.singleCell;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

import javax.annotation.Nullable;
import java.nio.file.Path;

/**
 * Basic configuration for loading single-cell data.
 * @author poirigui
 */
@Getter
@SuperBuilder
public class SingleCellDataLoaderConfig {

    /**
     * Name of the QT to use if more than one is present in the data as per {@link SingleCellDataLoader#getQuantitationTypes()}.
     * <p>
     * This may be set to null, in which case the loader must only detect a single QT.
     */
    @Nullable
    private String quantitationTypeName;

    /**
     * A location where cell type can be found in a format covered by {@link GenericMetadataSingleCellDataLoader}.
     * <p>
     * If null, no cell type assignment will be imported. However, the loader might have other provisions for loading
     * cell types. For example, {@link AnnDataSingleCellDataLoader} can import cell types using a factor name.
     */
    @Nullable
    private Path cellTypeAssignmentPath;
}
