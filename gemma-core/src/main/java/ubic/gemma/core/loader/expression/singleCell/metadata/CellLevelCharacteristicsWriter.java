package ubic.gemma.core.loader.expression.singleCell.metadata;

import lombok.Setter;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.util.Assert;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.CellLevelCharacteristics;
import ubic.gemma.model.expression.bioAssayData.CellTypeAssignment;
import ubic.gemma.model.expression.bioAssayData.SingleCellDimension;

import java.io.IOException;
import java.io.Writer;
import java.util.Collection;

@Setter
public class CellLevelCharacteristicsWriter {

    private static final CSVFormat CTA_FORMAT = CSVFormat.TDF.builder().setHeader( "sample_id", "cell_id", "cell_type", "cell_type_uri" ).build();
    private static final CSVFormat CLC_FORMAT = CSVFormat.TDF.builder().setHeader( "sample_id", "cell_id", "category", "category_uri", "category_id", "value", "value_uri" ).build();
    private static final CSVFormat CLC_WITH_CATEGORY_ID_FORMAT = CSVFormat.TDF.builder().setHeader( "sample_id", "cell_id", "category", "category_uri", "value", "value_uri" ).build();

    /**
     * Use the {@link BioAssay} numerical ID instead of its name.
     */
    private boolean useBioAssayId;

    public void write( SingleCellDimension dimension, Writer writer ) throws IOException {
        try ( CSVPrinter printer = CLC_WITH_CATEGORY_ID_FORMAT.print( writer ) ) {
            for ( CellTypeAssignment cta : dimension.getCellTypeAssignments() ) {
                write( cta, dimension, true, printer );
            }
            for ( CellLevelCharacteristics clc : dimension.getCellLevelCharacteristics() ) {
                write( clc, dimension, true, printer );
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
            write( cellLevelCharacteristics, dimension, false, printer );
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
            for ( CellLevelCharacteristics characteristics : cellLevelCharacteristics ) {
                write( characteristics, dimension, true, printer );
            }
        }
    }

    private void write( CellTypeAssignment cellTypeAssignment, SingleCellDimension dimension, CSVPrinter printer ) throws IOException {
        Assert.notNull( dimension.getCellIds(), "The dimension must have cell IDs." );
        int[] cellTypeIndices = cellTypeAssignment.getCellTypeIndices();
        for ( int cellIndex = 0; cellIndex < cellTypeIndices.length; cellIndex++ ) {
            Characteristic cellType = cellTypeAssignment.getCellType( cellIndex );
            if ( cellType == null ) {
                continue;
            }
            String sampleId = useBioAssayId ? dimension.getBioAssay( cellIndex ).getId().toString() : dimension.getBioAssay( cellIndex ).getName();
            String cellId = dimension.getCellIds().get( cellIndex );
            printer.printRecord( sampleId, cellId, cellType.getValue(), cellType.getValueUri() );
        }
    }

    private void write( CellLevelCharacteristics cellLevelCharacteristics, SingleCellDimension dimension, boolean includeCategoryId, CSVPrinter printer ) throws IOException {
        Assert.notNull( dimension.getCellIds(), "The dimension must have cell IDs." );
        int[] indices = cellLevelCharacteristics.getIndices();
        for ( int cellIndex = 0; cellIndex < indices.length; cellIndex++ ) {
            Characteristic c = cellLevelCharacteristics.getCharacteristic( cellIndex );
            if ( c == null ) {
                continue;
            }
            String sampleId = useBioAssayId ? dimension.getBioAssay( cellIndex ).getId().toString() : dimension.getBioAssay( cellIndex ).getName();
            String cellId = dimension.getCellIds().get( cellIndex );
            if ( includeCategoryId ) {
                printer.printRecord( sampleId, cellId, c.getValue(), c.getValueUri() );
            } else {
                printer.printRecord( sampleId, cellId, c.getCategory(), c.getCategoryUri(), null, c.getValue(), c.getValueUri() );
            }
        }
    }
}
