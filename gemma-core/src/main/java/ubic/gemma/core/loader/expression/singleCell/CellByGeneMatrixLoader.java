package ubic.gemma.core.loader.expression.singleCell;

import org.apache.commons.csv.CSVFormat;
import ubic.gemma.core.loader.util.mapper.BioAssayMapper;

import java.io.IOException;
import java.util.stream.Stream;

/**
 * Common interface for loading cell-by-gene single-cell data matrices.
 * @author poirigui
 */
public interface CellByGeneMatrixLoader extends SingleCellDataLoader {

    CSVFormat DEFAULT_FORMAT = CSVFormat.TDF.builder()
            .setHeader()
            .setSkipHeaderRecord( true )
            .get();

    /**
     * {@inheritDoc}
     * <p>
     * When loading data from a cell-by-gene matrix, it is highly recommended to use {@link ubic.gemma.core.loader.util.mapper.TabularDataBioAssayMapper}
     * which can deal with various edge cases.
     */
    @Override
    void setBioAssayToSampleNameMapper( BioAssayMapper bioAssayToSampleNameMatcher );

    /**
     * Stream the genes contained in a cell-by-gene matrix.
     * <p>
     * Because genes can be stored as columns, this method provides a memory-efficient way to access gene identifiers
     * without keeping all the data in-memory.
     */
    Stream<String> streamGenes() throws IOException;

    /**
     * Set the tabular/CSV format to use for parsing the cell-by-gene matrix.
     * <p>
     * The default is a TSV whose first row contains cell IDs and first column contains gene identifiers.
     */
    void setFormat( CSVFormat csvFormat );

    /**
     * Operate on the transpose of the matrix.
     * <p>
     * This is usually less efficient because vectors need to be sliced across all the lines of the file.
     */
    void setTranspose( boolean transpose );
}
