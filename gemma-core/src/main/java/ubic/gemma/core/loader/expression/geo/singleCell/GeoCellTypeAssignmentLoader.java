package ubic.gemma.core.loader.expression.geo.singleCell;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import ubic.gemma.core.loader.expression.geo.model.GeoSeries;
import ubic.gemma.core.loader.expression.singleCell.AbstractDelegatingSingleCellDataLoader;
import ubic.gemma.core.loader.expression.singleCell.SingleCellDataLoader;
import ubic.gemma.core.loader.util.ftp.FTPClientFactory;
import ubic.gemma.model.common.description.Categories;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.bioAssayData.CellTypeAssignment;
import ubic.gemma.model.expression.bioAssayData.SingleCellDimension;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;

/**
 * Loads author-provided cell type from GEO supplementary files.
 * <p>
 * This loader makes best efforts to cover as many cases as possible.
 * <p>
 * Note that in the best of worlds, cell type assignments are properly structured and loaded with
 * {@link ubic.gemma.core.loader.expression.singleCell.GenericMetadataSingleCellDataLoader}.
 * @author poirigui
 */
@CommonsLog
public class GeoCellTypeAssignmentLoader extends AbstractDelegatingSingleCellDataLoader {

    /**
     * A list of CSV formats to attempt to use.
     */
    private static final CSVFormat[] csvFormats = { CSVFormat.TDF, CSVFormat.EXCEL };

    private final GeoSeries series;

    private FTPClientFactory ftpClientFactory;

    public GeoCellTypeAssignmentLoader( GeoSeries series, SingleCellDataLoader delegate ) {
        super( delegate );
        this.series = series;
    }

    @Override
    public Set<CellTypeAssignment> getCellTypeAssignments( SingleCellDimension dimension ) throws IOException {
        Set<CellTypeAssignment> assignments = new HashSet<>();
        for ( String file : series.getSupplementaryFiles() ) {
            // detect possible candidates
            for ( int i = 0; i < csvFormats.length; i++ ) {
                CSVFormat format = csvFormats[i];
                try {
                    assignments.addAll( parseCellType( file, format, dimension ) );
                    break;
                } catch ( Exception e ) {
                    if ( i < csvFormats.length - 1 ) {
                        log.warn( "Failed to parse cell type assignment from " + file + " with " + format + ", trying another format.", e );
                    } else {
                        log.warn( "Failed to parse cell type assignment from " + file + ", it will be ignored.", e );
                    }
                }
            }
        }
        // also include the assignment detected from the delegate
        assignments.addAll( super.getCellTypeAssignments( dimension ) );
        return assignments;
    }

    private List<CellTypeAssignment> parseCellType( String file, CSVFormat csvFormat, SingleCellDimension dimension ) throws IOException {
        try ( CSVParser reader = csvFormat.parse( new InputStreamReader( openFile( file ) ) ) ) {
            int sampleNameColumn = detectSampleNameColumn( reader );
            int cellIdColumn = detectCellIdColumn( reader );
            int[] cellTypeColumns = detectCellTypeColumns( reader );

            // those have matching indices
            List<CellTypeAssignment> assignments = new ArrayList<>( cellTypeColumns.length );
            List<Map<String, Characteristic>> cellTypes = new ArrayList<>( cellTypeColumns.length );
            for ( int c : cellTypeColumns ) {
                CellTypeAssignment cta = CellTypeAssignment.Factory.newInstance( reader.getHeaderNames().get( c ) );
                cta.setCellTypeIndices( new int[dimension.getNumberOfCells()] );
                Arrays.fill( cta.getCellTypeIndices(), CellTypeAssignment.UNKNOWN_CELL_TYPE );
                assignments.add( cta );
                cellTypes.add( new HashMap<>() );
            }

            for ( CSVRecord record : reader ) {
                String sampleName = StringUtils.stripToNull( record.get( sampleNameColumn ) );
                String cellId = StringUtils.stripToNull( record.get( cellIdColumn ) );
                int cellIndex = getCellIndex( sampleName, cellId, dimension );
                for ( int i = 0; i < cellTypeColumns.length; i++ ) {
                    CellTypeAssignment assignment = assignments.get( i );
                    String cellType = StringUtils.stripToNull( record.get( cellTypeColumns[i] ) );
                    int ix;
                    if ( cellTypes.get( i ).containsKey( cellType ) ) {
                        ix = assignment.getCellTypes().indexOf( cellTypes.get( i ).get( cellType ) );
                    } else if ( isNa( cellType ) ) {
                        ix = CellTypeAssignment.UNKNOWN_CELL_TYPE;
                        log.debug( "Cell type is missing for " + sampleName + ":" + cellId + ", will be encoded as " + CellTypeAssignment.UNKNOWN_CELL_TYPE + "." );
                    } else {
                        Characteristic c = Characteristic.Factory.newInstance( Categories.CELL_TYPE, cellType, null );
                        assignment.getCellTypes().add( c );
                        assignment.setNumberOfCellTypes( assignment.getCellTypes().size() );
                        ix = assignment.getCellTypes().size() - 1;
                        log.info( "New cell type detected: " + c + ", it will be encoded with " + ix + "." );
                    }

                    assignment.getCellTypeIndices()[cellIndex] = ix;
                }
            }
            return assignments;
        }
    }

    private InputStream openFile( String file ) throws IOException {
        // TODO: gzip decompression, HTTP download, etc.
        return ftpClientFactory.openStream( new URL( file ) );
    }

    /**
     * Check if a cell type indicator is for missing data.
     */
    private boolean isNa( @Nullable String cellType ) {
        // TODO: implement other possible indicators
        return cellType == null;
    }

    private int getCellIndex( String sampleName, String cellId, SingleCellDimension dimension ) {
        return 0;

    }

    /**
     * Detect the column that contains the sample name.
     */
    private int detectSampleNameColumn( CSVParser parser ) {
        return 0;
    }

    /**
     * Detect the column that contains the cell ID.
     */
    private int detectCellIdColumn( CSVParser parser ) {
        return 1;
    }

    /**
     * Detect all the columns that could contain cell type information.
     */
    private int[] detectCellTypeColumns( CSVParser parser ) {
        return new int[] { 2, 3, 4 };
    }
}
