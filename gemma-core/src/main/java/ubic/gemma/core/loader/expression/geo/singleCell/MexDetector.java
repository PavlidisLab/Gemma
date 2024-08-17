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
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.ExternalDatabases;
import ubic.gemma.model.expression.bioAssay.BioAssay;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
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
                for ( int i = 0; i <= maxRetries; i++ ) {
                    barcodes = null;
                    features = null;
                    matrix = null;
                    // we just have to read the header of the TAR archive and not its content
                    try ( TarInputStream tis = new TarInputStream( openSupplementaryFileAsStream( file, true ) ) ) {
                        TarEntry te;
                        while ( ( te = tis.getNextEntry() ) != null ) {
                            if ( !te.isFile() ) {
                                continue;
                            }
                            if ( isMexFile( te.getName(), MexFileType.BARCODES ) ) {
                                barcodes = te.getName();
                            } else if ( isMexFile( te.getName(), MexFileType.FEATURES ) || isMexFile( te.getName(), MexFileType.GENES ) ) {
                                if ( isMexFile( file, MexFileType.GENES ) ) {
                                    log.info( geoAccession + ": Found an old-style MEX file " + te.getName() + " in a TAR archive, treating it as features.tsv." );
                                }
                                features = te.getName();
                            } else if ( isMexFile( te.getName(), MexFileType.MATRIX ) ) {
                                matrix = te.getName();
                                if ( features != null && barcodes != null ) {
                                    // TAR entries are generally sorted, so features.tsv and barcodes.tsv appear before
                                    // matrix.mtx, if we were not to skip at this point, the whole matrix would have to be
                                    // read, which would be inefficient. The downside is that we cannot detect cases
                                    // where an archive contains two matrices
                                    break;
                                }
                            }
                        }
                        if ( barcodes != null && features != null && matrix != null ) {
                            log.info( String.format( "%s: Found MEX files bundled in a TAR archive %s:\n\t\t%s\n\t\t%s\n\t\t%s",
                                    geoAccession, file, barcodes, features, matrix ) );
                            return true;
                        } else if ( barcodes != null || features != null || matrix != null ) {
                            log.warn( String.format( "%s: Found incomplete MEX files bundled in a TAR archive %s:\n\t%s",
                                    geoAccession, file,
                                    Stream.of( barcodes, features, matrix ).filter( Objects::nonNull ).collect( Collectors.joining( "\n\t" ) ) ) );
                        }
                    } catch ( Exception e ) {
                        log.warn( String.format( "%s: MEX files could not be retrieved successfully.", geoAccession ), e );
                        if ( isRetryable( i, e ) ) {
                            log.info( String.format( "%s: Retrying download of %s...", geoAccession, file ) );
                            backoff( i );
                        } else {
                            if ( i == maxRetries ) {
                                log.error( String.format( "%s: Failed to read TAR archive %s, will move on to the next supplementary material...",
                                        geoAccession, file ), e );
                            }
                            break;
                        }
                    }
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
     * This will first download the sample with {@link #downloadSingleCellData(GeoSample)}, then create a {@code series/sample}
     * folder structure and  finally hard-link all the sample files in there. This ensures that if two series mention
     * the same sample, they can reuse the same files.
     */
    public void downloadSingleCellData( GeoSeries series, GeoSample sample ) throws NoSingleCellDataFoundException, IOException {
        downloadSingleCellData( sample );
        Path sampleDir = downloadDirectory.resolve( sample.getGeoAccession() );
        Path destDir = downloadDirectory
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
        } catch ( IOException e ) {
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
        Assert.notNull( downloadDirectory, "A download directory must be set." );

        if ( sample.getSupplementaryFiles().isEmpty() ) {
            throw new NoSingleCellDataFoundException( sample.getGeoAccession() + " does not have any supplementary files." );
        }

        Path sampleDirectory = downloadDirectory.resolve( sample.getGeoAccession() );

        // detect MEX (3 files per GEO sample)
        String barcodes = null, features = null, matrix = null;
        for ( String file : sample.getSupplementaryFiles() ) {
            if ( isMexFile( file, MexFileType.BARCODES ) ) {
                if ( barcodes != null ) {
                    log.warn( String.format( "%s: There is already an entry for barcodes: %s", sample.getGeoAccession(), barcodes ) );
                    barcodes = null;
                    break;
                }
                barcodes = file;
            } else if ( isMexFile( file, MexFileType.FEATURES ) || isMexFile( file, MexFileType.GENES ) ) {
                if ( features != null ) {
                    log.warn( String.format( "%s: There is already an entry for features: %s", sample.getGeoAccession(), features ) );
                    features = null;
                    break;
                }
                features = file;
            } else if ( isMexFile( file, MexFileType.MATRIX ) ) {
                if ( matrix != null ) {
                    log.warn( String.format( "%s: There is already an entry for matrix: %s", sample.getGeoAccession(), matrix ) );
                    matrix = null;
                    break;
                }
                matrix = file;
            }
        }

        if ( barcodes != null && features != null && matrix != null ) {
            log.info( String.format( "%s: Downloading MEX data from supplementary materials...", sample.getGeoAccession() ) );
            String[] files = { barcodes, features, matrix };
            String[] dests = { "barcodes.tsv.gz", "features.tsv.gz", "matrix.mtx.gz" };
            StopWatch timer = StopWatch.createStarted();
            for ( int i = 0; i < files.length; i++ ) {
                Path dest = sampleDirectory.resolve( dests[i] );
                if ( existsAndHasExpectedSize( dest, files[i] ) ) {
                    log.info( String.format( "%s: Skipping download of %s to %s because it already exists and has expected size.",
                            sample.getGeoAccession(), files[i], dest ) );
                    continue;
                }
                for ( int j = 0; j <= maxRetries; j++ ) {
                    log.info( String.format( "%s: Downloading %s to %s...", sample.getGeoAccession(), files[i], dest ) );
                    PathUtils.createParentDirectories( dest );
                    try ( InputStream is = openSupplementaryFileAsStream( files[i], false );
                            OutputStream os = openGzippedOutputStream( files[i], dest ) ) {
                        long downloadedBytes = IOUtils.copyLarge( is, os );
                        log.info( String.format( "%s: Done downloading %s (%s in %s @ %.3f MB/s).",
                                sample.getGeoAccession(), files[i], FileUtils.byteCountToDisplaySize( downloadedBytes ), timer,
                                ( 1000.0 / ( 1000.0 * 1000.0 ) ) * ( downloadedBytes / timer.getTime() ) ) );
                        break;
                    } catch ( Exception e ) {
                        if ( isRetryable( j, e ) ) {
                            log.warn( String.format( "%s: MEX files could not be downloaded successfully, removing %s...", sample.getGeoAccession(), dest ), e );
                            // only delete the problematic file since we're retrying
                            if ( Files.exists( dest ) ) {
                                PathUtils.deleteFile( dest );
                            }
                            log.info( String.format( "%s: Retrying download of %s...", sample.getGeoAccession(), files[i] ) );
                            backoff( i );
                        } else {
                            log.warn( String.format( "%s: MEX files could not be downloaded successfully, removing %s...", sample.getGeoAccession(), sampleDirectory ), e );
                            // not retrying, delete everything in the sample folder
                            PathUtils.deleteDirectory( sampleDirectory );
                            if ( i == maxRetries ) {
                                log.error( sample.getGeoAccession() + ": Maximum number of retries reached for " + files[i] + ", raising the last exception." );
                            }
                            throw e;
                        }
                    }
                }
            }
            return;
        }

        // detect MEX (1 TAR archive per GEO sample)
        for ( String file : sample.getSupplementaryFiles() ) {
            if ( file.endsWith( ".tar" ) || file.endsWith( ".tar.gz" ) ) {
                for ( int i = 0; i <= maxRetries; i++ ) {
                    barcodes = null;
                    features = null;
                    matrix = null;
                    boolean completed = false;
                    boolean retrying = false;
                    try ( TarInputStream tis = new TarInputStream( openSupplementaryFileAsStream( file, true ) ) ) {
                        StopWatch timer = StopWatch.createStarted();
                        long copiedBytes = 0L;
                        TarEntry te;
                        while ( ( te = tis.getNextEntry() ) != null ) {
                            if ( !te.isFile() ) {
                                continue;
                            }
                            Path dest;
                            if ( isMexFile( te.getName(), MexFileType.BARCODES ) ) {
                                if ( barcodes != null ) {
                                    log.warn( String.format( "%s: There is already an entry for barcodes: %s", sample.getGeoAccession(), barcodes ) );
                                    barcodes = null;
                                    break;
                                }
                                dest = sampleDirectory.resolve( "barcodes.tsv.gz" );
                                barcodes = te.getName();
                            } else if ( isMexFile( te.getName(), MexFileType.FEATURES ) || isMexFile( te.getName(), MexFileType.GENES ) ) {
                                if ( features != null ) {
                                    log.warn( String.format( "%s: There is already an entry for features: %s", sample.getGeoAccession(), features ) );
                                    features = null;
                                    break;
                                }
                                features = te.getName();
                                dest = sampleDirectory.resolve( "features.tsv.gz" );
                            } else if ( isMexFile( te.getName(), MexFileType.MATRIX ) ) {
                                if ( matrix != null ) {
                                    log.warn( String.format( "%s: There is already an entry for matrix: %s", sample.getGeoAccession(), matrix ) );
                                    matrix = null;
                                    break;
                                }
                                matrix = te.getName();
                                dest = sampleDirectory.resolve( "matrix.mtx.gz" );
                            } else {
                                continue;
                            }
                            if ( dest.toFile().exists() && dest.toFile().length() == te.getSize() ) {
                                log.info( String.format( "%s: Skipping copy of %s to %s because it already exists and has expected size of %s.",
                                        sample.getGeoAccession(), te.getName(), dest, FileUtils.byteCountToDisplaySize( te.getSize() ) ) );
                                if ( isMexFile( te.getName(), MexFileType.MATRIX ) && barcodes != null && features != null ) {
                                    // same kind of reasoning here that we use in hasSingleCellData(): if we have barcodes,
                                    // features and matrix is already on-disk, we can avoid reading the matrix
                                    break;
                                } else {
                                    continue;
                                }
                            }
                            log.info( String.format( "%s: Copying %s from TAR archive %s to %s...", sample.getGeoAccession(), te.getName(), file, dest ) );
                            PathUtils.createParentDirectories( dest );
                            try ( OutputStream os = openGzippedOutputStream( te.getName(), dest ) ) {
                                copiedBytes += IOUtils.copyLarge( tis, os );
                            } catch ( Exception e ) {
                                if ( isRetryable( i, e ) ) {
                                    // only remove the affected file since we're retrying
                                    log.warn( String.format( "%s: MEX file could not be downloaded successfully, removing %s...", sample.getGeoAccession(), dest ), e );
                                    if ( Files.exists( dest ) ) {
                                        PathUtils.deleteFile( dest );
                                    }
                                    backoff( i );
                                } else {
                                    log.warn( String.format( "%s: MEX files could not be downloaded successfully, removing %s...", sample.getGeoAccession(), sampleDirectory ), e );
                                    PathUtils.deleteDirectory( sampleDirectory );
                                }
                                throw e;
                            }
                        }
                        completed = barcodes != null && features != null && matrix != null;
                        if ( completed ) {
                            if ( copiedBytes > 0 ) {
                                log.info( String.format( "%s: Done copying MEX files from TAR archive (%s in %s @ %.3f MB/s).",
                                        sample.getGeoAccession(),
                                        FileUtils.byteCountToDisplaySize( copiedBytes ), timer,
                                        ( 1000.0 / ( 1000.0 * 1000.0 ) ) * ( copiedBytes / timer.getTime() ) ) );
                            }
                            return;
                        }
                    } catch ( Exception e ) {
                        retrying = isRetryable( i, e );
                        if ( retrying ) {
                            log.info( String.format( "%s: Retrying download of %s...", sample.getGeoAccession(), file ) );
                        } else {
                            if ( i == maxRetries ) {
                                log.error( String.format( "%s: Maximum number of retries reached for %s, raising the last exception.", sample.getGeoAccession(), file ) );
                            }
                            throw e;
                        }
                    } finally {
                        // Because we copied file as we traversed the TAR archive, it's possible that not all expected
                        // files were encountered, or maybe some of them were encountered twice, thus we need to clean
                        // up the directory if that occurs.
                        // If we are retrying, do not remove downloaded files to save some time: missing files might be
                        // downloaded in the next attempt
                        if ( !completed && !retrying && sampleDirectory.toFile().exists() ) {
                            log.warn( String.format( "%s: MEX files are incomplete, removing %s...", sample.getGeoAccession(), sampleDirectory ) );
                            PathUtils.deleteDirectory( sampleDirectory );
                        }
                    }
                }
            }
        }

        throw new NoSingleCellDataFoundException( "No single-cell data was downloaded for " + sample.getGeoAccession() + "." );
    }

    @Override
    public List<String> getAdditionalSupplementaryFiles( GeoSeries series ) {
        return series.getSupplementaryFiles().stream()
                .filter( this::isAdditionalSupplementaryFile )
                .collect( Collectors.toList() );
    }

    @Override
    public List<String> getAdditionalSupplementaryFiles( GeoSample sample ) {
        return sample.getSupplementaryFiles().stream()
                .filter( this::isAdditionalSupplementaryFile )
                .collect( Collectors.toList() );
    }

    private boolean isAdditionalSupplementaryFile( String f ) {
        return !isMexFile( f, MexFileType.GENES )
                && !isMexFile( f, MexFileType.FEATURES )
                && !isMexFile( f, MexFileType.BARCODES )
                && !isMexFile( f, MexFileType.MATRIX )
                // FIXME: the tar might contain additional supplementary files
                && !f.endsWith( ".tar" ) && !f.endsWith( ".tar.gz" )
                && !f.endsWith( "_RAW.tar" );
    }

    @Override
    public SingleCellDataLoader getSingleCellDataLoader( GeoSeries series ) throws NoSingleCellDataFoundException {
        Assert.notNull( downloadDirectory, "A download directory must be set." );

        List<String> sampleNames = new ArrayList<>();
        List<Path> barcodesFiles = new ArrayList<>(),
                featuresFiles = new ArrayList<>(),
                matricesFiles = new ArrayList<>();

        for ( GeoSample sample : series.getSamples() ) {
            Path sampleDir = downloadDirectory.resolve( sample.getGeoAccession() );
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
}
