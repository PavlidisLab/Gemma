package ubic.gemma.core.loader.expression.geo.singleCell;

import lombok.extern.apachecommons.CommonsLog;
import ubic.gemma.core.loader.util.anndata.AnnData;
import ubic.gemma.core.loader.util.hdf5.TruncatedH5FileException;

import java.io.IOException;
import java.nio.file.Path;

/**
 * @author poirigui
 */
@CommonsLog
public abstract class AbstractSingleH5FileInSeriesSingleCellDetector extends AbstractSingleFileInSeriesSingleCellDetector {

    protected AbstractSingleH5FileInSeriesSingleCellDetector( String name, String extension ) {
        super( name, extension );
    }

    /**
     * {@inheritDoc}
     * <p>
     * In addition to size and existence, also check if the H5 is truncated.
     */
    @Override
    protected boolean existsAndHasExpectedSize( Path dest, String remoteFile, long expectedContentLength, boolean decompressIfNeeded, boolean storeCompressed ) throws IOException {
        return super.existsAndHasExpectedSize( dest, remoteFile, expectedContentLength, decompressIfNeeded, storeCompressed )
                && !isTruncated( dest );
    }

    private boolean isTruncated( Path dest ) throws IOException {
        try ( AnnData ignored = AnnData.open( dest ) ) {
            return false;
        } catch ( TruncatedH5FileException e ) {
            log.warn( dest + " appears to be a truncated H5 file, it will re-downloaded...", e );
            return true;
        } catch ( IllegalArgumentException e ) {
            log.warn( "AnnData file " + dest + " is likely invalid, however this method is only checking if the file is truncated.", e );
            return false;
        }
    }
}
