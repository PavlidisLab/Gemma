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

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

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

    public List<DatasetMetadata> fetchAllDatasetMetadata() throws IOException {
        return objectMapper.readValue( new URL( "https://api.cellxgene.cziscience.com/dp/v1/datasets/index" ),
                objectMapper.getTypeFactory().constructCollectionLikeType( List.class, DatasetMetadata.class ) );
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
