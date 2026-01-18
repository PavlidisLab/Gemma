package ubic.gemma.core.datastructure.matrix;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author poirigui
 */
public class CellBrowserTabularMetadataReader {

    private static final CSVFormat CELL_BROWSER_METADATA_FORMAT = CSVFormat.TDF.builder()
            .setHeader()
            .setSkipHeaderRecord( true )
            .get();

    /**
     * Read single-cell metadata from a UCSC Cell Browser tabular file.
     *
     * @return a mapping of cell IDs to their metadata fields.
     */
    public Map<String, Map<String, Object>> read( Reader reader ) throws IOException {
        try ( CSVParser parser = CELL_BROWSER_METADATA_FORMAT.parse( reader ) ) {
            Map<String, Map<String, Object>> result = new HashMap<>();
            List<String> headerNames = parser.getHeaderNames();
            List<String> metaFields = headerNames.subList( 1, headerNames.size() );
            for ( CSVRecord record : parser ) {
                String cellId = record.get( "cellId" );
                for ( String metaField : metaFields ) {
                    String val = record.get( metaField );
                    if ( val == null || val.isEmpty() ) {
                        continue;
                    }
                    result.computeIfAbsent( cellId, ( k ) -> new HashMap<>() )
                            .put( metaField, val );
                }
            }
            return result;
        }
    }
}
