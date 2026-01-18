package ubic.gemma.core.loader.expression.ucsc.cellbrowser;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import ubic.gemma.core.datastructure.matrix.CellBrowserTabularMetadataReader;
import ubic.gemma.core.datastructure.matrix.SingleCellExpressionDataMatrix;
import ubic.gemma.core.loader.expression.ucsc.cellbrowser.model.Dataset;
import ubic.gemma.core.loader.expression.ucsc.cellbrowser.model.DatasetDescription;
import ubic.gemma.core.loader.expression.ucsc.cellbrowser.model.DatasetSummary;
import ubic.gemma.core.util.ProgressInputStream;
import ubic.gemma.core.util.ProgressReporter;
import ubic.gemma.core.visualization.cellbrowser.CellBrowserTabularMatrixReader;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.designElement.CompositeSequence;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

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

    public static DatasetDescription getDatasetDescription( String datasetId ) throws IOException {
        URL url = new URL( UCSC_CELL_BROWSER_URL + "/" + urlEncode( datasetId ) + "/desc.json" );
        return mapper.readValue( url, DatasetDescription.class );
    }

    /**
     * Construct a URL to access an UCSC Cell Browser dataset.
     */
    public static String getDatasetUrl( String datasetId ) {
        return UCSC_CELL_BROWSER_URL + "?ds=" + urlEncode( datasetId );
    }

    /**
     * Retrieve the metadata for a given dataset.
     *
     * @return a mapping of cell IDs to their metadata fields.
     */
    public static Map<String, Map<String, Object>> getDatasetMetadata( String datasetId ) throws IOException {
        URL url = new URL( UCSC_CELL_BROWSER_URL + "/" + urlEncode( datasetId ) + "/meta.tsv" );
        try ( Reader reader = new InputStreamReader( url.openStream(), StandardCharsets.UTF_8 ) ) {
            return new CellBrowserTabularMetadataReader().read( reader );
        }
    }

    /**
     * Retrieve the metadata for a specific field.
     *
     * @return a mapping of cell IDs to the value of the specified metadata field.
     */
    public static Map<String, Object> getDatasetMetadata( String datasetId, String metaField ) throws IOException {
        // TODO: support the binary format available under /{datasetId}/metaFields/{metaField}.bin.gz
        URL url = new URL( UCSC_CELL_BROWSER_URL + "/" + urlEncode( datasetId ) + "/meta.tsv" );
        try ( Reader reader = new InputStreamReader( url.openStream(), StandardCharsets.UTF_8 ) ) {
            Map<String, Map<String, Object>> meta = new CellBrowserTabularMetadataReader().read( reader );
            if ( meta.values().stream().noneMatch( e -> e.containsKey( metaField ) ) ) {
                String possibleValues = meta.values().stream()
                        .flatMap( e -> e.keySet().stream() )
                        .distinct()
                        .sorted()
                        .collect( Collectors.joining( ", " ) );
                throw new IllegalArgumentException( "Metadata field '" + metaField + "' not found in dataset '" + datasetId + "'. Possible values are: " + possibleValues + "." );
            }
            return meta.entrySet().stream()
                    .filter( e -> e.getValue().containsKey( metaField ) )
                    .collect( Collectors.toMap( Map.Entry::getKey, e -> e.getValue().get( metaField ) ) );
        }
    }

    /**
     * Create a stream over the expression data matrix of a given dataset.
     *
     * @param designElementsMap
     * @param cellIdToAssayMap  mapping of cell IDs to {@link BioAssay} they belong to
     */
    public static SingleCellExpressionDataMatrix<?> getDatasetDataMatrix( String datasetId, QuantitationType quantitationType, Map<String, CompositeSequence> designElementsMap, Map<String, BioAssay> cellIdToAssayMap, ProgressReporter progressReporter ) throws IOException {
        URL url = new URL( UCSC_CELL_BROWSER_URL + "/" + urlEncode( datasetId ) + "/exprMatrix.tsv.gz" );
        URLConnection connection = url.openConnection();
        long contentLength = connection.getContentLengthLong();
        try ( Reader reader = new InputStreamReader( new GZIPInputStream( new ProgressInputStream(
                connection.getInputStream(), progressReporter, contentLength ) ), StandardCharsets.UTF_8 ) ) {
            return new CellBrowserTabularMatrixReader().readMatrix( reader, quantitationType, designElementsMap, cellIdToAssayMap );
        } finally {
            if ( connection instanceof HttpURLConnection ) {
                ( ( HttpURLConnection ) connection ).disconnect();
            }
        }
    }

    private static String urlEncode( String s ) {
        try {
            return URLEncoder.encode( s, StandardCharsets.UTF_8.name() );
        } catch ( UnsupportedEncodingException e ) {
            throw new RuntimeException( e );
        }
    }
}
