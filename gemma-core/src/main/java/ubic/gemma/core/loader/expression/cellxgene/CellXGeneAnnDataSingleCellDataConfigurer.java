package ubic.gemma.core.loader.expression.cellxgene;

import lombok.extern.apachecommons.CommonsLog;
import ubic.gemma.core.loader.expression.singleCell.*;
import ubic.gemma.core.loader.expression.singleCell.transform.SingleCellDataSortBySample;
import ubic.gemma.core.loader.expression.singleCell.transform.SingleCellDataTransformationFactory;
import ubic.gemma.core.loader.expression.singleCell.transform.SingleCellDataTranspose;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Configures a {@link AnnDataSingleCellDataLoader} for CELLxGENE datasets.
 *
 * @author poirigui
 */
@CommonsLog
public class CellXGeneAnnDataSingleCellDataConfigurer implements SingleCellDataLoaderConfigurer<AnnDataSingleCellDataLoader> {

    private final Path annDataFile;

    private final SingleCellDataTransformationFactory singleCellDataTransformationFactory;
    private final Path cellXGeneTransposedPath;

    public CellXGeneAnnDataSingleCellDataConfigurer( Path annDataFile, SingleCellDataTransformationFactory singleCellDataTransformationFactory, Path cellXGeneTransposedPath ) {
        this.annDataFile = annDataFile;
        this.singleCellDataTransformationFactory = singleCellDataTransformationFactory;
        this.cellXGeneTransposedPath = cellXGeneTransposedPath;
    }

    public CellXGeneAnnDataSingleCellDataConfigurer( Path annDataFile, SingleCellDataTransformationFactory singleCellDataTransformationFactory ) {
        this.annDataFile = annDataFile;
        this.singleCellDataTransformationFactory = singleCellDataTransformationFactory;
        this.cellXGeneTransposedPath = null;
    }

    @Override
    public AnnDataSingleCellDataLoader configureLoader( SingleCellDataLoaderConfig config ) {
        boolean keepPooledSample = config instanceof CellXGeneAnnDataSingleCellDataLoaderConfig
                && ( ( CellXGeneAnnDataSingleCellDataLoaderConfig ) config ).isKeepPooledSample();
        boolean keepUnknownSample = config instanceof CellXGeneAnnDataSingleCellDataLoaderConfig
                && ( ( CellXGeneAnnDataSingleCellDataLoaderConfig ) config ).isKeepUnknownSample();
        if ( config.isIgnoreDataVectors() ) {
            log.warn( "The skipDataVectors flag is set in the configuration, will not transpose and sort by sample. Reading metadata will work as usual, but loading data will not be supported." );
            CellXGeneAnnDataSingleCellDataLoader loader = new CellXGeneAnnDataSingleCellDataLoader( annDataFile, keepPooledSample, keepUnknownSample );
            loader.setIgnoreDataVectors( true );
            return applyConfig( loader, config, false );
        }

        Path finalSortedFile;
        Set<Path> tempFiles = new HashSet<>();
        AtomicBoolean wasTransposedOnDisk = new AtomicBoolean();
        try {
            finalSortedFile = transformOnDisk( annDataFile, config, tempFiles, wasTransposedOnDisk );
        } catch ( Exception e ) {
            removeTemporaryFilesSilently( tempFiles );
            throw new RuntimeException( "Error wile attempting to automatically transform " + annDataFile + ".", e );
        }
        return applyConfig( new CellXGeneAnnDataSingleCellDataLoader( finalSortedFile, keepPooledSample, keepUnknownSample ) {
            @Override
            public void close() throws IOException {
                try {
                    super.close();
                } finally {
                    removeTemporaryFilesSilently( Collections.singleton( finalSortedFile ) );
                }
            }
        }, config, wasTransposedOnDisk.get() );
    }

