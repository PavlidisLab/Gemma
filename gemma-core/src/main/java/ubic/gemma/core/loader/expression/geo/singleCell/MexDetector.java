package ubic.gemma.core.loader.expression.geo.singleCell;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.file.PathUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.tools.tar.TarEntry;
import org.apache.tools.tar.TarInputStream;
import org.springframework.util.Assert;
import ubic.gemma.core.loader.expression.geo.model.GeoSample;
import ubic.gemma.core.loader.expression.geo.model.GeoSeries;
import ubic.gemma.core.loader.expression.singleCell.MexSingleCellDataLoader;
import ubic.gemma.core.loader.expression.singleCell.SingleCellDataLoader;
import ubic.gemma.core.util.ProgressInputStream;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.ExternalDatabases;
import ubic.gemma.model.expression.bioAssay.BioAssay;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;

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
public class MexDetector extends AbstractSingleCellDetector implements SingleCellDetector {

    /**
     * Use GEO accession for comparing the sample name.
     */
    private static final SingleCellDataLoader.BioAssayToSampleNameMatcher GEO_SAMPLE_NAME_COMPARATOR = new SingleCellDataLoader.BioAssayToSampleNameMatcher() {
        @Override
        public boolean matches( BioAssay ba, String n ) {
            return matchGeoAccession( ba.getAccession(), n )
                    || ba.getName().equals( n )
                    || matchGeoAccession( ba.getSampleUsed().getExternalAccession(), n )
                    || ba.getSampleUsed().getName().equals( n );
        }

        private boolean matchGeoAccession( @Nullable DatabaseEntry accession, String n ) {
            return accession != null && accession.getExternalDatabase().getName().equals( ExternalDatabases.GEO )
                    && accession.getAccession().equals( n );
        }
    };

    private long maxEntrySizeToSkipInTar = 20_000_000L;

    /**
     * Set the maximum size of TAR entry to skip. If an entry exceeding this size is found, the supplementary material
     * will be ignored.
     */
    public void setMaxEntrySizeToSkipInTar( long maxEntrySizeToSkipInTar ) {
        this.maxEntrySizeToSkipInTar = maxEntrySizeToSkipInTar;
    }

    /**
     * {@inheritDoc}
     * <p>
     * MEX data detection is not supported at the series level, so while this method can return true if barcodes/genes/matrices
     * are present in the series supplementary files, {@link #downloadSingleCellData(GeoSeries)} will subsequently fail.
     */
    @Override
    public boolean hasSingleCellData( GeoSeries series ) {
        // don't bother looking up MEX files in TAR archives at the series-level, it's just wasteful since we cannot
        // download them
        return hasSingleCellData( series.getGeoAccession(), series.getSupplementaryFiles(), false );
    }

    /**
     * Check if a sample contains single-cell data in the context of its series.
     */
    public boolean hasSingleCellData( GeoSeries series, GeoSample sample ) {
        return hasSingleCellData( series.getGeoAccession(), mergeSupplementaryFiles( series, sample ), true );
    }

    @Override
    public boolean hasSingleCellData( GeoSample sample ) {
        return hasSingleCellData( sample, true );
    }

    /**
     * Check if a GEO sample contains single-cell data.
     * @param allowTarLookup allow looking into TAR archives for MEX files
     */
    public boolean hasSingleCellData( GeoSample sample, boolean allowTarLookup ) {
        return hasSingleCellData( sample.getGeoAccession(), sample.getSupplementaryFiles(), allowTarLookup );
    }

