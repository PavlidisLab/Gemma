package ubic.gemma.core.loader.expression.singleCell;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang.StringUtils;
import ubic.gemma.core.loader.util.anndata.AnnData;
import ubic.gemma.core.loader.util.anndata.Dataframe;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;

/**
 * Automatically configure an {@link AnnDataSingleCellDataLoader}.
 */
@CommonsLog
public abstract class AbstractAnnDataSingleCellDataLoaderConfigurer implements SingleCellDataLoaderConfigurer<AnnDataSingleCellDataLoader> {

    /**
     * Keywords to look for in column names for detecting a cell type column.
     */
    private static final String[] CELL_TYPE_COLUMN_NAME_KEYWORDS = { "celltype", "cell_type" };

    /**
     * Keywords to look for in column values for detecting an unknown cell type indicator.
     */
    private static final String[] UNKNOWN_CELL_TYPE_INDICATORS = { "UNK", "NA" };

    private final Path annDataFile;

    protected AbstractAnnDataSingleCellDataLoaderConfigurer( Path annDataFile ) {
        this.annDataFile = annDataFile;
    }

    /**
     * Automatically configure a loader for an AnnData file.
     */
    @Override
    public AnnDataSingleCellDataLoader configureLoader() {
        AnnDataSingleCellDataLoader loader = new AnnDataSingleCellDataLoader( annDataFile );
        try ( AnnData ad = AnnData.open( annDataFile ) ) {
            if ( applyLoaderSettings( ad.getObs(), loader ) ) {
                // this is typically the case, so check it first
                log.info( "AnnData loader settings were applied on the obs dataframe, the loader will be configured to use the transpose." );
                loader.setTranspose( true );
            } else if ( applyLoaderSettings( ad.getVar(), loader ) ) {
                loader.setTranspose( false );
            } else {
                log.warn( "Failed to detect AnnData loader settings, the loader will be left unconfigured" );
            }
            if ( ad.getRawX() != null && ad.getRawVar() != null ) {
                log.warn( "AnnData contains a 'raw.X' and 'raw.var' groups, using those." );
                loader.setUseRawX( true );
            }
        } catch ( IOException e ) {
            log.error( "Error while attempting to detect AnnData loader settings from GEO series.", e );
        }
        return loader;
    }

    private boolean applyLoaderSettings( Dataframe<?> df, AnnDataSingleCellDataLoader loader ) {
        String sampleColumn = null;
        String cellTypeColumn = null;
        String unknownCellTypeIndicator = null;
        for ( String col : df.getColumns() ) {
            if ( !df.getColumnType( col ).equals( String.class ) ) {
                continue;
            }
            Set<String> vals = df.getColumn( col, String.class ).uniqueValues();
            if ( sampleColumn == null && isSampleNameColumn( df, col, vals ) ) {
                log.info( "Detected that " + col + " is the sample name column." );
                sampleColumn = col;
            }
            if ( cellTypeColumn == null && isCellTypeColumn( df, col, vals ) ) {
                log.info( "Detected that " + col + " is the cell type column." );
                cellTypeColumn = col;
                unknownCellTypeIndicator = getUnknownCellTypeIndicator( df, col, vals );
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
            return true;
        }
        return false;
    }

    /**
     * Check if a given dataframe column contains sample names.
     */
    protected abstract boolean isSampleNameColumn( Dataframe<?> df, String column, Set<String> vals );

    protected boolean isCellTypeColumn( Dataframe<?> df, String column, Set<String> vals ) {
        return Arrays.stream( CELL_TYPE_COLUMN_NAME_KEYWORDS )
                .anyMatch( kwd -> StringUtils.containsIgnoreCase( column, kwd ) );
    }

    /**
     * Extract the unknown cell type indicator from a set of values.
     */
    @Nullable
    protected String getUnknownCellTypeIndicator( Dataframe<?> df, String column, Set<String> vals ) {
        for ( String indicator : UNKNOWN_CELL_TYPE_INDICATORS ) {
            if ( vals.contains( indicator ) ) {
                return indicator;
            }
        }
        return null;
    }
}