    private Path transformOnDisk( Path fileToUse, SingleCellDataLoaderConfig config, Set<Path> tempFiles, AtomicBoolean wasTransposedOnDisk ) throws IOException {
        // CELLxGENE datasets are always transposed (w.r.t. to what Gemma wants)
        // transpose it unless the configuration explicitly requests not to
        boolean transposeOnDisk = !( config instanceof AnnDataSingleCellDataLoaderConfig )
                || !Boolean.FALSE.equals( ( ( AnnDataSingleCellDataLoaderConfig ) config ).getTranspose() );

        if (cellXGeneTransposedPath != null && Files.exists( cellXGeneTransposedPath.resolve( annDataFile.getFileName()) )) {
            return cellXGeneTransposedPath.resolve( annDataFile.getFileName() );
        }

        if ( transposeOnDisk ) {
            log.info( "Transposing " + fileToUse + "..." );
            SingleCellDataTranspose sbs = singleCellDataTransformationFactory
                    .getTransformation( SingleCellDataTranspose.class );
            sbs.setInputFile( fileToUse, SingleCellDataType.ANNDATA );

            fileToUse = singleCellDataTransformationFactory.createTemporaryFile( SingleCellDataType.ANNDATA );
            try {
                sbs.setOutputFile( fileToUse, SingleCellDataType.ANNDATA );
            } finally {
                tempFiles.add( fileToUse );
            }
            sbs.perform();
            wasTransposedOnDisk.set( true );
        }

        // CELLxGENE datasets are usually not sorted by sample
        log.info( "Sorting " + fileToUse + " by sample..." );
        SingleCellDataSortBySample sbs = singleCellDataTransformationFactory
                .getTransformation( SingleCellDataSortBySample.class );
        sbs.setSampleColumnName( "donor_id" );
        sbs.setInputFile( fileToUse, SingleCellDataType.ANNDATA );

        if( cellXGeneTransposedPath == null) {
            fileToUse = singleCellDataTransformationFactory.createTemporaryFile( SingleCellDataType.ANNDATA );
        } else{
            Files.createDirectories( cellXGeneTransposedPath );
            fileToUse = cellXGeneTransposedPath.resolve( annDataFile.getFileName() );
        }

        try {
            sbs.setOutputFile( fileToUse, SingleCellDataType.ANNDATA );
        } finally {
            if (cellXGeneTransposedPath == null){
                tempFiles.add( fileToUse );
            }
        }
        sbs.perform();

        return fileToUse;
    }

    private AnnDataSingleCellDataLoader applyConfig( AnnDataSingleCellDataLoader loader, SingleCellDataLoaderConfig config, boolean wasTransposedOnDisk ) {
        if ( config instanceof AnnDataSingleCellDataLoaderConfig ) {
            AnnDataSingleCellDataLoaderConfig annDataConfig = ( ( AnnDataSingleCellDataLoaderConfig ) config );
            if ( annDataConfig.getTranspose() != null ) {
                // if the data was already transposed on disk, do not transpose again
                if ( wasTransposedOnDisk ) {
                    loader.setTranspose( !annDataConfig.getTranspose() );
                } else {
                    throw new IllegalStateException( "Data was transposed on-disk, but the configuration is explicitly requesting not to transpose data." );
                }
            } else if ( wasTransposedOnDisk ) {
                loader.setTranspose( false );
            }
            if ( annDataConfig.getSampleFactorName() != null ) {
                loader.setSampleFactorName( annDataConfig.getSampleFactorName() );
            }
            if ( annDataConfig.getCellTypeFactorName() != null ) {
                loader.setCellTypeFactorName( annDataConfig.getCellTypeFactorName() );
            }
            if ( annDataConfig.getCellTypeUriFactorName() != null ) {
                loader.setCellTypeUriFactorName( annDataConfig.getCellTypeUriFactorName() );
            }
        } else if ( wasTransposedOnDisk ) {
            loader.setTranspose( false );
        }
        return loader;
    }

    private void removeTemporaryFilesSilently( Set<Path> tempFiles ) {
        for ( Path p : tempFiles ) {
            try {
                Files.deleteIfExists( p );
            } catch ( IOException e ) {
                log.warn( "Could not delete temporary file: " + p, e );
            }
        }
    }
}
