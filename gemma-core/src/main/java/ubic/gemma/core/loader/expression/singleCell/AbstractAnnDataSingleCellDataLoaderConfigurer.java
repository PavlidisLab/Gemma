package ubic.gemma.core.loader.expression.singleCell;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang.StringUtils;
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

/**
 * Base class for {@link AnnDataSingleCellDataLoader} configurers.
 * <p>
 * This base class provides capabilities for detecting data stored in various columns.
 * @author poirigui
 */
@CommonsLog
public abstract class AbstractAnnDataSingleCellDataLoaderConfigurer implements SingleCellDataLoaderConfigurer<AnnDataSingleCellDataLoader> {

    private static final String[] SAMPLE_NAME_COLUMN_NAME_KEYWORDS = {};

    /**
     * Keywords to look for in column names for detecting a cell type column.
     */
    private static final String[] CELL_TYPE_COLUMN_NAME_KEYWORDS = { "celltype", "cell_type" };

    /**
     * Keywords to look for in column values for detecting an unknown cell type indicator.
     */
    private static final String[] UNKNOWN_CELL_TYPE_INDICATORS = { "UNK", "NA" };

    private static final String[] GENE_COLUMN_NAME_KEYWORDS = {};

    private final Path annDataFile;

    private Path pythonExecutable;

    protected AbstractAnnDataSingleCellDataLoaderConfigurer( Path annDataFile ) {
        this.annDataFile = annDataFile;
    }

    public void setPythonExecutable( Path pythonExecutable ) {
        this.pythonExecutable = pythonExecutable;
    }

