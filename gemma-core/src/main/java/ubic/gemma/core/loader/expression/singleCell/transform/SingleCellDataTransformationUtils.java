package ubic.gemma.core.loader.expression.singleCell.transform;

import lombok.extern.apachecommons.CommonsLog;
import ubic.gemma.core.loader.expression.singleCell.SingleCellDataType;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;

/**
 * @author poirigui
 */
@CommonsLog
public class SingleCellDataTransformationUtils {

    private static final Map<SingleCellDataType, String> FILE_EXTENSION_MAP;

    static {
        FILE_EXTENSION_MAP = new EnumMap<>(SingleCellDataType.class);
        FILE_EXTENSION_MAP.put(SingleCellDataType.ANNDATA, ".h5ad");
        FILE_EXTENSION_MAP.put(SingleCellDataType.SEURAT_DISK, ".h5Seurat");
        FILE_EXTENSION_MAP.put(SingleCellDataType.LOOM, ".loom");
        FILE_EXTENSION_MAP.put(SingleCellDataType.MEX, ".mex");
    }

    /**
     * Obtain a temporary file for the output of a transformation.
     * <p>
     * The file will be created in the scratch directory if it is configured.
     * <p>
     * In the case of {@link SingleCellDataType#MEX}, a temporary directory is created instead.
     */
    public static Path createTemporaryFile( @Nullable Path scratchDir, SingleCellDataType dataType ) throws IOException {
        boolean isDir;
        String fileExt = FILE_EXTENSION_MAP.get(dataType);
        isDir = dataType == SingleCellDataType.MEX;

        if (fileExt == null) {
            throw new IllegalArgumentException("Unknown single-cell data type: " + dataType);
        }

        if ( scratchDir != null ) {
            if ( !Files.exists( scratchDir ) ) {
                log.info( "Scratch directory " + scratchDir + " does not exist, creating it." );
                Files.createDirectories( scratchDir );
            }
            return isDir ? Files.createTempDirectory( scratchDir, null ) :
                    Files.createTempFile( scratchDir, null, fileExt );
        } else {
            return isDir ? Files.createTempDirectory( "gemma-scratch-" ) :
                    Files.createTempFile( "gemma-scratch-", fileExt );
        }
    }
}
