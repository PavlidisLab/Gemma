package ubic.gemma.core.loader.expression.geo.singleCell;

import lombok.Setter;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.file.PathUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.util.Assert;
import ubic.gemma.core.loader.expression.geo.model.GeoSample;
import ubic.gemma.core.loader.expression.geo.model.GeoSeries;
import ubic.gemma.core.loader.expression.singleCell.MexSingleCellDataLoader;
import ubic.gemma.core.loader.expression.singleCell.SingleCellDataLoader;
import ubic.gemma.core.util.ProgressInputStream;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;

import static org.apache.commons.io.FileUtils.byteCountToDisplaySize;

/**
 * Detects 10X MEX data from GEO series and samples.
 * <p>
 * Older MEX datasets use the {@code genes.tsv.gz} instead of {@code features.tsv.gz}. Those are copied using the new
 * naming scheme into the download directory.
 * <p>
 * MEX data is only supported at the sample-level. However, we do support detecting its presence at the series-level,
 * but not downloading.
 * @author poirigui
 */
@Setter
public class MexDetector extends AbstractSingleCellDetector implements SingleCellDetector {

    public static final String
            DEFAULT_BARCODES_FILE_SUFFIX = "barcodes.tsv",
            DEFAULT_BARCODE_METADATA_FILE_SUFFIX = "barcode_metadata.tsv",
            DEFAULT_FEATURES_FILE_SUFFIX = "features.tsv",
            DEFAULT_GENES_FILE_SUFFIX = "genes.tsv",
            DEFAULT_MATRIX_FILE_SUFFIX = "matrix.mtx";

    private String barcodesFileSuffix = DEFAULT_BARCODES_FILE_SUFFIX;
    @Nullable
    private String barcodeMetadataFileSuffix = DEFAULT_BARCODE_METADATA_FILE_SUFFIX;
    private String featuresFileSuffix = DEFAULT_FEATURES_FILE_SUFFIX;
    @Nullable
    private String genesFileSuffix = DEFAULT_GENES_FILE_SUFFIX;
    private String matrixFileSuffix = DEFAULT_MATRIX_FILE_SUFFIX;

    /**
     * Set the maximum size of an archive entry to skip the supplementary file altogether.
     * <p>
     * Note that if a MEX file was previously found in the archive, it will not be skipped.
     */
    private long maxEntrySizeInArchiveToSkip = 25_000_000L;

    /**
     * {@inheritDoc}
     * <p>
     * MEX data detection is not supported at the series level, so while this method can return true if barcodes/genes/matrices
     * are present in the series supplementary files, {@link #downloadSingleCellData(GeoSeries)} will subsequently fail.
     */
    @Override
    public boolean hasSingleCellData( GeoSeries series ) {
        Assert.notNull( series.getGeoAccession() );
        // don't bother looking up MEX files in archives at the series-level, it's just wasteful since we cannot
        // download them
        return hasSingleCellData( series.getGeoAccession(), series.getSupplementaryFiles(), false );
    }

    /**
     * Check if a sample contains single-cell data in the context of its series.
     */
    public boolean hasSingleCellData( GeoSeries series, GeoSample sample ) {
        Assert.notNull( sample.getGeoAccession() );
        return hasSingleCellData( sample.getGeoAccession(), mergeSupplementaryFiles( series, sample ), true );
    }

    @Override
    public boolean hasSingleCellData( GeoSample sample ) {
        return hasSingleCellData( sample, true );
    }

    /**
     * Check if a GEO sample contains single-cell data.
     * @param allowArchiveLookup allow looking into archives for MEX files
     */
    public boolean hasSingleCellData( GeoSample sample, boolean allowArchiveLookup ) {
        Assert.notNull( sample.getGeoAccession() );
        return hasSingleCellData( sample.getGeoAccession(), sample.getSupplementaryFiles(), allowArchiveLookup );
    }

