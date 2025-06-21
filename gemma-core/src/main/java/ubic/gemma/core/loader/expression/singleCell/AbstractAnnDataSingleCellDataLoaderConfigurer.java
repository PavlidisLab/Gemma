package ubic.gemma.core.loader.expression.singleCell;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.StringUtils;
import ubic.gemma.core.loader.expression.singleCell.transform.*;
import ubic.gemma.core.loader.util.anndata.AnnData;
import ubic.gemma.core.loader.util.anndata.Dataframe;
import ubic.gemma.core.loader.util.anndata.Layer;
import ubic.gemma.core.loader.util.anndata.MissingEncodingAttributeException;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Base class for {@link AnnDataSingleCellDataLoader} configurers.
 * <p>
 * This base class provides capabilities for detecting data stored in various columns.
 * @author poirigui
 */
@CommonsLog
public abstract class AbstractAnnDataSingleCellDataLoaderConfigurer implements SingleCellDataLoaderConfigurer<AnnDataSingleCellDataLoader> {

    /**
     * TODO
     */
    private static final String[] SAMPLE_NAME_COLUMN_NAME_KEYWORDS = {};

    /**
     * TODO
     */
    public static final String[] CELL_ID_COLUMN_NAME_KEYWORDS = {};

    /**
     * Keywords to look for in column names for detecting a cell type column.
     */
    private static final String[] CELL_TYPE_COLUMN_NAME_KEYWORDS = { "celltype", "cell_type" };

    /**
     * Keywords to look for in column values for detecting an unknown cell type indicator.
     */
    private static final String[] UNKNOWN_CELL_TYPE_INDICATORS = { "UNK", "NA" };

    /**
     * TODO
     */
    private static final String[] GENE_COLUMN_NAME_KEYWORDS = {};

    private final Path annDataFile;

    // both are necessary to perform on-disk transformations
    @Nullable
    private Path pythonExecutable;
    @Nullable
    private Path scratchDir;

    protected AbstractAnnDataSingleCellDataLoaderConfigurer( Path annDataFile ) {
        this.annDataFile = annDataFile;
    }

    /**
     * Set the path to a Python executable. If null, no transformation will be performed on the AnnData file.
     */
    public void setPythonExecutable( Path pythonExecutable ) {
        this.pythonExecutable = pythonExecutable;
    }

    /**
     * Set the path to a scratch directory to use for on-disk transformations. If null, no transformation will be
     * performed on the AnnData file.
     */
    public void setScratchDir( Path scratchDir ) {
        this.scratchDir = scratchDir;
    }

    /**
     * Automatically configure a loader for an AnnData file.
     */
    @Override
    public AnnDataSingleCellDataLoader configureLoader( SingleCellDataLoaderConfig config ) {
        ArrayList<Path> tempFilesToRemove = new ArrayList<Path>();
        AtomicBoolean wasTransposedOnDisk = new AtomicBoolean( false );
        Path dataFileToUse;
        if ( pythonExecutable != null && scratchDir != null ) {
            try {
                dataFileToUse = transformIfNecessary( tempFilesToRemove, pythonExecutable, scratchDir, config, wasTransposedOnDisk );
            } catch ( Exception e ) {
                deleteTemporaryFilesQuietly( tempFilesToRemove );
                throw new RuntimeException( "Error wile attempting to automatically transform " + annDataFile + ".", e );
            }
        } else {
            log.warn( "No Python executable or scratch directory is set, will not perform any transformation on " + annDataFile + "." );
            dataFileToUse = annDataFile;
        }
        AnnDataSingleCellDataLoader loader = new AnnDataSingleCellDataLoader( dataFileToUse ) {
            @Override
            public void close() throws IOException {
                try {
                    super.close();
                } finally {
                    deleteTemporaryFilesQuietly( tempFilesToRemove );
                }
            }
        };
        try ( AnnData ad = AnnData.open( dataFileToUse ) ) {
            boolean transpose = configureTranspose( loader, ad, config, wasTransposedOnDisk.get() );
            if ( transpose ) {
                configureSampleAndCellTypeColumns( ad.getObs(), loader, config );
            } else {
                configureSampleAndCellTypeColumns( ad.getVar(), loader, config );
            }
            configureRawX( loader, ad, config );
        } catch ( Exception e ) {
            try {
                loader.close();
            } catch ( Exception ex ) {
                log.error( "Failed to close loader, another exception was caught so this will not be raised.", ex );
            }
            if ( e instanceof RuntimeException ) {
                throw ( RuntimeException ) e;
            } else {
                throw new RuntimeException( "Error while attempting to configure AnnData loader.", e );
            }
        }
        return loader;
    }