    /**
     * Check if a sample or series has single-cell data.
     * @param geoAccession       GEO accession of the sample or series
     * @param supplementaryFiles list of supplementary file
     * @param allowTarLookup     allow looking up TAR archives for MEX data, use this with parsimony because it requires
     *                           partially downloading the archive
     */
    private boolean hasSingleCellData( String geoAccession, Collection<String> supplementaryFiles, boolean allowTarLookup ) {
        // detect MEX (3 files per GEO sample)
        String barcodes = null, features = null, matrix = null;
        for ( String file : supplementaryFiles ) {
            if ( isMexFile( file, MexFileType.BARCODES ) ) {
                barcodes = file;
            } else if ( isMexFile( file, MexFileType.FEATURES ) || isMexFile( file, MexFileType.GENES ) ) {
                if ( isMexFile( file, MexFileType.GENES ) ) {
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

        // detect MEX (1 TAR archive per GEO sample)
        for ( String file : supplementaryFiles ) {
            if ( allowTarLookup && ( file.endsWith( ".tar" ) || file.endsWith( ".tar.gz" ) ) ) {
                log.info( "Looking up the TAR header of " + file + " for MEX data..." );
                try {
                    Boolean done = retry( ( attempt, lastAttempt ) -> {
                        String barcodesT = null;
                        String featuresT = null;
                        String matrixT = null;
                        // we just have to read the header of the TAR archive and not its content
                        try ( TarInputStream tis = new TarInputStream( openSupplementaryFileAsStream( file, attempt, true ) ) ) {
                            TarEntry te;
                            while ( ( te = tis.getNextEntry() ) != null ) {
                                if ( !te.isFile() ) {
                                    continue;
                                }
                                if ( isMexFile( te.getName(), MexFileType.BARCODES ) ) {
                                    barcodesT = te.getName();
                                } else if ( isMexFile( te.getName(), MexFileType.FEATURES ) || isMexFile( te.getName(), MexFileType.GENES ) ) {
                                    if ( isMexFile( file, MexFileType.GENES ) ) {
                                        log.info( geoAccession + ": Found an old-style MEX file " + te.getName() + " in a TAR archive, treating it as features.tsv." );
                                    }
                                    featuresT = te.getName();
                                } else if ( isMexFile( te.getName(), MexFileType.MATRIX ) ) {
                                    matrixT = te.getName();
                                    if ( featuresT != null && barcodesT != null ) {
                                        // TAR entries are generally sorted, so features.tsv and barcodes.tsv appear before
                                        // matrix.mtx, if we were not to skip at this point, the whole matrix would have to be
                                        // read, which would be inefficient. The downside is that we cannot detect cases
                                        // where an archive contains two matrices
                                        break;
                                    }
                                } else if ( te.getSize() > maxEntrySizeToSkipInTar ) {
                                    log.warn( geoAccession + ": " + file + " has an entry exceeding " + maxEntrySizeToSkipInTar + " B, the rest of the archive will be ignored." );
                                    break;
                                }
                            }
                            if ( barcodesT != null && featuresT != null && matrixT != null ) {
                                log.info( String.format( "%s: Found MEX files bundled in a TAR archive %s:\n\t\t%s\n\t\t%s\n\t\t%s",
                                        geoAccession, file, barcodesT, featuresT, matrixT ) );
                                return true;
                            } else if ( barcodesT != null || featuresT != null || matrixT != null ) {
                                log.warn( String.format( "%s: Found incomplete MEX files bundled in a TAR archive %s:\n\t%s",
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
                    log.error( String.format( "%s: Failed to read TAR archive %s, will move on to the next supplementary material...",
                            geoAccession, file ), e );
                }
            }
        }

        return false;
    }

    @Override
    public void downloadSingleCellData( GeoSeries series ) throws NoSingleCellDataFoundException {
        if ( hasSingleCellData( series ) ) {
            throw new NoSingleCellDataFoundException( "MEX files were found, but single-cell data is not supported at the series level." );
        }
        throw new NoSingleCellDataFoundException( "MEX does not support single-cell data at the series level." );
    }

    /**
     * Download a GEO sample within the context of its series.
     * <p>
     * This will first download the sample with {@link #downloadSingleCellData(String, Collection)} with the merged
     * supplementary files from the series and the sample, then create a {@code series/sample} folder structure and
     * finally hard-link all the sample files in there. This ensures that if two series mention the same sample, they
     * can reuse the same files.
     */
    public void downloadSingleCellData( GeoSeries series, GeoSample sample ) throws NoSingleCellDataFoundException, IOException {
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
        } catch ( Exception e ) {
            log.warn( sample.getGeoAccession() + ": An I/O error occurred, cleaning up " + destDir + "...", e );
            // note here that the series directory is kept since it might contain other samples
            PathUtils.deleteDirectory( destDir );
            throw e;
        }
    }

    /**
     * Retrieve single-cell data for the given GEO sample to disk.
     * @throws NoSingleCellDataFoundException if no single-cell data is found in the given GEO sample
     */
    @Override
    public void downloadSingleCellData( GeoSample sample ) throws NoSingleCellDataFoundException, IOException {
        downloadSingleCellData( sample.getGeoAccession(), sample.getSupplementaryFiles() );
    }

    private void downloadSingleCellData( String geoAccession, Collection<String> supplementaryFiles ) throws IOException, NoSingleCellDataFoundException {
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
                    log.warn( String.format( "%s: There is already an entry for barcodes: %s", geoAccession, barcodes ) );
                    barcodes = null;
                    break;
                }
                barcodes = file;
            } else if ( isMexFile( file, MexFileType.FEATURES ) || isMexFile( file, MexFileType.GENES ) ) {
                if ( features != null ) {
                    log.warn( String.format( "%s: There is already an entry for features: %s", geoAccession, features ) );
                    features = null;
                    break;
                }
                features = file;
            } else if ( isMexFile( file, MexFileType.MATRIX ) ) {
                if ( matrix != null ) {
                    log.warn( String.format( "%s: There is already an entry for matrix: %s", geoAccession, matrix ) );
                    matrix = null;
                    break;
                }
                matrix = file;
            }
        }

        if ( barcodes != null && features != null && matrix != null ) {
            log.info( String.format( "%s: Downloading MEX data from supplementary materials...", geoAccession ) );
            String[] files = { barcodes, features, matrix };
            String[] dests = { "barcodes.tsv.gz", "features.tsv.gz", "matrix.mtx.gz" };
            StopWatch timer = StopWatch.createStarted();
            for ( int i = 0; i < files.length; i++ ) {
                Path dest = sampleDirectory.resolve( dests[i] );
                if ( existsAndHasExpectedSize( dest, files[i] ) ) {
                    log.info( String.format( "%s: Skipping download of %s to %s because it already exists and has expected size.",
                            geoAccession, files[i], dest ) );
                    continue;
                }
                try {
                    String file = files[i];
                    retry( ( attempt, lastAttempt ) -> {
                        log.info( String.format( "%s: Downloading %s to %s...", geoAccession, file, dest ) );
                        PathUtils.createParentDirectories( dest );
                        try ( InputStream is = openSupplementaryFileAsStream( file, attempt, false );
                                OutputStream os = openGzippedOutputStream( file, dest ) ) {
                            long downloadedBytes = IOUtils.copyLarge( is, os );
                            log.info( String.format( "%s: Done downloading %s (%s in %s @ %.3f MB/s).",
                                    geoAccession, file, FileUtils.byteCountToDisplaySize( downloadedBytes ), timer,
                                    ( 1000.0 / ( 1000.0 * 1000.0 ) ) * ( downloadedBytes / timer.getTime() ) ) );
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
                    log.warn( String.format( "%s: MEX files could not be downloaded successfully, removing %s...", geoAccession, sampleDirectory ), e );
                    // not retrying, delete everything in the sample folder
                    PathUtils.deleteDirectory( sampleDirectory );
                    throw e;
                }
            }
            return;
        }

        // detect MEX (1 TAR archive per GEO sample)
        for ( String file : supplementaryFiles ) {
            if ( file.endsWith( ".tar" ) || file.endsWith( ".tar.gz" ) ) {
                Boolean found = retry( ( attempt, lastAttempt ) -> {
                    String barcodesT = null;
                    String featuresT = null;
                    String matrixT = null;
                    boolean completed = false;
                    try ( TarInputStream tis = new TarInputStream( openSupplementaryFileAsStream( file, attempt, true ) ) ) {
                        StopWatch timer = StopWatch.createStarted();
                        long copiedBytes = 0L;
                        TarEntry te;
                        while ( ( te = tis.getNextEntry() ) != null ) {
                            if ( !te.isFile() ) {
                                continue;
                            }
                            Path dest;
                            if ( isMexFile( te.getName(), MexFileType.BARCODES ) ) {
                                if ( barcodesT != null ) {
                                    log.warn( String.format( "%s: There is already an entry for barcodes: %s", geoAccession, barcodesT ) );
                                    barcodesT = null;
                                    break;
                                }
                                dest = sampleDirectory.resolve( "barcodes.tsv.gz" );
                                barcodesT = te.getName();
                            } else if ( isMexFile( te.getName(), MexFileType.FEATURES ) || isMexFile( te.getName(), MexFileType.GENES ) ) {
                                if ( featuresT != null ) {
                                    log.warn( String.format( "%s: There is already an entry for features: %s", geoAccession, featuresT ) );
                                    featuresT = null;
                                    break;
                                }
                                featuresT = te.getName();
                                dest = sampleDirectory.resolve( "features.tsv.gz" );
                            } else if ( isMexFile( te.getName(), MexFileType.MATRIX ) ) {
                                if ( matrixT != null ) {
                                    log.warn( String.format( "%s: There is already an entry for matrix: %s", geoAccession, matrixT ) );
                                    matrixT = null;
                                    break;
                                }
                                matrixT = te.getName();
                                dest = sampleDirectory.resolve( "matrix.mtx.gz" );
                            } else if ( te.getSize() > maxEntrySizeToSkipInTar ) {
                                log.warn( geoAccession + ": " + file + " has an entry exceeding " + maxEntrySizeToSkipInTar + " B, the rest of the archive will be ignored." );
                                break;
                            } else {
                                // skip to the next entry
                                continue;
                            }
                            if ( dest.toFile().exists() && dest.toFile().length() == te.getSize() ) {
                                log.info( String.format( "%s: Skipping copy of %s to %s because it already exists and has expected size of %s.",
                                        geoAccession, te.getName(), dest, FileUtils.byteCountToDisplaySize( te.getSize() ) ) );
                                if ( isMexFile( te.getName(), MexFileType.MATRIX ) && barcodesT != null && featuresT != null ) {
                                    // same kind of reasoning here that we use in hasSingleCellData(): if we have barcodes,
                                    // features and matrix is already on-disk, we can avoid reading the matrix
                                    break;
                                } else {
                                    continue;
                                }
                            }
                            log.info( String.format( "%s: Copying %s from TAR archive %s to %s...", geoAccession, te.getName(), file, dest ) );
                            PathUtils.createParentDirectories( dest );
                            try ( OutputStream os = openGzippedOutputStream( te.getName(), dest ) ) {
                                String what = te.getName();
                                if ( attempt > 0 ) {
                                    what += " (attempt #" + ( attempt + 1 ) + ")";
                                }
                                copiedBytes += IOUtils.copyLarge( new ProgressInputStream( tis, what, MexDetector.class.getName(), te.getSize() ), os );
                            } catch ( Exception e ) {
                                    // only remove the affected file since we're retrying
                                log.warn( String.format( "%s: MEX file could not be downloaded successfully, removing %s...", geoAccession, dest ), e );
                                    if ( Files.exists( dest ) ) {
                                        PathUtils.deleteFile( dest );
                                    }
                                throw e;
                            }
                        }
                        completed = barcodesT != null && featuresT != null && matrixT != null;
                        if ( completed ) {
                            if ( copiedBytes > 0 ) {
                                log.info( String.format( "%s: Done copying MEX files from TAR archive (%s in %s @ %.3f MB/s).",
                                        geoAccession,
                                        FileUtils.byteCountToDisplaySize( copiedBytes ), timer,
                                        ( 1000.0 / ( 1000.0 * 1000.0 ) ) * ( copiedBytes / timer.getTime() ) ) );
                            }
                        }
                    } finally {
                        // Because we copied file as we traversed the TAR archive, it's possible that not all expected
                        // files were encountered, or maybe some of them were encountered twice, thus we need to clean
                        // up the directory if that occurs.
                        // If we are retrying, do not remove downloaded files to save some time: missing files might be
                        // downloaded in the next attempt
                        if ( !completed && !lastAttempt && sampleDirectory.toFile().exists() ) {
                            log.warn( String.format( "%s: MEX files are incomplete, removing %s...", geoAccession, sampleDirectory ) );
                            PathUtils.deleteDirectory( sampleDirectory );
                        }
                    }
                    return completed;
                }, "extracting MEX files from " + file );
                if ( found ) {
                    return;
                }
            }
        }

        throw new NoSingleCellDataFoundException( "No single-cell data was downloaded for " + geoAccession + "." );
    }

    @Override
    public List<String> getAdditionalSupplementaryFiles( GeoSeries series ) {
        return getAdditionalSupplementaryFiles( series.getGeoAccession(), series.getSupplementaryFiles().stream().filter( f -> !f.endsWith( "_RAW.tar" ) ) );
    }

    /**
     * Obtain additional supplementary file of a sample within the context of its series.
     * @see #getAdditionalSupplementaryFiles(GeoSample)
     */
    public List<String> getAdditionalSupplementaryFiles( GeoSeries series, GeoSample sample ) {
        return getAdditionalSupplementaryFiles( sample.getGeoAccession(), mergeSupplementaryFiles( series, sample ).stream() );
    }

    @Override
    public List<String> getAdditionalSupplementaryFiles( GeoSample sample ) {
        return getAdditionalSupplementaryFiles( sample.getGeoAccession(), sample.getSupplementaryFiles().stream() );
    }

    private List<String> getAdditionalSupplementaryFiles( String geoAccession, Stream<String> supplementaryFiles ) {
        return supplementaryFiles
                .flatMap( file -> {
                    if ( file.endsWith( ".tar" ) || file.endsWith( ".tar.gz" ) ) {
                        //extract files in tar
                        try {
                            return retry( ( attempt, lastAttempt ) -> {
                                try ( TarInputStream tis = new TarInputStream( openSupplementaryFileAsStream( file, attempt, true ) ) ) {
                                    List<String> files = new ArrayList<>();
                                    TarEntry entry;
                                    while ( ( entry = tis.getNextEntry() ) != null ) {
                                        if ( !entry.isFile() ) {
                                            continue;
                                        }
                                        if ( entry.getSize() > maxEntrySizeToSkipInTar ) {
                                            log.warn( geoAccession + ": " + file + " has an entry exceeding " + maxEntrySizeToSkipInTar + " B, the rest of the archive will be ignored." );
                                            break;
                                        }
                                        // add a {file}! prefix to make it clear it was found inside an archive, this
                                        // syntax is similar to how Java refers to files within JAR.
                                        // TODO: check if file are URL-encoded in GEO metadata, in which case a '!'
                                        //       could never appear
                                        files.add( file + "!" + URLEncoder.encode( entry.getName(), StandardCharsets.UTF_8.name() ) );
                                    }
                                    return files.stream();
                                }
                            }, "looking for additional supplementary files in " + file );
                        } catch ( IOException e ) {
                            log.error( String.format( "%s: Failed to read TAR archive %s, will move on to the next supplementary material...",
                                    geoAccession, file ), e );
                            return Stream.of( file );
                        }
                    } else {
                        return Stream.of( file );
                    }
                } )
                .filter( this::isAdditionalSupplementaryFile )
                .collect( Collectors.toList() );
    }

    private boolean isAdditionalSupplementaryFile( String f ) {
        return !isMexFile( f, MexFileType.GENES )
                && !isMexFile( f, MexFileType.FEATURES )
                && !isMexFile( f, MexFileType.BARCODES )
                && !isMexFile( f, MexFileType.MATRIX );
    }

    @Override
    public SingleCellDataLoader getSingleCellDataLoader( GeoSeries series ) throws NoSingleCellDataFoundException {
        Assert.notNull( getDownloadDirectory(), "A download directory must be set." );

        List<String> sampleNames = new ArrayList<>();
        List<Path> barcodesFiles = new ArrayList<>(),
                featuresFiles = new ArrayList<>(),
                matricesFiles = new ArrayList<>();

        for ( GeoSample sample : series.getSamples() ) {
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
            MexSingleCellDataLoader loader = new MexSingleCellDataLoader( sampleNames, barcodesFiles, featuresFiles, matricesFiles );
            loader.setBioAssayToSampleNameMatcher( GEO_SAMPLE_NAME_COMPARATOR );
            return loader;
        }

        throw new NoSingleCellDataFoundException( "No single-cell data was found for " + series.getGeoAccession() );
    }

    private enum MexFileType {
        BARCODES,
        FEATURES,
        GENES,
        MATRIX
    }

    private boolean isMexFile( String name, MexFileType type ) {
        switch ( type ) {
            case BARCODES:
                return name.endsWith( "barcodes.tsv" ) || name.endsWith( "barcodes.tsv.gz" );
            case FEATURES:
                return name.endsWith( "features.tsv" ) || name.endsWith( "features.tsv.gz" );
            case GENES:
                // older version of 10X pipeline uses genes.tsv, we import it as features.tsv
                return name.endsWith( "genes.tsv" ) || name.endsWith( "genes.tsv.gz" );
            case MATRIX:
                return name.endsWith( "matrix.mtx" ) || name.endsWith( "matrix.mtx.gz" );
            default:
                return false;
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
                // omit this, otherwise it might get looked up and it's redundant since it contains the supplementary
                // materials from all the samples
                .filter( f -> !f.endsWith( "_RAW.tar" ) )
                .forEach( mergedSupplementaryFiles::add );
        return mergedSupplementaryFiles;
    }
}