    /**
     * Check if a sample or series has single-cell data.
     * @param geoAccession       GEO accession of the sample or series
     * @param supplementaryFiles list of supplementary file
     * @param allowArchiveLookup allow looking up archives for MEX data, use this with parsimony because it requires
     *                           partially downloading the archive
     */
    private boolean hasSingleCellData( String geoAccession, Collection<String> supplementaryFiles, boolean allowArchiveLookup ) {
        // detect MEX (3 files per GEO sample)
        String barcodes = null, features = null, matrix = null;
        for ( String file : supplementaryFiles ) {
            if ( isMexFile( file, MexFileType.BARCODES ) ) {
                barcodes = file;
            } else if ( isMexFile( file, MexFileType.FEATURES ) ) {
                if ( genesFileSuffix != null && ( endsWith( file, genesFileSuffix ) ) ) {
                    log.info( geoAccession + ": Found an old-style MEX file " + file + ", treating it as features.tsv." );
                }
                features = file;
            } else if ( isMexFile( file, MexFileType.MATRIX ) ) {
                matrix = file;
            }
        }

        if ( barcodes != null && features != null && matrix != null ) {
            log.info( String.format( "%s: Found MEX files in supplementary materials:\n\t%s\n\t%s\n\t%s",
                    geoAccession, barcodes, features, matrix ) );
            return true;
        } else if ( barcodes != null || features != null || matrix != null ) {
            log.warn( String.format( "%s: Found incomplete MEX files in supplementary materials:\n\t%s",
                    geoAccession,
                    Stream.of( barcodes, features, matrix ).filter( Objects::nonNull ).collect( Collectors.joining( "\n\t" ) ) ) );
        }

        // detect MEX (1 archive per GEO sample)
        for ( String file : supplementaryFiles ) {
            if ( allowArchiveLookup && ( file.endsWith( ".tar" ) || file.endsWith( ".tar.gz" ) || file.endsWith( ".zip" ) ) ) {
                log.info( "Looking up the content of " + file + " for MEX data..." );
                try {
                    Boolean done = retry( ( attempt, lastAttempt ) -> {
                        String barcodesT = null;
                        String featuresT = null;
                        String matrixT = null;
                        // we just have to read the header of the archive and not its content
                        try ( ArchiveInputStream<?> tis = openSupplementaryFileAsArchiveStream( file, attempt ) ) {
                            ArchiveEntry te;
                            while ( ( te = tis.getNextEntry() ) != null ) {
                                if ( te.isDirectory() ) {
                                    continue;
                                }
                                if ( isMexFile( te.getName(), MexFileType.BARCODES ) ) {
                                    barcodesT = te.getName();
                                    if ( featuresT != null && matrixT != null ) {
                                        // Archive entries are generally sorted, so features.tsv and barcodes.tsv appear before
                                        // matrix.mtx, if we were not to skip at this point, the whole matrix would have to be
                                        // read, which would be inefficient. The downside is that we cannot detect cases
                                        // where an archive contains two matrices
                                        break;
                                    }
                                } else if ( isMexFile( te.getName(), MexFileType.FEATURES ) ) {
                                    if ( genesFileSuffix != null && ( endsWith( te.getName(), genesFileSuffix ) ) ) {
                                        log.info( geoAccession + ": Found an old-style MEX file " + te.getName() + " in an archive, treating it as features.tsv." );
                                    }
                                    featuresT = te.getName();
                                    if ( barcodesT != null && matrixT != null ) {
                                        // Archive entries are generally sorted, so features.tsv and barcodes.tsv appear before
                                        // matrix.mtx, if we were not to skip at this point, the whole matrix would have to be
                                        // read, which would be inefficient. The downside is that we cannot detect cases
                                        // where an archive contains two matrices
                                        break;
                                    }
                                } else if ( isMexFile( te.getName(), MexFileType.MATRIX ) ) {
                                    matrixT = te.getName();
                                    if ( featuresT != null && barcodesT != null ) {
                                        // Archive entries are generally sorted, so features.tsv and barcodes.tsv appear before
                                        // matrix.mtx, if we were not to skip at this point, the whole matrix would have to be
                                        // read, which would be inefficient. The downside is that we cannot detect cases
                                        // where an archive contains two matrices
                                        break;
                                    }
                                } else if ( skipForLargeArchiveEntry( geoAccession, file, te, barcodesT, featuresT, matrixT ) ) {
                                    break;
                                }
                            }
                            if ( barcodesT != null && featuresT != null && matrixT != null ) {
                                log.info( String.format( "%s: Found MEX files bundled in an archive %s:\n\t\t%s\n\t\t%s\n\t\t%s",
                                        geoAccession, file, barcodesT, featuresT, matrixT ) );
                                return true;
                            } else if ( barcodesT != null || featuresT != null || matrixT != null ) {
                                log.warn( String.format( "%s: Found incomplete MEX files bundled in an archive %s:\n\t%s",
                                        geoAccession, file,
                                        Stream.of( barcodesT, featuresT, matrixT ).filter( Objects::nonNull ).collect( Collectors.joining( "\n\t" ) ) ) );
                            }
                            return false;
                        }
                    }, "checking if " + file + " contains MEX data" );
                    if ( done ) {
                        return true;
                    }
                } catch ( IOException e ) {
                    log.error( String.format( "%s: Failed to read archive %s, will move on to the next supplementary material...",
                            geoAccession, file ), e );
                }
            }
        }

        return false;
    }

