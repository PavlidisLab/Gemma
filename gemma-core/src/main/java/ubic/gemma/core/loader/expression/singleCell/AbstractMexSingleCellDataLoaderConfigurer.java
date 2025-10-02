package ubic.gemma.core.loader.expression.singleCell;

import no.uib.cipr.matrix.io.MatrixVectorReader;
import no.uib.cipr.matrix.sparse.CompRowMatrix;
import org.apache.commons.io.file.PathUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.Assert;
import ubic.gemma.core.loader.expression.singleCell.transform.SingleCell10xMexFilter;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.GZIPInputStream;

import static ubic.gemma.core.loader.util.MatrixMarketUtils.getNonEmptyColumns;
import static ubic.gemma.core.loader.util.MatrixMarketUtils.readMatrixMarketFromPath;
import static ubic.gemma.core.util.concurrent.FutureUtils.parallelMapRange;

public abstract class AbstractMexSingleCellDataLoaderConfigurer implements SingleCellDataLoaderConfigurer<MexSingleCellDataLoader> {

    protected final Log log = LogFactory.getLog( getClass() );

    @Nullable
    private final Path cellRangerPrefix;

    /**
     *
     * @param cellRangerPrefix path to an installation of Cell Ranger, required if 10x filtering is deemed necessary,
     * {@link MexSingleCellDataLoaderConfig#getApply10xFilter()}.
     */
    protected AbstractMexSingleCellDataLoaderConfigurer( @Nullable Path cellRangerPrefix ) {
        this.cellRangerPrefix = cellRangerPrefix;
    }

    @Override
    public MexSingleCellDataLoader configureLoader( SingleCellDataLoaderConfig config ) {
        List<String> sampleNames = getSampleNames();
        List<Path> sampleDirs = getSampleDirs();

        // not all samples might be used, so these arrays keep track of the used ones
        List<String> usedSampleNames = new ArrayList<>();
        List<Path> usedSampleDirs = new ArrayList<>();
        List<Path> barcodeFiles = new ArrayList<>();
        List<Path> genesFiles = new ArrayList<>();
        List<Path> matrixFiles = new ArrayList<>();

        for ( int i = 0; i < sampleDirs.size(); i++ ) {
            String sampleName = sampleNames.get( i );
            Path sampleDir = sampleDirs.get( i );
            if ( !Files.exists( sampleDir ) ) {
                String m2 = "Sample directory " + sampleDir + " for " + sampleName + " does not exist.";
                if ( config.isIgnoreSamplesLackingData() ) {
                    log.warn( m2 );
                    continue;
                } else {
                    throw new IllegalStateException( m2 + " You can set ignoreSamplesLackingData to ignore this error." );
                }
            }
            Path b = sampleDir.resolve( "barcodes.tsv.gz" );
            Path f = sampleDir.resolve( "features.tsv.gz" );
            Path m = sampleDir.resolve( "matrix.mtx.gz" );
            if ( Files.exists( b ) && Files.exists( f ) && Files.exists( m ) ) {
                usedSampleNames.add( sampleName );
                usedSampleDirs.add( sampleDir );
                barcodeFiles.add( b );
                genesFiles.add( f );
                matrixFiles.add( m );
            } else {
                String m2 = "Expected MEX files are missing in " + sampleDir + ".";
                if ( config.isIgnoreSamplesLackingData() ) {
                    log.warn( m2 );
                } else {
                    throw new IllegalStateException( m2 + " You can set ignoreSamplesLackingData to ignore this error." );
                }
            }
        }

        MexSingleCellDataLoader loader;
        boolean apply10xFilter;
        if ( config.isSkipTransformations() ) {
            log.warn( "The skipTransformations flag is set in the configuration, will not check if filtering 10x MEX data is necessary." );
            apply10xFilter = false;
        } else {
            if ( config instanceof MexSingleCellDataLoaderConfig ) {
                MexSingleCellDataLoaderConfig mexConfig = ( ( MexSingleCellDataLoaderConfig ) config );
                if ( mexConfig.getApply10xFilter() != null ) {
                    apply10xFilter = mexConfig.getApply10xFilter();
                } else {
                    apply10xFilter = detectUnfiltered10xData( usedSampleNames, usedSampleDirs );
                }
            } else {
                apply10xFilter = detectUnfiltered10xData( usedSampleNames, usedSampleDirs );
            }
        }
        if ( apply10xFilter ) {
            loader = createFiltered10xMexLoader( usedSampleNames, usedSampleDirs, config );
        } else {
            loader = new MexSingleCellDataLoader( usedSampleNames, barcodeFiles, genesFiles, matrixFiles );
        }

        if ( config.isSkipTransformations() ) {
            log.warn( "The skipTransformations flag is set in the configuration, will not discard empty cells in MEX data." );
            loader.setDiscardEmptyCells( false );
        }

        if ( config instanceof MexSingleCellDataLoaderConfig ) {
            MexSingleCellDataLoaderConfig mexConfig = ( MexSingleCellDataLoaderConfig ) config;
            loader.setAllowMappingDesignElementsToGeneSymbols( mexConfig.isAllowMappingDesignElementsToGeneSymbols() );
            loader.setUseDoublePrecision( ( mexConfig.isUseDoublePrecision() ) );
        }

        return loader;
    }