    private boolean configureTranspose( AnnDataSingleCellDataLoader loader, AnnData ad, SingleCellDataLoaderConfig config, boolean wasTransposedOnDisk ) {
        // then detect if transposing is necessary
        if ( isTransposed( ad.getObs(), ad.getVar(), config ) ) {
            if ( wasTransposedOnDisk ) {
                log.info( "AnnData object was already transposed on-disk, the loader will be configured to use it as-is." );
                loader.setTranspose( false );
                return false;
            }
            // this is typically the case, so check it first
            log.info( "AnnData loader settings were applied on the obs dataframe, the loader will be configured to use the transpose." );
            loader.setTranspose( true );
            return true;
        } else {
            // already has the right orientation, var contains cells/samples
            log.info( "AnnData object has the correct orientation, no transpose is needed." );
            loader.setTranspose( false );
            return false;
        }
    }

    private void configureSampleAndCellTypeColumns( Dataframe<?> var, AnnDataSingleCellDataLoader loader, SingleCellDataLoaderConfig config ) {
        String sampleColumn = null;
        String cellTypeColumn = null;
        String unknownCellTypeIndicator = null;
        boolean ignoreCellTypeColumn = false;
        if ( config instanceof AnnDataSingleCellDataLoaderConfig ) {
            AnnDataSingleCellDataLoaderConfig annDataConfig = ( AnnDataSingleCellDataLoaderConfig ) config;
            sampleColumn = annDataConfig.getSampleFactorName();
            cellTypeColumn = annDataConfig.getCellTypeFactorName();
            unknownCellTypeIndicator = annDataConfig.getUnknownCellTypeIndicator();
            ignoreCellTypeColumn = annDataConfig.isIgnoreCellTypeFactor();
        }
        if ( sampleColumn == null || ( cellTypeColumn == null && !ignoreCellTypeColumn ) ) {
            log.info( "Automatically detecting sample and/or cell type columns in AnnData file " + annDataFile + "..." );
            for ( Dataframe.Column<?, ?> col : var ) {
                if ( !col.getType().equals( String.class ) ) {
                    continue;
                }
                if ( sampleColumn == null && isSampleNameColumn( ( Dataframe.Column<?, String> ) col ) ) {
                    log.info( "Detected that '" + col.getName() + "' is the sample name column." );
                    sampleColumn = col.getName();
                }
                if ( cellTypeColumn == null && !ignoreCellTypeColumn && isCellTypeColumn( ( Dataframe.Column<?, String> ) col ) ) {
                    log.info( "Detected that '" + col.getName() + "' is the cell type column." );
                    cellTypeColumn = col.getName();
                    if ( unknownCellTypeIndicator == null ) {
                        unknownCellTypeIndicator = getUnknownCellTypeIndicator( ( Dataframe.Column<?, String> ) col );
                        if ( unknownCellTypeIndicator != null ) {
                            log.info( "Detected that '" + col.getName() + "' uses '" + unknownCellTypeIndicator + "' as an unknown cell type indicator." );
                        }
                    }
                }
            }
            if ( sampleColumn == null ) {
                log.warn( "Failed to detect a sample name column in AnnData file " + annDataFile + "." );
            }
            if ( cellTypeColumn == null && !ignoreCellTypeColumn ) {
                log.warn( "Failed to detect a cell type column in AnnData file " + annDataFile + "." );
            }
        }
        if ( sampleColumn != null ) {
            loader.setSampleFactorName( sampleColumn );
        }
        if ( cellTypeColumn != null ) {
            loader.setCellTypeFactorName( cellTypeColumn );
        }
        loader.setIgnoreCellTypeFactor( ignoreCellTypeColumn );
    }

    private void configureRawX( AnnDataSingleCellDataLoader loader, AnnData ad, SingleCellDataLoaderConfig config ) {
        if ( config instanceof AnnDataSingleCellDataLoaderConfig && ( ( AnnDataSingleCellDataLoaderConfig ) config ).getUseRawX() != null ) {
            loader.setUseRawX( ( ( AnnDataSingleCellDataLoaderConfig ) config ).getUseRawX() );
        } else if ( ad.getRawX() != null && ad.getRawVar() != null ) {
            log.warn( "AnnData contains a 'raw.X' and 'raw.var' groups, using those." );
            loader.setUseRawX( true );
        }
    }