    @Override
    public Path downloadSingleCellData( GeoSeries series ) throws NoSingleCellDataFoundException {
        if ( !hasSingleCellData( series ) ) {
            throw new NoSingleCellDataFoundException( "No MEX single-cell data was found at the series-level." );
        }
        throw new UnsupportedOperationException( "MEX files were found, but single-cell data is not supported at the series level." );
    }

    /**
     * Download a GEO sample within the context of its series.
     * <p>
     * This will first download the sample with {@link #downloadSingleCellData(String, Collection)} with the merged
     * supplementary files from the series and the sample, then create a {@code series/sample} folder structure and
     * finally hard-link all the sample files in there. This ensures that if two series mention the same sample, they
     * can reuse the same files.
     */
    public Path downloadSingleCellData( GeoSeries series, GeoSample sample ) throws NoSingleCellDataFoundException, IOException {
        Assert.notNull( series.getGeoAccession() );
        Assert.notNull( sample.getGeoAccession() );
        downloadSingleCellData( sample.getGeoAccession(), mergeSupplementaryFiles( series, sample ) );
        Path sampleDir = getDownloadDirectory().resolve( sample.getGeoAccession() );
        Path destDir = getDownloadDirectory()
                .resolve( series.getGeoAccession() )
                .resolve( sample.getGeoAccession() );
        Files.createDirectories( destDir );
        try {
            log.info( String.format( "%s: Linking MEX files from %s to %s...", sample.getGeoAccession(), sampleDir, destDir ) );
            String[] files = { "barcodes.tsv.gz", "features.tsv.gz", "matrix.mtx.gz" };
            for ( String file : files ) {
                if ( !Files.exists( destDir.resolve( file ) ) ) {
                    Files.createLink( destDir.resolve( file ), sampleDir.resolve( file ) );
                } else if ( !Files.isSameFile( destDir.resolve( file ), sampleDir.resolve( file ) ) ) {
                    log.info( String.format( "%s: Overwriting existing %s with %s since it's not the same file...",
                            sample.getGeoAccession(), destDir.resolve( file ), sampleDir.resolve( file ) ) );
                    Files.delete( destDir.resolve( file ) );
                    Files.createLink( destDir.resolve( file ), sampleDir.resolve( file ) );
                } else {
                    log.debug( String.format( "%s: Skipping link creation for %s, file already exist.",
                            series.getGeoAccession(), destDir.resolve( file ) ) );
                }
            }
            return destDir;
        } catch ( Exception e ) {
            log.warn( sample.getGeoAccession() + ": An error occurred, cleaning up " + destDir + "...", e );
            // note here that the series directory is kept since it might contain other samples
            PathUtils.deleteDirectory( destDir );
            throw e;
        }
    }

    /**
     * Retrieve single-cell data for the given GEO sample to disk.
     *
     * @throws NoSingleCellDataFoundException if no single-cell data is found in the given GEO sample
     */
    @Override
    public Path downloadSingleCellData( GeoSample sample ) throws NoSingleCellDataFoundException, IOException {
        Assert.notNull( sample.getGeoAccession() );
        return downloadSingleCellData( sample.getGeoAccession(), sample.getSupplementaryFiles() );
    }

