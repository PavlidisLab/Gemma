package ubic.gemma.core.loader.expression.geo.singleCell;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.file.PathUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.util.Assert;
import ubic.gemma.core.loader.expression.geo.model.GeoSample;
import ubic.gemma.core.loader.expression.geo.model.GeoSeries;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.apache.commons.io.FileUtils.byteCountToDisplaySize;

/**
 * Handle detection and download of single-cell data from a single file in the supplementary materials of a GEO series.
 * @author poirigui
 */
@CommonsLog
public abstract class AbstractSingleFileInSeriesSingleCellDetector extends AbstractSingleCellDetector {

    private final String name;
    private final String extension;

    protected AbstractSingleFileInSeriesSingleCellDetector( String name, String extension ) {
        Assert.isTrue( extension.startsWith( "." ), "Extension must start with a dot." );
        this.name = name;
        this.extension = extension;
    }

    /**
     * Indicate if the given supplementary file is accepted.
     * <p>
     * The default implementation checks if the file ends with the extension (or the extension + {@code .gz}).
     */
    protected boolean accepts( String supplementaryFile ) {
        return supplementaryFile.endsWith( extension ) || supplementaryFile.endsWith( extension + ".gz" );
    }

    /**
     * Obtain the download destination for the single-cell data file.
     */
    protected Path getDest( GeoSeries series ) {
        Assert.notNull( getDownloadDirectory(), "A download directory must be set." );
        return getDownloadDirectory().resolve( series + extension );
    }

    @Override
    public boolean hasSingleCellData( GeoSeries series ) {
        boolean found = false;
        for ( String file : series.getSupplementaryFiles() ) {
            if ( accepts( file ) ) {
                log.info( String.format( "%s: Found %s in supplementary materials:\n\t%s", series.getGeoAccession(), name, file ) );
                found = true;
            }
        }
        return found;
    }

    /**
     * Detection is not supported at the sample level.
     */
    @Override
    public boolean hasSingleCellData( GeoSample sample ) {
        return false;
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException if more than one matching file is present in the supplementary files of the
     *                                  series, use {@link #downloadSingleCellData(GeoSeries, String)} as a workaround.
     */
    @Override
    public void downloadSingleCellData( GeoSeries series ) throws NoSingleCellDataFoundException, IOException {
        if ( series.getSupplementaryFiles().isEmpty() ) {
            throw new NoSingleCellDataFoundException( series.getGeoAccession() + " does not have any supplementary files." );
        }
        Set<String> matchedSupplementaryFiles = series.getSupplementaryFiles().stream()
                .filter( this::accepts )
                .collect( Collectors.toSet() );
        String file;
        if ( matchedSupplementaryFiles.size() == 1 ) {
            file = matchedSupplementaryFiles.iterator().next();
        } else if ( matchedSupplementaryFiles.size() > 1 ) {
            throw new IllegalArgumentException( String.format( "More than one %s file is present in the supplementary files of %s: %s",
                    name, series.getGeoAccession(), matchedSupplementaryFiles.stream().map( FilenameUtils::getName ).collect( Collectors.toSet() ) ) );
        } else {
            throw new NoSingleCellDataFoundException( "No " + name + " data could be found in " + series.getGeoAccession() + " supplementary files." );
        }
        downloadSingleCellData( series, file );
    }

    /**
     * Download a specific supplementary file at the series-level.
     * @throws IllegalArgumentException if the supplementary file is not present in the series
     */
    public void downloadSingleCellData( GeoSeries series, String file ) throws IOException {
        Assert.isTrue( series.getSupplementaryFiles().contains( file ), series.getGeoAccession() + " does not have a supplementary file named " + file );

        Path dest = getDest( series );
        if ( existsAndHasExpectedSize( dest, file ) ) {
            log.info( String.format( "%s: Skipping download of %s to %s because it already exists and has expected size.",
                    series.getGeoAccession(), file, dest ) );
            return;
        }

        log.info( series.getGeoAccession() + ": Retrieving " + name + " file " + file + " to " + dest + "..." );
        retry( ( attempt, lastAttempt ) -> {
            PathUtils.createParentDirectories( dest );
            StopWatch timer = StopWatch.createStarted();
            try ( InputStream is = openSupplementaryFileAsStream( file, attempt, true ) ) {
                OutputStream os = Files.newOutputStream( dest );
                long downloadedBytes = IOUtils.copyLarge( is, os );
                log.info( String.format( "%s: Retrieved " + name + " file (%s in %s @ %s/s).", series.getGeoAccession(),
                        byteCountToDisplaySize( downloadedBytes ), timer,
                        byteCountToDisplaySize( 1000.0 * downloadedBytes / timer.getTime() ) ) );
            } catch ( Exception e ) {
                log.warn( String.format( "%s: %s file could not be downloaded successfully, removing %s...",
                        series.getGeoAccession(), name, dest ), e );
                PathUtils.deleteFile( dest );
                throw e;
            }
            return null;
        }, "downloading " + file + " to " + dest + " for " + name );
    }

    @Override
    public void downloadSingleCellData( GeoSample sample ) throws NoSingleCellDataFoundException {
        throw new NoSingleCellDataFoundException( name + " does not support single-cell data at the sample-level." );
    }

    @Override
    public List<String> getAdditionalSupplementaryFiles( GeoSeries sample ) {
        return sample.getSupplementaryFiles().stream()
                .filter( f -> !this.accepts( f ) && !f.endsWith( "_RAW.tar" ) )
                .collect( Collectors.toList() );
    }

    @Override
    public List<String> getAdditionalSupplementaryFiles( GeoSample sample ) {
        // since this is for a single file in the series, all sample attachments are considered additional
        return new ArrayList<>( sample.getSupplementaryFiles() );
    }
}