    private void deleteTemporaryFilesQuietly( ArrayList<Path> tempFilesToRemove ) {
        for ( Path path : tempFilesToRemove ) {
            try {
                log.info( "Removing temporary file " + path + "..." );
                Files.delete( path );
            } catch ( IOException e ) {
                log.error( "Failed to delete " + path + ".", e );
            }
        }
    }

    private Path transformIfNecessary( Collection<Path> tempFilesToRemove, Path pythonExecutable, Path scratchDir, SingleCellDataLoaderConfig config, AtomicBoolean wasTransposedOnDisk ) throws IOException {
        Path dataFileToUse = annDataFile;

        // check if rewriting is necessary
        try ( AnnData ad = AnnData.open( dataFileToUse ) ) {
            // ignored
        } catch ( MissingEncodingAttributeException e ) {
            log.warn( "AnnData file " + annDataFile + " is lacking encoding attributes, will rewrite it.", e );
            dataFileToUse = performTransformation( new SingleCellDataRewrite(), dataFileToUse, tempFilesToRemove, pythonExecutable, scratchDir );
        }

        // check for unraw
        try ( AnnData ad = AnnData.open( dataFileToUse ) ) {
            if ( isUnrawXNecessary( ad, config ) ) {
                log.info( "AnnData file" + annDataFile + " has raw.X and raw.var and needs to be transposed later on, extracting it as the main layer..." );
                dataFileToUse = performTransformation( new SingleCellDataUnraw(), dataFileToUse, tempFilesToRemove, pythonExecutable, scratchDir );
            }
        }

        // check for transposing
        try ( AnnData ad = AnnData.open( dataFileToUse ) ) {
            // if raw.X is present, we have to rewrite first
            if ( ad.getX() != null && isTransposeOnDiskNecessary( ad.getX(), ad.getObs(), ad.getVar(), config ) ) {
                log.info( "AnnData file" + annDataFile + " needs to be transposed on-disk, performing..." );
                dataFileToUse = performTransformation( new SingleCellDataTranspose(), dataFileToUse, tempFilesToRemove, pythonExecutable, scratchDir );
                wasTransposedOnDisk.set( true );
            }
        }

        return dataFileToUse;
    }

    /**
     * Check if unraw is necessary.
     */
    private boolean isUnrawXNecessary( AnnData ad, SingleCellDataLoaderConfig config ) {
        if ( config instanceof AnnDataSingleCellDataLoaderConfig
                && ( ( AnnDataSingleCellDataLoaderConfig ) config ).getUseRawX() != null ) {
            // if the user explicitly wants to use raw.X or X, we don't need to unraw
            return false;
        }
        //  We only need to unraw if the matrix is transposed on-disk, otherwise the loader can use raw.X and raw.var directly
        return ad.getRawX() != null && ad.getRawVar() != null && isTransposeOnDiskNecessary( ad.getRawX(), ad.getObs(), ad.getRawVar(), config );
    }

    /**
     * Check if transposing on-disk is necessary.
     * <p>
     * This happens when the matrix layout does not allow retrieving gene vectors efficiently. Transposing on-disk is
     * unnecessary for dense matrices.
     */
    private boolean isTransposeOnDiskNecessary( Layer X, Dataframe<?> obs, Dataframe<?> var, SingleCellDataLoaderConfig config ) {
        if ( X.isSparse() ) {
            // two scenarios:
            // the matrix is in CSR and obs contain sample names or cell types (or var contain genes)
            // the matrix is in CSC and var contain sample names or cell types (or obs contain genes)
            if ( ( X.getSparseMatrix().isCsr() && isTransposed( obs, var, config ) )
                    || ( X.getSparseMatrix().isCsc() && isTransposed( var, obs, config ) ) ) {
                return true;
            } else {
                // matrix already has the right orientation or can be transposed via setTranspose()
                return false;
            }
        } else {
            // we don't support dense matrix for now, but if we did, transposing would not be necessary
            return false;
        }
    }

    private Path performTransformation( SingleCellInputOutputFileTransformation transformation, Path dataFileToUse, Collection<Path> tempFilesToRemove, Path pythonExecutable, Path scratchDir ) throws IOException {
        Path tempFile = Files.createTempFile( scratchDir, null, ".h5ad" );
        tempFilesToRemove.add( tempFile );
        if ( transformation instanceof PythonBasedSingleCellDataTransformation ) {
            ( ( PythonBasedSingleCellDataTransformation ) transformation )
                    .setPythonExecutable( pythonExecutable );
        }
        transformation.setInputFile( dataFileToUse, SingleCellDataType.ANNDATA );
        transformation.setOutputFile( tempFile, SingleCellDataType.ANNDATA );
        transformation.perform();
        return tempFile;
    }