    protected abstract List<String> getSampleNames();

    protected abstract List<Path> getSampleDirs();

    private boolean detectUnfiltered10xData( List<String> sampleNames, List<Path> sampleDirs ) {
        for ( int i = 0; i < sampleNames.size(); i++ ) {
            String sampleName = sampleNames.get( i );
            if ( detectUnfiltered10xData( sampleName, sampleDirs.get( i ) ) ) {
                return true;
            }
        }
        return false;
    }

    /**
     * Detect if the data is unfiltered 10x MEX data.
     */
    private boolean detectUnfiltered10xData( String sampleName, Path sampleDir ) {
        return detect10x( sampleName, sampleDir ) && detectUnfiltered( sampleName, sampleDir );
    }

    /**
     * Detect if a MEX dataset is using the 10x Chromium Sequencing platform.
     */
    protected boolean detect10x( String sampleName, Path sampleDir ) {
        Path mexFile = sampleDir.resolve( "matrix.mtx.gz" );
        if ( !Files.exists( mexFile ) ) {
            log.warn( sampleName + ": " + mexFile + " does not exist, cannot use it to check if the data is from a 10x Chromium Sequencing platform." );
            return false;
        }
        String[] comments;
        try ( MatrixVectorReader reader = new MatrixVectorReader( new InputStreamReader( new GZIPInputStream( Files.newInputStream( mexFile ) ) ) ) ) {
            reader.readMatrixInfo();
            comments = reader.readComments();
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
        // The comment is in the following form:
        // %metadata_json: {"software_version": "Cell Ranger cellranger-7.1.0", "format_version": 2}
        for ( String comment : comments ) {
            if ( StringUtils.containsAny( comment, "Cell Ranger", "cellranger" ) ) {
                return true;
            }
        }
        return false;
    }

    /**
     * Detect if a MEX dataset is unfiltered.
     */
    protected boolean detectUnfiltered( String sampleName, Path sampleDir ) {
        Path matrixFile = sampleDir.resolve( "matrix.mtx.gz" );
        CompRowMatrix matrix;
        try ( MatrixVectorReader mvr = readMatrixMarketFromPath( matrixFile ) ) {
            log.info( "Reading " + matrixFile + "..." );
            matrix = new CompRowMatrix( mvr );
        } catch ( IOException e ) {
            throw new RuntimeException( "Failed to read " + matrixFile + ": " + ExceptionUtils.getRootCauseMessage( e ), e );
        }
        int[] nonEmptyCellIndices;
        try {
            nonEmptyCellIndices = getNonEmptyColumns( matrixFile );
        } catch ( IOException e ) {
            throw new RuntimeException( "Failed to read " + matrixFile + ": " + ExceptionUtils.getRootCauseMessage( e ), e );
        }
        return nonEmptyCellIndices.length < matrix.numColumns();
    }

    /**
     * Detect the genome used by a 10x dataset.
     */
    protected abstract String detect10xGenome( String sampleName, Path sampleDir );

    /**
     * Detect the chemistry used by a 10x dataset.
     */
    @Nullable
    protected abstract String detect10xChemistry( String sampleName, Path sampleDir );

    /**
     * Create a MEX loader with filtered data.
     */
    private MexSingleCellDataLoader createFiltered10xMexLoader( List<String> usedSampleNames, List<Path> sampleDirs, SingleCellDataLoaderConfig config ) {
        Assert.notNull( cellRangerPrefix, "A Cell Ranger prefix must be configured to appy the 10x filter." );
        List<Path> filteredBarcodeFiles = new ArrayList<>( usedSampleNames.size() );
        List<Path> filteredGenesFiles = new ArrayList<>( usedSampleNames.size() );
        List<Path> filteredMatrixFiles = new ArrayList<>( usedSampleNames.size() );
        List<Path> sampleDirsToCleanup = Collections.synchronizedList( new ArrayList<>( usedSampleNames.size() ) );

        try {
            if ( config.getTransformExecutor() != null ) {
                List<Path> filteredSampleDirs = parallelMapRange( i -> {
                    String sampleName = usedSampleNames.get( i );
                    Path sampleDir = sampleDirs.get( i );
                    try {
                        return filter10xSample( sampleName, sampleDir, sampleDirsToCleanup, config );
                    } catch ( IOException e ) {
                        throw new RuntimeException( e );
                    }
                }, 0, usedSampleNames.size(), config.getTransformExecutor(), true );
                for ( Path filteredSampleDir : filteredSampleDirs ) {
                    filteredBarcodeFiles.add( filteredSampleDir.resolve( "barcodes.tsv.gz" ) );
                    filteredGenesFiles.add( filteredSampleDir.resolve( "features.tsv.gz" ) );
                    filteredMatrixFiles.add( filteredSampleDir.resolve( "matrix.mtx.gz" ) );
                }
            } else {
                log.warn( "Transforming 10x single-cell data serially, you should specify executor to speed up the process (i.e. by passing -transformThreads/--transform-threads from the CLI)." );
                for ( int i = 0; i < sampleDirs.size(); i++ ) {
                    String sampleName = usedSampleNames.get( i );
                    Path sampleDir = sampleDirs.get( i );
                    Path filteredSampleDir = filter10xSample( sampleName, sampleDir, sampleDirsToCleanup, config );
                    filteredBarcodeFiles.add( filteredSampleDir.resolve( "barcodes.tsv.gz" ) );
                    filteredGenesFiles.add( filteredSampleDir.resolve( "features.tsv.gz" ) );
                    filteredMatrixFiles.add( filteredSampleDir.resolve( "matrix.mtx.gz" ) );
                }
            }
        } catch ( Exception e ) {
            try {
                cleanupFiltered10xMexData( sampleDirsToCleanup );
            } catch ( IOException ex ) {
                e.addSuppressed( ex );
            }
            throw new RuntimeException( e );
        }

        return new MexSingleCellDataLoader( usedSampleNames, filteredBarcodeFiles, filteredGenesFiles, filteredMatrixFiles ) {
            @Override
            public void close() throws IOException {
                try {
                    super.close();
                } finally {
                    cleanupFiltered10xMexData( sampleDirsToCleanup );
                }
            }
        };
    }

    private Path filter10xSample( String sampleName, Path sampleDir, List<Path> sampleDirsToCleanup, SingleCellDataLoaderConfig config ) throws IOException {
        Assert.notNull( cellRangerPrefix );
        Path filteredSampleDir;
        if ( ( config instanceof MexSingleCellDataLoaderConfig && Boolean.TRUE.equals( ( ( MexSingleCellDataLoaderConfig ) config ).getApply10xFilter() ) )
                || detectUnfiltered10xData( sampleName, sampleDir ) ) {
            filteredSampleDir = Files.createTempDirectory( sampleDir.getFileName().toString() );
            sampleDirsToCleanup.add( filteredSampleDir );
            SingleCell10xMexFilter filter = new SingleCell10xMexFilter();
            filter.setCellRangerPrefix( cellRangerPrefix );
            filter.setInputFile( sampleDir, SingleCellDataType.MEX );
            filter.setOutputFile( filteredSampleDir, SingleCellDataType.MEX );
            filter.setGenome( detect10xGenome( sampleName, sampleDir ) );
            String chemistry;
            if ( config instanceof MexSingleCellDataLoaderConfig ) {
                if ( ( ( MexSingleCellDataLoaderConfig ) config ).getUse10xChemistry() != null ) {
                    chemistry = ( ( MexSingleCellDataLoaderConfig ) config ).getUse10xChemistry();
                } else {
                    chemistry = detect10xChemistry( sampleName, sampleDir );
                }
            } else {
                chemistry = detect10xChemistry( sampleName, sampleDir );
            }
            filter.setChemistry( chemistry );
            filter.perform();
        } else {
            log.info( sampleDir + " does not appear to be unfiltered 10x data, using as-is." );
            filteredSampleDir = sampleDir;
        }
        return filteredSampleDir;
    }

    /**
     * Remove filtered MEX data.
     */
    private void cleanupFiltered10xMexData( List<Path> filteredSampleDirs ) throws IOException {
        IOException firstException = null;
        for ( Path filteredSampleDir : filteredSampleDirs ) {
            try {
                PathUtils.delete( filteredSampleDir );
            } catch ( IOException e ) {
                if ( firstException == null ) {
                    firstException = e;
                } else {
                    firstException.addSuppressed( e );
                }
            }
        }
        if ( firstException != null ) {
            throw firstException;
        }
    }
}
