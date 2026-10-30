package ubic.gemma.core.loader.expression.singleCell.metadata;

import lombok.Setter;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.CellLevelCharacteristics;
import ubic.gemma.model.expression.bioAssayData.CellTypeAssignment;
import ubic.gemma.model.expression.bioAssayData.SingleCellDimension;

import org.springframework.lang.Nullable;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

@Setter
public class CellLevelCharacteristicsWriter {

    private static final CSVFormat CTA_FORMAT = CSVFormat.TDF.builder().setHeader( "sample_id", "cell_id", "cell_type", "cell_type_uri" ).build();
    private static final CSVFormat CLC_FORMAT = CSVFormat.TDF.builder().setHeader( "sample_id", "cell_id", "category", "category_uri", "value", "value_uri", "name" ).build();
    /**
     * The category ID numbers the characteristics in the order they are written, and the name identifies them, so
     * that a file read back with {@link GenericCellLevelCharacteristicsMetadataParser} recreates them under the same
     * names.
     */
    private static final CSVFormat CLC_WITH_CATEGORY_ID_FORMAT = CSVFormat.TDF.builder().setHeader( "sample_id", "cell_id", "category", "category_uri", "category_id", "value", "value_uri", "name" ).build();

    /**
     * Use the {@link BioAssay} numerical ID instead of its name.
     */
    private boolean useBioAssayIds;

    public void write( SingleCellDimension dimension, Writer writer ) throws IOException {
        int i = 1;
        try ( CSVPrinter printer = CLC_WITH_CATEGORY_ID_FORMAT.print( writer ) ) {
            // the dimension holds both in hash sets, sort them so that category IDs are stable across requests
            for ( CellTypeAssignment cta : sorted( dimension.getCellTypeAssignments(), CellTypeAssignment.COMPARATOR ) ) {
                write( cta, dimension, String.valueOf( i++ ), printer );
            }
            for ( CellLevelCharacteristics clc : sorted( dimension.getCellLevelCharacteristics(), CellLevelCharacteristics.COMPARATOR ) ) {
                write( clc, dimension, String.valueOf( i++ ), printer );
            }
        }
    }

    /**
     * Write a single cell type assignment to a writer.
     * <p>
     * No {@code category_id} column is generated.
     */
    public void write( CellTypeAssignment cellLevelCharacteristics, SingleCellDimension dimension, Writer writer ) throws IOException {
        try ( CSVPrinter printer = CTA_FORMAT.print( writer ) ) {
            write( cellLevelCharacteristics, dimension, printer );
        }
    }

    /**
     * Write a single set of cell-level characteristics to a writer.
     * <p>
     * No {@code category_id} column is generated.
     */
    public void write( CellLevelCharacteristics cellLevelCharacteristics, SingleCellDimension dimension, Writer writer ) throws IOException {
        try ( CSVPrinter printer = CLC_FORMAT.print( writer ) ) {
            write( cellLevelCharacteristics, dimension, null, printer );
        }
    }

    /**
     * Write multiple sets of cell-level characteristics to a writer.
     * <p>
     * This includes an additional column, {@code category_id}, to distinguish between the different cell-level
     * characteristics that use the same category.
     */
    public void write( Collection<CellLevelCharacteristics> cellLevelCharacteristics, SingleCellDimension dimension, Writer writer ) throws IOException {
        try ( CSVPrinter printer = CLC_WITH_CATEGORY_ID_FORMAT.print( writer ) ) {
            int i = 1;
            for ( CellLevelCharacteristics characteristics : sorted( cellLevelCharacteristics, CellLevelCharacteristics.COMPARATOR ) ) {
                write( characteristics, dimension, String.valueOf( i++ ), printer );
            }
        }
    }

    private void write( CellTypeAssignment cellTypeAssignment, SingleCellDimension dimension, CSVPrinter printer ) throws IOException {
        int[] cellTypeIndices = cellTypeAssignment.getCellTypeIndices();
        for ( int cellIndex = 0; cellIndex < cellTypeIndices.length; cellIndex++ ) {
            Characteristic cellType = cellTypeAssignment.getCellType( cellIndex );
            if ( cellType == null ) {
                continue;
            }
            String sampleId = getSampleId( dimension, cellIndex );
            String cellId = dimension.getCellIds().get( cellIndex );
            printer.printRecord( sampleId, cellId, cellType.getValue(), cellType.getValueUri() );
        }
    }

    private void write( CellLevelCharacteristics cellLevelCharacteristics, SingleCellDimension dimension, @Nullable String categoryId, CSVPrinter printer ) throws IOException {
        int[] indices = cellLevelCharacteristics.getIndices();
        for ( int cellIndex = 0; cellIndex < indices.length; cellIndex++ ) {
            Characteristic c = cellLevelCharacteristics.getCharacteristic( cellIndex );
            if ( c == null ) {
                continue;
            }
            String sampleId = getSampleId( dimension, cellIndex );
            String cellId = dimension.getCellIds().get( cellIndex );
            if ( categoryId != null ) {
                printer.printRecord( sampleId, cellId, c.getCategory(), c.getCategoryUri(), categoryId, c.getValue(), c.getValueUri(), cellLevelCharacteristics.getName() );
            } else {
                printer.printRecord( sampleId, cellId, c.getCategory(), c.getCategoryUri(), c.getValue(), c.getValueUri(), cellLevelCharacteristics.getName() );
            }
        }
    }

    private static <T> List<T> sorted( Collection<? extends T> items, Comparator<? super T> comparator ) {
        List<T> result = new ArrayList<>( items );
        result.sort( comparator );
        return result;
    }

    private String getSampleId( SingleCellDimension dimension, int cellIndex ) {
        BioAssay bioAssay = dimension.getBioAssay( cellIndex );
        if ( useBioAssayIds ) {
            return String.valueOf( bioAssay.getId() );
        } else if ( bioAssay.getShortName() != null ) {
            return bioAssay.getShortName();
        } else {
            return bioAssay.getName();
        }
    }
}
