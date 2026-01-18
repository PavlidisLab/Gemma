package ubic.gemma.core.loader.expression.ucsc.cellbrowser;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import ubic.gemma.core.loader.expression.ucsc.cellbrowser.model.Dataset;
import ubic.gemma.core.loader.expression.ucsc.cellbrowser.model.DatasetSummary;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * @author poirigui
 */
public class UcscCellBrowserUtils {

    private static final String UCSC_CELL_BROWSER_URL = "https://cells.ucsc.edu";

    private static final ObjectMapper mapper = new ObjectMapper()
            .configure( DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false );

    /**
     * Obtain all the UCSC Cell Browser datasets.
     */
    public static List<DatasetSummary> getDatasets() throws IOException {
        return parseDatasets( new URL( UCSC_CELL_BROWSER_URL + "/dataset.json" ) ).getDatasets();
    }

    static Datasets parseDatasets( URL url ) throws IOException {
        return mapper.readValue( url, Datasets.class );
    }

    @Data
    static class Datasets {
        public List<DatasetSummary> datasets;
    }

    public static Dataset getDataset( String datasetId ) throws IOException {
        return parseDataset( new URL( UCSC_CELL_BROWSER_URL + "/" + urlEncode( datasetId ) + "/dataset.json" ) );
    }

    static Dataset parseDataset( URL url ) throws IOException {
        return mapper.readValue( url, Dataset.class );
    }

    /**
     * Construct a URL to access an UCSC Cell Browser dataset.
     *
     * @param datasetId
     * @return
     */
    public static String getDatasetUrl( String datasetId ) {
        return UCSC_CELL_BROWSER_URL + "?ds=" + urlEncode( datasetId );
    }

    private static String urlEncode( String s ) {
        try {
            return URLEncoder.encode( s, StandardCharsets.UTF_8.name() );
        } catch ( UnsupportedEncodingException e ) {
            throw new RuntimeException( e );
        }
    }
}
