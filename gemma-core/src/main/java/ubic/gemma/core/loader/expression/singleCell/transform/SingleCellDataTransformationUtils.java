package ubic.gemma.core.loader.expression.singleCell.transform;

import lombok.extern.apachecommons.CommonsLog;
import ubic.gemma.core.loader.expression.singleCell.SingleCellDataType;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * @author poirigui
 */
@CommonsLog
public class SingleCellDataTransformationUtils {

    /**
     * Obtain a temporary file for the output of a transformation.
     * <p>
     * The file will be created in the scratch directory if it is configured.
     * <p>
     * In the case of {@link SingleCellDataType#MEX}, a temporary directory is created instead.
     */
    public static Path createTemporaryFile( @Nullable Path scratchDir, SingleCellDataType dataType ) throws IOException {
        boolean isDir;
        String fileExt;
        switch ( dataType ) {
            case ANNDATA:
                isDir = false;
                fileExt = ".h5ad";
                break;
            case SEURAT_DISK:
                isDir = false;
                fileExt = ".h5Seurat";
                break;
            case LOOM:
                isDir = false;
                fileExt = ".loom";
                break;
            case MEX:
                isDir = true;
                fileExt = ".mex";
                break;
            default:
                throw new IllegalArgumentException( "Unknown single-cell data type: " + dataType );
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
