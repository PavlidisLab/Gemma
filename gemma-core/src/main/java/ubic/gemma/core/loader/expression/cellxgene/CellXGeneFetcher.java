package ubic.gemma.core.loader.expression.cellxgene;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import org.springframework.util.Assert;
import ubic.gemma.core.loader.expression.cellxgene.model.CollectionMetadata;
import ubic.gemma.core.loader.expression.cellxgene.model.DatasetAssetDownloadMetadata;
import ubic.gemma.core.loader.expression.cellxgene.model.DatasetMetadata;
import ubic.gemma.core.loader.util.fetcher2.AbstractFetcher;
import ubic.gemma.core.loader.util.hdf5.H5File;
import ubic.gemma.core.loader.util.hdf5.TruncatedH5FileException;
import ubic.gemma.core.util.SimpleDownloader;
import ubic.gemma.core.util.SimpleRetryPolicy;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Fetch data from CELLxGENE.
 *
 * @author poirigui
 */
public class CellXGeneFetcher extends AbstractFetcher {

    private final ObjectMapper objectMapper;
    private final Path downloadPath;

    public CellXGeneFetcher( SimpleRetryPolicy retryPolicy, Path downloadPath ) {
        super( new SimpleDownloader( retryPolicy ) );
        this.objectMapper = new ObjectMapper()
                .configure( DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false )
                .configure( DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true )
                .setPropertyNamingStrategy( PropertyNamingStrategies.SNAKE_CASE );
        this.downloadPath = downloadPath;
    }

    public List<CollectionMetadata> fetchAllCollectionMetadata() throws IOException {
        return objectMapper.readValue( new URL( "https://api.cellxgene.cziscience.com/dp/v1/collections/index" ),
                objectMapper.getTypeFactory().constructCollectionLikeType( List.class, CollectionMetadata.class ) );
    }

    public CollectionMetadata fetchCollectionMetadata( String collectionId ) throws IOException {
        return objectMapper.readValue( new URL( "https://api.cellxgene.cziscience.com/dp/v1/collections/" + collectionId ), CollectionMetadata.class );
    }

    private List<DatasetMetadata> cachedDatasetMetadata = null;
    private Map<String, DatasetMetadata> cachedDatasetMetadataByDatasetId = null;
    private long lastDatasetMetadataContentLength = -1;

    public List<DatasetMetadata> fetchAllDatasetMetadata() throws IOException {
        URLConnection connection = new URL( "https://api.cellxgene.cziscience.com/dp/v1/datasets/index" ).openConnection();
        try {
            if ( cachedDatasetMetadata == null || connection.getContentLengthLong() == -1 || connection.getContentLengthLong() != lastDatasetMetadataContentLength ) {
                log.warn( "Fetching metadata for all CELLxGENE datasets, this might take a moment..." );
                cachedDatasetMetadata = objectMapper.readValue( connection.getInputStream(),
                        objectMapper.getTypeFactory().constructCollectionLikeType( List.class, DatasetMetadata.class ) );
                cachedDatasetMetadataByDatasetId = cachedDatasetMetadata.stream()
                        .collect( Collectors.toMap( DatasetMetadata::getId, Function.identity() ) );
                lastDatasetMetadataContentLength = connection.getContentLengthLong();
            }
        } finally {
            if ( connection instanceof HttpURLConnection ) {
                ( ( HttpURLConnection ) connection ).disconnect();
            }
        }
        return cachedDatasetMetadata;
    }

    /**
     * Retrieves the metadata for a specific CELLxGENE dataset.
     * <p>
     * There is no dedicated endpoint for fetching a single dataset's metadata, so this method relies on cache the
     * output of {@link #fetchAllDatasetMetadata()}.
     */
    public DatasetMetadata fetchDatasetMetadata( String datasetId ) throws IOException {
        fetchAllDatasetMetadata();
        if ( cachedDatasetMetadataByDatasetId.containsKey( datasetId ) ) {
            return cachedDatasetMetadataByDatasetId.get( datasetId );
        } else {
            throw new FileNotFoundException( "No dataset with ID " + datasetId + " found in CELLxGENE." );
        }
    }

    public DatasetAssetDownloadMetadata fetchDatasetAssetDownloadMetadata( String datasetId, String assetId ) throws IOException {
        return objectMapper.readValue( new URL( "https://api.cellxgene.cziscience.com/dp/v1/datasets/" + datasetId + "/asset/" + assetId ),
                DatasetAssetDownloadMetadata.class );
    }

    public Path downloadDatasetAsset( String datasetId, String assetId, FileType fileType ) throws IOException {
        Assert.isTrue( fileType == FileType.H5AD, "Only H5AD file type is supported currently." );
        DatasetAssetDownloadMetadata meta = fetchDatasetAssetDownloadMetadata( datasetId, assetId );
        Path dest = downloadPath.resolve( datasetId + ".h5ad" );
        simpleDownloader.download( new URL( meta.getUrl() ),
                dest, false );
        try ( H5File ignored = H5File.open( dest ) ) {
            // TODO: do some checks?
        } catch ( TruncatedH5FileException e ) {
            Files.delete( dest );
            throw e;
        }
        return dest;
    }
}