    /**
     * Check if a pair of obs/var is transposed as per what Gemma expects (i.e. obs for genes and var for cells).
     */
    private boolean isTransposed( Dataframe<?> obs, Dataframe<?> var, SingleCellDataLoaderConfig config ) {
        if ( config instanceof AnnDataSingleCellDataLoaderConfig && ( ( AnnDataSingleCellDataLoaderConfig ) config ).getTranspose() != null ) {
            // if the config says so, we'll trust it
            return ( ( AnnDataSingleCellDataLoaderConfig ) config ).getTranspose();
        }
        return hasSampleNameColumn( obs, config ) || hasCellIdColumn( obs ) || hasCellTypeColumn( obs, config ) || hasGenes( var );
    }

    private boolean hasGenes( Dataframe<?> df ) {
        for ( Dataframe.Column<?, ?> column : df ) {
            if ( column.getType().equals( String.class ) && isGeneColumn( ( Dataframe.Column<?, String> ) column ) ) {
                return true;
            }
        }
        return false;
    }

    private boolean hasSampleNameColumn( Dataframe<?> df, SingleCellDataLoaderConfig config ) {
        if ( config instanceof AnnDataSingleCellDataLoaderConfig ) {
            String sampleFactorName;
            if ( ( sampleFactorName = ( ( AnnDataSingleCellDataLoaderConfig ) config ).getSampleFactorName() ) != null ) {
                return df.getColumns().contains( sampleFactorName );
            }
        }
        for ( Dataframe.Column<?, ?> column : df ) {
            if ( column.getType().equals( String.class ) && isSampleNameColumn( ( Dataframe.Column<?, String> ) column ) ) {
                return true;
            }
        }
        return false;
    }

    private boolean hasCellIdColumn( Dataframe<?> df ) {
        for ( Dataframe.Column<?, ?> column : df ) {
            if ( column.getType().equals( String.class ) && isCellIdColumn( ( Dataframe.Column<?, String> ) column ) ) {
                return true;
            }
        }
        return false;
    }

    private boolean hasCellTypeColumn( Dataframe<?> df, SingleCellDataLoaderConfig config ) {
        if ( config instanceof AnnDataSingleCellDataLoaderConfig ) {
            String cellTypeFactorName;
            if ( ( cellTypeFactorName = ( ( AnnDataSingleCellDataLoaderConfig ) config ).getCellTypeFactorName() ) != null ) {
                return df.getColumns().contains( cellTypeFactorName );
            }
        }
        for ( Dataframe.Column<?, ?> column : df ) {
            if ( column.getType().equals( String.class ) && isCellTypeColumn( ( Dataframe.Column<?, String> ) column ) ) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if the given dataframe column contains cell identifiers.
     */
    protected boolean isCellIdColumn( Dataframe.Column<?, String> column ) {
        return Arrays.stream( CELL_ID_COLUMN_NAME_KEYWORDS )
                .anyMatch( kwd -> StringUtils.containsIgnoreCase( column.getName(), kwd ) );
    }

    /**
     * Check if a given dataframe column contains sample names.
     */
    protected boolean isSampleNameColumn( Dataframe.Column<?, String> column ) {
        return Arrays.stream( SAMPLE_NAME_COLUMN_NAME_KEYWORDS )
                .anyMatch( kwd -> StringUtils.containsIgnoreCase( column.getName(), kwd ) );
    }

    /**
     * Check if a given column contains cell types.
     */
    protected boolean isCellTypeColumn( Dataframe.Column<?, String> column ) {
        return Arrays.stream( CELL_TYPE_COLUMN_NAME_KEYWORDS )
                .anyMatch( kwd -> StringUtils.containsIgnoreCase( column.getName(), kwd ) );
    }

    /**
     * Extract the unknown cell type indicator from a set of values.
     */
    @Nullable
    protected String getUnknownCellTypeIndicator( Dataframe.Column<?, String> column ) {
        Set<String> vals = column.uniqueValues();
        for ( String indicator : UNKNOWN_CELL_TYPE_INDICATORS ) {
            if ( vals.contains( indicator ) ) {
                return indicator;
            }
        }
        return null;
    }

    /**
     * Check if a given column contains gene identifiers.
     */
    protected boolean isGeneColumn( Dataframe.Column<?, String> column ) {
        return Arrays.stream( GENE_COLUMN_NAME_KEYWORDS )
                .anyMatch( kwd -> StringUtils.containsIgnoreCase( column.getName(), kwd ) );
    }
}