    private Path downloadSingleCellData( String geoAccession, Collection<String> supplementaryFiles ) throws IOException, NoSingleCellDataFoundException {
        Assert.notNull( getDownloadDirectory(), "A download directory must be set." );

        if ( supplementaryFiles.isEmpty() ) {
            throw new NoSingleCellDataFoundException( geoAccession + " does not have any supplementary files." );
        }

        Path sampleDirectory = getDownloadDirectory().resolve( geoAccession );

        // detect MEX (3 files per GEO sample)
        String barcodes = null, features = null, matrix = null;
        for ( String file : supplementaryFiles ) {
            if ( isMexFile( file, MexFileType.BARCODES ) ) {
                if ( barcodes != null ) {
                    throw new UnsupportedOperationException( String.format( "%s: There is already an entry for barcodes: %s, %s cannot be downloaded.", geoAccession, barcodes, file ) );
                }
                if ( barcodeMetadataFileSuffix != null && endsWith( file, barcodeMetadataFileSuffix ) ) {
                    throw new UnsupportedOperationException( "Barcode metadata files are not supported." );
                }
                barcodes = file;
            } else if ( isMexFile( file, MexFileType.FEATURES ) ) {
                if ( features != null ) {
                    throw new UnsupportedOperationException( String.format( "%s: There is already an entry for features: %s, %s cannot be downloaded.", geoAccession, features, file ) );
                }
                features = file;
            } else if ( isMexFile( file, MexFileType.MATRIX ) ) {
                if ( matrix != null ) {
                    throw new UnsupportedOperationException( String.format( "%s: There is already an entry for matrix: %s, %s cannot be downloaded.", geoAccession, matrix, file ) );
                }
                matrix = file;
            }
        }

        if ( barcodes != null && features != null && matrix != null ) {
            log.info( String.format( "%s: Downloading MEX data from supplementary materials...", geoAccession ) );
            String[] files = { barcodes, features, matrix };
            String[] dests = { "barcodes.tsv.gz", "features.tsv.gz", "matrix.mtx.gz" };
            for ( int i = 0; i < files.length; i++ ) {
                Path dest = sampleDirectory.resolve( dests[i] );
                if ( existsAndHasExpectedSize( dest, files[i], false ) ) {
                    log.info( String.format( "%s: Skipping download of %s to %s because it already exists and has expected size.",
                            geoAccession, files[i], dest ) );
                    continue;
                }
                try {
                    String file = files[i];
                    retry( ( attempt, lastAttempt ) -> {
                        StopWatch timer = StopWatch.createStarted();
                        log.info( String.format( "%s: Downloading %s to %s...", geoAccession, file, dest ) );
                        PathUtils.createParentDirectories( dest );
                        try ( InputStream is = openSupplementaryFileAsStream( file, attempt, false );
                                OutputStream os = openGzippedOutputStream( file, dest ) ) {
                            long downloadedBytes = IOUtils.copyLarge( is, os );
                            log.info( String.format( "%s: Done downloading %s (%s in %s @ %s/s).",
                                    geoAccession, file, byteCountToDisplaySize( downloadedBytes ), timer,
                                    byteCountToDisplaySize( 1000 * downloadedBytes / timer.getTime() ) ) );
                            return null;
                        } catch ( Exception e ) {
                            log.warn( String.format( "%s: MEX files could not be downloaded successfully, removing %s...", geoAccession, dest ), e );
                            if ( Files.exists( dest ) ) {
                                PathUtils.deleteFile( dest );
                            }
                            throw e;
                        }
                    }, "downloading " + file + " to " + dest + " for " + geoAccession );
                } catch ( Exception e ) {
                    log.warn( String.format( "%s: An error occurred, removing %s...", geoAccession, sampleDirectory ), e );
                    // not retrying, delete everything in the sample folder
                    PathUtils.deleteDirectory( sampleDirectory );
                    throw e;
                }
            }
            return sampleDirectory;
        }

        // detect MEX (1 archive per GEO sample)
        for ( String file : supplementaryFiles ) {
            if ( !isSupportedArchive( file ) ) {
                continue;
            }
            try {
                Boolean found = retry( ( attempt, lastAttempt ) -> {
                    try ( ArchiveInputStream<?> tis = openSupplementaryFileAsArchiveStream( file, attempt ) ) {
                        StopWatch timer = StopWatch.createStarted();
                        String barcodesT = null;
                        String featuresT = null;
                        String matrixT = null;
                        long copiedBytes = 0L;
                        ArchiveEntry te;
                        while ( ( te = tis.getNextEntry() ) != null ) {
                            if ( te.isDirectory() ) {
                                continue;
                            }
                            Path dest;
                            if ( isMexFile( te.getName(), MexFileType.BARCODES ) ) {
                                if ( barcodesT != null ) {
                                    throw new UnsupportedOperationException( String.format( "%s: There is already an entry for barcodes: %s, %s cannot be downloaded.", geoAccession, barcodesT, te.getName() ) );
                                }
                                if ( barcodeMetadataFileSuffix != null && ( te.getName().endsWith( barcodeMetadataFileSuffix ) || te.getName().endsWith( barcodeMetadataFileSuffix + ".gz" ) ) ) {
                                    throw new UnsupportedOperationException( "Barcode metadata files are not supported." );
                                }
                                dest = sampleDirectory.resolve( "barcodes.tsv.gz" );
                                barcodesT = te.getName();
                            } else if ( isMexFile( te.getName(), MexFileType.FEATURES ) ) {
                                if ( featuresT != null ) {
                                    throw new UnsupportedOperationException( String.format( "%s: There is already an entry for features: %s, %s cannot be downloaded.", geoAccession, featuresT, te.getName() ) );
                                }
                                featuresT = te.getName();
                                dest = sampleDirectory.resolve( "features.tsv.gz" );
                            } else if ( isMexFile( te.getName(), MexFileType.MATRIX ) ) {
                                if ( matrixT != null ) {
                                    throw new UnsupportedOperationException( String.format( "%s: There is already an entry for matrix: %s, %s cannot be downloaded.", geoAccession, matrixT, te.getName() ) );
                                }
                                matrixT = te.getName();
                                dest = sampleDirectory.resolve( "matrix.mtx.gz" );
                            } else if ( skipForLargeArchiveEntry( geoAccession, file, te, barcodesT, featuresT, matrixT ) ) {
                                break;
                            } else {
                                // skip to the next entry
                                continue;
                            }
                            if ( dest.toFile().exists() && dest.toFile().length() == te.getSize() ) {
                                log.info( String.format( "%s: Skipping copy of %s to %s because it already exists and has expected size of %s.",
                                        geoAccession, te.getName(), dest, byteCountToDisplaySize( te.getSize() ) ) );
                                if ( isMexFile( te.getName(), MexFileType.MATRIX ) && barcodesT != null && featuresT != null ) {
                                    // same kind of reasoning here that we use in hasSingleCellData(): if we have barcodes,
                                    // features and matrix is already on-disk, we can avoid reading the matrix
                                    break;
                                } else {
                                    continue;
                                }
                            }
                            log.info( String.format( "%s: Copying %s from archive %s to %s...", geoAccession, te.getName(), file, dest ) );
                            PathUtils.createParentDirectories( dest );
                            try ( OutputStream os = openGzippedOutputStream( te.getName(), dest ) ) {
                                String what = te.getName();
                                if ( attempt > 0 ) {
                                    what += " (attempt #" + ( attempt + 1 ) + ")";
                                }
                                copiedBytes += IOUtils.copyLarge( new ProgressInputStream( tis, what, MexDetector.class.getName(), te.getSize() ), os );
                            } catch ( Exception e ) {
                                // only remove the affected file since we might be retrying, if not the whole directory will be removed
                                log.warn( String.format( "%s: MEX file %s could not be downloaded successfully, removing %s...", geoAccession, file, dest ), e );
                                if ( Files.exists( dest ) ) {
                                    PathUtils.deleteFile( dest );
                                }
                                throw e;
                            }
                        }
                        if ( barcodesT != null && featuresT != null && matrixT != null ) {
                            if ( copiedBytes > 0 ) {
                                log.info( String.format( "%s: Done copying MEX files from archive (%s in %s @ %s/s).",
                                        geoAccession,
                                        byteCountToDisplaySize( copiedBytes ), timer,
                                        byteCountToDisplaySize( 1000 * copiedBytes / timer.getTime() ) ) );
                            }
                            return true;
                        } else if ( sampleDirectory.toFile().exists() ) {
                            log.warn( String.format( "%s: MEX files are incomplete, removing %s...", geoAccession, sampleDirectory ) );
                            PathUtils.deleteDirectory( sampleDirectory );
                        }
                        return false;
                    }
                }, "extracting MEX files from " + file );
                if ( found ) {
                    return sampleDirectory;
                }
            } catch ( Exception e ) {
                // Because we copied file as we traversed the archive, it's possible that not all expected
                // files were encountered, or maybe some of them were encountered twice, thus we need to clean
                // up the directory if that occurs.
                // If we are retrying, do not remove downloaded files to save some time: missing files might be
                // downloaded in the next attempt
                if ( sampleDirectory.toFile().exists() ) {
                    log.warn( String.format( "%s: An error occurred, removing %s...", geoAccession, sampleDirectory ), e );
                    PathUtils.deleteDirectory( sampleDirectory );
                }
                throw e;
            }
        }

        throw new NoSingleCellDataFoundException( "No single-cell data was downloaded for " + geoAccession + "." );
    }

