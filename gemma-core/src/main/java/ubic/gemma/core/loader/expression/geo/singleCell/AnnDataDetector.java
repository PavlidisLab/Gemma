package ubic.gemma.core.loader.expression.geo.singleCell;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang.StringUtils;
import ubic.gemma.core.loader.expression.geo.model.GeoSample;
import ubic.gemma.core.loader.expression.geo.model.GeoSeries;
import ubic.gemma.core.loader.expression.singleCell.AnnDataSingleCellDataLoader;
import ubic.gemma.core.loader.expression.singleCell.SingleCellDataLoader;
import ubic.gemma.core.loader.util.anndata.AnnData;
import ubic.gemma.core.loader.util.anndata.Dataframe;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Detects AnnData in GEO series.
 * <p>
 * This detector has additional heuristics for detecting sample names.
 * @author poirigui
 */
@CommonsLog
public class AnnDataDetector extends AbstractSingleH5FileInSeriesSingleCellDetector implements SingleCellDetector {

    /**
     * Keywords to look for in column names for detecting a cell type column.
     */
    private static final String[] CELL_TYPE_COLUMN_NAME_KEYWORDS = { "celltype", "cell_type" };

    /**
     * Keywords to look for in column values for detecting an unknown cell type indicator.
     */
    private static final String[] UNKNOWN_CELL_TYPE_INDICATORS = { "UNK", "NA" };

    private final GeoSampleToSampleNameMatcher matcher = new GeoSampleToSampleNameMatcher();

    public AnnDataDetector() {
        super( "AnnData", ".h5ad" );
    }

    @Override
    protected boolean accepts( String supplementaryFile ) {
        return super.accepts( supplementaryFile ) || supplementaryFile.endsWith( ".h5ad.h5" ) || supplementaryFile.endsWith( ".h5ad.h5.gz" );
    }

    @Override
    public SingleCellDataLoader getSingleCellDataLoader( GeoSeries series ) throws NoSingleCellDataFoundException {
        Path annDataFile = getDest( series );
        if ( Files.exists( annDataFile ) ) {
            AnnDataSingleCellDataLoader loader = new AnnDataSingleCellDataLoader( annDataFile );
            try ( AnnData ad = AnnData.open( annDataFile ) ) {
                if ( applyLoaderSettings( series, ad.getObs(), loader ) ) {
                    // this is typically the case, so check it first
                    log.info( "AnnData loader settings were applied on the obs dataframe, the loader will be configured to use the transpose." );
                    loader.setTranspose( true );
                } else if ( applyLoaderSettings( series, ad.getVar(), loader ) ) {
                    loader.setTranspose( false );
                } else {
                    log.warn( "Failed to detect AnnData loader settings from GEO series." );
                }
            } catch ( IOException e ) {
                log.error( "Error while attempting to detect AnnData loader settings from GEO series.", e );
            }
            return loader;
        }
        throw new NoSingleCellDataFoundException( "Could not find " + annDataFile + " for " + series.getGeoAccession() );
    }

    private boolean applyLoaderSettings( GeoSeries series, Dataframe<?> ad, AnnDataSingleCellDataLoader loader ) {
        String sampleColumn = null;
        String cellTypeColumn = null;
        String unknownCellTypeIndicator = null;
        for ( String col : ad.getColumns() ) {
            if ( ad.getColumnType( col ).equals( String.class ) ) {
                Set<String> vals = ad.getColumn( col, String.class ).uniqueValues().stream()
                        .map( StringUtils::normalizeSpace )
                        .collect( Collectors.toSet() );
                if ( sampleColumn == null && vals.stream().allMatch( val -> matcher.match( series.getSamples(), val ).size() == 1 ) ) {
                    List<GeoSample> matchedSamples = vals.stream()
                            .flatMap( val -> matcher.match( series.getSamples(), val ).stream() )
                            .collect( Collectors.toList() );
                    log.info( "Detected that " + col + " is the sample column with following values: " + vals + " matching GEO samples: " + matchedSamples + "." );
                    sampleColumn = col;
                    continue;
                }
                if ( cellTypeColumn == null && Arrays.stream( CELL_TYPE_COLUMN_NAME_KEYWORDS )
                        .anyMatch( kwd -> StringUtils.containsIgnoreCase( col, kwd ) ) ) {
                    log.info( "Detected that " + col + " is the cell type column." );
                    cellTypeColumn = col;
                    for ( String indicator : UNKNOWN_CELL_TYPE_INDICATORS ) {
                        if ( vals.contains( indicator ) ) {
                            log.info( "Detected that " + col + " uses " + indicator + " as an unknown cell type indicator." );
                            unknownCellTypeIndicator = indicator;
                            break;
                        }
                    }
                    continue;
                }
            }
            ad.getColumn( col );
        }
        if ( sampleColumn != null || cellTypeColumn != null ) {
            loader.setSampleFactorName( sampleColumn );
            loader.setCellTypeFactorName( cellTypeColumn );
            loader.setUnknownCellTypeIndicator( unknownCellTypeIndicator );
            return true;
        }
        return false;
    }
}