    /**
     * Automatically configure a loader for an AnnData file.
     */
    @Override
    public AnnDataSingleCellDataLoader configureLoader() {
        ArrayList<Path> tempFilesToRemove = new ArrayList<Path>();
        Path dataFileToUse;
        try {
            dataFileToUse = transformIfNecessary( tempFilesToRemove );
        } catch ( IOException e ) {
            deleteTemporaryFilesQuietly( tempFilesToRemove );
            throw new RuntimeException( "Error wile attempting to automatically transform " + annDataFile + ".", e );
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
            // first, detect if transposing is necessary
            if ( isTransposed( ad.getObs(), ad.getVar() ) ) {
                // this is typically the case, so check it first
                log.info( "AnnData loader settings were applied on the obs dataframe, the loader will be configured to use the transpose." );
                loader.setTranspose( true );
                applyLoaderSettings( ad.getObs(), loader );
            } else {
                // already has the right orientation, var contains cells/samples
                loader.setTranspose( false );
                applyLoaderSettings( ad.getVar(), loader );
            }
            if ( ad.getRawX() != null && ad.getRawVar() != null ) {
                log.warn( "AnnData contains a 'raw.X' and 'raw.var' groups, using those." );
                loader.setUseRawX( true );
            }
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

    private Path transformIfNecessary( Collection<Path> tempFilesToRemove ) throws IOException {
        Path dataFileToUse = annDataFile;

        // check if rewriting is necessary
        try ( AnnData ad = AnnData.open( dataFileToUse ) ) {
            // ignored
        } catch ( MissingEncodingAttributeException e ) {
            log.warn( "AnnData file " + annDataFile + " is lacking encoding attributes, will rewrite it.", e );
            dataFileToUse = performTransformation( new SingleCellDataRewrite(), dataFileToUse, tempFilesToRemove );
        }

        // check for unraw
        // we only need to unraw if the matrix is transposed, otherwise the loader can use raw.X and raw.var directly
        try ( AnnData ad = AnnData.open( dataFileToUse ) ) {
            if ( ad.getRawX() != null && ad.getRawVar() != null && isTransposeNecessary( ad.getRawX(), ad.getObs(), ad.getRawVar() ) ) {
                // unraw
                log.info( "AnnData file" + annDataFile + " has raw.X and raw.var and needs to be transposed later on, extracting it as the main layer..." );
                dataFileToUse = performTransformation( new SingleCellDataUnraw(), dataFileToUse, tempFilesToRemove );
            }
        }

        // check for transposing
        try ( AnnData ad = AnnData.open( dataFileToUse ) ) {
            // if raw.X is present, we have to rewrite first
            if ( ad.getX() != null && isTransposeNecessary( ad.getX(), ad.getObs(), ad.getVar() ) ) {
                // two scenarios:
                log.info( "AnnData file" + annDataFile + " needs to be transposed on-disk, performing..." );
                dataFileToUse = performTransformation( new SingleCellDataTranspose(), dataFileToUse, tempFilesToRemove );
            }
        }

        return dataFileToUse;
    }

    private Path performTransformation( SingleCellInputOutputFileTransformation transformation, Path dataFileToUse, Collection<Path> tempFilesToRemove ) throws IOException {
        Path tempFile = Files.createTempFile( null, ".h5ad" );
        tempFilesToRemove.add( tempFile );
        if ( transformation instanceof PythonBasedSingleCellDataTransformation ) {
            ( ( PythonBasedSingleCellDataTransformation ) transformation ).setPythonExecutable( pythonExecutable );
        }
        transformation.setInputFile( dataFileToUse, SingleCellDataType.ANNDATA );
        transformation.setOutputFile( tempFile, SingleCellDataType.ANNDATA );
        transformation.perform();
        return tempFile;
    }

    /**
     * Check if transposing on-disk is necessary.
     * @return
     */
    private boolean isTransposeNecessary( Layer X, Dataframe<?> obs, Dataframe<?> var ) {
        if ( X.isSparse() ) {
            // two scenarios:
            // the matrix is in CSR and obs contain sample names or cell types (or var contain genes)
            // the matrix is in CSC and var contain sample names or cell types (or obs contain genes)
            if ( ( X.getSparseMatrix().isCsr() && isTransposed( obs, var ) )
                    || ( X.getSparseMatrix().isCsc() && isTransposed( var, obs ) ) ) {
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

    /**
     * Check if a pair of obs/var is transposed as per what Gemma expects (i.e. obs for genes and var for cells).
     */
    private boolean isTransposed( Dataframe<?> obs, Dataframe<?> var ) {
        return hasSampleNameColumn( obs ) || hasCellIdColumn( obs ) || hasCellTypeColumn( obs ) || hasGenes( var );
    }

    private boolean hasGenes( Dataframe<?> df ) {
        for ( Dataframe.Column<?, ?> column : df ) {
            if ( column.getType().equals( String.class ) && isGeneColumn( ( Dataframe.Column<?, String> ) column ) ) {
                return true;
            }
        }
        return false;
    }

    private boolean hasSampleNameColumn( Dataframe<?> df ) {
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

    private boolean hasCellTypeColumn( Dataframe<?> df ) {
        for ( Dataframe.Column<?, ?> column : df ) {
            if ( column.getType().equals( String.class ) && isCellTypeColumn( ( Dataframe.Column<?, String> ) column ) ) {
                return true;
            }
        }
        return false;
    }

    private void applyLoaderSettings( Dataframe<?> var, AnnDataSingleCellDataLoader loader ) {
        String sampleColumn = null;
        String cellTypeColumn = null;
        String unknownCellTypeIndicator = null;
        for ( Dataframe.Column<?, ?> col : var ) {
            if ( !col.getType().equals( String.class ) ) {
                continue;
            }
            if ( sampleColumn == null && isSampleNameColumn( ( Dataframe.Column<?, String> ) col ) ) {
                log.info( "Detected that " + col + " is the sample name column." );
                sampleColumn = col.getName();
            }
            if ( cellTypeColumn == null && isCellTypeColumn( ( Dataframe.Column<?, String> ) col ) ) {
                log.info( "Detected that " + col + " is the cell type column." );
                cellTypeColumn = col.getName();
                unknownCellTypeIndicator = getUnknownCellTypeIndicator( ( Dataframe.Column<?, String> ) col );
                if ( unknownCellTypeIndicator != null ) {
                    log.info( "Detected that " + col + " uses " + unknownCellTypeIndicator + " as an unknown cell type indicator." );
                }
            }
        }
        if ( sampleColumn != null || cellTypeColumn != null ) {
            if ( sampleColumn != null ) {
                loader.setSampleFactorName( sampleColumn );
            }
            if ( cellTypeColumn != null ) {
                loader.setCellTypeFactorName( cellTypeColumn );
                loader.setUnknownCellTypeIndicator( unknownCellTypeIndicator );
            }
        } else {
            log.warn( "Failed to detect AnnData loader settings, the loader will be left unconfigured" );
        }
    }

    /**
     * Check if the given dataframe column contains cell identifiers.
     */
    protected boolean isCellIdColumn( Dataframe.Column<?, String> column ) {
        return Arrays.stream( SAMPLE_NAME_COLUMN_NAME_KEYWORDS )
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