    @Override
    public List<String> getAdditionalSupplementaryFiles( GeoSeries series ) {
        Assert.notNull( series.getGeoAccession() );
        return getAdditionalSupplementaryFiles( series.getGeoAccession(), series.getSupplementaryFiles().stream().filter( f -> !f.endsWith( "_RAW.tar" ) ) );
    }

    @Override
    public List<String> getAdditionalSupplementaryFiles( GeoSample sample ) {
        Assert.notNull( sample.getGeoAccession() );
        return getAdditionalSupplementaryFiles( sample.getGeoAccession(), sample.getSupplementaryFiles().stream() );
    }

    private List<String> getAdditionalSupplementaryFiles( String geoAccession, Stream<String> supplementaryFiles ) {
        return supplementaryFiles
                .flatMap( file -> {
                    if ( !isSupportedArchive( file ) ) {
                        return Stream.of( file );
                    }
                    //extract files in tar
                    try {
                        return retry( ( attempt, lastAttempt ) -> {
                            try ( ArchiveInputStream<?> tis = openSupplementaryFileAsArchiveStream( file, attempt ) ) {
                                List<String> files = new ArrayList<>();
                                ArchiveEntry entry;
                                while ( ( entry = tis.getNextEntry() ) != null ) {
                                    if ( entry.isDirectory() ) {
                                        continue;
                                    }
                                    if ( skipForLargeArchiveEntry( geoAccession, file, entry, null, null, null ) ) {
                                        break;
                                    }
                                    // check for common mistakes from submitters
                                    if ( FilenameUtils.getName( entry.getName() ).equals( ".DS_Store" )
                                            || FilenameUtils.getName( entry.getName() ).startsWith( "._" )
                                            || FilenameUtils.getName( entry.getName() ).equals( "index.html" ) ) {
                                        continue;
                                    }
                                    // add a {file}! prefix to make it clear it was found inside an archive, this
                                    // syntax is similar to how Java refers to files within JAR.
                                    // TODO: check if file are URL-encoded in GEO metadata, in which case a '!'
                                    //       could never appear
                                    files.add( file + "!/" + entry.getName() );
                                }
                                return files.stream();
                            }
                        }, "looking for additional supplementary files in " + file );
                    } catch ( IOException e ) {
                        log.error( String.format( "%s: Failed to read archive %s, will move on to the next supplementary material...",
                                geoAccession, file ), e );
                        return Stream.of( file );
                    }
                } )
                .filter( this::isAdditionalSupplementaryFile )
                .collect( Collectors.toList() );
    }

    private boolean isAdditionalSupplementaryFile( String f ) {
        return !isMexFile( f, MexFileType.FEATURES )
                && !isMexFile( f, MexFileType.BARCODES )
                && !isMexFile( f, MexFileType.MATRIX );
    }

    @Override
    public SingleCellDataLoader getSingleCellDataLoader( GeoSeries series ) throws NoSingleCellDataFoundException {
        Assert.notNull( series.getGeoAccession() );
        Assert.notNull( getDownloadDirectory(), "A download directory must be set." );

        List<String> sampleNames = new ArrayList<>();
        List<Path> barcodesFiles = new ArrayList<>(),
                featuresFiles = new ArrayList<>(),
                matricesFiles = new ArrayList<>();

        for ( GeoSample sample : series.getSamples() ) {
            Assert.notNull( sample.getGeoAccession() );
            Path sampleDir = getDownloadDirectory().resolve( sample.getGeoAccession() );
            if ( Files.exists( sampleDir ) ) {
                sampleNames.add( sample.getGeoAccession() );
                Path b = sampleDir.resolve( "barcodes.tsv.gz" ), f = sampleDir.resolve( "features.tsv.gz" ), m = sampleDir.resolve( "matrix.mtx.gz" );
                if ( Files.exists( b ) && Files.exists( f ) && Files.exists( m ) ) {
                    barcodesFiles.add( b );
                    featuresFiles.add( f );
                    matricesFiles.add( m );
                } else {
                    throw new IllegalStateException( String.format( "Expected MEX files are missing in %s", sampleDir ) );
                }
            }
        }

        if ( !sampleNames.isEmpty() ) {
            return new MexSingleCellDataLoader( sampleNames, barcodesFiles, featuresFiles, matricesFiles );
        }

        throw new NoSingleCellDataFoundException( "No single-cell data was found for " + series.getGeoAccession() );
    }

    private enum MexFileType {
        BARCODES,
        FEATURES,
        MATRIX
    }

    private boolean isMexFile( String name, MexFileType type ) {
        switch ( type ) {
            case BARCODES:
                return endsWith( name, barcodesFileSuffix )
                        // this is used for combined references
                        || ( barcodeMetadataFileSuffix != null && ( endsWith( name, barcodeMetadataFileSuffix ) ) );
            case FEATURES:
                return endsWith( name, featuresFileSuffix )
                        // older version of 10X pipeline uses genes.tsv, we import it as features.tsv
                        || ( genesFileSuffix != null && endsWith( name, genesFileSuffix ) );
            case MATRIX:
                return endsWith( name, matrixFileSuffix );
            default:
                return false;
        }
    }

    private boolean endsWith( String filename, String suffix ) {
        return filename.endsWith( suffix ) || filename.endsWith( suffix + ".gz" );
    }

    /**
     * Check if a supplementary file should be skipped given an archive entry that has to be consumed.
     */
    private boolean skipForLargeArchiveEntry( String geoAccession, String file, ArchiveEntry te, @Nullable String barcodesT, @Nullable String featuresT, @Nullable String matrixT ) {
        if ( te.getSize() <= maxEntrySizeInArchiveToSkip )
            return false;
        String m = String.format( "%s: %s has an entry %s of %s exceeding %s",
                geoAccession, file, te.getName(), byteCountToDisplaySize( te.getSize() ), byteCountToDisplaySize( maxEntrySizeInArchiveToSkip ) );
        if ( barcodesT == null && featuresT == null && matrixT == null ) {
            log.warn( m + ", the rest of the archive will be ignored." );
            return true;
        } else {
            log.warn( m + ", but a MEX file was already found, the rest of the archive will be read." );
            return false;
        }
    }

    /**
     * Check if a given file is a supported archive format.
     */
    private boolean isSupportedArchive( String file ) {
        return file.endsWith( ".tar" ) || file.endsWith( ".tar.gz" ) || file.endsWith( ".zip" );
    }

    /**
     * Open a given file as an archive stream.
     */
    private ArchiveInputStream<?> openSupplementaryFileAsArchiveStream( String file, int attempt ) throws IOException {
        if ( file.endsWith( ".zip" ) ) {
            return new ZipArchiveInputStream( openSupplementaryFileAsStream( file, attempt, false ) );
        } else if ( file.endsWith( ".tar" ) ) {
            return new TarArchiveInputStream( openSupplementaryFileAsStream( file, attempt, false ) );
        } else if ( file.endsWith( ".tar.gz" ) ) {
            return new TarArchiveInputStream( openSupplementaryFileAsStream( file, attempt, true ) );
        } else {
            throw new IllegalArgumentException( "No idea how to open " + file );
        }
    }

    private OutputStream openGzippedOutputStream( String name, Path dest ) throws IOException {
        if ( name.endsWith( ".gz" ) ) {
            return Files.newOutputStream( dest );
        } else {
            return new GZIPOutputStream( Files.newOutputStream( dest ) );
        }
    }

    private Set<String> mergeSupplementaryFiles( GeoSeries series, GeoSample sample ) {
        Set<String> mergedSupplementaryFiles = new HashSet<>( sample.getSupplementaryFiles() );
        series.getSupplementaryFiles().stream()
                // omit this, otherwise it might get looked up, and it's redundant since it contains the supplementary
                // materials from all the samples
                .filter( f -> !f.endsWith( "_RAW.tar" ) )
                .forEach( mergedSupplementaryFiles::add );
        return mergedSupplementaryFiles;
    }
}
